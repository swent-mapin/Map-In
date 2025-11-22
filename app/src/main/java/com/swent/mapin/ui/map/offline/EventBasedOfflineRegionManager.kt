package com.swent.mapin.ui.map.offline

import android.util.Log
import com.mapbox.geojson.Point
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.network.ConnectivityService
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Manages automatic downloading of map tiles for saved and joined events.
 *
 * Monitors the user's saved and joined events and proactively downloads map tiles covering a 2km
 * radius around each event location when online. This ensures users can view event locations
 * offline without relying solely on Mapbox's automatic viewport caching.
 *
 * Respects Mapbox's 750 unique tile pack limit for offline regions.
 *
 * @property offlineRegionManager Manager for downloading tile regions
 * @property connectivityService Service for monitoring network connectivity
 * @property scope Coroutine scope for asynchronous operations
 * @property radiusKm Radius around each event to download (default: 2km)
 * @property maxRegions Maximum number of regions to cache (respects Mapbox 750 tile pack limit)
 */
class EventBasedOfflineRegionManager(
    private val offlineRegionManager: OfflineRegionManager,
    private val connectivityService: ConnectivityService,
    private val scope: CoroutineScope,
    private val radiusKm: Double = DEFAULT_RADIUS_KM,
    private val maxRegions: Int = DEFAULT_MAX_REGIONS,
    private val onDownloadStart: (Event) -> Unit = {},
    private val onDownloadProgress: (Event, Float) -> Unit = { _, _ -> },
    private val onDownloadComplete: (Event, Result<Unit>) -> Unit = { _, _ -> }
) {

  companion object {
    private const val TAG = "EventBasedOfflineRegionManager"
    const val DEFAULT_RADIUS_KM = 2.0
    private const val DEFAULT_MAX_REGIONS = 100 // Conservative limit below Mapbox's 750
  }

  private var observerJob: Job? = null
  private val downloadedEventIds = mutableSetOf<String>()

  /**
   * Starts observing saved and joined events.
   *
   * When events change and the device is online, automatically downloads map tiles for a 2km radius
   * around each event location.
   *
   * @param onSavedEventsFlow Flow providing saved events (typically from MapEventStateController)
   * @param onJoinedEventsFlow Flow providing joined events (typically from MapEventStateController)
   */
  fun observeEvents(onSavedEventsFlow: Flow<List<Event>>, onJoinedEventsFlow: Flow<List<Event>>) {
    // Cancel any existing observation
    observerJob?.cancel()

    observerJob =
        scope.launch {
          // Combine saved and joined events into a single flow
          combine(onSavedEventsFlow, onJoinedEventsFlow) { saved, joined -> saved + joined }
              .collect { allEvents ->
                // Remove duplicates (event may be both saved and joined)
                val uniqueEvents = allEvents.distinctBy { it.uid }

                Log.d(TAG, "Events updated: ${uniqueEvents.size} unique events to process")

                // Only download if online
                if (connectivityService.isConnected()) {
                  downloadRegionsForEvents(uniqueEvents)
                } else {
                  Log.d(TAG, "Device offline, skipping region downloads")
                }
              }
        }
  }

  /**
   * Manually triggers region downloads for the given events.
   *
   * Useful for one-time downloads without ongoing observation.
   *
   * @param events List of events to download regions for
   */
  suspend fun downloadRegionsForEvents(events: List<Event>) {
    // Limit to maxRegions to respect Mapbox tile pack limits
    val eventsToDownload = events.take(maxRegions)

    if (events.size > maxRegions) {
      Log.w(
          TAG,
          "Event count (${events.size}) exceeds max regions ($maxRegions). " +
              "Only downloading first $maxRegions events.")
    }

    // Download sequentially to avoid canceling in-progress downloads
    for (event in eventsToDownload) {
      // Skip if already downloaded
      if (downloadedEventIds.contains(event.uid)) {
        Log.d(TAG, "Event ${event.uid} already downloaded, skipping")
        continue
      }

      // Calculate 2km radius bounds
      val bounds = calculateBoundsForRadius(event.location.latitude, event.location.longitude)

      Log.d(
          TAG,
          "Downloading region for event: ${event.title} " +
              "(${event.location.latitude}, ${event.location.longitude})")

      // Notify UI that download is starting
      onDownloadStart(event)

      // Download and wait for completion before starting next one
      val result = downloadRegionSuspend(event, bounds)
      result
          .onSuccess {
            downloadedEventIds.add(event.uid)
            Log.d(TAG, "Successfully downloaded region for event: ${event.title}")
            onDownloadComplete(event, Result.success(Unit))
          }
          .onFailure { error ->
            Log.e(TAG, "Failed to download region for event ${event.title}: $error")
            onDownloadComplete(event, Result.failure(error))
          }
    }
  }

  /**
   * Downloads a region and suspends until completion.
   *
   * @param event The event to download for
   * @param bounds The geographic bounds to download
   * @return Result indicating success or failure
   */
  private suspend fun downloadRegionSuspend(event: Event, bounds: CoordinateBounds): Result<Unit> {
    val deferred = CompletableDeferred<Result<Unit>>()

    offlineRegionManager.downloadRegion(
        bounds = bounds,
        onProgress = { progress ->
          Log.d(TAG, "Event ${event.uid} download progress: ${(progress * 100).toInt()}%")
          onDownloadProgress(event, progress)
        },
        onComplete = { result -> deferred.complete(result) })

    return deferred.await()
  }

  /**
   * Calculates a bounding box for a given radius around a center point.
   *
   * Uses a simple approximation suitable for small radii (< 10km) at typical latitudes. For 2km
   * radius, the error is negligible for our use case.
   *
   * @param centerLat Center latitude in degrees
   * @param centerLng Center longitude in degrees
   * @return CoordinateBounds representing the bounding box
   */
  private fun calculateBoundsForRadius(centerLat: Double, centerLng: Double): CoordinateBounds {
    return calculateBoundsForRadius(centerLat, centerLng, radiusKm)
  }

  /**
   * Stops observing events and cancels any active downloads.
   *
   * Should be called when the manager is no longer needed (e.g., user logs out).
   */
  fun stopObserving() {
    observerJob?.cancel()
    observerJob = null
    offlineRegionManager.cancelActiveDownload()
    Log.d(TAG, "Stopped observing events")
  }

  /**
   * Clears the record of downloaded events.
   *
   * This will cause all events to be re-downloaded on the next observation cycle. Useful for
   * forcing a refresh or after clearing the tile cache.
   */
  fun clearDownloadedEventIds() {
    downloadedEventIds.clear()
    Log.d(TAG, "Cleared downloaded event IDs")
  }

  /**
   * Gets the number of downloaded event regions.
   *
   * @return Count of successfully downloaded event regions
   */
  fun getDownloadedCount(): Int = downloadedEventIds.size

  /**
   * Gets the maximum number of regions allowed.
   *
   * @return Maximum regions limit
   */
  fun getMaxRegions(): Int = maxRegions
}

/**
 * Calculates a bounding box for a given radius around a center point.
 *
 * Uses simple approximation based on degrees of latitude/longitude:
 * - 1 degree latitude ≈ 111 km everywhere
 * - 1 degree longitude ≈ 111 km * cos(latitude) at a given latitude
 *
 * For small radii (< 10km) at typical latitudes, this approximation is sufficient.
 *
 * @param centerLat Center latitude in degrees
 * @param centerLng Center longitude in degrees
 * @param radiusKm Radius in kilometers
 * @return CoordinateBounds representing the bounding box
 */
fun calculateBoundsForRadius(
    centerLat: Double,
    centerLng: Double,
    radiusKm: Double
): CoordinateBounds {
  // Approximate degrees per km
  val latDegreePerKm = 1.0 / 111.0
  val clampedLatForLongitude = centerLat.coerceIn(-89.9999, 89.9999)
  val cosLat = abs(kotlin.math.cos(Math.toRadians(clampedLatForLongitude)))
  val safeCosLat = max(1e-6, cosLat) // Prevent division by zero near the poles
  val lngDegreePerKm = 1.0 / (111.0 * safeCosLat)

  // Calculate offsets
  val latOffset = radiusKm * latDegreePerKm
  val lngOffset = radiusKm * lngDegreePerKm

  // Calculate bounds (clamped to valid lat/lng ranges)
  val swLat = max(-90.0, centerLat - latOffset)
  val swLng = max(-180.0, centerLng - lngOffset)
  val neLat = kotlin.math.min(90.0, centerLat + latOffset)
  val neLng = kotlin.math.min(180.0, centerLng + lngOffset)

  return CoordinateBounds(
      southwest = Point.fromLngLat(swLng, swLat), northeast = Point.fromLngLat(neLng, neLat))
}

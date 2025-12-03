package com.swent.mapin.ui.map.offline

import android.content.Context
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
 * @property context Android context for showing system notifications
 * @property radiusKm Radius around each event to download (default: 2km)
 * @property maxRegions Maximum number of regions to cache (respects Mapbox 750 tile pack limit)
 */
class EventBasedOfflineRegionManager(
    private val offlineRegionManager: OfflineRegionManager,
    private val connectivityService: ConnectivityService,
    private val scope: CoroutineScope,
    private val context: Context? = null,
    private val radiusKm: Double = DEFAULT_RADIUS_KM,
    private val maxRegions: Int = DEFAULT_MAX_REGIONS,
    private val onDownloadStart: (Event) -> Unit = {},
    private val onDownloadProgress: (Event, Float) -> Unit = { _, _ -> },
    private val onDownloadComplete: (Event, Result<Unit>) -> Unit = { _, _ -> }
) {

  companion object {
    private const val TAG = "EventBasedOfflineRegionManager"
    const val DEFAULT_RADIUS_KM = 2.0
    private const val DEFAULT_MAX_REGIONS = 100 // Conservative limit to stay under 4GB cache
  }

  private val notificationManager: DownloadNotificationManager? =
      context?.let { DownloadNotificationManager(it) }

  private var observerJob: Job? = null
  private var deletionObserverJob: Job? = null
  private val downloadedEventIds = mutableSetOf<String>()
  private val eventLocations = mutableMapOf<String, Pair<Double, Double>>() // eventId -> (lat, lng)
  private var previousEventIds = setOf<String>()

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
      notificationManager?.showDownloadProgress(event, 0f)

      // Download and wait for completion before starting next one
      val result = downloadRegionSuspend(event, bounds)
      result
          .onSuccess {
            downloadedEventIds.add(event.uid)
            // Store location for future deletion
            eventLocations[event.uid] = Pair(event.location.latitude, event.location.longitude)
            Log.d(TAG, "Successfully downloaded region for event: ${event.title}")
            notificationManager?.showDownloadComplete(event)
            onDownloadComplete(event, Result.success(Unit))
          }
          .onFailure { error ->
            Log.e(TAG, "Failed to download region for event ${event.title}: $error")
            notificationManager?.showDownloadFailed(event)
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
          notificationManager?.showDownloadProgress(event, progress)
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
   * Starts observing saved and joined events for deletion.
   *
   * When events are removed (unsaved or left) or when events have finished (past end date),
   * automatically deletes the corresponding map tiles to free up storage space.
   *
   * @param onSavedEventsFlow Flow providing saved events
   * @param onJoinedEventsFlow Flow providing joined events
   */
  fun observeEventsForDeletion(
      onSavedEventsFlow: Flow<List<Event>>,
      onJoinedEventsFlow: Flow<List<Event>>
  ) {
    // Cancel any existing observation
    deletionObserverJob?.cancel()

    deletionObserverJob =
        scope.launch {
          // Combine saved and joined events into a single flow
          combine(onSavedEventsFlow, onJoinedEventsFlow) { saved, joined -> saved + joined }
              .collect { allEvents ->
                // Remove duplicates (event may be both saved and joined)
                val uniqueEvents = allEvents.distinctBy { it.uid }

                // Filter out finished events (those that have ended)
                val activeEvents = uniqueEvents.filter { !isEventFinished(it) }
                val currentEventIds = activeEvents.map { it.uid }.toSet()

                // Find events that were removed OR finished (in previous but not in current)
                val removedEventIds = previousEventIds - currentEventIds

                if (removedEventIds.isNotEmpty()) {
                  Log.d(
                      TAG,
                      "Detected ${removedEventIds.size} removed/finished events, deleting regions")
                  deleteRegionsForEvents(removedEventIds)
                }

                // Update previous state for next iteration
                previousEventIds = currentEventIds
              }
        }
  }

  /**
   * Checks if an event has finished based on its end date.
   *
   * An event is considered finished if:
   * - It has an endDate AND the endDate is in the past, OR
   * - It has no endDate but has a date AND the date is in the past
   *
   * @param event The event to check
   * @return true if the event has finished, false otherwise
   */
  private fun isEventFinished(event: Event): Boolean {
    val now = com.google.firebase.Timestamp.now()

    // Check endDate first (if provided)
    event.endDate?.let {
      return it < now
    }

    // Fall back to start date if no endDate
    event.date?.let {
      return it < now
    }

    // If no dates at all, consider it active
    return false
  }

  /**
   * Deletes tile regions for the given event IDs.
   *
   * @param eventIds Set of event IDs to delete regions for
   */
  private fun deleteRegionsForEvents(eventIds: Set<String>) {
    scope.launch {
      for (eventId in eventIds) {
        // Get stored location for this event
        val location = eventLocations[eventId]
        if (location != null) {
          val (lat, lng) = location
          val bounds = calculateBoundsForRadius(lat, lng)

          Log.d(TAG, "Deleting region for removed event: $eventId")

          val result = offlineRegionManager.removeTileRegion(bounds)
          result
              .onSuccess {
                downloadedEventIds.remove(eventId)
                eventLocations.remove(eventId)
                Log.d(TAG, "Successfully deleted region for event: $eventId")
              }
              .onFailure { error ->
                Log.e(TAG, "Failed to delete region for event $eventId: $error")
                // Still remove from tracking even if deletion fails
                downloadedEventIds.remove(eventId)
                eventLocations.remove(eventId)
              }
        } else {
          // Event wasn't downloaded or location not stored, just remove from tracking
          downloadedEventIds.remove(eventId)
          Log.d(TAG, "Removed event $eventId from tracking (no location stored)")
        }
      }
    }
  }

  /**
   * Deletes the tile region for a specific event.
   *
   * @param event The event to delete the region for
   */
  fun deleteRegionForEvent(event: Event) {
    scope.launch {
      val bounds = calculateBoundsForRadius(event.location.latitude, event.location.longitude)

      Log.d(TAG, "Deleting region for event: ${event.title}")

      val result = offlineRegionManager.removeTileRegion(bounds)
      result
          .onSuccess {
            downloadedEventIds.remove(event.uid)
            eventLocations.remove(event.uid)
            Log.d(TAG, "Successfully deleted region for event: ${event.title}")
          }
          .onFailure { error ->
            Log.e(TAG, "Failed to delete region for event ${event.title}: $error")
          }
    }
  }

  /**
   * Stops observing events and cancels any active downloads.
   *
   * Should be called when the manager is no longer needed (e.g., user logs out).
   */
  fun stopObserving() {
    observerJob?.cancel()
    observerJob = null
    deletionObserverJob?.cancel()
    deletionObserverJob = null
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
    eventLocations.clear()
    previousEventIds = setOf()
    Log.d(TAG, "Cleared downloaded event IDs and locations")
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

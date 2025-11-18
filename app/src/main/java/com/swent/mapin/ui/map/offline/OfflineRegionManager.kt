package com.swent.mapin.ui.map.offline

import android.util.Log
import com.mapbox.common.Cancelable
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.Style
import com.mapbox.maps.TilesetDescriptorOptions
import kotlinx.coroutines.flow.first

/**
 * Manager for downloading and caching offline map regions.
 *
 * Coordinates automatic tile downloads for viewed map areas when the device is online. Downloads
 * are triggered asynchronously based on viewport changes and connectivity state.
 *
 * @property tileStore The TileStore instance for managing cached tiles
 * @property connectivityProvider Provider for checking network connectivity
 * @property offlineManager Mapbox OfflineManager for creating tileset descriptors
 */
class OfflineRegionManager(
    private val tileStore: TileStore,
    private val connectivityProvider: () -> kotlinx.coroutines.flow.Flow<Boolean>,
    private val offlineManager: OfflineManager = OfflineManager()
) {

  companion object {
    private const val TAG = "OfflineRegionManager"
    private const val MIN_ZOOM = 0
    private const val MAX_ZOOM = 16
    private const val TILE_REGION_ID_PREFIX = "region_"
  }

  private var currentDownload: Cancelable? = null

  /**
   * Downloads tiles for the specified geographic bounds.
   *
   * Only triggers download if device is online. If a download is already in progress, it will be
   * canceled before starting the new download.
   *
   * @param bounds The geographic bounds to download tiles for
   * @param styleUri The map style URI to download tiles for
   * @param onProgress Callback for download progress updates (0.0 to 1.0)
   * @param onComplete Callback invoked when download completes or fails
   */
  suspend fun downloadRegion(
      bounds: CoordinateBounds,
      styleUri: String = Style.MAPBOX_STREETS,
      onProgress: (Float) -> Unit = {},
      onComplete: (Result<Unit>) -> Unit = {}
  ) {
    // Check connectivity
    val isConnected =
        try {
          connectivityProvider().first()
        } catch (e: Exception) {
          Log.e(TAG, "Failed to check connectivity", e)
          onComplete(Result.failure(e))
          return
        }

    if (!isConnected) {
      Log.d(TAG, "Device offline, skipping download")
      onComplete(Result.failure(IllegalStateException("Device is offline")))
      return
    }

    // Cancel any existing download and clear state to prevent stale callbacks
    currentDownload?.cancel()
    currentDownload = null

    try {
      // Create tileset descriptor
      val tilesetDescriptor =
          offlineManager.createTilesetDescriptor(
              TilesetDescriptorOptions.Builder()
                  .styleURI(styleUri)
                  .minZoom(MIN_ZOOM.toByte())
                  .maxZoom(MAX_ZOOM.toByte())
                  .build())

      // Convert bounds to Polygon geometry
      val geometry = bounds.toPolygon()

      // Configure tile region options
      val tileRegionId = generateRegionId(bounds)
      val loadOptions =
          TileRegionLoadOptions.Builder()
              .geometry(geometry)
              .descriptors(listOf(tilesetDescriptor))
              .acceptExpired(false)
              .networkRestriction(NetworkRestriction.NONE)
              .build()

      // Start download
      currentDownload =
          tileStore.loadTileRegion(
              tileRegionId,
              loadOptions,
              { progress ->
                // Progress callback
                val completedResources = progress.completedResourceCount
                val requiredResources = progress.requiredResourceCount
                if (requiredResources > 0) {
                  val progressPercent = completedResources.toFloat() / requiredResources
                  onProgress(progressPercent)
                }
              }) { expected ->
                currentDownload = null
                if (expected.isValue) {
                  Log.d(TAG, "Region download completed: $tileRegionId")
                  onComplete(Result.success(Unit))
                } else {
                  expected.error?.let { error ->
                    Log.e(TAG, "Region download failed: ${error.message}", null)
                    onComplete(Result.failure(Exception(error.message)))
                  }
                }
              }

      Log.d(TAG, "Started download for region: $tileRegionId")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start region download", e)
      currentDownload = null
      onComplete(Result.failure(e))
    }
  }

  /**
   * Cancels any active download and clears internal state.
   *
   * Should be called when the owning component (e.g., ViewModel) is being destroyed to prevent
   * leaking work or keeping network resources alive.
   */
  fun cancelActiveDownload() {
    currentDownload?.cancel()
    currentDownload = null
    Log.d(TAG, "Active download cancelled")
  }

  /**
   * Generates a unique region ID based on bounds.
   *
   * @param bounds The coordinate bounds
   * @return A unique region identifier
   */
  private fun generateRegionId(bounds: CoordinateBounds): String {
    val hash = bounds.hashCode()
    return "$TILE_REGION_ID_PREFIX${hash.toString().replace("-", "n")}"
  }
}

/**
 * Represents geographic coordinate bounds.
 *
 * @property southwest The southwest corner point
 * @property northeast The northeast corner point
 */
data class CoordinateBounds(val southwest: Point, val northeast: Point) {

  /**
   * Converts bounds to a Polygon geometry for Mapbox API.
   *
   * @return A Polygon representing the bounding box
   */
  fun toPolygon(): Polygon {
    val sw = southwest
    val ne = northeast

    // Create polygon from bounds (clockwise order)
    val coordinates =
        listOf(
            listOf(
                Point.fromLngLat(sw.longitude(), sw.latitude()), // SW
                Point.fromLngLat(ne.longitude(), sw.latitude()), // SE
                Point.fromLngLat(ne.longitude(), ne.latitude()), // NE
                Point.fromLngLat(sw.longitude(), ne.latitude()), // NW
                Point.fromLngLat(sw.longitude(), sw.latitude()) // Close the ring
                ))

    return Polygon.fromLngLats(coordinates)
  }
}

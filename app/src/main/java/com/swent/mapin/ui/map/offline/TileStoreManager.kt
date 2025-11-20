package com.swent.mapin.ui.map.offline

import androidx.annotation.VisibleForTesting
import com.mapbox.bindgen.Value
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions

/**
 * Manager for Mapbox TileStore, handling offline map tile caching configuration.
 *
 * Provides a wrapper around Mapbox's TileStore API to manage offline tile storage with configurable
 * disk quotas and lifecycle management.
 *
 * @property tileStore The underlying Mapbox TileStore instance
 * @property diskQuotaMB Maximum storage space for cached tiles in megabytes
 */
class TileStoreManager(
    private val tileStore: TileStore = TileStore.create(),
    private val diskQuotaMB: Long = DEFAULT_DISK_QUOTA_MB
) {
  init {
    require(diskQuotaMB > 0) { "Disk quota must be positive, got $diskQuotaMB MB" }
  }

  companion object {
    /**
     * Default disk quota for tile storage: 2 GB for offline map regions (supports ~40-60 regions)
     */
    const val DEFAULT_DISK_QUOTA_MB = 2048L

    /** Conversion factor from megabytes to bytes */
    private const val MB_TO_BYTES = 1024L * 1024L
  }

  /**
   * Initializes the TileStore with the configured disk quota.
   *
   * Sets the maximum storage space for cached map tiles. When approaching the quota, the tile store
   * automatically evicts expired tile packs to free up space.
   *
   * @throws IllegalStateException if disk quota cannot be set
   */
  fun initialize() {
    val quotaBytes = diskQuotaMB * MB_TO_BYTES
    tileStore.setOption(TileStoreOptions.DISK_QUOTA, Value(quotaBytes))
  }

  /**
   * Gets the current TileStore instance.
   *
   * Exposed for integration with Mapbox MapView and offline tile loading operations.
   *
   * @return The configured TileStore instance
   */
  fun getTileStore(): TileStore = tileStore

  /**
   * Gets the configured disk quota in megabytes.
   *
   * @return Maximum storage space allocated for cached tiles
   */
  fun getDiskQuotaMB(): Long = diskQuotaMB

  /**
   * Gets the configured disk quota in bytes.
   *
   * Useful for displaying storage information to users or monitoring usage.
   *
   * @return Maximum storage space in bytes
   */
  @VisibleForTesting internal fun getDiskQuotaBytes(): Long = diskQuotaMB * MB_TO_BYTES
}

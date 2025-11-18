package com.swent.mapin.ui.map.offline

import androidx.annotation.VisibleForTesting

/**
 * Singleton provider for TileStoreManager.
 *
 * Ensures a single TileStoreManager instance is used throughout the app, preventing multiple
 * TileStore configurations from conflicting.
 */
object TileStoreManagerProvider {
  private var instance: TileStoreManager? = null

  /**
   * Gets or creates the TileStoreManager instance.
   *
   * Initializes the TileStore on first access with the default disk quota.
   *
   * @return The singleton TileStoreManager instance
   */
  fun getInstance(): TileStoreManager {
    return instance
        ?: synchronized(this) {
          instance
              ?: TileStoreManager().also {
                it.initialize()
                instance = it
              }
        }
  }

  /**
   * Sets a custom TileStoreManager instance.
   *
   * This method is intended for testing purposes only, allowing tests to inject mock or fake
   * implementations of TileStoreManager.
   *
   * @param manager The TileStoreManager instance to use
   */
  @VisibleForTesting
  fun setInstance(manager: TileStoreManager) {
    synchronized(this) { instance = manager }
  }

  /**
   * Clears the current TileStoreManager instance.
   *
   * This method is intended for testing purposes only, allowing tests to reset the singleton state
   * between test cases to ensure test isolation.
   */
  @VisibleForTesting
  fun clearInstance() {
    synchronized(this) { instance = null }
  }
}

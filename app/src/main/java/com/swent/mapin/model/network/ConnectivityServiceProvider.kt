package com.swent.mapin.model.network

import android.content.Context
import androidx.annotation.VisibleForTesting

/**
 * Singleton provider for ConnectivityService. Ensures a single instance is used throughout the app.
 */
object ConnectivityServiceProvider {
  private var instance: ConnectivityService? = null

  /**
   * Gets or creates the ConnectivityService instance.
   *
   * @param context Application context
   * @return The singleton ConnectivityService instance
   */
  fun getInstance(context: Context): ConnectivityService {
    return instance
        ?: synchronized(this) {
          instance ?: ConnectivityServiceImpl(context.applicationContext).also { instance = it }
        }
  }

  /**
   * Sets a custom ConnectivityService instance.
   *
   * This method is intended for testing purposes only, allowing tests to inject mock or fake
   * implementations of ConnectivityService.
   *
   * @param service The ConnectivityService instance to use
   */
  @VisibleForTesting
  fun setInstance(service: ConnectivityService) {
    instance = service
  }

  /**
   * Clears the current ConnectivityService instance.
   *
   * This method is intended for testing purposes only, allowing tests to reset the singleton state
   * between test cases to ensure test isolation.
   */
  @VisibleForTesting
  fun clearInstance() {
    instance = null
  }
}

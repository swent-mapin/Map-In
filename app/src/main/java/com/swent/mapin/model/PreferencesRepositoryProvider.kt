package com.swent.mapin.model

import android.content.Context
import androidx.annotation.VisibleForTesting

/**
 * Singleton provider for PreferencesRepository. Ensures a single instance is used throughout the
 * app.
 */
object PreferencesRepositoryProvider {
  private var instance: PreferencesRepository? = null

  fun getInstance(context: Context): PreferencesRepository {
    return instance
        ?: synchronized(this) {
          instance ?: PreferencesRepository(context.applicationContext).also { instance = it }
        }
  }

  /**
   * Sets a custom PreferencesRepository instance.
   *
   * This method is intended for testing purposes only, allowing tests to inject mock or fake
   * implementations of PreferencesRepository.
   *
   * @param repository The PreferencesRepository instance to use
   */
  @VisibleForTesting
  fun setInstance(repository: PreferencesRepository) {
    instance = repository
  }

  /**
   * Clears the current PreferencesRepository instance.
   *
   * This method is intended for testing purposes only, allowing tests to reset the singleton state
   * between test cases to ensure test isolation.
   */
  @VisibleForTesting
  fun clearInstance() {
    instance = null
  }
}

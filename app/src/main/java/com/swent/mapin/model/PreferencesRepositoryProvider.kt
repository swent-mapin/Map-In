package com.swent.mapin.model

import android.content.Context

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

  // For testing purposes
  fun setInstance(repository: PreferencesRepository) {
    instance = repository
  }

  fun clearInstance() {
    instance = null
  }
}

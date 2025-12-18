package com.swent.mapin.model.location

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.swent.mapin.ui.map.directions.ApiKeyProvider
import okhttp3.OkHttpClient

// Assisted by AI

/**
 * Factory class for creating an instance of [LocationViewModel]. This allows the ViewModel to be
 * constructed with a pre-configured [LocationRepository] that uses the Mapbox Access Token
 * retrieved from the Context.
 *
 * @param context The application context, used to retrieve the Mapbox Access Token.
 * @param client The OkHttpClient instance to be used by the repository.
 */
class LocationViewModelFactory(private val context: Context, private val client: OkHttpClient) :
    ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {

      // 1. Retrieve the Token using your provider
      val token = ApiKeyProvider.getMapboxAccessToken(context)

      check(token.isNotEmpty()) {
        "Mapbox Access Token not found. Please check ApiKeyProvider and your string resources."
      }

      // 2. Create the configured Repository
      val repository = MapboxRepository(client, token)

      // 3. Pass the repository to the ViewModel
      return LocationViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

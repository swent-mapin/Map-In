package com.swent.mapin.ui.map.directions

import android.content.Context
import com.swent.mapin.R

/**
 * Utility object for retrieving the Mapbox access token.
 *
 * This object provides centralized access to the Mapbox API token stored in string resources.
 */
object ApiKeyProvider {

  /**
   * Retrieves the Mapbox access token from string resources.
   *
   * The token is stored in res/values/mapbox_access_token.xml under the key "mapbox_access_token".
   *
   * @param context Application or activity context
   * @return The Mapbox access token, or an empty string if not found
   */
  fun getMapboxAccessToken(context: Context): String {
    return try {
      context.getString(R.string.mapbox_access_token)
    } catch (_: Exception) {
      ""
    }
  }
}

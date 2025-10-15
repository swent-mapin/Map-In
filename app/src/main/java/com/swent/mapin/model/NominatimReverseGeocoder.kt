package com.swent.mapin.model

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.*

// Documentation and proofreading assisted by AI tools

/**
 * Implementation of [LocationRepository] that performs **reverse geocoding** using the Nominatim
 * API (OpenStreetMap). This repository converts geographic coordinates (lat, lon) into
 * human-readable location names ([Location] objects).
 *
 * Features:
 * - Rate limiting to respect API usage rules.
 * - Validation of JSON responses to ensure coordinates are valid.
 *
 * @property client The [OkHttpClient] used to perform HTTP requests.
 */
class NominatimReverseGeocoder(client: OkHttpClient) :
    NominatimLocationRepository(client), LocationRepository {

  /**
   * Performs a reverse geocoding request for the given latitude and longitude.
   * - Uses the Nominatim API to fetch a single location matching the coordinates.
   * - Parses the JSON response and returns a [Location] object.
   *
   * @param lat The latitude of the location.
   * @param lon The longitude of the location.
   * @return A [Location] object representing the coordinates.
   * @throws [LocationSearchException] If the request fails or the response cannot be parsed.
   */
  override suspend fun reverseGeocode(lat: Double, lon: Double): Location? =
      withContext(ioDispatcher) {
        rateLimiter.acquire()

        try {
          val url =
              HttpUrl.Builder()
                  .scheme("https")
                  .host(BASE_URL)
                  .addPathSegment("reverse")
                  .addQueryParameter("lat", lat.toString())
                  .addQueryParameter("lon", lon.toString())
                  .addQueryParameter("format", "json")
                  .addQueryParameter("addressdetails", "1")
                  .build()
                  .toString()

          val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()

          client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              Log.e("NominatimReverse", "Request failed: ${response.code}")
              throw IOException("Unexpected code $response")
            }

            val body = response.body?.string() ?: throw IOException("Response body was null")
            val obj = JSONObject(body)
            val location = jsonObjectToLocation(obj)

            return@withContext location
          }
        } catch (e: Exception) {
          Log.e("NominatimReverse", "Reverse geocoding failed: ${e.message}", e)
          throw LocationSearchException("Failed to reverse geocode", e)
        }
      }

  /**
   * Converts a [JSONObject] from the Nominatim API response into a [Location] object.
   *
   * @param obj The JSON object returned by the Nominatim API.
   * @return A valid [Location] object.
   * @throws [JSONException] If latitude or longitude are missing or invalid.
   */
  private fun jsonObjectToLocation(obj: JSONObject): Location {
    val displayName = obj.optString("display_name", "Unknown location")
    val lat = obj.optDouble("lat", Double.NaN)
    val lon = obj.optDouble("lon", Double.NaN)

    if (lat.isNaN() || lon.isNaN()) {
      throw JSONException("Invalid coordinates in JSON object")
    }

    return Location(displayName, lat, lon)
  }

  /**
   * Forward geocoding is not supported in this repository.
   *
   * @throws [UnsupportedOperationException] Always thrown since this repository only supports
   *   reverse geocoding.
   */
  override suspend fun forwardGeocode(query: String): List<Location> {
    throw UnsupportedOperationException("Geocoding not supported in this repository")
  }
}

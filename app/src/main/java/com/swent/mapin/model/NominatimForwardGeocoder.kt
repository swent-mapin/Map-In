package com.swent.mapin.model

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

// Documentation and proofreading assisted by AI tools

/**
 * Implementation of [LocationRepository] that performs **forward geocoding** using the Nominatim
 * API (OpenStreetMap). This repository converts location names or queries into geographic
 * coordinates ([Location] objects).
 *
 * Features:
 * - Caching of recent queries to reduce network requests.
 * - Rate limiting to respect API usage rules.
 * - Validation of JSON responses to skip invalid location entries.
 *
 * @property client The [OkHttpClient] used to perform HTTP requests.
 */
class NominatimForwardGeocoder(client: OkHttpClient) :
    NominatimLocationRepository(client), LocationRepository {

  /**
   * Performs a forward geocoding request for the given [query].
   * - Trims and normalizes the query for caching.
   * - Returns cached results if available.
   * - Uses the Nominatim API to fetch results if not cached.
   * - Parses JSON responses and returns a list of valid [Location] objects.
   *
   * @param query The location name or address to geocode.
   * @return A list of [Location] matching the query.
   * @throws [LocationSearchException] If the request fails or response cannot be parsed.
   */
  override suspend fun forwardGeocode(query: String): List<Location> =
      withContext(ioDispatcher) {
        val normalizedQuery = query.trim().lowercase()
        cache[normalizedQuery]?.let {
          return@withContext it
        }

        rateLimiter.acquire()

        try {
          val url =
              HttpUrl.Builder()
                  .scheme("https")
                  .host(BASE_URL)
                  .addPathSegment("search")
                  .addQueryParameter("q", query)
                  .addQueryParameter("format", "json")
                  .addQueryParameter("addressdetails", "1")
                  .addQueryParameter("limit", "5")
                  .build()
                  .toString()

          val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()

          client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              Log.e("NominatimForward", "Request failed: ${response.code}")
              throw IOException("Unexpected code $response")
            }

            val body = response.body?.string() ?: throw IOException("Response body was null")
            val arr = JSONArray(body)
            val locations = jsonArrayToLocations(arr)

            cache[normalizedQuery] = locations
            return@withContext locations
          }
        } catch (e: Exception) {
          Log.e("NominatimForward", "Search failed: ${e.message}", e)
          throw LocationSearchException("Failed to forward geocode", e)
        }
      }

  /**
   * Converts a [JSONArray] from the Nominatim API response into a list of [Location] objects.
   *
   * Skips entries with missing or invalid `display_name`, `lat`, or `lon`.
   *
   * @param arr The JSON array returned by the Nominatim API.
   * @return A list of valid [Location] objects.
   */
  private fun jsonArrayToLocations(arr: JSONArray): List<Location> {
    val locations = mutableListOf<Location>()
    for (i in 0 until arr.length()) {
      val obj = arr.getJSONObject(i)

      val name = obj.optString("display_name", "")
      val lat = obj.optDouble("lat", Double.NaN)
      val lon = obj.optDouble("lon", Double.NaN)

      if (name == "" || lat.isNaN() || lon.isNaN()) {
        Log.w("NominatimRepo", "Skipping invalid location entry: $obj")
        continue
      }
      locations.add(Location(name = name, latitude = lat, longitude = lon))
    }
    return locations
  }

  /**
   * Reverse geocoding is not supported in this repository.
   *
   * @throws [UnsupportedOperationException] Always thrown since this repository only supports
   *   forward geocoding.
   */
  override suspend fun reverseGeocode(lat: Double, lon: Double): Location? {
    throw UnsupportedOperationException("Reverse geocoding not supported in this repository")
  }
}

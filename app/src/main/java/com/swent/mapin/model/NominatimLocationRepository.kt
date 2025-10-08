package com.swent.mapin.model

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.json.JSONArray

// Documentation and proofreading assisted by AI tools

/**
 * A repository implementation that uses the Nominatim API to search for geographic locations.
 *
 * This class performs network requests to the OpenStreetMap Nominatim service to retrieve location
 * data based on a query string. It includes basic caching and rate-limiting to reduce redundant
 * requests and avoid hitting API limits.
 *
 * @property client An instance of [OkHttpClient] used to perform HTTP requests.
 */
class NominatimLocationRepository(
    private val client: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocationRepository {
  companion object {
    private const val BASE_URL = "nominatim.openstreetmap.org"
    private const val USER_AGENT = "MapIn/1.0 (eloimarie.bernier@epfl.ch)"
    private const val MAX_CACHE_SIZE = 100
    private const val RATE_LIMIT_MS = 1000L
    private const val ADDRESS_DETAILS = "1"
    private const val RESULT_LIMIT = "5"
  }

  /**
   * A simple LRU cache to store recent search results. Limits to [MAX_CACHE_SIZE] entries and
   * evicts the oldest when full.
   */
  private val cache =
      object : LinkedHashMap<String, List<Location>>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, List<Location>>): Boolean {
          return size > MAX_CACHE_SIZE
        }
      }

  /** A rate limiter to ensure at most one request per [RATE_LIMIT_MS] milliseconds. */
  val rateLimiter = RateLimiter(RATE_LIMIT_MS)

  override suspend fun search(query: String): List<Location> =
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
                  .addQueryParameter("addressdetails", ADDRESS_DETAILS)
                  .addQueryParameter("limit", RESULT_LIMIT)
                  .build()
                  .toString()

          val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()

          client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              Log.e("NominatimRepo", "Request failed: ${response.code}")
              throw IOException("Unexpected code $response")
            }

            val body = response.body?.string() ?: throw IOException("Response body was null")
            val arr = JSONArray(body)
            val locations = jsonArrayToLocations(arr)

            cache[normalizedQuery] = locations
            return@withContext locations
          }
        } catch (e: Exception) {
          Log.e("NominatimRepo", "Search failed: ${e.message}", e)
          throw LocationSearchException("Failed to search locations", e)
        }
      }

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
}

/**
 * A simple rate limiter that ensures a minimum interval between actions.
 *
 * @property intervalMs The minimum interval in milliseconds between allowed actions.
 */
// Note: This RateLimiter is currently used in a single-threaded context.
// If reused across multiple coroutines or shared between components, consider adding thread safety
// (e.g., with Mutex).
class RateLimiter(private val intervalMs: Long) {
  private var lastTime = 0L

  /** Suspends the coroutine if the last action was too recent. */
  suspend fun acquire() {
    val now = System.currentTimeMillis()
    val waitTime = intervalMs - (now - lastTime)
    if (waitTime > 0) delay(waitTime)
    lastTime = System.currentTimeMillis()
  }
}

class LocationSearchException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

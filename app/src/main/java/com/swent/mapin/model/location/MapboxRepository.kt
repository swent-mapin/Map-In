package com.swent.mapin.model.location

import android.util.Log
import com.swent.mapin.model.dispatcher.CoroutineDispatcherProvider
import com.swent.mapin.model.dispatcher.DefaultDispatcherProvider
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// Assisted by AI

/**
 * Implementation of [LocationRepository] using the Mapbox Geocoding API. Features: consolidated
 * Forward and Reverse geocoding, in-memory LRU Caching.
 *
 * @property client The OkHttpClient for network requests.
 * @property token The Mapbox Access Token required for API authentication.
 */
class MapboxRepository(
    private val client: OkHttpClient,
    private val token: String,
    private val dispatcherProvider: CoroutineDispatcherProvider = DefaultDispatcherProvider()
) : LocationRepository {

  companion object {
    private const val BASE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places"
    private const val MAX_CACHE_SIZE = 100
  }

  private val cache =
      object : LinkedHashMap<String, List<Location>>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<Location>>
        ): Boolean {
          return size > MAX_CACHE_SIZE
        }
      }

  private val requestMutexes = ConcurrentHashMap<String, Mutex>()

  /**
   * Performs forward geocoding (text to coordinates) using Mapbox. Checks the local cache before
   * making a network request.
   */
  override suspend fun forwardGeocode(query: String): List<Location> =
      withContext(dispatcherProvider.io) {
        val normalizedQuery = query.trim().lowercase()

        // Get or create a mutex for this query
        val mutex = requestMutexes.getOrPut(normalizedQuery) { Mutex() }

        mutex.withLock {
          // Double-check cache inside the lock
          synchronized(cache) {
            cache[normalizedQuery]?.let {
              return@withContext it
            }
          }

          // Prepare Request
          val url = "$BASE_URL/${query}.json?access_token=$token&limit=5&autocomplete=true"
          val request = Request.Builder().url(url).build()

          try {
            client.newCall(request).execute().use { response ->
              if (!response.isSuccessful) {
                Log.e("MapboxRepo", "Forward geocode failed: ${response.code}")
                throw LocationSearchException("Mapbox API Error: ${response.code}")
              }

              val responseBody =
                  response.body?.string()
                      ?: throw LocationSearchException("Mapbox API Error: Empty response")

              // Parse JSON (Native Android)
              val results = parseLocationResponse(responseBody)

              // Update Cache
              synchronized(cache) { cache[normalizedQuery] = results }

              return@withContext results
            }
          } catch (e: Exception) {
            Log.e("MapboxRepo", "Network error during forward geocoding", e)
            throw LocationSearchException("Network error", e)
          } finally {
            requestMutexes.remove(normalizedQuery)
          }
        }
      }

  private fun parseLocationResponse(responseBody: String): List<Location> {
    val json = JSONObject(responseBody)
    val features = json.optJSONArray("features") ?: return emptyList()
    val results = mutableListOf<Location>()

    for (i in 0 until features.length()) {
      val feature = features.getJSONObject(i)
      val placeName = feature.optString("place_name", "Unknown")
      val center = feature.optJSONArray("center")

      if (center != null && center.length() >= 2) {
        val lon = center.getDouble(0)
        val lat = center.getDouble(1)
        results.add(Location.from(placeName, lat, lon))
      }
    }

    return results
  }

  /** Performs reverse geocoding (coordinates to address/name). */
  override suspend fun reverseGeocode(lat: Double, lon: Double): Location =
      withContext(dispatcherProvider.io) {
        val url = "$BASE_URL/$lon,$lat.json?access_token=$token&limit=1"
        val request = Request.Builder().url(url).build()

        try {
          client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              Log.e("MapboxRepo", "Reverse geocode failed: ${response.code}")
              throw LocationSearchException("Mapbox API Error: ${response.code}")
            }

            val responseBody =
                response.body?.string()
                    ?: throw LocationSearchException("Mapbox API Error: Empty response")
            val json = JSONObject(responseBody)
            val features = json.optJSONArray("features")

            if (features != null && features.length() > 0) {
              val feature = features.getJSONObject(0)
              val placeName = feature.optString("place_name", Location.NO_NAME)
              val center = feature.optJSONArray("center")

              if (center != null && center.length() >= 2) {
                val finalLon = center.getDouble(0)
                val finalLat = center.getDouble(1)
                return@withContext Location.from(placeName, finalLat, finalLon)
              }
            }
            return@withContext Location.UNDEFINED
          }
        } catch (e: Exception) {
          Log.e("MapboxRepo", "Network error during reverse geocoding", e)
          throw LocationSearchException("Network error", e)
        }
      }
}

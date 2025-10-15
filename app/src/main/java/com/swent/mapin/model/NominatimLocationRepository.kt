package com.swent.mapin.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient

/**
 * Base repository for Nominatim geocoding operations.
 *
 * Provides shared functionality for forward and reverse geocoding, including:
 * - HTTP client management
 * - Rate limiting to respect API usage
 * - Caching of recent queries using a simple LRU cache
 *
 * Subclasses should implement specific geocoding methods (forwardGeocode, reverseGeocode) according
 * to the repository type (forward or reverse).
 *
 * @property client The [OkHttpClient] used to perform HTTP requests.
 * @property ioDispatcher The [CoroutineDispatcher] for executing network and I/O operations.
 *   Defaults to [Dispatchers.IO].
 */
abstract class NominatimLocationRepository(
    protected val client: OkHttpClient,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  companion object {
    const val BASE_URL = "nominatim.openstreetmap.org"
    const val USER_AGENT = "MapIn/1.0 (eloimarie.bernier@epfl.ch)"
    const val MAX_CACHE_SIZE = 100
    const val RATE_LIMIT_MS = 1000L
  }

  /** Rate limiter to ensure a minimum interval between requests. */
  protected val rateLimiter = RateLimiter(RATE_LIMIT_MS)

  /** Simple Least Recently Used (LRU) cache for storing recent query results. */
  protected val cache =
      object : LinkedHashMap<String, List<Location>>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<Location>>
        ): Boolean {
          return size > MAX_CACHE_SIZE
        }
      }
}

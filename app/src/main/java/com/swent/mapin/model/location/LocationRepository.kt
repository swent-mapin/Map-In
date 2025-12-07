package com.swent.mapin.model.location

import com.swent.mapin.model.Location

/**
 * Interface defining the contract for geocoding operations. It abstracts the underlying API
 * (Mapbox, Nominatim, etc.).
 */
interface LocationRepository {
  /**
   * Converts a text query (address or place name) into a list of locations suggestions.
   *
   * @param query The search string (e.g., "Lausanne").
   * @return A list of matching [Location] objects.
   */
  suspend fun forwardGeocode(query: String): List<Location>

  /**
   * Converts coordinates into a location object with an address/name.
   *
   * @param lat The latitude.
   * @param lon The longitude.
   * @return The matching [Location], or null if no result is found.
   */
  suspend fun reverseGeocode(lat: Double, lon: Double): Location?
}

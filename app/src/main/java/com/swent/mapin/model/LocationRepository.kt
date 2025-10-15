package com.swent.mapin.model

/** Interface for geocoding repositories. */
interface LocationRepository {
  /**
   * Forward geocoding: address → coordinates
   *
   * @param query The address or place name to geocode.
   * @return A list of matching locations.
   */
  suspend fun forwardGeocode(query: String): List<Location>

  /**
   * Reverse geocoding: coordinates → address
   *
   * @param lat The latitude of the location.
   * @param lon The longitude of the location.
   * @return The matching location, or null if none found.
   */
  suspend fun reverseGeocode(lat: Double, lon: Double): Location?
}

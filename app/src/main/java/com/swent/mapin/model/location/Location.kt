package com.swent.mapin.model.location

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation

// Assisted by AI

/**
 * Represents a geographical point with an associated Geohash. Fields are nullable to allow for an
 * UNDEFINED state (Null Object Pattern).
 *
 * @property name The human-readable name of the place (e.g., "Eiffel Tower").
 * @property latitude The latitude in degrees.
 * @property longitude The longitude in degrees.
 * @property geohash The calculated hash for proximity searches and indexing.
 */
data class Location(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null
) {
  /**
   * Checks if the location is fully defined.
   *
   * @return True if latitude, longitude, and geohash are non-null; false otherwise.
   */
  fun isDefined(): Boolean {
    return latitude != null && longitude != null && geohash != null
  }

  companion object {
    /** Represents an undefined location state. */
    val UNDEFINED = Location(name = null, latitude = null, longitude = null, geohash = null)
    const val NO_NAME = "Unknown Location"

    /**
     * Factory method to create a Location instance. Automatically calculates the [geohash] using
     * GeoFireUtils based on the coordinates.
     *
     * @param name The name of the location.
     * @param lat The latitude.
     * @param lng The longitude.
     * @return A complete [Location] object.
     */
    fun from(name: String?, lat: Double?, lng: Double?): Location {
      if (name == null || lat == null || lng == null) return UNDEFINED
      val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(lat, lng))
      return Location(name, lat, lng, hash)
    }
  }
}

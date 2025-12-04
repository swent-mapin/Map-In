package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.firestore.GeoPoint
import com.swent.mapin.model.Location
import com.swent.mapin.util.EventUtils

/**
 * Implementation of DistanceCalculator that uses the Haversine formula.
 *
 * This implementation delegates to EventUtils.calculateHaversineDistance for the actual
 * calculation.
 */
class HaversineDistanceCalculator : DistanceCalculator {
  /**
   * Calculates the Haversine distance between two locations.
   *
   * @param from The starting location
   * @param to The destination location
   * @return The distance in kilometers, or null if either location has invalid coordinates (0.0,
   *   0.0)
   */
  override fun distanceKm(from: Location, to: Location): Double? {
    // Skip calculation if either location has default/invalid coordinates
    if (isInvalidLocation(from) || isInvalidLocation(to)) {
      return null
    }

    val fromGeoPoint = GeoPoint(from.latitude, from.longitude)
    val toGeoPoint = GeoPoint(to.latitude, to.longitude)

    return EventUtils.calculateHaversineDistance(fromGeoPoint, toGeoPoint)
  }

  /**
   * Checks if a location has invalid coordinates.
   *
   * A location is considered invalid if both latitude and longitude are 0.0, which is often used as
   * a placeholder.
   */
  private fun isInvalidLocation(location: Location): Boolean {
    return location.latitude == 0.0 && location.longitude == 0.0
  }
}

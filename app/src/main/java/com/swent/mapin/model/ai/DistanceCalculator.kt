package com.swent.mapin.model.ai

// Assisted by AI

import com.swent.mapin.model.Location

/**
 * Interface for calculating the distance between two geographical locations.
 *
 * This abstraction allows the AI candidate selector to compute distances without depending on a
 * specific implementation (Haversine, Google Maps API, etc.).
 */
interface DistanceCalculator {
  /**
   * Calculates the distance in kilometers between two locations.
   *
   * @param from The starting location
   * @param to The destination location
   * @return The distance in kilometers, or null if the calculation is not possible (e.g., invalid
   *   coordinates)
   */
  fun distanceKm(from: Location, to: Location): Double?
}

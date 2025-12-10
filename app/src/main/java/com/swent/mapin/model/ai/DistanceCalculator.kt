package com.swent.mapin.model.ai

// Assisted by AI

import com.swent.mapin.model.location.Location
import com.google.firebase.firestore.GeoPoint
import com.swent.mapin.util.EventUtils

/**
 * Calculator for geographical distance between locations using Haversine formula.
 *
 * This class delegates to EventUtils.calculateHaversineDistance for the actual calculation.
 */
open class DistanceCalculator {
  /**
   * Calculates the Haversine distance between two locations.
   *
   * @param from The starting location
   * @param to The destination location
   * @return The distance in kilometers, or null if coordinates are invalid
   */
  open fun distanceKm(from: Location, to: Location): Double? {
    if (from.isDefined() && to.isDefined()) {
      val fromGeoPoint = GeoPoint(from.latitude!!, from.longitude!!)
      val toGeoPoint = GeoPoint(to.latitude!!, to.longitude!!)

      return EventUtils.calculateHaversineDistance(fromGeoPoint, toGeoPoint)
    }
    return null
  }
}

package com.swent.mapin.util

import com.google.firebase.firestore.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Utility object for event-related calculations. */
object EventUtils {
  /**
   * Calculates the Haversine distance between two geographical points.
   *
   * @param geoPoint1 The first geographical point.
   * @param geoPoint2 The second geographical point.
   * @return The distance between the two points in kilometers.
   */
  fun calculateHaversineDistance(geoPoint1: GeoPoint, geoPoint2: GeoPoint): Double {
    val lat1 = Math.toRadians(geoPoint1.latitude)
    val lon1 = Math.toRadians(geoPoint1.longitude)
    val lat2 = Math.toRadians(geoPoint2.latitude)
    val lon2 = Math.toRadians(geoPoint2.longitude)
    val dLat = lat2 - lat1
    val dLon = lon2 - lon1
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    val r = 6371.0
    return c * r
  }
}

package com.swent.mapin.ui.map.offline

import com.mapbox.geojson.Point
import kotlin.math.cos
import kotlin.math.pow

/**
 * Utility for calculating viewport bounds from camera state.
 *
 * Provides methods to estimate geographic coordinate bounds based on camera center position and
 * zoom level.
 */
object ViewportBoundsCalculator {

  /** Earth's radius in kilometers */
  private const val EARTH_RADIUS_KM = 6371.0

  /** Tile size in pixels at zoom 0 */
  private const val TILE_SIZE_AT_ZOOM_0 = 256.0

  /**
   * Calculates approximate coordinate bounds for the current viewport.
   *
   * Uses the camera center point and zoom level to estimate the visible geographic area. The
   * calculation accounts for latitude-based distortion using Mercator projection properties.
   *
   * @param center The camera center point (latitude, longitude)
   * @param zoom The current zoom level
   * @param widthPx Viewport width in pixels
   * @param heightPx Viewport height in pixels
   * @return CoordinateBounds representing the visible area
   */
  fun calculateBounds(center: Point, zoom: Double, widthPx: Int, heightPx: Int): CoordinateBounds {
    val lat = center.latitude()
    val lng = center.longitude()

    // Calculate meters per pixel at this zoom and latitude
    val metersPerPixel = calculateMetersPerPixel(lat, zoom)

    // Calculate offset in meters
    val halfWidthMeters = (widthPx / 2.0) * metersPerPixel
    val halfHeightMeters = (heightPx / 2.0) * metersPerPixel

    // Convert meters to degrees
    // At latitude, 1 degree longitude = 111.32km * cos(latitude)
    // At any latitude, 1 degree latitude ≈ 111.32km
    val latDegreesPerMeter = 1.0 / 111320.0
    val lngDegreesPerMeter = 1.0 / (111320.0 * cos(Math.toRadians(lat)))

    val latOffset = halfHeightMeters * latDegreesPerMeter
    val lngOffset = halfWidthMeters * lngDegreesPerMeter

    // Calculate bounds
    val swLat = lat - latOffset
    val swLng = lng - lngOffset
    val neLat = lat + latOffset
    val neLng = lng + lngOffset

    val southwest = Point.fromLngLat(swLng, swLat)
    val northeast = Point.fromLngLat(neLng, neLat)

    return CoordinateBounds(southwest, northeast)
  }

  /**
   * Calculates meters per pixel at a given latitude and zoom level.
   *
   * Uses Mercator projection formula to account for latitude-based scale distortion.
   *
   * @param latitude The latitude in degrees
   * @param zoom The zoom level
   * @return Meters per pixel at this location and zoom
   */
  private fun calculateMetersPerPixel(latitude: Double, zoom: Double): Double {
    // Circumference of Earth at this latitude
    val latitudeRadians = Math.toRadians(latitude)
    val circumference = 2 * Math.PI * EARTH_RADIUS_KM * 1000 * cos(latitudeRadians)

    // Total pixels at this zoom level
    val totalPixels = TILE_SIZE_AT_ZOOM_0 * 2.0.pow(zoom)

    return circumference / totalPixels
  }
}

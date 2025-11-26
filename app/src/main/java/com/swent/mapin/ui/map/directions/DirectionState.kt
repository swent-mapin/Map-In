package com.swent.mapin.ui.map.directions

// Assisted by AI

import com.mapbox.geojson.Point

/**
 * Sealed class representing the different states of direction display on the map.
 *
 * This class is used to manage the UI state for showing routes between the user's location and
 * event destinations. It supports three states: cleared, loading, and displayed.
 */
sealed class DirectionState {
  /** Initial state when no direction is being displayed on the map. */
  object Cleared : DirectionState()

  /**
   * Transient state while direction data is being fetched. Typically shown during API calls or
   * route calculation.
   */
  object Loading : DirectionState()

  /**
   * Active state when a route is displayed on the map.
   *
   * @property routePoints Ordered list of geographic points forming the route path
   * @property origin Starting point of the route (typically user's location)
   * @property destination End point of the route (typically event location)
   * @property routeInfo Information about route distance and duration
   */
  data class Displayed(
      val routePoints: List<Point>,
      val origin: Point,
      val destination: Point,
      val routeInfo: RouteInfo = RouteInfo.ZERO
  ) : DirectionState()
}

/**
 * Data class containing essential direction information.
 *
 * @property origin Starting point of the direction
 * @property destination End point of the direction
 */
data class DirectionInfo(val origin: Point, val destination: Point)

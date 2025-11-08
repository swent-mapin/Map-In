package com.swent.mapin.ui.map.directions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing direction display state on the map.
 *
 * This ViewModel handles the lifecycle and state transitions for showing routes between the user's
 * location and event destinations. It follows MVVM architecture principles, exposing an observable
 * state that the UI layer can react to.
 *
 * Current implementation uses a mock straight-line route. Future integration with Mapbox Directions
 * API will provide real routing data along roads and paths.
 *
 * @see DirectionState for possible states
 */
class DirectionViewModel : ViewModel() {

  private var _directionState by mutableStateOf<DirectionState>(DirectionState.Cleared)

  /** Current state of direction display. Observable by the UI to react to state changes. */
  val directionState: DirectionState
    get() = _directionState

  /**
   * Requests directions from origin to destination.
   *
   * Initiates an asynchronous operation that:
   * 1. Sets state to Loading
   * 2. Simulates API call with 500ms delay
   * 3. Generates route points (currently straight line, will be replaced with API data)
   * 4. Sets state to Displayed with route information
   *
   * @param origin Starting point (typically user's current location)
   * @param destination End point (typically event location)
   */
  fun requestDirections(origin: Point, destination: Point) {
    viewModelScope.launch {
      _directionState = DirectionState.Loading
      delay(500)

      val routePoints = createStraightLineRoute(origin, destination)
      _directionState =
          DirectionState.Displayed(
              routePoints = routePoints, origin = origin, destination = destination)
    }
  }

  /** Clears the currently displayed direction from the map. Resets the state to Cleared. */
  fun clearDirection() {
    _directionState = DirectionState.Cleared
  }

  /**
   * Creates a straight line route between two geographic points.
   *
   * This is a temporary implementation that generates 11 evenly-spaced points between the start and
   * end locations. This provides smooth line rendering while maintaining simplicity.
   *
   * @param start Starting geographic point
   * @param end Ending geographic point
   * @return List of 11 points forming a straight line from start to end
   * @note This will be replaced with Mapbox Directions API integration that provides actual
   *   road-following routes.
   */
  private fun createStraightLineRoute(start: Point, end: Point): List<Point> {
    val points = mutableListOf<Point>()
    val steps = 10

    for (i in 0..steps) {
      val fraction = i.toDouble() / steps
      val lat = start.latitude() + (end.latitude() - start.latitude()) * fraction
      val lng = start.longitude() + (end.longitude() - start.longitude()) * fraction
      points.add(Point.fromLngLat(lng, lat))
    }

    return points
  }
}

package com.swent.mapin.ui.map.directions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing direction display state on the map.
 *
 * This ViewModel handles the lifecycle and state transitions for showing routes between the user's
 * location and event destinations. It follows MVVM architecture principles, exposing an observable
 * state that the UI layer can react to.
 *
 * Uses the Mapbox Directions API to fetch walking routes between two points.
 *
 * @property directionsService Service for fetching directions from Mapbox API
 * @see DirectionState for possible states
 */
class DirectionViewModel(private val directionsService: MapboxDirectionsService? = null) :
    ViewModel() {

  private var _directionState by mutableStateOf<DirectionState>(DirectionState.Cleared)

  /** Current state of direction display. Observable by the UI to react to state changes. */
  val directionState: DirectionState
    get() = _directionState

  /**
   * Requests walking directions from origin to destination.
   *
   * Initiates an asynchronous operation that:
   * 1. Sets state to Loading
   * 2. Fetches route from Mapbox Directions API
   * 3. Sets state to Displayed with route information on success, or Cleared on failure
   *
   * @param origin Starting point (typically user's current location)
   * @param destination End point (typically event location)
   */
  fun requestDirections(origin: Point, destination: Point) {
    viewModelScope.launch {
      _directionState = DirectionState.Loading

      val routePoints = directionsService?.getDirections(origin, destination)

      if (routePoints != null && routePoints.isNotEmpty()) {
        _directionState =
            DirectionState.Displayed(
                routePoints = routePoints, origin = origin, destination = destination)
      } else {
        _directionState = DirectionState.Cleared
      }
    }
  }

  /** Clears the currently displayed direction from the map. Resets the state to Cleared. */
  fun clearDirection() {
    _directionState = DirectionState.Cleared
  }
}

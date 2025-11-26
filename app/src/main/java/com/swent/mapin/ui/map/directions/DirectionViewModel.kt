package com.swent.mapin.ui.map.directions

//Assisted by AI

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing direction display state on the map.
 *
 * This ViewModel handles the lifecycle and state transitions for showing routes between the user's
 * location and event destinations. It follows MVVM architecture principles, exposing an observable
 * state that the UI layer can react to.
 *
 * Uses the Mapbox Directions API to fetch walking routes between two points. Supports live tracking
 * to update routes as the user moves.
 *
 * @property directionsService Service for fetching directions from Mapbox API
 * @property currentTimeMillis Function to get current time in milliseconds (injectable for testing)
 * @property minDistanceThreshold Minimum distance in meters user must move before route updates
 * @property minTimeThreshold Minimum time in milliseconds that must pass before route updates
 * @see DirectionState for possible states
 */
class DirectionViewModel(
    private val directionsService: MapboxDirectionsService? = null,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
    private val minDistanceThreshold: Float = 10.0f,
    private val minTimeThreshold: Long = 10000L
) : ViewModel() {

  private var _directionState by mutableStateOf<DirectionState>(DirectionState.Cleared)

  /** Current state of direction display. Observable by the UI to react to state changes. */
  val directionState: DirectionState
    get() = _directionState

  private var lastUpdateLocation: Location? = null
  private var lastUpdateTime: Long = 0L
  private var rateLimitJob: Job? = null

  private val rateLimitDelayMillis = 1000L

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
   * @param currentLocation Optional current location to initialize tracking state
   */
  fun requestDirections(origin: Point, destination: Point, currentLocation: Location? = null) {
    viewModelScope.launch {
      _directionState = DirectionState.Loading

      val result = directionsService?.getDirections(origin, destination)

      if (result != null && result.routePoints.isNotEmpty()) {
        _directionState =
            DirectionState.Displayed(
                routePoints = result.routePoints,
                origin = origin,
                destination = destination,
                routeInfo = result.routeInfo)

        if (currentLocation != null) {
          lastUpdateLocation = currentLocation
          lastUpdateTime = currentTimeMillis()
        }
      } else {
        _directionState = DirectionState.Cleared
      }
    }
  }

  /**
   * Updates the route based on user's location change.
   *
   * Only triggers a route update if:
   * - Directions are currently displayed
   * - User has moved at least minDistanceThreshold meters
   * - At least minTimeThreshold milliseconds have passed since last update
   *
   * @param location User's current location
   */
  fun onLocationUpdate(location: Location) {
    val currentState = _directionState
    if (currentState !is DirectionState.Displayed) {
      return
    }

    val currentTime = currentTimeMillis()
    val lastLoc = lastUpdateLocation

    val shouldUpdate =
        if (lastLoc == null) {
          true
        } else {
          val distance = lastLoc.distanceTo(location)
          val timePassed = currentTime - lastUpdateTime
          distance >= minDistanceThreshold && timePassed >= minTimeThreshold
        }

    if (shouldUpdate && rateLimitJob == null) {
      rateLimitJob =
          viewModelScope.launch {
            delay(rateLimitDelayMillis)

            // Re-check state after delay to avoid race conditions
            val state = _directionState
            if (state !is DirectionState.Displayed) {
              rateLimitJob = null
              return@launch
            }

            val newOrigin = Point.fromLngLat(location.longitude, location.latitude)
            val result = directionsService?.getDirections(newOrigin, state.destination)

            if (result != null && result.routePoints.isNotEmpty()) {
              _directionState =
                  DirectionState.Displayed(
                      routePoints = result.routePoints,
                      origin = newOrigin,
                      destination = state.destination,
                      routeInfo = result.routeInfo)
              lastUpdateLocation = location
              lastUpdateTime = currentTimeMillis()
            }

            rateLimitJob = null
          }
    }
  }

  /** Clears the currently displayed direction from the map. Resets the state to Cleared. */
  fun clearDirection() {
    _directionState = DirectionState.Cleared
    lastUpdateLocation = null
    lastUpdateTime = 0L
    rateLimitJob?.cancel()
    rateLimitJob = null
  }
}

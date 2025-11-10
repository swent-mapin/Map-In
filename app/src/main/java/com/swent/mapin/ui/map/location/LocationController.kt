package com.swent.mapin.ui.map.location

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Controller managing location state and operations for the map screen.
 *
 * Handles:
 * - Location permission checking
 * - Location updates via LocationManager
 * - User location centering state
 * - Callbacks for UI interactions (center camera, request permission)
 */
class LocationController(
    private val locationManager: LocationManager,
    private val scope: CoroutineScope,
    private val setErrorMessage: (String) -> Unit
) {

  private var _currentLocation by mutableStateOf<Location?>(null)
  val currentLocation: Location?
    get() = _currentLocation

  private var _hasLocationPermission by mutableStateOf(false)
  val hasLocationPermission: Boolean
    get() = _hasLocationPermission

  private var _locationBearing by mutableStateOf(0f)
  val locationBearing: Float
    get() = _locationBearing

  private var _isCenteredOnUser by mutableStateOf(false)
  val isCenteredOnUser: Boolean
    get() = _isCenteredOnUser

  var onCenterOnUserLocation: (() -> Unit)? = null
  var onRequestLocationPermission: (() -> Unit)? = null

  /** Checks and updates the location permission status. */
  fun checkLocationPermission() {
    _hasLocationPermission = locationManager.hasLocationPermission()
  }

  /** Starts listening to location updates if permission is granted. */
  fun startLocationUpdates() {
    if (!locationManager.hasLocationPermission()) {
      _hasLocationPermission = false
      return
    }

    _hasLocationPermission = true

    scope.launch {
      locationManager
          .getLocationUpdates()
          .catch { e ->
            android.util.Log.e("LocationController", "Error getting location updates", e)
            setErrorMessage("Failed to get location updates")
          }
          .collect { location ->
            _currentLocation = location
            if (location.hasBearing()) {
              _locationBearing = location.bearing
            }
          }
    }
  }

  /** Gets the last known location and optionally centers the camera on it. */
  fun getLastKnownLocation(centerCamera: Boolean = false) {
    locationManager.getLastKnownLocation(
        onSuccess = { location ->
          _currentLocation = location
          if (location.hasBearing()) {
            _locationBearing = location.bearing
          }
          if (centerCamera) {
            onCenterOnUserLocation?.invoke()
          }
        },
        onError = { android.util.Log.w("LocationController", "Could not get last known location") })
  }

  /**
   * Handles the location button click. If permission is granted, centers on user location.
   * Otherwise, requests permission.
   */
  fun onLocationButtonClick() {
    if (locationManager.hasLocationPermission()) {
      _isCenteredOnUser = true
      onCenterOnUserLocation?.invoke()
    } else {
      onRequestLocationPermission?.invoke()
    }
  }

  /**
   * Updates the centered state based on camera position. Call this when the camera moves to check
   * if still centered on user.
   */
  fun updateCenteredState(cameraLat: Double, cameraLon: Double) {
    val userLoc = _currentLocation
    if (userLoc == null) {
      _isCenteredOnUser = false
      return
    }

    // Check if camera is close enough to user location (within ~50 meters)
    val latDiff = abs(cameraLat - userLoc.latitude)
    val lonDiff = abs(cameraLon - userLoc.longitude)
    val threshold = 0.0005

    _isCenteredOnUser = latDiff < threshold && lonDiff < threshold
  }

  /**
   * Manually marks that the camera is no longer centered on the user. Call this when user manually
   * moves the map.
   */
  fun onMapMoved() {
    _isCenteredOnUser = false
  }
}

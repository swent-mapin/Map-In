package com.swent.mapin.ui.map.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manager for handling user location updates and permissions.
 *
 * Provides a flow-based API for location updates and checks for location permissions.
 */
class LocationManager(private val context: Context) {
  private val fusedLocationClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(context)

  /**
   * Checks if location permissions are granted.
   *
   * @return true if both FINE and COARSE location permissions are granted
   */
  fun hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
  }

  /**
   * Creates a Flow that emits location updates.
   *
   * Automatically handles lifecycle by removing location updates when the flow is cancelled.
   *
   * @return Flow of Location objects
   * @throws SecurityException if location permissions are not granted
   */
  @Suppress("MissingPermission")
  fun getLocationUpdates(): Flow<Location> = callbackFlow {
    if (!hasLocationPermission()) {
      close(SecurityException("Location permission not granted"))
      return@callbackFlow
    }

    val locationRequest =
        LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000L // 10 seconds
                )
            .apply {
              setMinUpdateIntervalMillis(5000L) // 5 seconds
              setMaxUpdateDelayMillis(15000L) // 15 seconds
            }
            .build()

    val locationCallback =
        object : LocationCallback() {
          override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location -> trySend(location) }
          }
        }

    try {
      fusedLocationClient.requestLocationUpdates(
          locationRequest, locationCallback, Looper.getMainLooper())
    } catch (e: SecurityException) {
      close(e)
    }

    awaitClose { fusedLocationClient.removeLocationUpdates(locationCallback) }
  }

  /**
   * Gets the last known location immediately.
   *
   * @param onSuccess Callback invoked with the location if available
   * @param onError Callback invoked if location cannot be retrieved
   */
  @Suppress("MissingPermission")
  fun getLastKnownLocation(onSuccess: (Location) -> Unit, onError: () -> Unit) {
    if (!hasLocationPermission()) {
      onError()
      return
    }

    try {
      fusedLocationClient.lastLocation
          .addOnSuccessListener { location: Location? ->
            if (location != null) {
              onSuccess(location)
            } else {
              onError()
            }
          }
          .addOnFailureListener { onError() }
    } catch (e: SecurityException) {
      onError()
    }
  }
}

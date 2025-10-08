package com.swent.mapin.map

/*
 * This file was partially written with the assistance of AI tools (programming assistant).
 * Reviewed and adjusted by the Map'In team.
 */

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Main map screen composable.
 *
 * Responsibilities:
 * - Obtain the initial camera position from the ViewModel.
 * - Configure and render a GoogleMap with basic UI settings.
 *
 * Notes:
 * - No markers or overlays are rendered yet.
 * - This composable can be used from your app UI (e.g., MainActivity).
 */
@Composable
fun MapScreen(
    // By default, get a MapViewModel scoped to the current UI.
    viewModel: MapViewModel = viewModel()
) {
    // Remember the camera state across recompositions.
    // Initialize with the ViewModel-provided position and a zoom level of 12f.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(viewModel.initialCamera, 12f)
    }

    // GoogleMap composable:
    // - properties: immutable map options (none for now).
    // - uiSettings: enable/disable built-in controls and gestures.
    // - cameraPositionState: binds camera state to Compose.
    GoogleMap(
        properties = MapProperties(),
        uiSettings = MapUiSettings(
            compassEnabled = true,           // Show compass when the map is rotated.
            myLocationButtonEnabled = false, // "My location" button disabled (no permissions/logic here).
            zoomControlsEnabled = false,     // Hide native zoom buttons (gestures are enough).
            zoomGesturesEnabled = true,      // Pinch to zoom.
            scrollGesturesEnabled = true,    // Pan the map.
            rotationGesturesEnabled = true,  // Two-finger rotate.
            tiltGesturesEnabled = true       // Two-finger tilt.
        ),
        cameraPositionState = cameraPositionState
    ) {
        // Map content slot.
        // Add markers, polylines, overlays, etc. here when needed.
    }
}

package com.swent.mapin.map

/*
 * This file was partially written with the assistance of AI tools (programming assistant).
 * Reviewed and adjusted by the Map'In team.
 */

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng

/**
 * MapScreen
 *
 * Responsibilities:
 * - Owns the Google Map UI and reads all map data from the ViewModel.
 * - Renders two heatmap layers:
 *   1) A low-contrast "historical" background (irregular clouds).
 *   2) A foreground heatmap of current events.
 * - Displays markers for each event with a title and a snippet showing the attendees count.
 *
 * Performance notes:
 * - `remember(...)` is used to avoid rebuilding heavy providers (heatmaps) unless inputs change.
 * - `collectAsState()` subscribes to the events StateFlow and triggers recomposition when updated.
 * - The map camera state is remembered to keep position/zoom across recompositions.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    // Camera is initialized once with the VM-provided position and kept stable across recompositions.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(viewModel.initialCamera, 12f)
    }

    // Reactive events list (markers + foreground heatmap source).
    val events by viewModel.events.collectAsState()

    // Convert events to weighted points for the heatmap.
    // Weights are normalized to [0, 1] using the current maximum attendees.
    val weightedData = remember(events) {
        val max = (events.maxOfOrNull { it.attendees } ?: 1).coerceAtLeast(1)
        events.map { e ->
            val weight = e.attendees.toDouble() / max.toDouble()
            WeightedLatLng(LatLng(e.latitude, e.longitude), weight)
        }
    }

    // Foreground heatmap (current activity).
    // Radius is in screen pixels (max 50). Larger -> smoother, wider influence.
    val heatmapProvider = remember(weightedData) {
        HeatmapTileProvider.Builder()
            .weightedData(weightedData)
            .radius(50)
            .build()
    }

    // Background heatmap ("historical clouds"):
    // - Custom blue gradient with transparency at low intensity.
    // - Uses the precomputed point cloud from the ViewModel (deterministic shapes).
    val historicalGradient = remember {
        val colors = intArrayOf(
            Color.argb(0,   33, 150, 243),   // fully transparent at low density
            Color.argb(128, 33, 150, 243),   // mid transparency
            Color.argb(255, 25, 118, 210)    // opaque at high density
        )
        val startPoints = floatArrayOf(0.2f, 0.6f, 1f)
        Gradient(colors, startPoints)
    }
    val historicalProvider = remember(viewModel.historicalCloud) {
        HeatmapTileProvider.Builder()
            .weightedData(viewModel.historicalCloud)
            .radius(50)
            .gradient(historicalGradient)
            .build()
    }

    // GoogleMap container:
    // - MapProperties: immutable map options (kept default here).
    // - MapUiSettings: toggles built-in UI features and gestures.
    // - cameraPositionState: binds Compose state to the map camera.
    GoogleMap(
        properties = MapProperties(),
        uiSettings = MapUiSettings(
            compassEnabled = true,
            myLocationButtonEnabled = false, // no location permission/logic here
            zoomControlsEnabled = false,     // gestures provide zoom
            zoomGesturesEnabled = true,
            scrollGesturesEnabled = true,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true
        ),
        cameraPositionState = cameraPositionState
    ) {
        // Layer 0: historical background (lower zIndex).
        // Higher transparency so it stays subtle behind the main data.
        TileOverlay(
            tileProvider = historicalProvider,
            transparency = 0.38f,
            zIndex = 0f
        )

        // Layer 1: current activity (foreground).
        // Slight transparency to keep base map readable.
        TileOverlay(
            tileProvider = heatmapProvider,
            transparency = 0.15f,
            zIndex = 1f
        )

        // Event markers:
        // - Title shows the event name.
        // - Snippet uses the required "attendees" wording.
        events.forEach { e ->
            Marker(
                state = rememberMarkerState(position = LatLng(e.latitude, e.longitude)),
                title = e.name,
                snippet = "${e.attendees} attendees"
            )
        }
    }
}

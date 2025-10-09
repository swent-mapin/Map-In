package com.swent.mapin.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig

// Assisted by AI

/**
 * Main map screen without any "historical cloud" logic.
 * - Shows event markers and a live heatmap.
 * - Coordinates with a bottom sheet and blocks map gestures when fully expanded.
 */
@Composable
fun MapScreen() {
  // Compute sheet sizes from the current screen height
  val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
  val sheetConfig =
      BottomSheetConfig(
          collapsedHeight = MapConstants.COLLAPSED_HEIGHT,
          mediumHeight = MapConstants.MEDIUM_HEIGHT,
          fullHeight = screenHeightDp * MapConstants.FULL_HEIGHT_PERCENTAGE)

  // ViewModel holding map state, events and bottom sheet state
  val viewModel = rememberMapScreenViewModel(sheetConfig)

  // Initial camera position
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(viewModel.initialCamera, MapConstants.DEFAULT_ZOOM)
  }

  // When sheet reaches MEDIUM, remember the reference zoom level
  LaunchedEffect(viewModel.bottomSheetState) {
    if (viewModel.bottomSheetState == BottomSheetState.MEDIUM) {
      viewModel.updateMediumReferenceZoom(cameraPositionState.position.zoom)
    }
  }

  // Collapse the sheet when user zooms the map significantly
  LaunchedEffect(cameraPositionState.position.zoom) {
    if (viewModel.checkZoomInteraction(cameraPositionState.position.zoom)) {
      viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    }
  }

  // Density and geometry used to detect touches near the sheet top edge
  val density = LocalDensity.current
  val densityDpi = remember(density) { density.density.toInt() * 160 }
  val screenHeightPx = remember(screenHeightDp, density) { with(density) { screenHeightDp.toPx() } }
  val sheetTopPx = screenHeightPx - with(density) { viewModel.currentSheetHeight.toPx() }

  // Current list of events to render as markers and heatmap points
  val events by viewModel.events.collectAsState()

  // Build weighted data for the heatmap from the events list
  val weightedData =
      remember(events) {
        val max = (events.maxOfOrNull { it.attendees } ?: 1).coerceAtLeast(1)
        events.map { e ->
          val weight = e.attendees.toDouble() / max.toDouble()
          WeightedLatLng(com.google.android.gms.maps.model.LatLng(e.latitude, e.longitude), weight)
        }
      }

  // Heatmap provider for live events (no historical overlay)
  val heatmapProvider =
      remember(weightedData) {
        HeatmapTileProvider.Builder().weightedData(weightedData).radius(50).build()
      }

  Box(modifier = Modifier.fillMaxSize()) {
    // Google Map with custom gesture handling to coordinate with the bottom sheet
    GoogleMap(
        modifier =
            Modifier.fillMaxSize()
                .then(
                    Modifier.mapPointerInput(
                        bottomSheetState = viewModel.bottomSheetState,
                        sheetTopPx = sheetTopPx,
                        densityDpi = densityDpi,
                        onCollapseSheet = {
                          viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
                        },
                        checkTouchProximity = viewModel::checkTouchProximityToSheet)),
        properties = MapProperties(),
        uiSettings =
            MapUiSettings(
                compassEnabled = true,
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                zoomGesturesEnabled = true,
                scrollGesturesEnabled = true,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = true),
        cameraPositionState = cameraPositionState) {
          // Render only the live heatmap (no historical layer)
          if (viewModel.heatmapEnabled) {
            TileOverlay(tileProvider = heatmapProvider, transparency = 0.15f, zIndex = 1f)
          }

          // Event markers
          events.forEach { e ->
            Marker(
                state =
                    rememberMarkerState(
                        position =
                            com.google.android.gms.maps.model.LatLng(e.latitude, e.longitude)),
                title = e.name,
                snippet = "${e.attendees} attendees")
          }
        }

    // Subtle gradient under the system status bar for better contrast
    TopGradient()

    // Dark scrim that fades in as the sheet expands from medium to full
    ScrimOverlay(
        currentHeightDp = viewModel.currentSheetHeight,
        mediumHeightDp = sheetConfig.mediumHeight,
        fullHeightDp = sheetConfig.fullHeight)

    // While fully expanded, block all map interactions
    if (viewModel.bottomSheetState == BottomSheetState.FULL) {
      MapInteractionBlocker()
    }

    // Floating button to toggle the heatmap on/off
    FloatingActionButton(
        onClick = { viewModel.toggleHeatmap() },
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).testTag("heatmapToggle")) {
          Text(if (viewModel.heatmapEnabled) "Heatmap ON" else "Heatmap OFF")
        }

    // Bottom sheet hosting the screen content; provides its height back to the map
    BottomSheet(
        config = sheetConfig,
        currentState = viewModel.bottomSheetState,
        onStateChange = { newState -> viewModel.setBottomSheetState(newState) },
        calculateTargetState = viewModel::calculateTargetState,
        stateToHeight = viewModel::getHeightForState,
        onHeightChange = { height -> viewModel.currentSheetHeight = height },
        modifier = Modifier.align(Alignment.BottomCenter).testTag("bottomSheet")) {
          BottomSheetContent(
              state = viewModel.bottomSheetState,
              fullEntryKey = viewModel.fullEntryKey,
              searchBarState =
                  SearchBarState(
                      query = viewModel.searchQuery,
                      shouldRequestFocus = viewModel.shouldFocusSearch,
                      onQueryChange = viewModel::onSearchQueryChange,
                      onTap = viewModel::onSearchTap,
                      onFocusHandled = viewModel::onSearchFocusHandled))
        }
  }
}

/** Top gradient overlay for better visibility of status bar icons on the map. */
@Composable
private fun TopGradient() {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(100.dp)
              .background(
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent))))
}

/** Dark overlay that gradually appears as the sheet expands from medium to full. */
@Composable
private fun ScrimOverlay(currentHeightDp: Dp, mediumHeightDp: Dp, fullHeightDp: Dp) {
  val opacity =
      if (currentHeightDp >= mediumHeightDp) {
        val progress =
            ((currentHeightDp - mediumHeightDp) / (fullHeightDp - mediumHeightDp)).coerceIn(0f, 1f)
        progress * MapConstants.MAX_SCRIM_ALPHA
      } else {
        0f
      }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .testTag("scrimOverlay")
              .background(Color.Black.copy(alpha = opacity)))
}

/** Transparent overlay that consumes all map gestures while the sheet is fully expanded. */
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
private fun MapInteractionBlocker() {
  Box(
      modifier =
          Modifier.fillMaxSize().testTag("mapInteractionBlocker").pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { pointerInputChange -> pointerInputChange.consume() }
              }
            }
          })
}

/**
 * Pointer input helper that collapses the sheet if the user drags near the sheet's top edge. Keeps
 * map gestures responsive while avoiding accidental drags under the sheet.
 */
private fun Modifier.mapPointerInput(
    bottomSheetState: BottomSheetState,
    sheetTopPx: Float,
    densityDpi: Int,
    onCollapseSheet: () -> Unit,
    checkTouchProximity: (Float, Float, Int) -> Boolean
) =
    this.pointerInput(bottomSheetState, sheetTopPx) {
      awaitPointerEventScope {
        while (true) {
          val event = awaitPointerEvent()
          if (event.type == PointerEventType.Move) {
            event.changes.firstOrNull()?.let { change ->
              val touchY = change.position.y
              if (checkTouchProximity(touchY, sheetTopPx, densityDpi)) {
                onCollapseSheet()
              }
            }
          }
        }
      }
    }

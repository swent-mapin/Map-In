package com.swent.mapin.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlinx.coroutines.flow.distinctUntilChanged

// Assisted by AI
/**
 * Main map screen with bottom sheet overlay.
 *
 * Architecture:
 * - Google Maps as background layer with location markers
 * - Top gradient for status bar visibility
 * - Progressive scrim overlay (darkens as sheet expands)
 * - Map interaction blocker (active only in full mode)
 * - Floating action button to toggle heatmap visualization
 * - Bottom sheet with content
 *
 * State is managed by MapScreenViewModel.
 */
@Composable
fun MapScreen() {
  val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
  val sheetConfig =
      BottomSheetConfig(
          collapsedHeight = MapConstants.COLLAPSED_HEIGHT,
          mediumHeight = MapConstants.MEDIUM_HEIGHT,
          fullHeight = screenHeightDp * MapConstants.FULL_HEIGHT_PERCENTAGE)

  val viewModel = rememberMapScreenViewModel(sheetConfig)

  val cameraPositionState = rememberCameraPositionState {
    position =
        CameraPosition.fromLatLngZoom(
            LatLng(MapConstants.DEFAULT_LATITUDE, MapConstants.DEFAULT_LONGITUDE),
            MapConstants.DEFAULT_ZOOM)
  }

  LaunchedEffect(viewModel.bottomSheetState) {
    if (viewModel.bottomSheetState == BottomSheetState.MEDIUM) {
      viewModel.updateMediumReferenceZoom(cameraPositionState.position.zoom)
    }
  }

  LaunchedEffect(Unit) {
    snapshotFlow { cameraPositionState.position.zoom }
        .distinctUntilChanged()
        .collect { z ->
          if (viewModel.checkZoomInteraction(z)) {
            viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
          }
        }
  }

  val density = LocalDensity.current
  val densityDpi = remember(density) { density.density.toInt() * 160 }
  val screenHeightPx = remember(screenHeightDp, density) { with(density) { screenHeightDp.toPx() } }
  val sheetTopPx = screenHeightPx - with(density) { viewModel.currentSheetHeight.toPx() }

  Box(modifier = Modifier.fillMaxSize().testTag(UiTestTags.MAP_SCREEN)) {
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
        cameraPositionState = cameraPositionState) {
          // Display location markers
          viewModel.locations.forEach { location ->
            Marker(
                state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                title = location.name,
                snippet = "${location.attendees} attendees")
          }

          // Display weighted heatmap based on attendees
          if (viewModel.showHeatmap && viewModel.locations.isNotEmpty()) {
            val weightedData =
                remember(viewModel.locations) {
                  val max = viewModel.locations.maxOfOrNull { it.attendees } ?: 0
                  // Create weighted points: more attendees = higher weight = more red
                  viewModel.locations.map { location ->
                    val weight = if (max == 0) 0.0 else location.attendees.toDouble() / max
                    WeightedLatLng(LatLng(location.latitude, location.longitude), weight)
                  }
                }

            val heatmapProvider =
                remember(weightedData) {
                  HeatmapTileProvider.Builder()
                      .weightedData(weightedData)
                      .radius(50) // Radius in pixels
                      .opacity(0.7) // Make it visible but not overwhelming
                      .build()
                }
            TileOverlay(tileProvider = heatmapProvider)
          }
        }

    TopGradient()

    ScrimOverlay(
        currentHeightDp = viewModel.currentSheetHeight,
        mediumHeightDp = sheetConfig.mediumHeight,
        fullHeightDp = sheetConfig.fullHeight)

    if (viewModel.bottomSheetState == BottomSheetState.FULL) {
      MapInteractionBlocker()
    }

    // Floating action button positioned at a fixed location (above collapsed sheet)
    Box(
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = MapConstants.COLLAPSED_HEIGHT + 16.dp, end = 16.dp)) {
          FloatingActionButton(
              onClick = { viewModel.toggleHeatmap() },
              modifier = Modifier.testTag("heatmapToggle"),
              containerColor =
                  if (viewModel.showHeatmap) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.secondaryContainer
                  }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_dialog_map),
                    contentDescription = if (viewModel.showHeatmap) "Heatmap ON" else "Heatmap OFF",
                    tint =
                        if (viewModel.showHeatmap) {
                          MaterialTheme.colorScheme.onPrimary
                        } else {
                          MaterialTheme.colorScheme.onSecondaryContainer
                        })
              }
        }

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

/** Top gradient overlay for better visibility of status bar icons on map */
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

/**
 * Dark overlay that gradually appears as sheet expands from medium to full.
 *
 * Opacity calculation:
 * - Below medium height: no scrim, opacity = 0
 * - At medium height: op = 0 (scrim starts appearing)
 * - Between medium and full: opacity increases linearly
 * - At full height: op = MAX_SCRIM_ALPHA (0.5)
 */
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
 * Creates a pointer input modifier for the map, handling touch events to collapse the bottom sheet
 * when dragging down near the sheet's top edge.
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

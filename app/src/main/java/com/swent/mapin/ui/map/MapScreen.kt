package com.swent.mapin.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig

// Assisted by AI
/**
 * Main map screen with bottom sheet overlay.
 *
 * Architecture:
 * - Google Maps as background layer TODO
 * - Top gradient for status bar visibility
 * - Progressive scrim overlay (darkens as sheet expands)
 * - Map interaction blocker (active only in full mode)
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

  LaunchedEffect(cameraPositionState.position.zoom) {
    if (viewModel.checkZoomInteraction(cameraPositionState.position.zoom)) {
      viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    }
  }

  val density = LocalDensity.current
  val densityDpi = remember(density) { density.density.toInt() * 160 }
  val screenHeightPx = remember(screenHeightDp, density) { with(density) { screenHeightDp.toPx() } }
  val sheetTopPx = screenHeightPx - with(density) { viewModel.currentSheetHeight.toPx() }

  Box(modifier = Modifier.fillMaxSize()) {
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
        cameraPositionState = cameraPositionState)

    TopGradient()

    ScrimOverlay(
        currentHeightDp = viewModel.currentSheetHeight,
        mediumHeightDp = sheetConfig.mediumHeight,
        fullHeightDp = sheetConfig.fullHeight)

    if (viewModel.bottomSheetState == BottomSheetState.FULL) {
      MapInteractionBlocker()
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

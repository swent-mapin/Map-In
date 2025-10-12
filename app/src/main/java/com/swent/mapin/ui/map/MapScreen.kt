package com.swent.mapin.ui.map

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.swent.mapin.R
import com.swent.mapin.model.Location
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

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
fun MapScreen(onLocationClick: (Location) -> Unit = {}) {
  val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
  val sheetConfig =
      BottomSheetConfig(
          collapsedHeight = MapConstants.COLLAPSED_HEIGHT,
          mediumHeight = MapConstants.MEDIUM_HEIGHT,
          fullHeight = screenHeightDp * MapConstants.FULL_HEIGHT_PERCENTAGE)

  val viewModel = rememberMapScreenViewModel(sheetConfig)
  val snackbarHostState = remember { SnackbarHostState() }

  // Show error messages in snackbar
  LaunchedEffect(viewModel.errorMessage) {
    viewModel.errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearError()
    }
  }

  val mapViewportState = rememberMapViewportState {
    setCameraOptions {
      zoom(MapConstants.DEFAULT_ZOOM.toDouble())
      center(Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE))
      pitch(0.0)
      bearing(0.0)
    }
  }

  ObserveSheetStateForZoomUpdate(viewModel, mapViewportState)
  ObserveZoomForSheetCollapse(viewModel, mapViewportState)

  val density = LocalDensity.current
  val densityDpi = remember(density) { (density.density * 160).toInt() }
  val screenHeightPx = remember(screenHeightDp, density) { screenHeightDp.value * density.density }
  val sheetTopPx = screenHeightPx - (viewModel.currentSheetHeight.value * density.density)

  val isDarkTheme = isSystemInDarkTheme()
  val lightPreset = if (isDarkTheme) LightPresetValue.NIGHT else LightPresetValue.DAY
  val standardStyleState = rememberStandardStyleState {
    configurationsState.apply {
      this.lightPreset = lightPreset
      showPointOfInterestLabels = BooleanValue(false)
    }
  }
  Box(modifier = Modifier.fillMaxSize().testTag(UiTestTags.MAP_SCREEN)) {
    MapboxMap(
        Modifier.fillMaxSize()
            .then(
                Modifier.mapPointerInput(
                    bottomSheetState = viewModel.bottomSheetState,
                    sheetTopPx = sheetTopPx,
                    densityDpi = densityDpi,
                    onCollapseSheet = { viewModel.setBottomSheetState(BottomSheetState.COLLAPSED) },
                    checkTouchProximity = viewModel::checkTouchProximityToSheet)),
        mapViewportState = mapViewportState,
        style = { MapboxStandardStyle(standardStyleState = standardStyleState) },
        compass = {
          Compass(
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(bottom = MapConstants.COLLAPSED_HEIGHT + 80.dp, end = 16.dp))
        },
        scaleBar = {
          AnimatedVisibility(
              visible = viewModel.isZooming,
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)) {
                ScaleBar()
              }
        }) {
          val marker =
              rememberIconImage(
                  key = R.drawable.ic_map_marker,
                  painter = painterResource(R.drawable.ic_map_marker))
          viewModel.locations.forEach { loc ->
            PointAnnotation(point = Point.fromLngLat(loc.longitude, loc.latitude)) {
              iconImage = marker
              // Place label below marker icon
              iconAnchor = IconAnchor.BOTTOM
              textAnchor = TextAnchor.TOP
              textOffset = listOf(0.0, 0.5) // x, y (ems). Positive y moves label downward
              textSize = 14.0
              // Adapt text color to theme + add halo for readability
              if (isDarkTheme) {
                textColor = Color.White
                textHaloColor = Color.Black.copy(alpha = 0.8f)
              } else {
                textColor = Color.Black
                textHaloColor = Color.White.copy(alpha = 0.85f)
              }
              textHaloWidth = 1.5
              textField = loc.name
              interactionsState.onClicked {
                onLocationClick(loc)
                true
              }
            }
          }
        }

    TopGradient()

    ScrimOverlay(
        currentHeightDp = viewModel.currentSheetHeight,
        mediumHeightDp = sheetConfig.mediumHeight,
        fullHeightDp = sheetConfig.fullHeight)

    ConditionalMapBlocker(bottomSheetState = viewModel.bottomSheetState)

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
                      onFocusHandled = viewModel::onSearchFocusHandled),
              showMemoryForm = viewModel.showMemoryForm,
              availableEvents = viewModel.availableEvents,
              onCreateMemoryClick = viewModel::showMemoryForm,
              onMemorySave = viewModel::onMemorySave,
              onMemoryCancel = viewModel::onMemoryCancel)
        }

    // Loading indicator while saving memory
    if (viewModel.isSavingMemory) {
      Box(
          modifier =
              Modifier.fillMaxSize()
                  .background(Color.Black.copy(alpha = 0.5f))
                  .testTag("memoryLoadingIndicator"),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
    }

    // Snackbar for error messages
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
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

/** Observes bottom sheet state and updates zoom reference when transitioning to MEDIUM state. */
@Composable
private fun ObserveSheetStateForZoomUpdate(
    viewModel: MapScreenViewModel,
    mapViewportState: MapViewportState
) {
  LaunchedEffect(viewModel.bottomSheetState, mapViewportState) {
    if (viewModel.bottomSheetState == BottomSheetState.MEDIUM) {
      mapViewportState.cameraState?.let { viewModel.updateMediumReferenceZoom(it.zoom.toFloat()) }
    }
  }
}

/** Observes camera zoom changes and collapses sheet when user interacts with zoom. */
@Composable
private fun ObserveZoomForSheetCollapse(
    viewModel: MapScreenViewModel,
    mapViewportState: MapViewportState
) {
  LaunchedEffect(mapViewportState) {
    snapshotFlow { mapViewportState.cameraState?.zoom?.toFloat() ?: 0f }
        .filterNotNull()
        .distinctUntilChanged()
        .collect { z ->
          // Track zoom changes for ScaleBar visibility
          viewModel.onZoomChange(z)

          // Check if zoom interaction should collapse sheet
          if (viewModel.checkZoomInteraction(z)) {
            viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
          }
        }
  }
}

/** Conditionally displays map interaction blocker when sheet is fully expanded. */
@Composable
private fun ConditionalMapBlocker(bottomSheetState: BottomSheetState) {
  if (bottomSheetState == BottomSheetState.FULL) {
    MapInteractionBlocker()
  }
}

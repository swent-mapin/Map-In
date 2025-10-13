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
import com.swent.mapin.model.event.Event
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
fun MapScreen(onNavigateToProfile: () -> Unit = {}) {
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

  ObserveSheetStateForZoomUpdate(viewModel, cameraPositionState)
  ObserveZoomForSheetCollapse(viewModel, cameraPositionState)

  val density = LocalDensity.current
  val densityDpi = remember(density) { (density.density * 160).toInt() }
  val screenHeightPx = remember(screenHeightDp, density) { screenHeightDp.value * density.density }
  val sheetTopPx = screenHeightPx - (viewModel.currentSheetHeight.value * density.density)

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
          MapMarkers(events = viewModel.events)
          HeatmapOverlay(showHeatmap = viewModel.showHeatmap, events = viewModel.events)
        }

    TopGradient()

    ScrimOverlay(
        currentHeightDp = viewModel.currentSheetHeight,
        mediumHeightDp = sheetConfig.mediumHeight,
        fullHeightDp = sheetConfig.fullHeight)

    ConditionalMapBlocker(bottomSheetState = viewModel.bottomSheetState)

    // Profile button in top-right corner
    Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)) {
      ProfileButton(onClick = onNavigateToProfile)
    }

    Box(
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = MapConstants.COLLAPSED_HEIGHT + 16.dp, end = 16.dp)) {
          HeatmapToggleButton(
              showHeatmap = viewModel.showHeatmap, onToggle = viewModel::toggleHeatmap)
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

/** Observes bottom sheet state and updates zoom reference when transitioning to MEDIUM state. */
@Composable
private fun ObserveSheetStateForZoomUpdate(
    viewModel: MapScreenViewModel,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState
) {
  LaunchedEffect(viewModel.bottomSheetState) {
    if (viewModel.bottomSheetState == BottomSheetState.MEDIUM) {
      viewModel.updateMediumReferenceZoom(cameraPositionState.position.zoom)
    }
  }
}

/** Observes camera zoom changes and collapses sheet when user interacts with zoom. */
@Composable
private fun ObserveZoomForSheetCollapse(
    viewModel: MapScreenViewModel,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState
) {
  LaunchedEffect(Unit) {
    snapshotFlow { cameraPositionState.position.zoom }
        .distinctUntilChanged()
        .collect { z ->
          if (viewModel.checkZoomInteraction(z)) {
            viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
          }
        }
  }
}

/** Renders event markers on the map with title and attendee count. */
@Composable
private fun MapMarkers(events: List<Event>) {
  events.forEach { event ->
    Marker(
        state = MarkerState(position = LatLng(event.latitude, event.longitude)),
        title = event.title,
        snippet = "${event.attendeeCount ?: 0} attendees")
  }
}

/** Renders heatmap overlay based on event attendee density. */
@Composable
private fun HeatmapOverlay(showHeatmap: Boolean, events: List<Event>) {
  if (!showHeatmap || events.isEmpty()) return

  val weightedData =
      remember(events) {
        val maxAttendees = events.maxOfOrNull { it.attendeeCount ?: 0 } ?: 0
        val data =
            events.map { event ->
              val weight =
                  if (maxAttendees == 0) 1.0
                  else (event.attendeeCount ?: 0).toDouble() / maxAttendees
              WeightedLatLng(LatLng(event.latitude, event.longitude), weight)
            }
        data
      }

  val heatmapProvider =
      remember(weightedData) {
        HeatmapTileProvider.Builder().weightedData(weightedData).radius(50).opacity(0.7).build()
      }

  TileOverlay(tileProvider = heatmapProvider)
}

/** Conditionally displays map interaction blocker when sheet is fully expanded. */
@Composable
private fun ConditionalMapBlocker(bottomSheetState: BottomSheetState) {
  if (bottomSheetState == BottomSheetState.FULL) {
    MapInteractionBlocker()
  }
}

/** Floating action button to toggle heatmap visualization. */
@Composable
private fun HeatmapToggleButton(showHeatmap: Boolean, onToggle: () -> Unit) {
  FloatingActionButton(
      onClick = onToggle,
      modifier = Modifier.testTag("heatmapToggle"),
      containerColor =
          if (showHeatmap) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.secondaryContainer) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_dialog_map),
            contentDescription = if (showHeatmap) "Heatmap ON" else "Heatmap OFF",
            tint =
                if (showHeatmap) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer)
      }
}

/** Profile button for navigating to user profile screen. */
@Composable
private fun ProfileButton(onClick: () -> Unit) {
  FloatingActionButton(
      onClick = onClick,
      modifier = Modifier.testTag("profileButton"),
      containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
            contentDescription = "Profile",
            tint = MaterialTheme.colorScheme.onPrimaryContainer)
      }
}

package com.swent.mapin.ui.map.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.swent.mapin.ui.map.BottomSheetState
import com.swent.mapin.ui.map.MapScreenViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/**
 * Data class holding metrics for sheet interaction calculations.
 *
 * @property densityDpi Screen density in DPI for touch proximity calculations
 * @property sheetTopPx Current top position of the bottom sheet in pixels
 */
data class SheetInteractionMetrics(val densityDpi: Int, val sheetTopPx: Float)

/**
 * Calculates and remembers sheet interaction metrics based on screen dimensions.
 *
 * Converts dp values to pixels and computes the sheet's top position for touch detection.
 */
@Composable
fun rememberSheetInteractionMetrics(
    screenHeightDp: Dp,
    currentSheetHeight: Dp
): SheetInteractionMetrics {
  val density = LocalDensity.current
  val densityFactor = density.density
  val densityDpi = remember(densityFactor) { (densityFactor * 160).toInt() }
  val screenHeightPx =
      remember(screenHeightDp, densityFactor) { screenHeightDp.value * densityFactor }
  val sheetTopPx =
      remember(screenHeightPx, currentSheetHeight, densityFactor) {
        screenHeightPx - (currentSheetHeight.value * densityFactor)
      }
  return remember(densityDpi, sheetTopPx) { SheetInteractionMetrics(densityDpi, sheetTopPx) }
}

/** Pointer modifier that collapses the sheet when touches originate near its top edge. */
fun Modifier.mapPointerInput(
    bottomSheetState: BottomSheetState,
    sheetMetrics: SheetInteractionMetrics,
    onCollapseSheet: () -> Unit,
    checkTouchProximity: (Float, Float, Int) -> Boolean
) =
    this.pointerInput(bottomSheetState, sheetMetrics) {
      awaitPointerEventScope {
        while (true) {
          val event = awaitPointerEvent()
          if (event.type == PointerEventType.Move) {
            event.changes.firstOrNull()?.let { change ->
              val touchY = change.position.y
              if (checkTouchProximity(touchY, sheetMetrics.sheetTopPx, sheetMetrics.densityDpi)) {
                onCollapseSheet()
              }
            }
          }
        }
      }
    }

/** Updates the zoom baseline whenever the sheet settles in MEDIUM. */
@Composable
fun ObserveSheetStateForZoomUpdate(
    viewModel: MapScreenViewModel,
    mapViewportState: MapViewportState
) {
  LaunchedEffect(viewModel.bottomSheetState, mapViewportState) {
    if (viewModel.bottomSheetState == BottomSheetState.MEDIUM) {
      mapViewportState.cameraState?.let { viewModel.updateMediumReferenceZoom(it.zoom.toFloat()) }
    }
  }
}

/** Collapses the sheet after zoom interactions and keeps zoom state in sync. */
@Composable
fun ObserveZoomForSheetCollapse(viewModel: MapScreenViewModel, mapViewportState: MapViewportState) {
  LaunchedEffect(mapViewportState) {
    snapshotFlow { mapViewportState.cameraState?.zoom?.toFloat() ?: 0f }
        .filterNotNull()
        .distinctUntilChanged()
        .collect { z ->
          viewModel.onZoomChange(z)

          if (viewModel.checkZoomInteraction(z)) {
            viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
          }
        }
  }
}

package com.swent.mapin.ui.map.bottomsheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.map.BottomSheetState
import com.swent.mapin.ui.map.MapConstants
import kotlin.math.abs

/**
 * Handles state/metrics for the bottom sheet so the ViewModel focuses on orchestration.
 *
 * @param sheetConfig Geometry definitions for collapsed/medium/full states.
 * @param initialState Starting state when the screen is created.
 * @param isProgrammaticZoom Provider used to skip auto-collapse while map animations run.
 */
class BottomSheetStateController(
    private val sheetConfig: BottomSheetConfig,
    initialState: BottomSheetState,
    private val isProgrammaticZoom: () -> Boolean
) {

  private var _state by mutableStateOf(initialState)
  val state: BottomSheetState
    get() = _state

  private var _currentSheetHeight by mutableStateOf(sheetConfig.collapsedHeight)
  val currentSheetHeight: Dp
    get() = _currentSheetHeight

  private var isInMediumMode = initialState == BottomSheetState.MEDIUM
  private var _mediumReferenceZoom by mutableFloatStateOf(0f)

  private var _fullEntryKey by mutableIntStateOf(0)
  val fullEntryKey: Int
    get() = _fullEntryKey

  fun updateSheetHeight(height: Dp) {
    _currentSheetHeight = height
  }

  fun calculateTargetState(
      currentHeightPx: Float,
      collapsedPx: Float,
      mediumPx: Float,
      fullPx: Float
  ): BottomSheetState {
    return when {
      currentHeightPx < (collapsedPx + mediumPx) / 2f -> BottomSheetState.COLLAPSED
      currentHeightPx < (mediumPx + fullPx) / 2f -> BottomSheetState.MEDIUM
      else -> BottomSheetState.FULL
    }
  }

  fun getHeightForState(state: BottomSheetState): Dp {
    return when (state) {
      BottomSheetState.COLLAPSED -> sheetConfig.collapsedHeight
      BottomSheetState.MEDIUM -> sheetConfig.mediumHeight
      BottomSheetState.FULL -> sheetConfig.fullHeight
    }
  }

  fun transitionTo(target: BottomSheetState): SheetTransition {
    val previous = _state
    if (target == BottomSheetState.FULL && previous != BottomSheetState.FULL) {
      _fullEntryKey += 1
    }
    _state = target
    isInMediumMode = target == BottomSheetState.MEDIUM
    return SheetTransition(previous, target)
  }

  fun updateMediumReferenceZoom(zoom: Float) {
    if (isInMediumMode) {
      _mediumReferenceZoom = zoom
    }
  }

  fun shouldCollapseAfterZoom(currentZoom: Float): Boolean {
    if (!isInMediumMode) return false
    if (isProgrammaticZoom()) return false
    val zoomDelta = abs(currentZoom - _mediumReferenceZoom)
    return zoomDelta >= MapConstants.ZOOM_CHANGE_THRESHOLD
  }

  fun isTouchNearSheetTop(touchY: Float, sheetTopY: Float, densityDpi: Int): Boolean {
    if (!isInMediumMode) return false
    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f
    val distance = abs(touchY - sheetTopY)
    return distance <= thresholdPx
  }

  data class SheetTransition(val previousState: BottomSheetState, val newState: BottomSheetState) {
    val leftFull: Boolean =
        previousState == BottomSheetState.FULL && newState != BottomSheetState.FULL
  }
}

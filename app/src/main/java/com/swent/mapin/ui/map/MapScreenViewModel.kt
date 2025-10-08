package com.swent.mapin.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlin.math.abs

/**
 * ViewModel for MapScreen that encapsulates all state management and business logic.
 * - Manages bottom sheet state transitions
 * - Handles search interactions
 * - Detects zoom-based map interactions
 * - Coordinates scroll resets and focus management
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    private val onClearFocus: () -> Unit
) : ViewModel() {

  private var _bottomSheetState by mutableStateOf(initialSheetState)
  val bottomSheetState: BottomSheetState
    get() = _bottomSheetState

  private var _fullEntryKey by mutableIntStateOf(0)
  val fullEntryKey: Int
    get() = _fullEntryKey

  var currentSheetHeight by mutableStateOf(sheetConfig.collapsedHeight)

  private var _searchQuery by mutableStateOf("")
  val searchQuery: String
    get() = _searchQuery

  private var _shouldFocusSearch by mutableStateOf(false)
  val shouldFocusSearch: Boolean
    get() = _shouldFocusSearch

  private var _mediumReferenceZoom by mutableFloatStateOf(0f)

  private var isInMediumMode = false

  /** Update bottom sheet state and handle side effects */
  fun setBottomSheetState(target: BottomSheetState) {
    if (target == BottomSheetState.FULL && _bottomSheetState != BottomSheetState.FULL) {
      _fullEntryKey += 1
    }

    isInMediumMode = target == BottomSheetState.MEDIUM

    if (target != BottomSheetState.FULL && _bottomSheetState == BottomSheetState.FULL) {
      clearSearch()
    }

    _bottomSheetState = target
  }

  /** Handle search query changes - automatically expands to full mode */
  fun onSearchQueryChange(query: String) {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
    _searchQuery = query
  }

  /** Handle search bar tap - expands to full mode */
  fun onSearchTap() {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
  }

  /** Clear focus flag after search bar has been focused */
  fun onSearchFocusHandled() {
    _shouldFocusSearch = false
  }

  /** Clear search and dismiss keyboard */
  private fun clearSearch() {
    _shouldFocusSearch = false
    onClearFocus()
    if (_searchQuery.isNotEmpty()) {
      _searchQuery = ""
    }
  }

  /** Set reference zoom when entering medium mode */
  fun updateMediumReferenceZoom(zoom: Float) {
    if (isInMediumMode) {
      _mediumReferenceZoom = zoom
    }
  }

  /** Check if zoom change should trigger collapse */
  fun checkZoomInteraction(currentZoom: Float): Boolean {
    if (!isInMediumMode) return false

    val zoomDelta = abs(currentZoom - _mediumReferenceZoom)
    return zoomDelta >= MapConstants.ZOOM_CHANGE_THRESHOLD
  }

  /**
   * Check if touch position is within proximity of sheet top edge. Returns true if the touch is
   * close enough to trigger collapse.
   *
   * @param touchY Y coordinate of touch position in pixels
   * @param sheetTopY Y coordinate of sheet top edge in pixels
   * @param densityDpi Screen density in DPI for dp-to-px conversion
   */
  fun checkTouchProximityToSheet(touchY: Float, sheetTopY: Float, densityDpi: Int): Boolean {
    if (!isInMediumMode) return false

    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f
    val distance = abs(touchY - sheetTopY)

    return distance <= thresholdPx
  }

  /** Calculate target state after drag based on current height */
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

  /** Map state to height */
  fun getHeightForState(state: BottomSheetState): Dp {
    return when (state) {
      BottomSheetState.COLLAPSED -> sheetConfig.collapsedHeight
      BottomSheetState.MEDIUM -> sheetConfig.mediumHeight
      BottomSheetState.FULL -> sheetConfig.fullHeight
    }
  }

  /** Cleanup when ViewModel is destroyed. Later: add cleanup for coroutines/resources. */
  override fun onCleared() {
    super.onCleared()
  }
}

/**
 * Creates MapScreenViewModel with configuration.
 *
 * @param sheetConfig Configuration for bottom sheet heights
 * @param initialSheetState Initial state of the bottom sheet
 */
@Composable
fun rememberMapScreenViewModel(
    sheetConfig: BottomSheetConfig,
    initialSheetState: BottomSheetState = BottomSheetState.COLLAPSED
): MapScreenViewModel {
  val focusManager = LocalFocusManager.current

  return viewModel {
    MapScreenViewModel(
        initialSheetState = initialSheetState,
        sheetConfig = sheetConfig,
        onClearFocus = { focusManager.clearFocus(force = true) })
  }
}

package com.swent.mapin.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.swent.mapin.model.Location
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the Map screen.
 *
 * Responsibilities:
 * - Holds bottom sheet state and height mapping.
 * - Manages search state and focus behavior.
 * - Exposes the initial camera position and live event data (for markers/heatmap).
 * - Handles zoom change heuristics and touch proximity near the sheet top edge.
 *
 * Note: All "historical cloud" logic has been removed.
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    private val onClearFocus: () -> Unit
) : ViewModel() {

  // Backing field to avoid a public setter clash on JVM; expose as a getter only.
  private var _bottomSheetState by mutableStateOf(initialSheetState)
  val bottomSheetState: BottomSheetState
    get() = _bottomSheetState

  // Current measured height of the bottom sheet (source of truth for scrims/overlays).
  var currentSheetHeight by mutableStateOf(sheetConfig.collapsedHeight)

  // Search bar state.
  var searchQuery by mutableStateOf("")
    private set

  var shouldFocusSearch by mutableStateOf(false)
    private set

  // Monotonic key incremented when entering FULL to trigger content recomposition if needed.
  var fullEntryKey by mutableStateOf(0)
    private set

  // Initial camera position for the map.
  val initialCamera: LatLng = LatLng(MapConstants.DEFAULT_LATITUDE, MapConstants.DEFAULT_LONGITUDE)

  // Live events stream (sample data) used for markers and the live heatmap.
  private val _events =
      MutableStateFlow(
          listOf(
              Location("Event A", 46.5199, 6.5665, attendees = 120),
              Location("Event B", 46.5203, 6.5672, attendees = 60),
              Location("Event C", 46.5188, 6.5651, attendees = 200)))
  val events: StateFlow<List<Location>> = _events

  // Heatmap visibility toggle.
  var heatmapEnabled by mutableStateOf(false)
    private set

  fun toggleHeatmap() {
    heatmapEnabled = !heatmapEnabled
  }

  // --- Bottom sheet / search logic ---

  /**
   * Updates the bottom sheet state and performs side effects when entering or leaving the FULL
   * state (focus management and search reset).
   */
  fun setBottomSheetState(newState: BottomSheetState) {
    if (_bottomSheetState == newState) return

    val wasFull = _bottomSheetState == BottomSheetState.FULL
    val willBeFull = newState == BottomSheetState.FULL

    if (!wasFull && willBeFull) {
      // Entering FULL: increment the key to trigger dependent recompositions.
      fullEntryKey += 1
    }

    if (wasFull && !willBeFull) {
      // Leaving FULL: clear search and remove focus.
      searchQuery = ""
      shouldFocusSearch = false
      onClearFocus()
    }

    _bottomSheetState = newState
  }

  /**
   * Called when the search query changes. Expands the sheet to FULL (and requests focus) if not
   * already FULL.
   */
  fun onSearchQueryChange(newQuery: String) {
    if (bottomSheetState != BottomSheetState.FULL) {
      setBottomSheetState(BottomSheetState.FULL)
      shouldFocusSearch = true
    }
    searchQuery = newQuery
  }

  /** Called when tapping the search bar. Expands to FULL and requests focus if needed. */
  fun onSearchTap() {
    if (bottomSheetState != BottomSheetState.FULL) {
      setBottomSheetState(BottomSheetState.FULL)
      shouldFocusSearch = true
    }
    // If already FULL, do not re-request focus.
  }

  /** Marks that any requested focus has been handled by the UI. */
  fun onSearchFocusHandled() {
    shouldFocusSearch = false
  }

  // --- Zoom / sheet proximity logic ---

  // Reference zoom captured when the sheet reaches MEDIUM.
  private var mediumReferenceZoom: Float = 0f

  /** Capture the current zoom as the MEDIUM reference when in MEDIUM state. */
  fun updateMediumReferenceZoom(currentZoom: Float) {
    if (bottomSheetState == BottomSheetState.MEDIUM) {
      mediumReferenceZoom = currentZoom
    }
  }

  /**
   * Returns true if the zoom change from the reference exceeds the defined threshold while the
   * sheet is in MEDIUM, indicating a significant user interaction.
   */
  fun checkZoomInteraction(currentZoom: Float): Boolean {
    if (bottomSheetState != BottomSheetState.MEDIUM) return false
    return abs(currentZoom - mediumReferenceZoom) >= MapConstants.ZOOM_CHANGE_THRESHOLD
  }

  /**
   * Detects if a touch is close to the top edge of the bottom sheet (in MEDIUM state), using a
   * DPI-aware threshold, to decide whether to collapse the sheet.
   */
  fun checkTouchProximityToSheet(touchY: Float, sheetTopY: Float, densityDpi: Int): Boolean {
    if (bottomSheetState != BottomSheetState.MEDIUM) return false
    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f
    val dist = abs(touchY - sheetTopY)
    return dist <= thresholdPx
  }

  // --- Height/state mapping ---

  /**
   * Maps a current pixel height to a target state by snapping to the closest bucket among
   * COLLAPSED, MEDIUM, and FULL.
   */
  fun calculateTargetState(
      currentHeightPx: Float,
      collapsedHeightPx: Float,
      mediumHeightPx: Float,
      fullHeightPx: Float
  ): BottomSheetState {
    val firstMid = (collapsedHeightPx + mediumHeightPx) / 2f
    val secondMid = (mediumHeightPx + fullHeightPx) / 2f

    return when {
      currentHeightPx < firstMid -> BottomSheetState.COLLAPSED
      currentHeightPx < secondMid -> BottomSheetState.MEDIUM
      else -> BottomSheetState.FULL
    }
  }

  /** Converts a state to its configured height in Dp. */
  fun getHeightForState(state: BottomSheetState): Dp =
      when (state) {
        BottomSheetState.COLLAPSED -> sheetConfig.collapsedHeight
        BottomSheetState.MEDIUM -> sheetConfig.mediumHeight
        BottomSheetState.FULL -> sheetConfig.fullHeight
      }
}

/**
 * Composable helper to create and remember a MapScreenViewModel bound to the given config. Wires a
 * focus clear callback into the ViewModel for when the sheet collapses.
 */
@Composable
fun rememberMapScreenViewModel(
    sheetConfig: BottomSheetConfig,
    initialSheetState: BottomSheetState = BottomSheetState.COLLAPSED
): MapScreenViewModel {
  val focusManager = LocalFocusManager.current
  return remember(sheetConfig, initialSheetState) {
    MapScreenViewModel(
        initialSheetState = initialSheetState,
        sheetConfig = sheetConfig,
        onClearFocus = { focusManager.clearFocus() })
  }
}

package com.swent.mapin.ui.map

import androidx.compose.ui.unit.dp

/**
 * Configuration constants for the Map screen and bottom sheet behavior. These values control the
 * dimensions and behavior of the interactive bottom sheet.
 */
object MapConstants {
  /** Height of the bottom sheet when collapsed (showing only search bar) */
  val COLLAPSED_HEIGHT = 120.dp

  /** Height of the bottom sheet at medium state (showing menu and buttons) */
  val MEDIUM_HEIGHT = 400.dp

  /** Percentage of screen height for full state */
  const val FULL_HEIGHT_PERCENTAGE = 0.98f

  /** Maximum opacity of the scrim overlay when sheet is at full height */
  const val MAX_SCRIM_ALPHA = 0.5f

  /** Minimum zoom change threshold to trigger bottom sheet collapse */
  const val ZOOM_CHANGE_THRESHOLD = 0.5f

  /**
   * Maximum distance (in dp) from touch position to sheet top edge to trigger collapse. When user
   * pans the map within this distance from the sheet, it collapses.
   */
  const val SHEET_PROXIMITY_THRESHOLD_DP = 5f

  /** Overscroll allowance in dp - allows slight dragging beyond bounds for natural feel */
  const val OVERSCROLL_ALLOWANCE_DP = 60f

  /** Default map latitude */
  const val DEFAULT_LATITUDE = 46.5197

  /** Default map longitude */
  const val DEFAULT_LONGITUDE = 6.5668

  /** Default map zoom level */
  const val DEFAULT_ZOOM = 15f

  object HeatmapConfig {
    const val MAX_ZOOM_LEVEL = 18L
    const val OPACITY = 0.65
    val RADIUS_STOPS =
        listOf(
            0.0 to 18.0,
            14.0 to 32.0,
            22.0 to 48.0,
        )
    val WEIGHT_STOPS =
        listOf(
            0.0 to 0.0,
            5.0 to 0.4,
            25.0 to 0.8,
            100.0 to 1.0,
        )
  }

  object CameraConfig {
    const val ZOOM_DELTA_THRESHOLD = 0.01f
    const val SCALE_BAR_HIDE_DELAY_MS = 300L
    const val PROGRAMMATIC_ZOOM_RESET_DELAY_MS = 1100L
  }

  /** Heatmap color stops for visualization */
  object HeatmapColors {
    val COLOR_STOPS =
        listOf(
            0.0 to ColorStop(33.0, 102.0, 172.0, 0.0),
            0.2 to ColorStop(103.0, 169.0, 207.0, 255.0),
            0.4 to ColorStop(209.0, 229.0, 240.0, 255.0),
            0.6 to ColorStop(253.0, 219.0, 199.0, 255.0),
            0.8 to ColorStop(239.0, 138.0, 98.0, 255.0),
            1.0 to ColorStop(178.0, 24.0, 43.0, 255.0))
  }

  data class ColorStop(val r: Double, val g: Double, val b: Double, val a: Double)
}

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

  /**
   * Percentage of screen height for full state */
  const val FULL_HEIGHT_PERCENTAGE = 0.98f

  /** Maximum opacity of the scrim overlay when sheet is at full height */
  const val MAX_SCRIM_ALPHA = 0.5f

  /** Minimum zoom change threshold to trigger bottom sheet collapse */
  const val ZOOM_CHANGE_THRESHOLD = 0.5f

  /** Overscroll allowance in dp - allows slight dragging beyond bounds for natural feel */
  const val OVERSCROLL_ALLOWANCE_DP = 60f
}

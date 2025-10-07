package com.swent.mapin.ui.map

/**
 * States for the draggable bottom sheet.
 * - COLLAPSED: Only search bar visible
 * - MEDIUM: Search bar + quick actions
 * - FULL: All content visible, map interaction blocked
 */
enum class BottomSheetState {
  COLLAPSED,
  MEDIUM,
  FULL
}

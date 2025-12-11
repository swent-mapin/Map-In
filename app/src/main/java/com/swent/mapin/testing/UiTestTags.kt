package com.swent.mapin.testing

/**
 * Centralized test tags for UI testing.
 *
 * This object provides constant strings used as test tags throughout the application for automated
 * testing with Compose UI tests. Using centralized tags ensures consistency and makes it easier to
 * maintain tests.
 *
 * ## Naming Convention
 * - Screen tags: `{FEATURE}_SCREEN` (e.g., AUTH_SCREEN)
 * - Button tags: `{FEATURE}_{ACTION}_BUTTON` (e.g., AUTH_CONTINUE_BUTTON)
 * - Component tags: `{FEATURE}_{COMPONENT}_TYPE` (e.g., MAP_SEARCH_FIELD)
 * - Use PascalCase for tag values to maintain consistency
 */
object UiTestTags {

  // Authentication Screen
  const val AUTH_SCREEN = "AuthScreen"
  const val AUTH_CONTINUE_BUTTON = "AuthContinueButton"
  const val AUTH_ERROR_CARD = "AuthErrorCard"
  const val AUTH_ERROR_TEXT = "AuthErrorText"
  const val AUTH_ERROR_DISMISS = "AuthErrorDismiss"

  // Map Screen
  const val MAP_SCREEN = "MapScreen"
  const val GET_DIRECTIONS_BUTTON = "getDirectionsButton"
}

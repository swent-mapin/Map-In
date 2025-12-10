package com.swent.mapin.testing

/**
 * Centralized test tags for UI testing.
 *
 * This object provides constant strings used as test tags throughout the application for automated
 * testing with Compose UI tests. Using centralized tags ensures consistency and makes it easier to
 * maintain tests.
 */
object UiTestTags {
  /** Test tag for the authentication screen */
  const val AUTH_SCREEN = "AuthScreen"

  /** Test tag for the continue button on the authentication screen */
  const val AUTH_CONTINUE_BUTTON = "AuthContinueButton"

  /** Test tag for the main map screen */
  const val MAP_SCREEN = "MapScreen"

  /** Test tag for the get directions button */
  const val GET_DIRECTIONS_BUTTON = "getDirectionsButton"
}

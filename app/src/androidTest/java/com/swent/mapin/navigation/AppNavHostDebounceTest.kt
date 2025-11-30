package com.swent.mapin.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test to cover navigation debouncing in AppNavHost. */
@RunWith(AndroidJUnit4::class)
class AppNavHostDebounceTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun appNavHost_rapidBackClicks_handledGracefully() {
    composeTestRule.setContent { AppNavHost(isLoggedIn = true, renderMap = false) }
    composeTestRule.waitForIdle()

    // Navigate to profile
    composeTestRule.onNodeWithTag("profileButton").performClick()
    composeTestRule.waitForIdle()

    // Rapidly click back twice (tests debouncing)
    composeTestRule.onNodeWithTag("backButton").performClick()
    composeTestRule.onNodeWithTag("backButton").performClick()
    composeTestRule.waitForIdle()

    // Should be back on map without crash
    composeTestRule.onNodeWithTag("mapScreen").assertExists()
  }
}

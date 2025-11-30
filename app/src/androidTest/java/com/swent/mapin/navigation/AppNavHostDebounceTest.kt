package com.swent.mapin.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test to cover navigation debouncing in AppNavHost. */
@RunWith(AndroidJUnit4::class)
class AppNavHostDebounceTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun appNavHost_rapidNavigationCalls_handledGracefully() {
    lateinit var navController: androidx.navigation.NavHostController

    composeTestRule.setContent {
      navController = androidx.navigation.compose.rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Build up a navigation stack
    composeTestRule.runOnUiThread {
      navController.navigate("profile")
      navController.navigate("settings")
    }

    // Wait for settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Trigger back navigation rapidly through back button clicks
    // This exercises the safePopBackStack() debouncing logic
    // First click should navigate back to profile, second should be debounced
    composeTestRule.onNodeWithTag("backButton", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("backButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Should be on profile (first click succeeded, second was debounced)
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertExists()
  }
}

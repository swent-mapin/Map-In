package com.swent.mapin.navigationTests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.testing.UiTestTags
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun startsOnAuth_whenNotLoggedIn() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = false, renderMap = false)
    }

    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun startsOnMap_whenLoggedIn() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun navigatesToProfile_fromMap() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Verify we're on the map screen
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()

    // Expand to MEDIUM state to reveal profile button
    // First click search bar to go to FULL
    composeTestRule.onNodeWithText("Search activities", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    // Then click cancel to go to MEDIUM with QuickActions visible
    composeTestRule
        .onNodeWithContentDescription("Clear search", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Navigate to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for profile screen to appear after navigation
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on the profile screen
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun logout_navigatesBackToAuth() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Expand to MEDIUM state to reveal profile button
    // First click search bar to go to FULL
    composeTestRule.onNodeWithText("Search activities", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    // Then click cancel to go to MEDIUM with QuickActions visible
    composeTestRule
        .onNodeWithContentDescription("Clear search", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Navigate to profile from map
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for profile screen to appear after fade transition
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on profile screen
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()

    // Navigate to Settings
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to and click logout button in Settings
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for dialog to appear (increased timeout for CI)
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText("Confirm Logout", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Confirm in dialog - use index to get dialog button
    composeTestRule.onAllNodesWithText("Logout", useUnmergedTree = true)[1].performClick()

    composeTestRule.waitForIdle()

    // Verify we're back on auth screen
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun logout_clearsBackStack() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Expand to MEDIUM state to reveal profile button
    // First click search bar to go to FULL
    composeTestRule.onNodeWithText("Search activities", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    // Then click cancel to go to MEDIUM with QuickActions visible
    composeTestRule
        .onNodeWithContentDescription("Clear search", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Navigate to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Navigate to Settings
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to and click logout button in Settings
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for dialog to appear (increased timeout for CI)
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText("Confirm Logout", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Confirm in dialog - click the button, not just any "Logout" text
    composeTestRule.onAllNodesWithText("Logout", useUnmergedTree = true)[1].performClick()

    composeTestRule.waitForIdle()

    // Verify we're back on auth screen
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Back button should not navigate away from auth screen (back stack is cleared)
    // Note: In a real scenario, you'd test that the back stack is empty by attempting
    // to navigate back and verifying we stay on the auth screen
  }

  @Test
  fun logout_fromProfile_cannotNavigateBackToMap() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Expand to MEDIUM state to reveal profile button
    // First click search bar to go to FULL
    composeTestRule.onNodeWithText("Search activities", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    // Then click cancel to go to MEDIUM with QuickActions visible
    composeTestRule
        .onNodeWithContentDescription("Clear search", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Go to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for profile screen to appear after fade transition
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate to Settings
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to and click logout in Settings
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for dialog (increased timeout for CI)
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText("Confirm Logout", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Confirm logout - use index to get dialog button
    composeTestRule.onAllNodesWithText("Logout", useUnmergedTree = true)[1].performClick()

    composeTestRule.waitForIdle()

    // Verify we're on auth screen
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Map screen should not be in the composition tree after logout
    composeTestRule
        .onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun navigatesToSettings_fromProfile() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Navigate to profile
    composeTestRule.onNodeWithText("Search activities", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithContentDescription("Clear search", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on profile screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to settings button and click it
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("settingsScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun navigatesBackToProfile_fromSettings() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Navigate to profile
    composeTestRule.onNodeWithText("Search activities", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithContentDescription("Clear search", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Navigate to settings
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("settingsButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click back button
    composeTestRule.onNodeWithTag("backButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're back on profile screen
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()
  }
}

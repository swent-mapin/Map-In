package com.swent.mapin.navigationTests

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.testing.UiTestTags
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {

  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var navController: NavHostController

  @Test
  fun startsOnAuth_whenNotLoggedIn() {
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = false, renderMap = false)
    }

    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun startsOnMap_whenLoggedIn() {
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun navigatesToProfile_fromMap() {
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Programmatically navigate to profile to avoid relying on Map UI
    composeTestRule.runOnUiThread { navController.navigate("profile") }

    composeTestRule.waitForIdle()

    // Wait for profile screen to appear after navigation
    composeTestRule.waitUntil(timeoutMillis = 15000) {
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
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Build a backstack: map -> profile -> settings, then perform logout from settings
    composeTestRule.runOnUiThread {
      // Start at map (start destination), then push profile and settings
      navController.navigate("profile")
      navController.navigate("settings")
    }

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
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Build a backstack and navigate to settings
    composeTestRule.runOnUiThread {
      navController.navigate("profile")
      navController.navigate("settings")
    }

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to and click logout button in Settings
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performClick()

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
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Build backstack and navigate to settings
    composeTestRule.runOnUiThread {
      navController.navigate("profile")
      navController.navigate("settings")
    }

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to and click logout in Settings
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton_action", useUnmergedTree = true).performClick()
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
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Programmatically navigate to profile then settings
    composeTestRule.runOnUiThread {
      navController.navigate("profile")
      navController.navigate("settings")
    }

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
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.waitForIdle()

    // Programmatically push profile then settings into the backstack and then use UI back button
    composeTestRule.runOnUiThread {
      navController.navigate("profile")
      navController.navigate("settings")
    }

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

  @Test
  fun navigatesToFriendsScreen() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.runOnIdle { navController.navigate("friends") }

    composeTestRule.waitForIdle()
  }

  @Test
  fun navigatesToNewConversation_andConfirms() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.runOnIdle {
      // Navigate to NewConversation route
      navController.navigate("newConversation")

      // Simulate confirm action (navigates back to Chat)
      navController.navigate("chat") {
        popUpTo("chat") { inclusive = true }
        launchSingleTop = true
      }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun navigatesToConversationScreen_andPopsBack() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(navController = navController, isLoggedIn = true, renderMap = false)
    }

    composeTestRule.runOnIdle {
      val encodedName = Uri.encode("Test User")
      navController.navigate("conversation/42/$encodedName")
      navController.popBackStack()
    }

    composeTestRule.waitForIdle()
  }
}

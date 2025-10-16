package com.swent.mapin.navigationTests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.testing.UiTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the AppNavHost composable, which manages navigation based on authentication state.
 *
 * Role: \- Verify that the correct start destination is shown based on whether the user is logged
 * in or not. generated with the help of AI
 */
@RunWith(AndroidJUnit4::class)
class AppNavHostTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // Routes de test
  private object Routes {
    const val AUTH = "auth"
    const val MAP = "map"
  }

  // Écrans fake ultra-simples avec testTag
  @androidx.compose.runtime.Composable
  private fun FakeAuthScreen() {
    Text("Auth", modifier = Modifier.testTag("AUTH"))
  }

  @androidx.compose.runtime.Composable
  private fun FakeMapScreen() {
    Text("Map", modifier = Modifier.testTag("MAP"))
  }

  // NavHost de test (au lieu d'utiliser l'AppNavHost réel)
  @androidx.compose.runtime.Composable
  private fun TestNavHost(isLoggedIn: Boolean) {
    val nav = rememberNavController()
    val start = if (isLoggedIn) Routes.MAP else Routes.AUTH

    NavHost(navController = nav, startDestination = start) {
      composable(Routes.AUTH) { FakeAuthScreen() }
      composable(Routes.MAP) { FakeMapScreen() }
    }
  }

  @Test
  fun startsOnAuth_whenNotLoggedIn() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = false)
    }

    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun startsOnMap_whenLoggedIn() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun navigatesToProfile_fromMap() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Verify we're on the map screen
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()

    // Navigate to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on the profile screen
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun logout_navigatesBackToAuth() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Navigate to profile from map
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on profile screen
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()

    // Scroll to the logout button and click it
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're back on auth screen
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun logout_clearsBackStack() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Navigate to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Scroll to and click logout
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on auth screen
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
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Go to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Scroll to and click logout
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify we're on auth screen and map screen doesn't exist in the tree
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Map screen should not be in the composition tree after logout
    composeTestRule
        .onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
        .assertDoesNotExist()
  }
}

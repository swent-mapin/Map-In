package com.swent.mapin.navigationTests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
      AppNavHost(navController = rememberNavController(), isLoggedIn = false)
    }

    composeTestRule.waitForIdle()

    // AUTH_SCREEN is the root element, no need to scroll to it
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun authContinueNavigatesToMap() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = false)
    }

    composeTestRule.waitForIdle()

    // Click on continue
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_CONTINUE_BUTTON, useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // Map should be visible
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun startsOnMap_whenLoggedIn() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }
}

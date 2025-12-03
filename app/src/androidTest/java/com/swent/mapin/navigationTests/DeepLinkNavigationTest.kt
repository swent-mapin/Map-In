package com.swent.mapin.navigationTests

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.chat.ChatScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for deep link navigation.
 *
 * Tests verify that deep links correctly navigate to the intended screens and that tab selection
 * works properly for the Friends screen.
 *
 * Assisted by AI
 */
@RunWith(AndroidJUnit4::class)
class DeepLinkNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun deepLinkToFriendRequests_navigatesToFriendsScreen() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLink = "mapin://friendRequests/request123",
          renderMap = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 6000) {
      composeTestRule
          .onAllNodesWithTag("friendsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("friendsScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun deepLinkToFriendRequests_selectsRequestsTab() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLink = "mapin://friendRequests/request123",
          renderMap = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 6000) {
      composeTestRule
          .onAllNodesWithTag("friendsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("tabREQUESTS", useUnmergedTree = true).assertExists()
  }

  @Test
  fun deepLinkToEvent_navigatesToMapScreen() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLink = "mapin://events/event456",
          renderMap = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 6000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun deepLinkToMessagesWithoutId_navigatesToChatScreen() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLink = "mapin://messages",
          renderMap = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 6000) {
      composeTestRule
          .onAllNodesWithTag(ChatScreenTestTags.CHAT_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.CHAT_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun deepLinkToMap_navigatesToMapScreen() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLink = "mapin://map",
          renderMap = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 6000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun invalidDeepLinkScheme_doesNotNavigate() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLink = "https://example.com/invalid",
          renderMap = false)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun unknownDeepLinkHost_doesNotNavigate() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLink = "mapin://unknown/path",
          renderMap = false)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun nullDeepLink_startsAtDefaultScreen() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController, isLoggedIn = true, deepLink = null, renderMap = false)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }
}

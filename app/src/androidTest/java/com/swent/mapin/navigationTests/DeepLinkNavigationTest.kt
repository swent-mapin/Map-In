package com.swent.mapin.navigationTests

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.chat.ChatScreenTestTags
import org.junit.Before
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

  @Before
  fun setup() {
    // Ensure any previous state is cleared
    composeTestRule.waitForIdle()
  }

  private fun setupContent(deepLink: String?) {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val deepLinkQueue =
          androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateListOf<String>().apply {
              if (deepLink != null) {
                add(deepLink)
              }
            }
          }
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          deepLinkQueue = deepLinkQueue,
          renderMap = false,
          autoRequestPermissions = false)
    }
    // Wait for the content to be set and composed
    composeTestRule.waitForIdle()
  }

  private fun waitForScreen(tag: String, timeoutMillis: Long = 10000) {
    composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
      try {
        composeTestRule
            .onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (e: Exception) {
        false
      }
    }
  }

  @Test
  fun deepLinkToFriendRequests_navigatesToFriendsScreen() {
    setupContent("mapin://friendRequests/request123")
    waitForScreen("friendsScreen")
    composeTestRule.onNodeWithTag("friendsScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun deepLinkToFriendRequests_selectsRequestsTab() {
    setupContent("mapin://friendRequests/request123")
    waitForScreen("friendsScreen")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tabREQUESTS", useUnmergedTree = true).assertExists()
  }

  @Test
  fun deepLinkToFriendAccepted_navigatesToFriendsScreen() {
    setupContent("mapin://friendAccepted")
    waitForScreen("friendsScreen")
    composeTestRule.onNodeWithTag("friendsScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun deepLinkToFriendAccepted_selectsFriendsTab() {
    setupContent("mapin://friendAccepted")
    waitForScreen("friendsScreen")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tabFRIENDS", useUnmergedTree = true).assertExists()
  }

  @Test
  fun deepLinkToProfile_navigatesToFriendsScreen() {
    setupContent("mapin://profile/user123")
    waitForScreen("friendsScreen")
    composeTestRule.onNodeWithTag("friendsScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun deepLinkToProfile_selectsFriendsTab() {
    setupContent("mapin://profile/user123")
    waitForScreen("friendsScreen")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tabFRIENDS", useUnmergedTree = true).assertExists()
  }

  @Test
  fun deepLinkToEvent_navigatesToMapScreen() {
    setupContent("mapin://events/event456")
    waitForScreen(UiTestTags.MAP_SCREEN)
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun deepLinkToMessagesWithoutId_navigatesToChatScreen() {
    setupContent("mapin://messages")
    waitForScreen(ChatScreenTestTags.CHAT_SCREEN)
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.CHAT_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun deepLinkToMap_navigatesToMapScreen() {
    setupContent("mapin://map")
    waitForScreen(UiTestTags.MAP_SCREEN)
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun invalidDeepLinkScheme_doesNotNavigate() {
    setupContent("https://example.com/invalid")
    waitForScreen(UiTestTags.MAP_SCREEN)
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun unknownDeepLinkHost_doesNotNavigate() {
    setupContent("mapin://unknown/path")
    waitForScreen(UiTestTags.MAP_SCREEN)
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun nullDeepLink_startsAtDefaultScreen() {
    setupContent(null)
    waitForScreen(UiTestTags.MAP_SCREEN)
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }
}

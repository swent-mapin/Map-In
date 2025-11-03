package com.swent.mapin.ui.friends

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.FriendshipStatus
import com.swent.mapin.model.UserProfile
import org.junit.Rule
import org.junit.Test

// Assisted by AI

/** Instrumentation tests for FriendsScreen - covers the 7 essential features. */
class FriendsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Test 1: Search tab displays search field
  @Test
  fun searchTab_displaysSearchField() {
    composeTestRule.setContent { MaterialTheme { FriendsScreen(onNavigateBack = {}) } }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithTag("searchTextField").assertIsDisplayed()
  }

  // Test 2: Alice searches "Bob" and sees "Add" button
  @Test
  fun searchTab_displaysAddButtonForNewUsers() {
    val results =
        listOf(
            com.swent.mapin.model.SearchResultWithStatus(
                userProfile = UserProfile(userId = "1", name = "Bob", bio = "Hi"),
                hasPendingRequest = false))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(onNavigateBack = {}, searchQuery = "bob", searchResults = results)
      }
    }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sendRequestButton").assertExists()
    composeTestRule.onNodeWithText("Add").assertExists()
  }

  // Test 3: After clicking "Add", button becomes "Sent"
  @Test
  fun searchTab_sendRequestButtonChangesAfterClick() {
    val results =
        listOf(
            com.swent.mapin.model.SearchResultWithStatus(
                userProfile = UserProfile(userId = "1", name = "Eve", bio = "Developer"),
                hasPendingRequest = false))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(onNavigateBack = {}, searchQuery = "e", searchResults = results)
      }
    }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sendRequestButton").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("requestSentButton").assertExists()
    composeTestRule.onNodeWithText("Sent").assertExists()
  }

  // Test 4: Bob sees request in "Requests" tab
  @Test
  fun requestsTab_displaysPendingRequests() {
    val requests =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Alice", bio = "Nice to meet you"),
                friendshipStatus = FriendshipStatus.PENDING,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = {}, pendingRequests = requests) }
    }

    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    composeTestRule.onNodeWithText("Nice to meet you").assertIsDisplayed()
  }

  // Test 5: Bob accepts request
  @Test
  fun requestsTab_acceptButtonWorks() {
    var acceptedRequestId: String? = null
    val requests =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Alice"),
                friendshipStatus = FriendshipStatus.PENDING,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(
            onNavigateBack = {},
            pendingRequests = requests,
            onAcceptRequest = { acceptedRequestId = it })
      }
    }

    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    composeTestRule.onNodeWithTag("acceptButton").performClick()
    assert(acceptedRequestId == "req1")
  }

  // Test 6: Alice and Bob see each other in "Friends"
  @Test
  fun friendsListTab_displaysFriendsList() {
    val friends =
        listOf(
            FriendWithProfile(
                userProfile =
                    UserProfile(userId = "1", name = "Alice", location = "Paris", bio = "Hello"),
                friendshipStatus = FriendshipStatus.ACCEPTED,
                requestId = "req1"),
            FriendWithProfile(
                userProfile =
                    UserProfile(userId = "2", name = "Bob", location = "London", bio = "Hi"),
                friendshipStatus = FriendshipStatus.ACCEPTED,
                requestId = "req2"))

    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = {}, friends = friends) }
    }

    composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    composeTestRule.onNodeWithText("Paris").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    composeTestRule.onNodeWithText("London").assertIsDisplayed()
  }

  // Test 7: Alice can remove Bob
  @Test
  fun friendsListTab_removeFriendConfirmationWorks() {
    var removedUserId: String? = null
    val friends =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Bob"),
                friendshipStatus = FriendshipStatus.ACCEPTED,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(
            onNavigateBack = {}, friends = friends, onRemoveFriend = { removedUserId = it })
      }
    }

    composeTestRule.onNodeWithTag("removeFriendButton").performClick()
    composeTestRule.onNodeWithText("Remove").performClick()
    assert(removedUserId == "1")
  }

  // Additional essential tests for UI stability

  @Test
  fun friendsScreen_displaysAllTabs() {
    composeTestRule.setContent { MaterialTheme { FriendsScreen(onNavigateBack = {}) } }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("friendsScreen").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("friendsTabRow").assertExists()
    composeTestRule.onNodeWithTag("tabFRIENDS").assertExists()
    composeTestRule.onNodeWithTag("tabREQUESTS").assertExists()
    composeTestRule.onNodeWithTag("tabSEARCH").assertExists()
  }

  @Test
  fun friendsScreen_tabSwitchingWorks() {
    composeTestRule.setContent { MaterialTheme { FriendsScreen(onNavigateBack = {}) } }

    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    composeTestRule.onNodeWithTag("requestsTab").assertIsDisplayed()

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithTag("searchTab").assertIsDisplayed()

    composeTestRule.onNodeWithTag("tabFRIENDS").performClick()
    composeTestRule.onNodeWithTag("friendsListTab").assertIsDisplayed()
  }

  @Test
  fun searchTab_searchQueryChangeWorks() {
    var searchQuery by mutableStateOf("")
    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(
            onNavigateBack = {},
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it })
      }
    }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithTag("searchTextField").performTextInput("test")
    composeTestRule.waitForIdle()
    assert(searchQuery == "test")
  }
}

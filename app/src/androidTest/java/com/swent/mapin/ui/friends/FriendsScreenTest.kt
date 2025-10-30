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

/**
 * Instrumentation test suite for the FriendsScreen composable.
 *
 * High-level description:
 * - These tests mount the FriendsScreen composable using a Compose test rule.
 * - Each test follows the pattern: setup UI state -> interact (optional) -> assert expectations.
 * - Tests use testTag identifiers defined in the composable to locate elements reliably.
 *
 * Testing goals covered here:
 * - Verify tab visibility and tab switching behavior.
 * - Validate friends list rendering, remove flow (dialog + callbacks).
 * - Validate pending requests rendering and accept/reject flows.
 * - Validate search UI behavior, query updates, empty results and send request flows.
 * - Cover edge cases such as long names, empty fields and multiple items.
 */
class FriendsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // ==================== FriendsScreen Tests ====================

  // Test: Ensure main tabs (Friends, Requests, Search) are present.
  // Setup: Mount the FriendsScreen in a minimal theme wrapper.
  // Action: none
  // Assertion: Tab row and individual tabs are displayed.
  @Test
  fun friendsScreen_displaysAllTabs() {
    composeTestRule.setContent { MaterialTheme { FriendsScreen(onNavigateBack = {}) } }
    composeTestRule.waitForIdle()

    // Wait until the friends screen is fully rendered
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("friendsScreen").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("friendsTabRow").assertExists()
    composeTestRule.onNodeWithTag("tabFRIENDS").assertExists()
    composeTestRule.onNodeWithTag("tabREQUESTS").assertExists()
    composeTestRule.onNodeWithTag("tabSEARCH").assertExists()
  }

  // Test: Verify the navigation (back) button triggers the provided callback.
  // Setup: Provide a mutable flag and pass a lambda that sets it true.
  // Action: Perform click on the back button node.
  // Assertion: The flag must be true after the click.
  @Test
  fun friendsScreen_backButtonWorks() {
    var backPressed = false

    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = { backPressed = true }) }
    }

    composeTestRule.onNodeWithTag("backButton").performClick()
    assert(backPressed)
  }

  // Test: Check tab switching behavior between Requests, Search and Friends.
  // Setup: Mount screen.
  // Action: Click each tab in sequence.
  // Assertion: The corresponding tab content node is displayed after each click.
  @Test
  fun friendsScreen_tabSwitchingWorks() {
    composeTestRule.setContent { MaterialTheme { FriendsScreen(onNavigateBack = {}) } }

    // Switch to Requests tab
    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    composeTestRule.onNodeWithTag("requestsTab").assertIsDisplayed()

    // Switch to Search tab
    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithTag("searchTab").assertIsDisplayed()

    // Switch back to Friends tab
    composeTestRule.onNodeWithTag("tabFRIENDS").performClick()
    composeTestRule.onNodeWithTag("friendsListTab").assertIsDisplayed()
  }

  // ==================== Friends List Tab Tests ====================

  // Test: Verify empty state messaging is shown when friend list is empty.
  // Use an explicitly typed empty list to avoid inference issues in tests.
  @Test
  fun friendsListTab_displaysEmptyState() {
    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = {}, friends = emptyList<FriendWithProfile>()) }
    }

    composeTestRule.onNodeWithText("No friends yet").assertIsDisplayed()
    composeTestRule.onNodeWithText("Search for friends to add them").assertIsDisplayed()
  }

  // Test: Verify that a populated friends list renders each friend's details.
  // Setup: two FriendWithProfile entries with name and location fields.
  // Assertion: names and locations are visible in the UI.
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

  // Test: Removing a friend opens a confirmation dialog with correct content.
  // Action: click the remove button for a friend.
  // Assertion: the dialog title and the dynamic message containing the friend's name are shown.
  @Test
  fun friendsListTab_removeFriendButtonOpensDialog() {
    val friends =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Alice"),
                friendshipStatus = FriendshipStatus.ACCEPTED,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = {}, friends = friends) }
    }

    composeTestRule.onNodeWithTag("removeFriendButton").performClick()
    composeTestRule.onNodeWithText("Remove Friend").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Are you sure you want to remove Alice from your friends?")
        .assertIsDisplayed()
  }

  // Test: Confirming removal triggers provided callback with correct user id.
  // Setup: capture the id passed to onRemoveFriend lambda.
  // Action: open dialog and click the Remove button.
  // Assertion: removedUserId must equal the friend's id.
  @Test
  fun friendsListTab_removeFriendConfirmationWorks() {
    var removedUserId: String? = null
    val friends =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Alice"),
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

  // Test: Cancelling removal does not call the onRemoveFriend callback.
  @Test
  fun friendsListTab_removeFriendCancellationWorks() {
    var removedUserId: String? = null
    val friends =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Alice"),
                friendshipStatus = FriendshipStatus.ACCEPTED,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(
            onNavigateBack = {}, friends = friends, onRemoveFriend = { removedUserId = it })
      }
    }

    composeTestRule.onNodeWithTag("removeFriendButton").performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()
    assert(removedUserId == null)
  }

  // ==================== Requests Tab Tests ====================

  // Test: Empty pending requests state shows proper messaging.
  @Test
  fun requestsTab_displaysEmptyState() {
    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(onNavigateBack = {}, pendingRequests = emptyList<FriendWithProfile>())
      }
    }

    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    composeTestRule.onNodeWithText("No pending requests").assertIsDisplayed()
    composeTestRule.onNodeWithText("Friend requests will appear here").assertIsDisplayed()
  }

  // Test: Render list of pending requests including bio text when present.
  @Test
  fun requestsTab_displaysPendingRequests() {
    val requests =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Charlie", bio = "Nice to meet you"),
                friendshipStatus = FriendshipStatus.PENDING,
                requestId = "req1"),
            FriendWithProfile(
                userProfile = UserProfile(userId = "2", name = "Diana", bio = "Hello there"),
                friendshipStatus = FriendshipStatus.PENDING,
                requestId = "req2"))

    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = {}, pendingRequests = requests) }
    }

    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    composeTestRule.onNodeWithText("Charlie").assertIsDisplayed()
    composeTestRule.onNodeWithText("Nice to meet you").assertIsDisplayed()
    composeTestRule.onNodeWithText("Diana").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hello there").assertIsDisplayed()
  }

  // Test: Accepting a request triggers onAcceptRequest with the correct request id.
  @Test
  fun requestsTab_acceptButtonWorks() {
    var acceptedRequestId: String? = null
    val requests =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Charlie"),
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

  // Test: Rejecting a request triggers onRejectRequest with the correct request id.
  @Test
  fun requestsTab_rejectButtonWorks() {
    var rejectedRequestId: String? = null
    val requests =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Charlie"),
                friendshipStatus = FriendshipStatus.PENDING,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(
            onNavigateBack = {},
            pendingRequests = requests,
            onRejectRequest = { rejectedRequestId = it })
      }
    }

    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    composeTestRule.onNodeWithTag("rejectButton").performClick()
    assert(rejectedRequestId == "req1")
  }

  // ==================== Search Tab Tests ====================

  // Test: Initial empty search state (no query) shows guidance text.
  @Test
  fun searchTab_displaysInitialEmptyState() {
    composeTestRule.setContent { MaterialTheme { FriendsScreen(onNavigateBack = {}) } }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithText("Search for friends").assertIsDisplayed()
    composeTestRule.onNodeWithText("Enter a name or email to find friends").assertIsDisplayed()
  }

  // Test: Search input field exists and is focusable/usable.
  @Test
  fun searchTab_displaysSearchField() {
    composeTestRule.setContent { MaterialTheme { FriendsScreen(onNavigateBack = {}) } }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithTag("searchTextField").assertIsDisplayed()
  }

  // Test: Typing into the search field triggers the provided onSearchQueryChange callback.
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

  // Test: When search returns no results, the empty results messaging is displayed.
  @Test
  fun searchTab_displaysNoResultsState() {
    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(
            onNavigateBack = {}, searchQuery = "xyz", searchResults = emptyList<UserProfile>())
      }
    }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
    composeTestRule.onNodeWithText("Try a different search term").assertIsDisplayed()
  }

  // Test: Search results render a list of UserProfile items with names and bios.
  @Test
  fun searchTab_displaysSearchResults() {
    val results =
        listOf(
            UserProfile(userId = "1", name = "Eve", bio = "Developer"),
            UserProfile(userId = "2", name = "Frank", bio = "Designer"))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(onNavigateBack = {}, searchQuery = "e", searchResults = results)
      }
    }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithText("Eve").assertIsDisplayed()
    composeTestRule.onNodeWithText("Developer").assertIsDisplayed()
    composeTestRule.onNodeWithText("Frank").assertIsDisplayed()
    composeTestRule.onNodeWithText("Designer").assertIsDisplayed()
  }

  // Test: The send friend request button calls the provided callback with correct user id.
  @Test
  fun searchTab_sendRequestButtonWorks() {
    var sentToUserId: String? = null
    val results = listOf(UserProfile(userId = "1", name = "Eve", bio = "Developer"))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(
            onNavigateBack = {},
            searchQuery = "e",
            searchResults = results,
            onSendFriendRequest = { sentToUserId = it })
      }
    }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.onNodeWithTag("sendRequestButton").performClick()
    composeTestRule.waitForIdle()
    assert(sentToUserId == "1")
  }

  // Test: After clicking the add/send button, its visual state changes to "Sent" and the
  // requestSentButton node is visible (ensuring UI state update).
  @Test
  fun searchTab_sendRequestButtonChangesAfterClick() {
    val results = listOf(UserProfile(userId = "1", name = "Eve", bio = "Developer"))

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(onNavigateBack = {}, searchQuery = "e", searchResults = results)
      }
    }

    composeTestRule.onNodeWithTag("tabSEARCH").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sendRequestButton").assertExists()
    composeTestRule.onNodeWithTag("sendRequestButton").performClick()
    composeTestRule.waitForIdle()
    // Verify that the button switched to the sent state
    composeTestRule.onNodeWithTag("requestSentButton").assertExists()
    composeTestRule.onNodeWithText("Sent").assertExists()
  }

  // ==================== Edge Cases Tests ====================

  // Test: Long names should still appear (possibly truncated) without causing errors.
  @Test
  fun friendsScreen_handlesLongNames() {
    val friends =
        listOf(
            FriendWithProfile(
                userProfile =
                    UserProfile(
                        userId = "1",
                        name = "Very Long Name That Should Be Truncated With Ellipsis",
                        location = "Very Long Location Name"),
                friendshipStatus = FriendshipStatus.ACCEPTED,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = {}, friends = friends) }
    }
    composeTestRule.waitForIdle()

    // Wait until the friends list tab is displayed
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("friendsListTab").fetchSemanticsNodes().isNotEmpty()
    }

    // Check that the long name exists (with substring match since it may be truncated)
    composeTestRule.onNodeWithText("Very Long Name", substring = true).assertExists()
  }

  // Test: Empty bio and location fields should not crash rendering; name still displays.
  @Test
  fun friendsScreen_handlesEmptyBioAndLocation() {
    val friends =
        listOf(
            FriendWithProfile(
                userProfile = UserProfile(userId = "1", name = "Alice", location = "", bio = ""),
                friendshipStatus = FriendshipStatus.ACCEPTED,
                requestId = "req1"))

    composeTestRule.setContent {
      MaterialTheme { FriendsScreen(onNavigateBack = {}, friends = friends) }
    }

    composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
  }

  // Test: Several friends and requests are rendered correctly in their respective tabs.
  @Test
  fun friendsScreen_handlesMultipleRequestsAndFriends() {
    val friends =
        List(5) { index ->
          FriendWithProfile(
              userProfile = UserProfile(userId = "friend$index", name = "Friend $index"),
              friendshipStatus = FriendshipStatus.ACCEPTED,
              requestId = "req$index")
        }

    val requests =
        List(3) { index ->
          FriendWithProfile(
              userProfile = UserProfile(userId = "request$index", name = "Request $index"),
              friendshipStatus = FriendshipStatus.PENDING,
              requestId = "reqPending$index")
        }

    composeTestRule.setContent {
      MaterialTheme {
        FriendsScreen(onNavigateBack = {}, friends = friends, pendingRequests = requests)
      }
    }

    // Check friends tab
    friends.forEach { friend ->
      composeTestRule.onNodeWithText(friend.userProfile.name).assertIsDisplayed()
    }

    // Check requests tab
    composeTestRule.onNodeWithTag("tabREQUESTS").performClick()
    requests.forEach { request ->
      composeTestRule.onNodeWithText(request.userProfile.name).assertIsDisplayed()
    }
  }
}

package com.swent.mapin.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.UserProfile
import com.swent.mapin.ui.friends.FriendsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@Composable
fun NewConversationScreenForTest(
    conversationViewModel: ConversationViewModel,
    friendsViewModel: FriendsViewModel,
    onNavigateBack: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
  NewConversationScreen(
      conversationViewModel = conversationViewModel,
      friendsViewModel = friendsViewModel,
      onNavigateBack = onNavigateBack,
      onConfirm = onConfirm)
}

class NewConversationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun sampleFriends(): List<FriendWithProfile> {
    val f1 = FriendWithProfile(userProfile = UserProfile(userId = "1", name = "Alice"))
    val f2 = FriendWithProfile(userProfile = UserProfile(userId = "2", name = "Bob"))
    val f3 = FriendWithProfile(userProfile = UserProfile(userId = "3", name = "Charlie"))
    return listOf(f1, f2, f3)
  }

  @Test
  fun showsEmptyState_whenNoFriendsAvailable() {
    val mockFriendsViewModel = mockk<FriendsViewModel>(relaxed = true)
    every { mockFriendsViewModel.friends } returns MutableStateFlow(emptyList())

    composeTestRule.setContent { NewConversationScreen(friendsViewModel = mockFriendsViewModel) }

    composeTestRule.onNodeWithText("No friends yet").assertIsDisplayed()
  }

  @Test
  fun displaysAllFriends_andTogglesSelection() {
    val mockFriendsViewModel = mockk<FriendsViewModel>(relaxed = true)
    every { mockFriendsViewModel.friends } returns MutableStateFlow(sampleFriends())

    composeTestRule.setContent { NewConversationScreen(friendsViewModel = mockFriendsViewModel) }

    // Alice and Bob visible
    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Alice")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Bob")
        .assertIsDisplayed()

    // Select Alice
    val aliceTag = "${NewConversationScreenTestTags.FRIEND_ITEM}_Alice"
    composeTestRule.onNodeWithTag(aliceTag).performClick()
    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()

    // Deselect Alice
    composeTestRule.onNodeWithTag(aliceTag).performClick()
    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).assertDoesNotExist()
  }

  @Test
  fun confirmSingleFriend_createsConversationAndCallsOnConfirm() {
    val mockFriendsViewModel = mockk<FriendsViewModel>(relaxed = true)
    val mockConversationViewModel = mockk<ConversationViewModel>(relaxed = true)

    every { mockFriendsViewModel.friends } returns MutableStateFlow(sampleFriends())

    // 🔑 IMPORTANT: stub suspend call
    coEvery { mockConversationViewModel.getExistingConversation(any()) } returns null

    var confirmed = false

    composeTestRule.setContent {
      NewConversationScreen(
          friendsViewModel = mockFriendsViewModel,
          conversationViewModel = mockConversationViewModel,
          onConfirm = { confirmed = true })
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Alice")
        .performClick()

    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).performClick()

    // ⏳ wait for coroutine
    composeTestRule.waitForIdle()

    Assert.assertTrue(confirmed)
    coVerify { mockConversationViewModel.createConversation(any()) }
  }

  @Test
  fun confirmMultipleFriends_opensDialog_andCanCancel() {
    val mockFriendsViewModel = mockk<FriendsViewModel>(relaxed = true)
    val mockConversationViewModel = mockk<ConversationViewModel>(relaxed = true)

    every { mockFriendsViewModel.friends } returns MutableStateFlow(sampleFriends())

    composeTestRule.setContent {
      NewConversationScreenForTest(
          friendsViewModel = mockFriendsViewModel,
          conversationViewModel = mockConversationViewModel)
    }

    // Select Alice and Bob (group chat)
    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Alice")
        .performClick()
    composeTestRule.onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Bob").performClick()

    // Confirm → should open dialog
    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).performClick()

    // Dialog visible
    composeTestRule
        .onNodeWithTag(NewConversationScreenTestTags.GROUP_NAME_DIALOG_TEXT)
        .assertIsDisplayed()

    // Cancel
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule
        .onNodeWithTag(NewConversationScreenTestTags.GROUP_NAME_DIALOG_TEXT)
        .assertIsNotDisplayed()
  }

  @Test
  fun confirmMultipleFriends_entersGroupName_andCreatesGroup() {
    val mockFriendsViewModel = mockk<FriendsViewModel>(relaxed = true)
    val mockConversationViewModel = mockk<ConversationViewModel>(relaxed = true)

    every { mockFriendsViewModel.friends } returns MutableStateFlow(sampleFriends())
    coEvery { mockConversationViewModel.getExistingConversation(any()) } returns null

    var confirmed = false

    composeTestRule.setContent {
      NewConversationScreen(
          friendsViewModel = mockFriendsViewModel,
          conversationViewModel = mockConversationViewModel,
          onConfirm = { confirmed = true })
    }

    // Select Alice, Bob, Charlie
    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Alice")
        .performClick()
    composeTestRule.onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Bob").performClick()
    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Charlie")
        .performClick()

    // Confirm → open dialog
    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(NewConversationScreenTestTags.GROUP_NAME_DIALOG_TEXT)
        .assertIsDisplayed()

    // Enter group name
    composeTestRule.onNode(hasSetTextAction()).performTextInput("MyGroup")

    // Press OK
    composeTestRule.onNodeWithText("OK").performClick()

    // ⏳ wait for coroutine
    composeTestRule.waitForIdle()

    Assert.assertTrue(confirmed)
    coVerify { mockConversationViewModel.createConversation(any()) }
  }

  @Test
  fun clickingBack_callsOnNavigateBack() {
    val mockFriendsViewModel = mockk<FriendsViewModel>(relaxed = true)
    every { mockFriendsViewModel.friends } returns MutableStateFlow(sampleFriends())

    var backCalled = false

    composeTestRule.setContent {
      NewConversationScreen(
          friendsViewModel = mockFriendsViewModel, onNavigateBack = { backCalled = true })
    }

    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.BACK_BUTTON).performClick()
    assert(backCalled)
  }
}

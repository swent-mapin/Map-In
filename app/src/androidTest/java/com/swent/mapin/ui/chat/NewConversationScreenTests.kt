package com.swent.mapin.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.UserProfile
import org.junit.Rule
import org.junit.Test

class NewConversationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun sampleFriends(): List<FriendWithProfile> {
    val f1 =
        FriendWithProfile(
            userProfile =
                UserProfile(
                    userId = "1",
                    name = "Alice",
                ))
    val f2 =
        FriendWithProfile(
            userProfile =
                UserProfile(
                    userId = "2",
                    name = "Bob",
                ))
    return listOf(f1, f2)
  }

  @Test
  fun screen_rendersAllUIElements() {
    composeTestRule.setContent { NewConversationScreen(friends = sampleFriends()) }

    composeTestRule
        .onNodeWithTag(NewConversationScreenTestTags.NEW_CONVERSATION_SCREEN)
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.BACK_BUTTON).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Alice")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${NewConversationScreenTestTags.FRIEND_ITEM}_Bob")
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON)
        .assertIsNotDisplayed()
  }

  @Test
  fun selectingFriend_showsConfirmButton_andCallsOnConfirm() {
    var confirmedFriends: List<FriendWithProfile> = emptyList()

    composeTestRule.setContent {
      NewConversationScreen(friends = sampleFriends(), onConfirm = { confirmedFriends = it })
    }

    val aliceTag = "${NewConversationScreenTestTags.FRIEND_ITEM}_Alice"
    composeTestRule.onNodeWithTag(aliceTag).performClick()

    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).performClick()

    assert(confirmedFriends.size == 1)
    assert(confirmedFriends.first().userProfile.name == "Alice")
  }

  @Test
  fun clickingFriend_togglesSelection_offAndOn() {
    composeTestRule.setContent { NewConversationScreen(friends = sampleFriends()) }

    val aliceTag = "${NewConversationScreenTestTags.FRIEND_ITEM}_Alice"

    composeTestRule.onNodeWithTag(aliceTag).performClick()

    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithTag(aliceTag).performClick()

    composeTestRule
        .onNodeWithTag(NewConversationScreenTestTags.CONFIRM_BUTTON)
        .assertIsNotDisplayed()
  }

  @Test
  fun clickingBack_callsOnNavigateBack() {
    var backCalled = false
    composeTestRule.setContent {
      NewConversationScreen(friends = sampleFriends(), onNavigateBack = { backCalled = true })
    }

    composeTestRule.onNodeWithTag(NewConversationScreenTestTags.BACK_BUTTON).performClick()

    assert(backCalled)
  }
}

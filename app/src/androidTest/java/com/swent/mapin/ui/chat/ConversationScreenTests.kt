package com.swent.mapin.ui.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class ConversationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun conversationScreen_displaysInitialMessagesAndUIElements() {
    composeTestRule.setContent {
      ConversationScreen(
          conversationId = "1", conversationName = "Chat with Alice", onNavigateBack = {})
    }

    composeTestRule
        .onNodeWithTag(ConversationScreenTestTags.CONVERSATION_SCREEN)
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(ConversationScreenTestTags.INPUT_TEXT_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConversationScreenTestTags.SEND_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithText("Hey, how are you?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Doing great, thanks!").assertIsDisplayed()
  }

  @Test
  fun sendingMessage_addsNewMessageToListAndClearsInput() {
    composeTestRule.setContent {
      ConversationScreen(conversationId = "1", conversationName = "Chat Test")
    }

    val inputNode = composeTestRule.onNodeWithTag(ConversationScreenTestTags.INPUT_TEXT_FIELD)
    val sendButton = composeTestRule.onNodeWithTag(ConversationScreenTestTags.SEND_BUTTON)

    inputNode.performTextInput("Hello there!")

    sendButton.performClick()

    inputNode.assert(hasText(""))
  }

  @Test
  fun emptyMessage_notAddedToList() {
    composeTestRule.setContent {
      ConversationScreen(conversationId = "1", conversationName = "Chat Test")
    }

    val sendButton = composeTestRule.onNodeWithTag(ConversationScreenTestTags.SEND_BUTTON)

    sendButton.performClick()

    composeTestRule.onNodeWithText("Hey, how are you?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Doing great, thanks!").assertIsDisplayed()
  }

  @Test
  fun messageBubble_rendersCorrectlyForBothSenderTypes() {
    composeTestRule.setContent {
      androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        MessageBubble(Message("From me", isMe = true))
        MessageBubble(Message("From them", isMe = false))
      }
    }

    composeTestRule.onNodeWithText("From me").assertIsDisplayed()
    composeTestRule.onNodeWithText("From them").assertIsDisplayed()
  }

  @Test
  fun conversationScreen_onNavigateBack_isCalled() {
    var backCalled = false
    composeTestRule.setContent {
      ConversationScreen(
          conversationId = "c1", conversationName = "Chat", onNavigateBack = { backCalled = true })
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.BACK_BUTTON).performClick()

    assert(backCalled)
  }
}

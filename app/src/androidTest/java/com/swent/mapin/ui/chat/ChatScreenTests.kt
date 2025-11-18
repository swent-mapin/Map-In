package com.swent.mapin.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

  @get:Rule val rule = createComposeRule()

  private fun sampleConversations() =
      listOf(
          Conversation(
              id = "c1",
              name = "Alice",
              participantIds = listOf("1", "2"),
              participants = emptyList(),
              lastMessage = "Hello"),
          Conversation(
              id = "c2",
              name = "Bob",
              participantIds = listOf("1", "3"),
              participants = emptyList(),
              lastMessage = ""))

  @Test
  fun topBar_backButton_callsNavigateBack() {
    var backClicked = false
    rule.setContent { MaterialTheme { ChatScreen(onNavigateBack = { backClicked = true }) } }

    rule.onNodeWithTag(ChatScreenTestTags.BACK_BUTTON).performClick()
    assert(backClicked)
  }

  @Test
  fun fab_click_callsOnNewConversation() {
    var fabClicked = false
    rule.setContent { MaterialTheme { ChatScreen(onNewConversation = { fabClicked = true }) } }

    rule.onNodeWithTag(ChatScreenTestTags.NEW_CONVERSATION_BUTTON).performClick()
    assert(fabClicked)
  }

  @Test
  fun emptyState_displayed_whenNoConversations() {
    val mockVm = mockk<ConversationViewModel>(relaxed = true)
    every { mockVm.userConversations } returns MutableStateFlow(emptyList())
    every { mockVm.observeConversations() } just Runs

    rule.setContent { MaterialTheme { ChatScreen(conversationViewModel = mockVm) } }

    rule.onNodeWithTag(ChatScreenTestTags.CHAT_SCREEN).assertIsDisplayed()
    rule
        .onNodeWithTag(ChatScreenTestTags.CHAT_EMPTY_TEXT)
        .assertIsDisplayed()
        .assertTextEquals("No conversations yet") // stringResource(R.string.empty_conversation)
  }

  @Test
  fun conversationList_displayed_and_click_callsOnOpenConversation() {
    val mockVm = mockk<ConversationViewModel>(relaxed = true)
    val conversations = sampleConversations()
    every { mockVm.userConversations } returns MutableStateFlow(conversations)
    every { mockVm.observeConversations() } just Runs

    val clicked = mutableListOf<Conversation>()

    rule.setContent {
      MaterialTheme {
        ChatScreen(conversationViewModel = mockVm, onOpenConversation = { clicked.add(it) })
      }
    }

    // Assert items displayed
    conversations.forEach { conversation ->
      rule
          .onNodeWithTag("${ChatScreenTestTags.CONVERSATION_ITEM}_${conversation.id}")
          .assertIsDisplayed()
    }

    // Click the first conversation
    val first = conversations.first()
    rule.onNodeWithTag("${ChatScreenTestTags.CONVERSATION_ITEM}_${first.id}").performClick()
    assert(clicked.contains(first))
  }

  @Test
  fun conversationItem_showsPlaceholder_whenNoProfilePicture() {
    val conversation =
        Conversation(
            id = "c3", name = "Charlie", participantIds = emptyList(), profilePictureUrl = null)
    rule.setContent { MaterialTheme { ConversationItem(conversation) } }

    rule.onNodeWithContentDescription("DefaultProfile").assertIsDisplayed()
    rule.onNodeWithText("Charlie").assertIsDisplayed()
    rule.onNodeWithText("No messages yet").assertIsDisplayed()
  }

  @Test
  fun conversationItem_showsLastMessage_whenExists() {
    val conversation =
        Conversation(
            id = "c4", name = "Dana", participantIds = emptyList(), lastMessage = "Hi there")
    rule.setContent { MaterialTheme { ConversationItem(conversation) } }

    rule.onNodeWithText("Dana").assertIsDisplayed()
    rule.onNodeWithText("Hi there").assertIsDisplayed()
  }
}

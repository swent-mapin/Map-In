package com.swent.mapin.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

  private val sampleConversations = LocalChatFriendsRepository.getAllConversations()
  @get:Rule val rule = createComposeRule()

  @Test
  fun topBar_backButton_callsNavigateBack() {
    var backClicked = false
    rule.setContent {
      MaterialTheme {
        ChatScreen(allConversations = emptyList(), onNavigateBack = { backClicked = true })
      }
    }

    rule.onNodeWithTag("backButton").performClick()
    assert(backClicked)
  }

  @Test
  fun fab_click_callsOnNewConversation() {
    var fabClicked = false
    rule.setContent {
      MaterialTheme {
        ChatScreen(allConversations = emptyList(), onNewConversation = { fabClicked = true })
      }
    }

    rule.onNodeWithTag(ChatScreenTestTags.NEW_CONVERSATION_BUTTON).performClick()
    assert(fabClicked)
  }

  @Test
  fun emptyState_displayed_whenNoConversations() {
    rule.setContent { MaterialTheme { ChatScreen(allConversations = emptyList()) } }

    rule.onNodeWithTag(ChatScreenTestTags.CHAT_SCREEN).assertIsDisplayed()
    rule
        .onNodeWithTag(ChatScreenTestTags.CHAT_EMPTY_TEXT)
        .assertIsDisplayed()
        .assertTextEquals("No conversations yet")
  }

  @Test
  fun conversationList_displayed_whenSampleConversationsExist() {
    var clickedConversationId: String? = null

    rule.setContent {
      MaterialTheme {
        ChatScreen(
            allConversations = sampleConversations,
            onOpenConversation = { clickedConversationId = it.id })
      }
    }

    val createdConversations = sampleConversations.filter { it.createdByCurrentUser }

    createdConversations.forEach { conversation ->
      rule
          .onNodeWithTag("${ChatScreenTestTags.CONVERSATION_ITEM}_${conversation.id}")
          .assertIsDisplayed()
    }

    val first = createdConversations.first()
    rule.onNodeWithTag("${ChatScreenTestTags.CONVERSATION_ITEM}_${first.id}").performClick()
    assert(clickedConversationId == first.id)
  }
}

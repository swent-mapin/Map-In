package com.swent.mapin.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.FriendshipStatus
import com.swent.mapin.model.UserProfile
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

  private val friend1 =
      FriendWithProfile(
          UserProfile(name = "Nathan", bio = "Chill guy", hobbies = listOf("Surf")),
          friendshipStatus = FriendshipStatus.ACCEPTED,
          "")
  private val friend2 =
      FriendWithProfile(
          UserProfile(name = "Alex", bio = "Photographer", hobbies = listOf("Coffee")),
          friendshipStatus = FriendshipStatus.ACCEPTED,
          "")
  private val friend3 =
      FriendWithProfile(
          UserProfile(name = "Zoe", bio = "Runner", hobbies = listOf("Music")),
          friendshipStatus = FriendshipStatus.ACCEPTED,
          "")

  val friendList = listOf(friend1, friend2, friend3)

  private val sampleConversations =
      listOf(
          Conversation("c1", "Nathan", listOf(friend1), "Hey there!", true),
          Conversation("c2", "Alex", listOf(friend2), "Shared a photo", false),
          Conversation("c3", "Zoe", listOf(friend3), "Let's meet up!", true))

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
        .assertTextEquals(
            "No conversations yet") // Make sure this matches the actual string in your app
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

    // Only conversations created by the current user appear in ChatScreen
    val createdConversations = sampleConversations.filter { it.createdByCurrentUser }

    createdConversations.forEach { conversation ->
      rule
          .onNodeWithTag("${ChatScreenTestTags.CONVERSATION_ITEM}_${conversation.id}")
          .assertIsDisplayed()
    }

    // Click the first conversation
    val first = createdConversations.first()
    rule.onNodeWithTag("${ChatScreenTestTags.CONVERSATION_ITEM}_${first.id}").performClick()
    assert(clickedConversationId == first.id)
  }
}

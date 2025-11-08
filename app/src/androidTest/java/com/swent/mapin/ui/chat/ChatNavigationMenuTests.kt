package com.swent.mapin.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ChatBottomBarTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun navigationBar_renders() {
    rule.setContent {
      MaterialTheme { ChatBottomBar(selectedTab = ChatTab.Chats, onTabSelected = {}) }
    }

    rule.onNodeWithTag(ChatScreenTestTags.CHAT_BOTTOM_BAR).assertIsDisplayed()
  }

  @Test
  fun navigationBarItem_click_SameTabDoesNotTriggersCallback() {
    var selectedTab: ChatTab? = null

    rule.setContent {
      MaterialTheme {
        ChatBottomBar(selectedTab = ChatTab.Chats, onTabSelected = { tab -> selectedTab = tab })
      }
    }

    rule.onNodeWithTag(ChatScreenTestTags.CHAT_BOTTOM_BAR).performClick()

    assert(selectedTab != ChatTab.Chats)
  }

  @Test
  fun navigationBarItem_label_isDisplayed() {
    rule.setContent {
      MaterialTheme { ChatBottomBar(selectedTab = ChatTab.Chats, onTabSelected = {}) }
    }

    rule
        .onNodeWithTag("${ChatScreenTestTags.CHAT_BOTTOM_BAR_ITEM}_${ChatTab.Chats.name}")
        .assertIsDisplayed()
  }
}

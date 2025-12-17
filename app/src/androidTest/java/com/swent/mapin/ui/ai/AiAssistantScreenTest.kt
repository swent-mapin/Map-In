package com.swent.mapin.ui.ai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.ui.theme.MapInTheme
import org.junit.Rule
import org.junit.Test

// Assisted by AI

/**
 * UI tests for AiAssistantScreen.
 *
 * These tests verify the static UI elements and basic interactions. Since the screen depends on
 * real services (TTS, STT, AI), we test the UI components in isolation using the static fallback
 * content.
 */
class AiAssistantScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun aiAssistantScreen_rendersCorrectly() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify main screen elements are displayed
    composeTestRule.onNodeWithTag("aiAssistantScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("microphoneButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("statusBanner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("conversationList").assertIsDisplayed()
  }

  @Test
  fun topBar_displaysCorrectTitle() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithText("AI Assistant").assertIsDisplayed()
  }

  @Test
  fun backButton_triggersNavigationCallback() {
    var backClicked = false

    composeTestRule.setContent {
      MapInTheme { AiAssistantScreen(onNavigateBack = { backClicked = true }) }
    }

    composeTestRule.onNodeWithTag("backButton").performClick()
    assert(backClicked)
  }

  @Test
  fun microphoneButton_isDisplayed() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify microphone button exists
    composeTestRule.onNodeWithTag("micButton").assertIsDisplayed()
  }

  @Test
  fun helpCard_displaysInitially() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("helpCard").assertIsDisplayed()
    composeTestRule.onNodeWithText("How to use the assistant").assertIsDisplayed()
  }

  @Test
  fun helpCard_containsExampleQueries() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify help text contains example queries (using substring match)
    composeTestRule
        .onNode(hasText("I'm looking for a concert tonight", substring = true))
        .assertIsDisplayed()
  }

  @Test
  fun statusBanner_showsReadyState_initially() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithText("💬 Ready to listen").assertIsDisplayed()
  }

  @Test
  fun conversationList_isDisplayed() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify LazyColumn is displayed
    composeTestRule.onNodeWithTag("conversationList").assertIsDisplayed()
  }

  @Test
  fun voiceInputButton_isDisplayed() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify button exists and is displayed
    composeTestRule.onNodeWithTag("micButton").assertIsDisplayed()
  }

  @Test
  fun helpCard_displaysAllExpectedElements() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify all help card elements
    composeTestRule.onNodeWithTag("helpCard").assertIsDisplayed()
    composeTestRule.onNodeWithText("How to use the assistant").assertIsDisplayed()

    // Check for example queries (using substring match)
    composeTestRule
        .onNode(hasText("I'm looking for a concert tonight", substring = true))
        .assertIsDisplayed()

    composeTestRule
        .onNode(hasText("Find me a sports event this weekend", substring = true))
        .assertIsDisplayed()

    composeTestRule
        .onNode(hasText("What festivals are coming up?", substring = true))
        .assertIsDisplayed()
  }

  @Test
  fun screen_handlesEmptyState() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify initial empty state shows help
    composeTestRule.onNodeWithTag("helpCard").assertIsDisplayed()

    // Verify no messages are displayed initially
    composeTestRule.onNodeWithTag("userMessage").assertDoesNotExist()
    composeTestRule.onNodeWithTag("aiMessage").assertDoesNotExist()
  }

  @Test
  fun resetButton_isDisplayed() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("resetButton").assertIsDisplayed()
  }

  @Test
  fun helpCard_containsJoinEventExamples() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify join event examples are shown
    composeTestRule.onNode(hasText("Join the first event", substring = true)).assertIsDisplayed()

    composeTestRule
        .onNode(hasText("Register for the second one", substring = true))
        .assertIsDisplayed()
  }

  @Test
  fun noUserOrAiMessages_initialState() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("userBubble").assertDoesNotExist()
    composeTestRule.onNodeWithTag("aiBubble").assertDoesNotExist()
  }

  @Test
  fun noEventCards_initialState() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onAllNodesWithTag("eventCard_").assertCountEquals(0)
  }

  @Test
  fun noFollowupQuestions_initialState() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("followupQuestion").assertDoesNotExist()
  }

  @Test
  fun backButton_onlyTriggersOnce() {
    var clickCount = 0

    composeTestRule.setContent {
      MapInTheme { AiAssistantScreen(onNavigateBack = { clickCount++ }) }
    }

    composeTestRule.onNodeWithTag("backButton").performClick()
    composeTestRule.onNodeWithTag("backButton").performClick()

    assert(clickCount == 1)
  }

  @Test
  fun micButton_hasClickAction() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").assertHasClickAction()
  }

  @Test
  fun resetButton_hasClickAction() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("resetButton").assertHasClickAction()
  }

  @Test
  fun backButton_hasClickAction() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("backButton").assertHasClickAction()
  }
}

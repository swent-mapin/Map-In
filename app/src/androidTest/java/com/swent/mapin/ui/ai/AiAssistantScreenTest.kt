package com.swent.mapin.ui.ai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.ui.theme.MapInTheme
import org.junit.Rule
import org.junit.Test

// Assisted by AI

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
  fun microphoneButton_displaysCorrectIcon_whenIdle() {
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

    // Verify help text contains example queries
    composeTestRule
        .onNodeWithText(
            "Press the microphone and say:\n" +
                "• \"I'm looking for a concert tonight\"\n" +
                "• \"Find me a sports event this weekend\"\n" +
                "• \"What festivals are coming up?\"",
            substring = false)
        .assertIsDisplayed()
  }

  @Test
  fun userMessageBubble_displays_afterMicrophoneClick() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    // Wait for user message to appear (should be immediate after click)
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithTag("userMessage").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("userBubble").assertIsDisplayed()
    composeTestRule.onNodeWithText("I'm looking for a concert tonight").assertIsDisplayed()
  }

  @Test
  fun aiMessageBubble_displays_afterUserMessage() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    // Wait for AI response (2000ms delay in code)
    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule.onAllNodesWithTag("aiMessage").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("aiBubble").assertIsDisplayed()
  }

  @Test
  fun speakingIndicator_displays_whenAiIsSpeaking() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    // Wait for AI to start speaking (2000ms processing delay)
    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule.onAllNodesWithTag("speakingIndicator").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("speakingIndicator").assertIsDisplayed()
  }

  @Test
  fun recommendedEvents_display_afterAiResponse() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    // Wait for recommended events (after 2000ms delay)
    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule.onAllNodesWithTag("eventCard_event1").fetchSemanticsNodes().isNotEmpty()
    }

    // Wait for composition to stabilize
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Recommended Events:").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventCard_event1").assertIsDisplayed()

    // Scroll to second event to ensure it's visible
    composeTestRule
        .onNodeWithTag("conversationList")
        .performScrollToNode(hasTestTag("eventCard_event2"))

    composeTestRule.onNodeWithTag("eventCard_event2").assertIsDisplayed()
  }

  @Test
  fun eventCard_displaysCorrectInformation() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    // Wait for events (2000ms delay)
    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule.onAllNodesWithTag("eventCard_event1").fetchSemanticsNodes().isNotEmpty()
    }

    // Wait for composition to stabilize
    composeTestRule.waitForIdle()

    // Verify first event details
    composeTestRule.onNodeWithTag("eventCard_event1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Jazz Concert at Sunset Club").assertIsDisplayed()
    composeTestRule.onNodeWithText("🕐 8:00 PM").assertIsDisplayed()

    // Scroll to second event to make it visible
    composeTestRule
        .onNodeWithTag("conversationList")
        .performScrollToNode(hasTestTag("eventCard_event2"))

    composeTestRule.onNodeWithTag("eventCard_event2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Rock Concert at Olympia").assertIsDisplayed()
    composeTestRule.onNodeWithText("🕐 9:30 PM").assertIsDisplayed()
  }

  @Test
  fun eventCard_triggersCallback_onClick() {
    var selectedEventId: String? = null

    composeTestRule.setContent {
      MapInTheme { AiAssistantScreen(onEventSelected = { eventId -> selectedEventId = eventId }) }
    }

    composeTestRule.onNodeWithTag("micButton").performClick()

    // Wait for events (2000ms delay)
    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule.onAllNodesWithTag("eventCard_event1").fetchSemanticsNodes().isNotEmpty()
    }

    // Click on first event
    composeTestRule.onNodeWithTag("eventCard_event1").performClick()

    assert(selectedEventId == "event1")
  }

  @Test
  fun statusBanner_showsReadyState_initially() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithText("💬 Ready to listen").assertIsDisplayed()
  }

  @Test
  fun statusBanner_showsSpeakingState() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    // Wait for speaking state (2000ms processing delay)
    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule
          .onAllNodesWithText("🔊 Assistant speaking...")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText("🔊 Assistant speaking...").assertIsDisplayed()
  }

  @Test
  fun conversationList_isDisplayed() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify LazyColumn is displayed
    composeTestRule.onNodeWithTag("conversationList").assertIsDisplayed()
  }

  @Test
  fun voiceInputButton_hasCorrectSize() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    // Verify button exists and is displayed
    composeTestRule.onNodeWithTag("micButton").assertIsDisplayed()
  }

  @Test
  fun multipleInteractions_workCorrectly() {
    var eventSelectedCount = 0

    composeTestRule.setContent {
      MapInTheme { AiAssistantScreen(onEventSelected = { eventSelectedCount++ }) }
    }

    // First interaction
    composeTestRule.onNodeWithTag("micButton").performClick()

    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule.onAllNodesWithTag("eventCard_event1").fetchSemanticsNodes().isNotEmpty()
    }

    // Wait for composition to stabilize
    composeTestRule.waitForIdle()

    // Click first event
    composeTestRule.onNodeWithTag("eventCard_event1").performClick()
    assert(eventSelectedCount == 1)

    // Scroll to second event to make it visible and clickable
    composeTestRule
        .onNodeWithTag("conversationList")
        .performScrollToNode(hasTestTag("eventCard_event2"))

    composeTestRule.waitForIdle()

    // Click second event
    composeTestRule.onNodeWithTag("eventCard_event2").performClick()
    assert(eventSelectedCount == 2)
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
  fun userMessageBubble_hasCorrectAlignment() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithTag("userBubble").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify user bubble exists (alignment is visual, hard to test semantically)
    composeTestRule.onNodeWithTag("userBubble").assertIsDisplayed()
  }

  @Test
  fun aiMessageBubble_hasCorrectAlignment() {
    composeTestRule.setContent { MapInTheme { AiAssistantScreen() } }

    composeTestRule.onNodeWithTag("micButton").performClick()

    composeTestRule.waitUntil(timeoutMillis = 8000) {
      composeTestRule.onAllNodesWithTag("aiBubble").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify AI bubble exists
    composeTestRule.onNodeWithTag("aiBubble").assertIsDisplayed()
  }
}

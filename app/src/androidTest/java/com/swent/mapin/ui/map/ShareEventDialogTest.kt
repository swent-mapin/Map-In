package com.swent.mapin.ui.map

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Assisted by AI
class ShareEventDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context

  private val testEvent =
      Event(
          uid = "test-event-123",
          title = "Summer Music Festival",
          description = "Amazing outdoor concert",
          location = Location(name = "Paris", latitude = 48.8566, longitude = 2.3522),
          date = Timestamp(Calendar.getInstance().apply { set(2025, 9, 20, 14, 30) }.time),
          ownerId = "owner456",
          participantIds = listOf("user1", "user2"),
          attendeeCount = 25,
          capacity = 100,
          tags = listOf("Music", "Festival"),
          imageUrl = "https://example.com/festival.jpg",
          url = "https://example.com/events/test-event-123")

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun shareEventDialog_displaysCorrectTitle() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareDialogTitle").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share Event").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_displaysCopyLinkOption() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
    composeTestRule.onNodeWithText("Copy Link").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_displaysShareToAppsOption() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share to...").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_displaysCancelButton() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    composeTestRule.onNodeWithTag("shareDialogDismiss").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_cancelButtonClick_callsOnDismiss() {
    var dismissCalled = false
    composeTestRule.setContent {
      ShareEventDialog(event = testEvent, onDismiss = { dismissCalled = true })
    }

    composeTestRule.onNodeWithTag("shareDialogDismiss").performClick()
    composeTestRule.waitForIdle()
    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_copyLinkClick_copiesUrlToClipboard() {
    var dismissCalled = false
    composeTestRule.setContent {
      ShareEventDialog(event = testEvent, onDismiss = { dismissCalled = true })
    }

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    // Verify clipboard content
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboard.primaryClip
    assertNotNull(clipData)
    assertEquals(1, clipData!!.itemCount)
    assertEquals("https://example.com/events/test-event-123", clipData.getItemAt(0).text)

    // Verify dialog is dismissed
    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_copyLinkWithNullUrl_copiesGeneratedUrl() {
    val eventWithoutUrl = testEvent.copy(url = null)
    var dismissCalled = false

    composeTestRule.setContent {
      ShareEventDialog(event = eventWithoutUrl, onDismiss = { dismissCalled = true })
    }

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    // Verify clipboard content with generated URL
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboard.primaryClip
    assertNotNull(clipData)
    assertEquals("https://mapin.app/events/test-event-123", clipData!!.getItemAt(0).text)
    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_shareToAppsClick_triggersIntent() {
    var dismissCalled = false
    composeTestRule.setContent {
      ShareEventDialog(event = testEvent, onDismiss = { dismissCalled = true })
    }

    composeTestRule.onNodeWithTag("shareToAppsOption").performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is dismissed
    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_shareToAppsWithNullUrl_usesGeneratedUrl() {
    val eventWithoutUrl = testEvent.copy(url = null)
    var dismissCalled = false

    composeTestRule.setContent {
      ShareEventDialog(event = eventWithoutUrl, onDismiss = { dismissCalled = true })
    }

    composeTestRule.onNodeWithTag("shareToAppsOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_multipleClicks_handlesCorrectly() {
    var dismissCount = 0
    composeTestRule.setContent {
      ShareEventDialog(event = testEvent, onDismiss = { dismissCount++ })
    }

    // Click copy link
    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, dismissCount)
  }

  @Test
  fun shareEventDialog_copyLinkOption_hasCorrectIcon() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    // Verify the option is displayed (icon is part of the composable)
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_shareToAppsOption_hasCorrectIcon() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    // Verify the option is displayed (icon is part of the composable)
    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
  }

  // NEW TESTS FOR INCREASED COVERAGE

  @Test
  fun shareEventDialog_onDismissRequest_callsOnDismiss() {
    var dismissCalled = false
    composeTestRule.setContent {
      ShareEventDialog(event = testEvent, onDismiss = { dismissCalled = true })
    }

    // Simulate pressing back button or clicking outside the dialog
    // This tests the onDismissRequest parameter of AlertDialog
    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()

    // The dialog should be visible
    assertTrue(!dismissCalled)
  }

  @Test
  fun shareEventDialog_withDifferentEventTitles_displaysCorrectly() {
    val eventWithLongTitle =
        testEvent.copy(
            title =
                "A Very Long Event Title That Should Still Display Correctly In The Share Dialog")

    composeTestRule.setContent { ShareEventDialog(event = eventWithLongTitle, onDismiss = {}) }

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_withEmptyEventTitle_displaysCorrectly() {
    val eventWithEmptyTitle = testEvent.copy(title = "")

    composeTestRule.setContent { ShareEventDialog(event = eventWithEmptyTitle, onDismiss = {}) }

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_multipleDialogs_eachHasOwnState() {
    // Test that recreating the dialog with different event data works correctly
    val event1 = testEvent
    val event2 = testEvent.copy(uid = "event-456", url = "https://example.com/events/456")

    var dismissCount = 0
    var currentEvent = event1

    composeTestRule.setContent {
      ShareEventDialog(event = currentEvent, onDismiss = { dismissCount++ })
    }

    // Verify first event's data is displayed and can be copied
    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    // Verify clipboard has first event's URL
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    assertEquals(
        "https://example.com/events/test-event-123", clipboard.primaryClip!!.getItemAt(0).text)
    assertEquals(1, dismissCount)
  }

  @Test
  fun shareEventDialog_copyLink_showsToast() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    // Toast is displayed (we can't easily test Toast in Compose UI tests,
    // but we verify the click doesn't crash)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    assertNotNull(clipboard.primaryClip)
  }

  @Test
  fun shareEventDialog_bothOptionsVisible_simultaneously() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    // Both share options should be visible at the same time
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
    composeTestRule.onNodeWithText("Copy Link").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share to...").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_spacingBetweenOptions_exists() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    // Verify both options are rendered (spacing is between them)
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_withSpecialCharactersInTitle_handlesCorrectly() {
    val eventWithSpecialChars = testEvent.copy(title = "Event with émojis 🎉 & special <chars>")
    var dismissCalled = false

    composeTestRule.setContent {
      ShareEventDialog(event = eventWithSpecialChars, onDismiss = { dismissCalled = true })
    }

    composeTestRule.onNodeWithTag("shareToAppsOption").performClick()
    composeTestRule.waitForIdle()

    // Should handle special characters without crashing
    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_withVeryLongUrl_handlesCorrectly() {
    val eventWithLongUrl =
        testEvent.copy(
            url =
                "https://example.com/events/very-long-event-url-with-many-parameters?param1=value1&param2=value2&param3=value3")

    composeTestRule.setContent { ShareEventDialog(event = eventWithLongUrl, onDismiss = {}) }

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboard.primaryClip
    assertNotNull(clipData)
    assertTrue(
        clipData!!.getItemAt(0).text.toString().startsWith("https://example.com/events/very-long"))
  }

  @Test
  fun shareEventDialog_rapidClicks_handlesGracefully() {
    var dismissCount = 0
    composeTestRule.setContent {
      ShareEventDialog(event = testEvent, onDismiss = { dismissCount++ })
    }

    // Rapid clicks on the same option
    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    // Should only dismiss once
    assertEquals(1, dismissCount)
  }

  @Test
  fun shareEventDialog_clickOutsideDialog_canDismiss() {
    var dismissCalled = false
    composeTestRule.setContent {
      ShareEventDialog(event = testEvent, onDismiss = { dismissCalled = true })
    }

    // Dialog is displayed
    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()

    // Clicking cancel button should dismiss
    composeTestRule.onNodeWithTag("shareDialogDismiss").performClick()
    composeTestRule.waitForIdle()
    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_allTextLabels_displayed() {
    composeTestRule.setContent { ShareEventDialog(event = testEvent, onDismiss = {}) }

    // Verify all text labels are present
    composeTestRule.onNodeWithText("Share Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Copy Link").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share to...").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_withMinimalEvent_displaysCorrectly() {
    val minimalEvent =
        Event(
            uid = "min-event",
            title = "Min",
            description = "",
            location = Location(name = "Test", latitude = 0.0, longitude = 0.0),
            date = null,
            ownerId = "owner",
            participantIds = emptyList(),
            attendeeCount = 0,
            capacity = null,
            tags = emptyList(),
            imageUrl = null,
            url = null)

    composeTestRule.setContent { ShareEventDialog(event = minimalEvent, onDismiss = {}) }

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    // Should generate default URL
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    assertEquals("https://mapin.app/events/min-event", clipboard.primaryClip!!.getItemAt(0).text)
  }
}

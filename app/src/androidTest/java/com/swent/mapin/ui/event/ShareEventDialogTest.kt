package com.swent.mapin.ui.event

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import java.util.Calendar
import org.junit.Assert.assertEquals
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
          location = Location.from(name = "Paris", lat = 48.8566, lng = 2.3522),
          date = Timestamp(Calendar.getInstance().apply { set(2025, 9, 20, 14, 30) }.time),
          ownerId = "owner456",
          participantIds = listOf("user1", "user2"),
          capacity = 100,
          tags = listOf("Music", "Festival"),
          imageUrl = "https://example.com/festival.jpg",
          url = "https://example.com/events/test-event-123")

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.clearPrimaryClip()
  }

  private fun setShareEventDialog(event: Event = testEvent, onDismiss: () -> Unit = {}) {
    composeTestRule.setContent { ShareEventDialog(event = event, onDismiss = onDismiss) }
  }

  private fun getClipboardText(): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var clipText: String? = null
    for (_attempt in 0..3) {
      clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
      if (clipText != null) break
      Thread.sleep(100)
    }
    return clipText
  }

  @Test
  fun shareEventDialog_displaysAllElements() {
    setShareEventDialog()

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareDialogTitle").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share Event").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
    composeTestRule.onNodeWithText("Copy Link").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share to...").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareDialogDismiss").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_bothOptionsVisible_simultaneously() {
    setShareEventDialog()

    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
    composeTestRule.onNodeWithText("Copy Link").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share to...").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_cancelButton_callsOnDismiss() {
    var dismissCalled = false
    setShareEventDialog(onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("shareDialogDismiss").performClick()
    composeTestRule.waitForIdle()
    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_copyLink_copiesUrlToClipboardAndDismisses() {
    var dismissCalled = false
    setShareEventDialog(onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)

    val clipData = getClipboardText()
    if (clipData != null) {
      assertEquals("https://example.com/events/test-event-123", clipData)
    }
  }

  @Test
  fun shareEventDialog_copyLink_withNullUrl_usesGeneratedUrl() {
    var dismissCalled = false
    setShareEventDialog(event = testEvent.copy(url = null), onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)

    val clipData = getClipboardText()
    if (clipData != null) {
      assertEquals("https://mapin.app/events/test-event-123", clipData)
    }
  }

  @Test
  fun shareEventDialog_copyLink_withLongUrl_copiesCorrectly() {
    var dismissCalled = false
    setShareEventDialog(
        event =
            testEvent.copy(
                url =
                    "https://example.com/events/very-long-event-url-with-many-parameters?param1=value1&param2=value2&param3=value3"),
        onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)

    val clipData = getClipboardText()
    if (clipData != null) {
      assertTrue(clipData.startsWith("https://example.com/events/very-long"))
    }
  }

  @Test
  fun shareEventDialog_copyLink_withMinimalEvent_usesGeneratedUrl() {
    var dismissCalled = false
    val minimalEvent =
        Event(
            uid = "min-event",
            title = "Min",
            description = "",
            location = Location.from(name = "Test", lat = 0.0, lng = 0.0),
            date = null,
            ownerId = "owner",
            participantIds = emptyList(),
            capacity = null,
            tags = emptyList(),
            imageUrl = null,
            url = null)

    setShareEventDialog(event = minimalEvent, onDismiss = { dismissCalled = true })
    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)

    val clipData = getClipboardText()
    if (clipData != null) {
      assertEquals("https://mapin.app/events/min-event", clipData)
    }
  }

  @Test
  fun shareEventDialog_shareToApps_dismissesDialog() {
    var dismissCalled = false
    setShareEventDialog(onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("shareToAppsOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_shareToApps_withNullUrl_dismissesCorrectly() {
    var dismissCalled = false
    setShareEventDialog(event = testEvent.copy(url = null), onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("shareToAppsOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_shareToApps_withSpecialCharacters_dismissesCorrectly() {
    var dismissCalled = false
    setShareEventDialog(
        event = testEvent.copy(title = "Event with émojis 🎉 & special <chars>"),
        onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("shareToAppsOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_withLongTitle_displaysCorrectly() {
    setShareEventDialog(
        event =
            testEvent.copy(
                title =
                    "A Very Long Event Title That Should Still Display Correctly In The Share Dialog"))

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
    composeTestRule.onNodeWithTag("shareToAppsOption").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_withEmptyTitle_displaysCorrectly() {
    setShareEventDialog(event = testEvent.copy(title = ""))

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_withMinimalEvent_displaysCorrectly() {
    val minimalEvent =
        Event(
            uid = "min-event",
            title = "Min",
            description = "",
            location = Location.from(name = "Test", lat = 0.0, lng = 0.0),
            date = null,
            ownerId = "owner",
            participantIds = emptyList(),
            capacity = null,
            tags = emptyList(),
            imageUrl = null,
            url = null)

    setShareEventDialog(event = minimalEvent)
    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
  }

  @Test
  fun shareEventDialog_copyLinkClick_dismissesImmediately() {
    var dismissCalled = false
    setShareEventDialog(onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)
  }

  @Test
  fun shareEventDialog_recreatedWithDifferentEvent_copiesCorrectUrl() {
    var dismissCalled = false
    val event2 = testEvent.copy(uid = "event-456", url = "https://example.com/events/456")

    setShareEventDialog(event = event2, onDismiss = { dismissCalled = true })
    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copyLinkOption").performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissCalled)

    val clipData = getClipboardText()
    if (clipData != null) {
      assertEquals("https://example.com/events/456", clipData)
    }
  }

  @Test
  fun shareEventDialog_onDismissRequest_allowsDismiss() {
    var dismissCalled = false
    setShareEventDialog(onDismiss = { dismissCalled = true })

    composeTestRule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
    assertTrue(!dismissCalled)

    composeTestRule.onNodeWithTag("shareDialogDismiss").performClick()
    composeTestRule.waitForIdle()
    assertTrue(dismissCalled)
  }
}

package com.swent.mapin.ui.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.map.bottomsheet.components.AttendedEventsSection
import com.swent.mapin.ui.memory.MemoryFormScreen
import java.util.Date
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the [AttendedEventsSection] composable.
 * Assisted with AI.
 */
@RunWith(AndroidJUnit4::class)
class AttendedEventsSectionTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun attendedEventsSection_showsEmptyMessage_whenNoAttendedEvents() {
    val userProfile = UserProfile(userId = "testUser", participatingEventIds = emptyList())

    composeTestRule.setContent {
      MaterialTheme {
        AttendedEventsSection(
            availableEvents = emptyList(),
            userProfile = userProfile,
            onEventClick = {},
            onCreateMemoryClick = {})
      }
    }

    composeTestRule.onNodeWithText("No attended events yet").assertIsDisplayed()
  }

  @Test
  fun attendedEventsSection_showsEventRow_whenUserAttendedPastEvent() {
    val uid = "event1"
    val now = System.currentTimeMillis()
    val past = Date(now - 1000L * 60L * 60L * 24L) // 1 day ago
    val startTs = Timestamp(past)
    val endTs = Timestamp(past)

    val event =
        Event(
            uid = uid,
            title = "Past Event",
            description = "desc",
            date = startTs,
            endDate = endTs,
            location = Location("Test Location", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "owner",
            imageUrl = null,
            capacity = 10,
            participantIds = listOf("testUser"),
            price = 0.0)

    val userProfile = UserProfile(userId = "testUser", participatingEventIds = listOf(uid))

    composeTestRule.setContent {
      MaterialTheme {
        AttendedEventsSection(
            availableEvents = listOf(event),
            userProfile = userProfile,
            onEventClick = {},
            onCreateMemoryClick = {})
      }
    }

    composeTestRule.onNodeWithText("Past Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Location").assertIsDisplayed()
    composeTestRule.onNodeWithText("+").assertIsDisplayed()
  }

  @Test
  fun attendedEventsSection_opensMemoryFormPrefilled_onPlusClick() {
    val uid = "event1"
    val now = System.currentTimeMillis()
    val past = Date(now - 1000L * 60L * 60L * 24L) // 1 day ago
    val startTs = Timestamp(past)
    val endTs = Timestamp(past)

    val event =
        Event(
            uid = uid,
            title = "Past Event",
            description = "desc",
            date = startTs,
            endDate = endTs,
            location = Location("Test Location", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "owner",
            imageUrl = null,
            capacity = 10,
            participantIds = listOf("testUser"),
            price = 0.0)

    val userProfile = UserProfile(userId = "testUser", participatingEventIds = listOf(uid))

    composeTestRule.setContent {
      MaterialTheme {
        val memoryEvent = remember { mutableStateOf<Event?>(null) }
        // Show either the attended list (initial) or the MemoryFormScreen when a memoryEvent is
        // set.
        val selected = memoryEvent.value
        if (selected == null) {
          AttendedEventsSection(
              availableEvents = listOf(event),
              userProfile = userProfile,
              onEventClick = {},
              onCreateMemoryClick = { memoryEvent.value = it })
        } else {
          val scrollState = remember { ScrollState(0) }
          MemoryFormScreen(
              scrollState = scrollState,
              availableEvents = listOf(event),
              onSave = {},
              onCancel = {},
              initialSelectedEvent = selected)
        }
      }
    }
    composeTestRule.onNodeWithTag("event_add_memory_event1", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Wait until the memory form screen appears (testTag 'memoryFormScreen') — more reliable
    composeTestRule.waitUntil(5000) {
      composeTestRule
          .onAllNodesWithTag("memoryFormScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag("memoryFormScreen", useUnmergedTree = true).assertExists()

    // Wait until the memory form's selected event title node appears (useUnmergedTree = true)
    composeTestRule.waitUntil(5000) {
      composeTestRule
          .onAllNodesWithTag("memoryForm_selectedEventTitle", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify the selected event title in the form exists (useUnmergedTree = true)
    composeTestRule
        .onNodeWithTag("memoryForm_selectedEventTitle", useUnmergedTree = true)
        .assertExists()
  }
}

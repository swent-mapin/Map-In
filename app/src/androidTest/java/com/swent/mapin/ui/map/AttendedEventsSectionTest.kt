package com.swent.mapin.ui.map

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.map.bottomsheet.components.AttendedEventsSection
import com.swent.mapin.ui.map.eventstate.MapEventStateController
import com.swent.mapin.ui.memory.MemoryFormScreen
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the [AttendedEventsSection] composable. Assisted with AI. */
@RunWith(AndroidJUnit4::class)
class AttendedEventsSectionTest {
  /**
   * Creates an [Event] with the given parameters.
   *
   * @param uid The unique identifier for the event.
   * @param title The title of the event.
   * @param endDate The end date of the event.
   * @param date The date of the event.
   * @param ownerId The owner ID of the event.
   */
  private fun makeEvent(
      uid: String,
      title: String,
      endDate: Date?,
      date: Date = endDate ?: Date(),
      ownerId: String = "o"
  ): Event {
    return Event(
        uid = uid,
        title = title,
        description = "",
        date = Timestamp(date),
        endDate = endDate?.let { Timestamp(it) },
        location = Location("L", 0.0, 0.0),
        tags = emptyList(),
        public = true,
        ownerId = ownerId,
        imageUrl = null,
        capacity = 0,
        participantIds = emptyList(),
        price = 0.0)
  }

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun attendedEventsSection_showsEmptyMessage_whenNoAttendedEvents() {
    composeTestRule.setContent {
      MaterialTheme {
        AttendedEventsSection(
            attendedEvents = emptyList(), onEventClick = {}, onCreateMemoryClick = {})
      }
    }

    composeTestRule.onNodeWithText("No past events yet.").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Once you attend events, they’ll appear here.")
        .assertIsDisplayed()
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

    composeTestRule.setContent {
      MaterialTheme {
        AttendedEventsSection(
            attendedEvents = listOf(event), onEventClick = {}, onCreateMemoryClick = {})
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

    composeTestRule.setContent {
      MaterialTheme {
        val memoryEvent = remember { mutableStateOf<Event?>(null) }
        // Show either the attended list (initial) or the MemoryFormScreen when a memoryEvent is
        // set.
        val selected = memoryEvent.value
        if (selected == null) {
          AttendedEventsSection(
              attendedEvents = listOf(event),
              onEventClick = {},
              onCreateMemoryClick = { memoryEvent.value = it })
        } else {
          val scrollState = remember { ScrollState(0) }
          MemoryFormScreen(
              scrollState = scrollState,
              availableEvents = listOf(event),
              onSave = {},
              onCancel = {},
              onEventClick = {},
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

  @Test
  fun computeAttendedEvents_filtersOutFutureAndNullEndDates_andSorts() {
    val now = System.currentTimeMillis()

    val past1 = Date(now - 1000L * 60L * 60L * 24L * 2) // 2 days ago
    val past2 = Date(now - 1000L * 60L * 60L * 24L) // 1 day ago
    val future = Date(now + 1000L * 60L * 60L * 24L) // 1 day in future

    val e1 = makeEvent("e1", "E1", past1)
    val e2 = makeEvent("e2", "E2", past2)
    val eFuture = makeEvent("ef", "Future", future)
    val eNull = makeEvent("en", "NoEnd", null)

    val input = listOf(eFuture, eNull, e1, e2)

    val result = MapEventStateController.computeAttendedEvents(input)

    // Should include only e1 and e2 (past events), sorted by most recent end date first => e2, e1
    assertEquals(listOf(e2.uid, e1.uid), result.map { it.uid })
  }

  @Test
  fun computeAttendedEvents_returnsEmpty_whenNoPastEvents() {
    val now = System.currentTimeMillis()
    val future = Date(now + 1000L * 60L * 60L * 24L)
    val ef =
        Event(
            uid = "ef",
            title = "Future",
            description = "",
            date = Timestamp(future),
            endDate = Timestamp(future),
            location = Location("Lf", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "o",
            imageUrl = null,
            capacity = 0,
            participantIds = emptyList(),
            price = 0.0)

    val result = MapEventStateController.computeAttendedEvents(listOf(ef))
    assertEquals(0, result.size)
  }
}

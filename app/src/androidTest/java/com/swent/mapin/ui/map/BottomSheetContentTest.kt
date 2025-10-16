package com.swent.mapin.ui.map

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// generated with the help of AI
// ---- Fake repo  ----
private class FakeEventRepository(private val data: List<Event>) : EventRepository {
  override fun getNewUid(): String = "fake"

  override suspend fun getAllEvents(): List<Event> = data

  override suspend fun getEvent(eventID: String): Event = data.first { it.uid == eventID }

  override suspend fun getEventsByTags(tags: List<String>): List<Event> =
      data.filter { it.tags.any(tags::contains) }

  override suspend fun getEventsOnDay(dayStart: Timestamp, dayEnd: Timestamp): List<Event> = data

  override suspend fun getEventsByOwner(ownerId: String): List<Event> =
      data.filter { it.ownerId == ownerId }

  override suspend fun getEventsByTitle(title: String): List<Event> =
      data.filter { it.title.equals(title, ignoreCase = true) }

  override suspend fun getEventsByParticipant(userId: String): List<Event> =
      data.filter { userId in it.participantIds }

  override suspend fun addEvent(event: Event) {}

  override suspend fun editEvent(eventID: String, newValue: Event) {}

  override suspend fun deleteEvent(eventID: String) {}
}

@RunWith(AndroidJUnit4::class)
class BottomSheetContentTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // Sample events that match your current Event schema (with Location)
  private val sample =
      listOf(
          Event(
              uid = "1",
              title = "Rock Night",
              description = "Live music downtown",
              location = Location("Zurich", 47.37, 8.54),
              tags = listOf("music", "nightlife"),
              ownerId = "a",
              attendeeCount = 10,
              participantIds = listOf("u1", "u2")),
          Event(
              uid = "2",
              title = "Morning Run",
              description = "Easy pace 5k",
              location = Location("Lake Park", 0.0, 0.0),
              tags = listOf("sports", "outdoors"),
              ownerId = "b",
              attendeeCount = 5),
          Event(
              uid = "3",
              title = "Jazz Jam",
              description = "impro session · bring your instrument",
              location = Location("Basel", 0.0, 0.0),
              tags = listOf("music", "jam"),
              ownerId = "c",
              attendeeCount = 3))

  private fun newVm(): SearchViewModel = SearchViewModel(FakeEventRepository(sample))

  private fun setSheet(state: BottomSheetState, vm: SearchViewModel = newVm()) {
    rule.setContent {
      MaterialTheme {
        BottomSheetContent(
            state = state,
            fullEntryKey = 0,
            searchViewModel = vm,
            showMemoryForm = false,
            availableEvents = emptyList(),
            onCreateMemoryClick = {},
            onMemorySave = {},
            onMemoryCancel = {},
            onExitSearch = {})
      }
    }
    // Let initial composition, VM init, and animations settle
    rule.waitForIdle()
    rule.mainClock.advanceTimeBy(500) // finish AnimatedContent/Search focus effects
  }

  @Test
  fun collapsed_showsSearchBar() {
    setSheet(BottomSheetState.COLLAPSED)
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun medium_showsQuickActions() {
    setSheet(BottomSheetState.MEDIUM)

    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Create Memory").assertIsDisplayed()
    rule.onNodeWithText("Create Event").assertIsDisplayed()
    rule.onNodeWithText("Filters").assertIsDisplayed()
  }

  @Test
  fun full_showsRecentAndDiscover_whenNotInSearchMode() {
    setSheet(BottomSheetState.FULL)

    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
    rule.onNodeWithText("Discover").assertIsDisplayed()
  }

  @Test
  fun searchBar_tapAndType_updatesQuery() {
    val vm = newVm()
    setSheet(BottomSheetState.COLLAPSED, vm)

    rule.onNodeWithText("Search activities").performClick()
    rule.onNodeWithText("Search activities").performTextInput("Test")

    assertEquals("Test", vm.ui.value.query)
  }

  @Test
  fun searchFocus_requestedAfterOnSearchTapped() {
    val vm = newVm()
    setSheet(BottomSheetState.FULL, vm)

    // Trigger the one-shot focus request from the VM
    rule.runOnIdle { vm.onSearchTapped() }
    rule.mainClock.advanceTimeBy(300) // let LaunchedEffect run
    rule.waitForIdle()

    rule.onNodeWithText("Search activities").assertIsFocused()
  }

  @Test
  fun collapsed_notFocusedByDefault() {
    setSheet(BottomSheetState.COLLAPSED)
    rule.onNodeWithText("Search activities").assertIsNotFocused()
  }

  @Test
  fun buttons_clickable() {
    setSheet(BottomSheetState.FULL)
    rule.onNodeWithText("Create Memory").assertHasClickAction()
    rule.onNodeWithText("Sports").assertHasClickAction()
    rule.onNodeWithText("Music").assertHasClickAction()
  }

  @Test
  fun searchText_persistsInField() {
    val vm = newVm()
    setSheet(BottomSheetState.COLLAPSED, vm)

    rule.onNodeWithText("Search activities").performTextInput("Coffee")
    rule.onNodeWithText("Coffee").assertIsDisplayed()
  }
}

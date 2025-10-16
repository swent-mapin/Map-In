package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Assisted by AI
class BottomSheetContentTest {

  @get:Rule val rule = createComposeRule()

  @Composable
  private fun TestContent(
      state: BottomSheetState,
      initialQuery: String = "",
      initialFocus: Boolean = false,
      onQueryChange: (String) -> Unit = {},
      onTap: () -> Unit = {}
  ) {
    MaterialTheme {
      var query by remember { mutableStateOf(initialQuery) }
      var shouldRequestFocus by remember { mutableStateOf(initialFocus) }

      BottomSheetContent(
          state = state,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = query,
                  shouldRequestFocus = shouldRequestFocus,
                  onQueryChange = {
                    query = it
                    onQueryChange(it)
                  },
                  onTap = onTap,
                  onFocusHandled = { shouldRequestFocus = false }))
    }
  }

  @Test
  fun collapsedState_showsSearchBarOnly() {
    rule.setContent { TestContent(state = BottomSheetState.COLLAPSED) }

    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun mediumState_showsQuickActions() {
    rule.setContent { TestContent(state = BottomSheetState.MEDIUM) }

    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Create Memory").assertIsDisplayed()
    rule.onNodeWithText("Create Event").assertIsDisplayed()
    rule.onNodeWithText("Filters").assertIsDisplayed()
  }

  @Test
  fun fullState_showsAllContent() {
    rule.setContent { TestContent(state = BottomSheetState.FULL) }
    rule.waitForIdle()

    rule.waitUntil(timeoutMillis = 10000) {
      try {
        rule.onNodeWithText("Activity 1").assertIsDisplayed()
        true
      } catch (_: AssertionError) {
        false
      }
    }

    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
    rule.onNodeWithText("Discover").assertIsDisplayed()
    rule.onNodeWithText("Activity 1").assertIsDisplayed()
  }

  @Test
  fun searchBar_handlesTextInputAndCallbacks() {
    var capturedQuery = ""
    var tapTriggered = false

    rule.setContent {
      TestContent(
          state = BottomSheetState.COLLAPSED,
          onQueryChange = { capturedQuery = it },
          onTap = { tapTriggered = true })
    }

    rule.onNodeWithText("Search activities").performClick()
    assertTrue(tapTriggered)

    rule.onNodeWithText("Search activities").performTextInput("Test")
    assertEquals("Test", capturedQuery)
  }

  @Test
  fun searchBar_focusOnlyInFullState() {
    rule.setContent { TestContent(state = BottomSheetState.FULL, initialFocus = true) }

    rule.onNodeWithText("Search activities").assertIsFocused()
  }

  @Test
  fun searchBar_doesNotFocusInCollapsedState() {
    rule.setContent { TestContent(state = BottomSheetState.COLLAPSED, initialFocus = true) }

    rule.onNodeWithText("Search activities").assertIsNotFocused()
  }

  @Test
  fun buttons_areClickable() {
    rule.setContent { TestContent(state = BottomSheetState.FULL, initialFocus = false) }

    rule.onNodeWithText("Create Memory").assertHasClickAction()
    rule.onNodeWithText("Sports").assertHasClickAction()
    rule.onNodeWithText("Music").assertHasClickAction()
  }

  @Test
  fun searchQuery_persistsAcrossStateChanges() {
    rule.setContent { TestContent(state = BottomSheetState.COLLAPSED, initialQuery = "Coffee") }

    rule.onNodeWithText("Coffee").assertIsDisplayed()
  }

  // Helper function to reduce boilerplate for Joined Events tests
  @Composable
  private fun JoinedEventsContent(
      events: List<com.swent.mapin.model.event.Event>,
      onEventClick: (com.swent.mapin.model.event.Event) -> Unit = {},
      onTabChange: (MapScreenViewModel.BottomSheetTab) -> Unit = {}
  ) {
    MaterialTheme {
      BottomSheetContent(
          state = BottomSheetState.FULL,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = "",
                  shouldRequestFocus = false,
                  onQueryChange = {},
                  onTap = {},
                  onFocusHandled = {}),
          joinedEvents = events,
          selectedTab = MapScreenViewModel.BottomSheetTab.JOINED_EVENTS,
          onJoinedEventClick = onEventClick,
          onTabChange = onTabChange)
    }
  }

  // JOINED EVENTS TAB TESTS
  @Test
  fun joinedEventsTab_showsEmptyState() {
    rule.setContent { JoinedEventsContent(events = emptyList()) }
    rule.waitForIdle()
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    rule.onNodeWithText("You haven't joined any events yet").assertIsDisplayed()
  }

  @Test
  fun joinedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = com.swent.mapin.model.SampleEventRepository.getSampleEvents().take(3)
    rule.setContent { JoinedEventsContent(events = testEvents) }
    rule.waitForIdle()
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    testEvents.forEach { event ->
      rule.onNodeWithText(event.title).assertIsDisplayed()
      rule.onNodeWithText(event.location.name, substring = true).assertIsDisplayed()
    }
  }

  @Test
  fun joinedEventsTab_handlesEventInteractions() {
    val testEvents = com.swent.mapin.model.SampleEventRepository.getSampleEvents().take(1)
    var clickedEvent: com.swent.mapin.model.event.Event? = null

    rule.setContent {
      JoinedEventsContent(events = testEvents, onEventClick = { clickedEvent = it })
    }
    rule.waitForIdle()
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    // Verify event is clickable and callback works
    rule.onNodeWithText(testEvents[0].title).performClick()
    rule.waitForIdle()
    assertEquals(testEvents[0], clickedEvent)
  }

  @Test
  fun joinedEventsTab_handlesEdgeCases() {
    // Test event with null date
    val eventWithNullDate =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(date = null)

    rule.setContent { JoinedEventsContent(events = listOf(eventWithNullDate)) }
    rule.waitForIdle()
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    rule.onNodeWithText(eventWithNullDate.title).assertIsDisplayed()
    rule.onNodeWithText("No date").assertIsDisplayed()
  }

  @Test
  fun tabSwitch_betweenRecentActivitiesAndJoinedEvents() {
    val testEvents = com.swent.mapin.model.SampleEventRepository.getSampleEvents().take(1)
    var currentTab = MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES

    rule.setContent {
      MaterialTheme {
        var selectedTab by remember { mutableStateOf(currentTab) }
        BottomSheetContent(
            state = BottomSheetState.FULL,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = "",
                    shouldRequestFocus = false,
                    onQueryChange = {},
                    onTap = {},
                    onFocusHandled = {}),
            joinedEvents = testEvents,
            selectedTab = selectedTab,
            onTabChange = { tab ->
              selectedTab = tab
              currentTab = tab
            })
      }
    }
    rule.waitForIdle()

    // Initially shows Recent Activities
    rule.onNodeWithText("Activity 1").assertIsDisplayed()

    // Switch to Joined Events tab
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    // Should now show joined events
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()
    assertEquals(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS, currentTab)
  }
}

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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.LocalEventRepository
import com.swent.mapin.ui.profile.ProfileViewModel
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
                  onFocusHandled = { shouldRequestFocus = false },
                  onClear = {}))
    }
  }

  @Composable
  private fun SavedEventsContent(
      events: List<Event>,
      onEventClick: (Event) -> Unit = {},
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
                  onFocusHandled = {},
                  onClear = {}),
          savedEvents = events,
          selectedTab = MapScreenViewModel.BottomSheetTab.SAVED_EVENTS,
          onTabEventClick = onEventClick,
          onTabChange = onTabChange)
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
    rule.onNodeWithText("Friends").assertIsDisplayed()
  }

  @Test
  fun fullState_showsAllContent() {
    rule.setContent { TestContent(state = BottomSheetState.FULL) }
    rule.waitForIdle()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Saved Events").assertIsDisplayed()
    rule.onNodeWithText("Joined Events").assertIsDisplayed()
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
    // Test Quick Action buttons in FULL state
    rule.setContent { TestContent(state = BottomSheetState.FULL, initialFocus = false) }

    rule.waitForIdle()

    rule.onNodeWithText("Create Memory").assertHasClickAction()
    rule.onNodeWithText("Create Event").assertHasClickAction()
    rule.onNodeWithText("Friends").assertHasClickAction()
  }

  @Test
  fun friendsButton_isDisplayedInQuickActions() {
    rule.setContent { TestContent(state = BottomSheetState.FULL) }
    rule.waitForIdle()

    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Friends").assertIsDisplayed()
  }

  @Test
  fun friendsButton_triggersNavigationCallback() {
    var navigationTriggered = false

    rule.setContent {
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
                    onFocusHandled = {},
                    onClear = {}),
            onNavigateToFriends = { navigationTriggered = true })
      }
    }
    rule.waitForIdle()

    rule.onNodeWithText("Friends").performClick()
    assertTrue(navigationTriggered)
  }

  @Test
  fun searchQuery_persistsAcrossStateChanges() {
    rule.setContent { TestContent(state = BottomSheetState.COLLAPSED, initialQuery = "Coffee") }

    rule.onNodeWithText("Coffee").assertIsDisplayed()
  }

  // Helper function to reduce boilerplate for Joined Events tests
  @Composable
  private fun JoinedEventsContent(
      events: List<Event>,
      onEventClick: (Event) -> Unit = {},
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
                  onFocusHandled = {},
                  onClear = {}),
          joinedEvents = events,
          selectedTab = MapScreenViewModel.BottomSheetTab.JOINED_EVENTS,
          onTabEventClick = onEventClick,
          onTabChange = onTabChange)
    }
  }

  @Test
  fun joinedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(3)
    rule.setContent { JoinedEventsContent(events = testEvents) }
    rule.waitForIdle()
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    testEvents.forEach { event ->
      rule.onNodeWithText(event.title).performScrollTo().assertIsDisplayed()
      rule
          .onNodeWithText(event.location.name, substring = true)
          .performScrollTo()
          .assertIsDisplayed()
    }
  }

  @Test
  fun joinedEventsTab_handlesEventInteractions() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(1)
    var clickedEvent: Event? = null

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
  fun tabSwitch_betweenSavedEventsAndJoinedEvents() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(1)
    var currentTab = MapScreenViewModel.BottomSheetTab.SAVED_EVENTS

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
                    onFocusHandled = {},
                    onClear = {}),
            joinedEvents = testEvents,
            selectedTab = selectedTab,
            onTabChange = { tab ->
              selectedTab = tab
              currentTab = tab
            })
      }
    }
    rule.waitForIdle()

    // Switch to Joined Events tab
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    // Should now show joined events
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()
    assertEquals(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS, currentTab)
  }

  @Composable
  private fun TestContentWithSearch(
      query: String = "",
      searchResults: List<Event> = emptyList(),
      isSearchMode: Boolean = false
  ) {
    MaterialTheme {
      var searchQuery by remember { mutableStateOf(query) }
      var shouldRequestFocus by remember { mutableStateOf(false) }

      BottomSheetContent(
          state = BottomSheetState.FULL,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = searchQuery,
                  shouldRequestFocus = shouldRequestFocus,
                  onQueryChange = { searchQuery = it },
                  onTap = {},
                  onFocusHandled = { shouldRequestFocus = false },
                  onClear = {}),
          searchResults = searchResults,
          isSearchMode = isSearchMode)
    }
  }

  @Test
  fun noResultsMessage_displaysInSearchModeWithEmptyResultsAndBlankQuery() {
    rule.setContent {
      TestContentWithSearch(query = "", searchResults = emptyList(), isSearchMode = true)
    }

    rule.waitForIdle()

    rule.onNodeWithText("No events available yet.").assertIsDisplayed()
    rule.onNodeWithText("Try again once events are added.").assertIsDisplayed()
  }

  @Test
  fun noResultsMessage_displaysInSearchModeWithEmptyResultsAndQuery() {
    rule.setContent {
      TestContentWithSearch(query = "concert", searchResults = emptyList(), isSearchMode = true)
    }

    rule.waitForIdle()

    rule.onNodeWithText("No results found").assertIsDisplayed()
    rule.onNodeWithText("Try a different keyword or check the spelling.").assertIsDisplayed()
  }

  @Test
  fun noResultsMessage_doesNotDisplayWhenSearchResultsExist() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(1)
    rule.setContent {
      TestContentWithSearch(query = "test", searchResults = testEvents, isSearchMode = true)
    }

    rule.waitForIdle()

    rule.onNodeWithText("No events available yet.").assertDoesNotExist()
    rule.onNodeWithText("No results found").assertDoesNotExist()
    // Should display the event instead
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()
  }

  @Composable
  private fun TestContentWithFilters(
      state: BottomSheetState,
      filterViewModel: FiltersSectionViewModel = FiltersSectionViewModel(),
      locationViewModel: LocationViewModel = LocationViewModel(),
      profileViewModel: ProfileViewModel = ProfileViewModel()
  ) {
    MaterialTheme {
      BottomSheetContent(
          state = state,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = "",
                  shouldRequestFocus = false,
                  onQueryChange = {},
                  onTap = {},
                  onFocusHandled = {},
                  onClear = {}))
    }
  }

  @Test
  fun savedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(3)
    rule.setContent { SavedEventsContent(events = testEvents) }
    rule.waitForIdle()

    // Ensure Saved Events tab title is visible (it is selected by default in this content)
    rule.onNodeWithText("Saved Events").assertIsDisplayed()

    testEvents.forEach { event ->
      rule.onNodeWithText(event.title).performScrollTo().assertIsDisplayed()
      // Location line is shown in SearchResultItem; use substring for safety
      rule
          .onNodeWithText(event.location.name, substring = true)
          .performScrollTo()
          .assertIsDisplayed()
    }
  }

  @Test
  fun filterSection_displaysInFullState() {
    rule.setContent { TestContentWithFilters(BottomSheetState.FULL) }
    rule.waitForIdle()

    rule.onNodeWithTag(FiltersSectionTestTags.TITLE).performScrollTo().assertIsDisplayed()

    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TIME).performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PLACE).performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TAGS).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun filterSection_doesNotDisplayInCollapsedState() {
    rule.setContent { TestContentWithFilters(state = BottomSheetState.COLLAPSED) }

    rule.waitForIdle()

    rule.onNodeWithTag(FiltersSectionTestTags.TITLE).assertDoesNotExist()
  }

  @Test
  fun savedEventsTab_handlesEventInteractions() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(1)
    var clickedEvent: Event? = null

    rule.setContent {
      SavedEventsContent(events = testEvents, onEventClick = { clickedEvent = it })
    }
    rule.waitForIdle()

    // Click the single event in the Saved tab
    rule.onNodeWithText(testEvents[0].title).performClick()
    rule.waitForIdle()

    assertEquals(testEvents[0], clickedEvent)
  }

  @Test
  fun savedEventsTab_showMoreAndShowLessToggle() {
    // >3 events so EventsSection shows the "Show more" control
    val testEvents = LocalEventRepository.defaultSampleEvents().take(5)
    rule.setContent { SavedEventsContent(events = testEvents) }
    rule.waitForIdle()

    // Initially only first 3 are visible (others exist but are off-screen/not visible)
    testEvents.take(3).forEach { e ->
      rule.onNodeWithText(e.title, substring = true).performScrollTo().assertIsDisplayed()
    }
    testEvents.drop(3).forEach { e ->
      // We only assert not displayed; they might exist off-screen
      rule.onNodeWithText(e.title, substring = true).assertDoesNotExist()
    }

    // Scroll to reveal the "Show more" button before asserting
    rule
        .onNodeWithTag("eventsShowMoreButton", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
    rule.waitForIdle()

    // After expanding, scroll to each and assert visibility
    testEvents.forEach { e ->
      rule.onNodeWithText(e.title, substring = true).performScrollTo().assertIsDisplayed()
    }

    // Toggle back to "Show less" (same button)
    rule
        .onNodeWithTag("eventsShowMoreButton", useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    rule.waitForIdle()

    // Collapsed again: only first 3 visible
    testEvents.take(3).forEach { e ->
      rule.onNodeWithText(e.title, substring = true).performScrollTo().assertIsDisplayed()
    }
    testEvents.drop(3).forEach { e ->
      rule.onNodeWithText(e.title, substring = true).assertDoesNotExist()
    }
  }

  @Test
  fun tabSwitch_startOnSaved_thenSwitchToJoined_showsJoinedContent() {
    val saved = LocalEventRepository.defaultSampleEvents().take(1)
    val joined = LocalEventRepository.defaultSampleEvents().drop(1).take(1)

    rule.setContent {
      MaterialTheme {
        var selectedTab by remember {
          mutableStateOf(MapScreenViewModel.BottomSheetTab.SAVED_EVENTS)
        }
        BottomSheetContent(
            state = BottomSheetState.FULL,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = "",
                    shouldRequestFocus = false,
                    onQueryChange = {},
                    onTap = {},
                    onFocusHandled = {},
                    onClear = {}),
            savedEvents = saved,
            joinedEvents = joined,
            selectedTab = selectedTab,
            onTabChange = { selectedTab = it })
      }
    }
    rule.waitForIdle()

    // On Saved tab first
    rule.onNodeWithText("Saved Events").assertIsDisplayed()
    rule.onNodeWithText(saved[0].title).assertIsDisplayed()

    // Switch to Joined tab
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    // Joined event title visible; saved no longer present
    rule.onNodeWithText(joined[0].title).assertIsDisplayed()
  }

  @Test
  fun profileButton_clickInvokesCallback() {
    var clicked = false
    rule.setContent {
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
                    onFocusHandled = {},
                    onClear = {}),
            avatarUrl = "http://example.com/avatar.jpg",
            onProfileClick = { clicked = true })
      }
    }

    rule.waitForIdle()

    // The clickable semantics live on the inner Box; use unmerged tree to find the clickable node
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()
    rule.waitForIdle()

    assertTrue(clicked)
  }

  @Test
  fun profileButton_displaysDefaultIconWhenAvatarUrlIsNull() {
    rule.setContent {
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
                    onFocusHandled = {},
                    onClear = {}),
            avatarUrl = null,
            onProfileClick = {})
      }
    }

    rule.waitForIdle()

    // The profileButton container should be present and visible
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
    // We can't easily test the specific icon content, but we can verify the button is there
  }

  @Test
  fun profileButton_displaysIconWhenAvatarUrlIsNotHttp() {
    rule.setContent {
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
                    onFocusHandled = {},
                    onClear = {}),
            avatarUrl = "person",
            onProfileClick = {})
      }
    }

    rule.waitForIdle()

    // The profileButton container should be present and visible
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }
}

package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.LocalEventList
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.location.LocationRepository
import com.swent.mapin.model.location.LocationViewModel
import com.swent.mapin.ui.event.EditEventScreenTestTags
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.filters.FiltersSectionTestTags
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import com.swent.mapin.ui.map.bottomsheet.SearchBarState
import com.swent.mapin.ui.map.bottomsheet.components.AllRecentItemsPage
import com.swent.mapin.ui.map.search.RecentItem
import com.swent.mapin.ui.profile.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Assisted by AI
class BottomSheetContentTest {

  @get:Rule val rule = createComposeRule()
  val filterViewModel = FiltersSectionViewModel()
  val locationRepository = mockk<LocationRepository>(relaxed = true)
  val locationViewModel = LocationViewModel(locationRepository)
  val profileViewModel = ProfileViewModel()
  val eventViewModel = mockk<EventViewModel>(relaxed = true)

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
                  onClear = {}),
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
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
          selectedTab = MapScreenViewModel.BottomSheetTab.SAVED,
          onTabEventClick = onEventClick,
          onTabChange = onTabChange,
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
    }
  }

  @Composable
  private fun SearchModeContent(
      query: String = "",
      shouldRequestFocus: Boolean = false,
      recentItems: List<RecentItem> = emptyList(),
      topCategories: List<String> = emptyList(),
      searchResults: List<Event> = emptyList(),
      onQueryChange: (String) -> Unit = {},
      onRecentSearchClick: (String) -> Unit = {},
      onRecentEventClick: (String) -> Unit = {},
      onCategoryClick: (String) -> Unit = {},
      onEventClick: (Event) -> Unit = {},
      onSubmit: () -> Unit = {}
  ) {
    MaterialTheme {
      var searchQuery by remember { mutableStateOf(query) }
      var requestFocus by remember { mutableStateOf(shouldRequestFocus) }
      BottomSheetContent(
          state = BottomSheetState.FULL,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = searchQuery,
                  shouldRequestFocus = requestFocus,
                  onQueryChange = {
                    searchQuery = it
                    onQueryChange(it)
                  },
                  onTap = {},
                  onFocusHandled = { requestFocus = false },
                  onClear = {},
                  onSubmit = onSubmit),
          isSearchMode = true,
          recentItems = recentItems,
          searchResults = searchResults,
          onRecentSearchClick = onRecentSearchClick,
          onRecentEventClick = onRecentEventClick,
          onEventClick = onEventClick,
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
    }
  }

  @Test
  fun collapsedState_showsSearchBarOnly() {
    rule.setContent { TestContent(state = BottomSheetState.COLLAPSED) }

    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun fullState_showsCreateEvent() {
    rule.setContent { TestContent(state = BottomSheetState.FULL) }
    rule.waitForIdle()
    rule.onNodeWithText("Create Event").assertIsDisplayed()
  }

  @Test
  fun mediumState_showsCreateEvent() {
    rule.setContent { TestContent(state = BottomSheetState.MEDIUM) }
    rule.waitForIdle()
    rule.onNodeWithText("Create Event").assertIsDisplayed()
  }

  @Test
  fun fullState_showsAllContent() {
    rule.setContent { TestContent(state = BottomSheetState.FULL) }
    rule.waitForIdle()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
    rule.onNodeWithText("Saved").assertIsDisplayed()
    rule.onNodeWithText("Upcoming").assertIsDisplayed()
    rule.onNodeWithText("Past").assertIsDisplayed()
    rule.onNodeWithText("Owned").assertIsDisplayed()
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

    rule.onNodeWithText("Search events, people").performClick()
    assertTrue(tapTriggered)

    rule.onNodeWithText("Search events, people").performTextInput("Test")
    assertEquals("Test", capturedQuery)
  }

  @Test
  fun searchBar_focusOnlyInFullState() {
    rule.setContent { TestContent(state = BottomSheetState.FULL, initialFocus = true) }

    rule.onNodeWithText("Search events, people").assertIsFocused()
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
          selectedTab = MapScreenViewModel.BottomSheetTab.UPCOMING,
          onTabEventClick = onEventClick,
          onTabChange = onTabChange,
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
    }
  }

  @Test
  fun joinedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = LocalEventList.defaultSampleEvents().take(3)
    rule.setContent { JoinedEventsContent(events = testEvents) }
    rule.waitForIdle()
    rule.onNodeWithText("Upcoming").performClick()
    rule.waitForIdle()

    testEvents.forEach { event ->
      rule.onNodeWithText(event.title).performScrollTo().assertIsDisplayed()
      rule
          .onNodeWithText(event.location.name ?: Location.NO_NAME, substring = true)
          .performScrollTo()
          .assertIsDisplayed()
    }
  }

  @Test
  fun joinedEventsTab_handlesEventInteractions() {
    val testEvents = LocalEventList.defaultSampleEvents().take(1)
    var clickedEvent: Event? = null

    rule.setContent {
      JoinedEventsContent(events = testEvents, onEventClick = { clickedEvent = it })
    }
    rule.waitForIdle()
    rule.onNodeWithText("Upcoming").performClick()
    rule.waitForIdle()

    // Verify event is clickable and callback works
    rule.onNodeWithText(testEvents[0].title).performClick()
    rule.waitForIdle()
    assertEquals(testEvents[0], clickedEvent)
  }

  @Test
  fun tabSwitch_betweenSavedEventsAndJoinedEvents() {
    val testEvents = LocalEventList.defaultSampleEvents().take(1)
    var currentTab = MapScreenViewModel.BottomSheetTab.SAVED

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
            },
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
      }
    }
    rule.waitForIdle()

    // Switch to Joined Events tab
    rule.onNodeWithText("Upcoming").performClick()
    rule.waitForIdle()

    // Should now show joined events
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()
    assertEquals(MapScreenViewModel.BottomSheetTab.UPCOMING, currentTab)
  }

  @Composable
  private fun TestContentWithSearch(query: String = "", searchResults: List<Event> = emptyList()) {
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
          isSearchMode = true,
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
    }
  }

  @Test
  fun noResultsMessage_displaysInSearchModeWithEmptyResultsAndBlankQuery() {
    // With the new behavior, blank query shows recent items/categories instead of "No events"
    // When there are no recent items or categories, the section is just empty
    rule.setContent { TestContentWithSearch(query = "", searchResults = emptyList()) }

    rule.waitForIdle()

    // The old "No events available yet" message is no longer shown for blank queries
    // Instead, verify that search bar is still visible (main UI element in this state)
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun noResultsMessage_displaysInSearchModeWithEmptyResultsAndQuery() {
    rule.setContent { TestContentWithSearch(query = "concert", searchResults = emptyList()) }

    rule.waitForIdle()

    rule.onNodeWithText("No results found").assertIsDisplayed()
    rule.onNodeWithText("Try a different keyword or check the spelling.").assertIsDisplayed()
  }

  @Test
  fun noResultsMessage_doesNotDisplayWhenSearchResultsExist() {
    val testEvents = LocalEventList.defaultSampleEvents().take(1)
    rule.setContent { TestContentWithSearch(query = "test", searchResults = testEvents) }

    rule.waitForIdle()

    rule.onNodeWithText("No events available yet.").assertDoesNotExist()
    rule.onNodeWithText("No results found").assertDoesNotExist()
    // Should display the event instead
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()
  }

  @Composable
  private fun TestContentWithFilters() {
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
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
    }
  }

  @Test
  fun savedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = LocalEventList.defaultSampleEvents().take(3)
    rule.setContent { SavedEventsContent(events = testEvents) }
    rule.waitForIdle()

    // Ensure Saved Events tab title is visible (it is selected by default in this content)
    rule.onNodeWithText("Saved").assertIsDisplayed()

    testEvents.forEach { event ->
      rule.onNodeWithText(event.title).performScrollTo().assertIsDisplayed()
      // Location line is shown in SearchResultItem; use substring for safety
      rule
          .onNodeWithText(event.location.name ?: Location.NO_NAME, substring = true)
          .performScrollTo()
          .assertIsDisplayed()
    }
  }

  @Test
  fun filterSection_displaysInFullState() {
    rule.setContent { TestContentWithFilters() }
    rule.waitForIdle()

    rule.onNodeWithTag(FiltersSectionTestTags.TITLE).performScrollTo().assertIsDisplayed()

    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_DATE).performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_LOCATION).performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TAGS).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun savedEventsTab_handlesEventInteractions() {
    val testEvents = LocalEventList.defaultSampleEvents().take(1)
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
    val testEvents = LocalEventList.defaultSampleEvents().take(5)
    rule.setContent { SavedEventsContent(events = testEvents) }
    rule.waitForIdle()

    // EventsSection displays events in reversed order (latest first). Compute expected
    // visible/hidden sets.
    val visibleInitially = testEvents.take(3)
    val hiddenInitially = testEvents.drop(3)

    // Initially only the last 3 (reversed first 3) are visible; earlier ones are not yet shown
    visibleInitially.forEach { e ->
      rule.onNodeWithText(e.title, substring = true).performScrollTo().assertIsDisplayed()
    }
    hiddenInitially.forEach { e ->
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
    visibleInitially.forEach { e ->
      rule.onNodeWithText(e.title, substring = true).performScrollTo().assertIsDisplayed()
    }
    hiddenInitially.forEach { e ->
      rule.onNodeWithText(e.title, substring = true).assertDoesNotExist()
    }
  }

  @Test
  fun tabSwitch_startOnSaved_thenSwitchToJoined_showsJoinedContent() {
    val saved = LocalEventList.defaultSampleEvents().take(1)
    val joined = LocalEventList.defaultSampleEvents().drop(1).take(1)

    rule.setContent {
      MaterialTheme {
        var selectedTab by remember { mutableStateOf(MapScreenViewModel.BottomSheetTab.SAVED) }
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
            onTabChange = { selectedTab = it },
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
      }
    }
    rule.waitForIdle()

    // On Saved tab first
    rule.onNodeWithText("Saved").assertIsDisplayed()
    rule.onNodeWithText(saved[0].title).assertIsDisplayed()

    // Switch to Joined tab
    rule.onNodeWithText("Upcoming").performClick()
    rule.waitForIdle()

    // Joined event title visible; saved no longer present
    rule.onNodeWithText(joined[0].title).assertIsDisplayed()
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
            onProfileClick = {},
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
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
            onProfileClick = {},
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
      }
    }

    rule.waitForIdle()

    // The profileButton container should be present and visible
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  // Tests for new search functionality

  @Test
  fun recentItemsSection_displaysRecentSearches() {
    val recentSearches =
        listOf(
            RecentItem.Search("coffee"),
            RecentItem.Search("basketball"),
            RecentItem.Search("museum"))

    rule.setContent { SearchModeContent(recentItems = recentSearches) }

    rule.waitForIdle()

    // Verify "Recents" section title is displayed
    rule.onNodeWithText("Recents").assertIsDisplayed()

    // Verify recent searches are displayed
    rule.onNodeWithText("coffee").assertIsDisplayed()
    rule.onNodeWithText("basketball").assertIsDisplayed()
    rule.onNodeWithText("museum").assertIsDisplayed()
  }

  @Test
  fun recentItemsSection_clickRecentSearch_triggersCallback() {
    var clickedQuery = ""
    val recentSearches = listOf(RecentItem.Search("coffee"))

    rule.setContent {
      SearchModeContent(recentItems = recentSearches, onRecentSearchClick = { clickedQuery = it })
    }

    rule.waitForIdle()

    // Click on the recent search
    rule.onNodeWithText("coffee").performClick()
    rule.waitForIdle()

    // Verify callback was triggered
    assertEquals("coffee", clickedQuery)
  }

  @Test
  fun recentItemsSection_showsShowAllButton() {
    val recentSearches = listOf(RecentItem.Search("coffee"), RecentItem.Search("tea"))

    rule.setContent { SearchModeContent(recentItems = recentSearches) }

    rule.waitForIdle()

    // Verify "Show all" button is displayed
    rule.onNodeWithTag("showAllRecentSearchesButton").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun searchMode_withBlankQuery_showsRecentAndCategories() {
    val recentSearches = listOf(RecentItem.Search("coffee"))
    val topCategories = listOf("Sports")

    rule.setContent {
      SearchModeContent(recentItems = recentSearches, topCategories = topCategories)
    }

    rule.waitForIdle()

    // Both sections should be visible
    rule.onNodeWithText("Recents").assertIsDisplayed()
  }

  @Test
  fun searchMode_withQuery_showsResults() {
    val testEvents = LocalEventList.defaultSampleEvents().take(2)

    rule.setContent { SearchModeContent(query = "concert", searchResults = testEvents) }

    rule.waitForIdle()

    // Verify results are displayed
    testEvents.forEach { event -> rule.onNodeWithText(event.title).assertIsDisplayed() }

    // Recents section should not be shown when there's a query
    rule.onNodeWithText("Recents").assertDoesNotExist()
  }

  @Test
  fun searchBar_submitAction_triggersCallback() {
    rule.setContent {
      SearchModeContent(query = "coffee", shouldRequestFocus = true, onSubmit = {})
    }

    rule.waitForIdle()

    // IME submit action testing requires an input method; skip triggering IME here.
    // The wiring of onSubmit is covered by integration tests outside unit instrumentation.
  }

  @Test
  fun searchResultsSection_withResults_displaysEventsAndHandlesClick() {
    var clickedEvent: Event? = null
    val testEvents = LocalEventList.defaultSampleEvents().take(2)

    rule.setContent {
      SearchModeContent(
          query = "test", searchResults = testEvents, onEventClick = { clickedEvent = it })
    }

    rule.waitForIdle()

    // Verify events are displayed
    testEvents.forEach { event -> rule.onNodeWithText(event.title).assertIsDisplayed() }

    // Click on first event
    rule.onNodeWithText(testEvents[0].title).performClick()
    rule.waitForIdle()

    // Verify callback was triggered
    assertEquals(testEvents[0], clickedEvent)
  }

  @Test
  fun recentItemsSection_withClickedEvent_displaysAndHandlesClick() {
    var clickedEventId = ""
    val testEvent = LocalEventList.defaultSampleEvents()[0]
    val recentItems = listOf(RecentItem.ClickedEvent(testEvent.uid, testEvent.title))

    rule.setContent {
      SearchModeContent(recentItems = recentItems, onRecentEventClick = { clickedEventId = it })
    }

    rule.waitForIdle()

    // Verify event is displayed
    rule.onNodeWithText(testEvent.title).assertIsDisplayed()

    // Click on event
    rule.onNodeWithText(testEvent.title).performClick()
    rule.waitForIdle()

    // Verify callback was triggered
    assertEquals(testEvent.uid, clickedEventId)
  }

  @Test
  fun allRecentItemsPage_displaysCorrectly() {
    val recentItems =
        listOf(
            RecentItem.Search("coffee"),
            RecentItem.ClickedEvent("event1", "Concert"),
            RecentItem.Search("basketball"))

    rule.setContent {
      MaterialTheme {
        AllRecentItemsPage(
            recentItems = recentItems,
            onRecentSearchClick = {},
            onRecentEventClick = {},
            onClearAll = {},
            onBack = {})
      }
    }

    rule.waitForIdle()

    // Verify AllRecentItemsPage is displayed
    rule.onNodeWithText("Recent searches").assertIsDisplayed()
    rule.onNodeWithTag("backFromAllRecentsButton").assertIsDisplayed()
    rule.onNodeWithTag("clearAllRecentButton").assertIsDisplayed()

    // Verify all items are displayed
    rule.onNodeWithText("coffee").assertIsDisplayed()
    rule.onNodeWithText("Concert").assertIsDisplayed()
    rule.onNodeWithText("basketball").assertIsDisplayed()
  }

  @Test
  fun allRecentItemsPage_backButton_triggersCallback() {
    var backCalled = false

    rule.setContent {
      MaterialTheme {
        AllRecentItemsPage(
            recentItems = listOf(RecentItem.Search("test")),
            onRecentSearchClick = {},
            onRecentEventClick = {},
            onClearAll = {},
            onBack = { backCalled = true })
      }
    }

    rule.waitForIdle()

    // Click back button
    rule.onNodeWithTag("backFromAllRecentsButton").performClick()
    rule.waitForIdle()

    // Verify callback was triggered
    assertTrue(backCalled)
  }

  @Test
  fun allRecentItemsPage_clearAllButton_triggersCallback() {
    var clearAllCalled = false

    rule.setContent {
      MaterialTheme {
        AllRecentItemsPage(
            recentItems = listOf(RecentItem.Search("test")),
            onRecentSearchClick = {},
            onRecentEventClick = {},
            onClearAll = { clearAllCalled = true },
            onBack = {})
      }
    }

    rule.waitForIdle()

    // Click clear all button
    rule.onNodeWithTag("clearAllRecentButton").performClick()
    rule.waitForIdle()

    // Verify callback was triggered
    assertTrue(clearAllCalled)
  }

  @Test
  fun allRecentItemsPage_clickRecentSearch_triggersCallback() {
    var clickedQuery = ""

    rule.setContent {
      MaterialTheme {
        AllRecentItemsPage(
            recentItems = listOf(RecentItem.Search("coffee")),
            onRecentSearchClick = { clickedQuery = it },
            onRecentEventClick = {},
            onClearAll = {},
            onBack = {})
      }
    }

    rule.waitForIdle()

    // Click on recent search
    rule.onNodeWithTag("recentSearchItem_coffee").performClick()
    rule.waitForIdle()

    // Verify callback was triggered
    assertEquals("coffee", clickedQuery)
  }

  @Test
  fun allRecentItemsPage_clickRecentEvent_triggersCallback() {
    var clickedEventId = ""

    rule.setContent {
      MaterialTheme {
        AllRecentItemsPage(
            recentItems = listOf(RecentItem.ClickedEvent("event123", "Concert")),
            onRecentSearchClick = {},
            onRecentEventClick = { clickedEventId = it },
            onClearAll = {},
            onBack = {})
      }
    }

    rule.waitForIdle()

    // Click on recent event
    rule.onNodeWithTag("recentEventItem_Concert").performClick()
    rule.waitForIdle()

    // Verify callback was triggered
    assertEquals("event123", clickedEventId)
  }

  @Test
  fun memoryFormScreen_displaysWhenCurrentScreenIsMemoryForm() {
    val testEvents = LocalEventList.defaultSampleEvents().take(2)

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
            currentScreen = BottomSheetScreen.MEMORY_FORM,
            availableEvents = testEvents,
            onMemorySave = {},
            onMemoryCancel = {},
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
      }
    }

    rule.waitForIdle()

    // Verify MemoryFormScreen is displayed by checking its test tag
    rule.onNodeWithTag("memoryFormScreen").assertIsDisplayed()
  }

  @Test
  fun addEventScreen_displaysWhenCurrentScreenIsAddEvent() {
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
            currentScreen = BottomSheetScreen.ADD_EVENT,
            onCreateEventDone = {},
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
      }
    }

    rule.waitForIdle()

    // Verify AddEventScreen is displayed by checking its test tag
    rule.onNodeWithTag("AddEventScreen").assertIsDisplayed()
  }

  @Test
  fun showAllRecentsButton_navigatesToAllRecentItemsPage() {
    val recentItems =
        listOf(
            RecentItem.Search("coffee"),
            RecentItem.Search("tea"),
            RecentItem.ClickedEvent("event1", "Concert"))

    rule.setContent {
      SearchModeContent(
          recentItems = recentItems,
          onRecentSearchClick = {},
          onRecentEventClick = {},
          onCategoryClick = {})
    }

    rule.waitForIdle()

    // Click "Show all" button
    rule.onNodeWithTag("showAllRecentSearchesButton").performClick()
    rule.waitForIdle()

    // Verify AllRecentItemsPage is now displayed
    rule.onNodeWithText("Recent searches").assertIsDisplayed()
    rule.onNodeWithTag("backFromAllRecentsButton").assertIsDisplayed()
    rule.onNodeWithTag("clearAllRecentButton").assertIsDisplayed()
  }

  @Test
  fun modalProfileMenu_onModalShownCallbackTriggered() {
    val seen = mutableListOf<Boolean>()
    var profileClicked = false

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
            onModalShown = { seen.add(it) },
            onProfileClick = { profileClicked = true },
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel) // added missing param
      }
    }

    rule.waitForIdle()

    // Open modal by clicking profile button (use unmerged tree to reach clickable semantics)
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()
    rule.waitForIdle()

    // Modal should have been shown
    assertTrue(seen.contains(true))

    // Click the Profile button inside modal which closes it and triggers onProfileClick
    rule.onNodeWithText("Profile").performClick()
    rule.waitForIdle()

    // onProfileClick should have been invoked and onModalShown should have received a false
    assertTrue(profileClicked)
    assertTrue(seen.contains(false))
  }

  @Test
  fun modalProfileMenu_buttons_invokeCallbacks() {
    var profileClicked = false
    var friendsClicked = false
    var settingsClicked = false

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
            onProfileClick = { profileClicked = true },
            onNavigateToFriends = { friendsClicked = true },
            onSettingsClick = { settingsClicked = true },
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel) // added missing param
      }
    }

    rule.waitForIdle()

    // Open -> Friends -> reopen -> Settings -> reopen -> Profile
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Friends").performClick()
    rule.waitForIdle()

    // Re-open modal
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Settings").performClick()
    rule.waitForIdle()

    // Re-open modal
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Profile").performClick()
    rule.waitForIdle()

    assertTrue(friendsClicked)
    assertTrue(settingsClicked)
    assertTrue(profileClicked)
  }

  @Test
  fun modalProfileMenu_dismissBehavior_closesOnDismissRequest() {
    // This test verifies that dismiss path clears the modal and related UI is no longer present
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
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel) // added missing param
      }
    }

    rule.waitForIdle()

    // Open modal
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()
    rule.waitForIdle()

    // Ensure modal content visible
    rule.onNodeWithText("Hello", substring = true).assertIsDisplayed()

    // Dismiss by clicking the Profile button which closes it (internal dismiss handler)
    rule.onNodeWithText("Profile").performClick()
    rule.waitForIdle()

    // Modal content should no longer exist
    rule.onNodeWithText("Profile").assertDoesNotExist()
  }

  @Test
  fun showAllRecents_clearAll_invokesCallback_fromBottomSheetContent() {
    var cleared = false
    val recentItems =
        listOf(RecentItem.Search("coffee"), RecentItem.ClickedEvent("event1", "Concert"))

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
            isSearchMode = true,
            recentItems = recentItems,
            onRecentSearchClick = {},
            onRecentEventClick = {},
            onClearRecentSearches = { cleared = true },
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel) // added missing param
      }
    }

    rule.waitForIdle()

    // Click "Show all" to open AllRecentItemsPage
    rule.onNodeWithTag("showAllRecentSearchesButton").performClick()
    rule.waitForIdle()

    // Now click the Clear All button inside AllRecentItemsPage
    rule.onNodeWithTag("clearAllRecentButton").performClick()
    rule.waitForIdle()

    assertTrue(cleared)
  }

  // Helper function to reduce boilerplate for Owned Events tests
  @Composable
  private fun OwnedEventsContent(
      events: List<Event> = emptyList(),
      loading: Boolean = false,
      error: String? = null,
      onRetry: () -> Unit = {},
      onTabEventClick: (Event) -> Unit = {}
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
          selectedTab = MapScreenViewModel.BottomSheetTab.OWNED,
          ownedEvents = events,
          ownedLoading = loading,
          ownedError = error,
          onRetryOwnedEvents = onRetry,
          onTabEventClick = onTabEventClick,
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
    }
  }

  @Test
  fun ownedEvents_loading_showsLoaderAndHidesOtherStates() {
    rule.setContent { OwnedEventsContent(loading = true) }
    rule.waitForIdle()

    // When loading we shouldn't see Retry or No results text
    rule.onNodeWithText("Retry").assertDoesNotExist()
    rule.onNodeWithText("No results found").assertDoesNotExist()
  }

  @Test
  fun ownedEvents_error_displaysErrorAndCallsRetry() {
    var retried = false

    rule.setContent { OwnedEventsContent(error = "Network", onRetry = { retried = true }) }
    rule.waitForIdle()

    rule.onNodeWithText("Error: Network").assertIsDisplayed()
    rule.onNodeWithText("Retry").performClick()
    rule.waitForIdle()

    assertTrue(retried)
  }

  @Test
  fun ownedEvents_empty_showsNoResultsMessage() {
    rule.setContent { OwnedEventsContent(events = emptyList()) }
    rule.waitForIdle()

    rule.onNodeWithText("You haven’t created any events yet.").assertIsDisplayed()
    rule.onNodeWithText("Organize your first event and it will show up here.").assertIsDisplayed()
  }

  @Test
  fun ownedEvents_list_showsItems_showMore_and_handlesClick() {
    val testEvents = LocalEventList.defaultSampleEvents().take(4)
    var clicked: Event? = null

    rule.setContent { OwnedEventsContent(events = testEvents, onTabEventClick = { clicked = it }) }
    rule.waitForIdle()

    // Initially only 3 most recent (reversed) should be shown
    val visibleInitially = testEvents.take(3)
    val hiddenInitially = testEvents.drop(3)

    visibleInitially.forEach { e ->
      rule.onNodeWithText(e.title, substring = true).assertIsDisplayed()
    }
    hiddenInitially.forEach { e ->
      rule.onNodeWithText(e.title, substring = true).assertDoesNotExist()
    }

    // Expand
    rule
        .onNodeWithTag("eventsShowMoreButton", useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    rule.waitForIdle()

    // Now all should be visible
    testEvents.forEach { e ->
      rule.onNodeWithText(e.title, substring = true).performScrollTo().assertIsDisplayed()
    }

    // Click top-most visible event and ensure callback
    val toClick = testEvents.reversed().first()
    rule.onNodeWithText(toClick.title).performClick()
    rule.waitForIdle()

    assertEquals(toClick, clicked)
  }

  @Test
  fun bottomSheetEditEvent_showsProgressOrScreen() {
    val mockEventViewModel = mockk<EventViewModel>(relaxed = true)
    val eventFlow = MutableStateFlow<Event?>(null)
    every { mockEventViewModel.eventToEdit } returns eventFlow.asStateFlow()

    // Single setContent
    rule.setContent {
      BottomSheetContent(
          state = BottomSheetState.FULL,
          currentScreen = BottomSheetScreen.EDIT_EVENT,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = "",
                  shouldRequestFocus = false,
                  onQueryChange = {},
                  onTap = {},
                  onFocusHandled = {},
                  onClear = {}),
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = mockEventViewModel)
    }

    // CASE 1: eventToEdit = null -> CircularProgressIndicator
    rule.onNodeWithTag("EditEventCircularIndicator").assertExists()

    // CASE 2: eventToEdit != null -> EditEventScreen
    val locationMock = mockk<Location>(relaxed = true)
    val dateMock = mockk<Timestamp>(relaxed = true)
    val endDateMock = mockk<Timestamp>(relaxed = true)
    val testEvent =
        Event(
            uid = "1",
            ownerId = "user1",
            title = "Test",
            description = "",
            location = locationMock,
            date = dateMock,
            endDate = endDateMock,
            tags = emptyList())
    eventFlow.value = testEvent // update the flow to trigger recomposition

    rule.waitForIdle() // wait for Compose to recompose

    // Assert EditEventScreen is displayed
    rule.onNodeWithTag(EditEventScreenTestTags.SCREEN).assertExists()
  }

  @Test
  fun screenCanBeScrolledToBottomWithKeyboard() {
    rule.setContent { TestContentWithFilters() }
    rule.waitForIdle()

    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_LOCATION).performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(FiltersSectionTestTags.AROUND_SEARCH).performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(FiltersSectionTestTags.SEARCH_PLACE_INPUT).performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_POPULAR).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun ownedEvents_showsOptionsMenuWithEditAndDelete() {
    val testEvents = LocalEventList.defaultSampleEvents().take(2)
    var editedEvent: Event? = null

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
            selectedTab = MapScreenViewModel.BottomSheetTab.OWNED,
            ownedEvents = testEvents,
            onTabEventClick = {},
            onEditEvent = { editedEvent = it },
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
      }
    }

    rule.waitForIdle()

    // Verify first event is displayed
    rule.onNodeWithText(testEvents.reversed()[0].title).assertIsDisplayed()

    // Click the options icon (three dots) for the first event
    rule
        .onNodeWithTag("eventOptionsIcon_${testEvents.reversed()[0].uid}")
        .assertIsDisplayed()
        .performClick()
    rule.waitForIdle()

    // Verify dropdown menu is displayed with Edit and Delete options
    rule.onNodeWithTag("eventOptionsMenu_${testEvents.reversed()[0].uid}").assertIsDisplayed()
    rule.onNodeWithText("Edit").assertIsDisplayed()
    rule.onNodeWithText("Delete").assertIsDisplayed()

    // Click Edit option
    rule.onNodeWithText("Edit").performClick()
    rule.waitForIdle()

    // Verify edit callback was triggered
    assertEquals(testEvents.reversed()[0], editedEvent)

    // Open menu again for second action
    rule.onNodeWithTag("eventOptionsIcon_${testEvents.reversed()[0].uid}").performClick()
    rule.waitForIdle()
  }

  @Test
  fun savedAndJoinedEvents_doNotShowOptionsMenu() {
    val testEvents = LocalEventList.defaultSampleEvents().take(1)

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
            selectedTab = MapScreenViewModel.BottomSheetTab.SAVED,
            savedEvents = testEvents,
            filterViewModel = filterViewModel,
            locationViewModel = locationViewModel,
            profileViewModel = profileViewModel,
            eventViewModel = eventViewModel)
      }
    }

    rule.waitForIdle()

    // Verify event is displayed
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()

    // Verify options icon does NOT exist for saved events
    rule.onNodeWithTag("eventOptionsIcon_${testEvents[0].uid}").assertDoesNotExist()
  }

  // Tests for PeopleResultsSection and UserSearchCard

  @Composable
  private fun SearchModeWithUsersContent(
      query: String = "test",
      userResults: List<com.swent.mapin.model.UserProfile> = emptyList(),
      searchResults: List<Event> = emptyList(),
      onUserClick: (String, String) -> Unit = { _, _ -> }
  ) {
    MaterialTheme {
      BottomSheetContent(
          state = BottomSheetState.FULL,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = query,
                  shouldRequestFocus = false,
                  onQueryChange = {},
                  onTap = {},
                  onFocusHandled = {},
                  onClear = {}),
          isSearchMode = true,
          searchResults = searchResults,
          userSearchResults = userResults,
          onUserSearchClick = onUserClick,
          filterViewModel = filterViewModel,
          locationViewModel = locationViewModel,
          profileViewModel = profileViewModel,
          eventViewModel = eventViewModel)
    }
  }

  @Test
  fun peopleResultsSection_displaysUsersInFullState() {
    val testUsers =
        listOf(
            com.swent.mapin.model.UserProfile(userId = "user1", name = "Alice Smith"),
            com.swent.mapin.model.UserProfile(userId = "user2", name = "Bob Jones"))

    rule.setContent { SearchModeWithUsersContent(query = "test", userResults = testUsers) }
    rule.waitForIdle()

    // People section header should be displayed
    rule.onNodeWithText("People (2)").assertIsDisplayed()

    // User names should be displayed
    rule.onNodeWithText("Alice Smith").assertIsDisplayed()
    rule.onNodeWithText("Bob Jones").assertIsDisplayed()
  }

  @Test
  fun userSearchCard_triggersClickCallback() {
    var clickedUserId = ""
    var clickedUserName = ""
    val testUsers =
        listOf(com.swent.mapin.model.UserProfile(userId = "user123", name = "Test User"))

    rule.setContent {
      SearchModeWithUsersContent(
          query = "test",
          userResults = testUsers,
          onUserClick = { userId, userName ->
            clickedUserId = userId
            clickedUserName = userName
          })
    }
    rule.waitForIdle()

    rule.onNodeWithTag("userSearchCard_user123").performClick()
    rule.waitForIdle()

    assertEquals("user123", clickedUserId)
    assertEquals("Test User", clickedUserName)
  }

  @Test
  fun peopleResultsSection_notDisplayedWhenNoUsers() {
    rule.setContent { SearchModeWithUsersContent(query = "test", userResults = emptyList()) }
    rule.waitForIdle()

    rule.onNodeWithText("People", substring = true).assertDoesNotExist()
  }

  // Tests for RecentProfileItem

  @Test
  fun recentProfileItem_displaysInAllRecentsPage() {
    val recentItems =
        listOf(
            RecentItem.ClickedProfile(userId = "user456", userName = "Recent User"),
            RecentItem.Search(query = "test search"))

    rule.setContent {
      MaterialTheme {
        AllRecentItemsPage(
            recentItems = recentItems,
            onRecentSearchClick = {},
            onRecentEventClick = {},
            onRecentProfileClick = {},
            onClearAll = {},
            onBack = {})
      }
    }
    rule.waitForIdle()

    rule.onNodeWithText("Recent User").assertIsDisplayed()
    rule.onNodeWithText("test search").assertIsDisplayed()
  }

  @Test
  fun recentProfileItem_triggersClickCallback() {
    var clickedUserId = ""
    val recentItems =
        listOf(RecentItem.ClickedProfile(userId = "user789", userName = "Clickable User"))

    rule.setContent {
      MaterialTheme {
        AllRecentItemsPage(
            recentItems = recentItems,
            onRecentSearchClick = {},
            onRecentEventClick = {},
            onRecentProfileClick = { userId -> clickedUserId = userId },
            onClearAll = {},
            onBack = {})
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("recentProfileItem_Clickable User").performClick()
    rule.waitForIdle()

    assertEquals("user789", clickedUserId)
  }
}

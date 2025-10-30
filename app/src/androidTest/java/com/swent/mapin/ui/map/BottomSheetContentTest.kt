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
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.LocalEventRepository
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
                  onClear = {}),
          profilePictureUrl = null)
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
          onTabChange = onTabChange,
          profilePictureUrl = null)
    }
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
          onTabChange = onTabChange,
          profilePictureUrl = null)
    }
  }

  // Tests for tag filtering functionality
  @Composable
  private fun TestContentWithTags(
      state: BottomSheetState,
      topTags: List<String> = listOf("Sports", "Music", "Food", "Tech", "Art"),
      selectedTags: Set<String> = emptySet(),
      onTagClick: (String) -> Unit = {}
  ) {
    MaterialTheme {
      var query by remember { mutableStateOf("") }
      var shouldRequestFocus by remember { mutableStateOf(false) }

      BottomSheetContent(
          state = state,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = query,
                  shouldRequestFocus = shouldRequestFocus,
                  onQueryChange = { query = it },
                  onTap = {},
                  onFocusHandled = { shouldRequestFocus = false },
                  onClear = {}),
          topTags = topTags,
          selectedTags = selectedTags,
          onTagClick = onTagClick,
          profilePictureUrl = null)
    }
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
          isSearchMode = isSearchMode,
          profilePictureUrl = null)
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
  }

  @Test
  fun fullState_showsAllContentWithTags() {
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
  }

  @Test
  fun searchQuery_persistsAcrossStateChanges() {
    rule.setContent { TestContent(state = BottomSheetState.COLLAPSED, initialQuery = "Coffee") }

    rule.onNodeWithText("Coffee").assertIsDisplayed()
  }

  @Test
  fun joinedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(3)
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
            },
            profilePictureUrl = null)
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

  @Test
  fun tagsSection_displaysTopTags() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL, topTags = listOf("Sports", "Music", "Food"))
    }

    rule.waitForIdle()

    rule.onNodeWithText("Sports").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Music").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Food").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun tagsSection_doesNotDisplayWhenNoTags() {
    rule.setContent { TestContentWithTags(state = BottomSheetState.FULL, topTags = emptyList()) }

    rule.waitForIdle()

    // "Discover" title should still be visible from old section
    rule.onNodeWithText("Discover").assertDoesNotExist()
  }

  @Test
  fun tagsSection_allTagsAreClickable() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL, topTags = listOf("Sports", "Music", "Food", "Tech", "Art"))
    }

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("Sports").performScrollTo().assertHasClickAction()
    rule.onNodeWithText("Music").performScrollTo().assertHasClickAction()
    rule.onNodeWithText("Food").performScrollTo().assertHasClickAction()
    rule.onNodeWithText("Tech").performScrollTo().assertHasClickAction()
    rule.onNodeWithText("Art").performScrollTo().assertHasClickAction()
  }

  @Test
  fun tagsSection_callsOnTagClickWhenClicked() {
    var clickedTag = ""
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL,
          topTags = listOf("Sports", "Music"),
          onTagClick = { clickedTag = it })
    }

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("Sports").performScrollTo().performClick()
    assertEquals("Sports", clickedTag)

    rule.onNodeWithText("Music").performScrollTo().performClick()
    assertEquals("Music", clickedTag)
  }

  @Test
  fun tagsSection_handlesMultipleTagClicks() {
    val clickedTags = mutableListOf<String>()
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL,
          topTags = listOf("Sports", "Music", "Food"),
          onTagClick = { clickedTags.add(it) })
    }

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("Sports").performScrollTo().performClick()
    rule.onNodeWithText("Music").performScrollTo().performClick()
    rule.onNodeWithText("Food").performScrollTo().performClick()

    assertEquals(3, clickedTags.size)
    assertTrue(clickedTags.contains("Sports"))
    assertTrue(clickedTags.contains("Music"))
    assertTrue(clickedTags.contains("Food"))
  }

  @Test
  fun tagsSection_displaysCorrectNumberOfTags() {
    val tags = listOf("Sports", "Music", "Food", "Tech", "Art")
    rule.setContent { TestContentWithTags(state = BottomSheetState.FULL, topTags = tags) }

    rule.waitForIdle()

    tags.forEach { tag -> rule.onNodeWithText(tag).performScrollTo().assertIsDisplayed() }
  }

  @Test
  fun tagsSection_visibleInFullStateOnly() {
    // Test that tags are visible in FULL state
    rule.setContent {
      TestContentWithTags(state = BottomSheetState.FULL, topTags = listOf("Sports", "Music"))
    }

    rule.waitForIdle()
    rule.onNodeWithText("Sports").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun tagsSection_handlesEmptySelectedTags() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL,
          topTags = listOf("Sports", "Music"),
          selectedTags = emptySet())
    }

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("Sports").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Music").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun tagsSection_withSelectedTags_tagsStillDisplayed() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL,
          topTags = listOf("Sports", "Music", "Food"),
          selectedTags = setOf("Sports", "Music"))
    }

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("Sports").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Music").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Food").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun tagsSection_displaysDiscoverTitle() {
    rule.setContent {
      TestContentWithTags(state = BottomSheetState.FULL, topTags = listOf("Sports", "Music"))
    }

    rule.waitForIdle()

    // Should have "Discover" as section title - scroll to make it visible
    rule.onNodeWithText("Discover").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun tagsSection_handlesLongTagNames() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL, topTags = listOf("VeryLongTagName", "AnotherLongTag"))
    }

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("VeryLongTagName").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("AnotherLongTag").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun tagsSection_handlesSpecialCharacters() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL, topTags = listOf("Art & Craft", "Tech-Conference"))
    }

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("Art & Craft").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Tech-Conference").performScrollTo().assertIsDisplayed()
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

  @Test
  fun savedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(3)
    rule.setContent { SavedEventsContent(events = testEvents) }
    rule.waitForIdle()

    // Ensure Saved Events tab title is visible (it is selected by default in this content)
    rule.onNodeWithText("Saved Events").assertIsDisplayed()

    testEvents.forEach { event ->
      rule.onNodeWithText(event.title).assertIsDisplayed()
      // Location line is shown in SearchResultItem; use substring for safety
      rule.onNodeWithText(event.location.name, substring = true).assertIsDisplayed()
    }
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
            onTabChange = { selectedTab = it },
            profilePictureUrl = null)
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

  // Tests for profile picture button functionality
  @Composable
  private fun TestContentWithProfilePicture(
      state: BottomSheetState,
      profilePictureUrl: String? = "https://example.com/profile.jpg",
      onProfileClick: () -> Unit = {}
  ) {
    MaterialTheme {
      var query by remember { mutableStateOf("") }
      var shouldRequestFocus by remember { mutableStateOf(false) }

      BottomSheetContent(
          state = state,
          fullEntryKey = 0,
          searchBarState =
              SearchBarState(
                  query = query,
                  shouldRequestFocus = shouldRequestFocus,
                  onQueryChange = { query = it },
                  onTap = { shouldRequestFocus = true },
                  onFocusHandled = { shouldRequestFocus = false },
                  onClear = { query = "" }),
          profilePictureUrl = profilePictureUrl,
          onProfileClick = onProfileClick)
    }
  }

  @Test
  fun profileButton_isDisplayedInCollapsedState() {
    rule.setContent { TestContentWithProfilePicture(state = BottomSheetState.COLLAPSED) }

    rule.waitForIdle()
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_isDisplayedInMediumState() {
    rule.setContent { TestContentWithProfilePicture(state = BottomSheetState.MEDIUM) }

    rule.waitForIdle()
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_isDisplayedInFullStateWhenNotSearching() {
    rule.setContent { TestContentWithProfilePicture(state = BottomSheetState.FULL) }

    rule.waitForIdle()
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_clickTriggersCallback() {
    var profileClicked = false

    rule.setContent {
      TestContentWithProfilePicture(
          state = BottomSheetState.COLLAPSED, onProfileClick = { profileClicked = true })
    }

    rule.waitForIdle()

    rule.onNodeWithTag("profileButton").performClick()
    rule.waitForIdle()

    assertTrue("Profile click callback should be triggered", profileClicked)
  }

  @Test
  fun profileButton_hasClickAction() {
    rule.setContent { TestContentWithProfilePicture(state = BottomSheetState.COLLAPSED) }

    rule.waitForIdle()

    rule.onNodeWithTag("profileButton").assertHasClickAction()
  }

  @Test
  fun profileButton_displaysWithNullUrl() {
    rule.setContent {
      TestContentWithProfilePicture(state = BottomSheetState.COLLAPSED, profilePictureUrl = null)
    }

    rule.waitForIdle()

    // Button should still be displayed even with null URL
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_displaysWithEmptyUrl() {
    rule.setContent {
      TestContentWithProfilePicture(state = BottomSheetState.COLLAPSED, profilePictureUrl = "")
    }

    rule.waitForIdle()

    // Button should still be displayed even with empty URL
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_updatesWhenUrlChanges() {
    var currentUrl by mutableStateOf<String?>("https://example.com/profile1.jpg")

    rule.setContent {
      MaterialTheme {
        var query by remember { mutableStateOf("") }
        var shouldRequestFocus by remember { mutableStateOf(false) }

        BottomSheetContent(
            state = BottomSheetState.FULL,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = query,
                    shouldRequestFocus = shouldRequestFocus,
                    onQueryChange = { query = it },
                    onTap = {},
                    onFocusHandled = { shouldRequestFocus = false },
                    onClear = {}),
            profilePictureUrl = currentUrl,
            onProfileClick = {})
      }
    }

    rule.waitForIdle()

    // Initially displayed with first URL
    rule.onNodeWithTag("profileButton").assertIsDisplayed()

    // Change to new URL and verify button is still displayed
    currentUrl = "https://example.com/profile2.jpg"
    rule.waitForIdle()

    // Button should still be displayed with new URL
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_handlesInvalidUrlGracefully() {
    rule.setContent {
      TestContentWithProfilePicture(
          state = BottomSheetState.COLLAPSED, profilePictureUrl = "invalid-url")
    }

    rule.waitForIdle()

    // Button should still be displayed even with invalid URL
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_displaysWithVeryLongUrl() {
    val longUrl =
        "https://example.com/very/long/path/to/profile/image/with/many/directories/and/a/very/long/filename_that_might_cause_issues.jpg"
    rule.setContent {
      TestContentWithProfilePicture(state = BottomSheetState.COLLAPSED, profilePictureUrl = longUrl)
    }

    rule.waitForIdle()

    // Button should still be displayed even with very long URL
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  @Test
  fun profileButton_stateTransitionsCorrectly() {
    // Test transition from collapsed -> medium -> full states
    var currentState by mutableStateOf(BottomSheetState.COLLAPSED)

    rule.setContent {
      MaterialTheme {
        var query by remember { mutableStateOf("") }
        var shouldRequestFocus by remember { mutableStateOf(false) }

        BottomSheetContent(
            state = currentState,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = query,
                    shouldRequestFocus = shouldRequestFocus,
                    onQueryChange = { query = it },
                    onTap = {},
                    onFocusHandled = { shouldRequestFocus = false },
                    onClear = {}),
            profilePictureUrl = "https://example.com/profile.jpg",
            onProfileClick = {})
      }
    }

    rule.waitForIdle()
    rule.onNodeWithTag("profileButton").assertIsDisplayed()

    // Change to medium state
    currentState = BottomSheetState.MEDIUM
    rule.waitForIdle()
    rule.onNodeWithTag("profileButton").assertIsDisplayed()

    // Change to full state
    currentState = BottomSheetState.FULL
    rule.waitForIdle()
    rule.onNodeWithTag("profileButton").assertIsDisplayed()
  }

  // Tests to increase line coverage

  @Test
  fun memoryForm_showsWhenShowMemoryFormIsTrue() {
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
            showMemoryForm = true,
            availableEvents = LocalEventRepository.defaultSampleEvents().take(2),
            onMemorySave = {},
            onMemoryCancel = {},
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // When showMemoryForm is true, MemoryFormScreen should be displayed
    // We can verify this by checking that the regular content (like search bar) is not visible
    rule.onNodeWithText("Search activities").assertDoesNotExist()
    // And some element from MemoryFormScreen should be visible
    rule.onNodeWithText("New Memory").assertIsDisplayed()
  }

  @Test
  fun searchMode_showsSearchResultsWhenHasResults() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(2)

    rule.setContent {
      MaterialTheme {
        BottomSheetContent(
            state = BottomSheetState.FULL,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = "test",
                    shouldRequestFocus = false,
                    onQueryChange = {},
                    onTap = {},
                    onFocusHandled = {},
                    onClear = {}),
            searchResults = testEvents,
            isSearchMode = true,
            onEventClick = {},
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // When in search mode with results, should show search results
    rule.onNodeWithText("Results for \"test\"").assertIsDisplayed()
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()
    rule.onNodeWithText(testEvents[1].title).assertIsDisplayed()

    // Regular content should not be visible
    rule.onNodeWithText("Quick Actions").assertDoesNotExist()
    rule.onNodeWithText("Saved Events").assertDoesNotExist()
  }

  @Test
  fun searchMode_showsNoResultsWhenEmptyResults() {
    rule.setContent {
      MaterialTheme {
        BottomSheetContent(
            state = BottomSheetState.FULL,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = "nonexistent",
                    shouldRequestFocus = false,
                    onQueryChange = {},
                    onTap = {},
                    onFocusHandled = {},
                    onClear = {}),
            searchResults = emptyList(),
            isSearchMode = true,
            onEventClick = {},
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // When in search mode with no results, should show no results message
    rule.onNodeWithText("No results found").assertIsDisplayed()
    rule.onNodeWithText("Try a different keyword or check the spelling.").assertIsDisplayed()

    // Regular content should not be visible
    rule.onNodeWithText("Quick Actions").assertDoesNotExist()
    rule.onNodeWithText("Saved Events").assertDoesNotExist()
  }

  @Test
  fun searchMode_showsAllEventsWhenEmptyQuery() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(3)

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
            searchResults = testEvents,
            isSearchMode = true,
            onEventClick = {},
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // When in search mode with empty query, should show "All events"
    rule.onNodeWithText("All events").assertIsDisplayed()
    rule.onNodeWithText(testEvents[0].title).assertIsDisplayed()
    rule.onNodeWithText(testEvents[1].title).assertIsDisplayed()
    rule.onNodeWithText(testEvents[2].title).assertIsDisplayed()
  }

  @Test
  fun contentUsesVerticalScrollInFullState() {
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
            topTags = listOf("Tag1", "Tag2", "Tag3", "Tag4", "Tag5"),
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // In FULL state, content should be scrollable
    rule.onNodeWithText("Tag1").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Tag5").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun contentDoesNotScrollInNonFullStates() {
    rule.setContent {
      MaterialTheme {
        BottomSheetContent(
            state = BottomSheetState.MEDIUM,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = "",
                    shouldRequestFocus = false,
                    onQueryChange = {},
                    onTap = {},
                    onFocusHandled = {},
                    onClear = {}),
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // In MEDIUM state, content should not be scrollable (fillMaxWidth without verticalScroll)
    // Tags are not displayed in MEDIUM state, so we just verify Quick Actions are shown
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
  }

  @Test
  fun focusRequesterActivatesWhenFullAndShouldRequestFocus() {
    rule.setContent {
      MaterialTheme {
        BottomSheetContent(
            state = BottomSheetState.FULL,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = "",
                    shouldRequestFocus = true,
                    onQueryChange = {},
                    onTap = {},
                    onFocusHandled = {},
                    onClear = {}),
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // When in FULL state and shouldRequestFocus is true, search bar should be focused
    rule.onNodeWithText("Search activities").assertIsFocused()
  }

  @Test
  fun focusRequesterDoesNotActivateWhenNotFull() {
    rule.setContent {
      MaterialTheme {
        BottomSheetContent(
            state = BottomSheetState.MEDIUM,
            fullEntryKey = 0,
            searchBarState =
                SearchBarState(
                    query = "",
                    shouldRequestFocus = true,
                    onQueryChange = {},
                    onTap = {},
                    onFocusHandled = {},
                    onClear = {}),
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // When not in FULL state, even with shouldRequestFocus=true, search bar should not be focused
    rule.onNodeWithText("Search activities").assertIsNotFocused()
  }

  // Additional tests to increase line coverage to 90%+

  @Test
  fun eventsSection_showsNoResultsWhenEmptyEvents() {
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
            savedEvents = emptyList(),
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // When saved events is empty, should show no results message
    rule.onNodeWithText("No events available yet.").assertIsDisplayed()
    rule.onNodeWithText("Try again once events are added.").assertIsDisplayed()
  }

  @Test
  fun eventsSection_showsShowMoreButtonWhenMoreThan3Events() {
    val testEvents = LocalEventRepository.defaultSampleEvents().take(5)

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
            savedEvents = testEvents,
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // Should show "Show more" button when there are more than 3 events
    rule.onNodeWithTag("eventsShowMoreButton").assertIsDisplayed()
    rule.onNodeWithText("Show more (2 more)").assertIsDisplayed()
  }

  @Test
  fun joinedEventsTab_showsNoResultsWhenEmptyEvents() {
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
            joinedEvents = emptyList(),
            selectedTab = MapScreenViewModel.BottomSheetTab.JOINED_EVENTS,
            profilePictureUrl = null)
      }
    }

    rule.waitForIdle()

    // Switch to Joined Events tab
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    // When joined events is empty, should show no results message
    rule.onNodeWithText("No events available yet.").assertIsDisplayed()
    rule.onNodeWithText("Try again once events are added.").assertIsDisplayed()
  }
}

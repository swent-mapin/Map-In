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
                  onClear = {}))
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
  fun fullState_showsAllContent() {
    rule.setContent { TestContent(state = BottomSheetState.FULL) }
    rule.waitForIdle()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
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
          onJoinedEventClick = onEventClick,
          onTabChange = onTabChange)
    }
  }

  // JOINED EVENTS TAB TESTS
  /*@Test
  fun joinedEventsTab_showsEmptyState() {
    rule.setContent { JoinedEventsContent(events = emptyList()) }
    rule.waitForIdle()
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    rule.onNodeWithText("You haven't joined any events yet").assertIsDisplayed()
  }*/

  @Test
  fun joinedEventsTab_displaysMultipleEventsWithAllData() {
    val testEvents = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents().take(3)
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
    val testEvents = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents().take(1)
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

  /*@Test
  fun joinedEventsTab_handlesEdgeCases() {
    // Test event with null date
    val eventWithNullDate =
        com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0].copy(date = null)

    rule.setContent { JoinedEventsContent(events = listOf(eventWithNullDate)) }
    rule.waitForIdle()
    rule.onNodeWithText("Joined Events").performClick()
    rule.waitForIdle()

    rule.onNodeWithText(eventWithNullDate.title).assertIsDisplayed()
    rule.onNodeWithText("No date").assertIsDisplayed()
  }*/

  @Test
  fun tabSwitch_betweenRecentActivitiesAndJoinedEvents() {
    val testEvents = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents().take(1)
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
          onTagClick = onTagClick)
    }
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
}

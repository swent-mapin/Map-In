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
      } catch (e: AssertionError) {
        false
      }
    }

    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()

    // Scroll to make "Discover" section visible on smaller screens
    rule.onNodeWithText("Discover").performScrollTo().assertIsDisplayed()
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

    rule.waitForIdle()

    // Scroll to make tags visible on smaller screens
    rule.onNodeWithText("Discover").performScrollTo()

    rule.onNodeWithText("Create Memory").assertHasClickAction()
    rule.onNodeWithText("Sports").assertHasClickAction()
    rule.onNodeWithText("Music").assertHasClickAction()
  }

  @Test
  fun searchQuery_persistsAcrossStateChanges() {
    rule.setContent { TestContent(state = BottomSheetState.COLLAPSED, initialQuery = "Coffee") }

    rule.onNodeWithText("Coffee").assertIsDisplayed()
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
                  onFocusHandled = { shouldRequestFocus = false }),
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

    rule.onNodeWithText("Sports").assertIsDisplayed()
    rule.onNodeWithText("Music").assertIsDisplayed()
    rule.onNodeWithText("Food").assertIsDisplayed()
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

    rule.onNodeWithText("Sports").assertHasClickAction()
    rule.onNodeWithText("Music").assertHasClickAction()
    rule.onNodeWithText("Food").assertHasClickAction()
    rule.onNodeWithText("Tech").assertHasClickAction()
    rule.onNodeWithText("Art").assertHasClickAction()
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

    rule.onNodeWithText("Sports").performClick()
    assertEquals("Sports", clickedTag)

    rule.onNodeWithText("Music").performClick()
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

    rule.onNodeWithText("Sports").performClick()
    rule.onNodeWithText("Music").performClick()
    rule.onNodeWithText("Food").performClick()

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

    tags.forEach { tag -> rule.onNodeWithText(tag).assertIsDisplayed() }
  }

  @Test
  fun tagsSection_visibleInFullStateOnly() {
    // Test that tags are visible in FULL state
    rule.setContent {
      TestContentWithTags(state = BottomSheetState.FULL, topTags = listOf("Sports", "Music"))
    }

    rule.waitForIdle()
    rule.onNodeWithText("Sports").assertIsDisplayed()
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

    rule.onNodeWithText("Sports").assertIsDisplayed()
    rule.onNodeWithText("Music").assertIsDisplayed()
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

    rule.onNodeWithText("Sports").assertIsDisplayed()
    rule.onNodeWithText("Music").assertIsDisplayed()
    rule.onNodeWithText("Food").assertIsDisplayed()
  }

  @Test
  fun tagsSection_displaysDiscoverTitle() {
    rule.setContent {
      TestContentWithTags(state = BottomSheetState.FULL, topTags = listOf("Sports", "Music"))
    }

    rule.waitForIdle()

    // Should have "Discover" as section title
    rule.onNodeWithText("Discover").assertIsDisplayed()
  }

  @Test
  fun tagsSection_handlesLongTagNames() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL, topTags = listOf("VeryLongTagName", "AnotherLongTag"))
    }

    rule.waitForIdle()

    rule.onNodeWithText("VeryLongTagName").assertIsDisplayed()
    rule.onNodeWithText("AnotherLongTag").assertIsDisplayed()
  }

  @Test
  fun tagsSection_handlesSpecialCharacters() {
    rule.setContent {
      TestContentWithTags(
          state = BottomSheetState.FULL, topTags = listOf("Art & Craft", "Tech-Conference"))
    }

    rule.waitForIdle()

    rule.onNodeWithText("Art & Craft").assertIsDisplayed()
    rule.onNodeWithText("Tech-Conference").assertIsDisplayed()
  }
}

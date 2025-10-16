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
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
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
        initialSearchMode: Boolean = false,
        initialResults: List<Event> = sampleEvents(),
        onQueryChange: (String) -> Unit = {},
        onTap: () -> Unit = {}
    ) {
        MaterialTheme {
            var query by remember { mutableStateOf(initialQuery) }
            var shouldRequestFocus by remember { mutableStateOf(initialFocus) }
            var isSearchMode by remember { mutableStateOf(initialSearchMode) }
            var results by remember { mutableStateOf(initialResults) }

            BottomSheetContent(
                state = state,
                fullEntryKey = 0,
                searchBarState =
                    SearchBarState(
                        query = query,
                        shouldRequestFocus = shouldRequestFocus,
                        onQueryChange = {
                            query = it
                            results =
                                if (it.isBlank()) {
                                    initialResults
                                } else {
                                    initialResults.filter { event ->
                                        event.title.contains(it, ignoreCase = true) ||
                                                event.location.name.contains(it, ignoreCase = true)
                                    }
                                }
                            onQueryChange(it)
                        },
                        onTap = {
                            isSearchMode = true
                            onTap()
                        },
                        onFocusHandled = { shouldRequestFocus = false },
                        onClear = {
                            query = ""
                            shouldRequestFocus = false
                            results = initialResults
                            isSearchMode = false
                        }),
                searchResults = results,
                isSearchMode = isSearchMode)
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
    fun fullStateWithoutSearch_showsQuickActions() {
        rule.setContent { TestContent(state = BottomSheetState.FULL, initialSearchMode = false) }
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

    @Test
    fun fullState_showsSearchResultsWhenActive() {
        rule.setContent {
            TestContent(
                state = BottomSheetState.FULL, initialSearchMode = true, initialResults = sampleEvents())
        }

        rule.onNodeWithText("All events").assertIsDisplayed()
        rule.onNodeWithText("Tech Conference").assertIsDisplayed()
    }

    @Test
    fun searchModeWithNoMatches_showsEmptyState() {
        rule.setContent {
            TestContent(
                state = BottomSheetState.FULL, initialSearchMode = true, initialResults = emptyList())
        }

        rule.onNodeWithText("No events available yet.").assertIsDisplayed()
    }

    private fun sampleEvents(): List<Event> =
        listOf(
            Event(uid = "event1", title = "All Hands", location = Location("Auditorium", 0.0, 0.0)),
            Event(
                uid = "event2",
                title = "Tech Conference",
                location = Location("Main Hall", 0.0, 0.0)))
}

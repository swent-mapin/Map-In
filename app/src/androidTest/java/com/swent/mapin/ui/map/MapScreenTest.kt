package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.swent.mapin.testing.UiTestTags
import org.junit.Rule
import org.junit.Test

class MapScreenTest {

  @get:Rule val rule = createComposeRule()

  // --- Helpers ---
  private fun setMap(renderMap: Boolean = false) {
    rule.setContent { MaterialTheme { MapScreen(renderMap = renderMap) } }
  }

  private fun goFullBySwipeUp() {
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeUp() }
    rule.waitUntil(10_000) {
      try {
        rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
        true
      } catch (_: AssertionError) {
        false
      }
    }
  }

  // --- Tests ---

  @Test
  fun renders_mapScreenRootAndSearchBar() {
    setMap()
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun collapsed_noInteractionBlocker() {
    setMap()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()
  }

  @Test
  fun swipeUp_toFull_showsBlocker_andSections() {
    setMap()
    goFullBySwipeUp()

    // In FULL state (not in search mode), default sections are visible
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
    rule.onNodeWithText("Discover").assertIsDisplayed()

    // Blocker must be present only in FULL
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun full_thenSwipeDown_hidesInteractionBlocker() {
    setMap()
    goFullBySwipeUp()

    // Leave FULL
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()

    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()
  }

  @Test
  fun tappingSearch_entersSearchMode_butBlockerDependsOnSheetState() {
    setMap()

    // Type to ensure the field works (search mode affects content, not blocker)
    rule.onNodeWithText("Search activities").performClick()
    rule.onNodeWithText("Search activities").performTextInput("music")
    rule.waitForIdle()

    // Still no blocker unless sheet is FULL
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()

    // Now push to FULL; blocker should appear
    goFullBySwipeUp()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun quickActionButtons_visible_inFull() {
    setMap()
    goFullBySwipeUp()
    rule.onNodeWithText("Create Memory").assertIsDisplayed()
    rule.onNodeWithText("Create Event").assertIsDisplayed()
    rule.onNodeWithText("Filters").assertIsDisplayed()
  }

  @Test
  fun scrimOverlay_visible_inAllStates() {
    setMap()
    // Scrim exists as a composable; opacity varies by height. We just assert presence.
    rule.onNodeWithTag("scrimOverlay").assertIsDisplayed()

    goFullBySwipeUp()
    rule.onNodeWithTag("scrimOverlay").assertIsDisplayed()
  }

  @Test
  fun searchText_persistsWhileEditing() {
    setMap()
    rule.onNodeWithText("Search activities").performClick()
    rule.onNodeWithText("Search activities").performTextInput("basketball")
    rule.waitForIdle()
    rule.onNodeWithText("basketball").assertIsDisplayed()
  }

  @Test
  fun full_flow_sectionsStillVisibleAfterMultipleTransitions() {
    setMap()

    // Full
    goFullBySwipeUp()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()

    // Down to medium/collapsed
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()

    // Full again
    goFullBySwipeUp()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
  }
}

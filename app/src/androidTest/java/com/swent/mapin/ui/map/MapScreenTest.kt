package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.swent.mapin.testing.UiTestTags
import org.junit.Rule
import org.junit.Test

/**
 * MapScreen tests aligned with the current implementation:
 * - Avoids Mapbox by using renderMap = false
 * - Verifies search bar, sections, scrim, interaction blocker, and profile FAB
 */
class MapScreenTest {

  @get:Rule val rule = createComposeRule()

  private fun setScreen() {
    rule.setContent { MaterialTheme { MapScreen(renderMap = false) } }
  }

  @Test
  fun mapScreen_composes_andShowsBasicChrome() {
    setScreen()

    // Root screen
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()

    // Search bar always visible
    rule.onNodeWithText("Search activities", useUnmergedTree = true).assertIsDisplayed()

    // Scrim overlay is always present (alpha varies by state)
    rule.onNodeWithTag("scrimOverlay", useUnmergedTree = true).assertExists()

    // Blocker appears only in FULL; initial should not have it
    rule.onNodeWithTag("mapInteractionBlocker", useUnmergedTree = true).assertDoesNotExist()

    // Profile FAB exists
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun tappingSearch_entersFull_andShowsSectionsAndBlocker() {
    setScreen()

    // Tapping the field expands sheet (your SearchBar does this on focus/tap)
    rule.onNodeWithText("Search activities").performClick()

    // Wait a bit for the animated content to settle
    rule.waitUntil(5_000) {
      try {
        rule.onNodeWithText("Quick Actions").assertExists()
        true
      } catch (_: AssertionError) {
        false
      }
    }

    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
    rule.onNodeWithText("Discover").assertIsDisplayed()

    // In FULL state, the map interaction blocker should be visible
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun full_thenSwipeDown_hidesInteractionBlocker() {
    setScreen()

    // Go FULL
    rule.onNodeWithText("Search activities").performClick()
    rule.waitUntil(5_000) {
      try {
        rule.onNodeWithTag("mapInteractionBlocker").assertExists()
        true
      } catch (_: AssertionError) {
        false
      }
    }
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()

    // Collapse sheet by swiping it down
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }

    // Wait for state to settle out of FULL
    rule.waitUntil(5_000) {
      try {
        rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()
        true
      } catch (_: AssertionError) {
        false
      }
    }
  }

  @Test
  fun profileButton_isVisible_andClickable() {
    setScreen()
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).assertIsDisplayed()
    // No-op click (we’re not asserting navigation here)
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()
    rule.onNodeWithTag("profileButton", useUnmergedTree = true).assertIsDisplayed()
  }
}

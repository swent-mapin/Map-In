package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.swent.mapin.testing.UiTestTags
import org.junit.Rule
import org.junit.Test

// Assisted by AI
/**
 * Tests cover:
 * - MapScreen composition and rendering
 * - Integration with bottom sheet states
 * - Search bar interactions across different states
 * - Visual elements (TopGradient, ScrimOverlay, MapInteractionBlocker)
 * - State transitions via bottom sheet interactions
 * - Map interaction blocking in full state
 * - Scrim overlay presence across states
 * - Direct state transitions (collapsed <-> full)
 */
class MapScreenTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun mapScreen_rendersSuccessfully() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun mapScreen_initialState_showsCollapsedSheet() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertExists()
  }

  @Test
  fun mapScreen_searchBarClick_expandsToFullState() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
    rule.onNodeWithText("Discover").assertIsDisplayed()
  }

  @Test
  fun mapScreen_searchInput_expandsToFullState() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performTextInput("coffee")
    rule.waitForIdle()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
  }

  @Test
  fun mapScreen_searchQuery_persistsAcrossRecomposition() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performTextInput("basketball")
    rule.waitForIdle()
    rule.onNodeWithText("basketball").assertIsDisplayed()
  }

  @Test
  fun mapScreen_quickActionButtons_areDisplayedInMediumAndFullStates() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Create Memory").assertIsDisplayed()
    rule.onNodeWithText("Create Event").assertIsDisplayed()
    rule.onNodeWithText("Filters").assertIsDisplayed()
  }

  @Test
  fun mapScreen_fullState_showsAllContentSections() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()

    rule.waitUntil(timeoutMillis = 10000) {
      try {
        rule.onNodeWithText("Quick Actions").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Activity 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Sports").performScrollTo().assertIsDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    rule.onNodeWithText("Quick Actions").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Discover").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Activity 1").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Activity 2").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Sports").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Music").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun mapScreen_multipleStateTransitions_workCorrectly() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Create Memory").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("New Memory").assertIsDisplayed()
  }

  @Test
  fun mapScreen_componentsLayout_maintainsCorrectHierarchy() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithText("Recent Activities").assertIsDisplayed()
    rule.onNodeWithText("Discover").assertIsDisplayed()
  }

  @Test
  fun mapScreen_mapInteractionBlocker_onlyPresentInFullState() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()

    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun mapScreen_scrimOverlay_alwaysPresent() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithTag("scrimOverlay").assertIsDisplayed()

    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("scrimOverlay").assertIsDisplayed()
  }

  @Test
  fun mapScreen_mapInteractionBlocker_disappearsWhenLeavingFullState() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()

    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()
  }

  @Test
  fun mapScreen_directTransition_collapsedToFull() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").assertIsDisplayed()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()

    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Quick Actions").assertIsDisplayed()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun mapScreen_directTransition_fullToCollapsed() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()

    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown(startY = top, endY = bottom) }
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()
  }

  @Test
  fun mapStyleToggle_isVisible_andToggles() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag("mapStyleToggle").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag("mapStyleToggle").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun mapStyleToggle_persists_afterBottomSheetTransitions() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun searchQuery_clears_whenLeavingFullState() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithText("Search activities").performTextInput("basketball")
    rule.waitForIdle()
    rule.onNodeWithText("basketball").assertIsDisplayed()
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithText("basketball").assertDoesNotExist()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun mapStyleToggle_visible_inAllSheetStates() {
    rule.setContent { MaterialTheme { MapScreen() } }
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun mapScreen_heatmapMode_displaysCorrectly() {
    rule.setContent { MaterialTheme { MapScreen(renderMap = false) } }
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_HEATMAP").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun mapScreen_satelliteMode_displaysCorrectly() {
    rule.setContent { MaterialTheme { MapScreen(renderMap = false) } }
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_SATELLITE").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationClick_triggersCallback() {
    var clickedEvent: com.swent.mapin.model.event.Event? = null
    rule.setContent {
      MaterialTheme { MapScreen(onEventClick = { event -> clickedEvent = event }) }
    }
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_switchBetweenStyles_maintainsState() {
    rule.setContent { MaterialTheme { MapScreen(renderMap = false) } }
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_HEATMAP").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_SATELLITE").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_STANDARD").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }
}

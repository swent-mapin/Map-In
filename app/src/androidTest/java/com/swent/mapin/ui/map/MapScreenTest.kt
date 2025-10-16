package com.swent.mapin.ui.map

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Ultra-minimal smoke tests for MapScreen. No swipes, no Mapbox work, no blockers/scrim assertions
 * — just the basics.
 */
class MapScreenTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_andShowsSearchBar() {
    rule.setContent { MaterialTheme { MapScreen(renderMap = false) } }
    rule.onNodeWithText("Search activities").assertIsDisplayed()
  }

  @Test
  fun tappingSearch_entersSearchMode_showsResultsHeader() {
    rule.setContent { MaterialTheme { MapScreen(renderMap = false) } }

    // tap the field → search mode (Results header)
    rule.onNodeWithText("Search activities").performClick()
    rule.waitForIdle()

    rule.onNodeWithText("Results").assertIsDisplayed()
    rule.onNodeWithText("Clear").assertIsDisplayed()
  }

  @Test
  fun typing_updatesSearchFieldText() {
    rule.setContent { MaterialTheme { MapScreen(renderMap = false) } }

    rule.onNodeWithText("Search activities").performClick()
    rule.onNodeWithText("Search activities").performTextInput("rock")
    rule.waitForIdle()

    // typed text is visible
    rule.onNodeWithText("rock").assertIsDisplayed()
  }
}

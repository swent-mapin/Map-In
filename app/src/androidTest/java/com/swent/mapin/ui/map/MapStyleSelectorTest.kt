package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapStyleSelectorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun mapStyleSelector_menuShowsOptionsAndSelectsStyle() {
    val selected = mutableStateOf(MapScreenViewModel.MapStyle.STANDARD)

    composeTestRule.setContent {
      MaterialTheme {
        MapStyleSelector(
            selectedStyle = selected.value, onStyleSelected = { style -> selected.value = style })
      }
    }

    composeTestRule.onNodeWithTag("mapStyleToggle").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("mapStyleOption_HEATMAP").assertIsDisplayed().performClick()

    composeTestRule.runOnIdle { assertEquals(MapScreenViewModel.MapStyle.HEATMAP, selected.value) }
    composeTestRule.onNodeWithTag("mapStyleMenu").assertDoesNotExist()
  }

  @Test
  fun mapStyleSelector_toggleReopensMenu() {
    composeTestRule.setContent {
      MaterialTheme {
        MapStyleSelector(
            selectedStyle = MapScreenViewModel.MapStyle.SATELLITE, onStyleSelected = {})
      }
    }

    composeTestRule.onNodeWithTag("mapStyleToggle").performClick()
    composeTestRule.onNodeWithTag("mapStyleMenu").assertIsDisplayed()

    composeTestRule.onNodeWithTag("mapStyleToggle").performClick()
    composeTestRule.onNodeWithTag("mapStyleMenu").assertDoesNotExist()

    composeTestRule.onNodeWithTag("mapStyleToggle").performClick()
    composeTestRule.onNodeWithTag("mapStyleMenu").assertIsDisplayed()
  }
}

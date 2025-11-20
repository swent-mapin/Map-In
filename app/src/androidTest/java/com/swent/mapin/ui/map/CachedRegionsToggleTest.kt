package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CachedRegionsToggleTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun cachedRegionsToggle_isDisplayed() {
    composeTestRule.setContent {
      MaterialTheme { CachedRegionsToggle(showCachedRegions = false, onClick = {}) }
    }

    composeTestRule.onNodeWithTag("cachedRegionsToggle").assertIsDisplayed()
  }

  @Test
  fun cachedRegionsToggle_hasClickAction() {
    composeTestRule.setContent {
      MaterialTheme { CachedRegionsToggle(showCachedRegions = false, onClick = {}) }
    }

    composeTestRule.onNodeWithTag("cachedRegionsToggle").assertHasClickAction()
  }

  @Test
  fun cachedRegionsToggle_onClick_isCalled() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme { CachedRegionsToggle(showCachedRegions = false, onClick = { clicked = true }) }
    }

    composeTestRule.onNodeWithTag("cachedRegionsToggle").performClick()

    assertTrue(clicked)
  }

  @Test
  fun cachedRegionsToggle_displaysCorrectIcon_whenHidden() {
    composeTestRule.setContent {
      MaterialTheme { CachedRegionsToggle(showCachedRegions = false, onClick = {}) }
    }

    composeTestRule.onNodeWithContentDescription("Show cached areas").assertIsDisplayed()
  }

  @Test
  fun cachedRegionsToggle_displaysCorrectIcon_whenVisible() {
    composeTestRule.setContent {
      MaterialTheme { CachedRegionsToggle(showCachedRegions = true, onClick = {}) }
    }

    composeTestRule.onNodeWithContentDescription("Hide cached areas").assertIsDisplayed()
  }

  @Test
  fun cachedRegionsToggle_canBeClickedMultipleTimes() {
    var clickCount = 0
    composeTestRule.setContent {
      MaterialTheme { CachedRegionsToggle(showCachedRegions = false, onClick = { clickCount++ }) }
    }

    composeTestRule.onNodeWithTag("cachedRegionsToggle").performClick()
    composeTestRule.onNodeWithTag("cachedRegionsToggle").performClick()
    composeTestRule.onNodeWithTag("cachedRegionsToggle").performClick()

    assertEquals(3, clickCount)
  }

  @Test
  fun cachedRegionsToggle_changesState() {
    var isVisible by mutableStateOf(false)
    composeTestRule.setContent {
      MaterialTheme {
        CachedRegionsToggle(showCachedRegions = isVisible, onClick = { isVisible = !isVisible })
      }
    }

    // Initially shows "Show cached areas"
    composeTestRule.onNodeWithContentDescription("Show cached areas").assertIsDisplayed()

    // Click to toggle
    composeTestRule.onNodeWithTag("cachedRegionsToggle").performClick()
    composeTestRule.waitForIdle()

    // Now shows "Hide cached areas"
    composeTestRule.onNodeWithContentDescription("Hide cached areas").assertIsDisplayed()
  }
}

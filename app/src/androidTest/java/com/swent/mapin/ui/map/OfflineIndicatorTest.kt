package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class OfflineIndicatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun offlineIndicator_isDisplayed_whenOffline() {
    composeTestRule.setContent {
      MaterialTheme { OfflineIndicator(isOffline = true, isInCachedRegion = false) }
    }

    composeTestRule.onNodeWithTag("offlineIndicator").assertIsDisplayed()
  }

  @Test
  fun offlineIndicator_isNotDisplayed_whenOnline() {
    composeTestRule.setContent {
      MaterialTheme { OfflineIndicator(isOffline = false, isInCachedRegion = false) }
    }

    composeTestRule.onNodeWithTag("offlineIndicator").assertDoesNotExist()
  }

  @Test
  fun offlineIndicator_displaysOfflineIcon() {
    composeTestRule.setContent {
      MaterialTheme { OfflineIndicator(isOffline = true, isInCachedRegion = false) }
    }

    composeTestRule.onNodeWithContentDescription("Offline").assertIsDisplayed()
  }

  @Test
  fun offlineIndicator_displaysCachedRegionText() {
    composeTestRule.setContent {
      MaterialTheme { OfflineIndicator(isOffline = true, isInCachedRegion = true) }
    }

    composeTestRule.onNodeWithContentDescription("Offline - Cached area").assertIsDisplayed()
  }

  @Test
  fun offlineIndicator_animatesVisibility() {
    var isOffline by mutableStateOf(false)

    composeTestRule.setContent {
      MaterialTheme { OfflineIndicator(isOffline = isOffline, isInCachedRegion = false) }
    }

    // Initially not displayed
    composeTestRule.onNodeWithTag("offlineIndicator").assertDoesNotExist()

    // Change to offline
    isOffline = true
    composeTestRule.waitForIdle()

    // Now should be displayed
    composeTestRule.onNodeWithTag("offlineIndicator").assertIsDisplayed()
  }
}

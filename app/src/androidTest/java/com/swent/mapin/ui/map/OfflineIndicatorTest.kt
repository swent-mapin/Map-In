package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
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
    composeTestRule.setContent { MaterialTheme { OfflineIndicator(isOffline = true) } }

    composeTestRule.onNodeWithTag("offlineIndicator").assertIsDisplayed()
  }

  @Test
  fun offlineIndicator_isNotDisplayed_whenOnline() {
    composeTestRule.setContent { MaterialTheme { OfflineIndicator(isOffline = false) } }

    composeTestRule.onNodeWithTag("offlineIndicator").assertDoesNotExist()
  }

  @Test
  fun offlineIndicator_displaysOfflineIcon() {
    composeTestRule.setContent { MaterialTheme { OfflineIndicator(isOffline = true) } }

    composeTestRule.onNodeWithContentDescription("Offline").assertIsDisplayed()
  }

  @Test
  fun offlineIndicator_animatesVisibility() {
    composeTestRule.setContent { MaterialTheme { OfflineIndicator(isOffline = false) } }

    // Initially not displayed
    composeTestRule.onNodeWithTag("offlineIndicator").assertDoesNotExist()

    // Change to offline
    composeTestRule.setContent { MaterialTheme { OfflineIndicator(isOffline = true) } }

    // Now should be displayed
    composeTestRule.onNodeWithTag("offlineIndicator").assertIsDisplayed()
  }
}

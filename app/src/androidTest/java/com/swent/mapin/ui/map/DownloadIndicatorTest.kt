package com.swent.mapin.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import org.junit.Rule
import org.junit.Test

class DownloadIndicatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testEvent =
      Event(
          uid = "test-event-1",
          title = "Test Event",
          location = Location("Test Location", 46.5, 6.5))

  @Test
  fun downloadIndicator_notVisible_whenNoDownloadAndNoComplete() {
    composeTestRule.setContent {
      DownloadIndicator(
          downloadingEvent = null, downloadProgress = 0f, showDownloadComplete = false)
    }

    composeTestRule.onNodeWithTag("downloadIndicatorProgress").assertDoesNotExist()
    composeTestRule.onNodeWithTag("downloadIndicatorComplete").assertDoesNotExist()
  }

  @Test
  fun downloadIndicator_showsProgress_whenDownloading() {
    composeTestRule.setContent {
      DownloadIndicator(
          downloadingEvent = testEvent, downloadProgress = 0.5f, showDownloadComplete = false)
    }

    composeTestRule.onNodeWithTag("downloadIndicatorProgress").assertIsDisplayed()
    composeTestRule.onNodeWithText("Downloading Test Event").assertIsDisplayed()
  }

  @Test
  fun downloadIndicator_showsComplete_whenDownloadFinished() {
    composeTestRule.setContent {
      DownloadIndicator(downloadingEvent = null, downloadProgress = 0f, showDownloadComplete = true)
    }

    composeTestRule.onNodeWithTag("downloadIndicatorComplete").assertIsDisplayed()
    composeTestRule.onNodeWithText("Downloads complete").assertIsDisplayed()
  }

  @Test
  fun downloadIndicator_hidesComplete_whenDownloading() {
    composeTestRule.setContent {
      DownloadIndicator(
          downloadingEvent = testEvent, downloadProgress = 0.3f, showDownloadComplete = true)
    }

    // Progress should be visible
    composeTestRule.onNodeWithTag("downloadIndicatorProgress").assertIsDisplayed()
    // Complete should not be visible while downloading
    composeTestRule.onNodeWithTag("downloadIndicatorComplete").assertDoesNotExist()
  }

  @Test
  fun downloadIndicator_updatesProgress_whenProgressChanges() {
    composeTestRule.setContent {
      DownloadIndicator(
          downloadingEvent = testEvent, downloadProgress = 0.75f, showDownloadComplete = false)
    }

    composeTestRule.onNodeWithTag("downloadIndicatorProgress").assertIsDisplayed()
    composeTestRule.onNodeWithText("Downloading Test Event").assertIsDisplayed()
  }
}

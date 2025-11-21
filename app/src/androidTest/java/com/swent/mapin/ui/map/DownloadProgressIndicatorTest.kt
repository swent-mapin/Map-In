package com.swent.mapin.ui.map

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swent.mapin.ui.map.offline.DownloadProgress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadProgressIndicatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun indicatorIsHiddenWhenIdle() {
    composeTestRule.setContent {
      DownloadProgressIndicator(downloadProgress = DownloadProgress.Idle)
    }

    composeTestRule.onNodeWithTag("downloadProgressIndicator").assertDoesNotExist()
  }

  @Test
  fun indicatorIsDisplayedWhenDownloading() {
    composeTestRule.setContent {
      DownloadProgressIndicator(downloadProgress = DownloadProgress.Downloading(2, 5))
    }

    composeTestRule.onNodeWithTag("downloadProgressIndicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Downloading offline maps").assertIsDisplayed()
  }

  @Test
  fun indicatorIsDisplayedWhenComplete() {
    composeTestRule.setContent {
      DownloadProgressIndicator(downloadProgress = DownloadProgress.Complete)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("downloadProgressIndicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Offline maps ready").assertIsDisplayed()
  }

  @Test
  fun indicatorChangesFromDownloadingToComplete() {
    val downloadProgressState = mutableStateOf<DownloadProgress>(DownloadProgress.Downloading(1, 3))

    composeTestRule.setContent {
      DownloadProgressIndicator(downloadProgress = downloadProgressState.value)
    }

    // Initially downloading
    composeTestRule.onNodeWithTag("downloadProgressIndicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Downloading offline maps").assertIsDisplayed()

    // Change to complete
    downloadProgressState.value = DownloadProgress.Complete
    composeTestRule.waitForIdle()

    // Should show complete message
    composeTestRule.onNodeWithTag("downloadProgressIndicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Offline maps ready").assertIsDisplayed()
  }

  @Test
  fun indicatorChangesFromIdleToDownloading() {
    val downloadProgressState = mutableStateOf<DownloadProgress>(DownloadProgress.Idle)

    composeTestRule.setContent {
      DownloadProgressIndicator(downloadProgress = downloadProgressState.value)
    }

    // Initially hidden
    composeTestRule.onNodeWithTag("downloadProgressIndicator").assertDoesNotExist()

    // Change to downloading
    downloadProgressState.value = DownloadProgress.Downloading(1, 2)
    composeTestRule.waitForIdle()

    // Should show downloading message
    composeTestRule.onNodeWithTag("downloadProgressIndicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Downloading offline maps").assertIsDisplayed()
  }
}

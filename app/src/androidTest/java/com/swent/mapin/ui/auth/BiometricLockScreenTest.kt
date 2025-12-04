package com.swent.mapin.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BiometricLockScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun displaysAllMainElements() {
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen() } }
    composeTestRule.onNodeWithTag("biometricLockScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("biometricLockScreen_logo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("biometricLockScreen_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("biometricLockScreen_fingerprintButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("biometricLockScreen_statusText").assertIsDisplayed()
    composeTestRule.onNodeWithTag("biometricLockScreen_useAnotherAccountButton").assertIsDisplayed()
  }

  @Test
  fun displaysCorrectTitle() {
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen() } }
    composeTestRule.onNodeWithTag("biometricLockScreen_title").assertTextEquals("Unlock Map'In")
  }

  @Test
  fun showsTapToUnlockByDefault() {
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen() } }
    composeTestRule
        .onNodeWithTag("biometricLockScreen_statusText")
        .assertTextEquals("Tap to unlock")
  }

  @Test
  fun showsVerifyingWhenAuthenticating() {
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen(isAuthenticating = true) } }
    composeTestRule.onNodeWithTag("biometricLockScreen_statusText").assertTextEquals("Verifying...")
  }

  @Test
  fun hidesUseAnotherAccountButtonWhenAuthenticating() {
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen(isAuthenticating = true) } }
    composeTestRule
        .onNodeWithTag("biometricLockScreen_useAnotherAccountButton")
        .assertDoesNotExist()
  }

  @Test
  fun displaysErrorMessage() {
    val error = "Authentication failed"
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen(errorMessage = error) } }
    composeTestRule
        .onNodeWithTag("biometricLockScreen_errorMessage")
        .assertIsDisplayed()
        .assertTextEquals(error)
  }

  @Test
  fun showsTapToTryAgainWhenError() {
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen(errorMessage = "Error") } }
    composeTestRule
        .onNodeWithTag("biometricLockScreen_statusText")
        .assertTextEquals("Tap to try again")
  }

  @Test
  fun hidesErrorMessageWhenNull() {
    composeTestRule.setContent { MaterialTheme { BiometricLockScreen(errorMessage = null) } }
    composeTestRule.onNodeWithTag("biometricLockScreen_errorMessage").assertDoesNotExist()
  }

  @Test
  fun fingerprintButtonCallsOnRetry() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme { BiometricLockScreen(onRetry = { clicked = true }) }
    }
    composeTestRule.onNodeWithTag("biometricLockScreen_fingerprintButton").performClick()
    assertTrue(clicked)
  }

  @Test
  fun fingerprintButtonDisabledWhenAuthenticating() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme { BiometricLockScreen(isAuthenticating = true, onRetry = { clicked = true }) }
    }
    composeTestRule.onNodeWithTag("biometricLockScreen_fingerprintButton").performClick()
    assertFalse(clicked)
  }

  @Test
  fun useAnotherAccountButtonCallsCallback() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme { BiometricLockScreen(onUseAnotherAccount = { clicked = true }) }
    }
    composeTestRule.onNodeWithTag("biometricLockScreen_useAnotherAccountButton").performClick()
    assertTrue(clicked)
  }
}

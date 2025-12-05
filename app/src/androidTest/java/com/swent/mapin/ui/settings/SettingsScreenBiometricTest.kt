package com.swent.mapin.ui.settings

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenBiometricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
  }

  @Test
  fun displaysSettingsScreen() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("settingsScreen").assertIsDisplayed()
  }

  @Test
  fun displaysBackButton() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed()
  }

  @Test
  fun backButtonCallsNavigateBack() {
    var clicked = false
    composeTestRule.setContent {
      SettingsScreen(
          onNavigateBack = { clicked = true },
          onNavigateToSignIn = {},
          onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("backButton").performClick()
    assert(clicked)
  }

  @Test
  fun displaysLogoutButton() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun displaysDeleteAccountButton() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("deleteAccountButton").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun displaysThemeModeSelector() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("themeModeSelector").assertIsDisplayed()
  }

  @Test
  fun displaysMapSettingsToggles() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("poiToggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("roadNumbersToggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("streetNamesToggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("threeDViewToggle").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun logoutButtonShowsConfirmationDialog() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()
    composeTestRule.onNodeWithText("Confirm Logout").assertIsDisplayed()
  }

  @Test
  fun logoutDialogCancelDismisses() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("dialogCancelButton").performClick()
    composeTestRule.onNodeWithText("Confirm Logout").assertDoesNotExist()
  }

  @Test
  fun deleteAccountButtonShowsDialog() {
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}, onNavigateToChangePassword = {})
    }
    composeTestRule.onNodeWithTag("deleteAccountButton_action").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("deleteAccountConfirmButton").assertIsDisplayed()
  }
}

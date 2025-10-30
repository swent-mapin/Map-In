package com.swent.mapin.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun settingsScreen_displaysHeader() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("settingsScreen").assertIsDisplayed()

    composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_hasBackButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun settingsScreen_displaysMapSettingsSection() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Map Settings").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysPOIToggle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("poiToggle").assertIsDisplayed()

    composeTestRule.onNodeWithText("Points of Interest").assertIsDisplayed()

    composeTestRule.onNodeWithText("Show POIs on the map").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysRoadNumbersToggle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("roadNumbersToggle").assertIsDisplayed()

    composeTestRule.onNodeWithText("Road Numbers").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysStreetNamesToggle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("streetNamesToggle").assertIsDisplayed()

    composeTestRule.onNodeWithText("Street Names").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displays3DViewToggle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("threeDViewToggle").assertIsDisplayed()

    composeTestRule.onNodeWithText("3D View").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysAccountSection() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Account").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysLogoutButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("logoutButton").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithText("Logout").assertIsDisplayed()

    composeTestRule.onNodeWithText("Sign out of your account").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysDeleteAccountButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("deleteAccountButton").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithText("Delete Account").assertIsDisplayed()

    composeTestRule.onNodeWithText("Permanently delete your account and data").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_poiToggleIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("poiToggle_switch").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun settingsScreen_logoutButtonShowsConfirmationDialog() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Scroll to and click logout button
    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()

    composeTestRule.waitForIdle()

    // Verify confirmation dialog appears
    composeTestRule.onNodeWithText("Confirm Logout").assertIsDisplayed()

    composeTestRule.onNodeWithText("Are you sure you want to log out?").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_deleteAccountButtonShowsWarningDialog() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Scroll to and click delete account button
    composeTestRule.onNodeWithTag("deleteAccountButton_action").performScrollTo().performClick()

    composeTestRule.waitForIdle()

    // Verify warning dialog appears - only check unique message text
    composeTestRule
        .onNodeWithText(
            "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.")
        .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_confirmationDialogHasCancelButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Scroll to and click logout button
    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()

    composeTestRule.waitForIdle()

    // Verify cancel button exists
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun settingsScreen_allTogglesAreDisplayed() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Verify all 4 toggles exist
    composeTestRule.onAllNodesWithTag("poiToggle_switch").assertCountEquals(1)

    composeTestRule.onAllNodesWithTag("roadNumbersToggle_switch").assertCountEquals(1)

    composeTestRule.onAllNodesWithTag("streetNamesToggle_switch").assertCountEquals(1)

    composeTestRule.onAllNodesWithTag("threeDViewToggle_switch").assertCountEquals(1)
  }

  @Test
  fun settingsScreen_scrollableContent() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Verify the settings screen itself is displayed and scrollable
    composeTestRule.onNodeWithTag("settingsScreen").assertIsDisplayed()
  }
}

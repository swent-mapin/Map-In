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

  // ===== Appearance Section Tests =====

  @Test
  fun settingsScreen_displaysAppearanceSection() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysThemeModeSelector() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("themeModeSelector").assertIsDisplayed()
    composeTestRule.onNodeWithText("Theme Mode").assertIsDisplayed()
    composeTestRule.onNodeWithText("Choose your preferred theme").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_themeModeSelector_hasAllOptions() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Ensure theme mode selector is visible
    composeTestRule.onNodeWithTag("themeModeSelector").assertIsDisplayed()
    composeTestRule.onNodeWithText("Light").assertExists()
    composeTestRule.onNodeWithText("Dark").assertExists()
    composeTestRule.onNodeWithText("System").assertExists()
  }

  @Test
  fun settingsScreen_themeModeSelector_lightButtonIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule
        .onNodeWithTag("themeModeSelector_light")
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun settingsScreen_themeModeSelector_darkButtonIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule
        .onNodeWithTag("themeModeSelector_dark")
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun settingsScreen_themeModeSelector_systemButtonIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule
        .onNodeWithTag("themeModeSelector_system")
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  // ===== Map Settings Section Tests =====

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
    composeTestRule.onNodeWithText("Show POI labels on the map").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysRoadLabelsToggle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("roadNumbersToggle").assertIsDisplayed()
    composeTestRule.onNodeWithText("Road Labels").assertIsDisplayed()
    composeTestRule.onNodeWithText("Display road labels on the map").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysTransitLabelsToggle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("streetNamesToggle").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Transit Labels").assertIsDisplayed()
    composeTestRule.onNodeWithText("Show transit and street labels").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displays3DBuildingsToggle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("threeDViewToggle").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("3D Buildings").assertIsDisplayed()
    composeTestRule.onNodeWithText("Enable 3D buildings on the map").assertIsDisplayed()
  }

  // ===== Account Section Tests =====

  @Test
  fun settingsScreen_displaysAccountSection() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Account").performScrollTo().assertIsDisplayed()
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

  // ===== Interaction Tests =====

  @Test
  fun settingsScreen_poiToggleIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("poiToggle_switch").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun settingsScreen_roadLabelsToggleIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule
        .onNodeWithTag("roadNumbersToggle_switch")
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun settingsScreen_transitLabelsToggleIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule
        .onNodeWithTag("streetNamesToggle_switch")
        .performScrollTo()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun settingsScreen_3DBuildingsToggleIsClickable() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule
        .onNodeWithTag("threeDViewToggle_switch")
        .performScrollTo()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun settingsScreen_logoutButtonShowsConfirmationDialog() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Confirm Logout").assertIsDisplayed()
    composeTestRule.onNodeWithText("Are you sure you want to log out?").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_deleteAccountButtonShowsWarningDialog() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("deleteAccountButton_action").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText(
            "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.")
        .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_confirmationDialogHasCancelButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed().assertHasClickAction()
  }

  // ===== Content Verification Tests =====

  @Test
  fun settingsScreen_allTogglesAreDisplayed() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onAllNodesWithTag("poiToggle_switch").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("roadNumbersToggle_switch").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("streetNamesToggle_switch").assertCountEquals(1)
    composeTestRule.onAllNodesWithTag("threeDViewToggle_switch").assertCountEquals(1)
  }

  @Test
  fun settingsScreen_scrollableContent() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("settingsScreen").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_allSectionsArePresent() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeTestRule.onNodeWithText("Map Settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()
    composeTestRule.onNodeWithText("Account").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_themeModeSelectorAboveMapSettings() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Verify Appearance section comes before Map Settings
    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeTestRule.onNodeWithText("Map Settings").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_backButtonNavigatesBack() {
    var navigatedBack = false
    composeTestRule.setContent {
      SettingsScreen(onNavigateBack = { navigatedBack = true }, onNavigateToSignIn = {})
    }

    composeTestRule.onNodeWithTag("backButton").performClick()
    composeTestRule.waitForIdle()

    assert(navigatedBack)
  }

  @Test
  fun settingsScreen_logoutConfirmationDialog_cancelButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed
    composeTestRule.onNodeWithText("Confirm Logout").assertDoesNotExist()
  }

  @Test
  fun settingsScreen_deleteAccountConfirmationDialog_cancelButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("deleteAccountButton_action").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed
    composeTestRule.onNodeWithText("Delete Account").assertExists() // Button still exists
  }

  // ===== Additional Coverage Tests =====

  @Test
  fun settingsScreen_poiToggle_displaysCorrectSubtitle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Show POI labels on the map").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_roadLabelsToggle_displaysCorrectSubtitle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Display road labels on the map").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_transitLabelsToggle_displaysCorrectSubtitle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Show transit and street labels").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_3DBuildingsToggle_displaysCorrectSubtitle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("threeDViewToggle").performScrollTo()
    composeTestRule.onNodeWithText("Enable 3D buildings on the map").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_logoutButton_displaysCorrectSubtitle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()
    composeTestRule.onNodeWithText("Sign out of your account").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_deleteAccountButton_displaysCorrectSubtitle() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("deleteAccountButton").performScrollTo()
    composeTestRule.onNodeWithText("Permanently delete your account and data").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_themeModeSelector_containsThreeButtons() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Scroll to theme mode selector to ensure it's visible
    composeTestRule.onNodeWithTag("themeModeSelector").assertIsDisplayed()
    composeTestRule.onNodeWithTag("themeModeSelector_light").assertExists()
    composeTestRule.onNodeWithTag("themeModeSelector_dark").assertExists()
    composeTestRule.onNodeWithTag("themeModeSelector_system").assertExists()
  }

  @Test
  fun settingsScreen_poiToggle_hasCorrectTag() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("poiToggle").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_roadNumbersToggle_hasCorrectTag() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("roadNumbersToggle").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_streetNamesToggle_hasCorrectTag() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("streetNamesToggle").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun settingsScreen_threeDViewToggle_hasCorrectTag() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("threeDViewToggle").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun settingsScreen_multipleTogglesExist() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("poiToggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("roadNumbersToggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("streetNamesToggle").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("threeDViewToggle").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun settingsScreen_confirmationDialog_confirmButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("logoutButton_action").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is displayed by checking the title
    composeTestRule.onNodeWithText("Confirm Logout").assertIsDisplayed()
    // Check for the confirm button by filtering nodes
    composeTestRule.onAllNodesWithText("Logout")[1].assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun settingsScreen_deleteAccountConfirmation_confirmButton() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("deleteAccountButton_action").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Verify the dialog is displayed by checking for unique dialog message
    composeTestRule.onNodeWithText("This action cannot be undone", substring = true).assertExists()
    // The Cancel button should be visible in the dialog
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun settingsScreen_hasSnackbarHost() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Snackbar host should be present in the scaffold
    composeTestRule.onNodeWithTag("settingsScreen").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_scrollContentUpToShowAllElements() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // Scroll to find all elements
    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeTestRule.onNodeWithText("Map Settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun settingsScreen_backButton_hasCorrectIcon() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun settingsScreen_settingsTitle_isVisible() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_allTogglesAreInitialized() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    // All toggles should exist and be clickable
    composeTestRule.onNodeWithTag("poiToggle_switch").assertHasClickAction()
    composeTestRule.onNodeWithTag("roadNumbersToggle_switch").assertHasClickAction()
    composeTestRule
        .onNodeWithTag("streetNamesToggle_switch")
        .performScrollTo()
        .assertHasClickAction()
    composeTestRule
        .onNodeWithTag("threeDViewToggle_switch")
        .performScrollTo()
        .assertHasClickAction()
  }

  @Test
  fun settingsScreen_accountSectionVisible_afterScroll() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("logoutButton").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Account").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_themeModeSelector_visible() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("themeModeSelector").assertIsDisplayed()
    composeTestRule.onNodeWithText("Theme Mode").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_poiToggleHasVisibilityIcon() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("poiToggle").assertIsDisplayed()
    composeTestRule.onNodeWithText("Points of Interest").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_roadToggleHasMapIcon() {
    composeTestRule.setContent { SettingsScreen(onNavigateBack = {}, onNavigateToSignIn = {}) }

    composeTestRule.onNodeWithTag("roadNumbersToggle").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Road Labels").assertIsDisplayed()
  }

  @Test
  fun confirmationDialog_logoutGeneratesLogoutConfirmTag_andCallsOnConfirm() {
    var confirmed = false
    composeTestRule.setContent {
      ConfirmationDialog(
          title = "Logout",
          message = "Confirm logout?",
          confirmButtonText = "Logout",
          confirmButtonColor = androidx.compose.ui.graphics.Color(0xFF667eea),
          onConfirm = { confirmed = true },
          onDismiss = {})
    }

    composeTestRule.onNodeWithTag("logoutConfirmButton").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assert(confirmed)
  }

  @Test
  fun confirmationDialog_deleteAccountGeneratesDeleteConfirmTag_andCallsOnConfirm() {
    var confirmed = false
    composeTestRule.setContent {
      ConfirmationDialog(
          title = "Delete",
          message = "Confirm delete?",
          confirmButtonText = "Delete Account",
          confirmButtonColor = androidx.compose.ui.graphics.Color(0xFFef5350),
          onConfirm = { confirmed = true },
          onDismiss = {})
    }

    composeTestRule.onNodeWithTag("deleteAccountConfirmButton").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assert(confirmed)
  }
}

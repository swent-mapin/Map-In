// Assisted by AI
package com.swent.mapin.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ChangePasswordScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun changePasswordScreen_displaysCorrectly() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Verify screen is displayed
    composeTestRule.onNodeWithTag("changePasswordScreen").assertIsDisplayed()

    // Verify header
    composeTestRule.onNodeWithText("Change Password").assertIsDisplayed()
    composeTestRule.onNodeWithText("Update Your Password").assertIsDisplayed()

    // Verify all input fields exist
    composeTestRule.onNodeWithTag("currentPasswordField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("newPasswordField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirmPasswordField").assertIsDisplayed()
  }

  @Test
  fun changePasswordScreen_hasBackButton() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun changePasswordScreen_hasActionButtons() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    composeTestRule
        .onNodeWithTag("cancelButton")
        .performScrollTo()
        .assertIsDisplayed()
        .assertHasClickAction()
    composeTestRule
        .onNodeWithTag("saveButton")
        .performScrollTo()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun changePasswordScreen_passwordFieldsHaveVisibilityToggle() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Verify visibility toggle buttons exist for all password fields
    composeTestRule.onNodeWithTag("currentPasswordField_visibilityToggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("newPasswordField_visibilityToggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirmPasswordField_visibilityToggle").assertIsDisplayed()
  }

  @Test
  fun changePasswordScreen_displaysPasswordRequirements() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Scroll to requirements card and verify it's displayed
    composeTestRule.onNodeWithTag("passwordRequirementsCard").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Password Requirements").assertIsDisplayed()

    // Verify all requirements are listed
    composeTestRule.onNodeWithText("At least 8 characters long").assertExists()
    composeTestRule.onNodeWithText("Contains at least one uppercase letter").assertExists()
    composeTestRule.onNodeWithText("Contains at least one lowercase letter").assertExists()
    composeTestRule.onNodeWithText("Contains at least one number").assertExists()
    composeTestRule.onNodeWithText("Contains at least one special character").assertExists()
  }

  @Test
  fun changePasswordScreen_backButtonTriggersNavigation() {
    var backPressed = false

    composeTestRule.setContent {
      ChangePasswordScreen(onNavigateBack = { backPressed = true }, onPasswordChanged = {})
    }

    composeTestRule.onNodeWithTag("backButton").performClick()
    assert(backPressed)
  }

  @Test
  fun changePasswordScreen_cancelButtonTriggersNavigation() {
    var backPressed = false

    composeTestRule.setContent {
      ChangePasswordScreen(onNavigateBack = { backPressed = true }, onPasswordChanged = {})
    }

    composeTestRule.onNodeWithTag("cancelButton").performScrollTo().performClick()
    assert(backPressed)
  }

  @Test
  fun changePasswordScreen_saveButtonTriggersCallback() {
    var passwordChanged = false

    composeTestRule.setContent {
      ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = { passwordChanged = true })
    }

    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()
    assert(passwordChanged)
  }

  @Test
  fun changePasswordScreen_canTypeInPasswordFields() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Type in current password field
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("oldPassword123")

    // Type in new password field
    composeTestRule.onNodeWithTag("newPasswordField").performTextInput("newPassword123")

    // Type in confirm password field
    composeTestRule.onNodeWithTag("confirmPasswordField").performTextInput("newPassword123")
  }

  @Test
  fun changePasswordScreen_visibilityToggleWorks() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Type password
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("testPassword")

    // Toggle visibility
    composeTestRule.onNodeWithTag("currentPasswordField_visibilityToggle").performClick()

    // Toggle again
    composeTestRule.onNodeWithTag("currentPasswordField_visibilityToggle").performClick()
  }
}

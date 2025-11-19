// Assisted by AI
package com.swent.mapin.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChangePasswordScreenTest {

  // Use Android compose rule since this file resides in androidTest (instrumentation) source set
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
    assertTrue(backPressed)
  }

  @Test
  fun changePasswordScreen_cancelButtonTriggersNavigation() {
    var backPressed = false

    composeTestRule.setContent {
      ChangePasswordScreen(onNavigateBack = { backPressed = true }, onPasswordChanged = {})
    }

    composeTestRule.onNodeWithTag("cancelButton").performScrollTo().performClick()
    assertTrue(backPressed)
  }

  @Test
  fun changePasswordScreen_saveButtonTriggersCallback() {
    var passwordChanged = false

    composeTestRule.setContent {
      ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = { passwordChanged = true })
    }

    // Fill in valid data
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("OldPass123!")
    composeTestRule.onNodeWithTag("newPasswordField").performTextInput("NewPass456!")
    composeTestRule.onNodeWithTag("confirmPasswordField").performTextInput("NewPass456!")

    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()
    assertTrue(passwordChanged)
  }

  @Test
  fun changePasswordScreen_validationShowsErrorForEmptyFields() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Click save without filling any fields
    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()

    // Error message should be displayed
    composeTestRule.onNodeWithTag("errorMessage").assertExists()
    composeTestRule.onNodeWithText("Current password is required").assertExists()
  }

  @Test
  fun changePasswordScreen_validationShowsErrorForMismatchedPasswords() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Fill fields with mismatched passwords
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("OldPass123!")
    composeTestRule.onNodeWithTag("newPasswordField").performTextInput("NewPass456!")
    composeTestRule.onNodeWithTag("confirmPasswordField").performTextInput("DifferentPass789!")

    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()

    // Error message should be displayed
    composeTestRule.onNodeWithTag("errorMessage").assertExists()
    composeTestRule.onNodeWithText("New password and confirmation do not match").assertExists()
  }

  @Test
  fun changePasswordScreen_validationShowsErrorForWeakPassword() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Fill fields with weak password (too short)
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("OldPass123!")
    composeTestRule.onNodeWithTag("newPasswordField").performTextInput("weak")
    composeTestRule.onNodeWithTag("confirmPasswordField").performTextInput("weak")

    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()

    // Error message should be displayed
    composeTestRule.onNodeWithTag("errorMessage").assertExists()
    composeTestRule.onNodeWithText("Password must be at least 8 characters long").assertExists()
  }

  @Test
  fun changePasswordScreen_validationShowsErrorForPasswordMissingUppercase() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Fill fields with password missing uppercase
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("OldPass123!")
    composeTestRule.onNodeWithTag("newPasswordField").performTextInput("newpass123!")
    composeTestRule.onNodeWithTag("confirmPasswordField").performTextInput("newpass123!")

    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()

    // Error message should be displayed
    composeTestRule.onNodeWithTag("errorMessage").assertExists()
    composeTestRule
        .onNodeWithText("Password must contain at least one uppercase letter")
        .assertExists()
  }

  @Test
  fun changePasswordScreen_validationShowsErrorForSamePassword() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Fill fields with same current and new password
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("SamePass123!")
    composeTestRule.onNodeWithTag("newPasswordField").performTextInput("SamePass123!")
    composeTestRule.onNodeWithTag("confirmPasswordField").performTextInput("SamePass123!")

    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()

    // Error message should be displayed
    composeTestRule.onNodeWithTag("errorMessage").assertExists()
    composeTestRule
        .onNodeWithText("New password must be different from current password")
        .assertExists()
  }

  @Test
  fun changePasswordScreen_errorMessageClearsWhenUserTypes() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Trigger validation error
    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("errorMessage").assertExists()

    // Type in a field
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("O")

    // Error message should be cleared
    composeTestRule.onNodeWithTag("errorMessage").assertDoesNotExist()
  }

  @Test
  fun changePasswordScreen_validDataDoesNotCallbackOnValidationError() {
    var passwordChanged = false

    composeTestRule.setContent {
      ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = { passwordChanged = true })
    }

    // Fill with invalid data (empty fields)
    composeTestRule.onNodeWithTag("saveButton").performScrollTo().performClick()

    // Callback should NOT be triggered
    assertTrue(!passwordChanged)
    // Error should be shown
    composeTestRule.onNodeWithTag("errorMessage").assertExists()
  }

  private fun SemanticsNodeInteraction.assertTextContains(substring: String) {
    // For password fields, we verify text was entered by checking if the field contains the text
    // This works because Compose semantics exposes the actual text content even when visually
    // masked
    val node = this.fetchSemanticsNode()
    val editableText =
        node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.EditableText) {
          androidx.compose.ui.text.AnnotatedString("")
        }
    assertTrue(
        "Expected text to contain '$substring' but was '${editableText.text}'",
        editableText.text.contains(substring))
  }

  @Test
  fun changePasswordScreen_canTypeInPasswordFields() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Type in current password field and verify by toggling visibility
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("oldPassword123")
    composeTestRule.onNodeWithTag("currentPasswordField_visibilityToggle").performClick()
    composeTestRule.onNodeWithTag("currentPasswordField").assertTextContains("oldPassword123")

    // Type in new password field and verify by toggling visibility
    composeTestRule.onNodeWithTag("newPasswordField").performTextInput("newPassword123")
    composeTestRule.onNodeWithTag("newPasswordField_visibilityToggle").performClick()
    composeTestRule.onNodeWithTag("newPasswordField").assertTextContains("newPassword123")

    // Type in confirm password field and verify by toggling visibility
    composeTestRule.onNodeWithTag("confirmPasswordField").performTextInput("newPassword123")
    composeTestRule.onNodeWithTag("confirmPasswordField_visibilityToggle").performClick()
    composeTestRule.onNodeWithTag("confirmPasswordField").assertTextContains("newPassword123")
  }

  @Test
  fun changePasswordScreen_visibilityToggleWorks() {
    composeTestRule.setContent { ChangePasswordScreen(onNavigateBack = {}, onPasswordChanged = {}) }

    // Type password
    composeTestRule.onNodeWithTag("currentPasswordField").performTextInput("testPassword")

    // Initially password is hidden - verify "Show password" content description
    composeTestRule
        .onNodeWithTag("currentPasswordField_visibilityToggle")
        .assertContentDescriptionEquals("Show password")

    // Toggle visibility on (show plain text) and assert text is visible
    composeTestRule.onNodeWithTag("currentPasswordField_visibilityToggle").performClick()
    composeTestRule.onNodeWithTag("currentPasswordField").assertTextContains("testPassword")
    // Verify content description changed to "Hide password"
    composeTestRule
        .onNodeWithTag("currentPasswordField_visibilityToggle")
        .assertContentDescriptionEquals("Hide password")

    // Toggle visibility off (mask password) and verify content description reverts
    composeTestRule.onNodeWithTag("currentPasswordField_visibilityToggle").performClick()
    composeTestRule
        .onNodeWithTag("currentPasswordField_visibilityToggle")
        .assertContentDescriptionEquals("Show password")
    // When masked, we cannot reliably assert the actual text via semantics (it returns bullets)
    // But we can verify the field still exists and has content
    composeTestRule.onNodeWithTag("currentPasswordField").assertExists()

    // Re-toggle to show and verify text is still there
    composeTestRule.onNodeWithTag("currentPasswordField_visibilityToggle").performClick()
    composeTestRule
        .onNodeWithTag("currentPasswordField_visibilityToggle")
        .assertContentDescriptionEquals("Hide password")
    composeTestRule.onNodeWithTag("currentPasswordField").assertTextContains("testPassword")
  }
}

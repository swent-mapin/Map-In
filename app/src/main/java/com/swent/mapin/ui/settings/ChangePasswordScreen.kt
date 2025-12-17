// Assisted by AI
package com.swent.mapin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.model.changepassword.ChangePasswordRepositoryProvider
import com.swent.mapin.ui.components.StandardTopAppBar

/**
 * Custom semantic property to expose password visibility state. This allows tests and accessibility
 * services to detect whether the password is currently visible.
 */
val PasswordVisibleKey = SemanticsPropertyKey<Boolean>("PasswordVisible")

/**
 * Change Password screen allowing users to update their password.
 *
 * Features:
 * - Current password input
 * - New password input
 * - Confirm new password input
 * - Save and Cancel buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    onPasswordChanged: () -> Unit = {},
    viewModel: ChangePasswordViewModel =
        viewModel(
            factory =
                ChangePasswordViewModel.Factory(ChangePasswordRepositoryProvider.getRepository()))
) {
  val uiState by viewModel.state.collectAsState()
  var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
  var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
  var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
  val isDarkTheme = isSystemInDarkTheme()
  val focusManager = LocalFocusManager.current

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag("changePasswordScreen"),
      topBar = { StandardTopAppBar(title = "Change Password", onNavigateBack = onNavigateBack) }) {
          paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(
                        color =
                            if (isDarkTheme) {
                              // Dark theme: use darker background
                              MaterialTheme.colorScheme.background
                            } else {
                              // Light theme: use slightly gray background
                              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            })
                    .padding(paddingValues)) {
              Column(
                  modifier =
                      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    // Header Section
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag("passwordHeaderSection"),
                        contentAlignment = Alignment.Center) {
                          Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier =
                                    Modifier.size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush =
                                                Brush.linearGradient(
                                                    colors =
                                                        listOf(
                                                            Color(0xFF667eea), Color(0xFF764ba2)))),
                                contentAlignment = Alignment.Center) {
                                  Icon(
                                      imageVector = Icons.Default.Lock,
                                      contentDescription = "Change Password",
                                      tint = Color.White,
                                      modifier = Modifier.size(40.dp))
                                }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Update Your Password",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text =
                                    "Enter your current password and choose a new secure password",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 32.dp))
                          }
                        }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Current Password Field
                    PasswordInputField(
                        value = uiState.currentPassword,
                        onValueChange = viewModel::updateCurrentPassword,
                        label = "Current Password",
                        placeholder = "Enter your current password",
                        isPasswordVisible = currentPasswordVisible,
                        onVisibilityToggle = { currentPasswordVisible = !currentPasswordVisible },
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        errorMessage = uiState.validationErrors.currentPasswordError,
                        testTag = "currentPasswordField")

                    Spacer(modifier = Modifier.height(16.dp))

                    // New Password Field
                    PasswordInputField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::updateNewPassword,
                        label = "New Password",
                        placeholder = "Enter your new password",
                        isPasswordVisible = newPasswordVisible,
                        onVisibilityToggle = { newPasswordVisible = !newPasswordVisible },
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        errorMessage = uiState.validationErrors.newPasswordError,
                        testTag = "newPasswordField")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm New Password Field
                    PasswordInputField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        label = "Confirm New Password",
                        placeholder = "Re-enter your new password",
                        isPasswordVisible = confirmPasswordVisible,
                        onVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        imeAction = ImeAction.Done,
                        onImeAction = {
                          focusManager.clearFocus()
                          viewModel.changePassword { onPasswordChanged() }
                        },
                        errorMessage = uiState.validationErrors.confirmPasswordError,
                        testTag = "confirmPasswordField")

                    Spacer(modifier = Modifier.height(32.dp))

                    // Validation Error Message
                    uiState.errorMessage?.let { error ->
                      Box(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .clip(RoundedCornerShape(12.dp))
                                  .background(MaterialTheme.colorScheme.errorContainer)
                                  .padding(16.dp)
                                  .testTag("errorMessage")) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium)
                          }
                      Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                          OutlinedButton(
                              onClick = onNavigateBack,
                              enabled = !uiState.isLoading,
                              modifier = Modifier.weight(1f).height(48.dp).testTag("cancelButton"),
                              shape = RoundedCornerShape(12.dp),
                              colors =
                                  ButtonDefaults.outlinedButtonColors(
                                      contentColor = MaterialTheme.colorScheme.primary)) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                              }

                          Button(
                              onClick = { viewModel.changePassword { onPasswordChanged() } },
                              enabled = !uiState.isLoading,
                              modifier = Modifier.weight(1f).height(48.dp).testTag("saveButton"),
                              shape = RoundedCornerShape(12.dp),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = Color(0xFF667eea),
                                      contentColor = Color.White)) {
                                Text(
                                    text = if (uiState.isLoading) "Saving..." else "Save Changes",
                                    fontWeight = FontWeight.Bold)
                              }
                        }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Password Requirements Info
                    PasswordRequirementsCard(password = uiState.newPassword)
                  }
            }
      }
}

/** Reusable password input field with visibility toggle */
@Composable
private fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPasswordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    errorMessage: String? = null,
    testTag: String
) {
  Column {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        visualTransformation =
            if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        trailingIcon = {
          IconButton(
              onClick = onVisibilityToggle,
              modifier = Modifier.testTag("${testTag}_visibilityToggle")) {
                Icon(
                    imageVector =
                        if (isPasswordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                    contentDescription =
                        stringResource(
                            if (isPasswordVisible) com.swent.mapin.R.string.password_hide
                            else com.swent.mapin.R.string.password_show))
              }
        },
        isError = errorMessage != null,
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth().testTag(testTag).semantics {
              // Mark as password field for accessibility
              password()
              // Add custom semantic property for password visibility state
              // This helps tests and accessibility services detect the current state
              set(PasswordVisibleKey, isPasswordVisible)
            },
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF667eea),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = Color(0xFF667eea),
                cursorColor = Color(0xFF667eea)))

    // Display error message if present
    errorMessage?.let { error ->
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = error,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 16.dp).testTag("${testTag}_error"))
    }
  }
}

/** Card showing password requirements with real-time validation */
@Composable
private fun PasswordRequirementsCard(password: String) {
  val isDarkTheme = isSystemInDarkTheme()

  // Calculate which requirements are met
  val hasMinLength = password.length >= 8
  val hasUppercase = password.any { it.isUpperCase() }
  val hasLowercase = password.any { it.isLowerCase() }
  val hasDigit = password.any { it.isDigit() }
  val hasSpecialChar = password.any { !it.isLetterOrDigit() }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(
                  color =
                      if (isDarkTheme) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                      } else {
                        MaterialTheme.colorScheme.surface
                      })
              .padding(16.dp)
              .testTag("passwordRequirementsCard")) {
        Text(
            text = "Password Requirements",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        PasswordRequirementItem(
            "At least 8 characters long",
            isValid = hasMinLength,
            showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            "Contains at least one uppercase letter",
            isValid = hasUppercase,
            showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            "Contains at least one lowercase letter",
            isValid = hasLowercase,
            showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            "Contains at least one number", isValid = hasDigit, showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            "Contains at least one special character",
            isValid = hasSpecialChar,
            showStatus = password.isNotEmpty())
      }
}

/** Individual password requirement item */
@Composable
private fun PasswordRequirementItem(
    requirement: String,
    isValid: Boolean = false,
    showStatus: Boolean = false
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically) {
        if (showStatus) {
          // Show checkmark or cross based on validation
          Icon(
              imageVector = if (isValid) Icons.Default.Check else Icons.Default.Close,
              contentDescription = if (isValid) "Requirement met" else "Requirement not met",
              tint = if (isValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp))
        } else {
          // Show neutral dot when no input yet
          Box(
              modifier =
                  Modifier.size(8.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary))
        }
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Text(
            text = requirement,
            style = MaterialTheme.typography.bodyMedium,
            color =
                when {
                  showStatus && isValid -> Color(0xFF4CAF50)
                  showStatus && !isValid -> MaterialTheme.colorScheme.error
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                })
      }
}

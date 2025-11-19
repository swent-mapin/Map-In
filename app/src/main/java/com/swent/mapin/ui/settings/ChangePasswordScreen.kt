// Assisted by AI
package com.swent.mapin.ui.settings

import android.os.Parcelable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parcelize

/**
 * Custom semantic property to expose password visibility state. This allows tests and accessibility
 * services to detect whether the password is currently visible.
 */
val PasswordVisibleKey = SemanticsPropertyKey<Boolean>("PasswordVisible")
var SemanticsPropertyReceiver.passwordVisible by PasswordVisibleKey

/**
 * Data class to hold password change form state. Survives configuration changes and process death
 * via Parcelable.
 */
@Parcelize
data class PasswordChangeState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val currentPasswordVisible: Boolean = false,
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val errorMessage: String? = null
) : Parcelable

/**
 * Validates password strength according to requirements:
 * - At least 8 characters
 * - Contains uppercase letter
 * - Contains lowercase letter
 * - Contains number
 * - Contains special character
 */
private fun validatePasswordStrength(password: String): String? {
  if (password.length < 8) return "Password must be at least 8 characters long"
  if (!password.any { it.isUpperCase() })
      return "Password must contain at least one uppercase letter"
  if (!password.any { it.isLowerCase() })
      return "Password must contain at least one lowercase letter"
  if (!password.any { it.isDigit() }) return "Password must contain at least one number"
  if (!password.any { !it.isLetterOrDigit() })
      return "Password must contain at least one special character"
  return null
}

/**
 * Validates the complete password change form. Returns error message if validation fails, null if
 * valid.
 */
private fun validatePasswordForm(state: PasswordChangeState): String? {
  if (state.currentPassword.isEmpty()) return "Current password is required"
  if (state.newPassword.isEmpty()) return "New password is required"
  if (state.confirmNewPassword.isEmpty()) return "Please confirm your new password"

  validatePasswordStrength(state.newPassword)?.let {
    return it
  }

  if (state.newPassword != state.confirmNewPassword) {
    return "New password and confirmation do not match"
  }

  if (state.currentPassword == state.newPassword) {
    return "New password must be different from current password"
  }

  return null
}

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
fun ChangePasswordScreen(onNavigateBack: () -> Unit, onPasswordChanged: () -> Unit = {}) {
  var state by rememberSaveable { mutableStateOf(PasswordChangeState()) }
  val isDarkTheme = isSystemInDarkTheme()
  val focusManager = LocalFocusManager.current

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag("changePasswordScreen"),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  "Change Password",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface)
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface)
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface))
      }) { paddingValues ->
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
                        value = state.currentPassword,
                        onValueChange = {
                          state = state.copy(currentPassword = it, errorMessage = null)
                        },
                        label = "Current Password",
                        placeholder = "Enter your current password",
                        isPasswordVisible = state.currentPasswordVisible,
                        onVisibilityToggle = {
                          state = state.copy(currentPasswordVisible = !state.currentPasswordVisible)
                        },
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        testTag = "currentPasswordField")

                    Spacer(modifier = Modifier.height(16.dp))

                    // New Password Field
                    PasswordInputField(
                        value = state.newPassword,
                        onValueChange = {
                          state = state.copy(newPassword = it, errorMessage = null)
                        },
                        label = "New Password",
                        placeholder = "Enter your new password",
                        isPasswordVisible = state.newPasswordVisible,
                        onVisibilityToggle = {
                          state = state.copy(newPasswordVisible = !state.newPasswordVisible)
                        },
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        testTag = "newPasswordField")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm New Password Field
                    PasswordInputField(
                        value = state.confirmNewPassword,
                        onValueChange = {
                          state = state.copy(confirmNewPassword = it, errorMessage = null)
                        },
                        label = "Confirm New Password",
                        placeholder = "Re-enter your new password",
                        isPasswordVisible = state.confirmPasswordVisible,
                        onVisibilityToggle = {
                          state = state.copy(confirmPasswordVisible = !state.confirmPasswordVisible)
                        },
                        imeAction = ImeAction.Done,
                        onImeAction = {
                          focusManager.clearFocus()
                          // Trigger validation on Done
                          val validationError = validatePasswordForm(state)
                          if (validationError != null) {
                            state = state.copy(errorMessage = validationError)
                          } else {
                            state = state.copy(errorMessage = null)
                            onPasswordChanged()
                          }
                        },
                        testTag = "confirmPasswordField")

                    Spacer(modifier = Modifier.height(32.dp))

                    // Validation Error Message
                    state.errorMessage?.let { error ->
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
                              modifier = Modifier.weight(1f).height(48.dp).testTag("cancelButton"),
                              shape = RoundedCornerShape(12.dp),
                              colors =
                                  ButtonDefaults.outlinedButtonColors(
                                      contentColor = MaterialTheme.colorScheme.primary)) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                              }

                          Button(
                              onClick = {
                                // Validate form before proceeding
                                val validationError = validatePasswordForm(state)
                                if (validationError != null) {
                                  // Show validation error
                                  state = state.copy(errorMessage = validationError)
                                } else {
                                  // Clear any previous errors and proceed
                                  state = state.copy(errorMessage = null)
                                  onPasswordChanged()
                                }
                              },
                              modifier = Modifier.weight(1f).height(48.dp).testTag("saveButton"),
                              shape = RoundedCornerShape(12.dp),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = Color(0xFF667eea),
                                      contentColor = Color.White)) {
                                Text("Save Changes", fontWeight = FontWeight.Bold)
                              }
                        }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Password Requirements Info
                    PasswordRequirementsCard()
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
    testTag: String
) {
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
}

/** Card showing password requirements */
@Composable
private fun PasswordRequirementsCard() {
  val isDarkTheme = isSystemInDarkTheme()
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
        PasswordRequirementItem("At least 8 characters long")
        PasswordRequirementItem("Contains at least one uppercase letter")
        PasswordRequirementItem("Contains at least one lowercase letter")
        PasswordRequirementItem("Contains at least one number")
        PasswordRequirementItem("Contains at least one special character")
      }
}

/** Individual password requirement item */
@Composable
private fun PasswordRequirementItem(requirement: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Increased marker size from 6.dp to 8.dp for better visibility
        Box(
            modifier =
                Modifier.size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .semantics(mergeDescendants = true) {
                      // Add semantic label for screen readers
                      // This helps accessibility services understand this is a requirement item
                    })
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Text(
            text = requirement,
            style =
                MaterialTheme.typography
                    .bodyMedium, // Changed from bodySmall for better readability
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}

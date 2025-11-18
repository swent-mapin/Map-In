// Assisted by AI
package com.swent.mapin.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

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
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmNewPassword by remember { mutableStateOf("") }
  var currentPasswordVisible by remember { mutableStateOf(false) }
  var newPasswordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }

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
                            if (MaterialTheme.colorScheme.background ==
                                MaterialTheme.colorScheme.surface) {
                              // Light theme: use a slightly gray background
                              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            } else {
                              // Dark theme: use a darker background
                              MaterialTheme.colorScheme.background
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
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Current Password",
                        placeholder = "Enter your current password",
                        isPasswordVisible = currentPasswordVisible,
                        onVisibilityToggle = { currentPasswordVisible = !currentPasswordVisible },
                        testTag = "currentPasswordField")

                    Spacer(modifier = Modifier.height(16.dp))

                    // New Password Field
                    PasswordInputField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "New Password",
                        placeholder = "Enter your new password",
                        isPasswordVisible = newPasswordVisible,
                        onVisibilityToggle = { newPasswordVisible = !newPasswordVisible },
                        testTag = "newPasswordField")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm New Password Field
                    PasswordInputField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = "Confirm New Password",
                        placeholder = "Re-enter your new password",
                        isPasswordVisible = confirmPasswordVisible,
                        onVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        testTag = "confirmPasswordField")

                    Spacer(modifier = Modifier.height(32.dp))

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
                                // TODO: Implement password change logic
                                onPasswordChanged()
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
    testTag: String
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      visualTransformation =
          if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
      trailingIcon = {
        IconButton(
            onClick = onVisibilityToggle,
            modifier = Modifier.testTag("${testTag}_visibilityToggle")) {
              Icon(
                  imageVector =
                      if (isPasswordVisible) Icons.Default.Visibility
                      else Icons.Default.VisibilityOff,
                  contentDescription = if (isPasswordVisible) "Hide password" else "Show password")
            }
      },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag(testTag),
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
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(
                  color =
                      if (MaterialTheme.colorScheme.background ==
                          MaterialTheme.colorScheme.surface) {
                        MaterialTheme.colorScheme.surface
                      } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
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
        Box(
            modifier =
                Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Text(
            text = requirement,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}

package com.swent.mapin.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swent.mapin.R
import com.swent.mapin.ui.theme.MapInTheme
import com.swent.mapin.util.PasswordValidation

// Assisted by AI

/**
 * Card showing password requirements with real-time validation feedback.
 *
 * @param password The current password input
 * @param passwordValidation Pre-computed validation results for password requirements
 */
@Composable
fun PasswordRequirementsCard(password: String, passwordValidation: PasswordValidation) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(16.dp)
              .testTag("passwordRequirementsCard")) {
        Text(
            text = stringResource(R.string.password_requirements_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        PasswordRequirementItem(
            requirement = stringResource(R.string.password_requirement_min_length),
            isValid = passwordValidation.hasMinLength,
            showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            requirement = stringResource(R.string.password_requirement_uppercase),
            isValid = passwordValidation.hasUppercase,
            showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            requirement = stringResource(R.string.password_requirement_lowercase),
            isValid = passwordValidation.hasLowercase,
            showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            requirement = stringResource(R.string.password_requirement_digit),
            isValid = passwordValidation.hasDigit,
            showStatus = password.isNotEmpty())
        PasswordRequirementItem(
            requirement = stringResource(R.string.password_requirement_special_char),
            isValid = passwordValidation.hasSpecialChar,
            showStatus = password.isNotEmpty())
      }
}

/**
 * Individual password requirement item showing a visual indicator and requirement text.
 *
 * @param requirement The requirement text to display
 * @param isValid Whether this requirement is currently satisfied
 * @param showStatus Whether to show validation status (checkmark/cross) or neutral state
 */
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
              contentDescription =
                  stringResource(
                      if (isValid) R.string.requirement_met else R.string.requirement_not_met),
              tint =
                  if (isValid) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp))
        } else {
          // Show neutral dot when no input yet
          Box(
              modifier =
                  Modifier.size(8.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = requirement,
            style = MaterialTheme.typography.bodyMedium,
            color =
                when {
                  showStatus && isValid -> MaterialTheme.colorScheme.primary
                  showStatus && !isValid -> MaterialTheme.colorScheme.error
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                })
      }
}

@Preview(showBackground = true, name = "Empty Password")
@Composable
private fun PasswordRequirementsCardPreviewEmpty() {
  MapInTheme {
    Surface {
      PasswordRequirementsCard(
          password = "",
          passwordValidation =
              PasswordValidation(
                  hasMinLength = false,
                  hasUppercase = false,
                  hasLowercase = false,
                  hasDigit = false,
                  hasSpecialChar = false))
    }
  }
}

@Preview(showBackground = true, name = "Partial Validation")
@Composable
private fun PasswordRequirementsCardPreviewPartial() {
  MapInTheme {
    Surface {
      PasswordRequirementsCard(
          password = "Test123",
          passwordValidation =
              PasswordValidation(
                  hasMinLength = false,
                  hasUppercase = true,
                  hasLowercase = true,
                  hasDigit = true,
                  hasSpecialChar = false))
    }
  }
}

@Preview(showBackground = true, name = "All Requirements Met")
@Composable
private fun PasswordRequirementsCardPreviewValid() {
  MapInTheme {
    Surface {
      PasswordRequirementsCard(
          password = "ValidPass123!",
          passwordValidation =
              PasswordValidation(
                  hasMinLength = true,
                  hasUppercase = true,
                  hasLowercase = true,
                  hasDigit = true,
                  hasSpecialChar = true))
    }
  }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun PasswordRequirementsCardPreviewDark() {
  MapInTheme(darkTheme = true) {
    Surface {
      PasswordRequirementsCard(
          password = "Test123",
          passwordValidation =
              PasswordValidation(
                  hasMinLength = false,
                  hasUppercase = true,
                  hasLowercase = true,
                  hasDigit = true,
                  hasSpecialChar = false))
    }
  }
}

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
import com.swent.mapin.ui.auth.PasswordRequirementStatus.INVALID
import com.swent.mapin.ui.auth.PasswordRequirementStatus.NEUTRAL
import com.swent.mapin.ui.auth.PasswordRequirementStatus.VALID
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
  val status = resolveRequirementStatus(showStatus, isValid)
  val textColor =
      when (status) {
        VALID -> MaterialTheme.colorScheme.primary
        INVALID -> MaterialTheme.colorScheme.error
        NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
      }

  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically) {
        RequirementStatusIcon(status)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = requirement, style = MaterialTheme.typography.bodyMedium, color = textColor)
      }
}

@Composable
private fun RequirementStatusIcon(status: PasswordRequirementStatus) {
  when (status) {
    NEUTRAL -> {
      Box(
          modifier =
              Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.onSurfaceVariant))
    }
    VALID -> {
      Icon(
          imageVector = Icons.Default.Check,
          contentDescription = stringResource(R.string.requirement_met),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp))
    }
    INVALID -> {
      Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(R.string.requirement_not_met),
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(16.dp))
    }
  }
}

private fun resolveRequirementStatus(
    showStatus: Boolean,
    isValid: Boolean
): PasswordRequirementStatus {
  if (!showStatus) return NEUTRAL
  return if (isValid) VALID else INVALID
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

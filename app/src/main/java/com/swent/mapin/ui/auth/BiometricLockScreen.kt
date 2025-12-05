package com.swent.mapin.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swent.mapin.R

/**
 * Full-screen composable shown while waiting for biometric authentication.
 *
 * This screen blocks access to the app until the user successfully authenticates using biometric
 * credentials (fingerprint, face, iris) or device credentials (PIN/pattern/password).
 *
 * @param isAuthenticating Whether biometric authentication is currently in progress
 * @param errorMessage Optional error message to display if authentication failed
 * @param onRetry Callback invoked when user taps the retry button
 * @param onUseAnotherAccount Callback invoked when user wants to logout and use another account
 */
@Composable
fun BiometricLockScreen(
    isAuthenticating: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onUseAnotherAccount: () -> Unit = {}
) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(
                  brush =
                      Brush.verticalGradient(
                          colors =
                              listOf(
                                  MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                  MaterialTheme.colorScheme.background)))
              .testTag("biometricLockScreen")) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              // App Logo
              Image(
                  painter = painterResource(id = R.drawable.logo),
                  contentDescription = "Map'In Logo",
                  modifier = Modifier.size(120.dp).testTag("biometricLockScreen_logo"))

              Spacer(modifier = Modifier.height(48.dp))

              // Unlock Map'In Title
              Text(
                  text = "Unlock Map'In",
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.testTag("biometricLockScreen_title"))

              Spacer(modifier = Modifier.height(48.dp))

              // Clickable Fingerprint Icon
              FingerprintButton(isAuthenticating = isAuthenticating, onRetry = onRetry)

              Spacer(modifier = Modifier.height(24.dp))

              // Status Text
              StatusText(isAuthenticating = isAuthenticating, hasError = errorMessage != null)

              // Error Message
              ErrorMessage(errorMessage = errorMessage)

              Spacer(modifier = Modifier.height(48.dp))

              // Use Another Account Button
              UseAnotherAccountButton(
                  isAuthenticating = isAuthenticating, onUseAnotherAccount = onUseAnotherAccount)
            }
      }
}

/** Clickable fingerprint button with loading state. */
@Composable
private fun FingerprintButton(isAuthenticating: Boolean, onRetry: () -> Unit) {
  Box(
      modifier =
          Modifier.size(120.dp)
              .clip(CircleShape)
              .clickable(enabled = !isAuthenticating) { onRetry() }
              .background(
                  brush =
                      Brush.linearGradient(colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))))
              .testTag("biometricLockScreen_fingerprintButton"),
      contentAlignment = Alignment.Center) {
        if (isAuthenticating) {
          CircularProgressIndicator(
              modifier = Modifier.size(60.dp), color = Color.White, strokeWidth = 4.dp)
        } else {
          Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = "Tap to unlock with fingerprint",
              tint = Color.White,
              modifier = Modifier.size(70.dp).testTag("biometricLockScreen_fingerprintIcon"))
        }
      }
}

/** Determines the status text based on authentication state. */
private fun getStatusText(isAuthenticating: Boolean, hasError: Boolean): String {
  return when {
    isAuthenticating -> "Verifying..."
    hasError -> "Tap to try again"
    else -> "Tap to unlock"
  }
}

/** Status text showing current authentication state. */
@Composable
private fun StatusText(isAuthenticating: Boolean, hasError: Boolean) {
  Text(
      text = getStatusText(isAuthenticating, hasError),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("biometricLockScreen_statusText"))
}

/** Error message displayed when authentication fails. */
@Composable
private fun ErrorMessage(errorMessage: String?) {
  if (errorMessage != null) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp).testTag("biometricLockScreen_errorMessage"))
  }
}

/** Button to switch to another account. */
@Composable
private fun UseAnotherAccountButton(isAuthenticating: Boolean, onUseAnotherAccount: () -> Unit) {
  if (!isAuthenticating) {
    OutlinedButton(
        onClick = onUseAnotherAccount,
        modifier =
            Modifier.padding(horizontal = 24.dp)
                .height(56.dp)
                .testTag("biometricLockScreen_useAnotherAccountButton"),
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
          Text(
              text = "Use another account",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold)
        }
  }
}

package com.swent.mapin.ui.auth

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.R
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.util.PasswordValidationUtils.validatePassword

/**
 * Main sign-in screen composable that handles user authentication.
 *
 * This screen provides multiple authentication methods:
 * - Email and password sign-in
 * - Email and password registration
 * - Google OAuth sign-in
 * - Microsoft OAuth sign-in
 *
 * The composable manages authentication state through a [SignInViewModel] and handles side effects
 * such as navigation on successful sign-in and error clearing.
 *
 * @param viewModel The [SignInViewModel] that manages authentication state and business logic.
 *   Defaults to a factory-created instance using the current context.
 * @param onSignInSuccess Callback invoked when authentication is successful. Use this for
 *   navigation to the main app screen.
 * @see SignInViewModel
 * @see SignInUiState
 */
@Composable
fun SignInScreen(
    viewModel: SignInViewModel = viewModel(factory = SignInViewModel.factory(LocalContext.current)),
    onSignInSuccess: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val credentialManager = remember { CredentialManager.create(context) }

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isRegistering by remember { mutableStateOf(false) }

  // Side Effect: Success Handling
  LaunchedEffect(uiState.isSignInSuccessful) {
    if (uiState.isSignInSuccessful) {
      onSignInSuccess()
    }
  }

  // Side Effect: Clear Error
  LaunchedEffect(email, password) { if (uiState.error != null) viewModel.clearError() }

  SignInContent(
      uiState = uiState,
      email = email,
      password = password,
      isRegistering = isRegistering,
      onEmailChange = { email = it },
      onPasswordChange = { password = it },
      onRegisterChange = { isRegistering = it },
      onClearError = { viewModel.clearError() },
      onSubmit = {
        if (isRegistering) viewModel.signUpWithEmail(email, password)
        else viewModel.signInWithEmail(email, password)
      },
      onGoogleSignIn = { viewModel.signInWithGoogle(credentialManager) {} },
      onMicrosoftSignIn = {
        val activity = context as? Activity
        if (activity != null) {
          viewModel.signInWithMicrosoft(activity)
        } else {
          Log.e("SignInScreen", "Failed to cast context to Activity for Microsoft sign-in")
        }
      })
}

/**
 * Content layout for the sign-in screen.
 *
 * This composable orchestrates the visual hierarchy and layout of all sign-in components, including
 * the logo header, input fields, action buttons, and social sign-in options. It is designed to be
 * scrollable to accommodate smaller screens and different keyboard states.
 *
 * @param uiState Current UI state containing loading status, errors, and authentication status.
 * @param email Current email input value.
 * @param password Current password input value.
 * @param isRegistering Whether the user is in registration mode (true) or sign-in mode (false).
 * @param onEmailChange Callback invoked when the email input changes.
 * @param onPasswordChange Callback invoked when the password input changes.
 * @param onRegisterChange Callback invoked when toggling between sign-in and registration modes.
 * @param onClearError Callback to clear the current error state.
 * @param onSubmit Callback invoked when the user submits the form (sign in or register).
 * @param onGoogleSignIn Callback invoked when the user initiates Google sign-in.
 * @param onMicrosoftSignIn Callback invoked when the user initiates Microsoft sign-in.
 */
@Composable
private fun SignInContent(
    uiState: SignInUiState,
    email: String,
    password: String,
    isRegistering: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegisterChange: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onMicrosoftSignIn: () -> Unit
) {
  Surface(
      modifier = Modifier.fillMaxSize().testTag(UiTestTags.AUTH_SCREEN),
      color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(vertical = 54.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top) {
              Spacer(modifier = Modifier.height(40.dp))
              SignInLogoHeader()
              Spacer(modifier = Modifier.height(40.dp))

              SignInInputSection(
                  email = email,
                  password = password,
                  isRegistering = isRegistering,
                  isLoading = uiState.isLoading,
                  error = uiState.error,
                  onEmailChange = onEmailChange,
                  onPasswordChange = onPasswordChange,
                  onClearError = onClearError)

              Spacer(modifier = Modifier.height(16.dp))

              SignInActionSection(
                  isRegistering = isRegistering,
                  isLoading = uiState.isLoading,
                  isValidInput = email.isNotBlank() && password.isNotBlank(),
                  onSubmit = onSubmit,
                  onRegisterChange = onRegisterChange)

              Spacer(modifier = Modifier.height(24.dp))

              SocialSignInSection(
                  isLoading = uiState.isLoading,
                  onGoogleSignIn = onGoogleSignIn,
                  onMicrosoftSignIn = onMicrosoftSignIn)
            }
      }
}

/**
 * Displays the application logo and tagline header.
 *
 * Shows a rounded logo image with the app's tagline "One Map. Every moment." positioned with a
 * subtle blur effect for visual emphasis.
 */
@Composable
private fun SignInLogoHeader() {
  Image(
      painter = painterResource(id = R.drawable.logo),
      contentDescription = "App Logo",
      modifier = Modifier.size(200.dp).clip(RoundedCornerShape(24.dp)))
  Box(
      modifier = Modifier.fillMaxWidth().padding(start = 110.dp, top = 4.dp, bottom = 40.dp),
      contentAlignment = Alignment.CenterStart) {
        Text(
            text = "One Map. Every moment.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.blur(0.7.dp))
      }
}

/**
 * Input section containing email and password fields with validation feedback.
 *
 * Provides text fields for email and password entry with the following features:
 * - Email field with appropriate keyboard type
 * - Password field with visibility toggle
 * - Password requirements card (shown during registration)
 * - Error message display when authentication fails
 *
 * Fields are automatically disabled during loading states.
 *
 * @param email Current email input value.
 * @param password Current password input value.
 * @param isRegistering Whether the user is in registration mode. When true, shows password
 *   requirements validation.
 * @param isLoading Whether an authentication operation is in progress. Disables input when true.
 * @param error Current authentication error, if any.
 * @param onEmailChange Callback invoked when the email input changes.
 * @param onPasswordChange Callback invoked when the password input changes.
 * @param onClearError Callback to dismiss the error message.
 */
@Composable
private fun SignInInputSection(
    email: String,
    password: String,
    isRegistering: Boolean,
    isLoading: Boolean,
    error: AuthError?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onClearError: () -> Unit
) {
  var passwordVisible by remember { mutableStateOf(false) }
  val context = LocalContext.current

  OutlinedTextField(
      value = email,
      onValueChange = onEmailChange,
      label = { Text("Email") },
      modifier = Modifier.fillMaxWidth(),
      enabled = !isLoading,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      singleLine = true)

  Spacer(modifier = Modifier.height(12.dp))

  OutlinedTextField(
      value = password,
      onValueChange = onPasswordChange,
      label = { Text("Password") },
      modifier = Modifier.fillMaxWidth().testTag("passwordField"),
      enabled = !isLoading,
      visualTransformation =
          if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      trailingIcon = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
          Icon(
              imageVector =
                  if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
              contentDescription = if (passwordVisible) "Hide password" else "Show password")
        }
      },
      singleLine = true)

  if (isRegistering) {
    Spacer(modifier = Modifier.height(16.dp))
    val passwordValidation by remember(password) { derivedStateOf { validatePassword(password) } }
    PasswordRequirementsCard(password = password, passwordValidation = passwordValidation)
  }

  error?.let { err ->
    val errorMessage = err.getMessage(context)
    if (errorMessage.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      SignInErrorCard(errorMessage = errorMessage, onDismiss = onClearError)
    }
  }
}

/**
 * Action section containing the submit button and sign-in/register toggle.
 *
 * Displays:
 * - A primary action button that changes label based on mode ("Sign In" or "Register")
 * - A loading indicator when authentication is in progress
 * - A toggle switch to alternate between sign-in and registration modes
 *
 * The submit button is disabled when loading or when input is invalid (empty email/password).
 *
 * @param isRegistering Whether the user is in registration mode.
 * @param isLoading Whether an authentication operation is in progress.
 * @param isValidInput Whether the current email and password inputs are valid (non-blank).
 * @param onSubmit Callback invoked when the submit button is pressed.
 * @param onRegisterChange Callback invoked when toggling between sign-in and registration modes.
 */
@Composable
private fun SignInActionSection(
    isRegistering: Boolean,
    isLoading: Boolean,
    isValidInput: Boolean,
    onSubmit: () -> Unit,
    onRegisterChange: (Boolean) -> Unit
) {
  Button(
      onClick = onSubmit,
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("emailPasswordButton"),
      enabled = !isLoading && isValidInput) {
        if (isLoading) {
          CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text(if (isRegistering) "Register" else "Sign In")
        }
      }

  Spacer(modifier = Modifier.height(16.dp))

  Row(
      modifier = Modifier.fillMaxWidth().testTag("toggleSwitchRow"),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Sign In",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("signInLabel"),
            color =
                if (!isRegistering) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isRegistering,
            onCheckedChange = onRegisterChange,
            modifier = Modifier.testTag("registerSwitch"),
            enabled = !isLoading)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Register",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("registerLabel"),
            color =
                if (isRegistering) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant)
      }
}

/**
 * Social sign-in section with OAuth provider buttons.
 *
 * Displays alternative authentication methods separated by a divider:
 * - Google OAuth sign-in
 * - Microsoft OAuth sign-in
 *
 * Both buttons are disabled during loading states.
 *
 * @param isLoading Whether an authentication operation is in progress.
 * @param onGoogleSignIn Callback invoked when the Google sign-in button is pressed.
 * @param onMicrosoftSignIn Callback invoked when the Microsoft sign-in button is pressed.
 */
@Composable
private fun SocialSignInSection(
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onMicrosoftSignIn: () -> Unit
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    HorizontalDivider(modifier = Modifier.weight(1f))
    Text(
        text = "  OR  ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider(modifier = Modifier.weight(1f))
  }

  Spacer(modifier = Modifier.height(24.dp))

  SocialLoginButton(
      text = "Continue with Google",
      iconId = R.drawable.google_sign_in,
      description = "Google logo",
      isLoading = isLoading,
      onClick = onGoogleSignIn)

  Spacer(modifier = Modifier.height(24.dp))

  SocialLoginButton(
      text = "Continue with Microsoft",
      iconId = R.drawable.microsoft_sign_in,
      description = "Microsoft logo",
      isLoading = isLoading,
      onClick = onMicrosoftSignIn)
}

/**
 * Reusable button component for social OAuth providers.
 *
 * Displays an outlined button with a provider icon and text label. Shows a loading indicator when
 * an authentication operation is in progress.
 *
 * @param text Button label text (e.g., "Continue with Google").
 * @param iconId Drawable resource ID for the provider's icon.
 * @param description Content description for the icon (for accessibility).
 * @param isLoading Whether to show a loading indicator instead of the button content.
 * @param onClick Callback invoked when the button is pressed.
 */
@Composable
private fun SocialLoginButton(
    text: String,
    iconId: Int,
    description: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
  OutlinedButton(
      onClick = onClick, modifier = Modifier.fillMaxWidth().height(65.dp), enabled = !isLoading) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        } else {
          Row(
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = iconId),
                    contentDescription = description,
                    modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = text, style = MaterialTheme.typography.titleMedium)
              }
        }
      }
}

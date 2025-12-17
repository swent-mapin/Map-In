package com.swent.mapin.ui.auth

import android.app.Activity
import android.widget.Toast
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
 * Sign-in screen that provides authentication options for users.
 *
 * Displays the app logo, slogan, and authentication buttons for Google and Microsoft sign-in.
 * Handles authentication state changes and displays appropriate feedback to the user.
 *
 * @param viewModel The [SignInViewModel] that manages authentication logic and UI state.
 * @param onSignInSuccess Callback invoked when sign-in is successful.
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
  var passwordVisible by remember { mutableStateOf(false) }
  var isRegistering by remember { mutableStateOf(false) }

  SignInEffects(
      uiState = uiState,
      context = context,
      email = email,
      password = password,
      onSignInSuccess = onSignInSuccess,
      onClearError = { viewModel.clearError() })

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
              SignInHeader()

              SignInFormFields(
                  email = email,
                  onEmailChange = { email = it },
                  password = password,
                  onPasswordChange = { password = it },
                  passwordVisible = passwordVisible,
                  onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                  isRegistering = isRegistering,
                  isLoading = uiState.isLoading)

              SignInActions(
                  viewModel = viewModel,
                  credentialManager = credentialManager,
                  context = context,
                  uiState = uiState,
                  email = email,
                  password = password,
                  isRegistering = isRegistering,
                  onRegisterToggle = { isRegistering = it })
            }
      }
}

@Composable
private fun SignInEffects(
    uiState: SignInUiState,
    context: android.content.Context,
    email: String,
    password: String,
    onSignInSuccess: () -> Unit,
    onClearError: () -> Unit
) {
  // Observe authentication state and trigger success callback
  LaunchedEffect(uiState.isSignInSuccessful) {
    if (uiState.isSignInSuccessful) {
      Toast.makeText(
              context,
              "✅ Sign-in successful! Welcome ${uiState.currentUser?.displayName ?: uiState.currentUser?.email ?: ""}",
              Toast.LENGTH_SHORT)
          .show()
      onSignInSuccess()
    }
  }

  // Clear error state when user starts editing input fields
  LaunchedEffect(email, password) {
    if (uiState.error != null) {
      onClearError()
    }
  }
}

@Composable
private fun SignInHeader() {
  Spacer(modifier = Modifier.height(40.dp))

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

  Spacer(modifier = Modifier.height(40.dp))
}

@Composable
private fun SignInFormFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isRegistering: Boolean,
    isLoading: Boolean
) {
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
        IconButton(onClick = onPasswordVisibilityToggle) {
          Icon(
              imageVector =
                  if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
              contentDescription = if (passwordVisible) "Hide password" else "Show password")
        }
      },
      singleLine = true)

  // Show password requirements when in register mode
  if (isRegistering) {
    Spacer(modifier = Modifier.height(16.dp))
    val passwordValidation by remember(password) { derivedStateOf { validatePassword(password) } }
    PasswordRequirementsCard(password = password, passwordValidation = passwordValidation)
  }
}

@Composable
private fun SignInActions(
    viewModel: SignInViewModel,
    credentialManager: CredentialManager,
    context: android.content.Context,
    uiState: SignInUiState,
    email: String,
    password: String,
    isRegistering: Boolean,
    onRegisterToggle: (Boolean) -> Unit
) {
  // Show error message below input fields
  uiState.error?.let { error ->
    val errorMessage = error.getMessage(context)
    if (errorMessage.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      SignInErrorCard(errorMessage = errorMessage, onDismiss = { viewModel.clearError() })
    }
  }

  Spacer(modifier = Modifier.height(16.dp))

  EmailPasswordButton(
      viewModel = viewModel,
      email = email,
      password = password,
      isRegistering = isRegistering,
      isLoading = uiState.isLoading)

  Spacer(modifier = Modifier.height(16.dp))

  SignInRegisterToggle(
      isRegistering = isRegistering,
      onRegisterToggle = onRegisterToggle,
      isLoading = uiState.isLoading)

  Spacer(modifier = Modifier.height(24.dp))

  OrDivider()

  Spacer(modifier = Modifier.height(24.dp))

  GoogleSignInButton(
      viewModel = viewModel, credentialManager = credentialManager, isLoading = uiState.isLoading)

  Spacer(modifier = Modifier.height(24.dp))

  MicrosoftSignInButton(viewModel = viewModel, context = context, isLoading = uiState.isLoading)
}

@Composable
private fun EmailPasswordButton(
    viewModel: SignInViewModel,
    email: String,
    password: String,
    isRegistering: Boolean,
    isLoading: Boolean
) {
  Button(
      onClick = {
        if (isRegistering) {
          viewModel.signUpWithEmail(email, password)
        } else {
          viewModel.signInWithEmail(email, password)
        }
      },
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("emailPasswordButton"),
      enabled = !isLoading && email.isNotBlank() && password.isNotBlank()) {
        if (isLoading) {
          CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text(if (isRegistering) "Register" else "Sign In")
        }
      }
}

@Composable
private fun SignInRegisterToggle(
    isRegistering: Boolean,
    onRegisterToggle: (Boolean) -> Unit,
    isLoading: Boolean
) {
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
            onCheckedChange = onRegisterToggle,
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

@Composable
private fun OrDivider() {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    HorizontalDivider(modifier = Modifier.weight(1f))
    Text(
        text = "  OR  ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider(modifier = Modifier.weight(1f))
  }
}

@Composable
private fun GoogleSignInButton(
    viewModel: SignInViewModel,
    credentialManager: CredentialManager,
    isLoading: Boolean
) {
  OutlinedButton(
      onClick = { viewModel.signInWithGoogle(credentialManager) {} },
      modifier = Modifier.fillMaxWidth().height(65.dp),
      enabled = !isLoading) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        } else {
          Row(
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.google_sign_in),
                    contentDescription = "Google logo",
                    modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Continue with Google", style = MaterialTheme.typography.titleMedium)
              }
        }
      }
}

@Composable
private fun MicrosoftSignInButton(
    viewModel: SignInViewModel,
    context: android.content.Context,
    isLoading: Boolean
) {
  OutlinedButton(
      onClick = {
        // Microsoft sign-in requires Activity context for authentication flow
        val activity = context as? Activity
        if (activity != null) {
          viewModel.signInWithMicrosoft(activity)
        } else {
          Toast.makeText(context, "Unable to start Microsoft sign-in", Toast.LENGTH_SHORT).show()
        }
      },
      modifier = Modifier.fillMaxWidth().height(65.dp),
      enabled = !isLoading) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        } else {
          Row(
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.microsoft_sign_in),
                    contentDescription = "Microsoft logo",
                    modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Continue with Microsoft", style = MaterialTheme.typography.titleMedium)
              }
        }
      }
}

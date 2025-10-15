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

  // Observe authentication state and trigger success callback
  LaunchedEffect(uiState.isSignInSuccessful) {
    if (uiState.isSignInSuccessful) {
      Toast.makeText(
              context,
              "✅ Connexion réussie ! Bienvenue ${uiState.currentUser?.displayName ?: uiState.currentUser?.email ?: ""}",
              Toast.LENGTH_SHORT)
          .show()
      onSignInSuccess()
    }
  }

  // Display errors as toast messages and clear them from state
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { message ->
      Toast.makeText(context, "❌ $message", Toast.LENGTH_LONG).show()
      viewModel.clearError()
    }
  }

  Surface(
      modifier = Modifier.fillMaxSize().testTag(UiTestTags.AUTH_SCREEN),
      color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 54.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top) {
              Spacer(modifier = Modifier.height(40.dp))

              Image(
                  painter = painterResource(id = R.drawable.logo),
                  contentDescription = "App Logo",
                  modifier = Modifier.size(200.dp).clip(RoundedCornerShape(24.dp)))

              Box(
                  modifier =
                      Modifier.fillMaxWidth().padding(start = 110.dp, top = 4.dp, bottom = 40.dp),
                  contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = "One Map. Every moment.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.blur(0.7.dp))
                  }

              Spacer(modifier = Modifier.height(40.dp))

              // Email/Password Sign-In Section
              OutlinedTextField(
                  value = email,
                  onValueChange = { email = it },
                  label = { Text("Email") },
                  modifier = Modifier.fillMaxWidth(),
                  enabled = !uiState.isLoading,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                  singleLine = true)

              Spacer(modifier = Modifier.height(12.dp))

              OutlinedTextField(
                  value = password,
                  onValueChange = { password = it },
                  label = { Text("Password") },
                  modifier = Modifier.fillMaxWidth(),
                  enabled = !uiState.isLoading,
                  visualTransformation =
                      if (passwordVisible) VisualTransformation.None
                      else PasswordVisualTransformation(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                  trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                      Icon(
                          imageVector =
                              if (passwordVisible) Icons.Filled.Visibility
                              else Icons.Filled.VisibilityOff,
                          contentDescription =
                              if (passwordVisible) "Hide password" else "Show password")
                    }
                  },
                  singleLine = true)

              Spacer(modifier = Modifier.height(16.dp))

              Button(
                  onClick = {
                    if (isRegistering) {
                      viewModel.signUpWithEmail(email, password)
                    } else {
                      viewModel.signInWithEmail(email, password)
                    }
                  },
                  modifier = Modifier.fillMaxWidth().height(50.dp).testTag("emailPasswordButton"),
                  enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()) {
                    if (uiState.isLoading) {
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
                        onCheckedChange = { isRegistering = it },
                        modifier = Modifier.testTag("registerSwitch"),
                        enabled = !uiState.isLoading)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Register",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("registerLabel"),
                        color =
                            if (isRegistering) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                  }

              Spacer(modifier = Modifier.height(24.dp))

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  OR  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.weight(1f))
                  }

              Spacer(modifier = Modifier.height(24.dp))

              OutlinedButton(
                  onClick = { viewModel.signInWithGoogle(credentialManager) {} },
                  modifier = Modifier.fillMaxWidth().height(65.dp),
                  enabled = !uiState.isLoading) {
                    if (uiState.isLoading) {
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
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleMedium)
                          }
                    }
                  }

              Spacer(modifier = Modifier.height(24.dp))

              OutlinedButton(
                  onClick = {
                    // Microsoft sign-in requires Activity context for authentication flow
                    val activity = context as? Activity
                    if (activity != null) {
                      viewModel.signInWithMicrosoft(activity)
                    } else {
                      Toast.makeText(
                              context, "Unable to start Microsoft sign-in", Toast.LENGTH_SHORT)
                          .show()
                    }
                  },
                  modifier = Modifier.fillMaxWidth().height(65.dp),
                  enabled = !uiState.isLoading) {
                    if (uiState.isLoading) {
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
                            Text(
                                text = "Continue with Microsoft",
                                style = MaterialTheme.typography.titleMedium)
                          }
                    }
                  }
            }
      }
}

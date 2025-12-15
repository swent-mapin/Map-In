package com.swent.mapin.ui.auth

import android.app.Activity
import android.content.Context
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

/** Sign-in screen that provides authentication options for users. */
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

  SignInEffects(uiState, context, onSignInSuccess, email, password, viewModel)

  Surface(
      modifier = Modifier.fillMaxSize().testTag(UiTestTags.AUTH_SCREEN),
      color = MaterialTheme.colorScheme.background) {
        SignInContent(
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = it },
            isRegistering = isRegistering,
            onRegisteringChange = { isRegistering = it },
            uiState = uiState,
            viewModel = viewModel,
            credentialManager = credentialManager,
            context = context)
      }
}

@Composable
private fun SignInEffects(
    uiState: SignInUiState,
    context: Context,
    onSignInSuccess: () -> Unit,
    email: String,
    password: String,
    viewModel: SignInViewModel
) {
  LaunchedEffect(uiState.isSignInSuccessful) {
    if (uiState.isSignInSuccessful) {
      val welcomeName = uiState.currentUser?.displayName ?: uiState.currentUser?.email ?: ""
      Toast.makeText(context, "✅ Sign-in successful! Welcome $welcomeName", Toast.LENGTH_SHORT)
          .show()
      onSignInSuccess()
    }
  }

  LaunchedEffect(email, password) {
    if (uiState.error != null) {
      viewModel.clearError()
    }
  }
}

@Composable
private fun SignInContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    isRegistering: Boolean,
    onRegisteringChange: (Boolean) -> Unit,
    uiState: SignInUiState,
    viewModel: SignInViewModel,
    credentialManager: CredentialManager,
    context: Context
) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .imePadding()
              .padding(vertical = 54.dp, horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top) {
        Spacer(modifier = Modifier.height(40.dp))
        AppLogoSection()
        Spacer(modifier = Modifier.height(40.dp))

        EmailPasswordSection(
            email = email,
            onEmailChange = onEmailChange,
            password = password,
            onPasswordChange = onPasswordChange,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = onPasswordVisibilityChange,
            isRegistering = isRegistering,
            isLoading = uiState.isLoading)

        ErrorSection(uiState = uiState, context = context, viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        SignInButton(
            isRegistering = isRegistering,
            isLoading = uiState.isLoading,
            email = email,
            password = password,
            viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        AuthModeToggle(
            isRegistering = isRegistering,
            onRegisteringChange = onRegisteringChange,
            isLoading = uiState.isLoading)

        Spacer(modifier = Modifier.height(24.dp))
        OrDivider()
        Spacer(modifier = Modifier.height(24.dp))

        SocialSignInButtons(
            isLoading = uiState.isLoading,
            viewModel = viewModel,
            credentialManager = credentialManager,
            context = context)
      }
}

@Composable
private fun AppLogoSection() {
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

@Composable
private fun EmailPasswordSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
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
        IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
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
}

@Composable
private fun ErrorSection(uiState: SignInUiState, context: Context, viewModel: SignInViewModel) {
  uiState.error?.let { error ->
    val errorMessage = error.getMessage(context)
    if (errorMessage.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      SignInErrorCard(errorMessage = errorMessage, onDismiss = { viewModel.clearError() })
    }
  }
}

@Composable
private fun SignInButton(
    isRegistering: Boolean,
    isLoading: Boolean,
    email: String,
    password: String,
    viewModel: SignInViewModel
) {
  Button(
      onClick = {
        if (isRegistering) viewModel.signUpWithEmail(email, password)
        else viewModel.signInWithEmail(email, password)
      },
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("emailPasswordButton"),
      enabled = !isLoading && email.isNotBlank() && password.isNotBlank()) {
        if (isLoading)
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
        else Text(if (isRegistering) "Register" else "Sign In")
      }
}

@Composable
private fun AuthModeToggle(
    isRegistering: Boolean,
    onRegisteringChange: (Boolean) -> Unit,
    isLoading: Boolean
) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag("toggleSwitchRow"),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Sign In",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("signInLabel"),
            color =
                if (!isRegistering) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isRegistering,
            onCheckedChange = onRegisteringChange,
            modifier = Modifier.testTag("registerSwitch"),
            enabled = !isLoading)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Register",
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
        "  OR  ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider(modifier = Modifier.weight(1f))
  }
}

@Composable
private fun SocialSignInButtons(
    isLoading: Boolean,
    viewModel: SignInViewModel,
    credentialManager: CredentialManager,
    context: Context
) {
  SocialSignInButton(
      { viewModel.signInWithGoogle(credentialManager) {} },
      isLoading,
      R.drawable.google_sign_in,
      "Google logo",
      "Continue with Google")
  Spacer(modifier = Modifier.height(24.dp))
  SocialSignInButton(
      {
        (context as? Activity)?.let { viewModel.signInWithMicrosoft(it) }
            ?: Toast.makeText(context, "Unable to start Microsoft sign-in", Toast.LENGTH_SHORT)
                .show()
      },
      isLoading,
      R.drawable.microsoft_sign_in,
      "Microsoft logo",
      "Continue with Microsoft")
}

@Composable
private fun SocialSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    iconRes: Int,
    iconDescription: String,
    text: String
) {
  OutlinedButton(
      onClick = onClick, modifier = Modifier.fillMaxWidth().height(65.dp), enabled = !isLoading) {
        if (isLoading)
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        else
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                  Image(
                      painter = painterResource(id = iconRes),
                      contentDescription = iconDescription,
                      modifier = Modifier.size(28.dp))
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(text = text, style = MaterialTheme.typography.titleMedium)
                }
      }
}

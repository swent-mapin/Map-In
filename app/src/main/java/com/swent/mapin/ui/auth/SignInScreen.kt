package com.swent.mapin.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.R
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.util.findActivity

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

  // Observe authentication state and trigger success callback
  LaunchedEffect(uiState.isSignInSuccessful) {
    if (uiState.isSignInSuccessful) {
      Toast.makeText(
              context,
              "✅ Connexion réussie ! Bienvenue ${uiState.currentUser?.displayName ?: ""}",
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

              Spacer(modifier = Modifier.height(150.dp))

              OutlinedButton(
                  onClick = {
                    val activity = context.findActivity()
                    if (activity != null) {
                      viewModel.signInWithGoogle(credentialManager, activity) {}
                    } else {
                      Toast.makeText(context, "Unable to start Google sign-in", Toast.LENGTH_SHORT)
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
                                painter = painterResource(id = R.drawable.google_sign_in),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium)
                          }
                    }
                  }

              Spacer(modifier = Modifier.height(24.dp))

              OutlinedButton(
                  onClick = {
                    // Microsoft sign-in requires Activity context for authentication flow
                    val activity = context.findActivity()
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
                                text = "Sign in with Microsoft",
                                style = MaterialTheme.typography.titleMedium)
                          }
                    }
                  }
            }
      }
}

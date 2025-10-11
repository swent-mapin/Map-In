package com.swent.mapin.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI state for the sign-in screen.
 *
 * @property isLoading Indicates whether an authentication operation is in progress.
 * @property errorMessage Error message to display to the user, or null if no error.
 * @property isSignInSuccessful Indicates whether the sign-in was successful.
 * @property currentUser The currently authenticated [FirebaseUser], or null if not authenticated.
 */
data class SignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignInSuccessful: Boolean = false,
    val currentUser: FirebaseUser? = null
)

/**
 * ViewModel that manages authentication logic and UI state for sign-in operations.
 *
 * Supports multiple authentication methods:
 * - Google Sign-In using Credential Manager API
 * - Microsoft Sign-In using Firebase OAuth provider
 *
 * @property context Application context used for credential manager operations.
 */
class SignInViewModel(context: Context) : ViewModel() {
  private val applicationContext = context.applicationContext
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  private val _uiState = MutableStateFlow(SignInUiState(currentUser = auth.currentUser))
  val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

  /**
   * Initiates Google sign-in flow using Credential Manager API.
   *
   * This method uses the modern Credential Manager API to handle Google authentication. It prevents
   * multiple simultaneous sign-in attempts and updates the UI state accordingly.
   *
   * @param credentialManager The [CredentialManager] instance for handling credentials.
   * @param onSuccess Callback invoked when sign-in is successful.
   */
  fun signInWithGoogle(credentialManager: CredentialManager, onSuccess: () -> Unit = {}) {
    if (_uiState.value.isLoading) {
      return
    }

    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

    viewModelScope.launch {
      try {
        Log.d(TAG, "Starting Google Sign-In process with GetSignInWithGoogleOption...")

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build()

        val request =
            GetCredentialRequest.Builder().addCredentialOption(signInWithGoogleOption).build()

        val credentialResult = credentialManager.getCredential(applicationContext, request)
        val credential = credentialResult.credential

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        Log.d(TAG, "Google ID Token created successfully")

        val googleCredential =
            GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        val authResult = auth.signInWithCredential(googleCredential).await()

        authResult.user?.let { user ->
          Log.d(TAG, "Sign-in successful for user: ${user.displayName}")
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false,
                  isSignInSuccessful = true,
                  currentUser = user,
                  errorMessage = null)
          onSuccess()
        }
            ?: run {
              Log.e(TAG, "No user returned from Firebase")
              _uiState.value =
                  _uiState.value.copy(
                      isLoading = false, errorMessage = "Sign-in failed: No user returned")
            }
      } catch (e: Exception) {
        Log.e(TAG, "Sign-in failed: ${e.javaClass.simpleName}", e)

        // Enhanced error logging for debugging
        val errorDetails =
            when (e) {
              is androidx.credentials.exceptions.GetCredentialException -> {
                Log.e(TAG, "GetCredentialException - Type: ${e.type}")
                Log.e(TAG, "GetCredentialException - Message: ${e.message}")
                "Credential error: ${e.type}\n${e.message}"
              }
              is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                "Sign-in was cancelled"
              }
              is androidx.credentials.exceptions.NoCredentialException -> {
                "No Google accounts found on device"
              }
              else -> {
                if (e.message == null) {
                  "Sign-in failed"
                } else {
                  "Sign-in failed: ${e.message}"
                }
              }
            }

        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorDetails)
      }
    }
  }

  /**
   * Initiates Microsoft sign-in flow using Firebase OAuth provider.
   *
   * This method starts an activity-based OAuth flow for Microsoft authentication. It requires an
   * Activity context to launch the authentication UI.
   *
   * @param activity The [Activity] context required for launching the OAuth flow.
   */
  fun signInWithMicrosoft(activity: Activity) {
    if (_uiState.value.isLoading) {
      return
    }

    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

    viewModelScope.launch {
      try {
        val provider =
            OAuthProvider.newBuilder(MICROSOFT_PROVIDER_ID)
                .setScopes(listOf("openid", "email", "profile", "User.Read"))
                .build()

        auth
            .startActivityForSignInWithProvider(activity, provider)
            .addOnSuccessListener { authResult ->
              authResult.user?.let { user ->
                Log.d(TAG, "Microsoft sign-in successful for: ${user.displayName}")
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isSignInSuccessful = true,
                        currentUser = user,
                        errorMessage = null)
              }
                  ?: run {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Microsoft sign-in failed: No user returned")
                  }
            }
            .addOnFailureListener { e ->
              Log.e(TAG, "Microsoft sign-in failed", e)
              _uiState.value =
                  _uiState.value.copy(
                      isLoading = false, errorMessage = "Microsoft sign-in failed: ${e.message}")
            }
      } catch (e: Exception) {
        Log.e(TAG, "Microsoft sign-in exception", e)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = "Microsoft sign-in failed: ${e.message}")
      }
    }
  }

  /** Clears the current error message from the UI state. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  companion object {
    private const val TAG = "SignInViewModel"
    private const val WEB_CLIENT_ID =
        "816281112017-76ci0ij534q5q4h4qcafo7kp8bcna5vd.apps.googleusercontent.com"
    private const val MICROSOFT_PROVIDER_ID = "microsoft.com"

    /**
     * Factory for creating [SignInViewModel] instances with proper context injection.
     *
     * @param context Application context to be injected into the ViewModel.
     * @return A [ViewModelProvider.Factory] that creates [SignInViewModel] instances.
     */
    fun factory(context: Context): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SignInViewModel(context.applicationContext) as T
          }
        }
  }
}

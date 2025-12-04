package com.swent.mapin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.PreferencesRepositoryProvider
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.notifications.FCMTokenManager
import com.swent.mapin.ui.auth.BiometricLockScreen
import com.swent.mapin.ui.settings.ThemeMode
import com.swent.mapin.ui.theme.MapInTheme
import com.swent.mapin.util.BiometricAuthManager
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

/**
 * Main activity of the app.* Role: - Android entry point that hosts the Jetpack Compose UI. -
 * Applies the Material 3 theme and shows the map screen.
 */
class MainActivity : FragmentActivity() {
  // Use a queue to handle multiple deep links instead of overwriting
  private val deepLinkQueue = mutableStateListOf<String>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    MemoryRepositoryProvider.setRepository(MemoryRepositoryProvider.createLocalRepository())

    // Initialize EventRepositoryFirestore (uncomment to use Firestore backend)
    EventRepositoryProvider.init(this)
    EventRepositoryProvider.getRepository()

    initializeFcmIfLoggedIn()

    // Extract deep link from initial intent and add to queue
    getDeepLinkUrlFromIntent(intent)?.let { deepLinkQueue.add(it) }

    setContent {
      val preferencesRepository = remember { PreferencesRepositoryProvider.getInstance(this) }
      // Cache the theme mode flow collection to prevent repeated DataStore reads
      val themeModeString by
          remember(preferencesRepository) { preferencesRepository.themeModeFlow }
              .collectAsState(initial = "system")
      val themeMode = ThemeMode.fromString(themeModeString)

      val darkTheme =
          when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
          }

      // Biometric authentication state
      var biometricAuthRequired by remember { mutableStateOf(false) }
      var biometricAuthCompleted by remember { mutableStateOf(false) }
      var isAuthenticating by remember { mutableStateOf(false) }
      var authErrorMessage by remember { mutableStateOf<String?>(null) }
      var failedAttempts by remember { mutableIntStateOf(0) }

      val biometricAuthManager = remember { BiometricAuthManager() }

      // Check if biometric authentication is required on launch
      val biometricUnlockEnabled by
          remember(preferencesRepository) { preferencesRepository.biometricUnlockFlow }
              .collectAsState(initial = false)

      // Determine if we need to show the biometric lock screen
      if (shouldRequireBiometric(biometricAuthManager, biometricUnlockEnabled) &&
          !biometricAuthCompleted) {
        biometricAuthRequired = true
      }

      MapInTheme(darkTheme = darkTheme) {
        if (biometricAuthRequired && !biometricAuthCompleted) {
          BiometricLockScreen(
              isAuthenticating = isAuthenticating,
              errorMessage = authErrorMessage,
              onRetry = {
                handleBiometricRetry(
                    biometricAuthManager = biometricAuthManager,
                    failedAttempts = failedAttempts,
                    onAuthenticating = { isAuthenticating = it },
                    onAuthCompleted = { biometricAuthCompleted = it },
                    onFailedAttempts = { failedAttempts = it },
                    onErrorMessage = { authErrorMessage = it })
              },
              onUseAnotherAccount = {
                handleUseAnotherAccount(
                    onAuthRequired = { biometricAuthRequired = it },
                    onAuthCompleted = { biometricAuthCompleted = it })
              })
        } else {
          val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
          AppNavHost(isLoggedIn = isLoggedIn, deepLinkQueue = deepLinkQueue)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    // Handle deep links when app is already running - add to queue
    getDeepLinkUrlFromIntent(intent)?.let { deepLinkQueue.add(it) }
  }

  /** Initialize FCM for already logged-in users (when app restarts with active session). */
  private fun initializeFcmIfLoggedIn() {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    lifecycleScope.launch {
      try {
        FCMTokenManager().initializeForCurrentUser()
      } catch (e: Exception) {
        Log.e("MainActivity", "Failed to initialize FCM for logged-in user", e)
      }
    }
  }

  /** Determine if biometric authentication should be required. */
  private fun shouldRequireBiometric(
      biometricAuthManager: BiometricAuthManager,
      biometricUnlockEnabled: Boolean
  ): Boolean {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    return isLoggedIn && biometricUnlockEnabled && biometricAuthManager.canUseBiometric(this)
  }

  /** Handle biometric authentication retry logic. */
  private fun handleBiometricRetry(
      biometricAuthManager: BiometricAuthManager,
      failedAttempts: Int,
      onAuthenticating: (Boolean) -> Unit,
      onAuthCompleted: (Boolean) -> Unit,
      onFailedAttempts: (Int) -> Unit,
      onErrorMessage: (String?) -> Unit
  ) {
    onAuthenticating(true)
    onErrorMessage(null)
    biometricAuthManager.authenticate(
        activity = this,
        onSuccess = {
          onAuthenticating(false)
          onAuthCompleted(true)
          onFailedAttempts(0)
        },
        onError = { error ->
          onAuthenticating(false)
          val newFailedAttempts = failedAttempts + 1
          onFailedAttempts(newFailedAttempts)
          onErrorMessage(
              if (newFailedAttempts >= 3) {
                "Too many failed attempts. Please use another account or try again later."
              } else {
                error
              })
        },
        onFallback = {
          onAuthenticating(false)
          onErrorMessage("Authentication cancelled. Please try again.")
        })
  }

  /** Handle switching to another account by signing out. */
  private fun handleUseAnotherAccount(
      onAuthRequired: (Boolean) -> Unit,
      onAuthCompleted: (Boolean) -> Unit
  ) {
    FirebaseAuth.getInstance().signOut()
    onAuthRequired(false)
    onAuthCompleted(true)
  }
}

internal fun getDeepLinkUrlFromIntent(intent: Intent?): String? {
  return intent?.getStringExtra("action_url")
}

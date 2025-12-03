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

    // Initialize FCM for already logged-in users (when app restarts with active session)
    if (FirebaseAuth.getInstance().currentUser != null) {
      lifecycleScope.launch {
        try {
          val fcmManager = FCMTokenManager()
          fcmManager.initializeForCurrentUser()
        } catch (e: Exception) {
          Log.e("MainActivity", "Failed to initialize FCM for logged-in user", e)
        }
      }
    }

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
      val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
      val biometricUnlockEnabled by
          remember(preferencesRepository) { preferencesRepository.biometricUnlockFlow }
              .collectAsState(initial = false)
      val canUseBiometric =
          remember(biometricAuthManager) { biometricAuthManager.canUseBiometric(this@MainActivity) }

      // Determine if we need to show the biometric lock screen
      if (isLoggedIn && biometricUnlockEnabled && canUseBiometric && !biometricAuthCompleted) {
        biometricAuthRequired = true
      }

      MapInTheme(darkTheme = darkTheme) {
        if (biometricAuthRequired && !biometricAuthCompleted) {
          // Show biometric lock screen
          BiometricLockScreen(
              isAuthenticating = isAuthenticating,
              errorMessage = authErrorMessage,
              onRetry = {
                isAuthenticating = true
                authErrorMessage = null
                biometricAuthManager.authenticate(
                    activity = this@MainActivity,
                    onSuccess = {
                      isAuthenticating = false
                      biometricAuthCompleted = true
                      failedAttempts = 0
                    },
                    onError = { error ->
                      isAuthenticating = false
                      failedAttempts++
                      authErrorMessage =
                          if (failedAttempts >= 3) {
                            "Too many failed attempts. Please use another account or try again later."
                          } else {
                            error
                          }
                    },
                    onFallback = {
                      isAuthenticating = false
                      authErrorMessage = "Authentication cancelled. Please try again."
                    })
              },
              onUseAnotherAccount = {
                // Logout and go to sign-in
                FirebaseAuth.getInstance().signOut()
                biometricAuthRequired = false
                biometricAuthCompleted = true
              })
        } else {
          // Show normal app flow with deep link support
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
}

internal fun getDeepLinkUrlFromIntent(intent: Intent?): String? {
  return intent?.getStringExtra("action_url")
}

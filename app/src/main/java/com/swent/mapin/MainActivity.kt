package com.swent.mapin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/** Global provider for OkHttpClient instance. Used throughout the app for network operations. */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

/**
 * Represents the state of biometric authentication lock screen. Used as an explicit state machine
 * to avoid UI flicker during transitions.
 */
private enum class BiometricLockState {
  /** User needs to authenticate - lock screen is shown */
  LOCKED,
  /** Authentication is in progress */
  UNLOCKING,
  /** User has been authenticated or biometric is not required - app content is shown */
  UNLOCKED
}

/**
 * Main activity of the Map-In application.
 *
 * This is the Android entry point that hosts the Jetpack Compose UI. It handles:
 * - Application initialization (repositories, FCM tokens)
 * - Deep link processing from push notifications and external sources
 * - Biometric authentication lock screen when enabled
 * - Theme configuration (light/dark/system)
 * - Edge-to-edge display support
 *
 * The activity maintains biometric authentication state to prevent unauthorized access when the
 * feature is enabled in settings.
 */
class MainActivity : FragmentActivity() {
  /**
   * Deep link extracted from the initial Intent (onCreate) or subsequent Intents (onNewIntent).
   *
   * **Lifecycle:**
   * - Set in onCreate() from the launching intent
   * - Updated in onNewIntent() when app receives new deep links while running
   * - Consumed by AppNavHost to trigger navigation when biometric lock is not active
   * - Cleared automatically after navigation processes it
   *
   * **Interaction with pendingDeepLink:** When biometric authentication is required, this value is
   * stored in pendingDeepLink and delivered only after successful authentication. If no biometric
   * lock is active, this is passed directly to AppNavHost.
   */
  private var deepLink by mutableStateOf<String?>(null)

  /**
   * Deep link awaiting delivery after biometric authentication completes.
   *
   * **Lifecycle:**
   * - Set in onNewIntent() when a new deep link arrives while the app is locked
   * - Replaces deepLink as the effective navigation target after unlock
   * - Cleared in a LaunchedEffect after being consumed by AppNavHost
   * - Remains null if no deep link arrives during locked state
   *
   * **Interaction with deepLink:** Takes precedence over deepLink when both are set. This ensures
   * that deep links received while the user is authenticating are not lost. Once authentication
   * succeeds, pendingDeepLink becomes the effectiveDeepLink passed to navigation.
   */
  private var pendingDeepLink by mutableStateOf<String?>(null)

  /** Atomic guard to prevent concurrent biometric authentication attempts */
  private val isAuthenticationInProgress = AtomicBoolean(false)

  /** Reference to active BiometricPrompt for lifecycle-aware cancellation */
  private var activeBiometricPrompt: BiometricPrompt? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    MemoryRepositoryProvider.setRepository(MemoryRepositoryProvider.createLocalRepository())

    // Initialize EventRepositoryFirestore (uncomment to use Firestore backend)
    EventRepositoryProvider.init(this)
    EventRepositoryProvider.getRepository()

    initializeFcmIfLoggedIn()

    // Extract deep link from initial intent
    deepLink = getDeepLinkUrlFromIntent(intent)

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

      // Biometric authentication state machine
      var lockState by remember { mutableStateOf(BiometricLockState.LOCKED) }
      var authErrorMessage by remember { mutableStateOf<String?>(null) }
      var failedAttempts by remember { mutableIntStateOf(0) }

      val biometricAuthManager = remember { BiometricAuthManager() }

      // Check if biometric authentication is required on launch
      val biometricUnlockEnabled by
          remember(preferencesRepository) { preferencesRepository.biometricUnlockFlow }
              .collectAsState(initial = false)

      // Determine if biometric is required (recalculated when biometricUnlockEnabled changes)
      val requiresBiometric = shouldRequireBiometric(biometricAuthManager, biometricUnlockEnabled)

      // Derive whether to show lock screen from state machine
      val shouldShowBiometricLock by
          remember(requiresBiometric) {
            derivedStateOf { requiresBiometric && lockState != BiometricLockState.UNLOCKED }
          }

      // Use rememberUpdatedState for mutable values captured in effect
      val currentFailedAttempts by rememberUpdatedState(failedAttempts)

      // Lifecycle-aware biometric authentication with proper cleanup
      DisposableEffect(shouldShowBiometricLock) {
        if (shouldShowBiometricLock && lockState == BiometricLockState.LOCKED) {
          lockState = BiometricLockState.UNLOCKING
          try {
            startBiometricAuthentication(
                biometricAuthManager = biometricAuthManager,
                failedAttempts = currentFailedAttempts,
                onAuthenticating = { /* State is managed by lockState */},
                onAuthCompleted = { success ->
                  if (success) {
                    lockState = BiometricLockState.UNLOCKED
                  }
                },
                onFailedAttempts = { failedAttempts = it },
                onErrorMessage = { error ->
                  authErrorMessage = error
                  if (error != null) {
                    // Authentication failed, go back to LOCKED to allow retry
                    lockState = BiometricLockState.LOCKED
                  }
                })
          } catch (e: Exception) {
            Log.e("MainActivity", "Biometric authentication failed unexpectedly", e)
            authErrorMessage =
                "Authentication unavailable. Please try again or use another account."
            lockState = BiometricLockState.LOCKED
            isAuthenticationInProgress.set(false)
          }
        }

        onDispose {
          // Cancel any ongoing biometric prompt when composable leaves composition
          cancelBiometricAuthentication()
        }
      }

      MapInTheme(darkTheme = darkTheme) {
        if (shouldShowBiometricLock) {
          // Show biometric lock screen
          BiometricLockScreen(
              isAuthenticating = lockState == BiometricLockState.UNLOCKING,
              errorMessage = authErrorMessage,
              onRetry = {
                lockState = BiometricLockState.UNLOCKING
                authErrorMessage = null
                try {
                  startBiometricAuthentication(
                      biometricAuthManager = biometricAuthManager,
                      failedAttempts = failedAttempts,
                      onAuthenticating = { /* State is managed by lockState */},
                      onAuthCompleted = { success ->
                        if (success) {
                          lockState = BiometricLockState.UNLOCKED
                        }
                      },
                      onFailedAttempts = { failedAttempts = it },
                      onErrorMessage = { error ->
                        authErrorMessage = error
                        if (error != null) {
                          lockState = BiometricLockState.LOCKED
                        }
                      })
                } catch (e: Exception) {
                  Log.e("MainActivity", "Biometric retry failed unexpectedly", e)
                  authErrorMessage =
                      "Authentication unavailable. Please try again or use another account."
                  lockState = BiometricLockState.LOCKED
                  isAuthenticationInProgress.set(false)
                }
              },
              onUseAnotherAccount = {
                handleUseAnotherAccount(
                    onAuthCompleted = {
                      // Signing out means biometric is no longer required
                      lockState = BiometricLockState.UNLOCKED
                    })
              })
        } else {
          // Deliver any pending deep link after successful authentication
          val effectiveDeepLink = pendingDeepLink ?: deepLink

          // Clear pending deep link after it's been consumed
          LaunchedEffect(Unit) {
            if (pendingDeepLink != null) {
              pendingDeepLink = null
            }
          }

          // Check if user is already authenticated with Firebase
          val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
          AppNavHost(isLoggedIn = isLoggedIn, deepLink = effectiveDeepLink)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    // Handle deep links when app is already running
    // Store as pending - will be delivered after biometric unlock if needed
    getDeepLinkUrlFromIntent(intent)?.let {
      pendingDeepLink = it
      deepLink = it
    }
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

  /**
   * Start biometric authentication with idempotent guard. Uses an atomic guard to prevent
   * concurrent/duplicate authentication attempts. If authentication is already in progress,
   * subsequent calls are ignored. Stores the BiometricPrompt reference for lifecycle-aware
   * cancellation.
   */
  private fun startBiometricAuthentication(
      biometricAuthManager: BiometricAuthManager,
      failedAttempts: Int,
      onAuthenticating: (Boolean) -> Unit,
      onAuthCompleted: (Boolean) -> Unit,
      onFailedAttempts: (Int) -> Unit,
      onErrorMessage: (String?) -> Unit
  ) {
    // Atomic guard: only proceed if no authentication is in progress
    if (!isAuthenticationInProgress.compareAndSet(false, true)) {
      return // Already authenticating, ignore this call
    }

    onAuthenticating(true)
    onErrorMessage(null)

    try {
      // Store the prompt reference for potential cancellation
      activeBiometricPrompt =
          biometricAuthManager.authenticate(
              activity = this,
              onSuccess = {
                activeBiometricPrompt = null
                isAuthenticationInProgress.set(false)
                onAuthenticating(false)
                onAuthCompleted(true)
                onFailedAttempts(0)
              },
              onError = { error ->
                activeBiometricPrompt = null
                isAuthenticationInProgress.set(false)
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
                activeBiometricPrompt = null
                isAuthenticationInProgress.set(false)
                onAuthenticating(false)
                onErrorMessage("Authentication cancelled. Please try again.")
              })
    } catch (e: Exception) {
      // Handle unexpected biometric API failures
      Log.e("MainActivity", "BiometricAuthManager.authenticate() threw exception", e)
      activeBiometricPrompt = null
      isAuthenticationInProgress.set(false)
      onAuthenticating(false)
      onErrorMessage("Biometric authentication is temporarily unavailable. Please try again.")
    }
  }

  /**
   * Cancel any ongoing biometric authentication. Called when composable leaves composition or
   * activity lifecycle changes.
   */
  private fun cancelBiometricAuthentication() {
    activeBiometricPrompt?.cancelAuthentication()
    activeBiometricPrompt = null
    isAuthenticationInProgress.set(false)
  }

  /** Handle switching to another account by signing out. */
  private fun handleUseAnotherAccount(onAuthCompleted: (Boolean) -> Unit) {
    FirebaseAuth.getInstance().signOut()
    onAuthCompleted(true)
  }
}

/** Extracts deep link URL from intent, checking both Firebase and PendingIntent keys. */
internal fun getDeepLinkUrlFromIntent(intent: Intent?): String? {
  // Check both "actionUrl" (from Firebase) and "action_url" (from PendingIntent)
  return intent?.getStringExtra("actionUrl") ?: intent?.getStringExtra("action_url")
}

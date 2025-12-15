package com.swent.mapin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
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
import com.swent.mapin.model.PreferencesRepository
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
  LOCKED,
  UNLOCKING,
  UNLOCKED
}

/** Holds biometric authentication state to reduce parameter passing complexity. */
private class BiometricAuthState {
  var lockState by mutableStateOf(BiometricLockState.LOCKED)
  var errorMessage by mutableStateOf<String?>(null)
  var failedAttempts by mutableIntStateOf(0)

  fun onAuthSuccess() {
    lockState = BiometricLockState.UNLOCKED
    failedAttempts = 0
  }

  fun onAuthError(error: String?) {
    errorMessage = error
    if (error != null) {
      lockState = BiometricLockState.LOCKED
    }
  }

  fun startUnlocking() {
    lockState = BiometricLockState.UNLOCKING
    errorMessage = null
  }
}

/** Main activity of the Map-In application. */
class MainActivity : FragmentActivity() {
  private var deepLink by mutableStateOf<String?>(null)
  private var pendingDeepLink by mutableStateOf<String?>(null)
  private val isAuthenticationInProgress = AtomicBoolean(false)
  private var activeBiometricPrompt: BiometricPrompt? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    initializeRepositories()
    initializeFcmIfLoggedIn()
    deepLink = getDeepLinkUrlFromIntent(intent)

    setContent { MainContent() }
  }

  private fun initializeRepositories() {
    MemoryRepositoryProvider.getRepository()
    EventRepositoryProvider.init(this)
    EventRepositoryProvider.getRepository()
  }

  @Composable
  private fun MainContent() {
    val preferencesRepository = remember { PreferencesRepositoryProvider.getInstance(this) }
    val darkTheme = rememberDarkTheme(preferencesRepository)
    val biometricAuthManager = remember { BiometricAuthManager() }
    val authState = remember { BiometricAuthState() }

    val biometricUnlockEnabled by
        remember(preferencesRepository) { preferencesRepository.biometricUnlockFlow }
            .collectAsState(initial = false)

    val requiresBiometric = shouldRequireBiometric(biometricAuthManager, biometricUnlockEnabled)
    val shouldShowBiometricLock by
        remember(requiresBiometric) {
          derivedStateOf { requiresBiometric && authState.lockState != BiometricLockState.UNLOCKED }
        }

    BiometricAuthEffect(shouldShowBiometricLock, authState, biometricAuthManager)

    MapInTheme(darkTheme = darkTheme) {
      if (shouldShowBiometricLock) {
        BiometricLockContent(authState, biometricAuthManager)
      } else {
        MainAppContent()
      }
    }
  }

  @Composable
  private fun rememberDarkTheme(preferencesRepository: PreferencesRepository): Boolean {
    val themeModeString by
        remember(preferencesRepository) { preferencesRepository.themeModeFlow }
            .collectAsState(initial = "system")
    val themeMode = ThemeMode.fromString(themeModeString)

    return when (themeMode) {
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
  }

  @Composable
  private fun BiometricAuthEffect(
      shouldShowBiometricLock: Boolean,
      authState: BiometricAuthState,
      biometricAuthManager: BiometricAuthManager
  ) {
    val currentFailedAttempts by rememberUpdatedState(authState.failedAttempts)

    DisposableEffect(shouldShowBiometricLock) {
      if (shouldShowBiometricLock && authState.lockState == BiometricLockState.LOCKED) {
        authState.lockState = BiometricLockState.UNLOCKING
        triggerBiometricAuth(biometricAuthManager, currentFailedAttempts, authState)
      }
      onDispose { cancelBiometricAuthentication() }
    }
  }

  private fun triggerBiometricAuth(
      biometricAuthManager: BiometricAuthManager,
      failedAttempts: Int,
      authState: BiometricAuthState
  ) {
    try {
      startBiometricAuthentication(
          biometricAuthManager,
          failedAttempts,
          { success -> if (success) authState.onAuthSuccess() },
          { authState.failedAttempts = it },
          { authState.onAuthError(it) })
    } catch (e: Exception) {
      Log.e("MainActivity", "Biometric authentication failed unexpectedly", e)
      authState.onAuthError("Authentication unavailable. Please try again or use another account.")
      isAuthenticationInProgress.set(false)
    }
  }

  @Composable
  private fun BiometricLockContent(
      authState: BiometricAuthState,
      biometricAuthManager: BiometricAuthManager
  ) {
    BiometricLockScreen(
        isAuthenticating = authState.lockState == BiometricLockState.UNLOCKING,
        errorMessage = authState.errorMessage,
        onRetry = {
          authState.startUnlocking()
          triggerBiometricAuth(biometricAuthManager, authState.failedAttempts, authState)
        },
        onUseAnotherAccount = { handleUseAnotherAccount { authState.onAuthSuccess() } })
  }

  @Composable
  private fun MainAppContent() {
    val effectiveDeepLink = pendingDeepLink ?: deepLink
    LaunchedEffect(Unit) { if (pendingDeepLink != null) pendingDeepLink = null }

    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    AppNavHost(isLoggedIn = isLoggedIn, deepLink = effectiveDeepLink)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    getDeepLinkUrlFromIntent(intent)?.let {
      pendingDeepLink = it
      deepLink = it
    }
  }

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

  private fun shouldRequireBiometric(
      biometricAuthManager: BiometricAuthManager,
      biometricUnlockEnabled: Boolean
  ): Boolean {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    return isLoggedIn && biometricUnlockEnabled && biometricAuthManager.canUseBiometric(this)
  }

  private fun startBiometricAuthentication(
      biometricAuthManager: BiometricAuthManager,
      failedAttempts: Int,
      onAuthCompleted: (Boolean) -> Unit,
      onFailedAttempts: (Int) -> Unit,
      onErrorMessage: (String?) -> Unit
  ) {
    if (!isAuthenticationInProgress.compareAndSet(false, true)) {
      return
    }

    onErrorMessage(null)

    activeBiometricPrompt =
        biometricAuthManager.authenticate(
            activity = this,
            onSuccess = {
              clearBiometricPromptState()
              onAuthCompleted(true)
              onFailedAttempts(0)
            },
            onError = { error ->
              clearBiometricPromptState()
              val newFailedAttempts = failedAttempts + 1
              onFailedAttempts(newFailedAttempts)
              onErrorMessage(getErrorMessage(newFailedAttempts, error))
            },
            onFallback = {
              clearBiometricPromptState()
              onErrorMessage("Authentication cancelled. Please try again.")
            })
  }

  private fun clearBiometricPromptState() {
    activeBiometricPrompt = null
    isAuthenticationInProgress.set(false)
  }

  private fun getErrorMessage(failedAttempts: Int, error: String): String {
    return if (failedAttempts >= 3) {
      "Too many failed attempts. Please use another account or try again later."
    } else {
      error
    }
  }

  private fun cancelBiometricAuthentication() {
    activeBiometricPrompt?.cancelAuthentication()
    clearBiometricPromptState()
  }

  private fun handleUseAnotherAccount(onAuthCompleted: (Boolean) -> Unit) {
    FirebaseAuth.getInstance().signOut()
    onAuthCompleted(true)
  }
}

/** Extracts deep link URL from intent, checking both Firebase and PendingIntent keys. */
internal fun getDeepLinkUrlFromIntent(intent: Intent?): String? {
  return intent?.getStringExtra("actionUrl") ?: intent?.getStringExtra("action_url")
}

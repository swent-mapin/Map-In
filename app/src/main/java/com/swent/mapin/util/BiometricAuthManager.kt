package com.swent.mapin.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Utility class that wraps Android's BiometricPrompt API for biometric authentication.
 *
 * This class provides a simplified interface for checking biometric availability and authenticating
 * users using fingerprint, face recognition, or device credentials (PIN/pattern). On Samsung
 * devices, this automatically integrates with Samsung Pass via the BiometricPrompt API.
 *
 * Usage:
 * ```
 * val biometricManager = BiometricAuthManager()
 * if (biometricManager.canUseBiometric(context)) {
 *   biometricManager.authenticate(
 *     activity = activity,
 *     onSuccess = { /* Handle success */ },
 *     onError = { error -> /* Handle error */ },
 *     onFallback = { /* Handle fallback */ }
 *   )
 * }
 * ```
 */
class BiometricAuthManager {

  /**
   * Check if the device supports biometric authentication.
   *
   * This checks for BIOMETRIC_WEAK (includes less secure face unlock), BIOMETRIC_STRONG
   * (fingerprint, iris, secure face), or DEVICE_CREDENTIAL (PIN/pattern/password) authentication
   * methods.
   *
   * @param activity The FragmentActivity context needed to access BiometricManager
   * @return true if device supports biometrics or device credentials, false otherwise
   */
  fun canUseBiometric(activity: FragmentActivity): Boolean {
    val biometricManager = BiometricManager.from(activity)
    // First try BIOMETRIC_STRONG (most secure - fingerprint, iris, secure face)
    val strongResult =
        biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)

    if (strongResult == BiometricManager.BIOMETRIC_SUCCESS) {
      return true
    }

    // If STRONG not available, try BIOMETRIC_WEAK (includes less secure face unlock)
    val weakResult =
        biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)

    return weakResult == BiometricManager.BIOMETRIC_SUCCESS
  }

  /**
   * Show biometric authentication prompt to the user.
   *
   * This displays the system biometric prompt. It supports:
   * - BIOMETRIC_STRONG: fingerprint, iris, secure face recognition
   * - BIOMETRIC_WEAK: less secure face recognition (more devices supported)
   * - With a "Cancel" button for user to dismiss
   *
   * @param activity The FragmentActivity from which to show the prompt
   * @param onSuccess Callback invoked when authentication succeeds
   * @param onError Callback invoked when authentication fails with an error message
   * @param onFallback Callback invoked when user cancels or authentication fails multiple times
   * @return BiometricPrompt instance that can be used to cancel authentication, or null if
   *   unavailable
   */
  fun authenticate(
      activity: FragmentActivity,
      onSuccess: () -> Unit,
      onError: (String) -> Unit,
      onFallback: () -> Unit
  ): BiometricPrompt? {
    // Early check: ensure biometric authentication is available
    if (!canUseBiometric(activity)) {
      onError("Biometric authentication not available on this device.")
      return null
    }

    val executor = ContextCompat.getMainExecutor(activity)

    val biometricPrompt =
        BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
              override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
              }

              override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                  BiometricPrompt.ERROR_USER_CANCELED,
                  BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                  BiometricPrompt.ERROR_CANCELED -> {
                    onFallback()
                  }
                  BiometricPrompt.ERROR_LOCKOUT,
                  BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                    onError("Too many attempts. Please try again later or use another account.")
                  }
                  else -> {
                    onError(errString.toString())
                  }
                }
              }

              override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't call onError here - this is called for each failed attempt
                // The user can try again unless locked out (handled in onAuthenticationError)
              }
            })

    // Determine which authenticators are available
    val biometricManager = BiometricManager.from(activity)

    // Check if device has BOTH strong and weak biometrics
    val hasStrong =
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    val hasWeak =
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    // KEY FIX: If device has BOTH (fingerprint + face), use BIOMETRIC_WEAK
    // This allows the system to show BOTH options in the prompt
    // BIOMETRIC_STRONG alone only shows fingerprint!
    val allowedAuthenticators =
        when {
          hasStrong && hasWeak -> {
            // Device has both fingerprint and face - use WEAK to show both options
            BiometricManager.Authenticators.BIOMETRIC_WEAK
          }
          hasStrong -> {
            // Only fingerprint (or secure face)
            BiometricManager.Authenticators.BIOMETRIC_STRONG
          }
          hasWeak -> {
            // Only face unlock
            BiometricManager.Authenticators.BIOMETRIC_WEAK
          }
          else -> {
            // Last resort: device credential
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
          }
        }

    // Dynamic subtitle based on what's available
    val subtitle =
        when {
          hasStrong && hasWeak -> "Use fingerprint or face to continue"
          hasStrong -> "Verify your fingerprint"
          hasWeak -> "Verify your face"
          else -> "Use your device PIN, pattern, or password"
        }

    // Build prompt info based on authenticator type
    val promptInfoBuilder =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Map'In")
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators)

    // Only add negative button if NOT using DEVICE_CREDENTIAL
    // (DEVICE_CREDENTIAL shows system UI with its own cancel)
    if (allowedAuthenticators != BiometricManager.Authenticators.DEVICE_CREDENTIAL) {
      promptInfoBuilder.setNegativeButtonText("Cancel")
    }

    val promptInfo = promptInfoBuilder.build()

    biometricPrompt.authenticate(promptInfo)
    return biometricPrompt
  }
}

package com.swent.mapin.ui.auth

import java.util.Locale

/**
 * Utility object for mapping Firebase authentication exceptions to [AuthError] types.
 *
 * This centralizes error mapping logic, making it easier to test and maintain. All string matching
 * is case-insensitive and uses normalized messages.
 */
object AuthErrorMapper {

  /**
   * Maps a sign-in exception to an appropriate [AuthError].
   *
   * @param exception The exception thrown during sign-in.
   * @return The corresponding [AuthError] type.
   */
  fun mapSignInException(exception: Exception): AuthError {
    val message = normalizeMessage(exception.message)

    return when {
      // No account found
      message.contains("no user record") || message.contains("user not found") ->
          AuthError.NoAccountFound

      // Invalid/incorrect credentials (group similar errors)
      message.contains("password is invalid") ||
          message.contains("wrong password") ||
          message.contains("credential is incorrect") ||
          message.contains("credential is malformed") ||
          message.contains("auth credential") ||
          message.contains("invalid credential") -> AuthError.IncorrectCredentials

      // Invalid email format
      message.contains("badly formatted") || message.contains("invalid email") ->
          AuthError.InvalidEmailFormat

      // Network errors
      message.contains("network error") ||
          message.contains("network_error") ||
          message.contains("unable to resolve host") ||
          message.contains("failed to connect") -> AuthError.NetworkError

      // Rate limiting
      message.contains("too many requests") ||
          message.contains("blocked all requests") ||
          message.contains("we have blocked") ||
          message.contains("unusual activity") -> AuthError.TooManyRequests

      // Account disabled
      message.contains("user has been disabled") || message.contains("account has been disabled") ->
          AuthError.AccountDisabled

      // Generic fallback
      else -> AuthError.SignInFailed
    }
  }

  /**
   * Maps a sign-up exception to an appropriate [AuthError].
   *
   * @param exception The exception thrown during sign-up.
   * @return The corresponding [AuthError] type.
   */
  fun mapSignUpException(exception: Exception): AuthError {
    val message = normalizeMessage(exception.message)

    return when {
      // Email already in use
      message.contains("email address is already in use") ||
          message.contains("email already in use") ||
          message.contains("account already exists") -> AuthError.EmailAlreadyInUse

      // Invalid email format
      message.contains("badly formatted") || message.contains("invalid email") ->
          AuthError.InvalidEmailFormat

      // Weak password
      message.contains("weak") || message.contains("password should be") -> AuthError.WeakPassword

      // Network errors
      message.contains("network error") ||
          message.contains("network_error") ||
          message.contains("unable to resolve host") ||
          message.contains("failed to connect") -> AuthError.NetworkError

      // Rate limiting
      message.contains("too many requests") ||
          message.contains("blocked all requests") ||
          message.contains("we have blocked") -> AuthError.TooManyRequests

      // Generic fallback with message
      else -> AuthError.SignUpFailed(exception.message)
    }
  }

  /**
   * Normalizes an exception message for consistent matching. Converts to lowercase and handles null
   * messages.
   *
   * @param message The raw exception message.
   * @return Normalized lowercase message, or empty string if null.
   */
  private fun normalizeMessage(message: String?): String {
    return message?.lowercase(Locale.ROOT) ?: ""
  }
}

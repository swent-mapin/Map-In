package com.swent.mapin.ui.auth

import android.content.Context
import androidx.annotation.StringRes
import com.swent.mapin.R

/**
 * Sealed class representing authentication errors.
 *
 * Using resource IDs allows for proper localization of error messages. The UI layer resolves these
 * to localized strings using [getMessage].
 */
sealed class AuthError {
  /**
   * Resolves the error to a localized string message.
   *
   * @param context Context used to resolve string resources.
   * @return The localized error message.
   */
  abstract fun getMessage(context: Context): String

  // Google Sign-In Errors
  object SignInCancelled : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_sign_in_cancelled)
  }

  object NoGoogleAccounts : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_no_google_accounts)
  }

  data class CredentialError(val type: String, val message: String?) : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_credential, "$type\n${message ?: ""}")
  }

  // General Sign-In Errors
  object SignInFailed : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_sign_in_failed)
  }

  data class SignInFailedWithMessage(val message: String) : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_sign_in_failed_with_message, message)
  }

  object NoUserReturned : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_no_user_returned)
  }

  // Microsoft Sign-In Errors
  data class MicrosoftSignInFailed(val message: String?) : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_microsoft_sign_in_failed, message ?: "")
  }

  // Email/Password Errors
  object EmailPasswordEmpty : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_email_password_empty)
  }

  object NoAccountFound : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_no_account_found)
  }

  object InvalidPassword : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_invalid_password)
  }

  object IncorrectCredentials : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_incorrect_credentials)
  }

  object InvalidEmailFormat : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_invalid_email_format)
  }

  object EmailAlreadyInUse : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_email_already_in_use)
  }

  object WeakPassword : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_weak_password)
  }

  object NetworkError : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_network)
  }

  object TooManyRequests : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_too_many_requests)
  }

  object AccountDisabled : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_account_disabled)
  }

  data class SignUpFailed(val message: String?) : AuthError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.auth_error_sign_up_failed, message ?: "")
  }

  /** Error from password validation with a resource ID. */
  data class PasswordValidation(@StringRes val messageResId: Int) : AuthError() {
    override fun getMessage(context: Context): String = context.getString(messageResId)
  }
}

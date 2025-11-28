package com.swent.mapin.util

import com.swent.mapin.R

// Assisted by AI

/**
 * Sealed class representing password validation results.
 *
 * This provides a type-safe way to handle validation outcomes and ensures a single source of truth
 * for validation error messages across the application.
 */
sealed class PasswordValidationResult {
  /** Password meets all requirements */
  object Valid : PasswordValidationResult()

  /** Password validation failed - contains which requirements were not met */
  sealed class Invalid : PasswordValidationResult() {
    /** Returns the string resource ID for the error message */
    abstract val messageResId: Int

    /** Password is too short (less than 8 characters) */
    object TooShort : Invalid() {
      override val messageResId = R.string.password_error_too_short
    }

    /** Password is missing an uppercase letter */
    object MissingUppercase : Invalid() {
      override val messageResId = R.string.password_error_missing_uppercase
    }

    /** Password is missing a lowercase letter */
    object MissingLowercase : Invalid() {
      override val messageResId = R.string.password_error_missing_lowercase
    }

    /** Password is missing a digit */
    object MissingDigit : Invalid() {
      override val messageResId = R.string.password_error_missing_digit
    }

    /** Password is missing a special character */
    object MissingSpecialChar : Invalid() {
      override val messageResId = R.string.password_error_missing_special_char
    }
  }
}

/**
 * Data class representing individual password requirement states.
 *
 * Used primarily for UI display to show which requirements are met/unmet in real-time.
 *
 * @property hasMinLength True if password has at least 8 characters
 * @property hasUppercase True if password contains at least one uppercase letter
 * @property hasLowercase True if password contains at least one lowercase letter
 * @property hasDigit True if password contains at least one digit
 * @property hasSpecialChar True if password contains at least one special character
 */
data class PasswordValidation(
    val hasMinLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasLowercase: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecialChar: Boolean = false
) {
  /** Returns true if all password requirements are met */
  fun isValid(): Boolean {
    return hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar
  }

  /**
   * Converts this validation state to a [PasswordValidationResult].
   *
   * @return [PasswordValidationResult.Valid] if all requirements are met, or the first
   *   [PasswordValidationResult.Invalid] subtype encountered
   */
  fun toResult(): PasswordValidationResult {
    return when {
      !hasMinLength -> PasswordValidationResult.Invalid.TooShort
      !hasUppercase -> PasswordValidationResult.Invalid.MissingUppercase
      !hasLowercase -> PasswordValidationResult.Invalid.MissingLowercase
      !hasDigit -> PasswordValidationResult.Invalid.MissingDigit
      !hasSpecialChar -> PasswordValidationResult.Invalid.MissingSpecialChar
      else -> PasswordValidationResult.Valid
    }
  }
}

/** Utility object for password validation. */
object PasswordValidationUtils {
  /**
   * Validates a password against security requirements.
   *
   * Requirements:
   * - At least 8 characters
   * - At least one uppercase letter (A-Z)
   * - At least one lowercase letter (a-z)
   * - At least one digit (0-9)
   * - At least one special character (anything not A-Z, a-z, or 0-9)
   *
   * Note: For security purposes, only ASCII letters and digits are considered "normal" characters.
   * All other characters including Unicode letters (Chinese, Cyrillic, etc.), emoji, and symbols
   * are treated as special characters.
   *
   * @param password The password to validate
   * @return [PasswordValidation] object containing validation results for each requirement
   */
  fun validatePassword(password: String): PasswordValidation {
    return PasswordValidation(
        hasMinLength = password.length >= 8,
        hasUppercase = password.any { it.isUpperCase() && it.isLetter() },
        hasLowercase = password.any { it.isLowerCase() && it.isLetter() },
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar =
            password.any { char ->
              // Special char = anything that's not an ASCII letter or digit
              !char.isLetterOrDigit() || (char.isLetter() && char !in 'A'..'Z' && char !in 'a'..'z')
            })
  }
}

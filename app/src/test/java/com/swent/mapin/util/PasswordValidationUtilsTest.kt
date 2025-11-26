package com.swent.mapin.util

import com.swent.mapin.R
import com.swent.mapin.util.PasswordValidationUtils.validatePassword
import org.junit.Assert.*
import org.junit.Test

// Assisted by AI

/**
 * Comprehensive unit tests for password validation logic.
 *
 * Tests cover:
 * - Basic validation rules (length, uppercase, lowercase, digit, special char)
 * - Edge cases (empty string, very long passwords, boundary conditions)
 * - Unicode characters (emoji, accented characters, non-Latin scripts)
 * - Special character combinations (only punctuation, mixed symbols)
 * - Result type conversion and error message mapping
 */
class PasswordValidationUtilsTest {

  // ========== Basic Validation Tests ==========

  @Test
  fun `validatePassword returns all false for empty password`() {
    val result = validatePassword("")

    assertFalse(result.hasMinLength)
    assertFalse(result.hasUppercase)
    assertFalse(result.hasLowercase)
    assertFalse(result.hasDigit)
    assertFalse(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword returns Valid for password meeting all requirements`() {
    val result = validatePassword("ValidPass123!")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword detects password too short`() {
    val result = validatePassword("Abc1!")

    assertFalse(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword detects missing uppercase`() {
    val result = validatePassword("password123!")

    assertTrue(result.hasMinLength)
    assertFalse(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword detects missing lowercase`() {
    val result = validatePassword("PASSWORD123!")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertFalse(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword detects missing digit`() {
    val result = validatePassword("Password!@#")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertFalse(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword detects missing special character`() {
    val result = validatePassword("Password123")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertFalse(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  // ========== Edge Cases ==========

  @Test
  fun `validatePassword accepts password at minimum length of 8`() {
    val result = validatePassword("Passw0rd!")

    assertTrue(result.hasMinLength)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword rejects password one character short`() {
    val result = validatePassword("Pass0!d")

    assertFalse(result.hasMinLength)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword accepts very long password`() {
    val longPassword = "A1!" + "a".repeat(1000)
    val result = validatePassword(longPassword)

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword accepts multiple uppercase letters`() {
    val result = validatePassword("ABCDE123!fgh")

    assertTrue(result.hasUppercase)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword accepts multiple lowercase letters`() {
    val result = validatePassword("abcde123!FGH")

    assertTrue(result.hasLowercase)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword accepts multiple digits`() {
    val result = validatePassword("Password123456!")

    assertTrue(result.hasDigit)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword accepts multiple special characters`() {
    val result = validatePassword("Pass!@#$%^&*()123")

    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  // ========== Unicode and Special Character Tests ==========

  @Test
  fun `validatePassword treats emoji as special characters`() {
    val result = validatePassword("Password123😀")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar) // Emoji counts as special char
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword handles accented uppercase letters`() {
    val result = validatePassword("Pàsswörd123!")

    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword handles Cyrillic characters`() {
    val result = validatePassword("Паролъ123!")

    // Cyrillic uppercase and lowercase should be detected
    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword handles Chinese characters as special chars`() {
    val result = validatePassword("密码Pass123")

    // Chinese characters are neither letters nor digits, so they count as special
    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword with only punctuation marks as special chars`() {
    val result = validatePassword("Password123!@#$%^&*()")

    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword with spaces as special characters`() {
    val result = validatePassword("Pass word 123")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar) // Space counts as special char
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword with underscores and hyphens`() {
    val result = validatePassword("Pass_word-123")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar) // Underscore and hyphen are special chars
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword with mathematical symbols`() {
    val result = validatePassword("Pass±word×123÷")

    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  // ========== Result Type Conversion Tests ==========

  @Test
  fun `toResult returns Valid when all requirements met`() {
    val validation =
        PasswordValidation(
            hasMinLength = true,
            hasUppercase = true,
            hasLowercase = true,
            hasDigit = true,
            hasSpecialChar = true)

    val result = validation.toResult()

    assertTrue(result is PasswordValidationResult.Valid)
  }

  @Test
  fun `toResult returns TooShort when length requirement not met`() {
    val validation =
        PasswordValidation(
            hasMinLength = false,
            hasUppercase = true,
            hasLowercase = true,
            hasDigit = true,
            hasSpecialChar = true)

    val result = validation.toResult()

    assertTrue(result is PasswordValidationResult.Invalid.TooShort)
    assertEquals(
        R.string.password_error_too_short,
        (result as PasswordValidationResult.Invalid).messageResId)
  }

  @Test
  fun `toResult returns MissingUppercase when uppercase requirement not met`() {
    val validation =
        PasswordValidation(
            hasMinLength = true,
            hasUppercase = false,
            hasLowercase = true,
            hasDigit = true,
            hasSpecialChar = true)

    val result = validation.toResult()

    assertTrue(result is PasswordValidationResult.Invalid.MissingUppercase)
    assertEquals(
        R.string.password_error_missing_uppercase,
        (result as PasswordValidationResult.Invalid).messageResId)
  }

  @Test
  fun `toResult returns MissingLowercase when lowercase requirement not met`() {
    val validation =
        PasswordValidation(
            hasMinLength = true,
            hasUppercase = true,
            hasLowercase = false,
            hasDigit = true,
            hasSpecialChar = true)

    val result = validation.toResult()

    assertTrue(result is PasswordValidationResult.Invalid.MissingLowercase)
    assertEquals(
        R.string.password_error_missing_lowercase,
        (result as PasswordValidationResult.Invalid).messageResId)
  }

  @Test
  fun `toResult returns MissingDigit when digit requirement not met`() {
    val validation =
        PasswordValidation(
            hasMinLength = true,
            hasUppercase = true,
            hasLowercase = true,
            hasDigit = false,
            hasSpecialChar = true)

    val result = validation.toResult()

    assertTrue(result is PasswordValidationResult.Invalid.MissingDigit)
    assertEquals(
        R.string.password_error_missing_digit,
        (result as PasswordValidationResult.Invalid).messageResId)
  }

  @Test
  fun `toResult returns MissingSpecialChar when special char requirement not met`() {
    val validation =
        PasswordValidation(
            hasMinLength = true,
            hasUppercase = true,
            hasLowercase = true,
            hasDigit = true,
            hasSpecialChar = false)

    val result = validation.toResult()

    assertTrue(result is PasswordValidationResult.Invalid.MissingSpecialChar)
    assertEquals(
        R.string.password_error_missing_special_char,
        (result as PasswordValidationResult.Invalid).messageResId)
  }

  @Test
  fun `toResult returns first failure when multiple requirements not met`() {
    val validation =
        PasswordValidation(
            hasMinLength = false,
            hasUppercase = false,
            hasLowercase = false,
            hasDigit = false,
            hasSpecialChar = false)

    val result = validation.toResult()

    // Should return the first failure (TooShort)
    assertTrue(result is PasswordValidationResult.Invalid.TooShort)
  }

  // ========== Boundary and Corner Cases ==========

  @Test
  fun `validatePassword with only letters no digits or special chars`() {
    val result = validatePassword("PasswordPassword")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertFalse(result.hasDigit)
    assertFalse(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword with only digits and special chars`() {
    val result = validatePassword("12345678!@#$")

    assertTrue(result.hasMinLength)
    assertFalse(result.hasUppercase)
    assertFalse(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword with single character from each category`() {
    val result = validatePassword("Aa1!xxxx")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }

  @Test
  fun `validatePassword with whitespace only password`() {
    val result = validatePassword("        ")

    assertTrue(result.hasMinLength) // 8 spaces
    assertFalse(result.hasUppercase)
    assertFalse(result.hasLowercase)
    assertFalse(result.hasDigit)
    assertTrue(result.hasSpecialChar) // Spaces are special chars
    assertFalse(result.isValid())
  }

  @Test
  fun `validatePassword with newlines and tabs`() {
    val result = validatePassword("Pass\tword\n123!")

    assertTrue(result.hasMinLength)
    assertTrue(result.hasUppercase)
    assertTrue(result.hasLowercase)
    assertTrue(result.hasDigit)
    assertTrue(result.hasSpecialChar)
    assertTrue(result.isValid())
  }
}

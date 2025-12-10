package com.swent.mapin.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthErrorMapper].
 *
 * Tests cover all error mapping scenarios including:
 * - Sign-in error mappings
 * - Sign-up error mappings
 * - Case-insensitive matching
 * - New error cases (network, rate limiting, account disabled)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthErrorMapperTest {

  // ========== Sign-In Error Mapping Tests ==========

  @Test
  fun `mapSignInException should map no user record to NoAccountFound`() {
    val exception = Exception("There is no user record corresponding to this identifier")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.NoAccountFound, result)
  }

  @Test
  fun `mapSignInException should map user not found to NoAccountFound`() {
    val exception = Exception("user not found")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.NoAccountFound, result)
  }

  @Test
  fun `mapSignInException should map invalid password to IncorrectCredentials`() {
    val exception = Exception("The password is invalid")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.IncorrectCredentials, result)
  }

  @Test
  fun `mapSignInException should map wrong password to IncorrectCredentials`() {
    val exception = Exception("Wrong password")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.IncorrectCredentials, result)
  }

  @Test
  fun `mapSignInException should map credential is incorrect to IncorrectCredentials`() {
    val exception = Exception("The supplied auth credential is incorrect")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.IncorrectCredentials, result)
  }

  @Test
  fun `mapSignInException should map credential is malformed to IncorrectCredentials`() {
    val exception = Exception("The credential is malformed or has expired")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.IncorrectCredentials, result)
  }

  @Test
  fun `mapSignInException should map auth credential error to IncorrectCredentials`() {
    val exception = Exception("The auth credential is invalid")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.IncorrectCredentials, result)
  }

  @Test
  fun `mapSignInException should map invalid credential to IncorrectCredentials`() {
    val exception = Exception("Invalid credential provided")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.IncorrectCredentials, result)
  }

  @Test
  fun `mapSignInException should map badly formatted email to InvalidEmailFormat`() {
    val exception = Exception("The email address is badly formatted")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.InvalidEmailFormat, result)
  }

  @Test
  fun `mapSignInException should map invalid email to InvalidEmailFormat`() {
    val exception = Exception("Invalid email address")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.InvalidEmailFormat, result)
  }

  @Test
  fun `mapSignInException should map network error to NetworkError`() {
    val exception = Exception("A network error has occurred")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.NetworkError, result)
  }

  @Test
  fun `mapSignInException should map NETWORK_ERROR to NetworkError`() {
    val exception = Exception("NETWORK_ERROR: Connection failed")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.NetworkError, result)
  }

  @Test
  fun `mapSignInException should map unable to resolve host to NetworkError`() {
    val exception = Exception("Unable to resolve host firebase.google.com")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.NetworkError, result)
  }

  @Test
  fun `mapSignInException should map failed to connect to NetworkError`() {
    val exception = Exception("Failed to connect to server")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.NetworkError, result)
  }

  @Test
  fun `mapSignInException should map too many requests to TooManyRequests`() {
    val exception =
        Exception("We have blocked all requests from this device due to too many requests")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.TooManyRequests, result)
  }

  @Test
  fun `mapSignInException should map blocked all requests to TooManyRequests`() {
    val exception = Exception("We have blocked all requests from this device")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.TooManyRequests, result)
  }

  @Test
  fun `mapSignInException should map unusual activity to TooManyRequests`() {
    val exception = Exception("Unusual activity detected")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.TooManyRequests, result)
  }

  @Test
  fun `mapSignInException should map user disabled to AccountDisabled`() {
    val exception = Exception("The user has been disabled")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.AccountDisabled, result)
  }

  @Test
  fun `mapSignInException should map account disabled to AccountDisabled`() {
    val exception = Exception("This account has been disabled by an administrator")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.AccountDisabled, result)
  }

  @Test
  fun `mapSignInException should return SignInFailed for unknown errors`() {
    val exception = Exception("Some unknown error")
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.SignInFailed, result)
  }

  @Test
  fun `mapSignInException should return SignInFailed for null message`() {
    val exception = Exception(null as String?)
    val result = AuthErrorMapper.mapSignInException(exception)
    assertEquals(AuthError.SignInFailed, result)
  }

  @Test
  fun `mapSignInException should be case insensitive`() {
    // Test uppercase
    val upperException = Exception("THE PASSWORD IS INVALID")
    assertEquals(AuthError.IncorrectCredentials, AuthErrorMapper.mapSignInException(upperException))

    // Test mixed case
    val mixedException = Exception("Network Error Occurred")
    assertEquals(AuthError.NetworkError, AuthErrorMapper.mapSignInException(mixedException))

    // Test lowercase
    val lowerException = Exception("user has been disabled")
    assertEquals(AuthError.AccountDisabled, AuthErrorMapper.mapSignInException(lowerException))
  }

  // ========== Sign-Up Error Mapping Tests ==========

  @Test
  fun `mapSignUpException should map email already in use to EmailAlreadyInUse`() {
    val exception = Exception("The email address is already in use by another account")
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertEquals(AuthError.EmailAlreadyInUse, result)
  }

  @Test
  fun `mapSignUpException should map account already exists to EmailAlreadyInUse`() {
    val exception = Exception("An account already exists with this email")
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertEquals(AuthError.EmailAlreadyInUse, result)
  }

  @Test
  fun `mapSignUpException should map badly formatted to InvalidEmailFormat`() {
    val exception = Exception("The email address is badly formatted")
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertEquals(AuthError.InvalidEmailFormat, result)
  }

  @Test
  fun `mapSignUpException should map weak password to WeakPassword`() {
    val exception = Exception("The password is too weak")
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertEquals(AuthError.WeakPassword, result)
  }

  @Test
  fun `mapSignUpException should map password should be to WeakPassword`() {
    val exception = Exception("Password should be at least 6 characters")
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertEquals(AuthError.WeakPassword, result)
  }

  @Test
  fun `mapSignUpException should map network error to NetworkError`() {
    val exception = Exception("A network error has occurred")
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertEquals(AuthError.NetworkError, result)
  }

  @Test
  fun `mapSignUpException should map too many requests to TooManyRequests`() {
    val exception = Exception("Too many requests. Try again later")
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertEquals(AuthError.TooManyRequests, result)
  }

  @Test
  fun `mapSignUpException should return SignUpFailed with message for unknown errors`() {
    val errorMessage = "Some unknown signup error"
    val exception = Exception(errorMessage)
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertTrue(result is AuthError.SignUpFailed)
    assertEquals(errorMessage, (result as AuthError.SignUpFailed).message)
  }

  @Test
  fun `mapSignUpException should return SignUpFailed with null for null message`() {
    val exception = Exception(null as String?)
    val result = AuthErrorMapper.mapSignUpException(exception)
    assertTrue(result is AuthError.SignUpFailed)
    assertEquals(null, (result as AuthError.SignUpFailed).message)
  }

  @Test
  fun `mapSignUpException should be case insensitive`() {
    // Test uppercase
    val upperException = Exception("EMAIL ADDRESS IS ALREADY IN USE")
    assertEquals(AuthError.EmailAlreadyInUse, AuthErrorMapper.mapSignUpException(upperException))

    // Test mixed case
    val mixedException = Exception("The Password Is Too Weak")
    assertEquals(AuthError.WeakPassword, AuthErrorMapper.mapSignUpException(mixedException))
  }
}

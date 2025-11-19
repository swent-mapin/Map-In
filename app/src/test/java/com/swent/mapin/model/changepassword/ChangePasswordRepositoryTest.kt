// Assisted by AI
package com.swent.mapin.model.changepassword

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordRepositoryTest {

  private lateinit var repository: ChangePasswordRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockAuthResult: AuthResult

  private val testEmail = "test@example.com"
  private val testCurrentPassword = "oldPassword123"
  private val testNewPassword = "newPassword456"

  @Before
  fun setup() {
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockAuthResult = mockk(relaxed = true)

    repository = ChangePasswordRepositoryFirebase(mockAuth)
  }

  @Test
  fun `changePassword succeeds with valid credentials`() = runTest {
    // Setup: user is authenticated with email
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.email } returns testEmail

    // Mock successful re-authentication
    every { mockUser.reauthenticate(any()) } returns
        Tasks.forResult(null) as com.google.android.gms.tasks.Task<Void>

    // Mock successful password update
    every { mockUser.updatePassword(testNewPassword) } returns
        Tasks.forResult(null) as com.google.android.gms.tasks.Task<Void>

    // Execute
    val result = repository.changePassword(testCurrentPassword, testNewPassword)

    // Verify
    assertTrue(result is PasswordChangeResult.Success)
    verify { mockUser.reauthenticate(any()) }
    verify { mockUser.updatePassword(testNewPassword) }
  }

  @Test
  fun `changePassword returns UserNotAuthenticated when no user is logged in`() = runTest {
    // Setup: no user authenticated
    every { mockAuth.currentUser } returns null

    // Execute
    val result = repository.changePassword(testCurrentPassword, testNewPassword)

    // Verify
    assertTrue(result is PasswordChangeResult.UserNotAuthenticated)
    verify(exactly = 0) { mockUser.reauthenticate(any()) }
    verify(exactly = 0) { mockUser.updatePassword(any()) }
  }

  @Test
  fun `changePassword returns Error when user has no email`() = runTest {
    // Setup: user authenticated but no email
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.email } returns null

    // Execute
    val result = repository.changePassword(testCurrentPassword, testNewPassword)

    // Verify
    assertTrue(result is PasswordChangeResult.Error)
    assertEquals("User email not found", (result as PasswordChangeResult.Error).message)
    verify(exactly = 0) { mockUser.reauthenticate(any()) }
  }

  @Test
  fun `changePassword returns InvalidCurrentPassword when re-authentication fails`() = runTest {
    // Setup
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.email } returns testEmail

    // Mock failed re-authentication with invalid credentials
    val exception = mockk<FirebaseAuthInvalidCredentialsException>()
    every { exception.message } returns "Invalid credentials"
    every { mockUser.reauthenticate(any()) } returns Tasks.forException(exception)

    // Execute
    val result = repository.changePassword(testCurrentPassword, testNewPassword)

    // Verify
    assertTrue(result is PasswordChangeResult.InvalidCurrentPassword)
    verify { mockUser.reauthenticate(any()) }
    verify(exactly = 0) { mockUser.updatePassword(any()) }
  }

  @Test
  fun `changePassword returns UserNotFound when user account doesn't exist`() = runTest {
    // Setup
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.email } returns testEmail

    // Mock re-authentication failure with invalid user exception
    val exception = mockk<FirebaseAuthInvalidUserException>("ERROR_USER_NOT_FOUND")
    every { mockUser.reauthenticate(any()) } returns Tasks.forException(exception)

    // Execute
    val result = repository.changePassword(testCurrentPassword, testNewPassword)

    // Verify
    assertTrue(result is PasswordChangeResult.UserNotFound)
    verify { mockUser.reauthenticate(any()) }
    verify(exactly = 0) { mockUser.updatePassword(any()) }
  }

  @Test
  fun `changePassword returns Error when updatePassword fails`() = runTest {
    // Setup
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.email } returns testEmail

    // Mock successful re-authentication
    every { mockUser.reauthenticate(any()) } returns
        Tasks.forResult(null) as com.google.android.gms.tasks.Task<Void>

    // Mock failed password update
    val exception = Exception("Network error")
    every { mockUser.updatePassword(testNewPassword) } returns Tasks.forException(exception)

    // Execute
    val result = repository.changePassword(testCurrentPassword, testNewPassword)

    // Verify
    assertTrue(result is PasswordChangeResult.Error)
    assertEquals("Network error", (result as PasswordChangeResult.Error).message)
    verify { mockUser.reauthenticate(any()) }
    verify { mockUser.updatePassword(testNewPassword) }
  }

  @Test
  fun `changePassword returns Error with default message when exception has no message`() =
      runTest {
        // Setup
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.email } returns testEmail

        // Mock successful re-authentication
        every { mockUser.reauthenticate(any()) } returns
            Tasks.forResult(null) as com.google.android.gms.tasks.Task<Void>

        // Mock failed password update with no message
        val exception = Exception()
        every { mockUser.updatePassword(testNewPassword) } returns Tasks.forException(exception)

        // Execute
        val result = repository.changePassword(testCurrentPassword, testNewPassword)

        // Verify
        assertTrue(result is PasswordChangeResult.Error)
        assertEquals("Failed to change password", (result as PasswordChangeResult.Error).message)
      }

  @Test
  fun `isEmailPasswordUser returns true when user has email password provider`() {
    // Setup: user with email/password provider
    val mockProviderData = mockk<UserInfo>()
    every { mockProviderData.providerId } returns "password"
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.providerData } returns listOf(mockProviderData)

    // Execute
    val result = repository.isEmailPasswordUser()

    // Verify
    assertTrue(result)
  }

  @Test
  fun `isEmailPasswordUser returns false when user has only OAuth providers`() {
    // Setup: user with Google provider only
    val mockProviderData = mockk<UserInfo>()
    every { mockProviderData.providerId } returns "google.com"
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.providerData } returns listOf(mockProviderData)

    // Execute
    val result = repository.isEmailPasswordUser()

    // Verify
    assertFalse(result)
  }

  @Test
  fun `isEmailPasswordUser returns false when no user is authenticated`() {
    // Setup: no user
    every { mockAuth.currentUser } returns null

    // Execute
    val result = repository.isEmailPasswordUser()

    // Verify
    assertFalse(result)
  }

  @Test
  fun `isEmailPasswordUser returns true when user has multiple providers including email`() {
    // Setup: user with both Google and email/password providers
    val googleProvider = mockk<UserInfo>()
    val emailProvider = mockk<UserInfo>()
    every { googleProvider.providerId } returns "google.com"
    every { emailProvider.providerId } returns "password"
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.providerData } returns listOf(googleProvider, emailProvider)

    // Execute
    val result = repository.isEmailPasswordUser()

    // Verify
    assertTrue(result)
  }

  @Test
  fun `getCurrentUser returns user when authenticated`() {
    // Setup
    every { mockAuth.currentUser } returns mockUser

    // Execute
    val result = repository.getCurrentUser()

    // Verify
    assertEquals(mockUser, result)
  }

  @Test
  fun `getCurrentUser returns null when not authenticated`() {
    // Setup
    every { mockAuth.currentUser } returns null

    // Execute
    val result = repository.getCurrentUser()

    // Verify
    assertNull(result)
  }
}

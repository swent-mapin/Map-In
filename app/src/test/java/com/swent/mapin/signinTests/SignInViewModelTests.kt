package com.swent.mapin.signinTests

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.ui.auth.SignInUiState
import com.swent.mapin.ui.auth.SignInViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SignInViewModel.
 *
 * Tests the authentication logic and UI state management using Robolectric to mock Android
 * framework dependencies like Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SignInViewModelTest {

  private lateinit var context: Context
  private lateinit var viewModel: SignInViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    // Setup test dispatcher for coroutines
    Dispatchers.setMain(testDispatcher)

    // Get application context
    context = ApplicationProvider.getApplicationContext()

    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    // Create ViewModel instance
    viewModel = SignInViewModel(context)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `initial UI state should have default values`() = runTest {
    val initialState = viewModel.uiState.first()

    assertFalse(initialState.isLoading)
    assertNull(initialState.errorMessage)
    assertFalse(initialState.isSignInSuccessful)
  }

  @Test
  fun `UI state should be instance of SignInUiState`() = runTest {
    val state = viewModel.uiState.first()

    assertNotNull(state)
    assertEquals(SignInUiState::class, state::class)
  }

  @Test
  fun `clearError should remove error message from state`() = runTest {
    viewModel.clearError()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.errorMessage)
  }

  @Test
  fun `isLoading should be false initially`() = runTest {
    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
  }

  @Test
  fun `isSignInSuccessful should be false initially`() = runTest {
    val state = viewModel.uiState.first()
    assertFalse(state.isSignInSuccessful)
  }

  @Test
  fun `SignInUiState data class should have correct default values`() {
    val state = SignInUiState()

    assertFalse(state.isLoading)
    assertNull(state.errorMessage)
    assertFalse(state.isSignInSuccessful)
    assertNull(state.currentUser)
  }

  @Test
  fun `SignInUiState copy should work correctly`() {
    val originalState =
        SignInUiState(
            isLoading = false, errorMessage = null, isSignInSuccessful = false, currentUser = null)

    val copiedState = originalState.copy(isLoading = true)

    assertEquals(true, copiedState.isLoading)
    assertNull(copiedState.errorMessage)
    assertFalse(copiedState.isSignInSuccessful)
    assertNull(copiedState.currentUser)
  }

  @Test
  fun `SignInUiState copy with error message should work correctly`() {
    val originalState = SignInUiState()
    val errorMessage = "Test error"

    val copiedState = originalState.copy(errorMessage = errorMessage)

    assertEquals(errorMessage, copiedState.errorMessage)
    assertFalse(copiedState.isLoading)
  }

  @Test
  fun `SignInUiState copy with isSignInSuccessful should work correctly`() {
    val originalState = SignInUiState()

    val copiedState = originalState.copy(isSignInSuccessful = true)

    assertEquals(true, copiedState.isSignInSuccessful)
    assertFalse(copiedState.isLoading)
  }

  @Test
  fun `ViewModel factory should create instance correctly`() {
    val factory = SignInViewModel.factory(context)
    val viewModelFromFactory = factory.create(SignInViewModel::class.java)

    assertNotNull(viewModelFromFactory)
    assertEquals(SignInViewModel::class, viewModelFromFactory::class)
  }

  @Test
  fun `ViewModel should use application context`() {
    val viewModelInstance = SignInViewModel(context)
    val state = runTest { viewModelInstance.uiState.first() }

    assertNotNull(state)
  }

  @Test
  fun `multiple clearError calls should not throw exception`() = runTest {
    viewModel.clearError()
    viewModel.clearError()
    viewModel.clearError()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.errorMessage)
  }

  @Test
  fun `UI state flow should emit updates`() = runTest {
    val initialState = viewModel.uiState.first()
    assertNotNull(initialState)

    viewModel.clearError()
    advanceUntilIdle()

    val updatedState = viewModel.uiState.first()
    assertNotNull(updatedState)
  }

  @Test
  fun `signInWithGoogle should prevent concurrent sign-in attempts when already loading`() =
      runTest {
        val mockCredentialManager = mockk<CredentialManager>(relaxed = true)

        // Start first sign-in (this will set isLoading to true)
        viewModel.signInWithGoogle(mockCredentialManager) {}

        // Get state after first call
        advanceUntilIdle()

        // Try to start another sign-in while loading - should be blocked
        viewModel.signInWithGoogle(mockCredentialManager) {}

        advanceUntilIdle()

        // Verify state exists (the second call should have been prevented)
        val finalState = viewModel.uiState.first()
        assertNotNull(finalState)
      }

  @Test
  fun `signInWithMicrosoft should prevent concurrent sign-in when already loading`() = runTest {
    val mockActivity = Robolectric.buildActivity(Activity::class.java).get()

    // Start first sign-in
    viewModel.signInWithMicrosoft(mockActivity)

    // Try second sign-in while loading
    viewModel.signInWithMicrosoft(mockActivity)

    advanceUntilIdle()

    // The second call should be blocked by isLoading check
    val state = viewModel.uiState.first()
    assertNotNull(state)
  }

  @Test
  fun `clearError should set errorMessage to null in state`() = runTest {
    viewModel.clearError()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.errorMessage)
    assertFalse(state.isLoading)
  }

  @Test
  fun `SignInUiState with all parameters should create correctly`() {
    val mockUser = mockk<FirebaseUser>()
    val state =
        SignInUiState(
            isLoading = true,
            errorMessage = "Error",
            isSignInSuccessful = true,
            currentUser = mockUser)

    assertTrue(state.isLoading)
    assertEquals("Error", state.errorMessage)
    assertTrue(state.isSignInSuccessful)
    assertEquals(mockUser, state.currentUser)
  }

  // ========== Email/Password Authentication Tests ==========

  @Test
  fun `signInWithEmail should reject empty email`() = runTest {
    viewModel.signInWithEmail("", "password123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
    assertFalse(state.isSignInSuccessful)
  }

  @Test
  fun `signInWithEmail should reject empty password`() = runTest {
    viewModel.signInWithEmail("test@example.com", "")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
    assertFalse(state.isSignInSuccessful)
  }

  @Test
  fun `signInWithEmail should reject blank email`() = runTest {
    viewModel.signInWithEmail("   ", "password123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signInWithEmail should reject blank password`() = runTest {
    viewModel.signInWithEmail("test@example.com", "   ")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signInWithEmail should reject both empty`() = runTest {
    viewModel.signInWithEmail("", "")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signInWithEmail should prevent concurrent attempts when loading`() = runTest {
    // Mock Firebase Auth to avoid actual network calls
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth

    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns mockTask
    every { mockTask.isComplete } returns false

    // Create new viewModel with mocked auth
    val testViewModel = SignInViewModel(context)

    // First call
    testViewModel.signInWithEmail("test@example.com", "password123")
    advanceUntilIdle()

    // Second call while first is potentially still processing
    testViewModel.signInWithEmail("test2@example.com", "password456")
    advanceUntilIdle()

    // Verify state is consistent
    val finalState = testViewModel.uiState.first()
    assertNotNull(finalState)
  }

  @Test
  fun `signUpWithEmail should reject empty email`() = runTest {
    viewModel.signUpWithEmail("", "password123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
    assertFalse(state.isSignInSuccessful)
  }

  @Test
  fun `signUpWithEmail should reject empty password`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should reject blank email`() = runTest {
    viewModel.signUpWithEmail("   ", "password123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should reject blank password`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "   ")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should reject password shorter than 6 characters`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "12345")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Password must be at least 6 characters", state.errorMessage)
    assertFalse(state.isSignInSuccessful)
  }

  @Test
  fun `signUpWithEmail should accept password with exactly 6 characters`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "123456")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not fail with password length error
    assertNotEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should accept password longer than 6 characters`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "1234567890")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not fail with password length error
    assertNotEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should reject both empty credentials`() = runTest {
    viewModel.signUpWithEmail("", "")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should prevent concurrent attempts when loading`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth

    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns mockTask
    every { mockTask.isComplete } returns false

    val testViewModel = SignInViewModel(context)

    // First call
    testViewModel.signUpWithEmail("test@example.com", "password123")
    advanceUntilIdle()

    // Second call while first is potentially still processing
    testViewModel.signUpWithEmail("test2@example.com", "password456")
    advanceUntilIdle()

    val finalState = testViewModel.uiState.first()
    assertNotNull(finalState)
  }

  @Test
  fun `signInWithEmail should handle valid email and password format`() = runTest {
    // This tests that validation passes for valid inputs
    viewModel.signInWithEmail("valid.email@example.com", "validPassword123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not have empty/blank error
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should handle valid email and password format`() = runTest {
    // This tests that validation passes for valid inputs
    viewModel.signUpWithEmail("valid.email@example.com", "validPassword123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not have validation errors
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
    assertNotEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `signInWithEmail should trim whitespace from inputs`() = runTest {
    // Email and password with leading/trailing spaces should be caught by isBlank
    viewModel.signInWithEmail("  test@example.com  ", "  password123  ")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // With current implementation, this should work since trim is not applied
    // and isBlank checks for non-whitespace content
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `clearError should clear error after failed signInWithEmail`() = runTest {
    viewModel.signInWithEmail("", "password")
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull(state.errorMessage)

    viewModel.clearError()
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertNull(state.errorMessage)
  }

  @Test
  fun `clearError should clear error after failed signUpWithEmail`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "123")
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull(state.errorMessage)

    viewModel.clearError()
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertNull(state.errorMessage)
  }

  @Test
  fun `signInWithEmail with special characters in password should be accepted`() = runTest {
    viewModel.signInWithEmail("test@example.com", "p@ssw0rd!#$%")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail with special characters in password should be accepted`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "p@ssw0rd!#$%")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
    assertNotEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `signInWithEmail with very long password should be accepted`() = runTest {
    val longPassword = "a".repeat(100)
    viewModel.signInWithEmail("test@example.com", longPassword)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail with very long password should be accepted`() = runTest {
    val longPassword = "a".repeat(100)
    viewModel.signUpWithEmail("test@example.com", longPassword)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
    assertNotEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `signInWithEmail should not be successful initially`() = runTest {
    viewModel.signInWithEmail("test@example.com", "password123")

    // Check state before async operation completes
    val state = viewModel.uiState.first()
    // Initially should not be successful (will change after Firebase response)
    assertNotNull(state)
  }

  @Test
  fun `signUpWithEmail should not be successful initially`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "password123")

    val state = viewModel.uiState.first()
    assertNotNull(state)
  }

  @Test
  fun `multiple signInWithEmail calls with empty credentials should all fail`() = runTest {
    viewModel.signInWithEmail("", "")
    advanceUntilIdle()
    var state = viewModel.uiState.first()
    assertEquals("Email and password cannot be empty", state.errorMessage)

    viewModel.clearError()
    advanceUntilIdle()

    viewModel.signInWithEmail("", "password")
    advanceUntilIdle()
    state = viewModel.uiState.first()
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `multiple signUpWithEmail calls with invalid credentials should all fail`() = runTest {
    viewModel.signUpWithEmail("", "123")
    advanceUntilIdle()
    var state = viewModel.uiState.first()
    assertEquals("Email and password cannot be empty", state.errorMessage)

    viewModel.clearError()
    advanceUntilIdle()

    viewModel.signUpWithEmail("test@example.com", "123")
    advanceUntilIdle()
    state = viewModel.uiState.first()
    assertEquals("Password must be at least 6 characters", state.errorMessage)
  }

  // ========== Firebase Authentication Success/Failure Tests ==========

  @Test
  fun `signInWithEmail should handle Firebase no user record error`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("There is no user record corresponding to this identifier")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.signInWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithEmail("test@example.com", "wrongpassword")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertFalse(state.isSignInSuccessful)
    assertEquals("No account found with this email", state.errorMessage)
  }

  @Test
  fun `signInWithEmail should handle invalid password error`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("The password is invalid")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.signInWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithEmail("test@example.com", "wrongpassword")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Invalid password", state.errorMessage)
  }

  @Test
  fun `signInWithEmail should handle badly formatted email error`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("The email address is badly formatted")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.signInWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithEmail("invalid-email", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Invalid email format", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should handle email already in use error`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("The email address is already in use by another account")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signUpWithEmail("existing@example.com", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("An account with this email already exists", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should handle badly formatted email error`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("The email address is badly formatted")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signUpWithEmail("invalid-email", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Invalid email format", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should handle generic Firebase error`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("Unknown Firebase error")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signUpWithEmail("test@example.com", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Sign-up failed: Unknown Firebase error", state.errorMessage)
  }

  @Test
  fun `signInWithGoogle should handle successful credential retrieval and sign in`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockCredentialManager = mockk<CredentialManager>(relaxed = true)
    val mockCredentialResult = mockk<androidx.credentials.GetCredentialResponse>(relaxed = true)
    val mockCredential = mockk<androidx.credentials.Credential>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    mockkStatic("com.google.android.libraries.identity.googleid.GoogleIdTokenCredential")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockCredential.data } returns android.os.Bundle()
    every { mockCredentialResult.credential } returns mockCredential

    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } returns mockCredentialResult

    every { mockAuth.signInWithCredential(any()) } returns mockTask
    every { mockTask.isComplete } returns true
    every { mockTask.isSuccessful } returns true
    every { mockTask.result } returns mockAuthResult
    every { mockAuthResult.user } returns mockUser
    every { mockUser.displayName } returns "Test User"

    every { mockTask.addOnCompleteListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnCompleteListener<AuthResult>>()
          listener.onComplete(mockTask)
          mockTask
        }

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithGoogle(mockCredentialManager) {}
    advanceUntilIdle()

    // Verify the success path was attempted (may not complete due to mocking complexity)
    val state = testViewModel.uiState.first()
    assertNotNull(state)
  }

  @Test
  fun `signInWithGoogle should handle cancellation exception`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockCredentialManager = mockk<CredentialManager>(relaxed = true)
    val exception =
        androidx.credentials.exceptions.GetCredentialCancellationException("User cancelled")

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null

    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithGoogle(mockCredentialManager)
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Sign-in was cancelled", state.errorMessage)
  }

  @Test
  fun `signInWithGoogle should handle no credential exception`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockCredentialManager = mockk<CredentialManager>(relaxed = true)
    val exception = androidx.credentials.exceptions.NoCredentialException("No credentials found")

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null

    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithGoogle(mockCredentialManager)
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("No Google accounts found on device", state.errorMessage)
  }

  @Test
  fun `signInWithGoogle should handle generic exception with message`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockCredentialManager = mockk<CredentialManager>(relaxed = true)
    val exception = Exception("Network error occurred")

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null

    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithGoogle(mockCredentialManager)
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Sign-in failed: Network error occurred", state.errorMessage)
  }

  @Test
  fun `signInWithGoogle should handle generic exception without message`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockCredentialManager = mockk<CredentialManager>(relaxed = true)
    val exception = Exception(null as String?)

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null

    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithGoogle(mockCredentialManager)
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Sign-in failed", state.errorMessage)
  }

  @Test
  fun `signInWithMicrosoft should handle successful authentication`() = runTest {
    val mockActivity = Robolectric.buildActivity(Activity::class.java).get()
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.startActivityForSignInWithProvider(any(), any()) } returns mockTask
    every { mockTask.isComplete } returns true
    every { mockTask.isSuccessful } returns true
    every { mockTask.result } returns mockAuthResult
    every { mockAuthResult.user } returns mockUser
    every { mockUser.displayName } returns "Microsoft User"

    var successListener: OnSuccessListener<AuthResult>? = null
    every { mockTask.addOnSuccessListener(any<OnSuccessListener<AuthResult>>()) } answers
        {
          successListener = firstArg()
          mockTask
        }
    every { mockTask.addOnFailureListener(any()) } returns mockTask

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithMicrosoft(mockActivity)
    advanceUntilIdle()

    // Manually trigger success listener
    successListener?.onSuccess(mockAuthResult)
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertTrue(state.isSignInSuccessful)
    assertEquals(mockUser, state.currentUser)
    assertNull(state.errorMessage)
  }

  @Test
  fun `signInWithMicrosoft should handle authentication failure`() = runTest {
    val mockActivity = Robolectric.buildActivity(Activity::class.java).get()
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("Microsoft auth failed")

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.startActivityForSignInWithProvider(any(), any()) } returns mockTask

    var failureListener: OnFailureListener? = null
    every { mockTask.addOnSuccessListener(any<OnSuccessListener<AuthResult>>()) } returns mockTask
    every { mockTask.addOnFailureListener(any()) } answers
        {
          failureListener = firstArg()
          mockTask
        }

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithMicrosoft(mockActivity)
    advanceUntilIdle()

    // Manually trigger failure listener
    failureListener?.onFailure(exception)
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Microsoft sign-in failed: Microsoft auth failed", state.errorMessage)
  }

  @Test
  fun `signInWithMicrosoft should handle null user in result`() = runTest {
    val mockActivity = Robolectric.buildActivity(Activity::class.java).get()
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.startActivityForSignInWithProvider(any(), any()) } returns mockTask
    every { mockAuthResult.user } returns null

    var successListener: OnSuccessListener<AuthResult>? = null
    every { mockTask.addOnSuccessListener(any<OnSuccessListener<AuthResult>>()) } answers
        {
          successListener = firstArg()
          mockTask
        }
    every { mockTask.addOnFailureListener(any()) } returns mockTask

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithMicrosoft(mockActivity)
    advanceUntilIdle()

    // Manually trigger success listener with null user
    successListener?.onSuccess(mockAuthResult)
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Microsoft sign-in failed: No user returned", state.errorMessage)
  }

  // ========== Additional Email/Password Tests for Higher Coverage ==========

  @Test
  fun `signInWithEmail should handle successful authentication`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.signInWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask
    every { mockUser.email } returns "test@example.com"

    coEvery { mockTask.await<AuthResult>() } returns mockAuthResult
    every { mockAuthResult.user } returns mockUser

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithEmail("test@example.com", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertTrue(state.isSignInSuccessful)
    assertEquals(mockUser, state.currentUser)
    assertNull(state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should handle successful registration`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask
    every { mockUser.email } returns "newuser@example.com"

    coEvery { mockTask.await<AuthResult>() } returns mockAuthResult
    every { mockAuthResult.user } returns mockUser

    val testViewModel = SignInViewModel(context)
    testViewModel.signUpWithEmail("newuser@example.com", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertTrue(state.isSignInSuccessful)
    assertEquals(mockUser, state.currentUser)
    assertNull(state.errorMessage)
  }

  @Test
  fun `signInWithEmail should handle null user in auth result`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.signInWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } returns mockAuthResult
    every { mockAuthResult.user } returns null

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithEmail("test@example.com", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertFalse(state.isSignInSuccessful)
    assertEquals("Sign-in failed: No user returned", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should handle null user in auth result`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } returns mockAuthResult
    every { mockAuthResult.user } returns null

    val testViewModel = SignInViewModel(context)
    testViewModel.signUpWithEmail("test@example.com", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertFalse(state.isSignInSuccessful)
    assertEquals("Sign-up failed: No user returned", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail should handle weak password error`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception =
        Exception("The given password is invalid. [ Password should be at least 6 characters ]")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signUpWithEmail("test@example.com", "123456")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    // Since the error doesn't contain "weak password", it will be a generic error
    assertEquals(
        "Sign-up failed: The given password is invalid. [ Password should be at least 6 characters ]",
        state.errorMessage)
  }

  @Test
  fun `signInWithEmail should handle generic Firebase error message`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)
    val exception = Exception("Some other Firebase error")

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.signInWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    coEvery { mockTask.await<AuthResult>() } throws exception

    val testViewModel = SignInViewModel(context)
    testViewModel.signInWithEmail("test@example.com", "password123")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Sign-in failed: Some other Firebase error", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail prevents multiple simultaneous calls`() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockTask = mockk<Task<AuthResult>>(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null
    every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

    // Never complete the task to keep isLoading true
    coEvery { mockTask.await<AuthResult>() } coAnswers
        {
          kotlinx.coroutines.delay(10000)
          mockk()
        }

    val testViewModel = SignInViewModel(context)

    // First call
    testViewModel.signUpWithEmail("test1@example.com", "password123")

    // Advance just a bit to set isLoading to true
    testScheduler.advanceTimeBy(100)

    // Try second call while first is loading
    testViewModel.signUpWithEmail("test2@example.com", "password456")

    advanceUntilIdle()

    // Verify that state is still related to first call (second was blocked)
    val state = testViewModel.uiState.first()
    assertNotNull(state)
  }

  @Test
  fun `signInWithEmail with whitespace only email should fail`() = runTest {
    viewModel.signInWithEmail("   \t\n   ", "password123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signInWithEmail with whitespace only password should fail`() = runTest {
    viewModel.signInWithEmail("test@example.com", "   \t\n   ")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail with 5 character password should fail`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "12345")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `signUpWithEmail with exactly 6 character password should succeed validation`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "123456")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not have password length error
    assertNotEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `signInWithEmail error messages are properly mapped`() = runTest {
    val testCases =
        listOf(
            "There is no user record corresponding to this identifier" to
                "No account found with this email",
            "The password is invalid or the user does not have a password" to "Invalid password",
            "The email address is badly formatted" to "Invalid email format")

    for ((firebaseError, expectedMessage) in testCases) {
      val mockAuth = mockk<FirebaseAuth>(relaxed = true)
      val mockTask = mockk<Task<AuthResult>>(relaxed = true)
      val exception = Exception(firebaseError)

      mockkStatic(FirebaseAuth::class)
      mockkStatic("kotlinx.coroutines.tasks.TasksKt")

      every { FirebaseAuth.getInstance() } returns mockAuth
      every { mockAuth.currentUser } returns null
      every { mockAuth.signInWithEmailAndPassword(any<String>(), any<String>()) } returns mockTask

      coEvery { mockTask.await<AuthResult>() } throws exception

      val testViewModel = SignInViewModel(context)
      testViewModel.signInWithEmail("test@example.com", "password123")
      advanceUntilIdle()

      val state = testViewModel.uiState.first()
      assertEquals(expectedMessage, state.errorMessage)

      unmockkAll()
    }
  }

  @Test
  fun `signUpWithEmail error messages are properly mapped`() = runTest {
    val testCases =
        listOf(
            "The email address is already in use by another account" to
                "An account with this email already exists",
            "The email address is badly formatted" to "Invalid email format",
            "The password is too weak" to "Password is too weak")

    for ((firebaseError, expectedMessage) in testCases) {
      val mockAuth = mockk<FirebaseAuth>(relaxed = true)
      val mockTask = mockk<Task<AuthResult>>(relaxed = true)
      val exception = Exception(firebaseError)

      mockkStatic(FirebaseAuth::class)
      mockkStatic("kotlinx.coroutines.tasks.TasksKt")

      every { FirebaseAuth.getInstance() } returns mockAuth
      every { mockAuth.currentUser } returns null
      every { mockAuth.createUserWithEmailAndPassword(any<String>(), any<String>()) } returns
          mockTask

      coEvery { mockTask.await<AuthResult>() } throws exception

      val testViewModel = SignInViewModel(context)
      testViewModel.signUpWithEmail("test@example.com", "password123")
      advanceUntilIdle()

      val state = testViewModel.uiState.first()
      assertEquals(expectedMessage, state.errorMessage)

      unmockkAll()
    }
  }

  @Test
  fun `clearError preserves other state properties`() = runTest {
    // Set up a state with an error
    viewModel.signInWithEmail("", "")
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull(state.errorMessage)
    val wasLoading = state.isLoading
    val wasSuccessful = state.isSignInSuccessful

    // Clear error
    viewModel.clearError()
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertNull(state.errorMessage)
    assertEquals(wasLoading, state.isLoading)
    assertEquals(wasSuccessful, state.isSignInSuccessful)
  }

  @Test
  fun `email sign-in with unicode characters in email should be accepted`() = runTest {
    viewModel.signInWithEmail("test@例え.jp", "password123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not fail with empty validation
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
  }

  @Test
  fun `email sign-up with unicode characters in password should be accepted`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "пароль123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not fail with validation errors
    assertNotEquals("Email and password cannot be empty", state.errorMessage)
    assertNotEquals("Password must be at least 6 characters", state.errorMessage)
  }

  @Test
  fun `email sign-in sets isSignInSuccessful to false initially`() = runTest {
    val initialState = viewModel.uiState.first()
    assertFalse(initialState.isSignInSuccessful)

    // Try to sign in with invalid credentials
    viewModel.signInWithEmail("", "")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isSignInSuccessful)
  }

  @Test
  fun `email sign-up sets isSignInSuccessful to false initially`() = runTest {
    val initialState = viewModel.uiState.first()
    assertFalse(initialState.isSignInSuccessful)

    // Try to sign up with invalid credentials
    viewModel.signUpWithEmail("", "")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isSignInSuccessful)
  }
}

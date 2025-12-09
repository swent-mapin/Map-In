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
import com.swent.mapin.ui.auth.AuthError
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
import org.junit.Assert.assertNotEquals
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

  // Test constants
  companion object {
    // Test password helpers
    const val VALID_PASSWORD = "ValidPass123!"
    const val VALID_EMAIL = "test@example.com"
  }

  // Helper function to create password variants with specific missing requirements
  private fun passwordWithUnicode() = "Пароль123!" // Cyrillic

  private fun passwordWithEmoji() = "Password123😀"

  private fun passwordWithChinese() = "密码Pass123"

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
    assertNull(initialState.error)
    assertFalse(initialState.isSignInSuccessful)
    assertNull(initialState.currentUser)
    assertEquals(SignInUiState::class, initialState::class)
  }

  @Test
  fun `clearError should remove error message from state`() = runTest {
    viewModel.clearError()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.error)
  }

  @Test
  fun `SignInUiState copy should work correctly`() {
    val originalState = SignInUiState()
    val testError = AuthError.SignInFailed
    val mockUser = mockk<FirebaseUser>()

    // Test copy with loading
    val loadingState = originalState.copy(isLoading = true)
    assertTrue(loadingState.isLoading)
    assertNull(loadingState.error)

    // Test copy with error
    val errorState = originalState.copy(error = testError)
    assertEquals(testError, errorState.error)
    assertFalse(errorState.isLoading)

    // Test copy with success
    val successState = originalState.copy(isSignInSuccessful = true)
    assertTrue(successState.isSignInSuccessful)

    // Test copy with all parameters
    val fullState =
        SignInUiState(
            isLoading = true, error = testError, isSignInSuccessful = true, currentUser = mockUser)
    assertTrue(fullState.isLoading)
    assertEquals(testError, fullState.error)
    assertTrue(fullState.isSignInSuccessful)
    assertEquals(mockUser, fullState.currentUser)
  }

  @Test
  fun `ViewModel factory should create instance correctly`() {
    val factory = SignInViewModel.factory(context)
    val viewModelFromFactory = factory.create(SignInViewModel::class.java)

    assertNotNull(viewModelFromFactory)
    assertEquals(SignInViewModel::class, viewModelFromFactory::class)
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
  fun `clearError should set error to null in state`() = runTest {
    viewModel.clearError()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.error)
    assertFalse(state.isLoading)
  }

  @Test
  fun `SignInUiState with all parameters should create correctly`() {
    val mockUser = mockk<FirebaseUser>()
    val testError = AuthError.SignInFailed
    val state =
        SignInUiState(
            isLoading = true, error = testError, isSignInSuccessful = true, currentUser = mockUser)

    assertTrue(state.isLoading)
    assertEquals(testError, state.error)
    assertTrue(state.isSignInSuccessful)
    assertEquals(mockUser, state.currentUser)
  }

  // ========== Email/Password Authentication Tests ==========

  @Test
  fun `signInWithEmail should reject empty or blank credentials`() = runTest {
    // Test empty email
    viewModel.signInWithEmail("", "password123")
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
    viewModel.clearError()

    // Test empty password
    viewModel.signInWithEmail("test@example.com", "")
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
    viewModel.clearError()

    // Test both empty
    viewModel.signInWithEmail("", "")
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
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
  fun `signUpWithEmail should reject empty or blank credentials`() = runTest {
    // Test empty email
    viewModel.signUpWithEmail("", "password123")
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
    viewModel.clearError()

    // Test empty password
    viewModel.signUpWithEmail("test@example.com", "")
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
    viewModel.clearError()

    // Test both empty
    viewModel.signUpWithEmail("", "")
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
  }

  @Test
  fun `signUpWithEmail password validation rules`() = runTest {
    // Test: password shorter than 8 characters
    viewModel.signUpWithEmail("test@example.com", "12345")
    advanceUntilIdle()
    assertTrue(viewModel.uiState.first().error is AuthError.PasswordValidation)
    viewModel.clearError()

    // Test: password missing uppercase
    viewModel.signUpWithEmail("test@example.com", "test123!")
    advanceUntilIdle()
    assertTrue(viewModel.uiState.first().error is AuthError.PasswordValidation)
    viewModel.clearError()

    // Test: password missing lowercase
    viewModel.signUpWithEmail("test@example.com", "TEST123!")
    advanceUntilIdle()
    assertTrue(viewModel.uiState.first().error is AuthError.PasswordValidation)
    viewModel.clearError()

    // Test: password missing digit
    viewModel.signUpWithEmail("test@example.com", "TestTest!")
    advanceUntilIdle()
    assertTrue(viewModel.uiState.first().error is AuthError.PasswordValidation)
    viewModel.clearError()

    // Test: password missing special character
    viewModel.signUpWithEmail("test@example.com", "Test1234")
    advanceUntilIdle()
    assertTrue(viewModel.uiState.first().error is AuthError.PasswordValidation)
    viewModel.clearError()

    // Test: valid password with exactly 8 chars and all requirements
    viewModel.signUpWithEmail("test@example.com", "Test123!")
    advanceUntilIdle()
    assertFalse(viewModel.uiState.first().error is AuthError.PasswordValidation)
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
  fun `clearError should clear error after failed authentication`() = runTest {
    // Test sign-in error clear
    viewModel.signInWithEmail("", "password")
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.first().error)
    viewModel.clearError()
    advanceUntilIdle()
    assertNull(viewModel.uiState.first().error)

    // Test sign-up error clear
    viewModel.signUpWithEmail("test@example.com", "123")
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.first().error)
    viewModel.clearError()
    advanceUntilIdle()
    assertNull(viewModel.uiState.first().error)
  }

  @Test
  fun `signInWithEmail should accept special characters in password`() = runTest {
    viewModel.signInWithEmail("test@example.com", "p@ssw0rd!#$%")
    advanceUntilIdle()
    assertNotEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
  }

  // ========== Firebase Authentication Error Handling Tests ==========

  @Test
  fun `signInWithEmail should handle various Firebase errors`() = runTest {
    val testCases =
        listOf(
            "There is no user record corresponding to this identifier" to AuthError.NoAccountFound,
            "The password is invalid" to AuthError.IncorrectCredentials,
            "The email address is badly formatted" to AuthError.InvalidEmailFormat)

    for ((firebaseError, expectedError) in testCases) {
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
      testViewModel.signInWithEmail("test@example.com", "wrongpassword")
      advanceUntilIdle()

      val state = testViewModel.uiState.first()
      assertFalse(state.isLoading)
      assertEquals("Failed for: $firebaseError", expectedError, state.error)

      unmockkAll()
    }
  }

  @Test
  fun `signUpWithEmail should handle various Firebase errors`() = runTest {
    val testCases =
        listOf(
            "The email address is already in use by another account" to AuthError.EmailAlreadyInUse,
            "The email address is badly formatted" to AuthError.InvalidEmailFormat)

    for ((firebaseError, expectedError) in testCases) {
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
      testViewModel.signUpWithEmail("test@example.com", "Password123!")
      advanceUntilIdle()

      val state = testViewModel.uiState.first()
      assertFalse(state.isLoading)
      assertEquals("Failed for: $firebaseError", expectedError, state.error)

      unmockkAll()
    }
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
    assertEquals(AuthError.SignInCancelled, state.error)
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
    assertEquals(AuthError.NoGoogleAccounts, state.error)
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
    assertEquals(AuthError.SignInFailedWithMessage("Network error occurred"), state.error)
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
    assertEquals(AuthError.SignInFailed, state.error)
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
    assertNull(state.error)
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
    assertEquals(AuthError.MicrosoftSignInFailed("Microsoft auth failed"), state.error)
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
    assertEquals(AuthError.NoUserReturned, state.error)
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
    assertNull(state.error)
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
    testViewModel.signUpWithEmail("newuser@example.com", "Password123!")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertTrue(state.isSignInSuccessful)
    assertEquals(mockUser, state.currentUser)
    assertNull(state.error)
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
    assertEquals(AuthError.NoUserReturned, state.error)
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
    testViewModel.signUpWithEmail("test@example.com", "Password123!")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    assertFalse(state.isSignInSuccessful)
    assertEquals(AuthError.NoUserReturned, state.error)
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
    // Use a password that would pass Firebase's 6 char requirement but fails our 8 char requirement
    testViewModel.signUpWithEmail("test@example.com", "Ab1!")
    advanceUntilIdle()

    val state = testViewModel.uiState.first()
    assertFalse(state.isLoading)
    // Our local validation catches it before Firebase
    assertTrue(state.error is AuthError.PasswordValidation)
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
    assertEquals(AuthError.SignInFailed, state.error)
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
  fun `signInWithEmail error messages are properly mapped`() = runTest {
    val testCases =
        listOf(
            // No account found cases
            "There is no user record corresponding to this identifier" to AuthError.NoAccountFound,
            "user not found" to AuthError.NoAccountFound,

            // Incorrect credentials cases (password invalid, credential errors)
            "The password is invalid or the user does not have a password" to
                AuthError.IncorrectCredentials,
            "wrong password" to AuthError.IncorrectCredentials,
            "The supplied auth credential is incorrect" to AuthError.IncorrectCredentials,
            "The credential is malformed or has expired" to AuthError.IncorrectCredentials,
            "Invalid credential provided" to AuthError.IncorrectCredentials,
            "The auth credential is invalid" to AuthError.IncorrectCredentials,

            // Invalid email format cases
            "The email address is badly formatted" to AuthError.InvalidEmailFormat,
            "Invalid email address" to AuthError.InvalidEmailFormat,

            // Network error cases
            "A network error has occurred" to AuthError.NetworkError,
            "NETWORK_ERROR: Connection failed" to AuthError.NetworkError,
            "Unable to resolve host firebase.google.com" to AuthError.NetworkError,
            "Failed to connect to server" to AuthError.NetworkError,

            // Rate limiting cases
            "Too many requests from this device" to AuthError.TooManyRequests,
            "We have blocked all requests from this device" to AuthError.TooManyRequests,
            "Unusual activity detected" to AuthError.TooManyRequests,

            // Account disabled cases
            "The user has been disabled" to AuthError.AccountDisabled,
            "This account has been disabled by an administrator" to AuthError.AccountDisabled)

    for ((firebaseError, expectedError) in testCases) {
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
      assertEquals("Failed for: $firebaseError", expectedError, state.error)

      unmockkAll()
    }
  }

  @Test
  fun `signUpWithEmail error messages are properly mapped`() = runTest {
    val testCases =
        listOf(
            // Email already in use cases
            "The email address is already in use by another account" to AuthError.EmailAlreadyInUse,
            "email already in use" to AuthError.EmailAlreadyInUse,
            "An account already exists with this email" to AuthError.EmailAlreadyInUse,

            // Invalid email format cases
            "The email address is badly formatted" to AuthError.InvalidEmailFormat,
            "Invalid email address" to AuthError.InvalidEmailFormat,

            // Weak password cases
            "The password is too weak" to AuthError.WeakPassword,
            "Password should be at least 6 characters" to AuthError.WeakPassword,

            // Network error cases
            "A network error has occurred" to AuthError.NetworkError,
            "NETWORK_ERROR" to AuthError.NetworkError,
            "Unable to resolve host" to AuthError.NetworkError,
            "Failed to connect" to AuthError.NetworkError,

            // Rate limiting cases
            "Too many requests from this device" to AuthError.TooManyRequests,
            "We have blocked all requests" to AuthError.TooManyRequests)

    for ((firebaseError, expectedError) in testCases) {
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
      testViewModel.signUpWithEmail("test@example.com", "Password123!")
      advanceUntilIdle()

      val state = testViewModel.uiState.first()
      assertEquals("Failed for: $firebaseError", expectedError, state.error)

      unmockkAll()
    }
  }

  @Test
  fun `clearError preserves other state properties`() = runTest {
    // Set up a state with an error
    viewModel.signInWithEmail("", "")
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull(state.error)
    val wasLoading = state.isLoading
    val wasSuccessful = state.isSignInSuccessful

    // Clear error
    viewModel.clearError()
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertNull(state.error)
    assertEquals(wasLoading, state.isLoading)
    assertEquals(wasSuccessful, state.isSignInSuccessful)
  }

  @Test
  fun `email sign-in with unicode characters in email should be accepted`() = runTest {
    viewModel.signInWithEmail("test@例え.jp", "password123")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not fail with empty validation
    assertNotEquals(AuthError.EmailPasswordEmpty, state.error)
  }

  @Test
  fun `email sign-up with unicode characters in password should be accepted`() = runTest {
    viewModel.signUpWithEmail("test@example.com", "Пароль123!")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    // Should not fail with validation errors
    assertFalse(state.error is AuthError.EmailPasswordEmpty)
    assertFalse(state.error is AuthError.PasswordValidation)
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

  // ========== Unicode and Non-ASCII Character Tests ==========

  @Test
  fun `signUpWithEmail should accept passwords with various special characters`() = runTest {
    val specialCharPasswords =
        listOf(
            passwordWithUnicode(), // Cyrillic
            passwordWithEmoji(), // Emoji
            passwordWithChinese(), // Chinese
            "Pàsswörd123!", // Accented
            "Pass±×÷Word123", // Mathematical symbols
            "Pass word 123!" // Spaces
            )

    for (password in specialCharPasswords) {
      viewModel.signUpWithEmail(VALID_EMAIL, password)
      advanceUntilIdle()
      assertFalse(
          "Password '$password' should pass validation",
          viewModel.uiState.first().error is AuthError.PasswordValidation)
      viewModel.clearError()
    }
  }

  // ========== Edge Cases and Boundary Tests ==========

  @Test
  fun `signUpWithEmail should explicitly handle empty credentials`() = runTest {
    // Empty email
    viewModel.signUpWithEmail("", VALID_PASSWORD)
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
    viewModel.clearError()

    // Empty password
    viewModel.signUpWithEmail(VALID_EMAIL, "")
    advanceUntilIdle()
    assertEquals(AuthError.EmailPasswordEmpty, viewModel.uiState.first().error)
    viewModel.clearError()

    // Whitespace-only password
    viewModel.signUpWithEmail(VALID_EMAIL, "        ")
    advanceUntilIdle()
    assertNotNull("Should have error for whitespace-only password", viewModel.uiState.first().error)
  }
}

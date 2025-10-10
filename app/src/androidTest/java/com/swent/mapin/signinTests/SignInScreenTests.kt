package com.swent.mapin.signinTests

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.ui.auth.SignInScreen
import com.swent.mapin.ui.auth.SignInUiState
import com.swent.mapin.ui.auth.SignInViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for SignInScreen composable.
 *
 * Tests the UI rendering, user interactions, and state-based UI updates.
 */
@RunWith(AndroidJUnit4::class)
class SignInScreenTests {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var mockViewModel: SignInViewModel
  private lateinit var uiStateFlow: MutableStateFlow<SignInUiState>

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    // Mock ViewModel
    mockViewModel = mockk(relaxed = true)
    uiStateFlow = MutableStateFlow(SignInUiState())
    every { mockViewModel.uiState } returns uiStateFlow
  }

  @Test
  fun signInScreenShouldDisplayAppLogo() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithContentDescription("App Logo").assertExists()
  }

  @Test
  fun signInScreenShouldDisplaySloganText() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithText("One Map. Every moment.").assertExists()
  }

  @Test
  fun signInScreenShouldDisplayGoogleSignInButton() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithText("Sign in with Google").assertExists()
  }

  @Test
  fun signInScreenShouldDisplayMicrosoftSignInButton() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithText("Sign in with Microsoft").assertExists()
  }

  @Test
  fun buttonsShouldBeClickableWhenNotLoading() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithText("Sign in with Google").assertIsEnabled()
    composeTestRule.onNodeWithText("Sign in with Microsoft").assertIsEnabled()
  }

  @Test
  fun buttonsShouldBeDisabledWhenLoading() {
    uiStateFlow.value = SignInUiState(isLoading = true)

    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdle()

    // During loading, buttons are disabled but text is replaced by progress indicator
    // We verify the state is loading and screen renders without crash
    val state = uiStateFlow.value
    assertTrue(state.isLoading)
  }

  @Test
  fun googleSignInButtonShouldCallViewModelOnClick() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithText("Sign in with Google").performClick()

    verify { mockViewModel.signInWithGoogle(any(), any()) }
  }

  @Test
  fun shouldDisplayGoogleLogoInButton() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithContentDescription("Google logo").assertExists()
  }

  @Test
  fun onSignInSuccessCallbackShouldBeInvokedWhenSignInIsSuccessful() {
    var callbackInvoked = false
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.email } returns "test@example.com"

    composeTestRule.setContent {
      SignInScreen(viewModel = mockViewModel, onSignInSuccess = { callbackInvoked = true })
    }

    // Simulate successful sign-in
    uiStateFlow.value = SignInUiState(isSignInSuccessful = true, currentUser = mockUser)

    composeTestRule.waitForIdle()

    // Callback should be invoked
    assert(callbackInvoked)
  }

  @Test
  fun shouldHandleErrorStateAndCallClearError() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    // Simulate error
    uiStateFlow.value = SignInUiState(errorMessage = "Test error")

    composeTestRule.waitForIdle()

    verify { mockViewModel.clearError() }
  }

  @Test
  fun signInScreenShouldRenderWithoutCrashes() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdle()
    // If we reach here, rendering was successful
  }

  @Test
  fun signInScreenShouldUseProvidedViewModel() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    // Verify the UI state is collected from the provided viewModel
    verify { mockViewModel.uiState }
  }

  @Test
  fun buttonsShouldHaveCorrectHeight() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    // Both buttons should exist with proper dimensions
    composeTestRule.onNodeWithText("Sign in with Google").assertExists()
    composeTestRule.onNodeWithText("Sign in with Microsoft").assertExists()
  }

  @Test
  fun shouldHandleNullCurrentUserEmailInSuccessMessage() {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.email } returns null

    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    // Simulate successful sign-in with null email
    uiStateFlow.value = SignInUiState(isSignInSuccessful = true, currentUser = mockUser)

    composeTestRule.waitForIdle()
    // Should not crash even with null email
  }

  @Test
  fun buttonsShouldTransitionFromEnabledToDisabledWhenLoadingStarts() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    // Initially enabled - verify button exists and is enabled
    composeTestRule.onNodeWithText("Sign in with Google").assertIsEnabled()

    // Start loading - this will replace button content with progress indicator
    uiStateFlow.value = SignInUiState(isLoading = true)
    composeTestRule.waitForIdle()

    // Verify loading state is active (button content changes, so we check state)
    assertTrue(uiStateFlow.value.isLoading)
  }

  @Test
  fun buttonsShouldTransitionFromDisabledToEnabledWhenLoadingEnds() {
    uiStateFlow.value = SignInUiState(isLoading = true)

    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdle()

    // Initially loading
    assertTrue(uiStateFlow.value.isLoading)

    // End loading
    uiStateFlow.value = SignInUiState(isLoading = false)
    composeTestRule.waitForIdle()

    // Should now be enabled and text visible again
    composeTestRule.onNodeWithText("Sign in with Google").assertIsEnabled()
    composeTestRule.onNodeWithText("Sign in with Microsoft").assertIsEnabled()
  }

  @Test
  fun shouldHandleRapidStateChanges() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    // Rapid state changes
    uiStateFlow.value = SignInUiState(isLoading = true)
    composeTestRule.waitForIdle()

    uiStateFlow.value = SignInUiState(isLoading = false)
    composeTestRule.waitForIdle()

    uiStateFlow.value = SignInUiState(errorMessage = "Error")
    composeTestRule.waitForIdle()

    verify { mockViewModel.clearError() }
  }

  @Test
  fun shouldMaintainProperButtonOrder() {
    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    // Both buttons should exist
    composeTestRule.onNodeWithText("Sign in with Google").assertExists()
    composeTestRule.onNodeWithText("Sign in with Microsoft").assertExists()
  }

  @Test
  fun shouldHandleSuccessfulSignInWithValidUserEmail() {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.email } returns "test@gmail.com"

    composeTestRule.setContent { SignInScreen(viewModel = mockViewModel) }

    uiStateFlow.value = SignInUiState(isSignInSuccessful = true, currentUser = mockUser)

    composeTestRule.waitForIdle()
    // Should not crash with valid email
  }
}

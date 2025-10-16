package com.swent.mapin.ui.profile

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ProfileScreen composable - Logout functionality.
 *
 * Tests the logout functionality and related UI interactions of the profile screen using Compose
 * testing framework.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var mockRepository: UserProfileRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var viewModel: ProfileViewModel

  private val testUserId = "test-user-123"
  private val testProfile =
      UserProfile(
          userId = testUserId,
          name = "John Doe",
          bio = "Test bio",
          hobbies = listOf("Reading", "Gaming"),
          location = "New York",
          avatarUrl = null,
          bannerUrl = null,
          hobbiesVisible = true)

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    // Mock FirebaseAuth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId
    every { mockUser.displayName } returns "John Doe"
    every { mockUser.photoUrl } returns null

    // Mock Repository
    mockRepository = mockk(relaxed = true)
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun profileScreen_logoutButton_isDisplayed() {
    var navigatedToSignIn = false

    composeTestRule.setContent {
      ProfileScreen(
          onNavigateBack = {},
          onNavigateToSignIn = { navigatedToSignIn = true },
          viewModel = viewModel)
    }

    // Wait for the content to load
    composeTestRule.waitForIdle()

    // Scroll to the logout button to make it visible
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()

    // Verify the logout button exists and is displayed
    composeTestRule.onNodeWithTag("logoutButton").assertExists().assertIsDisplayed()
  }

  @Test
  fun profileScreen_logoutButton_hasCorrectText() {
    composeTestRule.setContent {
      ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Scroll to the logout button
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()

    // Verify the logout button contains the text "Logout"
    composeTestRule.onNodeWithTag("logoutButton").assertTextContains("Logout")
  }

  @Test
  fun profileScreen_logoutButton_isClickable() {
    composeTestRule.setContent {
      ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Scroll to the logout button
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()

    // Verify the logout button is clickable
    composeTestRule.onNodeWithTag("logoutButton").assertHasClickAction()
  }

  @Test
  fun profileScreen_logoutButton_triggersSignOut() {
    every { mockAuth.signOut() } returns Unit
    var navigatedToSignIn = false

    composeTestRule.setContent {
      ProfileScreen(
          onNavigateBack = {},
          onNavigateToSignIn = { navigatedToSignIn = true },
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Scroll to and click the logout button
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton").performClick()

    composeTestRule.waitForIdle()

    // Verify that signOut was called
    verify { mockAuth.signOut() }

    // Verify navigation to sign-in screen
    assert(navigatedToSignIn) { "Expected navigation to sign-in screen" }
  }

  @Test
  fun profileScreen_afterLogout_navigatesToSignInScreen() {
    every { mockAuth.signOut() } returns Unit
    var navigatedToSignIn = false

    composeTestRule.setContent {
      ProfileScreen(
          onNavigateBack = {},
          onNavigateToSignIn = { navigatedToSignIn = true },
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Verify profile content is displayed before logout
    composeTestRule.onNodeWithText("John Doe").assertExists()

    // Scroll to and click the logout button
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton").performClick()

    composeTestRule.waitForIdle()

    // Verify navigation callback was triggered
    assert(navigatedToSignIn) { "Navigation to sign-in screen should be triggered" }
  }

  @Test
  fun profileScreen_logoutButton_notDisplayedInEditMode() {
    every { mockAuth.signOut() } returns Unit

    composeTestRule.setContent {
      ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Scroll to verify logout button exists initially
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton").assertExists()

    // Scroll up to find the edit button
    composeTestRule.onNodeWithTag("editButton").assertExists()
    composeTestRule.onNodeWithTag("editButton").performClick()
    composeTestRule.waitForIdle()

    // Verify logout button is not displayed in edit mode
    composeTestRule.onNodeWithTag("logoutButton").assertDoesNotExist()
  }

  @Test
  fun profileScreen_logoutFromEditMode_clearsEditState() {
    every { mockAuth.signOut() } returns Unit
    var navigatedToSignIn = false

    composeTestRule.setContent {
      ProfileScreen(
          onNavigateBack = {},
          onNavigateToSignIn = { navigatedToSignIn = true },
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Enter edit mode
    composeTestRule.onNodeWithTag("editButton").assertExists()
    composeTestRule.onNodeWithTag("editButton").performClick()
    composeTestRule.waitForIdle()

    // Exit edit mode (cancel)
    composeTestRule.onNodeWithText("Cancel").performScrollTo()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // Now logout button should be visible again
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton").assertExists()

    // Click logout
    composeTestRule.onNodeWithTag("logoutButton").performClick()

    composeTestRule.waitForIdle()

    // Verify signOut was called
    verify { mockAuth.signOut() }
    assert(navigatedToSignIn) { "Should navigate to sign-in screen" }
  }

  @Test
  fun profileScreen_logoutButton_maintainsStateAcrossRecomposition() {
    var recomposeCount = 0

    composeTestRule.setContent {
      recomposeCount++
      ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Verify logout button is still present
    composeTestRule.onNodeWithTag("logoutButton").assertExists()

    // Scroll to ensure button is visible
    composeTestRule.onNodeWithTag("logoutButton").performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton").assertIsDisplayed()
  }
}

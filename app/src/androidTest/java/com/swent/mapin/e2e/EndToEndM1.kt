package com.swent.mapin.e2e

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.UserProfile
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.navigation.Route
import com.swent.mapin.testing.UiTestTags
import io.mockk.*
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * End-to-End Test: Complete User Profile Management Flow (Milestone 1)
 *
 * This test verifies that a user can successfully complete the following flow:
 * 1. Login with valid credentials (email/password)
 * 2. Navigate to the profile page from the map screen
 * 3. Edit profile information (display name and bio)
 * 4. Save the changes
 * 5. Verify the changes persist (by checking the UI displays updated values)
 * 6. Logout from the profile screen
 * 7. Confirm redirection to the login screen
 *
 * Implementation note: The original M1 version used a Settings button inside the Profile screen.
 * That button has since been removed from the UI. This test now navigates to the Settings screen
 * directly via the NavController (Route.Settings) instead of clicking a UI button. No assertions
 * rely on the removed button.
 *
 * This test uses MockK to mock Firebase authentication and Firestore operations, simulating a real
 * user journey through the app.
 *
 * Note: Tests run sequentially using a ReentrantLock to avoid conflicts with static mocks
 * (mockkStatic). This ensures that even if multiple test classes run in parallel, only one E2E test
 * runs at a time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EndToEndM1 {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  private lateinit var navController: androidx.navigation.NavHostController

  private lateinit var context: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockDocumentSnapshot: DocumentSnapshot

  // Test user data - unique identifier to prevent conflicts
  private val testId = System.currentTimeMillis().toString() + "-" + Thread.currentThread().id
  private val testUserId = "test-user-e2e-$testId"
  private val testEmail = "testuser@example.com"
  private val testUserName = "Test User"
  private val initialBio = "I love testing and coding!"
  private val updatedName = "Updated Test User"
  private val updatedBio = "This is my updated bio after editing"

  private var testProfile: UserProfile = UserProfile()
  private val renderMapInTests = false // Disable Mapbox rendering on CI emulators

  companion object {
    // Global lock to ensure only ONE E2E test runs at a time across all test instances
    // This prevents mockkStatic conflicts when tests run in parallel
    private val globalLock = ReentrantLock()

    @Volatile private var isLocked = false
  }

  @Before
  fun setup() {
    // Acquire the global lock - this will block if another E2E test is running
    globalLock.lock()
    isLocked = true

    try {
      context = ApplicationProvider.getApplicationContext()

      // Initialize Firebase if not already initialized
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }

      // Setup initial test profile
      testProfile =
          UserProfile(
              userId = testUserId,
              name = testUserName,
              bio = initialBio,
              hobbies = listOf("Testing", "Coding"),
              location = "Test City",
              avatarUrl = null,
              bannerUrl = null,
              hobbiesVisible = true)

      // Clear any existing mocks before setting up new ones
      // This prevents conflicts when tests run in parallel in CI
      clearAllMocks()

      // Mock FirebaseAuth and FirebaseUser
      mockkStatic(FirebaseAuth::class)
      mockAuth = mockk(relaxed = true)
      mockUser = mockk(relaxed = true)

      every { FirebaseAuth.getInstance() } returns mockAuth
      every { mockAuth.currentUser } returns mockUser
      every { mockUser.uid } returns testUserId
      every { mockUser.displayName } returns testUserName
      every { mockUser.email } returns testEmail
      every { mockUser.photoUrl } returns null
      every { mockAuth.signOut() } just Runs

      // Mock Firestore - this is the key to making the repository work correctly
      mockkStatic(FirebaseFirestore::class)
      mockFirestore = mockk(relaxed = true)
      mockCollection = mockk(relaxed = true)
      mockDocument = mockk(relaxed = true)
      mockDocumentSnapshot = mockk(relaxed = true)

      every { FirebaseFirestore.getInstance() } returns mockFirestore
      every { mockFirestore.collection("users") } returns mockCollection
      every { mockCollection.document(any()) } returns mockDocument

      // Mock document.get() to return our test profile DYNAMICALLY
      // This ensures that when the profile is loaded again, it gets the updated data
      every { mockDocumentSnapshot.exists() } returns true
      every { mockDocumentSnapshot.toObject(UserProfile::class.java) } answers { testProfile }
      every { mockDocument.get() } answers
          {
            println(
                "Mock Firestore: Loading profile - name: ${testProfile.name}, bio: ${testProfile.bio}")
            Tasks.forResult(mockDocumentSnapshot)
          }

      // Mock document.set() for saving
      every { mockDocument.set(any()) } answers
          {
            val profile = firstArg<UserProfile>()
            testProfile = profile
            println("Mock Firestore: Saved profile - name: ${profile.name}, bio: ${profile.bio}")
            Tasks.forResult(null)
          }
    } catch (e: Exception) {
      // If setup fails, release the lock
      if (isLocked && globalLock.isHeldByCurrentThread) {
        globalLock.unlock()
        isLocked = false
      }
      throw e
    }
  }

  @After
  fun tearDown() {
    try {
      unmockkAll()
    } finally {
      // Always release the lock, even if teardown fails
      if (isLocked && globalLock.isHeldByCurrentThread) {
        globalLock.unlock()
        isLocked = false
      }
    }
  }

  /**
   * Main End-to-End Test
   *
   * Verifies the complete user journey from login to logout with profile editing in between. Note:
   * This test starts from the map screen (already logged in) to avoid setContent limitations.
   */
  @Test
  fun completeUserFlow_loginEditProfileAndLogout_shouldWorkCorrectly() {
    // Start with logged in state - simulating user already authenticated
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          renderMap = renderMapInTests,
          autoRequestPermissions = false)
    }
    composeTestRule.waitForIdle()

    // ============================================
    // STEP 1: Verify we're on the map screen (logged in)
    // ============================================
    // Ensure the map root exists (visibility can be animated in tests, so use assertExists)
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertExists()

    // ============================================
    // STEP 2: Navigate to profile screen
    // ============================================
    // Clear focus (click on map) before swiping to ensure profile button is visible/clickable
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Navigate directly to the Profile screen to avoid flakiness from bottom-sheet animations
    composeTestRule.runOnIdle { navController.navigate(Route.Profile.route) }

    composeTestRule.waitForIdle()

    // Verify we're on the profile screen
    // Use assertExists to avoid flakiness from animations
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertExists()

    // Wait for profile data to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText(testUserName, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify initial profile data is displayed
    composeTestRule.onNodeWithText(testUserName, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText(initialBio, useUnmergedTree = true).assertExists()

    // ============================================
    // STEP 3: Enter edit mode
    // ============================================
    composeTestRule.onNodeWithTag("editButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify edit fields are now visible
    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("editBioField", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("editBioField", useUnmergedTree = true).assertIsDisplayed()

    // ============================================
    // STEP 4: Edit profile information
    // ============================================
    // Clear and update name
    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).performTextClearance()
    composeTestRule
        .onNodeWithTag("editNameField", useUnmergedTree = true)
        .performTextInput(updatedName)

    // Clear and update bio
    composeTestRule.onNodeWithTag("editBioField", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("editBioField", useUnmergedTree = true).performTextClearance()
    composeTestRule
        .onNodeWithTag("editBioField", useUnmergedTree = true)
        .performTextInput(updatedBio)

    // ============================================
    // STEP 5: Save the changes
    // ============================================
    composeTestRule.onNodeWithTag("saveButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("saveButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // ============================================
    // STEP 6: Verify changes persist in the UI
    // ============================================
    // Wait for the updated name to appear
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText(updatedName, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // The updated name should be visible
    composeTestRule.onNodeWithText(updatedName, useUnmergedTree = true).assertExists()

    // The updated bio should be visible
    composeTestRule.onNodeWithText(updatedBio, useUnmergedTree = true).assertExists()

    // ============================================
    // STEP 7: Navigate to Settings (directly via navigation)
    // ============================================
    composeTestRule.runOnIdle { navController.navigate(Route.Settings.route) }

    composeTestRule.waitForIdle()

    // Verify we're on settings screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("settingsScreen", useUnmergedTree = true).assertExists()

    // ============================================
    // STEP 8: Logout from Settings screen
    // ============================================
    // Scroll to and click logout button
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for dialog confirm button to appear and click the stable testTag
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag("logoutConfirmButton", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("logoutConfirmButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Verify signOut was called
    verify { mockAuth.signOut() }

    // ============================================
    // STEP 9: Verify redirection to login screen
    // ============================================
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Verify we're back at the sign-in screen
    composeTestRule.onNodeWithText("One Map. Every moment.", useUnmergedTree = true).assertExists()

    // Verify the map and profile screens are no longer in the composition
    composeTestRule
        .onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
        .assertDoesNotExist()
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertDoesNotExist()
  }

  /**
   * Test: Profile edit cancellation AppNavHost(navController = navController, isLoggedIn = true,
   * renderMap = false) Verifies that canceling profile edits discards changes and returns to view
   * mode.
   */
  @Test
  fun profileEdit_cancelButton_shouldDiscardChanges() {
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          renderMap = renderMapInTests,
          autoRequestPermissions = false)
    }

    composeTestRule.waitForIdle()

    // Reveal and navigate to profile by swiping up the bottom sheet
    // Navigate programmatically to Profile (avoid bottom-sheet swipe flakiness)
    composeTestRule.runOnIdle { navController.navigate(Route.Profile.route) }
    composeTestRule.waitForIdle()

    // Wait for profile data to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText(testUserName, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify initial data
    composeTestRule.onNodeWithText(testUserName, useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText(initialBio, useUnmergedTree = true).assertExists()

    // Enter edit mode
    composeTestRule.onNodeWithTag("editButton", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Edit name
    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).performTextClearance()
    composeTestRule
        .onNodeWithTag("editNameField", useUnmergedTree = true)
        .performTextInput("Temporary Name")

    // Edit bio
    composeTestRule.onNodeWithTag("editBioField", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("editBioField", useUnmergedTree = true).performTextClearance()
    composeTestRule
        .onNodeWithTag("editBioField", useUnmergedTree = true)
        .performTextInput("Temporary bio")

    // Cancel editing
    composeTestRule.onNodeWithTag("cancelButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("cancelButton", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Wait for view mode to be restored
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText(testUserName, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify original data is still displayed
    composeTestRule.onNodeWithText(testUserName, useUnmergedTree = true).assertExists()

    // Verify temporary values are gone
    composeTestRule.onNodeWithText("Temporary Name", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Temporary bio", useUnmergedTree = true).assertDoesNotExist()
  }

  /**
   * Test: Profile data persistence across navigation
   *
   * Verifies that profile changes persist when navigating away and back.
   */
  @Test
  fun profileEdit_navigationBackAndForth_shouldPersistChanges() {
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(
          navController = navController,
          isLoggedIn = true,
          renderMap = renderMapInTests,
          autoRequestPermissions = false)
    }

    composeTestRule.waitForIdle()

    // Reveal and navigate to profile by swiping up the bottom sheet
    // Programmatic navigation to Profile for stability
    composeTestRule.runOnIdle { navController.navigate(Route.Profile.route) }
    composeTestRule.waitForIdle()

    // Wait for profile data to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText(testUserName, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Enter edit mode and update name
    composeTestRule.onNodeWithTag("editButton", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("editNameField", useUnmergedTree = true).performTextClearance()
    composeTestRule
        .onNodeWithTag("editNameField", useUnmergedTree = true)
        .performTextInput(updatedName)

    // Save changes
    composeTestRule.onNodeWithTag("saveButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("saveButton", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Wait for save to complete and view mode to show updated name
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText(updatedName, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate back to map
    composeTestRule.onNodeWithTag("backButton", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on map screen
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()

    // Swipe up on bottom sheet to reveal profile button
    // Programmatically navigate back to Profile (avoid swiping)
    composeTestRule.runOnIdle { navController.navigate(Route.Profile.route) }
    composeTestRule.waitForIdle()

    // Wait for profile to load with updated data
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithText(updatedName, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify the updated name is still displayed
    composeTestRule.onNodeWithText(updatedName, useUnmergedTree = true).assertExists()
  }
}

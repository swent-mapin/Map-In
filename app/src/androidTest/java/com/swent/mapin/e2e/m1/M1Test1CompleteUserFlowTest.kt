package com.swent.mapin.e2e.m1

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import com.swent.mapin.model.userprofile.UserProfile
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.navigation.Route
import com.swent.mapin.testing.UiTestTags
import io.mockk.*
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class M1Test1CompleteUserFlowTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  private lateinit var navController: androidx.navigation.NavHostController
  private lateinit var context: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockDocumentSnapshot: DocumentSnapshot

  private val testId = System.currentTimeMillis().toString() + "-" + Thread.currentThread().id
  private val testUserId = "test-user-e2e-$testId"
  private val testEmail = "testuser@example.com"
  private val testUserName = "Test User"
  private val initialBio = "I love testing and coding!"
  private val updatedName = "Updated Test User"
  private val updatedBio = "This is my updated bio after editing"
  private var testProfile: UserProfile = UserProfile()
  private val renderMapInTests = false

  companion object {
    private val globalLock = ReentrantLock()
    @Volatile private var isLocked = false
  }

  @Before
  fun setup() {
    globalLock.lock()
    isLocked = true

    try {
      context = ApplicationProvider.getApplicationContext()
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }

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

      clearAllMocks()

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

      mockkStatic(FirebaseFirestore::class)
      mockFirestore = mockk(relaxed = true)
      mockCollection = mockk(relaxed = true)
      mockDocument = mockk(relaxed = true)
      mockDocumentSnapshot = mockk(relaxed = true)

      every { FirebaseFirestore.getInstance() } returns mockFirestore
      every { mockFirestore.collection("users") } returns mockCollection
      every { mockCollection.document(any()) } returns mockDocument
      every { mockDocumentSnapshot.exists() } returns true
      every { mockDocumentSnapshot.toObject(UserProfile::class.java) } answers { testProfile }
      every { mockDocument.get() } answers
          {
            println(
                "Mock Firestore: Loading profile - name: ${testProfile.name}, bio: ${testProfile.bio}")
            Tasks.forResult(mockDocumentSnapshot)
          }
      every { mockDocument.set(any()) } answers
          {
            val profile = firstArg<UserProfile>()
            testProfile = profile
            println("Mock Firestore: Saved profile - name: ${profile.name}, bio: ${profile.bio}")
            Tasks.forResult(null)
          }
    } catch (e: Exception) {
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
}

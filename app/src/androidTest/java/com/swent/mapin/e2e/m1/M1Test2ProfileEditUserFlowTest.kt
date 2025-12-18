package com.swent.mapin.e2e.m1

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class M1Test2ProfileEditUserFlowTest {

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
}

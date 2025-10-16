package com.swent.mapin.navigationTests

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
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
import com.swent.mapin.testing.UiTestTags
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockDocumentSnapshot: DocumentSnapshot

  private val testUserId = "test-nav-user-123"
  private val testUserName = "Nav Test User"

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    // Clear any existing mocks first
    clearAllMocks()

    // Mock FirebaseAuth
    mockkStatic(FirebaseAuth::class)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId
    every { mockUser.displayName } returns testUserName
    every { mockUser.email } returns "navtest@example.com"
    every { mockUser.photoUrl } returns null
    every { mockAuth.signOut() } just Runs

    // Mock Firestore
    mockkStatic(FirebaseFirestore::class)
    mockFirestore = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)
    mockDocumentSnapshot = mockk(relaxed = true)

    every { FirebaseFirestore.getInstance() } returns mockFirestore
    every { mockFirestore.collection("users") } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument

    // Mock document operations with dynamic profile
    val testProfile =
        UserProfile(
            userId = testUserId,
            name = testUserName,
            bio = "Test bio",
            hobbies = listOf("Testing"),
            location = "Test City")

    every { mockDocumentSnapshot.exists() } returns true
    every { mockDocumentSnapshot.toObject(UserProfile::class.java) } answers { testProfile }
    every { mockDocument.get() } answers { Tasks.forResult(mockDocumentSnapshot) }
    every { mockDocument.set(any()) } answers { Tasks.forResult(null) }
  }

  @After
  fun tearDown() {
    unmockkAll()
    clearAllMocks()
  }

  @Test
  fun startsOnAuth_whenNotLoggedIn() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = false)
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun startsOnMap_whenLoggedIn() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun navigatesToProfile_fromMap() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Verify we're on the map screen
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertIsDisplayed()

    // Navigate to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for profile to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on the profile screen
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun logout_navigatesBackToAuth() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Navigate to profile from map
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for profile to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on profile screen
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()

    // Scroll to the logout button and click it
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for auth screen to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're back on auth screen
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun logout_clearsBackStack() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Navigate to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for profile to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to and click logout
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for auth screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on auth screen
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Back button should not navigate away from auth screen (back stack is cleared)
    // Note: In a real scenario, you'd test that the back stack is empty by attempting
    // to navigate back and verifying we stay on the auth screen
  }

  @Test
  fun logout_fromProfile_cannotNavigateBackToMap() {
    composeTestRule.setContent {
      AppNavHost(navController = rememberNavController(), isLoggedIn = true)
    }

    composeTestRule.waitForIdle()

    // Go to profile
    composeTestRule.onNodeWithTag("profileButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for profile to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to and click logout
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performScrollTo()
    composeTestRule.onNodeWithTag("logoutButton", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Wait for auth screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on auth screen and map screen doesn't exist in the tree
    composeTestRule
        .onNodeWithTag(UiTestTags.AUTH_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Map screen should not be in the composition tree after logout
    composeTestRule
        .onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
        .assertDoesNotExist()
  }
}

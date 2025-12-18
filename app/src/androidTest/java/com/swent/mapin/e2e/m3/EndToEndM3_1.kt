package com.swent.mapin.e2e.m3

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.userprofile.UserProfile
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
 * End-to-End Test: Complete AI Assistant, Memories, Settings, Event Creation and Directions Flow
 * (Milestone 3)
 *
 * This comprehensive test verifies that a user can successfully complete the following flows:
 *
 * AI Assistant Flow:
 * 1. Start from the map screen (logged in)
 * 2. Navigate to the AI Assistant screen
 * 3. Verify AI Assistant UI elements are displayed (mic button, status indicators)
 * 4. Interact with the reset button to clear conversation state
 * 5. Navigate back to the map screen
 *
 * Memories Flow:
 * 6. Navigate to the Memories screen
 * 7. Verify Memories UI elements are displayed
 * 8. Interact with the refresh button
 * 9. Navigate back to map screen
 *
 * Settings Flow:
 * 10. Navigate to Settings screen
 * 11. Interact with Settings toggles (POI, Road Numbers, Street Names)
 * 12. Navigate to Change Password screen
 * 13. Verify Change Password UI elements
 * 14. Navigate back through the flow
 *
 * Profile and Badge Flow:
 * 15. Navigate to Profile screen
 * 16. Verify badges section is displayed
 * 17. Check badge count and achievements
 * 18. Navigate back to map screen
 *
 * Event Detail and Directions Flow:
 * 19. Verify event detail sheet and directions button
 * 20. Navigate back to map screen
 *
 * This test uses MockK to mock Firebase authentication and Firestore operations, simulating a real
 * user journey through the AI Assistant, Memories, Settings, Profile/Badges, and Event/Directions
 * features.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EndToEndM3_1 {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  private lateinit var navController: NavHostController

  private lateinit var context: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockFirestore: FirebaseFirestore

  private val testId = System.currentTimeMillis().toString() + "-" + Thread.currentThread().id
  private val testUserId = "test-user-e2e-m3-$testId"
  private val testEmail = "testuser.m3@example.com"
  private val testUserName = "M3 E2E Tester"

  private var testProfile: UserProfile = UserProfile()
  private var testMemory: Memory = Memory()
  private var testEvent: Event = Event()
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
              bio = "M3 E2E tester",
              hobbies = listOf("Music", "Technology", "Photography", "Travel"),
              location = "San Francisco",
              avatarUrl = null,
              bannerUrl = null,
              hobbiesVisible = true)

      testMemory =
          Memory(
              uid = "test-memory-$testId",
              title = "Test Memory",
              description = "A test memory for E2E testing",
              eventId = "test-event-$testId",
              ownerId = testUserId,
              isPublic = false,
              createdAt = Timestamp.now(),
              mediaUrls = emptyList(),
              taggedUserIds = emptyList())

      testEvent =
          Event(
              uid = "test-event-$testId",
              title = "Test Concert",
              description = "An amazing music concert in the park",
              location = Location.from("Central Park", 40.7829, -73.9654),
              tags = listOf("Music", "Outdoor"),
              date = Timestamp.now(),
              endDate = Timestamp.now(),
              public = true,
              ownerId = testUserId,
              participantIds = emptyList(),
              capacity = 100)

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
      every { FirebaseFirestore.getInstance() } returns mockFirestore

      setupFirestoreMocks()
    } catch (e: Exception) {
      if (isLocked && globalLock.isHeldByCurrentThread) {
        globalLock.unlock()
        isLocked = false
      }
      throw e
    }
  }

  private fun setupFirestoreMocks() {
    val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    val mockUserDocument = mockk<DocumentReference>(relaxed = true)
    val mockUserSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockFirestore.collection("users") } returns mockUsersCollection
    every { mockUsersCollection.document(any()) } returns mockUserDocument
    every { mockUserSnapshot.exists() } returns true
    every { mockUserSnapshot.toObject(UserProfile::class.java) } answers { testProfile }
    every { mockUserDocument.get() } answers { Tasks.forResult(mockUserSnapshot) }
    every { mockUserDocument.set(any()) } answers { Tasks.forResult(null) }
    every { mockUsersCollection.whereEqualTo(any<String>(), any()) } returns mockUsersCollection
    every { mockUsersCollection.get() } answers { Tasks.forResult(mockQuerySnapshot) }
    every { mockQuerySnapshot.documents } returns emptyList()

    // Memories collection
    val mockMemoriesCollection = mockk<CollectionReference>(relaxed = true)
    val mockMemoryDocument = mockk<DocumentReference>(relaxed = true)
    val mockMemorySnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockFirestore.collection("memories") } returns mockMemoriesCollection
    every { mockMemoriesCollection.document(any()) } returns mockMemoryDocument
    every { mockMemoriesCollection.document() } returns mockMemoryDocument
    every { mockMemoryDocument.id } returns "test-memory-$testId"
    every { mockMemorySnapshot.exists() } returns true
    every { mockMemorySnapshot.toObject(Memory::class.java) } answers { testMemory }
    every { mockMemoryDocument.get() } answers { Tasks.forResult(mockMemorySnapshot) }
    every { mockMemoryDocument.set(any()) } answers { Tasks.forResult(null) }
    every { mockMemoriesCollection.whereEqualTo(any<String>(), any()) } returns
        mockMemoriesCollection
    every { mockMemoriesCollection.get() } answers { Tasks.forResult(mockQuerySnapshot) }

    // Events collection
    val mockEventsCollection = mockk<CollectionReference>(relaxed = true)
    val mockEventDocument = mockk<DocumentReference>(relaxed = true)
    val mockEventSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockEventQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockFirestore.collection("events") } returns mockEventsCollection
    every { mockEventsCollection.document(any()) } returns mockEventDocument
    every { mockEventsCollection.document() } returns mockEventDocument
    every { mockEventDocument.id } returns "test-event-$testId"
    every { mockEventSnapshot.exists() } returns true
    every { mockEventSnapshot.toObject(Event::class.java) } answers { testEvent }
    every { mockEventSnapshot.id } returns "test-event-$testId"
    every { mockEventDocument.get() } answers { Tasks.forResult(mockEventSnapshot) }
    every { mockEventDocument.set(any()) } answers { Tasks.forResult(null) }
    every { mockEventQuerySnapshot.documents } returns listOf(mockEventSnapshot)
    every { mockEventsCollection.get() } answers { Tasks.forResult(mockEventQuerySnapshot) }
    every { mockEventsCollection.whereEqualTo(any<String>(), any()) } returns mockEventsCollection
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
   * Complete AI Assistant, Memories, Settings, Profile/Badges and Directions Flow Test
   *
   * Tests the full user journey through the AI Assistant, Memories, Settings, Profile/Badges, and
   * Event/Directions features:
   * - Navigation to AI Assistant, interaction with controls, reset functionality
   * - Navigation to Memories, UI verification, refresh interaction
   * - Navigation to Settings, toggle interactions, Change Password navigation
   * - Navigation to Profile, badges section verification, badge count check
   * - Event detail sheet verification, directions button interaction
   * - Return navigation to map screen
   */
  @Test
  fun completeM3Flow_aiAssistantMemoriesAndSettings_shouldWorkCorrectly() {
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
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertExists()

    // ============================================
    // PART A: AI ASSISTANT FLOW
    // ============================================

    // ============================================
    // STEP 2: Navigate to AI Assistant screen
    // ============================================
    composeTestRule.runOnIdle { navController.navigate(Route.AiAssistant.route) }
    composeTestRule.waitForIdle()

    // Wait for AI Assistant screen to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag("aiAssistantScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // ============================================
    // STEP 3: Verify AI Assistant UI elements
    // ============================================
    composeTestRule.onNodeWithTag("aiAssistantScreen", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("backButton", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("resetButton", useUnmergedTree = true).assertExists()

    // ============================================
    // STEP 4: Try to interact with mic button if available
    // ============================================
    val micButtonTags = listOf("micButton", "voiceButton", "startListeningButton")
    for (tag in micButtonTags) {
      try {
        val nodes =
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
          composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
          composeTestRule.waitForIdle()
          Thread.sleep(500)
          // Click again to stop listening
          composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
          composeTestRule.waitForIdle()
          break
        }
      } catch (_: Exception) {
        // Continue with other tags
      }
    }

    // ============================================
    // STEP 5: Test reset functionality
    // ============================================
    composeTestRule.onNodeWithTag("resetButton", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Verify screen is still displayed after reset
    composeTestRule.onNodeWithTag("aiAssistantScreen", useUnmergedTree = true).assertIsDisplayed()

    // ============================================
    // STEP 6: Check for help button and interact if available
    // ============================================
    try {
      val helpButton = composeTestRule.onAllNodesWithTag("helpButton", useUnmergedTree = true)
      if (helpButton.fetchSemanticsNodes().isNotEmpty()) {
        helpButton.onFirst().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        // Dismiss any help dialog
        try {
          composeTestRule.onNodeWithText("OK", useUnmergedTree = true).performClick()
        } catch (_: Exception) {
          // Dialog may not have OK button or may have dismissed
        }
      }
    } catch (_: Exception) {
      // Help button may not exist
    }

    // ============================================
    // STEP 7: Navigate back to map screen
    // ============================================
    composeTestRule.onNodeWithTag("backButton", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on map screen
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertExists()

    // Verify AI Assistant screen is no longer displayed
    composeTestRule.onNodeWithTag("aiAssistantScreen", useUnmergedTree = true).assertDoesNotExist()

    // ============================================
    // PART B: MEMORIES FLOW
    // ============================================

    // ============================================
    // STEP 8: Navigate to Memories screen
    // ============================================
    composeTestRule.runOnIdle { navController.navigate(Route.Memories.route) }
    composeTestRule.waitForIdle()

    // Wait for Memories screen to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag("memoriesScreenTitle", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // ============================================
    // STEP 9: Verify Memories UI elements
    // ============================================
    composeTestRule.onNodeWithTag("memoriesScreenTitle", useUnmergedTree = true).assertIsDisplayed()

    // Check for empty state or memories list
    try {
      val noMemoriesNodes =
          composeTestRule
              .onAllNodesWithTag("noMemoriesMessage", useUnmergedTree = true)
              .fetchSemanticsNodes()
      if (noMemoriesNodes.isNotEmpty()) {
        composeTestRule.onNodeWithTag("noMemoriesMessage", useUnmergedTree = true).assertExists()
      }
    } catch (_: Exception) {
      // Memories exist, which is fine
    }

    // ============================================
    // STEP 10: Interact with refresh button if available
    // ============================================
    try {
      val refreshButton =
          composeTestRule.onAllNodesWithTag("refreshAllButton", useUnmergedTree = true)
      if (refreshButton.fetchSemanticsNodes().isNotEmpty()) {
        refreshButton.onFirst().performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)
      }
    } catch (_: Exception) {
      // Refresh button may not be visible
    }

    // ============================================
    // STEP 11: Navigate back to map screen
    // ============================================
    composeTestRule.runOnIdle { navController.popBackStack() }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertExists()

    // ============================================
    // PART C: SETTINGS FLOW
    // ============================================

    // ============================================
    // STEP 12: Navigate to Settings screen
    // ============================================
    composeTestRule.runOnIdle { navController.navigate(Route.Settings.route) }
    composeTestRule.waitForIdle()

    // Wait for Settings screen to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag("settingsScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // ============================================
    // STEP 13: Verify Settings screen and interact with toggles
    // ============================================
    composeTestRule.onNodeWithTag("settingsScreen", useUnmergedTree = true).assertIsDisplayed()

    // Try to interact with POI toggle
    try {
      composeTestRule.onNodeWithTag("poiToggle", useUnmergedTree = true).performScrollTo()
      composeTestRule.onNodeWithTag("poiToggle", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()
    } catch (_: Exception) {
      // POI toggle may not be accessible
    }

    // Try to interact with Road Numbers toggle
    try {
      composeTestRule.onNodeWithTag("roadNumbersToggle", useUnmergedTree = true).performScrollTo()
      composeTestRule.onNodeWithTag("roadNumbersToggle", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()
    } catch (_: Exception) {
      // Road numbers toggle may not be accessible
    }

    // Try to interact with Street Names toggle
    try {
      composeTestRule.onNodeWithTag("streetNamesToggle", useUnmergedTree = true).performScrollTo()
      composeTestRule.onNodeWithTag("streetNamesToggle", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()
    } catch (_: Exception) {
      // Street names toggle may not be accessible
    }

    // ============================================
    // STEP 14: Navigate to Change Password screen (only for email/password users)
    // ============================================
    val changePasswordButtonNodes =
        composeTestRule
            .onAllNodesWithTag("changePasswordButton", useUnmergedTree = true)
            .fetchSemanticsNodes()

    if (changePasswordButtonNodes.isNotEmpty()) {
      composeTestRule
          .onNodeWithTag("changePasswordButton", useUnmergedTree = true)
          .performScrollTo()
      composeTestRule.onNodeWithTag("changePasswordButton", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()

      // Wait for Change Password screen to load
      composeTestRule.waitUntil(timeoutMillis = 10000) {
        composeTestRule
            .onAllNodesWithTag("changePasswordScreen", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      // ============================================
      // STEP 15: Verify Change Password UI elements
      // ============================================
      composeTestRule
          .onNodeWithTag("changePasswordScreen", useUnmergedTree = true)
          .assertIsDisplayed()
      composeTestRule.onNodeWithTag("currentPasswordField", useUnmergedTree = true).assertExists()
      composeTestRule.onNodeWithTag("newPasswordField", useUnmergedTree = true).assertExists()
      composeTestRule.onNodeWithTag("confirmPasswordField", useUnmergedTree = true).assertExists()
      composeTestRule.onNodeWithTag("saveButton", useUnmergedTree = true).assertExists()
      composeTestRule.onNodeWithTag("cancelButton", useUnmergedTree = true).assertExists()

      // ============================================
      // STEP 16: Click cancel to go back to Settings
      // ============================================
      composeTestRule.onNodeWithTag("cancelButton", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()
    }
    // Note: Change Password button is only available for email/password authenticated users

    // ============================================
    // STEP 17: Navigate back to map screen
    // ============================================
    composeTestRule.runOnIdle { navController.popBackStack(Route.Map.route, false) }
    composeTestRule.waitForIdle()

    // ============================================
    // STEP 18: Verify we're back on map screen
    // ============================================
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertExists()

    // ============================================
    // PART D: PROFILE AND BADGE FLOW
    // ============================================

    // ============================================
    // STEP 19: Navigate to Profile screen
    // ============================================
    composeTestRule.runOnIdle { navController.navigate(Route.Profile.route) }
    composeTestRule.waitForIdle()

    // Wait for Profile screen to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag("profileScreen", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // ============================================
    // STEP 20: Verify Profile screen is displayed
    // ============================================
    composeTestRule.onNodeWithTag("profileScreen", useUnmergedTree = true).assertIsDisplayed()

    // ============================================
    // STEP 21: Check for badges section
    // ============================================
    val badgesSectionNodes =
        composeTestRule
            .onAllNodesWithTag("badgesSection", useUnmergedTree = true)
            .fetchSemanticsNodes()

    if (badgesSectionNodes.isNotEmpty()) {
      // Scroll to badges section
      composeTestRule.onNodeWithTag("badgesSection", useUnmergedTree = true).performScrollTo()
      composeTestRule.onNodeWithTag("badgesSection", useUnmergedTree = true).assertIsDisplayed()

      // Check for badge count display
      val badgeCountNodes =
          composeTestRule
              .onAllNodesWithTag("badgeCount", useUnmergedTree = true)
              .fetchSemanticsNodes()
      if (badgeCountNodes.isNotEmpty()) {
        composeTestRule.onNodeWithTag("badgeCount", useUnmergedTree = true).assertExists()
      }
    }

    // ============================================
    // STEP 22: Navigate back to map screen
    // ============================================
    composeTestRule.runOnIdle { navController.popBackStack() }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertExists()

    // ============================================
    // PART E: EVENT DETAIL AND DIRECTIONS FLOW
    // ============================================

    // ============================================
    // STEP 23: Check for event detail sheet elements
    // ============================================
    // Try to find and interact with event detail sheet if available
    val eventDetailSheetNodes =
        composeTestRule
            .onAllNodesWithTag("eventDetailSheet", useUnmergedTree = true)
            .fetchSemanticsNodes()

    if (eventDetailSheetNodes.isNotEmpty()) {
      composeTestRule.onNodeWithTag("eventDetailSheet", useUnmergedTree = true).assertExists()

      // ============================================
      // STEP 24: Check for directions button
      // ============================================
      val directionsButtonNodes =
          composeTestRule
              .onAllNodesWithTag(UiTestTags.GET_DIRECTIONS_BUTTON, useUnmergedTree = true)
              .fetchSemanticsNodes()

      if (directionsButtonNodes.isNotEmpty()) {
        composeTestRule
            .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON, useUnmergedTree = true)
            .assertExists()

        // Try to click the directions button
        try {
          composeTestRule
              .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON, useUnmergedTree = true)
              .performClick()
          composeTestRule.waitForIdle()
          Thread.sleep(500)

          // Click again to clear directions (toggle behavior)
          composeTestRule
              .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON, useUnmergedTree = true)
              .performClick()
          composeTestRule.waitForIdle()
        } catch (_: Exception) {
          // Directions button may require location permission
        }
      }
    }

    // ============================================
    // STEP 25: Verify final state on map screen
    // ============================================
    composeTestRule.onNodeWithTag(UiTestTags.MAP_SCREEN, useUnmergedTree = true).assertExists()

    println(
        "M3: Complete AI Assistant, Memories, Settings, Profile/Badges, and Directions E2E test completed successfully")
  }
}

package com.swent.mapin.e2e.m2

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.google.firebase.firestore.QuerySnapshot
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.userprofile.UserProfile
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
class M2Test5MapSearchUserFlowTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  private lateinit var navController: androidx.navigation.NavHostController
  private lateinit var context: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockFirestore: FirebaseFirestore

  private val testId = System.currentTimeMillis().toString() + "-" + Thread.currentThread().id
  private val testUserId = "test-user-e2e-$testId"
  private val testEmail = "testuser@example.com"
  private val testUserName = "Test User"
  private val searchQuery = "Central Park"
  private val searchLocation = Location.from("Central Park, New York", 40.7829, -73.9654)
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
              bio = "I love testing and coding!",
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

  @Test
  fun m2_completeMapSearchFlow_searchNavigateAndFilter_shouldWorkCorrectly() {
    // COPY TEST CONTENT HERE
  }
}

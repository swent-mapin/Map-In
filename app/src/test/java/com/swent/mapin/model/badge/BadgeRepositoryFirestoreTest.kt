package com.swent.mapin.model.badge

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BadgeRepositoryFirestore.
 *
 * Tests core Firestore operations and error handling.
 */
class BadgeRepositoryFirestoreTest {

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockDocumentReference: DocumentReference
  private lateinit var mockDocumentSnapshot: DocumentSnapshot
  private lateinit var mockUserProfileRepository: UserProfileRepository
  private lateinit var repository: BadgeRepositoryFirestore

  private val testUserId = "testUser123"
  private val testBadges =
      listOf(
          Badge(
              id = "friendly",
              title = "Friendly",
              description = "Make your first friend",
              iconName = "face",
              rarity = BadgeRarity.COMMON,
              isUnlocked = true,
              progress = 1f),
          Badge(
              id = "profile_pro",
              title = "Profile Pro",
              description = "Complete all profile fields",
              iconName = "person",
              rarity = BadgeRarity.RARE,
              isUnlocked = false,
              progress = 0.6f))

  @Before
  fun setup() {
    mockFirestore = mockk(relaxed = true)
    mockDocumentReference = mockk(relaxed = true)
    mockDocumentSnapshot = mockk(relaxed = true)
    mockUserProfileRepository = mockk(relaxed = true)
    repository = BadgeRepositoryFirestore(mockFirestore, mockUserProfileRepository)

    // Setup default mock chain
    every { mockFirestore.collection("users") } returns
        mockk(relaxed = true) { every { document(any()) } returns mockDocumentReference }
  }

  @After
  fun teardown() {
    clearAllMocks()
  }

  // ==================== saveBadgeProgress Tests ====================

  @Test
  fun `saveBadgeProgress successfully saves badges to Firestore`() = runTest {
    // Arrange
    coEvery { mockDocumentReference.update("badges", testBadges) } returns Tasks.forResult(null)

    // Act
    val result = repository.saveBadgeProgress(testUserId, testBadges)

    // Assert
    assertTrue("Save should succeed", result)
    coVerify { mockDocumentReference.update("badges", testBadges) }
  }

  @Test
  fun `saveBadgeProgress returns false on Firestore exception`() = runTest {
    // Arrange
    coEvery { mockDocumentReference.update("badges", testBadges) } returns
        Tasks.forException(Exception("Firestore error"))

    // Act
    val result = repository.saveBadgeProgress(testUserId, testBadges)

    // Assert
    assertFalse("Save should fail on exception", result)
  }

  // ==================== getUserBadges Tests ====================

  @Test
  fun `getUserBadges retrieves badges from Firestore successfully`() = runTest {
    // Arrange
    val badgesData =
        listOf(
            mapOf(
                "id" to "friendly",
                "title" to "Friendly",
                "description" to "Make your first friend",
                "iconName" to "face",
                "rarity" to "COMMON",
                "isUnlocked" to true,
                "progress" to 1.0))

    coEvery { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
    every { mockDocumentSnapshot.exists() } returns true
    every { mockDocumentSnapshot.get("badges") } returns badgesData

    // Act - Test cache by calling twice
    val result = repository.getUserBadges(testUserId)
    val cachedResult = repository.getUserBadges(testUserId)

    // Assert
    assertNotNull("Badges should not be null", result)
    assertEquals("Should return 1 badge", 1, result!!.size)
    assertEquals("Badge ID should match", "friendly", result[0].id)
    assertTrue("Badge should be unlocked", result[0].isUnlocked)
    assertNotNull("Cached result should not be null", cachedResult)
    coVerify(exactly = 1) { mockDocumentReference.get() }
  }

  @Test
  fun `getUserBadges returns null on Firestore exception`() = runTest {
    // Arrange
    coEvery { mockDocumentReference.get() } returns Tasks.forException(Exception("Network error"))

    // Act
    val result = repository.getUserBadges(testUserId)

    // Assert
    assertNull("Should return null on exception", result)
  }

  @Test
  fun `getUserBadges handles malformed badge data gracefully`() = runTest {
    // Arrange - Test document not exists, null badges, and malformed data
    coEvery { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
    every { mockDocumentSnapshot.exists() } returns false

    var result = repository.getUserBadges(testUserId)
    assertNull("Should return null when document doesn't exist", result)

    // Test null badges field
    every { mockDocumentSnapshot.exists() } returns true
    every { mockDocumentSnapshot.get("badges") } returns null
    result = repository.getUserBadges("user2")
    assertNotNull("Should return empty list", result)
    assertEquals(0, result!!.size)

    // Test malformed badge
    val badgesData =
        listOf(
            mapOf(
                "id" to "friendly",
                "title" to "Friendly",
                "description" to "Make your first friend",
                "iconName" to "face",
                "rarity" to "COMMON",
                "isUnlocked" to true,
                "progress" to 1.0),
            mapOf("id" to "invalid_badge", "rarity" to "INVALID_RARITY"))
    every { mockDocumentSnapshot.get("badges") } returns badgesData
    result = repository.getUserBadges("user3")
    assertNotNull("Should return non-null result", result)
    assertEquals("Should return only 1 valid badge", 1, result!!.size)
  }

  @Test
  fun `updateBadgeUnlockStatus works correctly`() = runTest {
    val badgesData =
        testBadges.map {
          mapOf(
              "id" to it.id,
              "title" to it.title,
              "description" to it.description,
              "iconName" to it.iconName,
              "rarity" to it.rarity.name,
              "isUnlocked" to it.isUnlocked,
              "progress" to it.progress.toDouble())
        }
    coEvery { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
    every { mockDocumentSnapshot.exists() } returns true
    every { mockDocumentSnapshot.get("badges") } returns badgesData
    coEvery { mockDocumentReference.update("badges", any<List<Badge>>()) } returns
        Tasks.forResult(null)

    val result = repository.updateBadgeUnlockStatus(testUserId, "profile_pro", true, 123456L)
    assertTrue("Update should succeed", result)
  }
}

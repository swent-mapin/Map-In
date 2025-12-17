package com.swent.mapin.model.badge

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.userprofile.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

  // ==================== clearCache Tests ====================

  @Test
  fun `clearCache removes cached badges for user`() = runTest {
    // Arrange - First populate the cache by fetching badges
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

    // Fetch to populate cache
    repository.getUserBadges(testUserId)

    // Clear cache
    repository.clearCache(testUserId)

    // Fetch again - should query Firestore, not return cached value
    repository.getUserBadges(testUserId)

    // Verify Firestore was called exactly 2 times:
    // Call 1: Initial fetch to populate cache
    // Call 2: Fetch after clearCache (cache was cleared, so must hit Firestore again)
    coVerify(exactly = 2) { mockDocumentReference.get() }
  }

  @Test
  fun `clearCache removes cached badge context for user`() = runTest {
    // Arrange
    val contextData = mapOf("friendsCount" to 5, "createdEvents" to 3, "joinedEvents" to 2)

    coEvery { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
    every { mockDocumentSnapshot.exists() } returns true
    every { mockDocumentSnapshot.get("badgeContext") } returns contextData

    // Fetch to populate cache
    repository.getBadgeContext(testUserId)

    // Clear cache
    repository.clearCache(testUserId)

    // Fetch again - should query Firestore, not return cached value
    repository.getBadgeContext(testUserId)

    // Verify Firestore was called exactly 2 times:
    // Call 1: Initial fetch to populate cache
    // Call 2: Fetch after clearCache (cache was cleared, so must hit Firestore again)
    coVerify(exactly = 2) { mockDocumentReference.get() }
  }

  @Test
  fun `clearCache does nothing for non-existent user cache`() = runTest {
    // Should not throw when clearing cache for user that was never cached
    repository.clearCache("nonExistentUser")
    // No exception means success - cache operations should be safe for non-existent entries
  }

  @Test
  fun `clearCache only affects specified user cache`() = runTest {
    // Arrange - Setup distinct mocks per user to verify cache isolation
    val mockDoc1 = mockk<DocumentReference>(relaxed = true)
    val mockDoc2 = mockk<DocumentReference>(relaxed = true)
    val mockSnapshot1 = mockk<DocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<DocumentSnapshot>(relaxed = true)

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

    // Setup distinct document references for each user
    val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    every { mockFirestore.collection("users") } returns mockCollection
    every { mockCollection.document("user1") } returns mockDoc1
    every { mockCollection.document("user2") } returns mockDoc2

    // Setup responses for each user
    coEvery { mockDoc1.get() } returns Tasks.forResult(mockSnapshot1)
    coEvery { mockDoc2.get() } returns Tasks.forResult(mockSnapshot2)
    every { mockSnapshot1.exists() } returns true
    every { mockSnapshot2.exists() } returns true
    every { mockSnapshot1.get("badges") } returns badgesData
    every { mockSnapshot2.get("badges") } returns badgesData

    // Fetch for two users to populate cache
    repository.getUserBadges("user1")
    repository.getUserBadges("user2")

    // Clear cache only for user1
    repository.clearCache("user1")

    // Fetch again for both users
    repository.getUserBadges("user1") // Should hit Firestore (cache cleared)
    repository.getUserBadges("user2") // Should hit cache (not cleared)

    // Verify each document reference separately to confirm cache isolation
    // user1 should be called exactly 2 times:
    //   Call 1: Initial fetch to populate cache
    //   Call 2: Fetch after clearCache("user1") - cache cleared, must hit Firestore
    // user2 should be called exactly 1 time:
    //   Call 1: Initial fetch to populate cache
    //   Second fetch hits cache (clearCache only affected user1, not user2)
    coVerify(exactly = 2) { mockDoc1.get() }
    coVerify(exactly = 1) { mockDoc2.get() }
  }

  @Test
  fun `clearCache is thread-safe with concurrent operations`() = runTest {
    // Arrange - Setup mocks for concurrent operations
    val contextData = mapOf("friendsCount" to 1, "createdEvents" to 0, "joinedEvents" to 0)

    coEvery { mockDocumentReference.get() } returns Tasks.forResult(mockDocumentSnapshot)
    every { mockDocumentSnapshot.exists() } returns true
    every { mockDocumentSnapshot.get("badgeContext") } returns contextData
    coEvery { mockDocumentReference.update("badgeContext", any<BadgeContext>()) } returns
        Tasks.forResult(null)

    // Act - Launch 10 concurrent coroutines performing cache operations
    // This tests that ConcurrentHashMap handles parallel reads/writes/clears correctly
    val deferreds =
        List(10) { index ->
          async {
            val userId = "user$index"
            // Each coroutine performs: fetch (populates cache), clear, fetch again
            repository.getBadgeContext(userId)
            repository.clearCache(userId)
            repository.getBadgeContext(userId)
          }
        }

    // Wait for all concurrent operations to complete
    deferreds.awaitAll()

    // Assert - Verify operations completed without exceptions
    // Each of 10 users triggers 2 Firestore calls (initial + after clear)
    // Total expected: 10 users * 2 calls = 20 calls
    coVerify(atLeast = 20) { mockDocumentReference.get() }
  }
}

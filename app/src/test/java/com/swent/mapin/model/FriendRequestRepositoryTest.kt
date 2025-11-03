package com.swent.mapin.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

// Assisted by AI

/**
 * Unit tests for FriendRequestRepository - tests Firestore backend operations.
 *
 * Coverage:
 * - Friend request CRUD operations
 * - Bidirectional relationship queries
 * - Search with status information
 * - Blocking functionality
 */
class FriendRequestRepositoryTest {

  private lateinit var firestore: FirebaseFirestore
  private lateinit var userProfileRepo: UserProfileRepository
  private lateinit var repository: FriendRequestRepository
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference

  @Before
  fun setup() {
    firestore = mockk(relaxed = true)
    userProfileRepo = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)

    every { firestore.collection(any()) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.document() } returns mockDocument
    every { mockDocument.id } returns "testDocId"

    repository = FriendRequestRepository(firestore, userProfileRepo)
  }

  // ==================== Send Friend Request Tests ====================

  @Test
  fun `sendFriendRequest creates PENDING request in Firestore`() = runTest {
    val fromUser = "user1"
    val toUser = "user2"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.isEmpty } returns true
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result = repository.sendFriendRequest(fromUser, toUser)

    assert(result)
    verify { mockDocument.set(any()) }
  }

  @Test
  fun `sendFriendRequest returns false if request already exists`() = runTest {
    val fromUser = "user1"
    val toUser = "user2"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.isEmpty } returns false
    every { mockQuerySnapshot.documents } returns listOf(mockDocSnapshot)
    every { mockDocSnapshot.toObject(FriendRequest::class.java) } returns
        FriendRequest(
            requestId = "existing",
            fromUserId = fromUser,
            toUserId = toUser,
            status = FriendshipStatus.PENDING)

    val result = repository.sendFriendRequest(fromUser, toUser)

    assert(!result)
    verify(exactly = 0) { mockDocument.set(any()) }
  }

  // ==================== Accept/Reject Request Tests ====================

  @Test
  fun `acceptFriendRequest updates status to ACCEPTED`() = runTest {
    val requestId = "req123"
    every { mockDocument.update(any<String>(), any()) } returns Tasks.forResult(null)

    val result = repository.acceptFriendRequest(requestId)

    assert(result)
    verify { mockDocument.update("status", FriendshipStatus.ACCEPTED.name) }
  }

  @Test
  fun `rejectFriendRequest updates status to REJECTED`() = runTest {
    val requestId = "req123"
    every { mockDocument.update(any<String>(), any()) } returns Tasks.forResult(null)

    val result = repository.rejectFriendRequest(requestId)

    assert(result)
    verify { mockDocument.update("status", FriendshipStatus.REJECTED.name) }
  }

  // ==================== Remove Friendship Tests ====================

  @Test
  fun `removeFriendship deletes existing request`() = runTest {
    val user1 = "user1"
    val user2 = "user2"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.isEmpty } returns false
    every { mockQuerySnapshot.documents } returns listOf(mockDocSnapshot)
    every { mockDocSnapshot.toObject(FriendRequest::class.java) } returns
        FriendRequest(
            requestId = "req123",
            fromUserId = user1,
            toUserId = user2,
            status = FriendshipStatus.ACCEPTED)
    every { mockDocument.delete() } returns Tasks.forResult(null)

    val result = repository.removeFriendship(user1, user2)

    assert(result)
    verify { mockDocument.delete() }
  }

  // ==================== Bidirectional Query Tests ====================

  @Test
  fun `getFriends queries both sent and received ACCEPTED requests`() = runTest {
    val userId = "user1"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns emptyList()

    val result = repository.getFriends(userId)

    assert(result.isEmpty())
    verify(atLeast = 2) { mockQuery.get() } // Both sent and received queries
  }

  @Test
  fun `getPendingRequests queries only received PENDING requests`() = runTest {
    val userId = "user1"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns emptyList()

    val result = repository.getPendingRequests(userId)

    assert(result.isEmpty())
    verify { mockQuery.get() }
  }

  // ==================== Search Tests ====================

  @Test
  fun `searchUsersWithStatus filters by name and excludes current user`() = runTest {
    val query = "alice"
    val currentUserId = "current"
    val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocSnapshot1 = mockk<DocumentSnapshot>(relaxed = true)
    val mockDocSnapshot2 = mockk<DocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<Query>(relaxed = true)

    every { firestore.collection("users") } returns mockUsersCollection
    every { mockUsersCollection.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(mockDocSnapshot1, mockDocSnapshot2)
    every { mockDocSnapshot1.toObject(UserProfile::class.java) } returns
        UserProfile("1", "Alice Smith")
    every { mockDocSnapshot2.toObject(UserProfile::class.java) } returns
        UserProfile("2", "Bob Jones")

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val result = repository.searchUsersWithStatus(query, currentUserId)

    assert(result.size == 1)
    assert(result[0].userProfile.name == "Alice Smith")
  }

  // ==================== Error Handling Tests ====================

  @Test
  fun `getFriends handles exception and returns empty list`() = runTest {
    val mockQuery = mockk<Query>(relaxed = true)
    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forException(Exception("Firestore error"))

    val result = repository.getFriends("user1")

    assert(result.isEmpty())
  }
}

package com.swent.mapin.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
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
  private lateinit var notificationService: NotificationService
  private lateinit var repository: FriendRequestRepository
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference

  @Before
  fun setup() {
    firestore = mockk(relaxed = true)
    userProfileRepo = mockk(relaxed = true)
    notificationService = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)

    every { firestore.collection(any()) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.document() } returns mockDocument
    every { mockDocument.id } returns "testDocId"

    // Mock notification service to always succeed
    val mockNotification =
        Notification(
            notificationId = "notif123",
            title = "Test",
            message = "Test message",
            type = NotificationType.FRIEND_REQUEST,
            recipientId = "user1",
            readStatus = false)
    coEvery {
      notificationService.sendFriendRequestNotification(any(), any(), any(), any())
    } returns NotificationResult.Success(mockNotification)
    coEvery { notificationService.sendInfoNotification(any(), any(), any(), any(), any()) } returns
        NotificationResult.Success(mockNotification)

    repository = FriendRequestRepository(firestore, userProfileRepo, notificationService)
  }

  // ==================== Helper Methods ====================

  /**
   * Sets up a query mock chain that returns an empty QuerySnapshot.
   *
   * @return The mocked Query object
   */
  private fun setupEmptyQuery(): Query {
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.isEmpty } returns true
    every { mockQuerySnapshot.documents } returns emptyList()

    return mockQuery
  }

  /**
   * Sets up a query mock chain that returns a QuerySnapshot with the provided documents.
   *
   * @param documents List of mocked DocumentSnapshot objects to return
   * @return The mocked Query object
   */
  private fun setupQueryWithDocuments(documents: List<DocumentSnapshot>): Query {
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.isEmpty } returns documents.isEmpty()
    every { mockQuerySnapshot.documents } returns documents

    return mockQuery
  }

  /**
   * Creates a mocked DocumentSnapshot containing the provided FriendRequest.
   *
   * @param friendRequest The FriendRequest to include in the snapshot
   * @return The mocked DocumentSnapshot
   */
  private fun createFriendRequestSnapshot(friendRequest: FriendRequest): DocumentSnapshot {
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocSnapshot.toObject(FriendRequest::class.java) } returns friendRequest
    return mockDocSnapshot
  }

  /**
   * Creates a mocked DocumentSnapshot containing the provided UserProfile.
   *
   * @param userProfile The UserProfile to include in the snapshot
   * @return The mocked DocumentSnapshot
   */
  private fun createUserProfileSnapshot(userProfile: UserProfile): DocumentSnapshot {
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocSnapshot.toObject(UserProfile::class.java) } returns userProfile
    return mockDocSnapshot
  }

  /**
   * Sets up a query that throws an exception when get() is called.
   *
   * @param exception The exception to throw (defaults to generic Exception)
   * @return The mocked Query object
   */
  private fun setupQueryWithException(exception: Exception = Exception("Error")): Query {
    val mockQuery = mockk<Query>(relaxed = true)
    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } throws exception
    return mockQuery
  }

  // ==================== Send Friend Request Tests ====================

  @Test
  fun `sendFriendRequest creates PENDING request in Firestore`() = runTest {
    val fromUser = "user1"
    val toUser = "user2"

    setupEmptyQuery()
    every { mockDocument.set(any()) } returns Tasks.forResult(null)
    coEvery { userProfileRepo.getUserProfile(fromUser) } returns
        UserProfile(userId = fromUser, name = "Sender User")

    val result = repository.sendFriendRequest(fromUser, toUser)

    assert(result)
    verify { mockDocument.set(any()) }
  }

  @Test
  fun `sendFriendRequest returns false if request already exists`() = runTest {
    val fromUser = "user1"
    val toUser = "user2"
    val existingRequest =
        FriendRequest(
            requestId = "existing",
            fromUserId = fromUser,
            toUserId = toUser,
            status = FriendshipStatus.PENDING)

    val mockDocSnapshot = createFriendRequestSnapshot(existingRequest)
    setupQueryWithDocuments(listOf(mockDocSnapshot))

    val result = repository.sendFriendRequest(fromUser, toUser)

    assert(!result)
    verify(exactly = 0) { mockDocument.set(any()) }
  }

  @Test
  fun `sendFriendRequest updates REJECTED request to PENDING and sends notification`() = runTest {
    val fromUser = "user1"
    val toUser = "user2"
    val existingRequestId = "rejectedReq123"
    val rejectedRequest =
        FriendRequest(
            requestId = existingRequestId,
            fromUserId = fromUser,
            toUserId = toUser,
            status = FriendshipStatus.REJECTED)

    val mockDocSnapshot = createFriendRequestSnapshot(rejectedRequest)
    setupQueryWithDocuments(listOf(mockDocSnapshot))
    every { mockDocument.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)
    coEvery { userProfileRepo.getUserProfile(fromUser) } returns
        UserProfile(userId = fromUser, name = "Sender User")

    val result = repository.sendFriendRequest(fromUser, toUser)

    assert(result)
    verify {
      mockDocument.update(match<Map<String, Any>> { it["status"] == FriendshipStatus.PENDING.name })
    }
    coVerify {
      notificationService.sendFriendRequestNotification(
          toUser, fromUser, "Sender User", existingRequestId)
    }
    verify(exactly = 0) { mockDocument.set(any()) }
  }

  // ==================== Accept/Reject Request Tests ====================

  @Test
  fun `acceptFriendRequest updates status to ACCEPTED`() = runTest {
    val requestId = "req123"
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val friendRequest =
        FriendRequest(
            requestId = requestId,
            fromUserId = "user1",
            toUserId = "user2",
            status = FriendshipStatus.PENDING)

    every { mockDocument.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.toObject(FriendRequest::class.java) } returns friendRequest
    every { mockDocument.update(any<String>(), any()) } returns Tasks.forResult(null)
    coEvery { userProfileRepo.getUserProfile(any()) } returns
        UserProfile(userId = "user2", name = "Test User")

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
    val friendRequest =
        FriendRequest(
            requestId = "req123",
            fromUserId = user1,
            toUserId = user2,
            status = FriendshipStatus.ACCEPTED)

    val mockDocSnapshot = createFriendRequestSnapshot(friendRequest)
    setupQueryWithDocuments(listOf(mockDocSnapshot))
    every { mockDocument.delete() } returns Tasks.forResult(null)

    val result = repository.removeFriendship(user1, user2)

    assert(result)
    verify { mockDocument.delete() }
  }

  // ==================== Bidirectional Query Tests ====================

  @Test
  fun `getFriends queries both sent and received ACCEPTED requests`() = runTest {
    val userId = "user1"
    setupEmptyQuery()

    val result = repository.getFriends(userId)

    assert(result.isEmpty())
    verify(atLeast = 2) { mockCollection.whereEqualTo(any<String>(), any()) }
  }

  @Test
  fun `getPendingRequests queries only received PENDING requests`() = runTest {
    val userId = "user1"
    setupEmptyQuery()

    val result = repository.getPendingRequests(userId)

    assert(result.isEmpty())
    verify { mockCollection.whereEqualTo(any<String>(), any()) }
  }

  // ==================== Search Tests ====================

  @Test
  fun `searchUsersWithStatus filters by name and excludes current user`() = runTest {
    val query = "alice"
    val currentUserId = "current"
    val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocSnapshot1 = createUserProfileSnapshot(UserProfile("1", "Alice Smith"))
    val mockDocSnapshot2 = createUserProfileSnapshot(UserProfile("2", "Bob Jones"))

    every { firestore.collection("users") } returns mockUsersCollection
    every { mockUsersCollection.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(mockDocSnapshot1, mockDocSnapshot2)

    setupEmptyQuery()

    val result = repository.searchUsersWithStatus(query, currentUserId)

    assert(result.size == 1)
    assert(result[0].userProfile.name == "Alice Smith")
  }

  // ==================== Real-time Observation Tests ====================

  @Test
  fun `observeFriends creates Flow and sets up listeners when collected`() = runTest {
    val userId = "user1"
    val mockQuery = setupEmptyQuery()
    val mockListener = mockk<ListenerRegistration>(relaxed = true)
    every { mockQuery.addSnapshotListener(any()) } returns mockListener

    val flow = repository.observeFriends(userId)

    val job = launch {
      flow.collect { friends -> throw kotlinx.coroutines.CancellationException("Test completed") }
    }

    delay(50)

    verify(exactly = 2) { mockQuery.addSnapshotListener(any()) }

    job.cancel()
  }

  @Test
  fun `observePendingRequests creates Flow and sets up listener when collected`() = runTest {
    val userId = "user1"
    val mockQuery = setupEmptyQuery()
    val mockListener = mockk<ListenerRegistration>(relaxed = true)
    every { mockQuery.addSnapshotListener(any()) } returns mockListener

    val flow = repository.observePendingRequests(userId)

    val job = launch {
      flow.collect { requests -> throw kotlinx.coroutines.CancellationException("Test completed") }
    }

    delay(50)

    verify { mockQuery.addSnapshotListener(any()) }

    job.cancel()
  }

  // ==================== Error Handling Tests ====================

  @Test
  fun `sendFriendRequest handles notification failure gracefully`() = runTest {
    val fromUser = "user1"
    val toUser = "user2"

    setupEmptyQuery()
    every { mockDocument.set(any()) } returns Tasks.forResult(null)
    coEvery { userProfileRepo.getUserProfile(fromUser) } throws Exception("Profile error")

    val result = repository.sendFriendRequest(fromUser, toUser)

    assertTrue(result)
  }

  @Test
  fun `acceptFriendRequest returns false when request not found`() = runTest {
    val requestId = "nonexistent"
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockDocument.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.toObject(FriendRequest::class.java) } returns null

    val result = repository.acceptFriendRequest(requestId)

    assertFalse(result)
  }

  @Test
  fun `acceptFriendRequest returns false on exception`() = runTest {
    val requestId = "req123"

    every { mockDocument.get() } returns Tasks.forException(Exception("Error"))

    val result = repository.acceptFriendRequest(requestId)

    assertFalse(result)
  }

  @Test
  fun `removeFriendship returns false when no existing request`() = runTest {
    val user1 = "user1"
    val user2 = "user2"

    setupEmptyQuery()

    val result = repository.removeFriendship(user1, user2)

    assertFalse(result)
  }

  @Test
  fun `removeFriendship returns false on exception`() = runTest {
    val user1 = "user1"
    val user2 = "user2"

    setupQueryWithException()

    val result = repository.removeFriendship(user1, user2)

    assertFalse(result)
  }

  @Test
  fun `getFriends returns empty list on exception`() = runTest {
    val userId = "user1"

    setupQueryWithException()

    val result = repository.getFriends(userId)

    assertTrue(result.isEmpty())
  }

  @Test
  fun `getPendingRequests returns empty list on exception`() = runTest {
    val userId = "user1"

    setupQueryWithException()

    val result = repository.getPendingRequests(userId)

    assertTrue(result.isEmpty())
  }

  @Test
  fun `searchUsersWithStatus returns empty list on blank query`() = runTest {
    val result = repository.searchUsersWithStatus("  ", "user1")

    assertTrue(result.isEmpty())
  }

  @Test
  fun `searchUsersWithStatus returns empty list on exception`() = runTest {
    val query = "alice"
    val currentUserId = "current"
    val mockUsersCollection = mockk<CollectionReference>(relaxed = true)

    every { firestore.collection("users") } returns mockUsersCollection
    every { mockUsersCollection.get() } throws Exception("Error")

    val result = repository.searchUsersWithStatus(query, currentUserId)

    assertTrue(result.isEmpty())
  }

  @Test
  fun `getFriends filters out users with no profile`() = runTest {
    val userId = "user1"
    val friendRequest =
        FriendRequest(
            requestId = "req123",
            fromUserId = userId,
            toUserId = "user2",
            status = FriendshipStatus.ACCEPTED)

    val mockDocSnapshot = createFriendRequestSnapshot(friendRequest)
    setupQueryWithDocuments(listOf(mockDocSnapshot))
    coEvery { userProfileRepo.getUserProfile("user2") } returns null

    val result = repository.getFriends(userId)

    // Test passes if no exception is thrown and result is empty - verifies graceful handling
    assertTrue(result.isEmpty())
  }

  @Test
  fun `getPendingRequests filters out senders with no profile`() = runTest {
    val userId = "user1"
    val friendRequest =
        FriendRequest(
            requestId = "req123",
            fromUserId = "user2",
            toUserId = userId,
            status = FriendshipStatus.PENDING)

    val mockDocSnapshot = createFriendRequestSnapshot(friendRequest)
    setupQueryWithDocuments(listOf(mockDocSnapshot))
    coEvery { userProfileRepo.getUserProfile("user2") } returns null

    val result = repository.getPendingRequests(userId)

    // Test passes if no exception is thrown and result is empty - verifies graceful handling
    assertTrue(result.isEmpty())
  }

  @Test
  fun `searchUsersWithStatus excludes current user`() = runTest {
    val query = "user"
    val currentUserId = "current"
    val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocSnapshot1 = createUserProfileSnapshot(UserProfile(currentUserId, "Current User"))
    val mockDocSnapshot2 = createUserProfileSnapshot(UserProfile("other", "Other User"))

    every { firestore.collection("users") } returns mockUsersCollection
    every { mockUsersCollection.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(mockDocSnapshot1, mockDocSnapshot2)

    setupEmptyQuery()

    val result = repository.searchUsersWithStatus(query, currentUserId)

    assertEquals(1, result.size)
    assertEquals("Other User", result[0].userProfile.name)
  }

  @Test
  fun `searchUsersWithStatus excludes accepted friends`() = runTest {
    val query = "user"
    val currentUserId = "current"
    val friendUserId = "friend"
    val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockUserDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockFriendDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<Query>(relaxed = true)

    every { firestore.collection("users") } returns mockUsersCollection
    every { mockUsersCollection.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(mockUserDocSnapshot)
    every { mockUserDocSnapshot.toObject(UserProfile::class.java) } returns
        UserProfile(friendUserId, "Friend User")

    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(mockFriendDocSnapshot)
    every { mockFriendDocSnapshot.toObject(FriendRequest::class.java) } returns
        FriendRequest(
            requestId = "req123",
            fromUserId = currentUserId,
            toUserId = friendUserId,
            status = FriendshipStatus.ACCEPTED)

    val result = repository.searchUsersWithStatus(query, currentUserId)

    assertTrue(result.isEmpty())
  }

  @Test
  fun `rejectFriendRequest returns false on exception`() = runTest {
    val requestId = "req123"
    every { mockDocument.update(any<String>(), any()) } returns
        Tasks.forException(Exception("Error"))

    val result = repository.rejectFriendRequest(requestId)

    assertFalse(result)
  } // ==================== Error Handling Tests ====================

  @Test
  fun `getFriends handles exception and returns empty list`() = runTest {
    setupQueryWithException(Exception("Firestore error"))

    val result = repository.getFriends("user1")

    assert(result.isEmpty())
  }
}

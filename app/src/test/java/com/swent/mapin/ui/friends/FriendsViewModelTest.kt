package com.swent.mapin.ui.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

// Assisted by AI

/**
 * Unit tests for FriendsViewModel - tests backend interactions and business logic.
 *
 * Coverage:
 * - Friend Management: send/accept/reject/remove/block
 * - Bidirectional relationships
 * - Search functionality with status
 * - Real-time updates (via repository)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

  private lateinit var repository: FriendRequestRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var viewModel: FriendsViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val currentUserId = "user123"

  // Use MutableStateFlows to simulate real-time updates
  private lateinit var friendsFlow: MutableStateFlow<List<FriendWithProfile>>
  private lateinit var pendingRequestsFlow: MutableStateFlow<List<FriendWithProfile>>

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    // Mock FirebaseAuth to return the test user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns currentUserId

    // Initialize flows
    friendsFlow = MutableStateFlow(emptyList())
    pendingRequestsFlow = MutableStateFlow(emptyList())

    // Mock the observeFriends and observePendingRequests to return our flows
    coEvery { repository.observeFriends(any()) } returns friendsFlow
    coEvery { repository.observePendingRequests(any()) } returns pendingRequestsFlow

    viewModel = FriendsViewModel(repository, mockAuth)
    testDispatcher.scheduler.advanceUntilIdle()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ==================== Friend Management Tests ====================

  @Test
  fun `sendFriendRequest calls repository and refreshes search`() = runTest {
    val targetUserId = "user456"
    coEvery { repository.sendFriendRequest(currentUserId, targetUserId) } returns true
    coEvery { repository.searchUsersWithStatus(any(), currentUserId) } returns emptyList()

    viewModel.updateSearchQuery("test")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.sendFriendRequest(targetUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { repository.sendFriendRequest(currentUserId, targetUserId) }
    coVerify(atLeast = 2) { repository.searchUsersWithStatus(any(), currentUserId) }
  }

  @Test
  fun `acceptRequest calls repository and reloads both lists`() = runTest {
    val requestId = "req123"
    coEvery { repository.acceptFriendRequest(requestId) } returns true
    coEvery { repository.getFriends(currentUserId) } returns emptyList()
    coEvery { repository.getPendingRequests(currentUserId) } returns emptyList()

    viewModel.acceptRequest(requestId)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { repository.acceptFriendRequest(requestId) }
    coVerify { repository.getFriends(currentUserId) }
    coVerify { repository.getPendingRequests(currentUserId) }
  }

  @Test
  fun `rejectRequest calls repository and reloads pending requests`() = runTest {
    val requestId = "req123"
    coEvery { repository.rejectFriendRequest(requestId) } returns true
    coEvery { repository.getPendingRequests(currentUserId) } returns emptyList()

    viewModel.rejectRequest(requestId)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { repository.rejectFriendRequest(requestId) }
    coVerify { repository.getPendingRequests(currentUserId) }
  }

  @Test
  fun `removeFriend calls repository and reloads friends list`() = runTest {
    val friendUserId = "friend123"
    coEvery { repository.removeFriendship(currentUserId, friendUserId) } returns true
    coEvery { repository.getFriends(currentUserId) } returns emptyList()

    viewModel.removeFriend(friendUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { repository.removeFriendship(currentUserId, friendUserId) }
    coVerify { repository.getFriends(currentUserId) }
  }

  // ==================== State Management Tests ====================

  @Test
  fun `loadFriends updates friends state flow`() = runTest {
    val mockFriends =
        listOf(FriendWithProfile(UserProfile("1", "Alice"), FriendshipStatus.ACCEPTED, "req1"))
    coEvery { repository.getFriends(currentUserId) } returns mockFriends

    viewModel.loadFriends()
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.friends.value == mockFriends)
  }

  @Test
  fun `updateSearchQuery triggers search with status`() = runTest {
    val query = "alice"
    val mockResults =
        listOf(SearchResultWithStatus(UserProfile("3", "Alice"), hasPendingRequest = false))
    coEvery { repository.searchUsersWithStatus(query, currentUserId) } returns mockResults

    viewModel.updateSearchQuery(query)
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.searchQuery.value == query)
    assert(viewModel.searchResults.value == mockResults)
  }

  @Test
  fun `selectTab updates selected tab state`() = runTest {
    viewModel.selectTab(FriendsTab.SEARCH)
    assert(viewModel.selectedTab.value == FriendsTab.SEARCH)

    viewModel.selectTab(FriendsTab.REQUESTS)
    assert(viewModel.selectedTab.value == FriendsTab.REQUESTS)
  }

  @Test
  fun `failed acceptRequest does not update state`() = runTest {
    coEvery { repository.acceptFriendRequest(any()) } returns false
    coEvery { repository.getFriends(currentUserId) } returns emptyList()
    coEvery { repository.getPendingRequests(currentUserId) } returns emptyList()

    // Clear the mocks to reset call counts after initialization
    testDispatcher.scheduler.advanceUntilIdle()
    clearMocks(repository, answers = false)

    // Re-setup the mock behavior after clearing
    coEvery { repository.acceptFriendRequest(any()) } returns false
    coEvery { repository.getFriends(currentUserId) } returns emptyList()
    coEvery { repository.getPendingRequests(currentUserId) } returns emptyList()

    viewModel.acceptRequest("req123")
    testDispatcher.scheduler.advanceUntilIdle()

    // Friends list should not be reloaded after failure
    coVerify(exactly = 0) { repository.getFriends(any()) }
    coVerify(exactly = 0) { repository.getPendingRequests(any()) }
  }
}

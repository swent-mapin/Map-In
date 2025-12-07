package com.swent.mapin.ui.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.FriendshipStatus
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.EventRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSheetViewModelTest {

  private lateinit var viewModel: ProfileSheetViewModel
  private lateinit var mockUserRepo: UserProfileRepository
  private lateinit var mockEventRepo: EventRepository
  private lateinit var mockFriendRepo: FriendRequestRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private val testDispatcher = UnconfinedTestDispatcher()

  private val currentUserId = "current-user"
  private val targetUserId = "target-user"
  private val testProfile =
      UserProfile(userId = targetUserId, name = "Target User", followerIds = listOf("follower1"))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockUserRepo = mockk(relaxed = true)
    mockEventRepo = mockk(relaxed = true)
    mockFriendRepo = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns currentUserId
    coEvery { mockEventRepo.getOwnedEvents(any()) } returns emptyList()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
      ProfileSheetViewModel(mockUserRepo, mockEventRepo, mockFriendRepo, mockAuth)

  @Test
  fun `loadProfile sets Error state when profile not found`() = runTest {
    coEvery { mockUserRepo.getUserProfile(targetUserId) } returns null
    viewModel = createViewModel()

    viewModel.loadProfile(targetUserId)

    assertTrue(viewModel.state is ProfileSheetState.Error)
    assertEquals("Profile not found", (viewModel.state as ProfileSheetState.Error).message)
  }

  @Test
  fun `loadProfile sets Loaded state with correct data`() = runTest {
    coEvery { mockUserRepo.getUserProfile(targetUserId) } returns testProfile
    coEvery { mockUserRepo.isFollowing(currentUserId, targetUserId) } returns false
    coEvery { mockFriendRepo.getFriendshipStatus(currentUserId, targetUserId) } returns null
    viewModel = createViewModel()

    viewModel.loadProfile(targetUserId)

    val state = viewModel.state as ProfileSheetState.Loaded
    assertEquals(testProfile, state.profile)
    assertFalse(state.isFollowing)
    assertFalse(state.isOwnProfile)
    assertEquals(FriendStatus.NOT_FRIEND, state.friendStatus)
  }

  @Test
  fun `loadProfile marks isOwnProfile true when viewing own profile`() = runTest {
    val ownProfile = UserProfile(userId = currentUserId, name = "Me")
    coEvery { mockUserRepo.getUserProfile(currentUserId) } returns ownProfile
    coEvery { mockFriendRepo.getFriendshipStatus(any(), any()) } returns null
    viewModel = createViewModel()

    viewModel.loadProfile(currentUserId)

    val state = viewModel.state as ProfileSheetState.Loaded
    assertTrue(state.isOwnProfile)
    assertFalse(state.isFollowing)
  }

  @Test
  fun `loadProfile sets Error state on exception`() = runTest {
    coEvery { mockUserRepo.getUserProfile(targetUserId) } throws RuntimeException("Network error")
    viewModel = createViewModel()

    viewModel.loadProfile(targetUserId)

    assertTrue(viewModel.state is ProfileSheetState.Error)
    assertEquals("Network error", (viewModel.state as ProfileSheetState.Error).message)
  }

  @Test
  fun `toggleFollow calls followUser when not following`() = runTest {
    coEvery { mockUserRepo.getUserProfile(targetUserId) } returns testProfile
    coEvery { mockUserRepo.isFollowing(currentUserId, targetUserId) } returns false
    coEvery { mockUserRepo.followUser(currentUserId, targetUserId) } returns true
    coEvery { mockFriendRepo.getFriendshipStatus(currentUserId, targetUserId) } returns null
    viewModel = createViewModel()
    viewModel.loadProfile(targetUserId)

    viewModel.toggleFollow()

    coVerify { mockUserRepo.followUser(currentUserId, targetUserId) }
  }

  @Test
  fun `friend status maps to pending`() = runTest {
    coEvery { mockUserRepo.getUserProfile(targetUserId) } returns testProfile
    coEvery { mockUserRepo.isFollowing(currentUserId, targetUserId) } returns false
    coEvery { mockFriendRepo.getFriendshipStatus(currentUserId, targetUserId) } returns
        FriendshipStatus.PENDING
    viewModel = createViewModel()

    viewModel.loadProfile(targetUserId)

    val state = viewModel.state as ProfileSheetState.Loaded
    assertEquals(FriendStatus.PENDING, state.friendStatus)
  }

  @Test
  fun `sendFriendRequest triggers repository`() = runTest {
    coEvery { mockUserRepo.getUserProfile(targetUserId) } returns testProfile
    coEvery { mockUserRepo.isFollowing(currentUserId, targetUserId) } returns false
    coEvery { mockFriendRepo.getFriendshipStatus(currentUserId, targetUserId) } returns null
    coEvery { mockFriendRepo.sendFriendRequest(currentUserId, targetUserId) } returns true
    viewModel = createViewModel()

    viewModel.loadProfile(targetUserId)
    viewModel.sendFriendRequest()

    coVerify { mockFriendRepo.sendFriendRequest(currentUserId, targetUserId) }
  }

  @Test
  fun `toggleFollow calls unfollowUser when already following`() = runTest {
    coEvery { mockUserRepo.getUserProfile(targetUserId) } returns testProfile
    coEvery { mockUserRepo.isFollowing(currentUserId, targetUserId) } returns true
    coEvery { mockUserRepo.unfollowUser(currentUserId, targetUserId) } returns true
    viewModel = createViewModel()
    viewModel.loadProfile(targetUserId)

    viewModel.toggleFollow()

    coVerify { mockUserRepo.unfollowUser(currentUserId, targetUserId) }
  }

  @Test
  fun `toggleFollow does nothing when viewing own profile`() = runTest {
    val ownProfile = UserProfile(userId = currentUserId, name = "Me")
    coEvery { mockUserRepo.getUserProfile(currentUserId) } returns ownProfile
    viewModel = createViewModel()
    viewModel.loadProfile(currentUserId)

    viewModel.toggleFollow()

    coVerify(exactly = 0) { mockUserRepo.followUser(any(), any()) }
    coVerify(exactly = 0) { mockUserRepo.unfollowUser(any(), any()) }
  }
}

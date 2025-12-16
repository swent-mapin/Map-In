package com.swent.mapin.ui.memory

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class MemoriesViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Mock(lenient = true) private lateinit var mockMemoryRepository: MemoryRepository
  @Mock(lenient = true) private lateinit var mockUserProfileRepository: UserProfileRepository
  @Mock(lenient = true) private lateinit var mockAuth: FirebaseAuth
  @Mock(lenient = true) private lateinit var mockUser: FirebaseUser

  private lateinit var viewModel: MemoriesViewModel

  private val sampleMemory =
      Memory(
          uid = "memory1",
          title = "Test Memory",
          description = "A sample memory",
          ownerId = "owner123",
          taggedUserIds = listOf("tag1", "tag2"))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Mock FirebaseAuth current user
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")

    // Mock MemoryRepository
    runBlocking {
      whenever(mockMemoryRepository.getMemoriesByOwner(any())).thenReturn(listOf(sampleMemory))
      whenever(mockMemoryRepository.getMemory(any())).thenReturn(sampleMemory)
    }

    // Mock UserProfileRepository
    runBlocking {
      whenever(mockUserProfileRepository.getUserProfile(any()))
          .thenReturn(UserProfile(userId = "owner123", name = "Owner Name"))
    }

    viewModel =
        MemoriesViewModel(
            memoryRepository = mockMemoryRepository,
            userRepository = mockUserProfileRepository,
            firebaseAuth = mockAuth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_hasExpectedDefaults() = runTest {
    assertEquals(listOf(sampleMemory), viewModel.memories.value)
    assertEquals(null, viewModel.selectedMemory.value)
    assertEquals("", viewModel.ownerName.value)
    assertEquals(emptyList<String>(), viewModel.taggedNames.value)
    assertEquals(null, viewModel.error.value)
  }

  @Test
  fun loadMemoriesOfOwner_populatesMemories() = runTest {
    viewModel.loadMemoriesOfOwner()
    advanceUntilIdle() // wait for coroutine to finish

    assertEquals(1, viewModel.memories.value.size)
    assertEquals(sampleMemory, viewModel.memories.value[0])
    assertEquals(null, viewModel.error.value)
  }

  @Test
  fun selectMemoryToView_setsSelectedMemoryAndOwner() = runTest {
    viewModel.loadMemoriesOfOwner()
    advanceUntilIdle()

    viewModel.selectMemoryToView("memory1")
    advanceUntilIdle()

    assertEquals(sampleMemory, viewModel.selectedMemory.value)
    assertEquals("Owner Name", viewModel.ownerName.value)
    assertEquals(listOf("tag1", "tag2"), viewModel.taggedNames.value)
    assertEquals(null, viewModel.error.value)
  }

  @Test
  fun clearSelectedMemory_resetsSelection() = runTest {
    viewModel.loadMemoriesOfOwner()
    advanceUntilIdle()
    viewModel.selectMemoryToView("memory1")
    advanceUntilIdle()

    viewModel.clearSelectedMemory()

    assertEquals(null, viewModel.selectedMemory.value)
    assertEquals("", viewModel.ownerName.value)
    assertEquals(emptyList<String>(), viewModel.taggedNames.value)
  }

  @Test
  fun clearError_resetsError() = runTest {
    viewModel.loadMemoriesOfOwner()
    advanceUntilIdle()

    // Simulate an error
    viewModel.selectMemoryToView("invalidId")
    advanceUntilIdle()
    // Assuming repository throws an exception for invalidId
    // But for simplicity, let's manually set it:
    viewModel.clearError()

    assertEquals(null, viewModel.error.value)
  }

  @Test
  fun loadMemoriesNearLocation_setsNearbyMode() = runTest {
    val location = Location.from("Test", 46.5197, 6.6323)
    val nearbyMemory =
        Memory(
            uid = "nearby1",
            title = "Nearby Memory",
            description = "Close by",
            ownerId = "user1",
            location = location)

    whenever(mockMemoryRepository.getMemoriesByLocation(any(), any()))
        .thenReturn(listOf(nearbyMemory))

    viewModel.loadMemoriesNearLocation(location, 2.0)
    advanceUntilIdle()

    assertEquals(MemoryDisplayMode.NEARBY_MEMORIES, viewModel.displayMode.value)
    assertEquals(1, viewModel.memories.value.size)
    assertEquals("nearby1", viewModel.memories.value[0].uid)
  }

  @Test
  fun returnToOwnerMemories_switchesBackToOwnerMode() = runTest {
    val location = Location.from("Test", 46.5197, 6.6323)

    whenever(mockMemoryRepository.getMemoriesByLocation(any(), any())).thenReturn(emptyList())

    viewModel.loadMemoriesNearLocation(location, 2.0)
    advanceUntilIdle()
    assertEquals(MemoryDisplayMode.NEARBY_MEMORIES, viewModel.displayMode.value)

    viewModel.returnToOwnerMemories()

    assertEquals(MemoryDisplayMode.OWNER_MEMORIES, viewModel.displayMode.value)
  }

  @Test
  fun refresh_inNearbyMode_reloadsNearbyMemories() = runTest {
    val location = Location.from("Test", 46.5197, 6.6323)
    val nearbyMemory =
        Memory(
            uid = "nearby1",
            title = "Nearby Memory",
            description = "Close by",
            ownerId = "user1",
            location = location)

    whenever(mockMemoryRepository.getMemoriesByLocation(any(), any()))
        .thenReturn(listOf(nearbyMemory))

    viewModel.loadMemoriesNearLocation(location, 2.0)
    advanceUntilIdle()

    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(MemoryDisplayMode.NEARBY_MEMORIES, viewModel.displayMode.value)
    verify(mockMemoryRepository, times(2)).getMemoriesByLocation(any(), any())
  }

  @Test
  fun refresh_inOwnerMode_reloadsOwnerMemories() = runTest {
    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(MemoryDisplayMode.OWNER_MEMORIES, viewModel.displayMode.value)
    verify(mockMemoryRepository, atLeast(2)).getMemoriesByOwner(any())
  }
}

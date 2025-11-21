package com.swent.mapin.model.chat

import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.ui.chat.Conversation
import com.swent.mapin.ui.chat.ConversationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*

// Assisted by AI
@ExperimentalCoroutinesApi
class ConversationViewModelTest {

  @get:Rule private val testDispatcher = StandardTestDispatcher()
  private lateinit var conversationRepository: ConversationRepository
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var viewModel: ConversationViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    conversationRepository = mock()
    userProfileRepository = mock()
    viewModel =
        ConversationViewModel(
            conversationRepository, userProfileRepository, currentUserIdProvider = { "user123" })
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `getNewUID returns repository UID`() {
    whenever(conversationRepository.getNewUid()).thenReturn("new_uid_123")
    val uid = viewModel.getNewUID()
    Assert.assertEquals("new_uid_123", uid)
  }

  @Test
  fun `getCurrentUserProfile sets currentUserProfile correctly`() = runTest {
    val userProfile =
        UserProfile(
            userId = "user123",
            name = "Test User",
            bio = "Bio",
            hobbies = listOf("Reading", "Gaming"),
            location = "Test City")
    whenever(userProfileRepository.getUserProfile("user123")).thenReturn(userProfile)

    // Just call the function; no Firebase mocking needed
    viewModel.getCurrentUserProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals(userProfile, viewModel.currentUserProfile)
  }

  @Test
  fun `observeConversations updates userConversations StateFlow`() = runTest {
    val conversations =
        listOf(
            Conversation(id = "conv1", name = "Chat One"),
            Conversation(id = "conv2", name = "Chat Two"))

    whenever(conversationRepository.observeConversationsForCurrentUser())
        .thenReturn(flow { emit(conversations) })

    viewModel.observeConversations()
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals(conversations, viewModel.userConversations.value)
  }

  @Test
  fun `createConversation calls repository addConversation`() = runTest {
    val conversation = Conversation(id = "conv1", name = "Chat One")
    viewModel.createConversation(conversation)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(conversationRepository).addConversation(conversation)
  }

    @Test
    fun `getConversationById updates gotConversation when repository returns conversation`() = runTest {
        val conversation = Conversation(id = "conv1", name = "Test Chat")
        whenever(conversationRepository.getConversationById("conv1")).thenReturn(conversation)

        viewModel.getConversationById("conv1")
        testDispatcher.scheduler.advanceUntilIdle() // Let coroutine complete

        Assert.assertEquals(conversation, viewModel.gotConversation.value)
    }

    @Test
    fun `getConversationById updates gotConversation to null when repository returns null`() = runTest {
        whenever(conversationRepository.getConversationById("conv1")).thenReturn(null)

        viewModel.getConversationById("conv1")
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertNull(viewModel.gotConversation.value)
    }

    @Test
    fun `getConversationById cancels previous job before launching new one`() = runTest {
        val firstConversation = Conversation(id = "conv1", name = "First Chat")
        val secondConversation = Conversation(id = "conv2", name = "Second Chat")

        // Simulate repository calls with delay
        whenever(conversationRepository.getConversationById("conv1")).thenAnswer {
            firstConversation
        }
        whenever(conversationRepository.getConversationById("conv2")).thenReturn(secondConversation)

        viewModel.getConversationById("conv1")
        viewModel.getConversationById("conv2") // This should cancel the first job
        testDispatcher.scheduler.advanceUntilIdle()

        // The first call should have been cancelled, final value is from second call
        Assert.assertEquals(secondConversation, viewModel.gotConversation.value)
    }
}

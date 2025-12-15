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
  fun `getNewUID delegates to repository`() {
    whenever(conversationRepository.getNewUid(listOf("a", "b"))).thenReturn("uid_123")

    val uid = viewModel.getNewUID(listOf("a", "b"))

    Assert.assertEquals("uid_123", uid)
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
  fun `getConversationById updates gotConversation when repository returns conversation`() =
      runTest {
        val conversation = Conversation(id = "conv1", name = "Test Chat")
        whenever(conversationRepository.getConversationById("conv1")).thenReturn(conversation)

        viewModel.getConversationById("conv1")
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertEquals(conversation, viewModel.gotConversation.value)
      }

  @Test
  fun `getConversationById updates gotConversation to null when repository returns null`() =
      runTest {
        whenever(conversationRepository.getConversationById("conv1")).thenReturn(null)

        viewModel.getConversationById("conv1")
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertNull(viewModel.gotConversation.value)
      }

  @Test
  fun `getConversationById cancels previous job before launching new one`() = runTest {
    val firstConversation = Conversation(id = "conv1", name = "First Chat")
    val secondConversation = Conversation(id = "conv2", name = "Second Chat")

    whenever(conversationRepository.getConversationById("conv1")).thenReturn(firstConversation)
    whenever(conversationRepository.getConversationById("conv2")).thenReturn(secondConversation)

    viewModel.getConversationById("conv1")
    viewModel.getConversationById("conv2")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals(secondConversation, viewModel.gotConversation.value)
  }

  @Test
  fun `getExistingConversation returns conversation when it exists`() = runTest {
    val conversationId = "conv1"
    val conversation = Conversation(id = conversationId, name = "Test Chat")

    whenever(conversationRepository.conversationExists(conversationId)).thenReturn(true)
    whenever(conversationRepository.getConversationById(conversationId)).thenReturn(conversation)

    val result = viewModel.getExistingConversation(conversationId)

    Assert.assertEquals(conversation, result)
  }

  @Test
  fun `getExistingConversation returns null when conversation does not exist`() = runTest {
    val conversationId = "conv1"

    whenever(conversationRepository.conversationExists(conversationId)).thenReturn(false)

    val result = viewModel.getExistingConversation(conversationId)

    Assert.assertNull(result)
    verify(conversationRepository, never()).getConversationById(any())
  }

  @Test
  fun `getExistingConversation returns null when repository returns null`() = runTest {
    val conversationId = "conv1"

    whenever(conversationRepository.conversationExists(conversationId)).thenReturn(true)
    whenever(conversationRepository.getConversationById(conversationId)).thenReturn(null)

    val result = viewModel.getExistingConversation(conversationId)

    Assert.assertNull(result)
  }

  @Test
  fun `leaveConversation calls repository leaveConversation`() = runTest {
    val conversationId = "conv1"

    viewModel.leaveConversation(conversationId)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(conversationRepository).leaveConversation(conversationId)
  }
}

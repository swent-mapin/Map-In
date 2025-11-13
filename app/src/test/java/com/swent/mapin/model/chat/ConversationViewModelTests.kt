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
}

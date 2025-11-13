package com.swent.mapin.model.chat

import com.google.firebase.firestore.DocumentSnapshot
import com.swent.mapin.ui.chat.Message
import com.swent.mapin.ui.chat.MessageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*

// Assisted by AI
@ExperimentalCoroutinesApi
class MessageViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var messageRepository: MessageRepository
  private lateinit var viewModel: MessageViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    messageRepository = mock()
    viewModel = MessageViewModel(messageRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `observeMessages updates messages StateFlow`() = runTest {
    val dummySnapshot: DocumentSnapshot = mock()
    val messagesList =
        listOf(Message(text = "Hello", isMe = true), Message(text = "World", isMe = false))
    whenever(messageRepository.observeMessagesFlow("conv1"))
        .thenReturn(flow { emit(messagesList to dummySnapshot) })

    viewModel.observeMessages("conv1")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals(messagesList, viewModel.messages.value)
  }

  @Test
  fun `sendMessage calls repository sendMessage`() = runTest {
    viewModel.sendMessage("conv1", "Hello world")
    testDispatcher.scheduler.advanceUntilIdle()

    verify(messageRepository).sendMessage("conv1", "Hello world")
  }

  @Test
  fun `loadMoreMessages prepends messages correctly`() = runTest {
    val lastSnapshot: DocumentSnapshot = mock()
    val initialMessages = listOf(Message(text = "Recent", isMe = true))
    val moreMessages = listOf(Message(text = "Older", isMe = false))

    // Set lastVisibleMessage using reflection
    val lastVisibleField = MessageViewModel::class.java.getDeclaredField("lastVisibleMessage")
    lastVisibleField.isAccessible = true
    lastVisibleField.set(viewModel, lastSnapshot)

    // Preload existing messages
    val messagesField = MessageViewModel::class.java.getDeclaredField("_messages")
    messagesField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val messagesFlow = messagesField.get(viewModel) as MutableStateFlow<List<Message>>
    messagesFlow.value = initialMessages

    whenever(messageRepository.loadMoreMessages("conv1", lastSnapshot))
        .thenReturn(moreMessages to mock())

    viewModel.loadMoreMessages("conv1")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals(moreMessages + initialMessages, viewModel.messages.value)
  }

  @Test
  fun `error in observeMessages sets error StateFlow`() = runTest {
    whenever(messageRepository.observeMessagesFlow("conv1"))
        .thenThrow(RuntimeException("Flow error"))

    viewModel.observeMessages("conv1")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals("Flow error", viewModel.error.value)
  }

  @Test
  fun `error in sendMessage sets error StateFlow`() = runTest {
    whenever(messageRepository.sendMessage("conv1", "Hi")).thenThrow(RuntimeException("Send error"))

    viewModel.sendMessage("conv1", "Hi")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals("Send error", viewModel.error.value)
  }

  @Test
  fun `error in loadMoreMessages sets error StateFlow`() = runTest {
    val lastSnapshot: DocumentSnapshot = mock()
    val lastVisibleField = MessageViewModel::class.java.getDeclaredField("lastVisibleMessage")
    lastVisibleField.isAccessible = true
    lastVisibleField.set(viewModel, lastSnapshot)

    whenever(messageRepository.loadMoreMessages("conv1", lastSnapshot))
        .thenThrow(RuntimeException("Load more error"))

    viewModel.loadMoreMessages("conv1")
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals("Load more error", viewModel.error.value)
  }
}

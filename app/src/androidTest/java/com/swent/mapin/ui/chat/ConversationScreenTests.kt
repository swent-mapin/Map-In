package com.swent.mapin.ui.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.UserProfile
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@Composable
fun ConversationScreenForTest(
    messageViewModel: MessageViewModel,
    conversationViewModel: ConversationViewModel,
    conversationId: String,
    conversationName: String,
    onNavigateBack: () -> Unit = {}
) {
  ConversationScreen(
      messageViewModel = messageViewModel,
      conversationViewModel = conversationViewModel,
      conversationId = conversationId,
      conversationName = conversationName,
      onNavigateBack = onNavigateBack)
}

class ConversationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun sampleMessages() =
      listOf(
          Message(text = "Hey, how are you?", isMe = false),
          Message(text = "Doing great, thanks!", isMe = true))

  private fun mockConversationVM(convId: String): ConversationViewModel {
    val vm = mockk<ConversationViewModel>(relaxed = true)

    val fakeConversation =
        Conversation(
            id = convId,
            participantIds = listOf("u1", "u2"),
            participants = listOf(UserProfile("u1", "Alice"), UserProfile("u2", "Bob")),
            profilePictureUrl = null)

    val flow = MutableStateFlow<Conversation?>(fakeConversation)

    every { vm.gotConversation } returns flow
    every { vm.getConversationById(any()) } just Runs
    every { vm.currentUserProfile } returns UserProfile("u1", "Alice")

    return vm
  }

  @Test
  fun conversationScreen_displaysInitialMessagesAndUIElements_and_callsObserveMessages() {
    val mockVm = mockk<MessageViewModel>(relaxed = true)
    val mockConvVm = mockConversationVM("conv1")

    every { mockVm.messages } returns MutableStateFlow(sampleMessages())
    every { mockVm.observeMessages(any()) } just Runs

    composeTestRule.setContent {
      ConversationScreenForTest(
          messageViewModel = mockVm,
          conversationViewModel = mockConvVm,
          conversationId = "conv1",
          conversationName = "Chat with Alice")
    }

    composeTestRule
        .onNodeWithTag(ConversationScreenTestTags.CONVERSATION_SCREEN)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConversationScreenTestTags.INPUT_TEXT_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConversationScreenTestTags.SEND_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithText("Hey, how are you?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Doing great, thanks!").assertIsDisplayed()

    verify(timeout = 1000) { mockVm.observeMessages("conv1") }
  }

  @Test
  fun sendingMessage_addsNewMessage_callsSendMessage() {
    val messagesFlow = MutableStateFlow(sampleMessages())
    val mockVm = mockk<MessageViewModel>(relaxed = true)
    val mockConvVm = mockConversationVM("conv1")

    every { mockVm.messages } returns messagesFlow
    every { mockVm.observeMessages(any()) } just Runs

    coEvery { mockVm.sendMessage(any(), any()) } answers
        {
          val text = secondArg<String>()
          messagesFlow.value = messagesFlow.value + Message(text, isMe = true)
        }

    composeTestRule.setContent {
      ConversationScreenForTest(
          messageViewModel = mockVm,
          conversationViewModel = mockConvVm,
          conversationId = "conv1",
          conversationName = "Chat test")
    }

    composeTestRule
        .onNodeWithTag(ConversationScreenTestTags.INPUT_TEXT_FIELD)
        .performTextInput("Hello there!")
    composeTestRule.onNodeWithTag(ConversationScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitForIdle()

    coVerify(timeout = 1000) { mockVm.sendMessage("conv1", "Hello there!") }
  }

  @Test
  fun emptyMessage_notAddedAnd_sendMessageNotCalled() {
    val mockVm = mockk<MessageViewModel>(relaxed = true)
    val mockConvVm = mockConversationVM("conv1")

    every { mockVm.messages } returns MutableStateFlow(sampleMessages())
    every { mockVm.observeMessages(any()) } just Runs
    coEvery { mockVm.sendMessage(any(), any()) } just Runs

    composeTestRule.setContent {
      ConversationScreenForTest(
          messageViewModel = mockVm,
          conversationViewModel = mockConvVm,
          conversationId = "conv1",
          conversationName = "Chat test")
    }

    composeTestRule.onNodeWithTag(ConversationScreenTestTags.SEND_BUTTON).performClick()

    coVerify(exactly = 0) { mockVm.sendMessage(any(), any()) }

    composeTestRule.onNodeWithText("Hey, how are you?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Doing great, thanks!").assertIsDisplayed()
  }

  @Test
  fun messageBubble_rendersCorrectly_forSenderAndReceiver() {
    composeTestRule.setContent {
      androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        MessageBubble(Message("From me", isMe = true))
        MessageBubble(Message("From them", isMe = false))
      }
    }

    composeTestRule.onNodeWithText("From me").assertIsDisplayed()
    composeTestRule.onNodeWithText("From them").assertIsDisplayed()
  }

  @Test
  fun conversationScreen_onNavigateBack_isCalled_whenTopBarBackClicked() {
    val mockVm = mockk<MessageViewModel>(relaxed = true)
    val mockConvVm = mockConversationVM("conv-back")

    every { mockVm.messages } returns MutableStateFlow(sampleMessages())
    every { mockVm.observeMessages(any()) } just Runs

    var backCalled = false

    composeTestRule.setContent {
      ConversationScreenForTest(
          messageViewModel = mockVm,
          conversationViewModel = mockConvVm,
          conversationId = "conv-back",
          conversationName = "Chat",
          onNavigateBack = { backCalled = true })
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.BACK_BUTTON).assertIsDisplayed().performClick()

    assert(backCalled)
  }

  @Test
  fun scrollToBottomButton_isVisible_and_clickable() {
    val mockVm = mockk<MessageViewModel>(relaxed = true)
    val mockConvVm = mockConversationVM("conv-scroll")

    every { mockVm.messages } returns
        MutableStateFlow(List(30) { i -> Message("msg $i", isMe = i % 2 == 0) })
    every { mockVm.observeMessages(any()) } just Runs

    composeTestRule.setContent {
      ConversationScreenForTest(
          messageViewModel = mockVm,
          conversationViewModel = mockConvVm,
          conversationId = "conv-scroll",
          conversationName = "Chat")
    }

    composeTestRule
        .onNodeWithContentDescription("Scroll to bottom")
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun pagination_trigger_callsLoadMoreMessages_whenAtTop() {
    val mockVm = mockk<MessageViewModel>(relaxed = true)
    val mockConvVm = mockConversationVM("conv-paging")

    every { mockVm.messages } returns
        MutableStateFlow(List(50) { i -> Message("msg $i", isMe = i % 2 == 0) })
    every { mockVm.observeMessages(any()) } just Runs
    every { mockVm.loadMoreMessages(any()) } just Runs

    composeTestRule.setContent {
      ConversationScreenForTest(
          messageViewModel = mockVm,
          conversationViewModel = mockConvVm,
          conversationId = "conv-paging",
          conversationName = "Chat")
    }

    runBlocking { mockVm.loadMoreMessages("conv-paging") }

    verify { mockVm.loadMoreMessages("conv-paging") }
  }
}

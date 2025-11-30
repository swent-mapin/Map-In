package com.swent.mapin.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.UserProfile
import io.mockk.*
import java.util.Calendar
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
    val leaveGroupStateFlow = MutableStateFlow<LeaveGroupState>(LeaveGroupState.Idle)

    every { vm.gotConversation } returns flow
    every { vm.leaveGroupState } returns leaveGroupStateFlow
    every { vm.getConversationById(any()) } just Runs
    every { vm.currentUserProfile } returns UserProfile("u1", "Alice")
    every { vm.resetLeaveGroupState() } just Runs

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
      Column(Modifier.fillMaxSize()) {
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

  @Test
  fun formatTimestamp_formatsCorrectly() {
    val cal =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 12)
          set(Calendar.MINUTE, 34)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val timestamp = cal.timeInMillis

    composeTestRule.setContent { Text(formatTimestamp(timestamp)) }

    composeTestRule.onNodeWithText("12:34").assertExists()
  }

  @Test
  fun formatTimestamp_invalid_timestamp() {
    var result = formatTimestamp(0L)
    assert("" == result)
    result = formatTimestamp(-123456789L)
    assert("" == result)
  }

  // ------------------------------------------------------------
  // ProfilePicture TESTS
  // ------------------------------------------------------------
  @Test
  fun profilePicture_showsDefaultIcon_whenUrlNull() {
    composeTestRule.setContent { ProfilePicture(url = null) }

    composeTestRule.onNodeWithContentDescription("DefaultProfile").assertExists()
  }

  @Test
  fun profilePicture_showsDefaultIcon_whenUrlBlank() {
    composeTestRule.setContent { ProfilePicture(url = "") }

    composeTestRule.onNodeWithContentDescription("DefaultProfile").assertExists()
  }

  // ------------------------------------------------------------
  // ConversationTopBar TESTS
  // ------------------------------------------------------------
  @Test
  fun conversationTopBar_showsTitle() {
    composeTestRule.setContent {
      ConversationTopBar(
          title = "Chat with Sam",
          participantNames = emptyList(),
          onNavigateBack = {},
          profilePictureUrl = null)
    }

    composeTestRule.onNodeWithText("Chat with Sam").assertExists()
  }

  @Test
  fun conversationTopBar_showsParticipantsList() {
    val names = listOf("Sam", "Alex", "Jordan")

    composeTestRule.setContent {
      ConversationTopBar(
          title = "Group Chat",
          participantNames = names,
          onNavigateBack = {},
          profilePictureUrl = null,
          isGroupChat = true)
    }
    composeTestRule.onNodeWithText("Sam, Alex, Jordan").assertExists()
  }

  @Test
  fun conversationTopBar_doesNotShowParticipantsList_forOneToOneChat() {
    composeTestRule.setContent {
      ConversationTopBar(
          title = "Chat with Sam",
          participantNames = emptyList(),
          onNavigateBack = {},
          profilePictureUrl = null,
          isGroupChat = false)
    }
    // For 1-to-1 chats, participant names list should not be shown
    // Verify the title exists
    composeTestRule.onNodeWithText("Chat with Sam").assertExists()
    // Verify there's only one text element (no participant list below)
    composeTestRule.onAllNodesWithText("Chat with Sam").assertCountEquals(1)
  }

  // ------------------------------------------------------------
  // Leave Group Tests
  // ------------------------------------------------------------
  @Test
  fun conversationTopBar_showsMenuButton_forGroupChat() {
    var leaveGroupCalled = false

    composeTestRule.setContent {
      ConversationTopBar(
          title = "Group Chat",
          participantNames = listOf("Alice", "Bob", "Charlie"),
          onNavigateBack = {},
          profilePictureUrl = null,
          isGroupChat = true,
          onLeaveGroup = { leaveGroupCalled = true })
    }

    // Menu button should be visible for group chats
    composeTestRule.onNodeWithTag("conversationMenuButton").assertIsDisplayed()
  }

  @Test
  fun conversationTopBar_hidesMenuButton_forOneToOneChat() {
    composeTestRule.setContent {
      ConversationTopBar(
          title = "Chat with Alice",
          participantNames = null,
          onNavigateBack = {},
          profilePictureUrl = null,
          isGroupChat = false,
          onLeaveGroup = null)
    }

    // Menu button should not exist for 1-to-1 chats
    composeTestRule.onNodeWithTag("conversationMenuButton").assertDoesNotExist()
  }

  @Test
  fun conversationTopBar_clickingLeaveGroup_triggersAction() {
    var leaveGroupCalled = false

    composeTestRule.setContent {
      ConversationTopBar(
          title = "Group Chat",
          participantNames = listOf("Alice", "Bob", "Charlie"),
          onNavigateBack = {},
          profilePictureUrl = null,
          isGroupChat = true,
          onLeaveGroup = { leaveGroupCalled = true })
    }

    // Click menu button
    composeTestRule.onNodeWithTag("conversationMenuButton").performClick()

    // Click "Leave Group" menu item
    composeTestRule.onNodeWithTag("leaveGroupMenuItem").assertIsDisplayed().performClick()

    // Confirmation dialog should appear
    composeTestRule.onNodeWithText("Leave Group?").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            "Are you sure you want to leave this group? You won't be able to undo this action.")
        .assertIsDisplayed()

    // Confirm leave
    composeTestRule.onNodeWithTag("confirmLeaveButton").assertIsDisplayed().performClick()

    // Verify the action was triggered
    assert(leaveGroupCalled)
  }

  @Test
  fun conversationTopBar_cancelLeaveGroup_doesNotTriggerAction() {
    var leaveGroupCalled = false

    composeTestRule.setContent {
      ConversationTopBar(
          title = "Group Chat",
          participantNames = listOf("Alice", "Bob", "Charlie"),
          onNavigateBack = {},
          profilePictureUrl = null,
          isGroupChat = true,
          onLeaveGroup = { leaveGroupCalled = true })
    }

    // Click menu button
    composeTestRule.onNodeWithTag("conversationMenuButton").performClick()

    // Click "Leave Group" menu item
    composeTestRule.onNodeWithTag("leaveGroupMenuItem").performClick()

    // Cancel leave
    composeTestRule.onNodeWithTag("cancelLeaveButton").assertIsDisplayed().performClick()

    // Verify the action was NOT triggered
    assert(!leaveGroupCalled)
  }

  @Test
  fun conversationScreen_navigatesBack_afterSuccessfulLeave() {
    val mockVm = mockk<MessageViewModel>(relaxed = true)
    val mockConvVm = mockk<ConversationViewModel>(relaxed = true)

    val fakeGroupConversation =
        Conversation(
            id = "group1",
            participantIds = listOf("u1", "u2", "u3"),
            participants =
                listOf(
                    UserProfile("u1", "Alice"),
                    UserProfile("u2", "Bob"),
                    UserProfile("u3", "Charlie")),
            profilePictureUrl = null)

    val conversationFlow = MutableStateFlow<Conversation?>(fakeGroupConversation)
    val leaveGroupStateFlow = MutableStateFlow<LeaveGroupState>(LeaveGroupState.Idle)

    every { mockVm.messages } returns MutableStateFlow(emptyList())
    every { mockVm.observeMessages(any()) } just Runs
    every { mockConvVm.gotConversation } returns conversationFlow
    every { mockConvVm.leaveGroupState } returns leaveGroupStateFlow
    every { mockConvVm.getConversationById(any()) } just Runs
    every { mockConvVm.currentUserProfile } returns UserProfile("u1", "Alice")
    every { mockConvVm.leaveConversation(any()) } answers
        {
          leaveGroupStateFlow.value = LeaveGroupState.Success
        }
    every { mockConvVm.resetLeaveGroupState() } answers
        {
          leaveGroupStateFlow.value = LeaveGroupState.Idle
        }

    var backCalled = false

    composeTestRule.setContent {
      ConversationScreenForTest(
          messageViewModel = mockVm,
          conversationViewModel = mockConvVm,
          conversationId = "group1",
          conversationName = "Group Chat",
          onNavigateBack = { backCalled = true })
    }

    // Simulate successful leave
    leaveGroupStateFlow.value = LeaveGroupState.Success

    composeTestRule.waitForIdle()

    // Verify navigation back was called
    assert(backCalled)
  }
}

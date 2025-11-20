package com.swent.mapin.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

object ConversationScreenTestTags {
  const val CONVERSATION_SCREEN = "conversationScreen"
  const val SEND_BUTTON = "sendButton"
  const val INPUT_TEXT_FIELD = "inputTextField"
}

const val MESSAGE_START = 0

// Data class for messages
data class Message(val text: String, val senderId: String, val isMe: Boolean)


/**
 * Displays the top app bar for ConversationScreen.
 *
 * @param title The title text displayed in the center of the top bar.
 * @param onNavigateBack Optional callback invoked when the back button is pressed.
 * @param profilePictureUrl The profile picture URL of this conversation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationTopBar(
    title: String,
    participantNames: List<String>? = emptyList(),
    onNavigateBack: (() -> Unit)? = null,
    profilePictureUrl: String? = ""
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile picture
                if (profilePictureUrl.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "DefaultProfile",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(profilePictureUrl),
                        contentDescription = "CustomProfile",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                //List of participants
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!participantNames.isNullOrEmpty()) {
                        Text(
                            text = participantNames.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag(ChatScreenTestTags.BACK_BUTTON)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag(ChatScreenTestTags.CHAT_TOP_BAR)
    )
}
/**
 * Assisted by AI Functions representing the UI for conversations between users
 *
 * @param conversationId The ID of the conversation
 * @param conversationName The name of the conversation or group chat
 * @param onNavigateBack Callback invoked when pressing the back button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    messageViewModel: MessageViewModel = viewModel(),
    conversationViewModel: ConversationViewModel = viewModel(),
    conversationId: String,
    conversationName: String,
) {

  // loads initial messages
  LaunchedEffect(conversationId) { messageViewModel.observeMessages(conversationId) }

  var messageText by remember { mutableStateOf(TextFieldValue("")) }
  val messages by messageViewModel.messages.collectAsState()
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  val shouldLoadMore by remember {
    derivedStateOf { listState.firstVisibleItemIndex == messages.lastIndex }
  }
  conversationViewModel.getConversationById(conversationId)
  val conversation by conversationViewModel.gotConversation.collectAsState()
  val participantNames = conversation?.participants?.map { conversation -> conversationName }

  // Dynamic loading when scrolling up
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) messageViewModel.loadMoreMessages(conversationId)
  }

  // Auto scroll to the Bottom of the LazyColumn when a new message is sent
  LaunchedEffect(messages.size) { listState.animateScrollToItem(MESSAGE_START) }

  Scaffold(
      topBar = {
          ConversationTopBar(conversationName, participantNames, onNavigateBack, conversation?.profilePictureUrl)
      },
      bottomBar = {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .imePadding() // ensures it moves above the keyboard
                    .navigationBarsPadding()
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              TextField(
                  value = messageText,
                  onValueChange = { messageText = it },
                  modifier =
                      Modifier.weight(1f).testTag(ConversationScreenTestTags.INPUT_TEXT_FIELD),
                  placeholder = { Text("Type a message...") },
                  singleLine = true,
                  shape = MaterialTheme.shapes.large)
              IconButton(
                  onClick = {
                    if (messageText.text.isNotBlank()) {
                      messageViewModel.sendMessage(conversationId, messageText.text)
                      messageText = TextFieldValue("")
                    }
                  },
                  modifier = Modifier.testTag(ConversationScreenTestTags.SEND_BUTTON)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                  }
            }
      },
      modifier = modifier.testTag(ConversationScreenTestTags.CONVERSATION_SCREEN)) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          // The lazy column to display messages
          LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              reverseLayout = true) {
                items(messages.reversed()) { message -> MessageBubble(message) }
              }
          // Button to scroll down to the newest message
          IconButton(
              onClick = { coroutineScope.launch { listState.animateScrollToItem(MESSAGE_START) } },
              modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "Scroll to bottom")
              }
        }
      }
}

/**
 * Assisted by AI Basic UI for messages as bubbles
 *
 * @param message The message sent or was sent
 */
@Composable
fun MessageBubble(message: Message) {
  val horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start

  val bubbleColor =
      if (message.isMe) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = horizontalArrangement) {
    Surface(color = bubbleColor, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
      Text(
          text = message.text,
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodyLarge)
    }
  }
}

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
import androidx.compose.runtime.mutableIntStateOf
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
import com.swent.mapin.model.UserProfile
import kotlinx.coroutines.launch

object ConversationScreenTestTags {
  const val CONVERSATION_SCREEN = "conversationScreen"
  const val SEND_BUTTON = "sendButton"
  const val INPUT_TEXT_FIELD = "inputTextField"
}

// Data class for messages
data class Message(val text: String, val senderId: String, val isMe: Boolean)

/**
 * Profile picture Composable, displays a profile picture If the url is null, displays a default one
 *
 * @param url The URL of the profile picture.
 */
@Composable
fun ProfilePicture(url: String?) {
  if (url.isNullOrBlank()) {
    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = "DefaultProfile",
        modifier = Modifier.size(32.dp).clip(CircleShape),
        tint = Color.Gray)
  } else {
    Image(
        painter = rememberAsyncImagePainter(url),
        contentDescription = "ProfilePicture",
        modifier = Modifier.size(32.dp).clip(CircleShape))
  }
}

/**
 * Displays the top app bar for ConversationScreen.
 *
 * @param title The title text displayed in the center of the top bar.
 * @param participantNames The list of participant's names.
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
          ProfilePicture(profilePictureUrl)
          Spacer(modifier = Modifier.width(8.dp))
          // List of participants
          Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (!participantNames.isNullOrEmpty()) {
              Text(
                  text = participantNames.joinToString(", "),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }
          }
        }
      },
      navigationIcon = {
        if (onNavigateBack != null) {
          IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag(ChatScreenTestTags.BACK_BUTTON)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer),
      modifier = Modifier.testTag(ChatScreenTestTags.CHAT_TOP_BAR))
}
/**
 * Assisted by AI Functions representing the UI for conversations between users
 *
 * @param conversationId The ID of the conversation
 * @param messageViewModel ViewModel for messages
 * @param conversationViewModel VideModel for conversations
 * @param conversationId ID of the specific conversation displayed
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
  val participantNames = conversation?.participants?.map { participant -> participant.name }

  // Dynamic loading when scrolling up
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) messageViewModel.loadMoreMessages(conversationId)
  }

  conversationViewModel.getCurrentUserProfile()
  val currentUserProfile = conversationViewModel.currentUserProfile

  // Auto scroll to the Bottom of the LazyColumn when a new message is sent
  val previousCount = remember { mutableIntStateOf(0) }

  LaunchedEffect(messages.size) {
    // Only scroll if a *new* message was added by someone else or me
    if (messages.size > previousCount.intValue) {
      listState.animateScrollToItem(messages.lastIndex)
    }
    previousCount.intValue = messages.size
  }
  Scaffold(
      topBar = {
        conversation?.participantIds?.size?.let {
          // If it is a group, then use group profile picture, if not then use the other user's
          if (it > 2) {
            ConversationTopBar(
                conversationName, participantNames, onNavigateBack, conversation?.profilePictureUrl)
          } else {
            val otherParticipant =
                conversation?.participants?.firstOrNull { it ->
                  it.userId != currentUserProfile.userId
                }
            ConversationTopBar(
                conversationName,
                participantNames,
                onNavigateBack,
                otherParticipant?.profilePictureUrl)
          }
        }
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
              modifier = Modifier.fillMaxSize().padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              reverseLayout = false) {
                items(messages) { message ->
                  val sender =
                      conversation?.participants?.firstOrNull { it.userId == message.senderId }
                  MessageBubble(message, sender)
                }
              }
          // Button to scroll down to the newest message
          IconButton(
              onClick = {
                coroutineScope.launch { listState.animateScrollToItem(messages.lastIndex) }
              },
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
 * @param sender The UserProfile of the sender
 */
@Composable
fun MessageBubble(message: Message, sender: UserProfile?) {
  val isMe = message.isMe

  val bubbleColor =
      if (isMe) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant

  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
      verticalAlignment = Alignment.Top) {
        if (!isMe) {
          // Avatar on LEFT for incoming messages
          ProfilePicture(sender?.profilePictureUrl)
          Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {

          // Name row (only for others)
          if (!isMe) {
            Text(
                text = sender?.name ?: "Unknown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp))
          } else {
            Text(
                text = "Me",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp))
          }

          Surface(color = bubbleColor, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge)
          }
        }

        if (isMe) {
          Spacer(Modifier.width(6.dp))
          // Avatar on RIGHT for outgoing messages
          ProfilePicture(sender?.profilePictureUrl)
        }
      }
}

package com.swent.mapin.ui.chat

import android.util.Log
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

object ConversationScreenTestTags {
  const val CONVERSATION_SCREEN = "conversationScreen"
  const val SEND_BUTTON = "sendButton"
  const val INPUT_TEXT_FIELD = "inputTextField"
}

// Data class for messages
data class Message(
    val text: String,
    val isMe: Boolean,
    val timestamp: Long = 0,
    val senderId: String = ""
)

/**
 * Formats a timestamp of message to a correct display format
 *
 * @param timestamp The timestamp to format
 */
fun formatTimestamp(timestamp: Long): String {
  if (timestamp <= 0L) return ""

  return try {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    formatter.format(Date(timestamp))
  } catch (e: Exception) {
    Log.e("timestamp format", "Error when formatting timestamp")
    ""
  }
}
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
 * @param isGroupChat Whether this is a group chat (more than 2 participants)
 * @param onLeaveGroup Callback invoked when the user wants to leave the group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationTopBar(
    title: String,
    participantNames: List<String>? = emptyList(),
    onNavigateBack: (() -> Unit)? = null,
    profilePictureUrl: String? = "",
    isGroupChat: Boolean = false,
    onLeaveGroup: (() -> Unit)? = null
) {
  var showMenu by remember { mutableStateOf(false) }
  var showLeaveConfirmation by remember { mutableStateOf(false) }

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
      // This was written with the help of Claude Sonnet 4.5
      actions = {
        if (isGroupChat && onLeaveGroup != null) {
          Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.testTag("conversationMenuButton")) {
                  Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                  text = { Text("Leave Group") },
                  onClick = {
                    showMenu = false
                    showLeaveConfirmation = true
                  },
                  modifier = Modifier.testTag("leaveGroupMenuItem"))
            }
          }
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer),
      modifier = Modifier.testTag(ChatScreenTestTags.CHAT_TOP_BAR))

  // Confirmation dialog for leaving group
  if (showLeaveConfirmation) {
    AlertDialog(
        onDismissRequest = { showLeaveConfirmation = false },
        title = { Text("Leave Group?") },
        text = {
          Text("Are you sure you want to leave this group? You won't be able to undo this action.")
        },
        confirmButton = {
          androidx.compose.material3.TextButton(
              onClick = {
                showLeaveConfirmation = false
                onLeaveGroup?.invoke()
              },
              modifier = Modifier.testTag("confirmLeaveButton")) {
                Text("Leave")
              }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(
              onClick = { showLeaveConfirmation = false },
              modifier = Modifier.testTag("cancelLeaveButton")) {
                Text("Cancel")
              }
        })
  }
}

/** Helper composable to render the appropriate conversation top bar based on conversation type. */
@Composable
private fun ConversationTopBarContent(
    conversation: Conversation?,
    conversationName: String,
    participantNames: List<String>?,
    currentUserProfile: UserProfile,
    onNavigateBack: () -> Unit,
    onLeaveGroup: () -> Unit
) {
  val participantCount = conversation?.participantIds?.size ?: return
  val isGroup = participantCount > 2
  val profileUrl =
      if (isGroup) conversation.profilePictureUrl
      else
          conversation.participants
              .firstOrNull { it.userId != currentUserProfile.userId }
              ?.profilePictureUrl
  ConversationTopBar(
      conversationName,
      if (isGroup) participantNames else emptyList(),
      onNavigateBack,
      profileUrl,
      isGroupChat = isGroup,
      onLeaveGroup = if (isGroup) onLeaveGroup else null)
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
  LaunchedEffect(conversationId) { conversationViewModel.getConversationById(conversationId) }

  val conversation by conversationViewModel.gotConversation.collectAsState()
  val participantNames = conversation?.participants?.map { participant -> participant.name }

  val leaveGroupState by conversationViewModel.leaveGroupState.collectAsState()

  // Handle leave group state changes
  LaunchedEffect(leaveGroupState) {
    if (leaveGroupState is LeaveGroupState.Success) {
      conversationViewModel.resetLeaveGroupState()
      onNavigateBack()
    }
  }

  // Dynamic loading when scrolling up
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) messageViewModel.loadMoreMessages(conversationId)
  }
  LaunchedEffect(Unit) { conversationViewModel.getCurrentUserProfile() }

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
        ConversationTopBarContent(
            conversation = conversation,
            conversationName = conversationName,
            participantNames = participantNames,
            currentUserProfile = currentUserProfile,
            onNavigateBack = onNavigateBack,
            onLeaveGroup = { conversationViewModel.leaveConversation(conversationId) })
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
          // Button to scroll down to the newest message (only if there are messages)
          if (messages.isNotEmpty()) {
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

  // Show loading dialog when leaving group
  if (leaveGroupState is LeaveGroupState.Loading) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Leaving Group") },
        text = {
          Row(
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
              }
        },
        confirmButton = {})
  }

  // Show error dialog if leaving group fails
  if (leaveGroupState is LeaveGroupState.Error) {
    AlertDialog(
        onDismissRequest = { conversationViewModel.resetLeaveGroupState() },
        title = { Text("Error") },
        text = { Text((leaveGroupState as LeaveGroupState.Error).message) },
        confirmButton = {
          androidx.compose.material3.TextButton(
              onClick = { conversationViewModel.resetLeaveGroupState() }) {
                Text("OK")
              }
        })
  }
}

/**
 * Assisted by AI Basic UI for messages as bubbles
 *
 * @param message The message sent or was sent
 * @param sender The UserProfile of the sender
 */
@Composable
fun MessageBubble(message: Message, sender: UserProfile? = null) {
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

          // Timestamp under the bubble
          Text(
              text = formatTimestamp(message.timestamp),
              style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
              modifier = Modifier.padding(top = 2.dp))
        }

        if (isMe) {
          Spacer(Modifier.width(6.dp))
          // Avatar on RIGHT for outgoing messages
          ProfilePicture(sender?.profilePictureUrl)
        }
      }
}

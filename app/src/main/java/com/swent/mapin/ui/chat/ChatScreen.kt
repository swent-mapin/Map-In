package com.swent.mapin.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.swent.mapin.R
import com.swent.mapin.model.UserProfile

object ChatScreenTestTags {
  const val CHAT_SCREEN = "Chats"
  const val CHAT_TAB = "ChatsTab"
  const val CHAT_BOTTOM_BAR_ITEM = "ChatBottomBarItem"
  const val CHAT_BOTTOM_BAR = "ChatBottomBar"
  const val CHAT_TOP_BAR = "ChatTopBar"
  const val CHAT_NAVIGATE_BUTTON = "ChatsNavigationButton"
  const val NEW_CONVERSATION_BUTTON = "NewConversationButton"
  const val CONVERSATION_ITEM = "ConversationItem"
  const val CHAT_EMPTY_TEXT = "ChatEmptyText"
  const val BACK_BUTTON = "backButton"
}
/**
 * Represents a chat conversation between one or more participants.
 *
 * @property id Unique identifier for the conversation.
 * @property participants The list of friends included in the conversation.
 * @property lastMessage The latest message in the conversation, if any.
 */
data class Conversation(
    val id: String = "",
    val name: String = "",
    val participantIds: List<String> = emptyList(),
    val participants: List<UserProfile> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val profilePictureUrl: String? = null
)

/**
 * Displays the top app bar for chat-related screens.
 *
 * @param title The title text displayed in the center of the top bar.
 * @param onNavigateBack Optional callback invoked when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(title: String, onNavigateBack: (() -> Unit)? = null) {
  TopAppBar(
      title = {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
              containerColor = MaterialTheme.colorScheme.surface,
              titleContentColor = MaterialTheme.colorScheme.onSurface,
              navigationIconContentColor = MaterialTheme.colorScheme.onSurface),
      modifier = Modifier.testTag(ChatScreenTestTags.CHAT_TOP_BAR))
}

/**
 * Displays a single conversation item in a chat list.
 *
 * @param conversation The conversation to display.
 * @param onClick Action invoked when the conversation item is clicked.
 */
@Composable
fun ConversationItem(
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

  Row(
      modifier = modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        if (conversation.profilePictureUrl.isNullOrBlank()) {
          Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "DefaultProfile",
              tint = Color.Gray,
              modifier = Modifier.size(48.dp).clip(CircleShape))
        } else {
          Image(
              painter = rememberAsyncImagePainter(conversation.profilePictureUrl),
              contentDescription = "CustomProfile",
              modifier = Modifier.size(48.dp).clip(CircleShape))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(conversation.name, style = MaterialTheme.typography.titleMedium)
          Text(
              conversation.lastMessage.ifBlank { "No messages yet" },
              style = MaterialTheme.typography.bodyMedium,
              color = Color.Gray,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
        }
      }
}

/**
 * Displays the chat screen showing all conversations created by the current user.
 *
 * Includes:
 * - A top app bar.
 * - A bottom navigation bar (ChatBottomBar).
 * - A floating action button to start a new conversation.
 *
 * @param modifier Modifier for styling or layout adjustments.
 * @param messageViewModel The viewModel for accessing messages and conversations.
 * @param onNavigateBack Callback invoked when the back button is pressed.
 * @param onNewConversation Callback invoked when the FAB is clicked.
 * @param onOpenConversation Callback invoked when a conversation item is selected.
 * @param onTabSelected Callback invoked when the user selects a different tab in the bottom
 *   navigation bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    messageViewModel: MessageViewModel = viewModel(),
    conversationViewModel: ConversationViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNewConversation: () -> Unit = {},
    onOpenConversation: (Conversation) -> Unit = {},
    onTabSelected: (ChatTab) -> Unit = {}
) {
  LaunchedEffect(Unit) { conversationViewModel.observeConversations() }

  val conversations by conversationViewModel.userConversations.collectAsState()

  Scaffold(
      topBar = {
        ChatTopBar(title = stringResource(R.string.chats), onNavigateBack = onNavigateBack)
      },
      bottomBar = {
        ChatBottomBar(ChatTab.Chats, onTabSelected = onTabSelected, modifier = Modifier)
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { onNewConversation() },
            modifier = Modifier.testTag(ChatScreenTestTags.NEW_CONVERSATION_BUTTON)) {
              Icon(Icons.Default.Add, contentDescription = "New Conversation")
            }
      },
      floatingActionButtonPosition = FabPosition.End,
  ) { paddingValues ->
    if (conversations.isEmpty()) {
      Column(
          modifier =
              Modifier.fillMaxSize()
                  .padding(paddingValues)
                  .padding(24.dp)
                  .testTag(ChatScreenTestTags.CHAT_SCREEN),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center) {
            Text(
                stringResource(R.string.empty_conversation),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(ChatScreenTestTags.CHAT_EMPTY_TEXT))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.empty_conversation_button), color = Color.Gray)
          }
    } else {
      LazyColumn(
          modifier = Modifier.padding(paddingValues).testTag(ChatScreenTestTags.CHAT_SCREEN)) {
            items(conversations) { conversation ->
              ConversationItem(
                  conversation,
                  onClick = { onOpenConversation(conversation) },
                  modifier =
                      Modifier.testTag(
                          "${ChatScreenTestTags.CONVERSATION_ITEM}_${conversation.id}"))
              HorizontalDivider()
            }
          }
    }
  }
}

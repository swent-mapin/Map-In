package com.swent.mapin.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swent.mapin.R
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.FriendshipStatus
import com.swent.mapin.model.UserProfile

object ChatScreenTestTags {
    const val CHAT_SCREEN = "Chats"
}

private val friend1 = FriendWithProfile(
    UserProfile(name = "Nathan", bio = "Chill guy", hobbies = listOf("Surf")),
    friendshipStatus = FriendshipStatus.ACCEPTED, ""
)
private val friend2 = FriendWithProfile(
    UserProfile(name = "Alex", bio = "Photographer", hobbies = listOf("Coffee")),
    friendshipStatus = FriendshipStatus.ACCEPTED, ""
)
private val friend3 = FriendWithProfile(
    UserProfile(name = "Zoe", bio = "Runner", hobbies = listOf("Music")),
    friendshipStatus = FriendshipStatus.ACCEPTED, ""
)
private val friendList = listOf(friend1, friend2, friend3)

private val sampleConversations = listOf(
    Conversation("c1", listOf(friend1), "Hey there!", true),
    Conversation("c2", listOf(friend2), "Shared a photo", false),
    Conversation("c3", listOf(friend3), "Let's meet up!", true)
)

/**
 * Represents a chat conversation between one or more participants.
 *
 * @property id Unique identifier for the conversation.
 * @property participants The list of friends included in the conversation.
 * @property lastMessage The latest message in the conversation, if any.
 * @property createdByCurrentUser Whether the current user initiated the conversation.
 */
data class Conversation(
    val id: String,
    val participants: List<FriendWithProfile>,
    val lastMessage: String = "",
    val createdByCurrentUser: Boolean = false
)

/**
 * Displays the top app bar for chat-related screens.
 *
 * @param title The title text displayed in the center of the top bar.
 * @param onNavigateBack Optional callback invoked when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
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
        )
    )
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
    onClick: () -> Unit = {}
) {
    val primaryParticipant = conversation.participants.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (primaryParticipant?.userProfile?.profilePictureUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            Image(
                painter = rememberAsyncImagePainter(primaryParticipant.userProfile.profilePictureUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                primaryParticipant?.userProfile?.name ?: "Unknown",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                conversation.lastMessage.ifBlank { "No messages yet" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
 * @param allConversations The full list of available conversations.
 * @param onNavigateBack Callback invoked when the back button is pressed.
 * @param onNewConversation Callback invoked when the FAB is clicked.
 * @param onOpenConversation Callback invoked when a conversation item is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    allConversations: List<Conversation> = sampleConversations,
    onNavigateBack: () -> Unit = {},
    onNewConversation: () -> Unit = {},
    onOpenConversation: (Conversation) -> Unit = {},
) {
    val createdConversations = allConversations.filter { it.createdByCurrentUser }

    Scaffold(
        topBar = { ChatTopBar(title = stringResource(R.string.chats), onNavigateBack = onNavigateBack) },
        bottomBar = { ChatBottomBar(ChatTab.Chats, onTabSelected = {}, modifier = Modifier) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNewConversation() }) {
                Icon(Icons.Default.Add, contentDescription = "New Conversation")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->

        if (createdConversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No conversations yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Tap + to start chatting with a friend!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(createdConversations) { conversation ->
                    ConversationItem(conversation, onClick = { onOpenConversation(conversation) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Screen allowing the user to select one or more friends to start a new conversation.
 *
 * - Tapping a friend toggles selection.
 * - A checkmark button appears in the top-right corner once at least one friend is selected.
 * - Pressing the checkmark calls [onConfirm] with the selected friends.
 *
 * @param friends List of friends available for selection.
 * @param onNavigateBack Callback invoked when the back button is pressed.
 * @param onConfirm Callback invoked when the user confirms selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationScreen(
    friends: List<FriendWithProfile> = friendList,
    onNavigateBack: () -> Unit = {},
    onConfirm: (List<FriendWithProfile>) -> Unit = {}
) {
    val selectedFriends = remember { mutableStateListOf<FriendWithProfile>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Conversation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedFriends.isNotEmpty()) {
                        IconButton(onClick = { onConfirm(selectedFriends) }) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm Selection")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(friends) { friend ->
                val isSelected = selectedFriends.contains(friend)
                val backgroundColor =
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedFriends.remove(friend)
                            else selectedFriends.add(friend)
                        }
                        .background(backgroundColor)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (friend.userProfile.profilePictureUrl.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(friend.userProfile.profilePictureUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(Modifier.width(12.dp))
                    Text(
                        friend.userProfile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

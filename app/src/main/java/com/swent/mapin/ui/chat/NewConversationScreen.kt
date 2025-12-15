package com.swent.mapin.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.swent.mapin.R
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.ui.friends.FriendsViewModel

object NewConversationScreenTestTags {
  const val NEW_CONVERSATION_SCREEN = "newConversationScreen"
  const val CONFIRM_BUTTON = "confirmButton"
  const val BACK_BUTTON = "backButton"
  const val FRIEND_ITEM = "friend"
  const val GROUP_NAME_DIALOG_TEXT = "groupText"
}

/** Creates a new 1-to-1 conversation with the selected friend. */
private fun createSingleConversation(
    friend: FriendWithProfile,
    conversationViewModel: ConversationViewModel
): Conversation {
  return Conversation(
      id = conversationViewModel.getNewUID(),
      name = friend.userProfile.name,
      participantIds = listOf(Firebase.auth.currentUser?.uid ?: "", friend.userProfile.userId),
      participants = listOf(conversationViewModel.currentUserProfile, friend.userProfile),
      profilePictureUrl = friend.userProfile.profilePictureUrl)
}

/** Creates a new group conversation with the selected friends. */
private fun createGroupConversation(
    groupName: String,
    selectedFriends: List<FriendWithProfile>,
    conversationViewModel: ConversationViewModel
): Conversation {
  val ids = selectedFriends.map { it.userProfile.userId }
  val profiles = selectedFriends.map { it.userProfile }
  return Conversation(
      id = conversationViewModel.getNewUID(),
      name = groupName,
      participantIds = ids + listOf(Firebase.auth.currentUser?.uid ?: ""),
      participants = profiles + listOf(conversationViewModel.currentUserProfile),
      profilePictureUrl = selectedFriends.first().userProfile.profilePictureUrl)
}

/** Top bar for the new conversation screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewConversationTopBar(
    onNavigateBack: () -> Unit,
    selectedFriends: SnapshotStateList<FriendWithProfile>,
    onConfirmClick: () -> Unit
) {
  TopAppBar(
      title = { Text("New Conversation") },
      navigationIcon = {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag(NewConversationScreenTestTags.BACK_BUTTON)) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
      },
      actions = {
        if (selectedFriends.isNotEmpty()) {
          IconButton(
              onClick = onConfirmClick,
              modifier = Modifier.testTag(NewConversationScreenTestTags.CONFIRM_BUTTON)) {
                Icon(Icons.Default.Check, contentDescription = "Confirm Selection")
              }
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer))
}

/** Empty state when user has no friends. */
@Composable
private fun EmptyFriendsContent(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxSize().padding(24.dp).testTag(ChatScreenTestTags.CHAT_SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.empty_friends), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_friends2), color = Color.Gray)
      }
}

/** Profile picture for a friend item, showing selection state. */
@Composable
private fun FriendProfilePicture(profilePictureUrl: String?, isSelected: Boolean) {
  val selectedColor = MaterialTheme.colorScheme.primary

  if (profilePictureUrl.isNullOrBlank()) {
    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = null,
        tint = if (isSelected) selectedColor else Color.Gray,
        modifier = Modifier.size(48.dp).clip(CircleShape))
  } else {
    Image(
        painter = rememberAsyncImagePainter(profilePictureUrl),
        contentDescription = null,
        modifier =
            Modifier.size(48.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) selectedColor else Color.Transparent,
                    shape = CircleShape))
  }
}

/** A single friend item row. */
@Composable
private fun FriendItem(
    friend: FriendWithProfile,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
  val backgroundColor =
      if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
      } else {
        Color.Transparent
      }
  val textColor =
      if (isSelected) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurface
      }

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = onToggleSelection)
              .background(backgroundColor)
              .padding(12.dp)
              .testTag("${NewConversationScreenTestTags.FRIEND_ITEM}_${friend.userProfile.name}"),
      verticalAlignment = Alignment.CenterVertically) {
        FriendProfilePicture(friend.userProfile.profilePictureUrl, isSelected)
        Spacer(Modifier.width(12.dp))
        Text(
            friend.userProfile.name,
            style = MaterialTheme.typography.titleMedium,
            color = textColor)
      }
  HorizontalDivider()
}

/** List of friends to select from. */
@Composable
private fun FriendsListContent(
    friends: List<FriendWithProfile>,
    selectedFriends: SnapshotStateList<FriendWithProfile>,
    modifier: Modifier = Modifier
) {
  LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
    items(friends) { friend ->
      val isSelected = selectedFriends.contains(friend)
      FriendItem(
          friend = friend,
          isSelected = isSelected,
          onToggleSelection = {
            if (isSelected) selectedFriends.remove(friend) else selectedFriends.add(friend)
          })
    }
  }
}

/** Dialog for entering group name. */
@Composable
private fun GroupNameDialog(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            "Enter Group Name",
            modifier = Modifier.testTag(NewConversationScreenTestTags.GROUP_NAME_DIALOG_TEXT))
      },
      text = {
        TextField(
            value = groupName,
            onValueChange = onGroupNameChange,
            placeholder = { Text("Group name") })
      },
      confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/**
 * Assisted by AI Screen allowing the user to select one or more friends to start a new
 * conversation.
 * - Tapping a friend toggles selection.
 * - A checkmark button appears in the top-right corner once at least one friend is selected.
 * - Pressing the checkmark calls [onConfirm] with the selected friends.
 *
 * @param conversationViewModel ViewModel for Conversation data class
 * @param friendsViewModel ViewModel for user's friends
 * @param onNavigateBack Callback invoked when the back button is pressed.
 * @param onConfirm Callback invoked when the user confirms selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationScreen(
    conversationViewModel: ConversationViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
  LaunchedEffect(Unit) { friendsViewModel.loadFriends() }

  val selectedFriends = remember { mutableStateListOf<FriendWithProfile>() }
  val friends by friendsViewModel.friends.collectAsState()
  val showGroupNameDialog = remember { mutableStateOf(false) }
  val groupName = remember { mutableStateOf("") }

  val onConfirmSelection: () -> Unit = {
    if (selectedFriends.size >= 2) {
      showGroupNameDialog.value = true
    } else {
      val friend = selectedFriends.first()
      val newConvo = createSingleConversation(friend, conversationViewModel)
      conversationViewModel.createConversation(newConvo)
      onConfirm()
    }
  }

  Scaffold(
      topBar = {
        NewConversationTopBar(
            onNavigateBack = onNavigateBack,
            selectedFriends = selectedFriends,
            onConfirmClick = onConfirmSelection)
      },
      modifier = Modifier.testTag(NewConversationScreenTestTags.NEW_CONVERSATION_SCREEN)) {
          paddingValues ->
        if (friends.isEmpty()) {
          EmptyFriendsContent(modifier = Modifier.padding(paddingValues))
        } else {
          FriendsListContent(
              friends = friends,
              selectedFriends = selectedFriends,
              modifier = Modifier.padding(paddingValues))
        }
      }

  if (showGroupNameDialog.value) {
    GroupNameDialog(
        groupName = groupName.value,
        onGroupNameChange = { groupName.value = it },
        onConfirm = {
          if (groupName.value.isNotBlank()) {
            val newConvo =
                createGroupConversation(groupName.value, selectedFriends, conversationViewModel)
            conversationViewModel.createConversation(newConvo)
            onConfirm()
            showGroupNameDialog.value = false
            groupName.value = ""
          }
        },
        onDismiss = {
          showGroupNameDialog.value = false
          groupName.value = ""
        })
  }
}

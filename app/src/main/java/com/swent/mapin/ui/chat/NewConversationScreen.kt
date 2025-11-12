package com.swent.mapin.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
}

/**
 * Assisted by AI
 * Screen allowing the user to select one or more friends to start a new
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
  LaunchedEffect(Unit){
      friendsViewModel.loadFriends()
  }
  val selectedFriends = remember { mutableStateListOf<FriendWithProfile>() }
  val friends by friendsViewModel.friends.collectAsState()
  val showGroupNameDialog = remember { mutableStateOf(false) }
  val groupName = remember { mutableStateOf("") }

  Scaffold(
      topBar = {
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
                    onClick = {
                        if(selectedFriends.size > 2) {
                            showGroupNameDialog.value = true
                        } else {
                            val friend = selectedFriends.first()
                            val newConvo = Conversation(
                                id = conversationViewModel.getNewUID(),
                                name = friend.userProfile.name,
                                participantIds = listOf(Firebase.auth.currentUser?.uid ?: "", friend.userProfile.userId),
                                participants = listOf(conversationViewModel.currentUserProfile,friend.userProfile),
                                profilePictureUrl = friend.userProfile.profilePictureUrl
                            )
                            conversationViewModel.createConversation(newConvo)
                            onConfirm()
                        }
                    },
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
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        )
      },
      modifier = Modifier.testTag(NewConversationScreenTestTags.NEW_CONVERSATION_SCREEN)
  ) {  paddingValues ->

        if(friends.isEmpty()){
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp)
                        .testTag(ChatScreenTestTags.CHAT_SCREEN),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.empty_friends),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.empty_friends2), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
            ) {
                items(friends) { friend ->
                    val isSelected = selectedFriends.contains(friend)
                    val backgroundColor =
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent

                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedFriends.remove(friend)
                                    else selectedFriends.add(friend)
                                }
                                .background(backgroundColor)
                                .padding(12.dp)
                                .testTag(
                                    "${NewConversationScreenTestTags.FRIEND_ITEM}_${friend.userProfile.name}"
                                ),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (friend.userProfile.profilePictureUrl.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint =
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                            )
                        } else {
                            Image(
                                painter =
                                    rememberAsyncImagePainter(friend.userProfile.profilePictureUrl),
                                contentDescription = null,
                                modifier =
                                    Modifier.size(48.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color =
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else Color.Transparent,
                                            shape = CircleShape
                                        )
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                        Text(
                            friend.userProfile.name,
                            style = MaterialTheme.typography.titleMedium,
                            color =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
  }

  if(showGroupNameDialog.value) {
      AlertDialog(
          onDismissRequest = { showGroupNameDialog.value = false },
          title = { Text("Enter Group Name") },
          text = {
              androidx.compose.material3.TextField(
                  value = groupName.value,
                  onValueChange = { groupName.value = it },
                  placeholder = { Text("Group name") }
              )
          },
          confirmButton = {
              androidx.compose.material3.TextButton(
                  onClick = {
                      if (groupName.value.isNotBlank()) {
                          val ids = selectedFriends.map { friends -> friends.userProfile.userId }
                          val profiles = selectedFriends.map { friends -> friends.userProfile }
                          val newConvo = Conversation(
                              id = conversationViewModel.getNewUID(),
                              name = groupName.value,
                              participantIds = ids + listOf((Firebase.auth.currentUser?.uid ?: "")),
                              participants = profiles + listOf(conversationViewModel.currentUserProfile),
                              profilePictureUrl = selectedFriends.first().userProfile.profilePictureUrl
                          )
                          conversationViewModel.createConversation(newConvo)
                          onConfirm()
                          showGroupNameDialog.value = false
                          groupName.value = ""
                      }
                  }
              ) {
                  Text("OK")
              }
          },
          dismissButton = {
              androidx.compose.material3.TextButton(
                  onClick = {
                      showGroupNameDialog.value = false
                      groupName.value = ""
                  }
              ) {
                  Text("Cancel")
              }
          }
      )
  }
}

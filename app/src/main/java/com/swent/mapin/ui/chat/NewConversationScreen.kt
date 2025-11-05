package com.swent.mapin.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swent.mapin.model.FriendWithProfile

/**
 * Screen allowing the user to select one or more friends to start a new conversation.
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
                        IconButton(onClick = { onConfirm(selectedFriends) }) { //TODO Add logic to type in a group name if number of selected friends >=2
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
        }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
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
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (friend.userProfile.profilePictureUrl.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint =
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(48.dp).clip(CircleShape))
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
                                        shape = CircleShape))
                    }

                    Spacer(Modifier.width(12.dp))
                    Text(
                        friend.userProfile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider()
            }
        }
    }
}
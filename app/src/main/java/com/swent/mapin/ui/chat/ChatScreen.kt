package com.swent.mapin.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.swent.mapin.ui.theme.MapInTheme
import com.swent.mapin.R
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.FriendshipStatus
import com.swent.mapin.model.UserProfile

object ChatScreenTestTags {
    const val CHAT_SCREEN = "chatScreen"

}
private val friend1Profile = UserProfile(
    name = "Nathan",
    bio = "Duchobag",
    hobbies = listOf("VsCode", "Surf")
)
private val friend1 = FriendWithProfile(friend1Profile, friendshipStatus = FriendshipStatus.ACCEPTED, "")

private val friendList = listOf(friend1)

/**
 * TODO
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    onNavigateBack: () -> Unit = {}
) {
    TopAppBar(
        title = {Text(stringResource(R.string.chats))},
        navigationIcon = {
            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    )
}

/**
 * Assisted by AI
 * TODO
 */

@Composable
fun ChatItem(
    friend: FriendWithProfile,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
        ,
        verticalAlignment = Alignment.CenterVertically,
    ){
        if(friend.userProfile.profilePictureUrl.isNullOrBlank()) {
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
                painter = rememberAsyncImagePainter(friend.userProfile.profilePictureUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(friend.userProfile.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "No messages yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * TODO
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    friends: List<FriendWithProfile> = emptyList()
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {ChatTopBar(onNavigateBack)},
        bottomBar = {ChatBottomBar(ChatTab.Chats, {tab -> }, modifier = Modifier)}
    ){ paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ){
            items(friends) { friend ->
                ChatItem(friend)
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MapInTheme { ChatScreen(friends = friendList)}
}
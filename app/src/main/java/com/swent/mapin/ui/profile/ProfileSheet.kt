package com.swent.mapin.ui.profile

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.event.Event
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ProfileSheet displays a user's profile in a bottom sheet format.
 *
 * @param userId The ID of the user whose profile to display
 * @param onClose Callback when the close button is pressed
 * @param onEventClick Callback when an event is clicked
 * @param viewModel The ViewModel for this sheet
 */
@Composable
fun ProfileSheet(
    userId: String,
    onClose: () -> Unit,
    onEventClick: (Event) -> Unit,
    viewModel: ProfileSheetViewModel = viewModel()
) {
  LaunchedEffect(userId) { viewModel.loadProfile(userId) }

  // Track if close action has been triggered to prevent multiple clicks
  var hasNavigatedBack by remember(userId) { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth().testTag("profileSheet")) {
    // Header with close button
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End) {
          IconButton(
              onClick = {
                if (!hasNavigatedBack) {
                  hasNavigatedBack = true
                  onClose()
                }
              },
              modifier = Modifier.testTag("profileSheetCloseButton")) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface)
              }
        }

    when (val state = viewModel.state) {
      is ProfileSheetState.Loading -> {
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.testTag("profileSheetLoading"))
            }
      }
      is ProfileSheetState.Error -> {
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            contentAlignment = Alignment.Center) {
              Text(
                  text = state.message,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.testTag("profileSheetError"))
            }
      }
      is ProfileSheetState.Loaded -> {
        ProfileSheetContent(
            profile = state.profile,
            upcomingEvents = state.upcomingEvents,
            pastEvents = state.pastEvents,
            isFollowing = state.isFollowing,
            isOwnProfile = state.isOwnProfile,
            friendStatus = state.friendStatus,
            onFollowToggle = { viewModel.toggleFollow() },
            onAddFriend = { viewModel.sendFriendRequest() },
            onEventClick = onEventClick)
      }
    }
  }
}

@Composable
internal fun ProfileSheetContent(
    profile: UserProfile,
    upcomingEvents: List<Event>,
    pastEvents: List<Event>,
    isFollowing: Boolean,
    isOwnProfile: Boolean,
    friendStatus: FriendStatus,
    onFollowToggle: () -> Unit,
    onAddFriend: () -> Unit,
    onEventClick: (Event) -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .verticalScroll(scrollState)
              .padding(horizontal = 16.dp)
              .padding(bottom = 16.dp)) {
        // Profile header
        ProfileHeader(profile = profile)

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row (following/followers)
        StatsRow(profile = profile)

        Spacer(modifier = Modifier.height(16.dp))

        // Follow button (only show if not own profile)
        if (!isOwnProfile) {
          FollowButton(isFollowing = isFollowing, onFollowToggle = onFollowToggle)
          Spacer(modifier = Modifier.height(8.dp))
          FriendActionRow(friendStatus = friendStatus, onAddFriend = onAddFriend)
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Badges section
        if (profile.badges.isNotEmpty()) {
          BadgesSection(badges = profile.badges)
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Upcoming events section
        if (upcomingEvents.isNotEmpty()) {
          EventsSection(
              title = "Upcoming Owned Events", events = upcomingEvents, onEventClick = onEventClick)
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Past events section
        if (pastEvents.isNotEmpty()) {
          EventsSection(
              title = "Past Owned Events", events = pastEvents, onEventClick = onEventClick)
        }

        // Empty state
        if (upcomingEvents.isEmpty() && pastEvents.isEmpty()) {
          Card(
              modifier = Modifier.fillMaxWidth().testTag("noEventsCard"),
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = "No events organized yet",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
              }
        }
      }
}

@Composable
private fun ProfileHeader(profile: UserProfile) {
  Column(
      modifier = Modifier.fillMaxWidth().testTag("profileHeader"),
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar
        Box(
            modifier =
                Modifier.size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("profileAvatar"),
            contentAlignment = Alignment.Center) {
              if (profile.avatarUrl != null &&
                  (profile.avatarUrl.startsWith("http") ||
                      profile.avatarUrl.startsWith("content"))) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Profile Picture",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxSize().clip(CircleShape).testTag("profileAvatarImage"))
              } else {
                Icon(
                    imageVector = getSheetAvatarIcon(profile.avatarUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(60.dp).testTag("profileAvatarIcon"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

        Spacer(modifier = Modifier.height(12.dp))

        // Name
        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("profileName"))

        // Bio
        if (profile.bio.isNotEmpty() && profile.bio != "Tell us about yourself...") {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
              text = profile.bio,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("profileBio"))
        }

        // Location
        if (profile.location.isNotEmpty() && profile.location != "Unknown") {
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = profile.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("profileLocation"))
          }
        }

        // Hobbies
        if (profile.hobbies.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = profile.hobbies.joinToString(", "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("profileHobbies"))
        }
      }
}

@Composable
private fun StatsRow(profile: UserProfile) {
  Row(
      modifier = Modifier.fillMaxWidth().testTag("profileStats"),
      horizontalArrangement = Arrangement.Center) {
        StatItem(count = profile.followerIds.size, label = "Followers")
      }
}

@Composable
private fun StatItem(count: Int, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold)
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun FollowButton(isFollowing: Boolean, onFollowToggle: () -> Unit) {
  if (isFollowing) {
    OutlinedButton(
        onClick = onFollowToggle, modifier = Modifier.fillMaxWidth().testTag("unfollowButton")) {
          Icon(
              imageVector = Icons.Default.PersonRemove,
              contentDescription = null,
              modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Unfollow")
        }
  } else {
    Button(onClick = onFollowToggle, modifier = Modifier.fillMaxWidth().testTag("followButton")) {
      Icon(
          imageVector = Icons.Default.PersonAdd,
          contentDescription = null,
          modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Follow")
    }
  }
}

@Composable
private fun FriendActionRow(friendStatus: FriendStatus, onAddFriend: () -> Unit) {
  when (friendStatus) {
    FriendStatus.NOT_FRIEND ->
        OutlinedButton(
            onClick = onAddFriend, modifier = Modifier.fillMaxWidth().testTag("addFriendButton")) {
              Icon(
                  imageVector = Icons.Default.PersonAdd,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Add Friend")
            }
    FriendStatus.PENDING ->
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth().testTag("pendingFriendButton")) {
              Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Pending")
            }
    FriendStatus.FRIENDS ->
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth().testTag("friendsIndicator")) {
              Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Friends")
            }
  }
}

@Composable
private fun EventsSection(title: String, events: List<Event>, onEventClick: (Event) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("eventsRow_$title")) {
          items(events) { event -> EventCard(event = event, onClick = { onEventClick(event) }) }
        }
  }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.width(200.dp).clickable(onClick = onClick).testTag("eventCard_${event.uid}"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
              text = event.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)

          Spacer(modifier = Modifier.height(4.dp))

          event.date?.let { timestamp ->
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            Text(
                text = dateFormat.format(timestamp.toDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          if (event.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.tags.first(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
          }
        }
      }
}

@VisibleForTesting
internal fun getSheetAvatarIcon(avatarUrl: String?): ImageVector {
  if (avatarUrl.isNullOrEmpty()) return Icons.Default.Person

  return when (avatarUrl) {
    "person" -> Icons.Default.Person
    "face" -> Icons.Default.Face
    "star" -> Icons.Default.Star
    "favorite" -> Icons.Default.Favorite
    else -> Icons.Default.Person
  }
}

package com.swent.mapin.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.SearchResultWithStatus

/**
 * Main Friends screen composable.
 *
 * Displays three tabs: Friends list, pending requests, and user search. Supports optional parameter
 * injection for testing purposes.
 *
 * @param onNavigateBack Callback invoked when the back button is pressed.
 * @param modifier Optional modifier for the screen.
 * @param viewModel ViewModel managing the screen state and business logic.
 * @param friends Optional override for friends list (used in tests).
 * @param pendingRequests Optional override for pending requests (used in tests).
 * @param searchResults Optional override for search results (used in tests).
 * @param searchQuery Optional override for search query (used in tests).
 * @param onRemoveFriend Optional callback override for removing friends (used in tests).
 * @param onAcceptRequest Optional callback override for accepting requests (used in tests).
 * @param onRejectRequest Optional callback override for rejecting requests (used in tests).
 * @param onSendFriendRequest Optional callback override for sending friend requests (used in
 *   tests).
 * @param onSearchQueryChange Optional callback override for search query changes (used in tests).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = viewModel(),
    friends: List<FriendWithProfile>? = null,
    pendingRequests: List<FriendWithProfile>? = null,
    searchResults: List<SearchResultWithStatus>? = null,
    searchQuery: String? = null,
    onRemoveFriend: ((String) -> Unit)? = null,
    onAcceptRequest: ((String) -> Unit)? = null,
    onRejectRequest: ((String) -> Unit)? = null,
    onSendFriendRequest: ((String) -> Unit)? = null,
    onSearchQueryChange: ((String) -> Unit)? = null
) {
  val selectedTab by viewModel.selectedTab.collectAsState()
  val friendsList = friends ?: viewModel.friends.collectAsState().value
  val pendingList = pendingRequests ?: viewModel.pendingRequests.collectAsState().value
  val searchList = searchResults ?: viewModel.searchResults.collectAsState().value
  val searchQ = searchQuery ?: viewModel.searchQuery.collectAsState().value

  Scaffold(
      modifier = modifier.fillMaxSize().testTag("friendsScreen"),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  "Friends",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer))
      }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
          TabRow(
              selectedTabIndex = selectedTab.ordinal,
              modifier = Modifier.fillMaxWidth().testTag("friendsTabRow")) {
                FriendsTab.entries.forEach { tab ->
                  Tab(
                      selected = selectedTab == tab,
                      onClick = { viewModel.selectTab(tab) },
                      modifier = Modifier.testTag("tab${tab.name}"),
                      text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center) {
                              Text(tab.title)
                              if (tab == FriendsTab.REQUESTS && pendingList.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("requestBadge")) {
                                      Text(
                                          pendingList.size.toString(),
                                          style = MaterialTheme.typography.labelSmall)
                                    }
                              }
                            }
                      })
                }
              }
          Box(Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
              FriendsTab.FRIENDS ->
                  FriendsListTab(
                      friendsList,
                      onRemoveFriend ?: viewModel::removeFriend,
                      Modifier.testTag("friendsListTab"))
              FriendsTab.REQUESTS ->
                  RequestsTab(
                      pendingList,
                      onAcceptRequest ?: viewModel::acceptRequest,
                      onRejectRequest ?: viewModel::rejectRequest,
                      Modifier.testTag("requestsTab"))
              FriendsTab.SEARCH ->
                  SearchTab(
                      searchQ,
                      onSearchQueryChange ?: viewModel::updateSearchQuery,
                      searchList,
                      onSendFriendRequest ?: viewModel::sendFriendRequest,
                      Modifier.testTag("searchTab"))
            }
          }
        }
      }
}

/** Enum representing the three tabs in the Friends screen. */
enum class FriendsTab(val title: String) {
  FRIENDS("Friends"),
  REQUESTS("Requests"),
  SEARCH("Search")
}

/**
 * Displays the Friends list tab.
 *
 * @param friends List of friends to display.
 * @param onRemove Callback invoked when a friend is removed.
 * @param modifier Optional modifier for the composable.
 */
@Composable
private fun FriendsListTab(
    friends: List<FriendWithProfile>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  if (friends.isEmpty()) {
    EmptyState(Icons.Default.Person, "No friends yet", "Search for friends to add them", modifier)
  } else {
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(friends, key = { it.userProfile.userId }) { friend ->
        FriendCard(
            friend,
            { onRemove(friend.userProfile.userId) },
            Modifier.testTag("friendCard_${friend.userProfile.userId}"))
      }
    }
  }
}

/**
 * Displays the Requests tab showing pending friend requests.
 *
 * @param requests List of pending requests to display.
 * @param onAccept Callback invoked when a request is accepted.
 * @param onReject Callback invoked when a request is rejected.
 * @param modifier Optional modifier for the composable.
 */
@Composable
private fun RequestsTab(
    requests: List<FriendWithProfile>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  if (requests.isEmpty()) {
    EmptyState(
        Icons.Default.Notifications,
        "No pending requests",
        "Friend requests will appear here",
        modifier)
  } else {
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(requests, key = { it.requestId }) { req ->
        RequestCard(
            req,
            { onAccept(req.requestId) },
            { onReject(req.requestId) },
            Modifier.testTag("requestCard_${req.requestId}"))
      }
    }
  }
}

/**
 * Displays the Search tab for finding new friends.
 *
 * @param query Current search query text.
 * @param onChange Callback invoked when the search query changes.
 * @param results List of search results to display.
 * @param onSend Callback invoked when a friend request is sent.
 * @param modifier Optional modifier for the composable.
 */
@Composable
private fun SearchTab(
    query: String,
    onChange: (String) -> Unit,
    results: List<SearchResultWithStatus>,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().testTag("searchTextField"),
        placeholder = { Text("Search by name or email...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        trailingIcon = {
          if (query.isNotEmpty())
              IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "Clear") }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp))
    when {
      query.isEmpty() ->
          EmptyState(
              Icons.Default.Search,
              "Search for friends",
              "Enter a name or email to find friends",
              Modifier.fillMaxSize())
      results.isEmpty() ->
          EmptyState(
              Icons.Default.SearchOff,
              "No results found",
              "Try a different search term",
              Modifier.fillMaxSize())
      else ->
          LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.userProfile.userId }) { res ->
              SearchCard(
                  res,
                  { onSend(res.userProfile.userId) },
                  Modifier.testTag("searchResultCard_${res.userProfile.userId}"))
            }
          }
    }
  }
}

/**
 * Card displaying a friend's information with a remove button.
 *
 * @param friend The friend to display.
 * @param onRemove Callback invoked when the remove button is clicked.
 * @param modifier Optional modifier for the card.
 */
@Composable
private fun FriendCard(
    friend: FriendWithProfile,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
  var showDialog by remember { mutableStateOf(false) }
  Card(
      modifier.fillMaxWidth(),
      RoundedCornerShape(12.dp),
      CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
      CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Avatar(
                  friend.userProfile.avatarUrl,
                  friend.userProfile.name,
                  Modifier.size(48.dp).testTag("avatar_${friend.userProfile.userId}"))
              Spacer(Modifier.width(12.dp))
              Column(Modifier.weight(1f)) {
                Text(
                    friend.userProfile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (friend.userProfile.location.isNotEmpty()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        friend.userProfile.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                  }
                }
              }
              IconButton({ showDialog = true }, Modifier.testTag("removeFriendButton")) {
                Icon(
                    Icons.Default.PersonRemove,
                    "Remove friend",
                    tint = MaterialTheme.colorScheme.error)
              }
            }
      }
  if (showDialog) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        icon = { Icon(Icons.Default.Warning, null) },
        title = { Text("Remove Friend") },
        text = {
          Text("Are you sure you want to remove ${friend.userProfile.name} from your friends?")
        },
        confirmButton = {
          Button(
              {
                onRemove()
                showDialog = false
              },
              colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) {
                Text("Remove")
              }
        },
        dismissButton = { TextButton({ showDialog = false }) { Text("Cancel") } })
  }
}

/**
 * Card displaying a pending friend request with accept/reject buttons.
 *
 * @param req The friend request to display.
 * @param onAccept Callback invoked when the accept button is clicked.
 * @param onReject Callback invoked when the reject button is clicked.
 * @param modifier Optional modifier for the card.
 */
@Composable
private fun RequestCard(
    req: FriendWithProfile,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier.fillMaxWidth(),
      RoundedCornerShape(12.dp),
      CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
      CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                req.userProfile.avatarUrl,
                req.userProfile.name,
                Modifier.size(48.dp).testTag("avatar_${req.userProfile.userId}"))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
              Text(
                  req.userProfile.name,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
              if (req.userProfile.bio.isNotEmpty()) {
                Text(
                    req.userProfile.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
              }
            }
          }
          Spacer(Modifier.height(12.dp))
          Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onReject,
                Modifier.weight(1f).testTag("rejectButton"),
                colors = ButtonDefaults.outlinedButtonColors(MaterialTheme.colorScheme.error)) {
                  Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Decline")
                }
            Button(onAccept, Modifier.weight(1f).testTag("acceptButton")) {
              Icon(Icons.Default.Check, null, Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("Accept")
            }
          }
        }
      }
}

/**
 * Card displaying a search result with an add friend button.
 *
 * @param result The search result to display.
 * @param onSend Callback invoked when the add friend button is clicked.
 * @param modifier Optional modifier for the card.
 */
@Composable
private fun SearchCard(
    result: SearchResultWithStatus,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
  var sent by remember { mutableStateOf(false) }
  Card(
      modifier.fillMaxWidth(),
      RoundedCornerShape(12.dp),
      CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
      CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Avatar(
                  result.userProfile.avatarUrl,
                  result.userProfile.name,
                  Modifier.size(48.dp).testTag("avatar_${result.userProfile.userId}"))
              Spacer(Modifier.width(12.dp))
              Column(Modifier.weight(1f)) {
                Text(
                    result.userProfile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (result.userProfile.bio.isNotEmpty()) {
                  Text(
                      result.userProfile.bio,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis)
                }
              }
              if (sent || result.hasPendingRequest) {
                OutlinedButton(
                    {}, enabled = false, modifier = Modifier.testTag("requestSentButton")) {
                      Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Sent")
                    }
              } else {
                Button(
                    {
                      onSend()
                      sent = true
                    },
                    Modifier.testTag("sendRequestButton")) {
                      Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Add")
                    }
              }
            }
      }
}

/**
 * Displays a user avatar (first letter of name or default icon).
 *
 * @param url Optional avatar URL (currently not used for images).
 * @param name User's name (first letter is displayed).
 * @param modifier Optional modifier for the avatar.
 */
@Composable
private fun Avatar(url: String?, name: String, modifier: Modifier = Modifier) {
  Box(
      modifier =
          modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center) {
        when {
          url.isNullOrEmpty() || url == "person" -> {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
          url.startsWith("http") -> {
            AsyncImage(
                model = url,
                contentDescription = "${name} avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape))
          }
          else -> {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
          }
        }
      }
}

/**
 * Displays an empty state message with an icon, title, and subtitle.
 *
 * @param icon Icon to display.
 * @param msg Main message text.
 * @param sub Subtitle text.
 * @param modifier Optional modifier for the empty state.
 */
@Composable
private fun EmptyState(icon: ImageVector, msg: String, sub: String, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize(), Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(
              icon,
              null,
              Modifier.size(64.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
          Text(
              msg,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
              sub,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
              textAlign = TextAlign.Center)
        }
  }
}

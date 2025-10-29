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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.UserProfile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

// Assisted by AI


/**
 * Friends screen and related UI components.
 *
 * Overview:
 * - This file contains the FriendsScreen composable and small presentation components used by it.
 * - The UI is driven by a FriendsViewModel in production but accepts optional parameters to
 *   override ViewModel state and callbacks for testing.
 * - Testability: many elements expose testTag identifiers used by instrumentation tests.
 *
 * Design notes:
 * - The top-level FriendsScreen prefers to be a thin presentation layer: it resolves the
 *   effective data and callbacks (either from provided params or the ViewModel) and passes plain
 *   data and lambda callbacks to child composables. This keeps child composables easy to unit
 *   test and review.
 * - Child composables are pure/presentation-focused and do not access the ViewModel directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = viewModel(),
    // Optional overrides for testing
    friends: List<FriendWithProfile>? = null,
    pendingRequests: List<FriendWithProfile>? = null,
    searchResults: List<UserProfile>? = null,
    searchQuery: String? = null,
    onRemoveFriend: ((String) -> Unit)? = null,
    onAcceptRequest: ((String) -> Unit)? = null,
    onRejectRequest: ((String) -> Unit)? = null,
    onSendFriendRequest: ((String) -> Unit)? = null,
    onSearchQueryChange: ((String) -> Unit)? = null
) {
    // --- State collection -------------------------------------------------
    // The primary production state is stored in the ViewModel and collected here using
    // collectAsState. Tests can provide concrete values via the optional params above which
    // override the ViewModel values when non-null.
    val selectedTab by viewModel.selectedTab.collectAsState()
    val vmFriends by viewModel.friends.collectAsState()
    val vmPending by viewModel.pendingRequests.collectAsState()
    val vmSearchResults by viewModel.searchResults.collectAsState()
    val vmSearchQuery by viewModel.searchQuery.collectAsState()

    // Resolve effective values: prefer explicit test overrides if provided.
    val friendsList = friends ?: vmFriends
    val pendingList = pendingRequests ?: vmPending
    val searchList = searchResults ?: vmSearchResults
    val searchQ = searchQuery ?: vmSearchQuery

    // Resolve callbacks so tests can inject behaviour; otherwise fall back to ViewModel methods.
    val removeCb = onRemoveFriend ?: viewModel::removeFriend
    val acceptCb = onAcceptRequest ?: viewModel::acceptRequest
    val rejectCb = onRejectRequest ?: viewModel::rejectRequest
    val sendCb = onSendFriendRequest ?: viewModel::sendFriendRequest
    val searchChangeCb = onSearchQueryChange ?: viewModel::updateSearchQuery

    // --- UI layout --------------------------------------------------------
    // Scaffold provides the top app bar and content area. A root testTag is added for tests.
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("friendsScreen"),
        topBar = {
            TopAppBar(
                title = { Text("Friends", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // Navigation icon calls the onNavigateBack callback passed in by the caller.
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // --- Tabs -------------------------------------------------------
            // TabRow is driven by the selectedTab from the ViewModel. Each Tab exposes a testTag
            // so tests can interact with it directly.
            TabRow(selectedTabIndex = selectedTab.ordinal, modifier = Modifier.fillMaxWidth().testTag("friendsTabRow")) {
                FriendsTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        modifier = Modifier.testTag("tab${tab.name}"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text(tab.title)
                                // Show a badge when there are pending requests (Requests tab only)
                                if (tab == FriendsTab.REQUESTS && pendingList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("requestBadge")) {
                                        Text(pendingList.size.toString(), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // --- Tab content ------------------------------------------------
            // The content area switches on the selectedTab. Child composables receive plain data
            // and callbacks, making them easy to test in isolation.
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTab) {
                    FriendsTab.FRIENDS -> {
                        FriendsListTab(friends = friendsList, onRemoveFriend = removeCb, modifier = Modifier.testTag("friendsListTab"))
                    }
                    FriendsTab.REQUESTS -> {
                        RequestsTab(requests = pendingList, onAcceptRequest = acceptCb, onRejectRequest = rejectCb, modifier = Modifier.testTag("requestsTab"))
                    }
                    FriendsTab.SEARCH -> {
                        SearchTab(searchQuery = searchQ, onSearchQueryChange = searchChangeCb, searchResults = searchList, onSendRequest = sendCb, modifier = Modifier.testTag("searchTab"))
                    }
                }
            }
        }
    }
}

/**
 * Logical representation of screen tabs. The title is used for the Tab label.
 */
enum class FriendsTab(val title: String) {
    FRIENDS("Friends"),
    REQUESTS("Requests"),
    SEARCH("Search")
}

// --------------------------- Child components ----------------------------

/**
 * FriendsListTab
 *
 * Pure presentation component that renders a list of FriendWithProfile items.
 * - Shows a reusable EmptyStateMessage when the list is empty.
 * - Calls onRemoveFriend with the user's id when removal is requested.
 */
@Composable
private fun FriendsListTab(friends: List<FriendWithProfile>, onRemoveFriend: (String) -> Unit, modifier: Modifier = Modifier) {
    if (friends.isEmpty()) {
        EmptyStateMessage(icon = Icons.Default.Person, message = "No friends yet", subtitle = "Search for friends to add them", modifier = modifier)
    } else {
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(friends, key = { it.userProfile.userId }) { friendWithProfile ->
                FriendCard(friend = friendWithProfile, onRemove = { onRemoveFriend(friendWithProfile.userProfile.userId) }, modifier = Modifier.testTag("friendCard_${friendWithProfile.userProfile.userId}"))
            }
        }
    }
}

/**
 * RequestsTab
 *
 * Renders pending friend requests and exposes accept/reject callbacks.
 */
@Composable
private fun RequestsTab(requests: List<FriendWithProfile>, onAcceptRequest: (String) -> Unit, onRejectRequest: (String) -> Unit, modifier: Modifier = Modifier) {
    if (requests.isEmpty()) {
        EmptyStateMessage(icon = Icons.Default.Notifications, message = "No pending requests", subtitle = "Friend requests will appear here", modifier = modifier)
    } else {
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(requests, key = { it.requestId }) { request ->
                FriendRequestCard(request = request, onAccept = { onAcceptRequest(request.requestId) }, onReject = { onRejectRequest(request.requestId) }, modifier = Modifier.testTag("requestCard_${request.requestId}"))
            }
        }
    }
}

/**
 * SearchTab
 *
 * Presents a search input and a list of search results. Behaviour is driven by the provided
 * searchQuery and searchResults; callbacks notify the parent of changes or actions.
 */
@Composable
private fun SearchTab(searchQuery: String, onSearchQueryChange: (String) -> Unit, searchResults: List<UserProfile>, onSendRequest: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().testTag("searchTextField"),
            placeholder = { Text("Search by name or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        when {
            searchQuery.isEmpty() -> EmptyStateMessage(icon = Icons.Default.Search, message = "Search for friends", subtitle = "Enter a name or email to find friends", modifier = Modifier.fillMaxSize())
            searchResults.isEmpty() -> EmptyStateMessage(icon = Icons.Default.SearchOff, message = "No results found", subtitle = "Try a different search term", modifier = Modifier.fillMaxSize())
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults, key = { it.userId }) { user ->
                    SearchResultCard(user = user, onSendRequest = { onSendRequest(user.userId) }, modifier = Modifier.testTag("searchResultCard_${user.userId}"))
                }
            }
        }
    }
}

/**
 * FriendCard
 *
 * Displays a single friend and manages a local confirmation dialog for removal.
 */
@Composable
private fun FriendCard(friend: FriendWithProfile, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(avatarUrl = friend.userProfile.avatarUrl, name = friend.userProfile.name, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = friend.userProfile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (friend.userProfile.location.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = friend.userProfile.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            IconButton(onClick = { showRemoveDialog = true }, modifier = Modifier.testTag("removeFriendButton")) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Remove friend", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${friend.userProfile.name} from your friends?") },
            confirmButton = { Button(onClick = { onRemove(); showRemoveDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") } }
        )
    }
}

/**
 * FriendRequestCard
 *
 * Presents a pending request with accept and reject actions.
 */
@Composable
private fun FriendRequestCard(request: FriendWithProfile, onAccept: () -> Unit, onReject: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(avatarUrl = request.userProfile.avatarUrl, name = request.userProfile.name, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = request.userProfile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (request.userProfile.bio.isNotEmpty()) {
                        Text(text = request.userProfile.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f).testTag("rejectButton"), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Decline")
                }

                Button(onClick = onAccept, modifier = Modifier.weight(1f).testTag("acceptButton")) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept")
                }
            }
        }
    }
}

/**
 * SearchResultCard
 *
 * Displays a search result and a button to send a friend request. Tracks a small local UI state
 * to represent the 'Sent' visual state after the user taps the button.
 */
@Composable
private fun SearchResultCard(user: UserProfile, onSendRequest: () -> Unit, modifier: Modifier = Modifier) {
    var requestSent by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(avatarUrl = user.avatarUrl, name = user.name, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (user.bio.isNotEmpty()) {
                    Text(text = user.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (requestSent) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.testTag("requestSentButton")) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sent")
                }
            } else {
                Button(onClick = { onSendRequest(); requestSent = true }, modifier = Modifier.testTag("sendRequestButton")) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
    }
}

/**
 * UserAvatar
 *
 * Lightweight avatar component that shows the first letter of the user's name when no URL is
 * present. Keeps the UI deterministic for tests and previews.
 */
@Composable
private fun UserAvatar(avatarUrl: String?, name: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (avatarUrl.isNullOrEmpty() || avatarUrl == "person") {
            Text(text = name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

/**
 * EmptyStateMessage
 *
 * Reusable component to show an icon, a primary message and a subtitle centered in the
 * available container. Used across tabs to show empty states.
 */
@Composable
private fun EmptyStateMessage(icon: ImageVector, message: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(text = message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

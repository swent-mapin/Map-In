package com.swent.mapin.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.SearchResultWithStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Friends screen.
 *
 * Manages the state and business logic for friend requests, friend lists, and user search
 * functionality. Includes real-time updates via Firestore listeners.
 *
 * @property repo Repository for friend request operations.
 * @property auth Firebase Auth instance for getting current user.
 */
class FriendsViewModel(
    private val repo: FriendRequestRepository = FriendRequestRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val currentUserId: String
    get() = auth.currentUser?.uid ?: ""

  private val _selectedTab = MutableStateFlow(FriendsTab.FRIENDS)
  /** Currently selected tab in the Friends screen. */
  val selectedTab: StateFlow<FriendsTab> = _selectedTab

  private val _friends = MutableStateFlow<List<FriendWithProfile>>(emptyList())
  /** List of the current user's friends. */
  val friends: StateFlow<List<FriendWithProfile>> = _friends

  private val _pendingRequests = MutableStateFlow<List<FriendWithProfile>>(emptyList())
  /** List of pending friend requests received by the current user. */
  val pendingRequests: StateFlow<List<FriendWithProfile>> = _pendingRequests

  private val _searchResults = MutableStateFlow<List<SearchResultWithStatus>>(emptyList())
  /** Search results for finding new friends. */
  val searchResults: StateFlow<List<SearchResultWithStatus>> = _searchResults

  private val _searchQuery = MutableStateFlow("")
  /** Current search query text. */
  val searchQuery: StateFlow<String> = _searchQuery

  init {
    if (currentUserId.isNotEmpty()) {
      startRealtimeObservers()
    }
  }

  /**
   * Starts real-time Firestore listeners for friends and pending requests. Updates are pushed
   * automatically when data changes in Firestore.
   */
  private fun startRealtimeObservers() {
    // Observe friends in real-time
    viewModelScope.launch {
      repo.observeFriends(currentUserId).collect { friendsList -> _friends.value = friendsList }
    }

    // Observe pending requests in real-time
    viewModelScope.launch {
      repo.observePendingRequests(currentUserId).collect { requests ->
        _pendingRequests.value = requests
      }
    }
  }

  /**
   * Switches to the specified tab.
   *
   * @param tab The tab to select.
   */
  fun selectTab(tab: FriendsTab) {
    _selectedTab.value = tab
  }

  /** Loads the current user's friends list from the repository. */
  fun loadFriends() {
    viewModelScope.launch { _friends.value = repo.getFriends(currentUserId) }
  }

  /** Loads pending friend requests from the repository. */
  fun loadPendingRequests() {
    viewModelScope.launch { _pendingRequests.value = repo.getPendingRequests(currentUserId) }
  }

  /**
   * Updates the search query and triggers a new search.
   *
   * @param query The search text entered by the user.
   */
  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
    viewModelScope.launch {
      _searchResults.value = repo.searchUsersWithStatus(query, currentUserId)
    }
  }

  /**
   * Accepts a pending friend request.
   *
   * @param requestId The ID of the request to accept.
   */
  fun acceptRequest(requestId: String) {
    viewModelScope.launch {
      if (repo.acceptFriendRequest(requestId)) {
        loadFriends()
        loadPendingRequests()
      }
    }
  }

  /**
   * Rejects a pending friend request.
   *
   * @param requestId The ID of the request to reject.
   */
  fun rejectRequest(requestId: String) {
    viewModelScope.launch { if (repo.rejectFriendRequest(requestId)) loadPendingRequests() }
  }

  /**
   * Removes a user from the friends list (bidirectional operation).
   *
   * @param userId The ID of the friend to remove.
   */
  fun removeFriend(userId: String) {
    viewModelScope.launch { if (repo.removeFriendship(currentUserId, userId)) loadFriends() }
  }

  /**
   * Sends a friend request to another user.
   *
   * @param userId The ID of the user to send a request to.
   */
  fun sendFriendRequest(userId: String) {
    viewModelScope.launch {
      if (repo.sendFriendRequest(currentUserId, userId)) {
        _searchResults.value = repo.searchUsersWithStatus(_searchQuery.value, currentUserId)
      }
    }
  }
}

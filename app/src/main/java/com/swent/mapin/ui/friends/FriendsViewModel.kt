package com.swent.mapin.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.swent.mapin.model.FriendWithProfile
import com.swent.mapin.model.UserProfile

// Assisted by AI


/**
 * ViewModel for the Friends screen.
 *
 * Responsibilities:
 * - Expose UI state as StateFlow instances so composables can collect them.
 * - Provide intent-style functions to mutate state (selectTab, updateSearchQuery, accept/reject/remove/send actions).
 * - Perform any asynchronous work (e.g. searching, network calls) from the ViewModelScope to keep UI reactive.
 *
 * Notes for testing and usage:
 * - All public state is exposed as immutable StateFlow to prevent external mutation.
 * - Methods in this class should remain fast and non-blocking; longer operations should launch coroutines
 *   on viewModelScope and update the MutableStateFlow properties when results are ready.
 */
class FriendsViewModel : ViewModel() {

    // The currently selected tab. Exposed as StateFlow so the UI can react to changes.
    // Default is FRIENDS tab.
    private val _selectedTab = MutableStateFlow(FriendsTab.FRIENDS)
    val selectedTab: StateFlow<FriendsTab> = _selectedTab

    // The list of friends with profile metadata used to render the friends list.
    // Keep this as MutableStateFlow to allow updates; expose as immutable StateFlow.
    private val _friends = MutableStateFlow<List<FriendWithProfile>>(emptyList())
    val friends: StateFlow<List<FriendWithProfile>> = _friends

    // Pending friend requests displayed in the Requests tab. Similar pattern as _friends.
    private val _pendingRequests = MutableStateFlow<List<FriendWithProfile>>(emptyList())
    val pendingRequests: StateFlow<List<FriendWithProfile>> = _pendingRequests

    // Results produced by a search operation. Represents UserProfile items that can be added as friends.
    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults

    // Current text query used for searching. UI writes to this via updateSearchQuery().
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Switch the currently selected tab.
     * This is a simple synchronous state change and safe to call from the UI thread.
     * @param tab the FriendsTab to select
     */
    fun selectTab(tab: FriendsTab) {
        _selectedTab.value = tab
    }

    /**
     * Update the search query and trigger a search operation.
     * Side effects:
     * - sets _searchQuery immediately to reflect UI typing
     * - launches a coroutine to perform the (potentially expensive) search
     *   and updates _searchResults when complete.
     *
     * Implementation detail:
     * - performSearch is a suspend function; it runs on viewModelScope to keep UI responsive.
     * - In a real app this would call a repository or network layer; here it calls performSearch()
     *   which is a stub returning an empty list. Replace this with actual search logic as needed.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // Simulate search logic
        viewModelScope.launch {
            _searchResults.value = performSearch(query)
        }
    }

    /**
     * Accept a pending friend request.
     * Expected behavior (not implemented here):
     * - call repository to accept the request on the backend
     * - update local state (_pendingRequests and _friends) accordingly
     *
     * Keep this method lightweight; perform network or DB operations inside viewModelScope.
     */
    fun acceptRequest(requestId: String) {
        // Logic to accept a friend request
    }

    /**
     * Reject a pending friend request.
     * Expected behavior (not implemented here):
     * - call repository to reject the request and remove it from _pendingRequests
     */
    fun rejectRequest(requestId: String) {
        // Logic to reject a friend request
    }

    /**
     * Remove a user from the friends list.
     * Expected behavior:
     * - update backend via repository
     * - update local _friends to remove the corresponding FriendWithProfile
     */
    fun removeFriend(userId: String) {
        // Logic to remove a friend
    }

    /**
     * Send a friend request to a user id.
     * Expected behavior:
     * - call send on repository/backend
     * - optionally update UI state (e.g. reflect pending request or change button state)
     */
    fun sendFriendRequest(userId: String) {
        // Logic to send a friend request
    }

    /**
     * Perform the actual search operation.
     * This is a suspend function so it can perform I/O without blocking the main thread.
     * Currently returns an empty list as a placeholder. Replace with repository call.
     */
    private suspend fun performSearch(query: String): List<UserProfile> {
        // Simulate search results
        return emptyList()
    }
}

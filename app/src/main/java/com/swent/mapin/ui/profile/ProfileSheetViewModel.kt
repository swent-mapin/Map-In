package com.swent.mapin.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.friends.FriendRequestRepository
import com.swent.mapin.model.friends.FriendshipStatus
import com.swent.mapin.model.notifications.NotificationService
import com.swent.mapin.model.userprofile.UserProfile
import com.swent.mapin.model.userprofile.UserProfileRepository
import kotlinx.coroutines.launch

/**
 * Sealed class representing the state of the profile sheet.
 *
 * This follows the loading-loaded-error pattern for async data fetching.
 */
sealed class ProfileSheetState {
  /** Initial state while profile data is being fetched */
  object Loading : ProfileSheetState()

  /**
   * Success state with loaded profile data.
   *
   * @property profile The user's profile information
   * @property upcomingEvents List of future/ongoing events owned by this user
   * @property pastEvents List of past events owned by this user
   * @property isFollowing Whether the current user is following this profile owner
   * @property isOwnProfile Whether this is the current user's own profile
   * @property friendStatus Current friendship status with this user
   */
  data class Loaded(
      val profile: UserProfile,
      val upcomingEvents: List<Event>,
      val pastEvents: List<Event>,
      val isFollowing: Boolean,
      val isOwnProfile: Boolean,
      val friendStatus: FriendStatus
  ) : ProfileSheetState()

  /**
   * Error state when profile loading fails.
   *
   * @property message Human-readable error message
   */
  data class Error(val message: String) : ProfileSheetState()
}

/** Enum representing the friendship status between current user and profile owner. */
enum class FriendStatus {
  /** No friendship or request exists */
  NOT_FRIEND,
  /** Friend request is pending */
  PENDING,
  /** Users are friends */
  FRIENDS
}

/**
 * ViewModel for the ProfileSheet component.
 *
 * Manages the state and operations for viewing another user's profile in a bottom sheet. Handles:
 * - Loading user profile and their events
 * - Checking friendship and following status
 * - Following/unfollowing users
 * - Sending and managing friend requests
 *
 * @property userProfileRepository Repository for user profile operations
 * @property eventRepository Repository for event operations
 * @property friendRequestRepository Repository for friend request operations
 * @property auth Firebase authentication instance
 */
class ProfileSheetViewModel(
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(),
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository(),
    private val friendRequestRepository: FriendRequestRepository? =
        FriendRequestRepository(notificationService = NotificationService()),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  var state by mutableStateOf<ProfileSheetState>(ProfileSheetState.Loading)
    private set

  private var currentTargetUserId: String? = null

  fun loadProfile(userId: String) {
    currentTargetUserId = userId
    state = ProfileSheetState.Loading

    viewModelScope.launch {
      try {
        val profile = userProfileRepository.getUserProfile(userId)
        if (profile == null) {
          state = ProfileSheetState.Error("Profile not found")
          return@launch
        }

        val currentUserId = auth.currentUser?.uid
        val isOwnProfile = currentUserId == userId
        val isFollowing =
            if (currentUserId != null && !isOwnProfile) {
              userProfileRepository.isFollowing(currentUserId, userId)
            } else {
              false
            }
        val friendStatus =
            if (currentUserId != null && !isOwnProfile) {
              friendStatusFrom(friendRequestRepository?.getFriendshipStatus(currentUserId, userId))
            } else {
              FriendStatus.NOT_FRIEND
            }

        val ownedEvents = eventRepository.getOwnedEvents(userId)
        val now = System.currentTimeMillis()

        // An event is upcoming/ongoing if its end date hasn't passed yet
        // If no end date, fall back to start date
        val upcomingEvents =
            ownedEvents
                .filter { event ->
                  val endTime = event.endDate?.toDate()?.time ?: event.date?.toDate()?.time
                  endTime?.let { it >= now } ?: false
                }
                .sortedBy { it.date?.toDate()?.time }

        // An event is past if its end date has passed
        val pastEvents =
            ownedEvents
                .filter { event ->
                  val endTime = event.endDate?.toDate()?.time ?: event.date?.toDate()?.time
                  endTime?.let { it < now } ?: false
                }
                .sortedByDescending { it.date?.toDate()?.time }

        state =
            ProfileSheetState.Loaded(
                profile = profile,
                upcomingEvents = upcomingEvents,
                pastEvents = pastEvents,
                isFollowing = isFollowing,
                isOwnProfile = isOwnProfile,
                friendStatus = friendStatus)
      } catch (e: Exception) {
        state = ProfileSheetState.Error(e.message ?: "Failed to load profile")
      }
    }
  }

  fun toggleFollow() {
    val currentState = state as? ProfileSheetState.Loaded ?: return
    val currentUserId = auth.currentUser?.uid ?: return
    val targetUserId = currentTargetUserId ?: return

    if (currentState.isOwnProfile) return

    viewModelScope.launch {
      val success =
          if (currentState.isFollowing) {
            userProfileRepository.unfollowUser(currentUserId, targetUserId)
          } else {
            userProfileRepository.followUser(currentUserId, targetUserId)
          }

      if (success) {
        // Reload to get updated follower counts
        loadProfile(targetUserId)
      }
    }
  }

  fun sendFriendRequest() {
    val currentState = state as? ProfileSheetState.Loaded ?: return
    val currentUserId = auth.currentUser?.uid ?: return
    val targetUserId = currentState.profile.userId

    viewModelScope.launch {
      val success = friendRequestRepository?.sendFriendRequest(currentUserId, targetUserId) == true
      if (success) {
        loadProfile(targetUserId)
      }
    }
  }
}

internal fun friendStatusFrom(status: FriendshipStatus?): FriendStatus {
  return when (status) {
    FriendshipStatus.ACCEPTED -> FriendStatus.FRIENDS
    FriendshipStatus.PENDING -> FriendStatus.PENDING
    else -> FriendStatus.NOT_FRIEND
  }
}

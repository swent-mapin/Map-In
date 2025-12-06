package com.swent.mapin.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import kotlinx.coroutines.launch

/** State for the profile sheet */
sealed class ProfileSheetState {
  object Loading : ProfileSheetState()

  data class Loaded(
      val profile: UserProfile,
      val upcomingEvents: List<Event>,
      val pastEvents: List<Event>,
      val isFollowing: Boolean,
      val isOwnProfile: Boolean
  ) : ProfileSheetState()

  data class Error(val message: String) : ProfileSheetState()
}

/** ViewModel for the ProfileSheet component */
class ProfileSheetViewModel(
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(),
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository(),
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

        val ownedEvents = eventRepository.getOwnedEvents(userId)
        val now = System.currentTimeMillis()

        val upcomingEvents =
            ownedEvents
                .filter { event -> event.date?.toDate()?.time?.let { it >= now } ?: false }
                .sortedBy { it.date?.toDate()?.time }

        val pastEvents =
            ownedEvents
                .filter { event -> event.date?.toDate()?.time?.let { it < now } ?: false }
                .sortedByDescending { it.date?.toDate()?.time }

        state =
            ProfileSheetState.Loaded(
                profile = profile,
                upcomingEvents = upcomingEvents,
                pastEvents = pastEvents,
                isFollowing = isFollowing,
                isOwnProfile = isOwnProfile)
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
}

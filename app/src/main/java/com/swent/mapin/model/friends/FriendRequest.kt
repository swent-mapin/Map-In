package com.swent.mapin.model.friends

import com.google.firebase.Timestamp
import com.swent.mapin.model.userprofile.UserProfile

/** Represents the status of a friendship or friend request. */
enum class FriendshipStatus {
  PENDING,
  ACCEPTED,
  REJECTED,
  BLOCKED
}

/**
 * Represents a friend request or friendship between two users.
 *
 * @property requestId Unique identifier for the request.
 * @property fromUserId User ID of the person who sent the request.
 * @property toUserId User ID of the person who received the request.
 * @property status Current status of the friendship.
 * @property timestamp When the request was created or last updated.
 */
data class FriendRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val timestamp: Timestamp = Timestamp.now()
)

/**
 * Represents a friend with their profile information and friendship status.
 *
 * @property userProfile The user's profile information.
 * @property friendshipStatus Current friendship status with this user.
 * @property requestId ID of the friend request (if applicable).
 */
data class FriendWithProfile(
    val userProfile: UserProfile,
    val friendshipStatus: FriendshipStatus = FriendshipStatus.PENDING,
    val requestId: String = ""
)

/**
 * Represents a search result with request status information.
 *
 * @property userProfile The user's profile information.
 * @property hasPendingRequest Whether there's a pending outgoing request to this user.
 */
data class SearchResultWithStatus(
    val userProfile: UserProfile,
    val hasPendingRequest: Boolean = false
)

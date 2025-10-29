package com.swent.mapin.model

import com.google.firebase.Timestamp

// Assisted by AI


/** Represents the status of a friendship or friend request. */
enum class FriendshipStatus {
  PENDING, // Request sent but not yet accepted
  ACCEPTED, // Request accepted, users are friends
  REJECTED, // Request rejected
  BLOCKED // User is blocked
}

/**
 * Data class representing a friend request or friendship.
 *
 * @property requestId Unique identifier for the request
 * @property fromUserId User ID of the person who sent the request
 * @property toUserId User ID of the person who received the request
 * @property status Current status of the request/friendship
 * @property timestamp When the request was created or last updated
 */
data class FriendRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val timestamp: Timestamp = Timestamp.now()
)

/**
 * Data class representing a user with friendship information. Used for displaying friends in the
 * UI.
 *
 * @property userProfile The user's profile information
 * @property friendshipStatus Current friendship status with this user
 * @property requestId ID of the friend request (if applicable)
 */
data class FriendWithProfile(
    val userProfile: UserProfile,
    val friendshipStatus: FriendshipStatus = FriendshipStatus.PENDING,
    val requestId: String = ""
)

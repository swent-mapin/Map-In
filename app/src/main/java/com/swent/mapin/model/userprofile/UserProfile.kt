package com.swent.mapin.model.userprofile

import com.swent.mapin.model.badge.Badge

/**
 * Data class representing a user's profile information.
 *
 * @property userId Unique identifier for the user
 * @property name User's display name
 * @property bio Short biography or description
 * @property hobbies List of user's hobbies/interests
 * @property location User's location (city, country, etc.)
 * @property profilePictureUrl URL to the user's profile picture (optional)
 * @property avatarUrl URL to the user's avatar (optional)
 * @property bannerUrl URL to the user's banner image (optional)
 * @property hobbiesVisible Whether the hobbies are visible to others
 * @property joinedEventIds List of event IDs the user is attending/participating in
 * @property savedEventIds List of event IDs the user has saved/bookmarked
 * @property ownedEventIds List of event IDs the user has created/organizes
 * @property fcmToken Firebase Cloud Messaging token for push notifications (optional)
 * @property badges List of user-specific badges with progress and unlock status
 */
data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val bio: String = "",
    val hobbies: List<String> = emptyList(),
    val location: String = "",
    val profilePictureUrl: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val hobbiesVisible: Boolean = true,
    val joinedEventIds: List<String> = emptyList(),
    val savedEventIds: List<String> = emptyList(),
    val ownedEventIds: List<String> = emptyList(),
    val fcmToken: String? = null,
    val badges: List<Badge> = emptyList(),
    val followingIds: List<String> = emptyList(),
    val followerIds: List<String> = emptyList()
)

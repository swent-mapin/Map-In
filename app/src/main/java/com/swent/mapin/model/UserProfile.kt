package com.swent.mapin.model

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
    val hobbiesVisible: Boolean = true
)

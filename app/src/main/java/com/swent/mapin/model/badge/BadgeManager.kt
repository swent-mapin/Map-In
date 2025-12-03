package com.swent.mapin.model.badge

import com.swent.mapin.model.UserProfile

/**
 * Singleton object responsible for calculating badge unlock status and progress.
 *
 * This manager handles all badge-related calculations in a stateless, thread-safe manner.
 * Each badge type has its own calculation logic based on user profile data and social metrics.
 *
 * Usage:
 * ```
 * val badges = BadgeManager.calculateBadges(userProfile, friendsCount)
 * ```
 */
object BadgeManager {

  /**
   * Calculate all badges for a user based on their profile and social metrics.
   *
   * This method recalculates all badge statuses and progress values from scratch.
   * It is thread-safe and can be called concurrently from multiple coroutines.
   *
   * @param userProfile The user's profile containing all profile data
   * @param friendsCount The number of friends the user has
   * @return List of all badges with updated unlock status and progress
   */
  fun calculateBadges(userProfile: UserProfile, friendsCount: Int): List<Badge> {
    return listOf(
        calculateFriendlyBadge(friendsCount),
        calculateProfileProBadge(userProfile)
    )
  }

  /**
   * Calculate the "Friendly" badge (COMMON rarity).
   *
   * Unlock criteria: User has at least 1 friend
   * Progress: Binary (0% or 100%)
   *
   * @param friendsCount Number of friends the user has
   * @return Badge with unlock status and progress
   */
  private fun calculateFriendlyBadge(friendsCount: Int): Badge {
    val isUnlocked = friendsCount >= 1
    val progress = if (isUnlocked) 1f else 0f

    return Badge(
        id = "friendly",
        title = "Friendly",
        description = "Make your first friend",
        iconName = "face",
        rarity = BadgeRarity.COMMON,
        isUnlocked = isUnlocked,
        progress = progress
    )
  }

  /**
   * Calculate the "Profile Pro" badge (RARE rarity).
   *
   * Unlock criteria: All 5 profile fields must be completed:
   * 1. Name is not empty
   * 2. Bio is filled (not default placeholder)
   * 3. Location is filled (not "Unknown")
   * 4. At least one hobby added
   * 5. Profile picture exists (avatarUrl or profilePictureUrl)
   *
   * Progress: 20% per completed field (0.0 to 1.0)
   *
   * @param userProfile The user's profile data
   * @return Badge with unlock status and progress
   */
  private fun calculateProfileProBadge(userProfile: UserProfile): Badge {
    var completedFields = 0

    // Check 1: Name is not empty
    if (userProfile.name.isNotEmpty()) {
      completedFields++
    }

    // Check 2: Bio is filled (not default placeholder)
    if (userProfile.bio.isNotEmpty() &&
        userProfile.bio != "Tell us about yourself...") {
      completedFields++
    }

    // Check 3: Location is filled (not "Unknown")
    if (userProfile.location.isNotEmpty() &&
        userProfile.location != "Unknown") {
      completedFields++
    }

    // Check 4: At least one hobby added
    if (userProfile.hobbies.isNotEmpty()) {
      completedFields++
    }

    // Check 5: Profile picture exists (check both avatarUrl and profilePictureUrl)
    // Accept if either avatarUrl is valid (not null, not empty, not default "person")
    // OR profilePictureUrl is valid (not null, not empty)
    val hasValidAvatarUrl = !userProfile.avatarUrl.isNullOrEmpty() &&
        userProfile.avatarUrl != "person"
    val hasValidProfilePictureUrl = !userProfile.profilePictureUrl.isNullOrEmpty()
    val hasProfilePicture = hasValidAvatarUrl || hasValidProfilePictureUrl

    if (hasProfilePicture) {
      completedFields++
    }

    // Calculate progress (20% per field = 0.2f per field)
    val progress = completedFields / 5f
    val isUnlocked = completedFields == 5

    return Badge(
        id = "profile_pro",
        title = "Profile Pro",
        description = "Complete all profile fields",
        iconName = "person",
        rarity = BadgeRarity.RARE,
        isUnlocked = isUnlocked,
        progress = progress
    )
  }
}


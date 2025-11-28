package com.swent.mapin.model.badge

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import com.swent.mapin.model.UserProfile

// Assisted by AI

/**
 * Manager for calculating badge unlock status and progress.
 *
 * This class provides simple logic to determine which badges should be unlocked based on the user's
 * profile and friends list.
 */
object BadgeManager {

  /**
   * Calculates the status of all badges for a given user.
   *
   * @param userProfile The user's profile data
   * @param friendsCount The number of friends the user has
   * @return List of badges with updated unlock status and progress
   */
  fun calculateBadges(userProfile: UserProfile, friendsCount: Int): List<Badge> {
    return listOf(calculateFriendlyBadge(friendsCount), calculateProfileProBadge(userProfile))
  }

  /** "Friendly" badge: Unlocked when the user has at least 1 friend. */
  private fun calculateFriendlyBadge(friendsCount: Int): Badge {
    val isUnlocked = friendsCount >= 1
    val progress = if (friendsCount >= 1) 1f else 0f

    return Badge(
        id = "first_friend",
        title = "Friendly",
        description = "Add your first friend",
        icon = Icons.Default.Face,
        rarity = BadgeRarity.COMMON,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  /**
   * "Profile Pro" badge: Unlocked when all profile fields are filled and the user has a profile
   * picture.
   *
   * Required fields:
   * - name (not empty)
   * - bio (not empty and not default placeholder)
   * - location (not empty and not default placeholder)
   * - hobbies (at least one)
   * - avatarUrl or profilePictureUrl (not null)
   */
  private fun calculateProfileProBadge(userProfile: UserProfile): Badge {
    // Check each required field
    val hasName = userProfile.name.isNotEmpty()
    val hasBio = userProfile.bio.isNotEmpty() && userProfile.bio != "Tell us about yourself..."
    val hasLocation = userProfile.location.isNotEmpty() && userProfile.location != "Unknown"
    val hasHobbies = userProfile.hobbies.isNotEmpty()
    val hasProfilePicture =
        !userProfile.avatarUrl.isNullOrEmpty() || !userProfile.profilePictureUrl.isNullOrEmpty()

    // Calculate progress (each field represents 20% of progress)
    val fieldsCompleted = listOf(hasName, hasBio, hasLocation, hasHobbies, hasProfilePicture)
    val progress = fieldsCompleted.count { it } / 5f

    val isUnlocked = hasName && hasBio && hasLocation && hasHobbies && hasProfilePicture

    return Badge(
        id = "profile_complete",
        title = "Profile Pro",
        description = "Complete your profile",
        icon = Icons.Default.Person,
        rarity = BadgeRarity.RARE,
        isUnlocked = isUnlocked,
        progress = progress)
  }
}

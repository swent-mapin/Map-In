package com.swent.mapin.model.badge

/**
 * Repository interface for managing badge data persistence.
 *
 * This interface defines the contract for saving and retrieving badge data. Implementations should
 * handle Firestore operations, caching, and error handling.
 */
interface BadgeRepository {

  /**
   * Save badge progress for a user.
   *
   * Stores the complete list of badges with their current unlock status and progress to persistent
   * storage (Firestore).
   *
   * @param userId The unique identifier of the user
   * @param badges The list of badges to save
   * @return True if save operation succeeded, false otherwise
   */
  suspend fun saveBadgeProgress(userId: String, badges: List<Badge>): Boolean

  /**
   * Retrieve badges for a user.
   *
   * Fetches the user's badge data from persistent storage. Returns null if no badge data exists or
   * if an error occurs.
   *
   * @param userId The unique identifier of the user
   * @return List of badges if found, null otherwise
   */
  suspend fun getUserBadges(userId: String): List<Badge>?

  /**
   * Saved the BadgeContext item of a user
   *
   * @param userId The unique identifier of the user
   * @param context The metadata stored for badges of a user
   * @return True if saved successfully, false if failed
   */
  suspend fun saveBadgeContext(userId: String, context: BadgeContext): Boolean

  /**
   * Gets the BadgeContext item of a user
   *
   * @param userId The unique identifier of the user
   * @return True if saved successfully, false if failed
   */
  suspend fun getBadgeContext(userId: String): BadgeContext

  suspend fun updateBadgesAfterContextChange(userId: String)

  /**
   * Clears the cache for a specific user.
   *
   * This is useful when the badge context may have been modified by another component (e.g.,
   * another repository instance) and we need to ensure fresh data is fetched.
   *
   * @param userId The unique identifier of the user whose cache should be cleared
   */
  fun clearCache(userId: String)

  /**
   * Update the unlock status of a specific badge.
   *
   * Updates a single badge's unlock status and timestamp without affecting other badges. Useful for
   * triggering badge unlock notifications.
   *
   * @param userId The unique identifier of the user
   * @param badgeId The unique identifier of the badge
   * @param isUnlocked The new unlock status
   * @param timestamp The timestamp when the badge was unlocked
   * @return True if update succeeded, false otherwise
   */
  suspend fun updateBadgeUnlockStatus(
      userId: String,
      badgeId: String,
      isUnlocked: Boolean,
      timestamp: Long
  ): Boolean
}

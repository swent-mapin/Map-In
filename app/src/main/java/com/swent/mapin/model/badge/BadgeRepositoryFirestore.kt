package com.swent.mapin.model.badge

import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.UserProfile
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.tasks.await

data class BadgeContext(
    val friendsCount: Int = 0,
    val createdEvents: Int = 0,
    val joinedEvents: Int = 0,
    val earlyJoin: Int = 0, // user joined an event in the morning
    val lateJoin: Int = 0, // user joined an event in the evening
    val earlyCreate: Int = 0, // user created an event in the morning
    val lateCreate: Int = 0, // user created an event in the evening
)
/**
 * Firestore implementation of BadgeRepository.
 *
 * This implementation stores badges in the user's profile document under the "badges" field. It
 * includes caching to minimize Firestore reads and retry logic for transient failures.
 *
 * Firestore structure:
 * ```
 * users/{userId}
 *   - badges: List<Badge>
 * ```
 */
class BadgeRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : BadgeRepository {

  companion object {
    private const val COLLECTION_USERS = "users"
    private const val FIELD_BADGES = "badges"
    private const val FIELD_BADGE_CONTEXT = "badgeContext"
    private const val MAX_RETRIES = 3
  }

  // Simple in-memory cache to minimize Firestore reads
  private val badgeCache = ConcurrentHashMap<String, List<Badge>>()
  private val badgeContextCache = ConcurrentHashMap<String, BadgeContext>()

  /** Save badge context for a user to Firestore */
  override suspend fun saveBadgeContext(userId: String, context: BadgeContext): Boolean {
    return executeWithRetry {
      try {
        firestore
            .collection(COLLECTION_USERS)
            .document(userId)
            .update(FIELD_BADGE_CONTEXT, context)
            .await()
        badgeContextCache[userId] = context
        println("Saved BadgeContext for user $userId: $context")
        true
      } catch (e: Exception) {
        println("Error saving BadgeContext: ${e.message}")
        e.printStackTrace()
        false
      }
    } ?: false
  }

  override suspend fun getBadgeContext(userId: String): BadgeContext {
    // Return from cache if available
    badgeContextCache[userId]?.let {
      return it
    }

    return executeWithRetry {
      val doc = firestore.collection(COLLECTION_USERS).document(userId).get().await()

      if (doc.exists()) {
        // Existing user
        val context = doc.toObject(BadgeContext::class.java)

        if (context != null) {
          // Found badgeContext in Firestore → normal case
          badgeContextCache[userId] = context
          return@executeWithRetry context
        } else {
          // No badgeContext → create it to maintain compatibility
          val defaultCtx = BadgeContext()
          saveBadgeContext(userId, defaultCtx)
          badgeContextCache[userId] = defaultCtx
          println("Created missing BadgeContext for legacy user $userId")
          return@executeWithRetry defaultCtx
        }
      } else {
        // User doesn't exist — shouldn't happen, but safe fallback
        val defaultCtx = BadgeContext()
        println("User document missing for $userId — returning default BadgeContext")
        return@executeWithRetry defaultCtx
      }
    } ?: BadgeContext()
  }

  override suspend fun updateBadgesAfterContextChange(userId: String) {
    val ctx = getBadgeContext(userId)
    val profileBadges = getUserBadges(userId) ?: emptyList()
    val updatedBadges = BadgeManager.calculateBadges(UserProfile(badges = profileBadges), ctx)
    saveBadgeProgress(userId, updatedBadges)
  }

  /**
   * Save badge progress for a user to Firestore.
   *
   * Updates the badges field in the user's document. If the document doesn't exist, this operation
   * will fail gracefully and return false.
   *
   * @param userId The unique identifier of the user
   * @param badges The list of badges to save
   * @return True if save operation succeeded, false otherwise
   */
  override suspend fun saveBadgeProgress(userId: String, badges: List<Badge>): Boolean {
    return executeWithRetry {
      try {
        firestore.collection(COLLECTION_USERS).document(userId).update(FIELD_BADGES, badges).await()

        // Update cache on successful save
        badgeCache[userId] = badges

        println(
            "BadgeRepositoryFirestore - Successfully saved ${badges.size} badges for user $userId")
        true
      } catch (e: Exception) {
        println("BadgeRepositoryFirestore - Error saving badges: ${e.message}")
        e.printStackTrace()
        false
      }
    } ?: false // Handle null return from executeWithRetry
  }

  /**
   * Retrieve badges for a user from Firestore.
   *
   * First checks the cache, then fetches from Firestore if not cached. Returns null if the user
   * document doesn't exist or if an error occurs.
   *
   * @param userId The unique identifier of the user
   * @return List of badges if found, null otherwise
   */
  override suspend fun getUserBadges(userId: String): List<Badge>? {
    // Check cache first
    badgeCache[userId]?.let {
      println("BadgeRepositoryFirestore - Returning cached badges for user $userId")
      return it
    }

    return executeWithRetry {
      try {
        val document = firestore.collection(COLLECTION_USERS).document(userId).get().await()

        if (document.exists()) {
          @Suppress("UNCHECKED_CAST")
          val badgesData = document.get(FIELD_BADGES) as? List<Map<String, Any>>

          val badges =
              badgesData?.mapNotNull { badgeMap ->
                try {
                  Badge(
                      id = badgeMap["id"] as? String ?: "",
                      title = badgeMap["title"] as? String ?: "",
                      description = badgeMap["description"] as? String ?: "",
                      iconName = badgeMap["iconName"] as? String ?: "",
                      rarity = BadgeRarity.valueOf(badgeMap["rarity"] as? String ?: "COMMON"),
                      isUnlocked = badgeMap["isUnlocked"] as? Boolean ?: false,
                      progress = (badgeMap["progress"] as? Number)?.toFloat() ?: 0f)
                } catch (e: Exception) {
                  println("BadgeRepositoryFirestore - Error parsing badge: ${e.message}")
                  null
                }
              } ?: emptyList()

          // Cache the result
          if (badges.isNotEmpty()) {
            badgeCache[userId] = badges
          }

          println("BadgeRepositoryFirestore - Retrieved ${badges.size} badges for user $userId")
          badges
        } else {
          println("BadgeRepositoryFirestore - No document found for user $userId")
          null
        }
      } catch (e: Exception) {
        println("BadgeRepositoryFirestore - Error retrieving badges: ${e.message}")
        e.printStackTrace()
        null
      }
    }
  }

  /**
   * Update the unlock status of a specific badge.
   *
   * This method updates a single badge within the badges array without affecting others. Note: This
   * implementation refetches all badges, updates the specific one, and saves back.
   *
   * @param userId The unique identifier of the user
   * @param badgeId The unique identifier of the badge
   * @param isUnlocked The new unlock status
   * @param timestamp The timestamp when the badge was unlocked (currently unused but kept for
   *   interface compatibility)
   * @return True if update succeeded, false otherwise
   */
  override suspend fun updateBadgeUnlockStatus(
      userId: String,
      badgeId: String,
      isUnlocked: Boolean,
      timestamp: Long
  ): Boolean {
    return executeWithRetry {
      try {
        // Fetch current badges
        val currentBadges = getUserBadges(userId) ?: return@executeWithRetry false

        // Update the specific badge
        val updatedBadges =
            currentBadges.map { badge ->
              if (badge.id == badgeId) {
                badge.copy(isUnlocked = isUnlocked)
              } else {
                badge
              }
            }

        // Save back to Firestore
        saveBadgeProgress(userId, updatedBadges)
      } catch (e: Exception) {
        println("BadgeRepositoryFirestore - Error updating badge unlock status: ${e.message}")
        e.printStackTrace()
        false
      }
    } ?: false // Handle null return from executeWithRetry
  }

  /**
   * Execute a Firestore operation with retry logic for transient failures.
   *
   * Retries up to MAX_RETRIES times with exponential backoff.
   *
   * @param operation The suspend function to execute
   * @return Result of the operation, or default value (false/null) on failure
   */
  private suspend fun <T> executeWithRetry(operation: suspend () -> T): T? {
    repeat(MAX_RETRIES) { attempt ->
      try {
        return operation()
      } catch (_: Exception) {
        if (attempt == MAX_RETRIES - 1) {
          // Last attempt failed, give up
          println("BadgeRepositoryFirestore - Operation failed after $MAX_RETRIES attempts")
          return null
        }
        // Wait before retrying (exponential backoff: 100ms, 200ms, 400ms)
        kotlinx.coroutines.delay(100L * (1L shl attempt))
      }
    }
    return null
  }
}

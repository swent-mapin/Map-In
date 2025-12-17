package com.swent.mapin.model.badge

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 *   - badgeContext: BadgeContext
 * ```
 */
class BadgeRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(firestore)
) : BadgeRepository {

  companion object {
    private const val TAG = "BadgeRepositoryFirestore"
    private const val COLLECTION_USERS = "users"
    private const val FIELD_BADGES = "badges"
    private const val FIELD_BADGE_CONTEXT = "badgeContext"
    private const val MAX_RETRIES = 3
  }

  // Simple in-memory cache to minimize Firestore reads
  private val badgeCache = ConcurrentHashMap<String, List<Badge>>()
  private val badgeContextCache = ConcurrentHashMap<String, BadgeContext>()

  // Mutex to ensure atomic cache invalidation and fetch operations
  private val updateMutex = Mutex()

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
        Log.d(TAG, "Saved BadgeContext for user $userId: $context")
        true
      } catch (e: Exception) {
        Log.e(TAG, "Error saving BadgeContext for user $userId", e)
        false
      }
    } ?: false
  }

  override suspend fun getBadgeContext(userId: String): BadgeContext {

    // Return from cache if available
    badgeContextCache[userId]?.let {
      return it
    }

    val result = executeWithRetry {
      val doc = firestore.collection(COLLECTION_USERS).document(userId).get().await()

      val context =
          if (doc.exists()) {
            // Extract the badgeContext field explicitly instead of using toObject()
            // Previous implementation used toObject(BadgeContext::class.java) which attempted
            // to deserialize the entire document, causing field mapping issues when the document
            // structure contained additional fields beyond BadgeContext properties
            @Suppress("UNCHECKED_CAST")
            val badgeContextData = doc[FIELD_BADGE_CONTEXT] as? Map<String, Any>

            if (badgeContextData != null) {
              val existing =
                  BadgeContext(
                      friendsCount = (badgeContextData["friendsCount"] as? Number)?.toInt() ?: 0,
                      createdEvents = (badgeContextData["createdEvents"] as? Number)?.toInt() ?: 0,
                      joinedEvents = (badgeContextData["joinedEvents"] as? Number)?.toInt() ?: 0,
                      earlyJoin = (badgeContextData["earlyJoin"] as? Number)?.toInt() ?: 0,
                      lateJoin = (badgeContextData["lateJoin"] as? Number)?.toInt() ?: 0,
                      earlyCreate = (badgeContextData["earlyCreate"] as? Number)?.toInt() ?: 0,
                      lateCreate = (badgeContextData["lateCreate"] as? Number)?.toInt() ?: 0)
              badgeContextCache[userId] = existing
              existing
            } else {
              val defaultCtx = BadgeContext()
              saveBadgeContext(userId, defaultCtx)
              badgeContextCache[userId] = defaultCtx
              Log.d(TAG, "Created missing BadgeContext for legacy user $userId")
              defaultCtx
            }
          } else {
            val defaultCtx = BadgeContext()
            Log.w(TAG, "User document missing for $userId — returning default BadgeContext")
            defaultCtx
          }

      context
    }

    return result ?: BadgeContext()
  }

  override suspend fun updateBadgesAfterContextChange(userId: String) {
    // Use mutex to ensure cache invalidation and fetch are atomic
    // This prevents race conditions when multiple coroutines update badges simultaneously
    updateMutex.withLock {
      // Invalidate cache to ensure we get fresh data from Firestore
      // This is important when multiple BadgeRepository instances exist
      badgeContextCache.remove(userId)
      val context = getBadgeContext(userId)
      // Fetch the actual user profile to get all profile fields for badge calculation
      val userProfile = userProfileRepository.getUserProfile(userId) ?: UserProfile(userId = userId)
      val updatedBadges = BadgeManager.calculateBadges(userProfile, context)
      saveBadgeProgress(userId, updatedBadges)
    }
  }

  /**
   * Clears both badge and badge context caches for a specific user. This ensures fresh data is
   * fetched from Firestore on the next read.
   */
  override fun clearCache(userId: String) {
    badgeCache.remove(userId)
    badgeContextCache.remove(userId)
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

        Log.d(TAG, "Successfully saved ${badges.size} badges for user $userId")
        true
      } catch (e: Exception) {
        Log.e(TAG, "Error saving badges for user $userId", e)
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
      Log.d(TAG, "Returning cached badges for user $userId")
      return it
    }

    return executeWithRetry {
      try {
        val document = firestore.collection(COLLECTION_USERS).document(userId).get().await()

        if (document.exists()) {
          @Suppress("UNCHECKED_CAST")
          val badgesData = document[FIELD_BADGES] as? List<Map<String, Any>>

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
                  Log.e(TAG, "Error parsing badge from Firestore data", e)
                  null
                }
              } ?: emptyList()

          // Cache the result
          if (badges.isNotEmpty()) {
            badgeCache[userId] = badges
          }

          Log.d(TAG, "Retrieved ${badges.size} badges for user $userId")
          badges
        } else {
          Log.w(TAG, "No document found for user $userId")
          null
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error retrieving badges for user $userId", e)
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
        Log.e(TAG, "Error updating badge unlock status for user $userId, badge $badgeId", e)
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
      } catch (e: Exception) {
        if (attempt == MAX_RETRIES - 1) {
          // Last attempt failed, give up
          Log.w(TAG, "Operation failed after $MAX_RETRIES attempts", e)
          return null
        }
        // Wait before retrying (exponential backoff: 100ms, 200ms, 400ms)
        kotlinx.coroutines.delay(100L * (1L shl attempt))
      }
    }
    return null
  }
}

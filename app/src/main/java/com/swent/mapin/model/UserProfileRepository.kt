package com.swent.mapin.model

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user profile data in Firestore.
 *
 * Provides methods to:
 * - Fetch user profiles from Firestore
 * - Save/update user profiles to Firestore
 * - Create default profiles for new users
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

  companion object {
    private const val COLLECTION_USERS = "users"
  }

  /** Get user profile from Firestore by user ID. Returns null if profile doesn't exist. */
  suspend fun getUserProfile(userId: String): UserProfile? {
    return try {
      val document = firestore.collection(COLLECTION_USERS).document(userId).get().await()

      if (document.exists()) {
        document.toObject(UserProfile::class.java)
      } else {
        null
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Save or update user profile to Firestore. Creates document if it doesn't exist, updates if it
   * does.
   */
  suspend fun saveUserProfile(profile: UserProfile): Boolean {
    return try {
      println("UserProfileRepository - Attempting to save profile for userId: ${profile.userId}")
      println("UserProfileRepository - Profile data: $profile")

      firestore.collection(COLLECTION_USERS).document(profile.userId).set(profile).await()

      println("UserProfileRepository - Profile saved successfully")
      true
    } catch (e: Exception) {
      println("UserProfileRepository - Error saving profile: ${e.message}")
      e.printStackTrace()
      false
    }
  }

  /** Create default profile for a new user. Typically called after user signs up. */
  suspend fun createDefaultProfile(
      userId: String,
      name: String,
      profilePictureUrl: String? = null
  ): UserProfile {
    val defaultProfile =
        UserProfile(
            userId = userId,
            name = name.ifEmpty { "Anonymous User" },
            bio = "Tell us about yourself...",
            hobbies = emptyList(),
            location = "Unknown",
            profilePictureUrl = profilePictureUrl,
            avatarUrl = "person", // Default avatar icon
            bannerUrl = "purple_blue", // Default banner gradient
            hobbiesVisible = true)

    saveUserProfile(defaultProfile)
    return defaultProfile
  }

  /**
   * Follow a user. Adds targetUserId to current user's followingIds, and adds currentUserId to
   * target user's followerIds.
   *
   * @param currentUserId The ID of the user who wants to follow
   * @param targetUserId The ID of the user to be followed
   * @return true if the follow operation succeeded, false if users are the same or operation failed
   */
  suspend fun followUser(currentUserId: String, targetUserId: String): Boolean {
    return updateFollowStatus(currentUserId, targetUserId, shouldFollow = true)
  }

  /**
   * Unfollow a user. Removes targetUserId from current user's followingIds, and removes
   * currentUserId from target user's followerIds.
   *
   * @param currentUserId The ID of the user who wants to unfollow
   * @param targetUserId The ID of the user to be unfollowed
   * @return true if the unfollow operation succeeded, false if users are the same or operation
   *   failed
   */
  suspend fun unfollowUser(currentUserId: String, targetUserId: String): Boolean {
    return updateFollowStatus(currentUserId, targetUserId, shouldFollow = false)
  }

  private suspend fun updateFollowStatus(
      currentUserId: String,
      targetUserId: String,
      shouldFollow: Boolean
  ): Boolean {
    if (currentUserId == targetUserId) return false

    return try {
      firestore
          .runTransaction { transaction ->
            val currentUserRef = firestore.collection(COLLECTION_USERS).document(currentUserId)
            val targetUserRef = firestore.collection(COLLECTION_USERS).document(targetUserId)

            val currentUserDoc = transaction[currentUserRef]
            val targetUserDoc = transaction[targetUserRef]

            if (!currentUserDoc.exists() || !targetUserDoc.exists()) {
              throw Exception("User not found")
            }

            val currentFollowing =
                (currentUserDoc["followingIds"] as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList()
            val targetFollowers =
                (targetUserDoc["followerIds"] as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList()

            if (shouldFollow) {
              if (!currentFollowing.contains(targetUserId)) {
                transaction.update(currentUserRef, "followingIds", currentFollowing + targetUserId)
              }
              if (!targetFollowers.contains(currentUserId)) {
                transaction.update(targetUserRef, "followerIds", targetFollowers + currentUserId)
              }
            } else {
              transaction.update(currentUserRef, "followingIds", currentFollowing - targetUserId)
              transaction.update(targetUserRef, "followerIds", targetFollowers - currentUserId)
            }
          }
          .await()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Check if currentUser is following targetUser.
   *
   * @param currentUserId The ID of the user to check
   * @param targetUserId The ID of the user who might be followed
   * @return true if currentUser is following targetUser, false otherwise
   */
  suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
    return try {
      val profile = getUserProfile(currentUserId)
      profile?.followingIds?.contains(targetUserId) ?: false
    } catch (e: Exception) {
      false
    }
  }
}

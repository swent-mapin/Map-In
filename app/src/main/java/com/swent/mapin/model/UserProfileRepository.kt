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
}

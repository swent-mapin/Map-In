package com.swent.mapin.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Manager for FCM (Firebase Cloud Messaging) tokens and push notifications.
 *
 * This class handles:
 * - Getting and refreshing FCM tokens
 * - Saving tokens to Firestore for the current user
 * - Subscribing to notification topics
 */
class FCMTokenManager(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

  companion object {
    private const val TAG = "FCMTokenManager"
    private const val COLLECTION_USERS = "users"
    private const val FIELD_FCM_TOKEN = "fcmToken"
    private const val FIELD_FCM_TOKENS = "fcmTokens" // For multi-device support
  }

  /**
   * Get the current FCM token for this device. This token is used to send push notifications to
   * this specific device.
   */
  suspend fun getToken(): String? {
    return try {
      messaging.token.await()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get FCM token", e)
      null
    }
  }

  /**
   * Save the FCM token to Firestore for the current user. This allows sending push notifications to
   * this user's device(s).
   *
   * @param token The FCM token to save (if null, gets current token)
   * @return Boolean indicating success
   */
  suspend fun saveTokenForCurrentUser(token: String? = null): Boolean {
    val userId = auth.currentUser?.uid ?: return false
    val fcmToken = token ?: getToken() ?: return false

    return try {
      // Option 1: Single token (replaces old token)
      firestore
          .collection(COLLECTION_USERS)
          .document(userId)
          .update(FIELD_FCM_TOKEN, fcmToken)
          .await()

      println("FCMTokenManager: Token saved successfully for user $userId")
      true
    } catch (e: Exception) {
      // If update fails, try set (for new users)
      try {
        firestore
            .collection(COLLECTION_USERS)
            .document(userId)
            .set(
                mapOf(FIELD_FCM_TOKEN to fcmToken),
                com.google.firebase.firestore.SetOptions.merge())
            .await()
        true
      } catch (e2: Exception) {
        Log.e(TAG, "Failed to save FCM token for user: $userId", e2)
        false
      }
    }
  }

  /**
   * Save FCM token to support multiple devices per user. Stores tokens in an array instead of
   * replacing.
   *
   * @param token The FCM token to add
   * @return Boolean indicating success
   */
  suspend fun addTokenForCurrentUser(token: String? = null): Boolean {
    val userId = auth.currentUser?.uid ?: return false
    val fcmToken = token ?: getToken() ?: return false

    return try {
      firestore
          .collection(COLLECTION_USERS)
          .document(userId)
          .update(FIELD_FCM_TOKENS, com.google.firebase.firestore.FieldValue.arrayUnion(fcmToken))
          .await()
      true
    } catch (e: Exception) {
      // If update fails, try set with merge
      try {
        firestore
            .collection(COLLECTION_USERS)
            .document(userId)
            .set(
                mapOf(FIELD_FCM_TOKENS to listOf(fcmToken)),
                com.google.firebase.firestore.SetOptions.merge())
            .await()
        true
      } catch (e2: Exception) {
        Log.e(TAG, "Failed to add FCM token for user: $userId", e2)
        false
      }
    }
  }

  /**
   * Remove the current FCM token from Firestore. Call this when user logs out or disables
   * notifications.
   */
  suspend fun removeTokenForCurrentUser(): Boolean {
    val userId = auth.currentUser?.uid ?: return false
    val fcmToken = getToken() ?: return false

    return try {
      firestore
          .collection(COLLECTION_USERS)
          .document(userId)
          .update(FIELD_FCM_TOKENS, com.google.firebase.firestore.FieldValue.arrayRemove(fcmToken))
          .await()
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to remove FCM token for user: $userId", e)
      false
    }
  }

  /**
   * Subscribe to a topic to receive topic-based notifications. Useful for: general announcements,
   * nearby events, etc.
   *
   * @param topic The topic name (e.g., "all_users", "nearby_events")
   */
  suspend fun subscribeToTopic(topic: String): Boolean {
    return try {
      messaging.subscribeToTopic(topic).await()
      println("FCMTokenManager: Subscribed to topic: $topic")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to subscribe to topic: $topic", e)
      false
    }
  }

  /**
   * Unsubscribe from a topic.
   *
   * @param topic The topic name to unsubscribe from
   */
  suspend fun unsubscribeFromTopic(topic: String): Boolean {
    return try {
      messaging.unsubscribeFromTopic(topic).await()
      println("FCMTokenManager: Unsubscribed from topic: $topic")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
      false
    }
  }

  /**
   * Delete the FCM token completely. Call this if user wants to stop receiving push notifications
   * entirely.
   */
  suspend fun deleteToken(): Boolean {
    return try {
      messaging.deleteToken().await()
      println("FCMTokenManager: Token deleted")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete FCM token", e)
      false
    }
  }

  /** Initialize FCM for the current user. Call this after user logs in. */
  suspend fun initializeForCurrentUser(): Boolean {
    val token = getToken() ?: return false
    return saveTokenForCurrentUser(token)
  }
}

package com.swent.mapin.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing notification data in Firestore.
 *
 * Provides methods to:
 * - Send notifications to users
 * - Fetch notifications for a user
 * - Mark notifications as read
 * - Delete notifications
 * - Listen to real-time notification updates
 */
class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

  companion object {
    private const val COLLECTION_NOTIFICATIONS = "notifications"
    private const val FIELD_RECIPIENT_ID = "recipientId"
    private const val FIELD_READ_STATUS = "readStatus"
    private const val FIELD_TIMESTAMP = "timestamp"
    private const val FIELD_PRIORITY = "priority"
  }

  /**
   * Send a notification to a user. Creates a new notification document in Firestore.
   *
   * @param notification The notification to send
   * @return NotificationResult indicating success or failure
   */
  suspend fun send(notification: Notification): NotificationResult {
    return try {
      // Generate ID if not provided
      val notificationId =
          notification.notificationId.ifEmpty {
            firestore.collection(COLLECTION_NOTIFICATIONS).document().id
          }

      val notificationToSend =
          notification.copy(notificationId = notificationId, timestamp = Timestamp.now())

      firestore
          .collection(COLLECTION_NOTIFICATIONS)
          .document(notificationId)
          .set(notificationToSend)
          .await()

      NotificationResult.Success(notificationToSend)
    } catch (e: Exception) {
      e.printStackTrace()
      NotificationResult.Error("Failed to send notification: ${e.message}", e)
    }
  }

  /**
   * Get all notifications for a specific user.
   *
   * @param userId The recipient user ID
   * @param includeRead Whether to include read notifications (default: true)
   * @param limit Maximum number of notifications to fetch (default: 50)
   * @return List of notifications
   */
  suspend fun getNotificationsForUser(
      userId: String,
      includeRead: Boolean = true,
      limit: Long = 50
  ): List<Notification> {
    return try {
      var query: Query =
          firestore
              .collection(COLLECTION_NOTIFICATIONS)
              .whereEqualTo(FIELD_RECIPIENT_ID, userId)
              .orderBy(FIELD_PRIORITY, Query.Direction.DESCENDING)
              .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
              .limit(limit)

      if (!includeRead) {
        query = query.whereEqualTo(FIELD_READ_STATUS, false)
      }

      val snapshot = query.get().await()
      snapshot.documents.mapNotNull { it.toObject(Notification::class.java) }
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  /**
   * Get unread notification count for a user.
   *
   * @param userId The user ID
   * @return Number of unread notifications
   */
  suspend fun getUnreadCount(userId: String): Int {
    return try {
      val snapshot =
          firestore
              .collection(COLLECTION_NOTIFICATIONS)
              .whereEqualTo(FIELD_RECIPIENT_ID, userId)
              .whereEqualTo(FIELD_READ_STATUS, false)
              .get()
              .await()

      snapshot.size()
    } catch (e: Exception) {
      e.printStackTrace()
      0
    }
  }

  /**
   * Mark a notification as read.
   *
   * @param notificationId The notification ID
   * @return Boolean indicating success
   */
  suspend fun markAsRead(notificationId: String): Boolean {
    return try {
      firestore
          .collection(COLLECTION_NOTIFICATIONS)
          .document(notificationId)
          .update(FIELD_READ_STATUS, true)
          .await()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Mark multiple notifications as read.
   *
   * @param notificationIds List of notification IDs
   * @return Boolean indicating success
   */
  suspend fun markMultipleAsRead(notificationIds: List<String>): Boolean {
    return try {
      val batch = firestore.batch()

      notificationIds.forEach { id ->
        val docRef = firestore.collection(COLLECTION_NOTIFICATIONS).document(id)
        batch.update(docRef, FIELD_READ_STATUS, true)
      }

      batch.commit().await()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Mark all notifications as read for a user.
   *
   * @param userId The user ID
   * @return Boolean indicating success
   */
  suspend fun markAllAsRead(userId: String): Boolean {
    return try {
      val unreadNotifications =
          firestore
              .collection(COLLECTION_NOTIFICATIONS)
              .whereEqualTo(FIELD_RECIPIENT_ID, userId)
              .whereEqualTo(FIELD_READ_STATUS, false)
              .get()
              .await()

      val batch = firestore.batch()
      unreadNotifications.documents.forEach { doc ->
        batch.update(doc.reference, FIELD_READ_STATUS, true)
      }

      batch.commit().await()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Delete a notification.
   *
   * @param notificationId The notification ID
   * @return Boolean indicating success
   */
  suspend fun deleteNotification(notificationId: String): Boolean {
    return try {
      firestore.collection(COLLECTION_NOTIFICATIONS).document(notificationId).delete().await()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Delete all notifications for a user.
   *
   * @param userId The user ID
   * @return Boolean indicating success
   */
  suspend fun deleteAllNotifications(userId: String): Boolean {
    return try {
      val notifications =
          firestore
              .collection(COLLECTION_NOTIFICATIONS)
              .whereEqualTo(FIELD_RECIPIENT_ID, userId)
              .get()
              .await()

      val batch = firestore.batch()
      notifications.documents.forEach { doc -> batch.delete(doc.reference) }

      batch.commit().await()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Get a single notification by ID.
   *
   * @param notificationId The notification ID
   * @return The notification, or null if not found
   */
  suspend fun getNotification(notificationId: String): Notification? {
    return try {
      val document =
          firestore.collection(COLLECTION_NOTIFICATIONS).document(notificationId).get().await()

      document.toObject(Notification::class.java)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Listen to real-time notifications for a user. Returns a Flow that emits the list of
   * notifications whenever it changes.
   *
   * @param userId The user ID
   * @param includeRead Whether to include read notifications
   * @return Flow of notification lists
   */
  fun observeNotifications(userId: String, includeRead: Boolean = true): Flow<List<Notification>> =
      callbackFlow {
        var query: Query =
            firestore
                .collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_RECIPIENT_ID, userId)
                .orderBy(FIELD_PRIORITY, Query.Direction.DESCENDING)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)

        if (!includeRead) {
          query = query.whereEqualTo(FIELD_READ_STATUS, false)
        }

        val listenerRegistration =
            query.addSnapshotListener { snapshot, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }

              if (snapshot != null) {
                val notifications =
                    snapshot.documents.mapNotNull { it.toObject(Notification::class.java) }
                trySend(notifications)
              }
            }

        awaitClose { listenerRegistration.remove() }
      }

  /**
   * Get notifications by type for a user.
   *
   * @param userId The user ID
   * @param type The notification type to filter by
   * @param limit Maximum number of notifications to fetch
   * @return List of notifications of the specified type
   */
  suspend fun getNotificationsByType(
      userId: String,
      type: NotificationType,
      limit: Long = 50
  ): List<Notification> {
    return try {
      val snapshot =
          firestore
              .collection(COLLECTION_NOTIFICATIONS)
              .whereEqualTo(FIELD_RECIPIENT_ID, userId)
              .whereEqualTo("type", type.name)
              .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
              .limit(limit)
              .get()
              .await()

      snapshot.documents.mapNotNull { it.toObject(Notification::class.java) }
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }
}

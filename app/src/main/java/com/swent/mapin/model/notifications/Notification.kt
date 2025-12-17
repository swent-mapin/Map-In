package com.swent.mapin.model.notifications

import com.google.firebase.Timestamp

/**
 * Enum representing the type of notification. Used to categorize notifications for filtering and
 * display purposes.
 */
enum class NotificationType {
  /** General information notification */
  INFO,
  /** Alert/warning notification requiring attention */
  ALERT,
  /** Reminder notification for events or actions */
  REMINDER,
  /** Message notification from chat or direct messages */
  MESSAGE,
  /** Friend request notification */
  FRIEND_REQUEST,
  /** Event invitation notification */
  EVENT_INVITATION,
  /** System notification */
  SYSTEM
}

/**
 * Data class representing a notification in the app.
 *
 * This is a reusable notification object that can be triggered by various parts of the app. It
 * handles the delivery and formatting of notifications without being tied to any specific type of
 * notification (messages, events, etc.).
 *
 * @property notificationId Unique identifier for the notification
 * @property title Title/headline of the notification
 * @property message Main content/body of the notification
 * @property type Type of notification (info, alert, reminder, etc.)
 * @property recipientId User ID of the recipient
 * @property senderId User ID of the sender (optional, for user-generated notifications)
 * @property readStatus Whether the notification has been read
 * @property timestamp When the notification was created
 * @property metadata Optional additional data as key-value pairs for extensibility
 * @property actionUrl Optional deep link or action URL for navigation
 * @property priority Priority level for notification sorting (higher = more important)
 */
data class Notification(
    val notificationId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val recipientId: String = "",
    val senderId: String? = null,
    val readStatus: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
    val metadata: Map<String, String> = emptyMap(),
    val actionUrl: String? = null,
    val priority: Int = 0
) {

  /** Returns a copy of this notification with readStatus set to true. */
  fun markAsRead(): Notification {
    return this.copy(readStatus = true)
  }

  /** Returns the type of this notification. */
  fun getNotificationType(): NotificationType {
    return type
  }

  /** Checks if this notification is unread. */
  fun isUnread(): Boolean {
    return !readStatus
  }

  /** Gets a metadata value by key, or null if not found. */
  fun getMetadata(key: String): String? {
    return metadata[key]
  }

  /** Returns true if this notification has an action URL. */
  fun hasAction(): Boolean {
    return !actionUrl.isNullOrEmpty()
  }
}

/**
 * Result class for notification operations. Provides a consistent way to handle success/failure of
 * notification operations.
 */
sealed class NotificationResult {
  data class Success(val notification: Notification) : NotificationResult()

  data class Error(val message: String, val exception: Exception? = null) : NotificationResult()
}

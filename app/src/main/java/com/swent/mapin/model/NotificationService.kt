package com.swent.mapin.model

import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service for sending push notifications.
 *
 * This service provides convenient methods for creating and sending push notifications for various
 * scenarios in the app (friend requests, messages, events, etc.).
 *
 * Note: Notifications are saved to Firestore. To send actual push notifications to devices, you
 * need to set up a Firebase Cloud Function that triggers when notifications are created. See
 * PUSH_NOTIFICATIONS_SETUP.md for details.
 */
class NotificationService(
    private val repository: NotificationRepository = NotificationRepository()
) {

  /**
   * Send a friend request notification.
   *
   * @param recipientId The user receiving the friend request
   * @param senderId The user sending the friend request
   * @param senderName Name of the sender
   * @param requestId ID of the friend request for reference
   */
  fun sendFriendRequestNotification(
      recipientId: String,
      senderId: String,
      senderName: String,
      requestId: String
  ) {
    val notification =
        Notification(
            title = "New Friend Request",
            message = "$senderName sent you a friend request",
            type = NotificationType.FRIEND_REQUEST,
            recipientId = recipientId,
            senderId = senderId,
            readStatus = false,
            metadata = mapOf("requestId" to requestId, "senderName" to senderName),
            actionUrl = "mapin://friendRequests/$requestId",
            priority = 1)

    sendNotificationAsync(notification)
  }

  /**
   * Send an event invitation notification.
   *
   * @param recipientId The user being invited
   * @param senderId The user sending the invitation
   * @param senderName Name of the sender
   * @param eventId ID of the event
   * @param eventName Name of the event
   */
  fun sendEventInvitationNotification(
      recipientId: String,
      senderId: String,
      senderName: String,
      eventId: String,
      eventName: String
  ) {
    val notification =
        Notification(
            title = "Event Invitation",
            message = "$senderName invited you to $eventName",
            type = NotificationType.EVENT_INVITATION,
            recipientId = recipientId,
            senderId = senderId,
            readStatus = false,
            metadata =
                mapOf("eventId" to eventId, "eventName" to eventName, "senderName" to senderName),
            actionUrl = "mapin://events/$eventId",
            priority = 2)

    sendNotificationAsync(notification)
  }

  /**
   * Send a message notification.
   *
   * @param recipientId The user receiving the message
   * @param senderId The user sending the message
   * @param senderName Name of the sender
   * @param messagePreview Preview of the message content
   * @param conversationId ID of the conversation
   */
  fun sendMessageNotification(
      recipientId: String,
      senderId: String,
      senderName: String,
      messagePreview: String,
      conversationId: String
  ) {
    val notification =
        Notification(
            title = "New Message from $senderName",
            message = messagePreview,
            type = NotificationType.MESSAGE,
            recipientId = recipientId,
            senderId = senderId,
            readStatus = false,
            metadata = mapOf("conversationId" to conversationId, "senderName" to senderName),
            actionUrl = "mapin://messages/$conversationId",
            priority = 2)

    sendNotificationAsync(notification)
  }

  /**
   * Send an event reminder notification.
   *
   * @param recipientId The user to remind
   * @param eventId ID of the event
   * @param eventName Name of the event
   * @param eventTime Time of the event
   * @param minutesBefore How many minutes before the event this reminder is for
   */
  fun sendEventReminderNotification(
      recipientId: String,
      eventId: String,
      eventName: String,
      eventTime: Timestamp,
      minutesBefore: Int
  ) {
    val timeText =
        when (minutesBefore) {
          0 -> "now"
          1 -> "in 1 minute"
          in 2..59 -> "in $minutesBefore minutes"
          60 -> "in 1 hour"
          else -> "in ${minutesBefore / 60} hours"
        }

    val notification =
        Notification(
            title = "Event Reminder",
            message = "$eventName starts $timeText",
            type = NotificationType.REMINDER,
            recipientId = recipientId,
            readStatus = false,
            metadata =
                mapOf(
                    "eventId" to eventId,
                    "eventName" to eventName,
                    "eventTime" to eventTime.seconds.toString(),
                    "minutesBefore" to minutesBefore.toString()),
            actionUrl = "mapin://events/$eventId",
            priority = 3)

    sendNotificationAsync(notification)
  }

  /**
   * Send a system alert notification.
   *
   * @param recipientId The user to notify
   * @param title Title of the alert
   * @param message Message content
   * @param priority Priority level (default: 1)
   */
  fun sendSystemAlertNotification(
      recipientId: String,
      title: String,
      message: String,
      priority: Int = 1
  ) {
    val notification =
        Notification(
            title = title,
            message = message,
            type = NotificationType.ALERT,
            recipientId = recipientId,
            readStatus = false,
            priority = priority)

    sendNotificationAsync(notification)
  }

  /**
   * Send an info notification.
   *
   * @param recipientId The user to notify
   * @param title Title of the notification
   * @param message Message content
   * @param metadata Optional metadata
   * @param actionUrl Optional action URL
   */
  fun sendInfoNotification(
      recipientId: String,
      title: String,
      message: String,
      metadata: Map<String, String> = emptyMap(),
      actionUrl: String? = null
  ) {
    val notification =
        Notification(
            title = title,
            message = message,
            type = NotificationType.INFO,
            recipientId = recipientId,
            readStatus = false,
            metadata = metadata,
            actionUrl = actionUrl,
            priority = 0)

    sendNotificationAsync(notification)
  }

  /**
   * Send a custom notification.
   *
   * @param notification The notification to send
   * @param onComplete Callback with the result
   */
  suspend fun sendNotification(notification: Notification): NotificationResult {
    return repository.send(notification)
  }

  /**
   * Send a notification asynchronously (fire and forget). Used internally for convenience methods.
   *
   * @param notification The notification to send
   */
  private fun sendNotificationAsync(notification: Notification) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        repository.send(notification)
      } catch (e: Exception) {
        // Log error but don't throw
        println("NotificationService: Failed to send notification - ${e.message}")
      }
    }
  }

  /**
   * Send notifications to multiple recipients.
   *
   * @param recipientIds List of recipient user IDs
   * @param title Notification title
   * @param message Notification message
   * @param type Notification type
   * @param senderId Optional sender ID
   * @param metadata Optional metadata
   * @param actionUrl Optional action URL
   */
  fun sendBulkNotifications(
      recipientIds: List<String>,
      title: String,
      message: String,
      type: NotificationType = NotificationType.INFO,
      senderId: String? = null,
      metadata: Map<String, String> = emptyMap(),
      actionUrl: String? = null
  ) {
    CoroutineScope(Dispatchers.IO).launch {
      recipientIds.forEach { recipientId ->
        val notification =
            Notification(
                title = title,
                message = message,
                type = type,
                recipientId = recipientId,
                senderId = senderId,
                metadata = metadata,
                actionUrl = actionUrl)

        try {
          repository.send(notification)
        } catch (e: Exception) {
          println(
              "NotificationService: Failed to send bulk notification to $recipientId - ${e.message}")
        }
      }
    }
  }

  /**
   * Create a notification builder for more complex scenarios.
   *
   * @return NotificationBuilder instance
   */
  fun createNotification(): NotificationBuilder {
    return NotificationBuilder(this)
  }
}

/** Builder class for creating notifications with a fluent API. */
class NotificationBuilder(private val service: NotificationService) {
  private var title: String = ""
  private var message: String = ""
  private var type: NotificationType = NotificationType.INFO
  private var recipientId: String = ""
  private var senderId: String? = null
  private var metadata: MutableMap<String, String> = mutableMapOf()
  private var actionUrl: String? = null
  private var priority: Int = 0

  fun title(title: String) = apply { this.title = title }

  fun message(message: String) = apply { this.message = message }

  fun type(type: NotificationType) = apply { this.type = type }

  fun recipientId(recipientId: String) = apply { this.recipientId = recipientId }

  fun senderId(senderId: String) = apply { this.senderId = senderId }

  fun addMetadata(key: String, value: String) = apply { this.metadata[key] = value }

  fun metadata(metadata: Map<String, String>) = apply { this.metadata.putAll(metadata) }

  fun actionUrl(actionUrl: String) = apply { this.actionUrl = actionUrl }

  fun priority(priority: Int) = apply { this.priority = priority }

  /**
   * Build and send the notification.
   *
   * @return NotificationResult indicating success or failure
   */
  suspend fun send(): NotificationResult {
    val notification =
        Notification(
            title = title,
            message = message,
            type = type,
            recipientId = recipientId,
            senderId = senderId,
            metadata = metadata,
            actionUrl = actionUrl,
            priority = priority)
    return service.sendNotification(notification)
  }

  /**
   * Build the notification without sending it.
   *
   * @return The built Notification object
   */
  fun build(): Notification {
    return Notification(
        title = title,
        message = message,
        type = type,
        recipientId = recipientId,
        senderId = senderId,
        metadata = metadata,
        actionUrl = actionUrl,
        priority = priority)
  }
}

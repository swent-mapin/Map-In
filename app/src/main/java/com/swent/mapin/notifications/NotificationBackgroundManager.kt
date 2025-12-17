package com.swent.mapin.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.swent.mapin.MainActivity
import com.swent.mapin.R
import com.swent.mapin.model.notifications.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Background service for managing Firebase Cloud Messaging push notifications.
 *
 * This service runs in the background and displays system notifications when messages are received
 * from Firebase Cloud Messaging, even when the app is closed.
 */
open class NotificationBackgroundManager : FirebaseMessagingService() {

  companion object {
    private const val TAG = "FCMService"
    private const val DEFAULT_CHANNEL_ID = "mapin_notifications"
    private const val DEFAULT_CHANNEL_NAME = "Map-In Notifications"
    private const val FRIEND_CHANNEL_ID = "mapin_friend_notifications"
    private const val FRIEND_CHANNEL_NAME = "Friend Requests"
    private const val EVENT_CHANNEL_ID = "mapin_event_notifications"
    private const val EVENT_CHANNEL_NAME = "Events"
    private const val MESSAGE_CHANNEL_ID = "mapin_message_notifications"
    private const val MESSAGE_CHANNEL_NAME = "Messages"
    private const val ALERT_CHANNEL_ID = "mapin_alert_notifications"
    private const val ALERT_CHANNEL_NAME = "Alerts"
  }

  // FCM Token Manager for saving tokens to Firestore
  private val tokenManager by lazy { FCMTokenManager() }

  /**
   * Initialize notification channels when the service is created. This ensures channels are created
   * once instead of on every notification.
   */
  override fun onCreate() {
    super.onCreate()
    // Create notification channels for Android O and above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      createNotificationChannels(notificationManager)
    }
  }

  /**
   * Called when a new FCM token is generated. This token is used to send push notifications to this
   * specific device.
   */
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.i(TAG, "New FCM token generated")

    // Save token to Firestore for the current user
    saveTokenToFirestore(token)
  }

  /**
   * Called when a message is received from Firebase Cloud Messaging. This is where we create and
   * display the system notification.
   */
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)

    Log.i(TAG, "Message received from: ${remoteMessage.from}")

    // Check if message contains a notification payload
    remoteMessage.notification?.let { notification ->
      val title = notification.title ?: "Map-In"
      val body = notification.body ?: ""

      // Get notification type from data payload
      val type =
          remoteMessage.data["type"]?.let {
            try {
              NotificationType.valueOf(it)
            } catch (_: Exception) {
              NotificationType.INFO
            }
          } ?: NotificationType.INFO

      val actionUrl = remoteMessage.data["actionUrl"]
      val notificationId = remoteMessage.data["notificationId"]

      sendNotification(title, body, type, actionUrl, notificationId)
      return // Don't process data payload if we have notification payload
    }

    // Check if message contains a data payload (only if no notification payload)
    if (remoteMessage.data.isNotEmpty()) {
      Log.i(TAG, "Message data payload: ${remoteMessage.data}")

      // Handle data payload (for background notifications)
      val title = remoteMessage.data["title"] ?: "Map-In"
      val message = remoteMessage.data["message"] ?: ""
      val type =
          remoteMessage.data["type"]?.let {
            try {
              NotificationType.valueOf(it)
            } catch (_: Exception) {
              NotificationType.INFO
            }
          } ?: NotificationType.INFO

      val actionUrl = remoteMessage.data["actionUrl"]
      val notificationId = remoteMessage.data["notificationId"]

      sendNotification(title, message, type, actionUrl, notificationId)
    }
  }

  /** Create and display a system notification. Protected to allow overriding in tests. */
  protected open fun sendNotification(
      title: String,
      messageBody: String,
      type: NotificationType,
      actionUrl: String?,
      notificationId: String?
  ) {
    // Create intent for when user taps notification
    val intent =
        Intent(this, MainActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          if (actionUrl != null) {
            putExtra("action_url", actionUrl)
          }
          if (notificationId != null) {
            putExtra("notification_id", notificationId)
          }
        }

    // Use unique requestCode to prevent intents from overwriting each other
    val requestCode = notificationId?.hashCode() ?: System.currentTimeMillis().toInt()

    val pendingIntent =
        PendingIntent.getActivity(
            this, requestCode, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

    // Select appropriate channel based on notification type
    val channelId = getChannelIdForType(type)

    // Get notification sound
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    // Build the notification
    val notificationBuilder =
        NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo) // Use your app icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

    // Get notification manager
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    // Show notification with unique ID
    // Use stable hash of notificationId if provided, otherwise use timestamp
    val id =
        notificationId?.let { generateStableNotificationId(it) }
            ?: (System.currentTimeMillis() and 0xfffffff).toInt()
    notificationManager.notify(id, notificationBuilder.build())
  }

  /**
   * Generate a stable notification ID from a string. Uses a simple but stable hash that won't
   * change across app restarts.
   */
  private fun generateStableNotificationId(notificationId: String): Int {
    // Use a stable hash algorithm instead of hashCode()
    var hash = 0
    for (char in notificationId) {
      hash = 31 * hash + char.code
    }
    // Ensure positive integer and within valid range
    return hash and 0x7FFFFFFF
  }

  /** Get the appropriate notification channel ID based on notification type. */
  private fun getChannelIdForType(type: NotificationType): String {
    return when (type) {
      NotificationType.FRIEND_REQUEST -> FRIEND_CHANNEL_ID
      NotificationType.EVENT_INVITATION,
      NotificationType.REMINDER -> EVENT_CHANNEL_ID
      NotificationType.MESSAGE -> MESSAGE_CHANNEL_ID
      NotificationType.ALERT,
      NotificationType.SYSTEM -> ALERT_CHANNEL_ID
      NotificationType.INFO -> DEFAULT_CHANNEL_ID
    }
  }

  /**
   * Create notification channels for Android O and above. Channels allow users to control
   * notification settings per category.
   */
  private fun createNotificationChannels(notificationManager: NotificationManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Default channel
      val defaultChannel =
          NotificationChannel(
                  DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
              .apply { description = "General notifications from Map-In" }

      // Friend requests channel
      val friendChannel =
          NotificationChannel(
                  FRIEND_CHANNEL_ID, FRIEND_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
              .apply { description = "Friend requests and friend activity" }

      // Events channel
      val eventChannel =
          NotificationChannel(
                  EVENT_CHANNEL_ID, EVENT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
              .apply { description = "Event invitations and reminders" }

      // Messages channel
      val messageChannel =
          NotificationChannel(
                  MESSAGE_CHANNEL_ID, MESSAGE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
              .apply { description = "Chat messages" }

      // Alerts channel
      val alertChannel =
          NotificationChannel(
                  ALERT_CHANNEL_ID, ALERT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
              .apply { description = "Important alerts and system notifications" }

      // Register all channels
      notificationManager.createNotificationChannel(defaultChannel)
      notificationManager.createNotificationChannel(friendChannel)
      notificationManager.createNotificationChannel(eventChannel)
      notificationManager.createNotificationChannel(messageChannel)
      notificationManager.createNotificationChannel(alertChannel)
    }
  }

  /**
   * Save FCM token to Firestore for the current user. This allows sending notifications to this
   * specific device. Uses multi-device approach (array) to support multiple devices per user.
   */
  private fun saveTokenToFirestore(token: String) {
    // Use coroutine to save token asynchronously
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val success = tokenManager.addTokenForCurrentUser(token)
        if (success) {
          Log.i(TAG, "Token saved to Firestore successfully")
        } else {
          Log.w(TAG, "Failed to save token to Firestore (user may not be logged in)")
        }
      } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Invalid token format: ${e.message}", e)
      } catch (e: SecurityException) {
        Log.e(TAG, "Security error saving token to Firestore: ${e.message}", e)
      } catch (e: Exception) {
        Log.e(TAG, "Unexpected error saving token to Firestore: ${e.message}", e)
      }
    }
  }
}

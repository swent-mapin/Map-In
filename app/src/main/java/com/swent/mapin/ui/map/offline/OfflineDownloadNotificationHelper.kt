package com.swent.mapin.ui.map.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.swent.mapin.MainActivity
import com.swent.mapin.R
import com.swent.mapin.model.event.Event
import com.swent.mapin.notifications.NotificationBackgroundManager

/**
 * Helper class for managing offline map download notifications.
 *
 * Shows progress notifications for each event being downloaded and completion notifications when
 * done. Notifications are tappable and navigate to the event detail when clicked.
 */
class OfflineDownloadNotificationHelper(private val context: Context) {

  companion object {
    private const val NOTIFICATION_ID_BASE = 10000
  }

  private val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    // Create notification channel for Android O and above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
          NotificationChannel(
                  NotificationBackgroundManager.OFFLINE_DOWNLOAD_CHANNEL_ID,
                  NotificationBackgroundManager.OFFLINE_DOWNLOAD_CHANNEL_NAME,
                  NotificationManager.IMPORTANCE_LOW)
              .apply { description = "Offline map download progress and completion" }
      notificationManager.createNotificationChannel(channel)
    }
  }

  /**
   * Show a progress notification for an event download.
   *
   * @param event The event being downloaded
   * @param progress Download progress (0.0 to 1.0)
   */
  fun showProgress(event: Event, progress: Float) {
    val notificationId = getNotificationId(event.uid)
    val progressPercent = (progress * 100).toInt()

    val intent = createEventIntent(event.uid)
    val pendingIntent =
        PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notification =
        NotificationCompat.Builder(
                context, NotificationBackgroundManager.OFFLINE_DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Downloading offline map")
            .setContentText(event.title)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()

    notificationManager.notify(notificationId, notification)
  }

  /**
   * Show a completion notification for an event download.
   *
   * @param event The event that finished downloading
   */
  fun showComplete(event: Event) {
    val notificationId = getNotificationId(event.uid)

    val intent = createEventIntent(event.uid)
    val pendingIntent =
        PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notification =
        NotificationCompat.Builder(
                context, NotificationBackgroundManager.OFFLINE_DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Offline map ready")
            .setContentText(event.title)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

    notificationManager.notify(notificationId, notification)
  }

  /**
   * Cancel a notification for an event.
   *
   * @param eventId The event ID whose notification to cancel
   */
  fun cancel(eventId: String) {
    val notificationId = getNotificationId(eventId)
    notificationManager.cancel(notificationId)
  }

  /**
   * Generate a stable notification ID for an event.
   *
   * @param eventId The event ID
   * @return A unique notification ID
   */
  private fun getNotificationId(eventId: String): Int {
    return NOTIFICATION_ID_BASE + (eventId.hashCode() and 0xFFFF)
  }

  /**
   * Create an intent to navigate to the event detail.
   *
   * @param eventId The event ID to navigate to
   * @return Intent configured to open the event
   */
  private fun createEventIntent(eventId: String): Intent {
    return Intent(context, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      putExtra("event_id", eventId)
      putExtra("open_event_detail", true)
    }
  }
}

package com.swent.mapin.ui.map.offline

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.swent.mapin.MainActivity
import com.swent.mapin.R
import com.swent.mapin.model.event.Event

/**
 * Manages Android system notifications for offline map downloads.
 *
 * This manager creates and updates notifications for each event's map download, showing progress
 * and completion status. Clicking a completion notification navigates to the event details.
 *
 * Uses the existing EVENT_CHANNEL from NotificationBackgroundManager for consistency.
 */
class DownloadNotificationManager(private val context: Context) {

  companion object {
    // Reuse existing event notification channel from NotificationBackgroundManager
    private const val CHANNEL_ID = "mapin_event_notifications"

    // Notification IDs are based on event UID hash to ensure uniqueness per event
    private fun getNotificationId(eventId: String): Int {
      return eventId.hashCode()
    }
  }

  private val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  /**
   * Shows or updates a download progress notification for an event.
   *
   * @param event The event being downloaded
   * @param progress Download progress from 0.0 to 1.0
   */
  fun showDownloadProgress(event: Event, progress: Float) {
    val progressPercent = (progress * 100).toInt()

    val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading map for ${event.title}")
            .setContentText("$progressPercent% complete")
            .setProgress(100, progressPercent, false)
            .setOngoing(true) // Cannot be dismissed while downloading
            .setSilent(true) // No sound/vibration for background downloads
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    notificationManager.notify(getNotificationId(event.uid), notification)
  }

  /**
   * Shows a completion notification for a successfully downloaded event map.
   *
   * Clicking this notification will navigate to the event details on the map.
   *
   * @param event The event whose download completed
   */
  fun showDownloadComplete(event: Event) {
    // Clear any ongoing/progress notification so the status bar icon stops animating
    cancelNotification(event.uid)

    val deepLinkIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
          putExtra("action_url", "mapin://events/${event.uid}")
        }

    val pendingIntent =
        PendingIntent.getActivity(
            context,
            getNotificationId(event.uid),
            deepLinkIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

    val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Map ready for ${event.title}")
            .setContentText("Tap to view event details")
            .setProgress(0, 0, false) // Ensure progress bar is cleared
            .setOngoing(false)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss when tapped
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    notificationManager.notify(getNotificationId(event.uid), notification)
  }

  /**
   * Shows a failure notification if a download encounters an error.
   *
   * @param event The event whose download failed
   */
  fun showDownloadFailed(event: Event) {
    // Clear any ongoing/progress notification
    cancelNotification(event.uid)

    val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed for ${event.title}")
            .setContentText("Unable to download offline map")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setSilent(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    notificationManager.notify(getNotificationId(event.uid), notification)
  }

  /**
   * Cancels/dismisses the notification for a specific event.
   *
   * @param eventId The UID of the event whose notification should be dismissed
   */
  fun cancelNotification(eventId: String) {
    notificationManager.cancel(getNotificationId(eventId))
  }
}

package com.swent.mapin.ui.map.offline

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for DownloadNotificationManager.
 *
 * Uses Robolectric to test Android notification functionality without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
class DownloadNotificationManagerTest {

  private lateinit var context: Context
  private lateinit var notificationManager: DownloadNotificationManager
  private lateinit var systemNotificationManager: NotificationManager

  private val testEvent =
      Event(
          uid = "test-event-123",
          title = "Test Event",
          url = null,
          description = "Test Description",
          date = Timestamp.now(),
          endDate = null,
          location = Location("Test Location", 46.5197, 6.6323),
          tags = listOf("test"),
          public = true,
          ownerId = "owner123",
          imageUrl = null,
          capacity = 100,
          participantIds = emptyList(),
          price = 0.0)

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    notificationManager = DownloadNotificationManager(context)
    systemNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  @Test
  fun `showDownloadProgress displays notification with correct progress`() {
    // Show progress at 50%
    notificationManager.showDownloadProgress(testEvent, 0.5f)

    // Verify notification was posted
    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val notifications = shadowNotificationManager.allNotifications
    // One child + one summary
    assert(notifications.size == 2)

    val notification = notifications.first { !NotificationCompat.isGroupSummary(it) }
    // Verify it's an ongoing notification (cannot be dismissed)
    assert((notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0)
  }

  @Test
  fun `showDownloadProgress updates existing notification`() {
    // Show initial progress
    notificationManager.showDownloadProgress(testEvent, 0.25f)

    // Update progress
    notificationManager.showDownloadProgress(testEvent, 0.75f)

    // Should still have only one child notification (updated, not added) plus summary
    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val childCount =
        shadowNotificationManager.allNotifications.count { !NotificationCompat.isGroupSummary(it) }
    assert(childCount == 1)
  }

  @Test
  fun `showDownloadComplete displays completion notification with deep link`() {
    notificationManager.showDownloadComplete(testEvent)

    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val notifications = shadowNotificationManager.allNotifications
    // summary + child
    assert(notifications.size == 2)

    val notification = notifications.first { !NotificationCompat.isGroupSummary(it) }
    // Verify it auto-cancels when tapped
    assert((notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0)
    // Verify it has a content intent (for deep link navigation)
    assert(notification.contentIntent != null)

    // Verify the intent contains the deep link action_url
    val shadowPendingIntent = shadowOf(notification.contentIntent)
    val savedIntent = shadowPendingIntent.savedIntent
    val actionUrl = savedIntent.getStringExtra("action_url")
    assert(actionUrl == "mapin://events/${testEvent.uid}")
  }

  @Test
  fun `showDownloadFailed displays failure notification`() {
    notificationManager.showDownloadFailed(testEvent)

    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val notifications = shadowNotificationManager.allNotifications
    assert(notifications.size == 2) // child + summary

    val notification = notifications.first { !NotificationCompat.isGroupSummary(it) }
    // Verify it auto-cancels
    assert((notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0)
  }

  @Test
  fun `cancelNotification removes notification`() {
    // Show a notification
    notificationManager.showDownloadProgress(testEvent, 0.5f)

    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val initialChildCount =
        shadowNotificationManager.allNotifications.count { !NotificationCompat.isGroupSummary(it) }
    assert(initialChildCount == 1)

    // Cancel it
    notificationManager.cancelNotification(testEvent.uid)

    // Verify notification was cancelled (child removed, summary cleared)
    val finalChildCount =
        shadowNotificationManager.allNotifications.count { !NotificationCompat.isGroupSummary(it) }
    assert(finalChildCount == 0)
  }

  @Test
  fun `multiple events have separate notifications`() {
    val event1 = testEvent.copy(uid = "event-1", title = "Event 1")
    val event2 = testEvent.copy(uid = "event-2", title = "Event 2")

    notificationManager.showDownloadProgress(event1, 0.3f)
    notificationManager.showDownloadProgress(event2, 0.7f)

    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val notifications = shadowNotificationManager.allNotifications
    // Should have two children plus summary
    val childCount = notifications.count { !NotificationCompat.isGroupSummary(it) }
    assert(childCount == 2)
  }

  @Test
  fun `completion notification replaces progress notification for same event`() {
    // Show progress notification
    notificationManager.showDownloadProgress(testEvent, 0.5f)
    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val initialChildCount =
        shadowNotificationManager.allNotifications.count { !NotificationCompat.isGroupSummary(it) }
    assert(initialChildCount == 1)

    // Show completion notification for same event
    notificationManager.showDownloadComplete(testEvent)

    // Should still have only 1 child notification (completion replaces progress)
    val finalChildCount =
        shadowNotificationManager.allNotifications.count { !NotificationCompat.isGroupSummary(it) }
    assert(finalChildCount == 1)

    val notification =
        shadowNotificationManager.allNotifications.first { !NotificationCompat.isGroupSummary(it) }
    // Verify it's the completion notification (auto-cancel flag)
    assert((notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0)
  }

  @Test
  fun `download notifications are silent`() {
    notificationManager.showDownloadProgress(testEvent, 0.4f)
    notificationManager.showDownloadComplete(testEvent)

    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val notifications = shadowNotificationManager.allNotifications
    assert(notifications.isNotEmpty())

    notifications.forEach { notification ->
      assert(notification.sound == null)
      assert(notification.vibrate == null)
      assert(notification.defaults == 0)
    }
  }

  @Test
  fun `notifications are grouped with summary`() {
    val event1 = testEvent.copy(uid = "event-1", title = "Event 1")
    val event2 = testEvent.copy(uid = "event-2", title = "Event 2")

    notificationManager.showDownloadComplete(event1)
    notificationManager.showDownloadComplete(event2)

    val shadowNotificationManager = shadowOf(systemNotificationManager)
    val notifications = shadowNotificationManager.allNotifications

    // There should be a group summary plus the two individual notifications
    val groupSummaries =
        notifications.filter { NotificationCompat.isGroupSummary(it) && it.group != null }
    assert(groupSummaries.isNotEmpty())
    // Children should share the same group key as the summary
    val groupKey = groupSummaries.first().group
    val childGroups =
        notifications.filterNot { NotificationCompat.isGroupSummary(it) }.map { it.group }.toSet()
    assert(childGroups.size == 1 && childGroups.first() == groupKey)
  }
}

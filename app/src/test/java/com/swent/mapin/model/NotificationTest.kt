package com.swent.mapin.model

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for the Notification data class. */
class NotificationTest {

  @Test
  fun `notification is created with default values`() {
    val notification = Notification()

    assertEquals("", notification.notificationId)
    assertEquals("", notification.title)
    assertEquals("", notification.message)
    assertEquals(NotificationType.INFO, notification.type)
    assertEquals("", notification.recipientId)
    assertNull(notification.senderId)
    assertFalse(notification.readStatus)
    assertTrue(notification.metadata.isEmpty())
    assertNull(notification.actionUrl)
    assertEquals(0, notification.priority)
  }

  @Test
  fun `notification is created with custom values`() {
    val metadata = mapOf("key1" to "value1", "key2" to "value2")
    val timestamp = Timestamp.now()

    val notification =
        Notification(
            notificationId = "notif123",
            title = "Test Title",
            message = "Test Message",
            type = NotificationType.ALERT,
            recipientId = "user123",
            senderId = "user456",
            readStatus = true,
            timestamp = timestamp,
            metadata = metadata,
            actionUrl = "mapin://test",
            priority = 2)

    assertEquals("notif123", notification.notificationId)
    assertEquals("Test Title", notification.title)
    assertEquals("Test Message", notification.message)
    assertEquals(NotificationType.ALERT, notification.type)
    assertEquals("user123", notification.recipientId)
    assertEquals("user456", notification.senderId)
    assertTrue(notification.readStatus)
    assertEquals(timestamp, notification.timestamp)
    assertEquals(metadata, notification.metadata)
    assertEquals("mapin://test", notification.actionUrl)
    assertEquals(2, notification.priority)
  }

  @Test
  fun `markAsRead returns notification with readStatus true`() {
    val notification = Notification(notificationId = "notif123", title = "Test", readStatus = false)

    assertFalse(notification.readStatus)

    val markedNotification = notification.markAsRead()

    assertTrue(markedNotification.readStatus)
    assertFalse(notification.readStatus) // Original should be unchanged
    assertEquals(notification.notificationId, markedNotification.notificationId)
    assertEquals(notification.title, markedNotification.title)
  }

  @Test
  fun `getNotificationType returns correct type`() {
    val infoNotification = Notification(type = NotificationType.INFO)
    assertEquals(NotificationType.INFO, infoNotification.getNotificationType())

    // Multiple types tested in one test to avoid redundancy
    val alertNotification = Notification(type = NotificationType.ALERT)
    assertEquals(NotificationType.ALERT, alertNotification.getNotificationType())
  }

  @Test
  fun `isUnread returns correct value`() {
    val unreadNotification = Notification(readStatus = false)
    assertTrue(unreadNotification.isUnread())

    val readNotification = Notification(readStatus = true)
    assertFalse(readNotification.isUnread())
  }

  @Test
  fun `getMetadata returns correct value`() {
    val metadata = mapOf("eventId" to "event123", "eventName" to "Test Event")
    val notification = Notification(metadata = metadata)

    assertEquals("event123", notification.getMetadata("eventId"))
    assertEquals("Test Event", notification.getMetadata("eventName"))
    assertNull(notification.getMetadata("nonExistentKey"))
  }

  @Test
  fun `hasAction returns correct value`() {
    val notificationWithAction = Notification(actionUrl = "mapin://test")
    assertTrue(notificationWithAction.hasAction())

    val notificationWithoutAction = Notification(actionUrl = null)
    assertFalse(notificationWithoutAction.hasAction())

    val notificationWithEmptyAction = Notification(actionUrl = "")
    assertFalse(notificationWithEmptyAction.hasAction())
  }

  @Test
  fun `notification types enum has all expected values`() {
    val types = NotificationType.values()

    assertTrue(types.contains(NotificationType.INFO))
    assertTrue(types.contains(NotificationType.ALERT))
    assertTrue(types.contains(NotificationType.REMINDER))
    assertTrue(types.contains(NotificationType.MESSAGE))
    assertTrue(types.contains(NotificationType.FRIEND_REQUEST))
    assertTrue(types.contains(NotificationType.EVENT_INVITATION))
    assertTrue(types.contains(NotificationType.SYSTEM))
  }

  @Test
  fun `notification result success contains notification`() {
    val notification = Notification(notificationId = "test123")
    val result = NotificationResult.Success(notification)

    assertTrue(result is NotificationResult.Success)
    assertEquals(notification, result.notification)
  }

  @Test
  fun `notification result error contains message`() {
    val errorMessage = "Test error message"
    val exception = Exception("Test exception")
    val result = NotificationResult.Error(errorMessage, exception)

    assertTrue(result is NotificationResult.Error)
    assertEquals(errorMessage, result.message)
    assertEquals(exception, result.exception)
  }

  @Test
  fun `notification result error can be created without exception`() {
    val errorMessage = "Test error message"
    val result = NotificationResult.Error(errorMessage)

    assertTrue(result is NotificationResult.Error)
    assertEquals(errorMessage, result.message)
    assertNull(result.exception)
  }

  @Test
  fun `notification can be copied with modifications`() {
    val original =
        Notification(
            notificationId = "notif123", title = "Original Title", message = "Original Message")

    val modified = original.copy(title = "Modified Title")

    assertEquals("notif123", modified.notificationId)
    assertEquals("Modified Title", modified.title)
    assertEquals("Original Message", modified.message)
  }

  @Test
  fun `notification with empty metadata is valid`() {
    val notification = Notification(metadata = emptyMap())

    assertTrue(notification.metadata.isEmpty())
    assertNull(notification.getMetadata("anyKey"))
  }

  @Test
  fun `notification priority can be negative or very high`() {
    val negativeNotification = Notification(priority = -1)
    assertEquals(-1, negativeNotification.priority)

    val highNotification = Notification(priority = 999)
    assertEquals(999, highNotification.priority)
  }
}

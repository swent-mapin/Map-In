package com.swent.mapin.model

import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for NotificationService. */
class NotificationServiceTest {

  @Mock private lateinit var mockRepository: NotificationRepository

  private lateinit var service: NotificationService

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    service = NotificationService(mockRepository)
  }

  @Test
  fun `sendFriendRequestNotification creates correct notification`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendFriendRequestNotification(
        recipientId = "user123",
        senderId = "user456",
        senderName = "John Doe",
        requestId = "request789")

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertEquals("New Friend Request", notification.title)
    assertTrue(notification.message.contains("John Doe"))
    assertEquals(NotificationType.FRIEND_REQUEST, notification.type)
    assertEquals("user123", notification.recipientId)
    assertEquals("user456", notification.senderId)
    assertEquals("request789", notification.getMetadata("requestId"))
    assertEquals("mapin://friendRequests/request789", notification.actionUrl)
    assertEquals(1, notification.priority)
  }

  @Test
  fun `sendEventInvitationNotification creates correct notification`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendEventInvitationNotification(
        recipientId = "user123",
        senderId = "user456",
        senderName = "Jane Smith",
        eventId = "event789",
        eventName = "Birthday Party")

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertEquals("Event Invitation", notification.title)
    assertTrue(notification.message.contains("Jane Smith"))
    assertTrue(notification.message.contains("Birthday Party"))
    assertEquals(NotificationType.EVENT_INVITATION, notification.type)
    assertEquals("user123", notification.recipientId)
    assertEquals("user456", notification.senderId)
    assertEquals("event789", notification.getMetadata("eventId"))
    assertEquals("Birthday Party", notification.getMetadata("eventName"))
    assertEquals("mapin://events/event789", notification.actionUrl)
    assertEquals(2, notification.priority)
  }

  @Test
  fun `sendMessageNotification creates correct notification`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendMessageNotification(
        recipientId = "user123",
        senderId = "user456",
        senderName = "Bob",
        messagePreview = "Hey, how are you?",
        conversationId = "conv789")

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertEquals("New Message from Bob", notification.title)
    assertEquals("Hey, how are you?", notification.message)
    assertEquals(NotificationType.MESSAGE, notification.type)
    assertEquals("user123", notification.recipientId)
    assertEquals("user456", notification.senderId)
    assertEquals("conv789", notification.getMetadata("conversationId"))
    assertEquals("mapin://messages/conv789", notification.actionUrl)
    assertEquals(2, notification.priority)
  }

  @Test
  fun `sendEventReminderNotification with 0 minutes shows now`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendEventReminderNotification(
        recipientId = "user123",
        eventId = "event789",
        eventName = "Meeting",
        eventTime = Timestamp.now(),
        minutesBefore = 0)

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertTrue(notification.message.contains("now"))
  }

  @Test
  fun `sendEventReminderNotification with 1 minute shows singular`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendEventReminderNotification(
        recipientId = "user123",
        eventId = "event789",
        eventName = "Meeting",
        eventTime = Timestamp.now(),
        minutesBefore = 1)

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertTrue(notification.message.contains("in 1 minute"))
  }

  @Test
  fun `sendEventReminderNotification with 60 minutes shows 1 hour`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendEventReminderNotification(
        recipientId = "user123",
        eventId = "event789",
        eventName = "Meeting",
        eventTime = Timestamp.now(),
        minutesBefore = 60)

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertTrue(notification.message.contains("in 1 hour"))
  }

  @Test
  fun `sendSystemAlertNotification creates correct notification`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendSystemAlertNotification(
        recipientId = "user123",
        title = "System Alert",
        message = "Please update the app",
        priority = 3)

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertEquals("System Alert", notification.title)
    assertEquals("Please update the app", notification.message)
    assertEquals(NotificationType.ALERT, notification.type)
    assertEquals("user123", notification.recipientId)
    assertEquals(3, notification.priority)
  }

  @Test
  fun `sendInfoNotification creates correct notification`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    val metadata = mapOf("key" to "value")

    service.sendInfoNotification(
        recipientId = "user123",
        title = "Info",
        message = "This is an info message",
        metadata = metadata,
        actionUrl = "mapin://info")

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertEquals("Info", notification.title)
    assertEquals("This is an info message", notification.message)
    assertEquals(NotificationType.INFO, notification.type)
    assertEquals("user123", notification.recipientId)
    assertEquals("value", notification.getMetadata("key"))
    assertEquals("mapin://info", notification.actionUrl)
  }

  @Test
  fun `sendNotification sends successfully`() = runTest {
    val notification = Notification(title = "Test", recipientId = "user123")
    val successResult = NotificationResult.Success(notification)

    whenever(mockRepository.send(notification)).thenReturn(successResult)

    val result = service.sendNotification(notification)

    assertTrue(result is NotificationResult.Success)
    verify(mockRepository).send(notification)
  }

  @Test
  fun `sendBulkNotifications sends to multiple recipients`() = runTest {
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendBulkNotifications(
        recipientIds = listOf("user1", "user2", "user3"),
        title = "Bulk Message",
        message = "This is sent to everyone",
        type = NotificationType.INFO)

    verify(mockRepository, times(3)).send(any())
  }

  @Test
  fun `sendBulkNotifications does nothing when recipient list is empty`() = runTest {
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendBulkNotifications(
        recipientIds = emptyList(),
        title = "Bulk Message",
        message = "This is sent to everyone",
        type = NotificationType.INFO)

    verify(mockRepository, never()).send(any())
  }

  @Test
  fun `createNotification returns builder`() {
    val builder = service.createNotification()

    assertNotNull(builder)
    assertTrue(builder is NotificationBuilder)
  }

  @Test
  fun `NotificationBuilder builds notification correctly`() = runTest {
    val builder =
        service
            .createNotification()
            .title("Test Title")
            .message("Test Message")
            .type(NotificationType.ALERT)
            .recipientId("user123")
            .senderId("user456")
            .addMetadata("key1", "value1")
            .actionUrl("mapin://test")
            .priority(2)

    val notification = builder.build()

    assertEquals("Test Title", notification.title)
    assertEquals("Test Message", notification.message)
    assertEquals(NotificationType.ALERT, notification.type)
    assertEquals("user123", notification.recipientId)
    assertEquals("user456", notification.senderId)
    assertEquals("value1", notification.getMetadata("key1"))
    assertEquals("mapin://test", notification.actionUrl)
    assertEquals(2, notification.priority)
  }

  @Test
  fun `NotificationBuilder metadata method adds all metadata`() = runTest {
    val metadata = mapOf("key1" to "value1", "key2" to "value2")

    val builder =
        service
            .createNotification()
            .title("Test")
            .message("Test message")
            .recipientId("user123")
            .metadata(metadata)

    val notification = builder.build()

    assertEquals("value1", notification.getMetadata("key1"))
    assertEquals("value2", notification.getMetadata("key2"))
  }

  @Test
  fun `NotificationBuilder send method sends notification`() = runTest {
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    val result =
        service
            .createNotification()
            .title("Test")
            .message("Test message")
            .recipientId("user123")
            .send()

    assertTrue(result is NotificationResult.Success)
    verify(mockRepository).send(any())
  }

  @Test
  fun `NotificationBuilder send throws exception when title is blank`() = runTest {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          kotlinx.coroutines.runBlocking {
            service
                .createNotification()
                .title("")
                .message("Test message")
                .recipientId("user123")
                .send()
          }
        }

    assertEquals("Title cannot be blank", exception.message)
  }

  @Test
  fun `NotificationBuilder send throws exception when message is blank`() = runTest {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          kotlinx.coroutines.runBlocking {
            service.createNotification().title("Test").message("").recipientId("user123").send()
          }
        }

    assertEquals("Message cannot be blank", exception.message)
  }

  @Test
  fun `NotificationBuilder send throws exception when recipientId is blank`() = runTest {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          kotlinx.coroutines.runBlocking {
            service
                .createNotification()
                .title("Test")
                .message("Test message")
                .recipientId("")
                .send()
          }
        }

    assertEquals("RecipientId cannot be blank", exception.message)
  }

  @Test
  fun `NotificationBuilder build throws exception when title is blank`() {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          service
              .createNotification()
              .title("")
              .message("Test message")
              .recipientId("user123")
              .build()
        }

    assertEquals("Title cannot be blank", exception.message)
  }

  @Test
  fun `NotificationBuilder build throws exception when message is blank`() {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          service.createNotification().title("Test").message("").recipientId("user123").build()
        }

    assertEquals("Message cannot be blank", exception.message)
  }

  @Test
  fun `NotificationBuilder build throws exception when recipientId is blank`() {
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          service.createNotification().title("Test").message("Test message").recipientId("").build()
        }

    assertEquals("RecipientId cannot be blank", exception.message)
  }

  @Test
  fun `sendNewEventFromFollowedUserNotification creates correct notification`() = runTest {
    val captor = argumentCaptor<Notification>()
    whenever(mockRepository.send(any())).thenReturn(NotificationResult.Success(Notification()))

    service.sendNewEventFromFollowedUserNotification(
        recipientId = "follower123",
        organizerId = "organizer456",
        organizerName = "Alice",
        eventId = "event789",
        eventTitle = "Music Festival")

    verify(mockRepository).send(captor.capture())

    val notification = captor.firstValue
    assertEquals("New event from Alice", notification.title)
    assertEquals("Music Festival", notification.message)
    assertEquals(NotificationType.EVENT_INVITATION, notification.type)
    assertEquals("follower123", notification.recipientId)
    assertEquals("organizer456", notification.senderId)
    assertEquals("event789", notification.getMetadata("eventId"))
    assertEquals("Music Festival", notification.getMetadata("eventTitle"))
    assertEquals("Alice", notification.getMetadata("organizerName"))
    assertEquals("mapin://events/event789", notification.actionUrl)
    assertEquals(1, notification.priority)
  }
}

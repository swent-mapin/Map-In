package com.swent.mapin.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.messaging.RemoteMessage
import com.swent.mapin.model.NotificationType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for NotificationBackgroundManager.
 *
 * Note: These tests use Robolectric to test Android components.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationBackgroundManagerTest {

  @Mock private lateinit var mockRemoteMessage: RemoteMessage
  @Mock private lateinit var mockNotification: RemoteMessage.Notification

  private lateinit var service: NotificationBackgroundManager
  private lateinit var context: Context

  // Track sendNotification calls
  private var lastNotificationTitle: String? = null
  private var lastNotificationMessage: String? = null
  private var lastNotificationType: NotificationType? = null

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    context = RuntimeEnvironment.getApplication()

    // Create a testable version of the service that doesn't create actual notifications
    service =
        object : NotificationBackgroundManager() {
          override fun sendNotification(
              title: String,
              messageBody: String,
              type: NotificationType,
              actionUrl: String?,
              notificationId: String?
          ) {
            // Instead of creating actual notifications, just track the call
            lastNotificationTitle = title
            lastNotificationMessage = messageBody
            lastNotificationType = type
          }
        }

    // Reset tracking variables
    lastNotificationTitle = null
    lastNotificationMessage = null
    lastNotificationType = null
  }

  /** Helper function to create a real service with attached context */
  private fun createRealServiceWithContext(): NotificationBackgroundManager {
    val realService = NotificationBackgroundManager()
    val baseContextField = android.content.ContextWrapper::class.java.getDeclaredField("mBase")
    baseContextField.isAccessible = true
    baseContextField.set(realService, context)
    return realService
  }

  @Test
  fun `onNewToken is called with valid token`() {
    val testToken = "test_fcm_token_12345"

    // This should not throw an exception
    service.onNewToken(testToken)

    // Verify the method executes without errors
    // In real implementation, this would save to Firestore
  }

  @Test
  fun `onNewToken handles empty token`() {
    val emptyToken = ""

    // Should handle gracefully
    service.onNewToken(emptyToken)
  }

  @Test
  fun `service creates notification channels on Android O+`() {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Channels should be created when service receives a message
    // This is tested indirectly through onMessageReceived
    assertNotNull(notificationManager)
  }

  @Test
  fun `onMessageReceived handles notification with data payload`() {
    val data =
        mapOf(
            "title" to "Test Title",
            "message" to "Test Message",
            "type" to "INFO",
            "notificationId" to "notif123",
            "actionUrl" to "mapin://test")

    whenever(mockRemoteMessage.data).thenReturn(data)
    whenever(mockRemoteMessage.notification).thenReturn(null)

    service.onMessageReceived(mockRemoteMessage)

    // Verify sendNotification was called with correct data
    assertEquals("Test Title", lastNotificationTitle)
    assertEquals("Test Message", lastNotificationMessage)
    assertEquals(NotificationType.INFO, lastNotificationType)
  }

  @Test
  fun `onMessageReceived handles notification payload`() {
    whenever(mockRemoteMessage.notification).thenReturn(mockNotification)
    whenever(mockNotification.title).thenReturn("Test Notification")
    whenever(mockNotification.body).thenReturn("Test Body")
    whenever(mockRemoteMessage.data).thenReturn(mapOf("type" to "ALERT"))

    service.onMessageReceived(mockRemoteMessage)

    // Verify sendNotification was called with notification payload data
    assertEquals("Test Notification", lastNotificationTitle)
    assertEquals("Test Body", lastNotificationMessage)
    assertEquals(NotificationType.ALERT, lastNotificationType)
  }

  @Test
  fun `onMessageReceived handles empty message`() {
    whenever(mockRemoteMessage.notification).thenReturn(null)
    whenever(mockRemoteMessage.data).thenReturn(emptyMap())

    // Should handle gracefully without crash
    service.onMessageReceived(mockRemoteMessage)
  }

  @Test
  fun `notification types are correctly mapped`() {
    // Test that all notification types can be parsed
    val types =
        listOf(
            "INFO", "ALERT", "REMINDER", "MESSAGE", "FRIEND_REQUEST", "EVENT_INVITATION", "SYSTEM")

    types.forEach { typeString ->
      val type =
          try {
            NotificationType.valueOf(typeString)
          } catch (e: Exception) {
            null
          }
      assertNotNull("Type $typeString should be valid", type)
    }
  }

  @Test
  fun `service handles invalid notification type gracefully`() {
    val data =
        mapOf(
            "title" to "Test", "message" to "Test message", "type" to "INVALID_TYPE" // Invalid type
            )

    whenever(mockRemoteMessage.data).thenReturn(data)
    whenever(mockRemoteMessage.notification).thenReturn(null)

    // Should default to INFO and not crash
    service.onMessageReceived(mockRemoteMessage)

    // Verify it defaulted to INFO type
    assertEquals("Test", lastNotificationTitle)
    assertEquals("Test message", lastNotificationMessage)
    assertEquals(NotificationType.INFO, lastNotificationType)
  }

  @Test
  fun `onMessageReceived with from field`() {
    whenever(mockRemoteMessage.from).thenReturn("firebase-messaging")
    whenever(mockRemoteMessage.notification).thenReturn(null)
    whenever(mockRemoteMessage.data).thenReturn(emptyMap())

    service.onMessageReceived(mockRemoteMessage)

    // Verify it processes without error
    verify(mockRemoteMessage).from
  }

  @Test
  fun `notification with all data fields`() {
    val completeData =
        mapOf(
            "title" to "Complete Notification",
            "message" to "This has all fields",
            "type" to "FRIEND_REQUEST",
            "notificationId" to "notif_complete_123",
            "actionUrl" to "mapin://friendRequests/req123",
            "senderId" to "user456",
            "metadata" to "{\"key\":\"value\"}")

    whenever(mockRemoteMessage.data).thenReturn(completeData)
    whenever(mockRemoteMessage.notification).thenReturn(null)

    // Should process all fields correctly
    service.onMessageReceived(mockRemoteMessage)

    // Verify the notification was created
    assertEquals("Complete Notification", lastNotificationTitle)
    assertEquals("This has all fields", lastNotificationMessage)
    assertEquals(NotificationType.FRIEND_REQUEST, lastNotificationType)
  }

  @Test
  fun `notification with minimal data`() {
    val minimalData = mapOf("title" to "Minimal", "message" to "Just basics")

    whenever(mockRemoteMessage.data).thenReturn(minimalData)
    whenever(mockRemoteMessage.notification).thenReturn(null)

    // Should work with minimal data
    service.onMessageReceived(mockRemoteMessage)

    // Verify notification was created with minimal data
    assertEquals("Minimal", lastNotificationTitle)
    assertEquals("Just basics", lastNotificationMessage)
    assertEquals(NotificationType.INFO, lastNotificationType) // Default type
  }

  @Test
  fun `notification payload takes precedence over data`() {
    whenever(mockRemoteMessage.notification).thenReturn(mockNotification)
    whenever(mockNotification.title).thenReturn("Notification Title")
    whenever(mockNotification.body).thenReturn("Notification Body")

    val data = mapOf("title" to "Data Title", "message" to "Data Message")
    whenever(mockRemoteMessage.data).thenReturn(data)

    service.onMessageReceived(mockRemoteMessage)

    // Notification payload should be processed first
    verify(mockNotification).title
    verify(mockNotification).body

    // Verify notification was created with notification payload, not data
    assertEquals("Notification Title", lastNotificationTitle)
    assertEquals("Notification Body", lastNotificationMessage)
  }

  @Test
  fun `service instance is created successfully`() {
    assertNotNull(service)
  }

  @Test
  fun `multiple tokens can be processed`() {
    val tokens = listOf("token1", "token2", "token3")

    tokens.forEach { token ->
      // Should handle multiple token updates
      service.onNewToken(token)
    }
  }

  @Test
  fun `notification with different notification types`() {
    // Test FRIEND_REQUEST type
    val friendData =
        mapOf(
            "title" to "New Friend Request",
            "message" to "John sent you a friend request",
            "type" to "FRIEND_REQUEST",
            "actionUrl" to "mapin://friendRequests/req123")

    whenever(mockRemoteMessage.data).thenReturn(friendData)
    whenever(mockRemoteMessage.notification).thenReturn(null)

    service.onMessageReceived(mockRemoteMessage)

    assertEquals("New Friend Request", lastNotificationTitle)
    assertEquals("John sent you a friend request", lastNotificationMessage)
    assertEquals(NotificationType.FRIEND_REQUEST, lastNotificationType)
  }

  @Test
  fun `notification with null title defaults to Map-In`() {
    whenever(mockRemoteMessage.notification).thenReturn(mockNotification)
    whenever(mockNotification.title).thenReturn(null)
    whenever(mockNotification.body).thenReturn("Test Body")
    whenever(mockRemoteMessage.data).thenReturn(emptyMap())

    service.onMessageReceived(mockRemoteMessage)

    // Should default to "Map-In" when title is null
    assertEquals("Map-In", lastNotificationTitle)
    assertEquals("Test Body", lastNotificationMessage)
  }

  @Test
  fun `notification with null body defaults to empty string`() {
    whenever(mockRemoteMessage.notification).thenReturn(mockNotification)
    whenever(mockNotification.title).thenReturn("Test Title")
    whenever(mockNotification.body).thenReturn(null)
    whenever(mockRemoteMessage.data).thenReturn(emptyMap())

    service.onMessageReceived(mockRemoteMessage)

    // Should default to empty string when body is null
    assertEquals("Test Title", lastNotificationTitle)
    assertEquals("", lastNotificationMessage)
  }

  @Test
  fun `service can be instantiated`() {
    val realService = NotificationBackgroundManager()
    assertNotNull(realService)
  }

  @Test
  fun `real sendNotification creates notification with all parameters`() {
    val realService = createRealServiceWithContext()

    // Call sendNotification directly with reflection to test the real implementation
    val method =
        NotificationBackgroundManager::class
            .java
            .getDeclaredMethod(
                "sendNotification",
                String::class.java,
                String::class.java,
                NotificationType::class.java,
                String::class.java,
                String::class.java)
    method.isAccessible = true

    // This will actually create a notification using the real implementation
    method.invoke(
        realService, "Test Title", "Test Body", NotificationType.INFO, "mapin://test", "notif123")

    // The notification should be created without errors
    assertNotNull(realService)
  }

  @Test
  fun `real sendNotification handles null actionUrl and notificationId`() {
    val realService = createRealServiceWithContext()

    val method =
        NotificationBackgroundManager::class
            .java
            .getDeclaredMethod(
                "sendNotification",
                String::class.java,
                String::class.java,
                NotificationType::class.java,
                String::class.java,
                String::class.java)
    method.isAccessible = true

    // Call with null actionUrl and notificationId
    method.invoke(realService, "Test Title", "Test Body", NotificationType.ALERT, null, null)

    assertNotNull(realService)
  }

  @Test
  fun `getChannelIdForType returns correct channels`() {
    val realService = NotificationBackgroundManager()

    val method =
        NotificationBackgroundManager::class
            .java
            .getDeclaredMethod("getChannelIdForType", NotificationType::class.java)
    method.isAccessible = true

    assertEquals(
        "mapin_friend_notifications", method.invoke(realService, NotificationType.FRIEND_REQUEST))
    assertEquals(
        "mapin_event_notifications", method.invoke(realService, NotificationType.EVENT_INVITATION))
    assertEquals("mapin_event_notifications", method.invoke(realService, NotificationType.REMINDER))
    assertEquals(
        "mapin_message_notifications", method.invoke(realService, NotificationType.MESSAGE))
    assertEquals("mapin_alert_notifications", method.invoke(realService, NotificationType.ALERT))
    assertEquals("mapin_alert_notifications", method.invoke(realService, NotificationType.SYSTEM))
    assertEquals("mapin_notifications", method.invoke(realService, NotificationType.INFO))
  }

  @Test
  fun `createNotificationChannels creates channels on Android O+`() {
    val realService = NotificationBackgroundManager()
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val method =
        NotificationBackgroundManager::class
            .java
            .getDeclaredMethod("createNotificationChannels", NotificationManager::class.java)
    method.isAccessible = true

    method.invoke(realService, notificationManager)

    // On API 26+, verify channels were created
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channels = notificationManager.notificationChannels
      assertTrue(channels.isNotEmpty())
    }

    assertNotNull(realService)
  }

  @Test
  fun `onMessageReceived calls real sendNotification with data payload`() {
    // Use real service to test the complete flow
    val realService = createRealServiceWithContext()

    val data =
        mapOf(
            "title" to "Real Test",
            "message" to "Real Message",
            "type" to "ALERT",
            "actionUrl" to "mapin://test",
            "notificationId" to "real123")

    whenever(mockRemoteMessage.data).thenReturn(data)
    whenever(mockRemoteMessage.notification).thenReturn(null)

    // This will call the real sendNotification method
    realService.onMessageReceived(mockRemoteMessage)

    assertNotNull(realService)
  }

  @Test
  fun `onMessageReceived calls real sendNotification with notification payload`() {
    val realService = createRealServiceWithContext()

    whenever(mockRemoteMessage.notification).thenReturn(mockNotification)
    whenever(mockNotification.title).thenReturn("Real Notification")
    whenever(mockNotification.body).thenReturn("Real Body")
    whenever(mockRemoteMessage.data)
        .thenReturn(mapOf("type" to "MESSAGE", "actionUrl" to "mapin://msg"))

    realService.onMessageReceived(mockRemoteMessage)

    assertNotNull(realService)
  }
}

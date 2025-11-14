# Push Notification Integration Examples

> **Complete guide with copy-paste ready code examples for integrating push notifications across your app.**

---

## 📋 Table of Contents

- [Part 1: FCM Setup & Initialization](#part-1-fcm-setup--initialization)
- [Part 2: Sending Notifications](#part-2-sending-notifications)
  - [Friend Request Notifications](#friend-request-notifications)
  - [Event Notifications](#event-notifications)
  - [Message Notifications](#message-notifications)
  - [System Notifications](#system-notifications)
  - [Location Notifications](#location-notifications)
- [Part 3: Handling Notifications](#part-3-handling-notifications)
  - [Notification Action Handler](#notification-action-handler)
  - [User Preferences](#user-preferences)

---

## Part 1: FCM Setup & Initialization

### Request Notification Permission (Android 13+)

Add this to your login screen or main screen to request notification permission.

```kotlin
@Composable
fun RequestNotificationPermission(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showRationale by remember { mutableStateOf(false) }

  val permissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { isGranted ->
            if (isGranted) {
              scope.launch {
                val fcmManager = FCMTokenManager()
                val success = fcmManager.initializeForCurrentUser()
                if (success) {
                  println("FCM initialized successfully")
                  onPermissionGranted()
                }
              }
            } else {
              println("Notification permission denied")
              onPermissionDenied()
            }
          })

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      when {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED -> {
          val fcmManager = FCMTokenManager()
          val success = fcmManager.initializeForCurrentUser()
          if (success) {
            println("FCM already initialized")
            onPermissionGranted()
          }
        }
        else -> {
          showRationale = true
        }
      }
    } else {
      val fcmManager = FCMTokenManager()
      val success = fcmManager.initializeForCurrentUser()
      if (success) {
        println("FCM initialized (Android <13)")
        onPermissionGranted()
      }
    }
  }

  if (showRationale) {
    AlertDialog(
        onDismissRequest = { showRationale = false },
        title = { Text("Enable Notifications") },
        text = {
          Text(
              "Stay updated! Enable notifications to receive:\n" +
                  "• Friend requests\n" +
                  "• Event invitations\n" +
                  "• New messages\n" +
                  "• Event reminders")
        },
        confirmButton = {
          Button(
              onClick = {
                showRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              }) {
                Text("Allow")
              }
        },
        dismissButton = {
          Button(
              onClick = {
                showRationale = false
                onPermissionDenied()
              }) {
                Text("Not Now")
              }
        })
  }
}
```

**Usage:**
```kotlin
@Composable
fun YourScreen() {
    RequestNotificationPermission()
}
```

---

### Initialize FCM After Login

```kotlin
suspend fun initializePushNotifications(userId: String): Boolean {
  val fcmManager = FCMTokenManager()
  val success = fcmManager.initializeForCurrentUser()

  if (success) {
    // Optional: Subscribe to topics
    fcmManager.subscribeToTopic("all_users")
    println("Push notifications initialized for user: $userId")
  }

  return success
}
```

**Usage:**
```kotlin
lifecycleScope.launch {
    initializePushNotifications(currentUserId)
}
```

---

### Clean Up FCM on Logout

```kotlin
suspend fun cleanupPushNotifications() {
  val fcmManager = FCMTokenManager()
  fcmManager.removeTokenForCurrentUser()
  fcmManager.unsubscribeFromTopic("all_users")
  println("Push notifications cleaned up")
}
```

**Usage:**
```kotlin
lifecycleScope.launch {
    cleanupPushNotifications()
}
```

---

### Get FCM Token for Debugging

```kotlin
suspend fun debugFCMToken(): String? {
  val fcmManager = FCMTokenManager()
  val token = fcmManager.getToken()
  println("FCM Token: $token")
  return token
}
```

**Usage:**
```kotlin
lifecycleScope.launch {
    val token = debugFCMToken()
    Log.d("FCM", "Token: $token")
}
```

---

## Part 2: Sending Notifications

### Friend Request Notifications

```kotlin
class FriendRequestNotificationExample(
    private val notificationService: NotificationService,
    private val scope: CoroutineScope
) {

  /** Send notification when a friend request is created. */
  fun onFriendRequestSent(
      fromUserId: String,
      toUserId: String,
      senderName: String,
      requestId: String
  ) {
    scope.launch {
      notificationService.sendFriendRequestNotification(
          recipientId = toUserId,
          senderId = fromUserId,
          senderName = senderName,
          requestId = requestId)
    }
  }

  /** Send notification when a friend request is accepted. */
  fun onFriendRequestAccepted(acceptorId: String, requesterId: String, acceptorName: String) {
    scope.launch {
      notificationService.sendInfoNotification(
          recipientId = requesterId,
          title = "Friend Request Accepted",
          message = "$acceptorName accepted your friend request",
          metadata = mapOf("userId" to acceptorId),
          actionUrl = "mapin://profile/$acceptorId")
    }
  }
}
```

**Usage:**
```kotlin
val example = FriendRequestNotificationExample(notificationService, viewModelScope)

// When sending friend request
example.onFriendRequestSent(
    fromUserId = currentUser.id,
    toUserId = targetUser.id,
    senderName = currentUser.name,
    requestId = request.id
)

// When accepting friend request
example.onFriendRequestAccepted(
    acceptorId = currentUser.id,
    requesterId = request.senderId,
    acceptorName = currentUser.name
)
```

---

### Event Notifications

```kotlin
class EventNotificationExample(
    private val notificationService: NotificationService,
    private val scope: CoroutineScope
) {

  /** Send notifications when users are invited to an event. */
  fun notifyEventInvitees(
      eventId: String,
      eventName: String,
      organizerId: String,
      organizerName: String,
      inviteeIds: List<String>
  ) {
    scope.launch {
      notificationService.sendBulkNotifications(
          recipientIds = inviteeIds,
          title = "Event Invitation",
          message = "$organizerName invited you to $eventName",
          type = NotificationType.EVENT_INVITATION,
          senderId = organizerId,
          metadata = mapOf("eventId" to eventId, "eventName" to eventName),
          actionUrl = "mapin://events/$eventId")
    }
  }

  /** Send reminder notifications before an event starts. */
  fun scheduleEventReminders(
      eventId: String,
      eventName: String,
      eventTime: Timestamp,
      participantIds: List<String>
  ) {
    // Schedule reminders at different intervals
    val reminderIntervals = listOf(60, 30, 15) // minutes before event

    scope.launch {
      participantIds.forEach { userId ->
        reminderIntervals.forEach { minutesBefore ->
          notificationService.sendEventReminderNotification(
              recipientId = userId,
              eventId = eventId,
              eventName = eventName,
              eventTime = eventTime,
              minutesBefore = minutesBefore)
        }
      }
    }
  }

  /** Notify when event is cancelled. */
  fun notifyEventCancellation(
      eventId: String,
      eventName: String,
      participantIds: List<String>,
      reason: String? = null
  ) {
    val message =
        if (reason != null) {
          "Event '$eventName' has been cancelled. Reason: $reason"
        } else {
          "Event '$eventName' has been cancelled"
        }

    scope.launch {
      notificationService.sendBulkNotifications(
          recipientIds = participantIds,
          title = "Event Cancelled",
          message = message,
          type = NotificationType.ALERT,
          metadata = mapOf("eventId" to eventId),
          actionUrl = "mapin://events/$eventId")
    }
  }

  /** Notify when event details are updated. */
  fun notifyEventUpdate(
      eventId: String,
      eventName: String,
      participantIds: List<String>,
      updateDescription: String
  ) {
    scope.launch {
      notificationService.sendBulkNotifications(
          recipientIds = participantIds,
          title = "Event Updated",
          message = "$eventName has been updated: $updateDescription",
          type = NotificationType.INFO,
          metadata = mapOf("eventId" to eventId),
          actionUrl = "mapin://events/$eventId")
    }
  }
}
```

**Usage:**
```kotlin
val example = EventNotificationExample(notificationService, viewModelScope)

// Invite users to event
example.notifyEventInvitees(
    eventId = event.id,
    eventName = event.name,
    organizerId = currentUser.id,
    organizerName = currentUser.name,
    inviteeIds = selectedUsers.map { it.id }
)

// Schedule reminders
example.scheduleEventReminders(
    eventId = event.id,
    eventName = event.name,
    eventTime = event.startTime,
    participantIds = event.participants
)

// Cancel event
example.notifyEventCancellation(
    eventId = event.id,
    eventName = event.name,
    participantIds = event.participants,
    reason = "Weather conditions"
)
```

---

### Message Notifications

```kotlin
class MessageNotificationExample(
    private val notificationService: NotificationService,
    private val scope: CoroutineScope
) {

  /** Send notification for a new chat message. */
  fun onNewMessage(
      recipientId: String,
      senderId: String,
      senderName: String,
      messageText: String,
      conversationId: String
  ) {
    // Only send if recipient is not currently viewing the conversation
    if (!isUserViewingConversation(recipientId, conversationId)) {
      val preview =
          if (messageText.length > 50) {
            messageText.take(47) + "..."
          } else {
            messageText
          }

      scope.launch {
        notificationService.sendMessageNotification(
            recipientId = recipientId,
            senderId = senderId,
            senderName = senderName,
            messagePreview = preview,
            conversationId = conversationId)
      }
    }
  }

  /** Send notification for multiple unread messages. */
  fun onMultipleUnreadMessages(
      recipientId: String,
      conversationId: String,
      senderName: String,
      messageCount: Int
  ) {
    scope.launch {
      notificationService
          .createNotification()
          .title("New Messages")
          .message("You have $messageCount unread messages from $senderName")
          .type(NotificationType.MESSAGE)
          .recipientId(recipientId)
          .addMetadata("conversationId", conversationId)
          .addMetadata("messageCount", messageCount.toString())
          .actionUrl("mapin://messages/$conversationId")
          .priority(2)
          .send()
    }
  }

  // Mock function - replace with actual implementation
  private fun isUserViewingConversation(userId: String, conversationId: String): Boolean {
    // Check if user is currently in the conversation screen
    return false
  }
}
```

**Usage:**
```kotlin
val example = MessageNotificationExample(notificationService, viewModelScope)

// New message
example.onNewMessage(
    recipientId = recipientUser.id,
    senderId = currentUser.id,
    senderName = currentUser.name,
    messageText = "Hey, are you coming to the party?",
    conversationId = conversation.id
)

// Multiple unread
example.onMultipleUnreadMessages(
    recipientId = user.id,
    conversationId = conversation.id,
    senderName = sender.name,
    messageCount = 5
)
```

---

### System Notifications

```kotlin
class SystemNotificationExample(
    private val notificationService: NotificationService,
    private val scope: CoroutineScope
) {

  /** Notify users about app updates. */
  fun notifyAppUpdate(userId: String, version: String, features: List<String>) {
    val message = buildString {
      append("New features in version $version:\n")
      features.take(3).forEach { feature -> append("• $feature\n") }
    }

    scope.launch {
      notificationService.sendInfoNotification(
          recipientId = userId,
          title = "App Updated!",
          message = message,
          metadata = mapOf("version" to version),
          actionUrl = "mapin://settings/about")
    }
  }

  /** Send maintenance notification. */
  fun notifyMaintenance(userIds: List<String>, startTime: String, duration: String) {
    scope.launch {
      notificationService.sendBulkNotifications(
          recipientIds = userIds,
          title = "Scheduled Maintenance",
          message = "The app will be under maintenance starting $startTime for $duration",
          type = NotificationType.SYSTEM,
          metadata = mapOf("startTime" to startTime, "duration" to duration))
    }
  }

  /** Send security alert. */
  fun notifySecurityAlert(userId: String, alertType: String, details: String) {
    scope.launch {
      notificationService.sendSystemAlertNotification(
          recipientId = userId,
          title = "Security Alert: $alertType",
          message = details,
          priority = 3)
    }
  }
}
```

**Usage:**
```kotlin
val example = SystemNotificationExample(notificationService, viewModelScope)

// App update
example.notifyAppUpdate(
    userId = user.id,
    version = "2.0.0",
    features = listOf("Dark mode", "New map features", "Performance improvements")
)

// Maintenance window
example.notifyMaintenance(
    userIds = allUsers.map { it.id },
    startTime = "2:00 AM",
    duration = "1 hour"
)
```

---

### Location Notifications

```kotlin
class LocationNotificationExample(
    private val notificationService: NotificationService,
    private val scope: CoroutineScope
) {

  /** Notify when a friend is nearby. */
  fun notifyFriendNearby(userId: String, friendId: String, friendName: String, distance: Double) {
    scope.launch {
      notificationService
          .createNotification()
          .title("Friend Nearby")
          .message("$friendName is ${distance.toInt()}m away from you")
          .type(NotificationType.INFO)
          .recipientId(userId)
          .senderId(friendId)
          .addMetadata("friendId", friendId)
          .addMetadata("distance", distance.toString())
          .actionUrl("mapin://map?focus=$friendId")
          .priority(1)
          .send()
    }
  }

  /** Notify when arriving at event location. */
  fun notifyEventLocationArrival(userId: String, eventId: String, eventName: String) {
    scope.launch {
      notificationService.sendInfoNotification(
          recipientId = userId,
          title = "You've arrived!",
          message = "Welcome to $eventName",
          metadata = mapOf("eventId" to eventId),
          actionUrl = "mapin://events/$eventId")
    }
  }
}
```

**Usage:**
```kotlin
val example = LocationNotificationExample(notificationService, viewModelScope)

// Friend nearby
example.notifyFriendNearby(
    userId = currentUser.id,
    friendId = friend.id,
    friendName = friend.name,
    distance = 150.5 // meters
)

// Arrived at event
example.notifyEventLocationArrival(
    userId = currentUser.id,
    eventId = event.id,
    eventName = event.name
)
```

---

## Part 3: Handling Notifications

### Notification Action Handler

```kotlin
class NotificationActionHandler {

  /** Handle notification click/action. */
  fun handleNotificationAction(notification: Notification, navigate: (String) -> Unit) {
    when (notification.type) {
      NotificationType.FRIEND_REQUEST -> {
        val requestId = notification.getMetadata("requestId")
        requestId?.let { navigate("mapin://friendRequests/$it") }
      }
      NotificationType.EVENT_INVITATION -> {
        val eventId = notification.getMetadata("eventId")
        eventId?.let { navigate("mapin://events/$it") }
      }
      NotificationType.MESSAGE -> {
        val conversationId = notification.getMetadata("conversationId")
        conversationId?.let { navigate("mapin://messages/$it") }
      }
      NotificationType.REMINDER -> {
        val eventId = notification.getMetadata("eventId")
        eventId?.let { navigate("mapin://events/$it") }
      }
      else -> {
        // Use generic actionUrl if available
        notification.actionUrl?.let { navigate(it) }
      }
    }
  }

  /** Filter notifications by criteria. */
  fun filterHighPriorityUnread(notifications: List<Notification>): List<Notification> {
    return notifications.filter { it.isUnread() && it.priority >= 2 }
  }

  /** Group notifications by type. */
  fun groupNotificationsByType(
      notifications: List<Notification>
  ): Map<NotificationType, List<Notification>> {
    return notifications.groupBy { it.type }
  }

  /** Get notification summary for badge display. */
  fun getNotificationSummary(notifications: List<Notification>): String {
    val unreadCount = notifications.count { it.isUnread() }
    return when {
      unreadCount == 0 -> "No new notifications"
      unreadCount == 1 -> "1 new notification"
      else -> "$unreadCount new notifications"
    }
  }
}
```

**Usage:**
```kotlin
val handler = NotificationActionHandler()

// Handle notification tap
handler.handleNotificationAction(notification) { deepLink ->
    navController.navigate(deepLink)
}

// Filter high priority
val urgentNotifications = handler.filterHighPriorityUnread(allNotifications)

// Group by type
val grouped = handler.groupNotificationsByType(allNotifications)
val friendRequests = grouped[NotificationType.FRIEND_REQUEST] ?: emptyList()

// Get summary
val summary = handler.getNotificationSummary(allNotifications)
```

---

### User Preferences

```kotlin
class NotificationPreferencesExample {

  data class NotificationPreferences(
      val enableFriendRequests: Boolean = true,
      val enableMessages: Boolean = true,
      val enableEventInvitations: Boolean = true,
      val enableEventReminders: Boolean = true,
      val enableSystemNotifications: Boolean = true,
      val quietHoursStart: Int? = null, // Hour of day (0-23)
      val quietHoursEnd: Int? = null
  )

  /** Check if notification should be sent based on user preferences. */
  fun shouldSendNotification(
      notification: Notification,
      preferences: NotificationPreferences
  ): Boolean {
    // Check quiet hours
    if (isQuietHours(preferences)) {
      return notification.priority >= 3 // Only urgent notifications during quiet hours
    }

    // Check type-specific preferences
    return when (notification.type) {
      NotificationType.FRIEND_REQUEST -> preferences.enableFriendRequests
      NotificationType.MESSAGE -> preferences.enableMessages
      NotificationType.EVENT_INVITATION -> preferences.enableEventInvitations
      NotificationType.REMINDER -> preferences.enableEventReminders
      NotificationType.SYSTEM,
      NotificationType.ALERT -> preferences.enableSystemNotifications
      else -> true
    }
  }

  private fun isQuietHours(preferences: NotificationPreferences): Boolean {
    val now = java.util.Calendar.getInstance()
    val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)

    val start = preferences.quietHoursStart ?: return false
    val end = preferences.quietHoursEnd ?: return false

    return if (start < end) {
      currentHour in start until end
    } else {
      currentHour >= start || currentHour < end
    }
  }
}
```

**Usage:**
```kotlin
val preferences = NotificationPreferences(
    enableMessages = true,
    enableFriendRequests = false,
    quietHoursStart = 22, // 10 PM
    quietHoursEnd = 8     // 8 AM
)

val preferencesHandler = NotificationPreferencesExample()

// Before sending notification
if (preferencesHandler.shouldSendNotification(notification, preferences)) {
    // Send notification
    notificationService.sendNotification(notification)
}
```

---

## 🎯 Quick Reference

### Common Notification Patterns

**1. Simple notification with action:**
```kotlin
notificationService.sendInfoNotification(
    recipientId = userId,
    title = "Hello!",
    message = "Welcome back",
    actionUrl = "mapin://home"
)
```

**2. Notification with metadata:**
```kotlin
notificationService
    .createNotification()
    .title("Friend Request")
    .message("John wants to be friends")
    .type(NotificationType.FRIEND_REQUEST)
    .recipientId(userId)
    .senderId(senderId)
    .addMetadata("requestId", requestId)
    .actionUrl("mapin://friendRequests/$requestId")
    .send()
```

**3. Bulk notification:**
```kotlin
notificationService.sendBulkNotifications(
    recipientIds = listOf("user1", "user2", "user3"),
    title = "Event Reminder",
    message = "Party starts in 1 hour!",
    type = NotificationType.REMINDER,
    actionUrl = "mapin://events/$eventId"
)
```

---

## ⚠️ Important Notes

1. **Don't forget to initialize FCM** after user login
2. **Clean up on logout** to remove FCM tokens
3. **Check user preferences** before sending notifications
4. **Use appropriate notification types** for proper channel routing
5. **Include actionUrl** for deep linking
6. **Add metadata** for additional context
7. **Test with Firebase Console** before deploying server code

---

## 📚 Related Documentation

- `PUSH_QUICK_START.md` - Quick setup guide
- `PUSH_NOTIFICATIONS_TOKEN_GUIDE.md` - Token management details
- `NOTIFICATION_INTEGRATION_GUIDE.md` - Complete integration reference
- Firebase Cloud Messaging docs - https://firebase.google.com/docs/cloud-messaging

---

**Need help?** Check the PR descriptions or ask your team! 🚀


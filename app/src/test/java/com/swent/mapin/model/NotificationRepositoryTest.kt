package com.swent.mapin.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.swent.mapin.model.notifications.Notification
import com.swent.mapin.model.notifications.NotificationRepository
import com.swent.mapin.model.notifications.NotificationResult
import com.swent.mapin.model.notifications.NotificationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/** Unit tests for NotificationRepository. */
class NotificationRepositoryTest {

  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockDocumentReference: DocumentReference
  @Mock private lateinit var mockDocumentSnapshot: DocumentSnapshot
  @Mock private lateinit var mockQuerySnapshot: QuerySnapshot
  @Mock private lateinit var mockQuery: Query

  private lateinit var repository: NotificationRepository

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    repository = NotificationRepository(mockFirestore)

    // Setup default mock behavior
    whenever(mockFirestore.collection(any<String>())).thenReturn(mockCollectionReference)
    whenever(mockCollectionReference.document()).thenReturn(mockDocumentReference)
    whenever(mockCollectionReference.document(any<String>())).thenReturn(mockDocumentReference)
    whenever(mockDocumentReference.id).thenReturn("generatedId123")
  }

  @Test
  fun `send notification with generated ID succeeds`() = runTest {
    val notification =
        Notification(title = "Test", message = "Test message", recipientId = "user123")

    whenever(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))

    val result = repository.send(notification)

    assertTrue(result is NotificationResult.Success)
    if (result is NotificationResult.Success) {
      assertEquals("generatedId123", result.notification.notificationId)
      assertEquals("Test", result.notification.title)
    }

    verify(mockDocumentReference).set(any())
  }

  @Test
  fun `send notification with existing ID succeeds`() = runTest {
    val notification =
        Notification(
            notificationId = "existing123",
            title = "Test",
            message = "Test message",
            recipientId = "user123")

    whenever(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))

    val result = repository.send(notification)

    assertTrue(result is NotificationResult.Success)
    if (result is NotificationResult.Success) {
      assertEquals("existing123", result.notification.notificationId)
    }

    verify(mockDocumentReference).set(any())
  }

  @Test
  fun `send notification failure returns error`() = runTest {
    val notification = Notification(title = "Test", recipientId = "user123")
    val exception = Exception("Firestore error")

    whenever(mockDocumentReference.set(any())).thenReturn(Tasks.forException(exception))

    val result = repository.send(notification)

    assertTrue(result is NotificationResult.Error)
    if (result is NotificationResult.Error) {
      assertTrue(result.message.contains("Failed to send notification"))
    }
  }

  @Test
  fun `getNotificationsForUser returns list of notifications`() = runTest {
    val notification1 =
        Notification(notificationId = "notif1", title = "Test 1", recipientId = "user123")
    val notification2 =
        Notification(notificationId = "notif2", title = "Test 2", recipientId = "user123")

    val mockDoc1 = mock(DocumentSnapshot::class.java)
    val mockDoc2 = mock(DocumentSnapshot::class.java)

    whenever(mockDoc1.toObject(Notification::class.java)).thenReturn(notification1)
    whenever(mockDoc2.toObject(Notification::class.java)).thenReturn(notification2)

    whenever(mockCollectionReference.whereEqualTo(any<String>(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(any<String>(), any())).thenReturn(mockQuery)
    whenever(mockQuery.orderBy(any<String>(), any<Query.Direction>())).thenReturn(mockQuery)
    whenever(mockQuery.limit(any<Long>())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDoc1, mockDoc2))

    val notifications = repository.getNotificationsForUser("user123")

    assertEquals(2, notifications.size)
    assertEquals("notif1", notifications[0].notificationId)
    assertEquals("notif2", notifications[1].notificationId)
  }

  @Test
  fun `getNotificationsForUser with includeRead false filters read notifications`() = runTest {
    whenever(mockCollectionReference.whereEqualTo(any<String>(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(any<String>(), any())).thenReturn(mockQuery)
    whenever(mockQuery.orderBy(any<String>(), any<Query.Direction>())).thenReturn(mockQuery)
    whenever(mockQuery.limit(any<Long>())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(emptyList())

    repository.getNotificationsForUser("user123", includeRead = false)

    // Verify that whereEqualTo was called with readStatus = false
    verify(mockQuery, atLeastOnce()).whereEqualTo("readStatus", false)
  }

  @Test
  fun `getUnreadCount returns correct count`() = runTest {
    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.size()).thenReturn(5)

    val count = repository.getUnreadCount("user123")

    assertEquals(5, count)
  }

  @Test
  fun `getUnreadCount returns zero on error`() = runTest {
    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forException(Exception("Error")))

    val count = repository.getUnreadCount("user123")

    assertEquals(0, count)
  }

  @Test
  fun `markAsRead succeeds`() = runTest {
    whenever(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null))

    val success = repository.markAsRead("notif123")

    assertTrue(success)
    verify(mockDocumentReference).update("readStatus", true)
  }

  @Test
  fun `markAsRead returns false on error`() = runTest {
    whenever(mockDocumentReference.update(anyString(), any()))
        .thenReturn(Tasks.forException(Exception("Error")))

    val success = repository.markAsRead("notif123")

    assertFalse(success)
  }

  @Test
  fun `markMultipleAsRead succeeds`() = runTest {
    val mockBatch = mock(WriteBatch::class.java)
    whenever(mockFirestore.batch()).thenReturn(mockBatch)
    whenever(mockBatch.update(any<DocumentReference>(), anyString(), any())).thenReturn(mockBatch)
    whenever(mockBatch.commit()).thenReturn(Tasks.forResult(null))

    val success = repository.markMultipleAsRead(listOf("notif1", "notif2", "notif3"))

    assertTrue(success)
    verify(mockBatch, times(3)).update(any<DocumentReference>(), eq("readStatus"), eq(true))
    verify(mockBatch).commit()
  }

  @Test
  fun `markAllAsRead succeeds`() = runTest {
    val mockBatch = mock(WriteBatch::class.java)
    val mockDoc1 = mock(DocumentSnapshot::class.java)
    val mockDoc2 = mock(DocumentSnapshot::class.java)
    val mockDocRef1 = mock(DocumentReference::class.java)
    val mockDocRef2 = mock(DocumentReference::class.java)

    whenever(mockDoc1.reference).thenReturn(mockDocRef1)
    whenever(mockDoc2.reference).thenReturn(mockDocRef2)

    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDoc1, mockDoc2))

    whenever(mockFirestore.batch()).thenReturn(mockBatch)
    whenever(mockBatch.update(any<DocumentReference>(), anyString(), any())).thenReturn(mockBatch)
    whenever(mockBatch.commit()).thenReturn(Tasks.forResult(null))

    val success = repository.markAllAsRead("user123")

    assertTrue(success)
    verify(mockBatch).commit()
  }

  @Test
  fun `deleteNotification succeeds`() = runTest {
    whenever(mockDocumentReference.delete()).thenReturn(Tasks.forResult(null))

    val success = repository.deleteNotification("notif123")

    assertTrue(success)
    verify(mockDocumentReference).delete()
  }

  @Test
  fun `deleteNotification returns false on error`() = runTest {
    whenever(mockDocumentReference.delete()).thenReturn(Tasks.forException(Exception("Error")))

    val success = repository.deleteNotification("notif123")

    assertFalse(success)
  }

  @Test
  fun `deleteAllNotifications succeeds`() = runTest {
    val mockBatch = mock(WriteBatch::class.java)
    val mockDoc1 = mock(DocumentSnapshot::class.java)
    val mockDocRef1 = mock(DocumentReference::class.java)

    whenever(mockDoc1.reference).thenReturn(mockDocRef1)

    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDoc1))

    whenever(mockFirestore.batch()).thenReturn(mockBatch)
    whenever(mockBatch.delete(any())).thenReturn(mockBatch)
    whenever(mockBatch.commit()).thenReturn(Tasks.forResult(null))

    val success = repository.deleteAllNotifications("user123")

    assertTrue(success)
    verify(mockBatch).commit()
  }

  @Test
  fun `getNotification returns notification`() = runTest {
    val notification = Notification(notificationId = "notif123", title = "Test")

    whenever(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockDocumentSnapshot))
    whenever(mockDocumentSnapshot.toObject(Notification::class.java)).thenReturn(notification)

    val result = repository.getNotification("notif123")

    assertNotNull(result)
    assertEquals("notif123", result?.notificationId)
    assertEquals("Test", result?.title)
  }

  @Test
  fun `getNotification returns null on error`() = runTest {
    whenever(mockDocumentReference.get()).thenReturn(Tasks.forException(Exception("Error")))

    val result = repository.getNotification("notif123")

    assertNull(result)
  }

  @Test
  fun `getNotificationsByType returns filtered notifications`() = runTest {
    val notification =
        Notification(notificationId = "notif1", title = "Test", type = NotificationType.ALERT)

    val mockDoc = mock(DocumentSnapshot::class.java)
    whenever(mockDoc.toObject(Notification::class.java)).thenReturn(notification)

    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.orderBy(anyString(), any(Query.Direction::class.java))).thenReturn(mockQuery)
    whenever(mockQuery.limit(any<Long>())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDoc))

    val notifications = repository.getNotificationsByType("user123", NotificationType.ALERT)

    assertEquals(1, notifications.size)
    assertEquals(NotificationType.ALERT, notifications[0].type)
  }

  @Test
  fun `markMultipleAsRead returns false on error`() = runTest {
    val mockBatch = mock(WriteBatch::class.java)
    whenever(mockFirestore.batch()).thenReturn(mockBatch)
    whenever(mockBatch.update(any<DocumentReference>(), anyString(), any())).thenReturn(mockBatch)
    whenever(mockBatch.commit()).thenReturn(Tasks.forException(Exception("Error")))

    val success = repository.markMultipleAsRead(listOf("notif1", "notif2"))

    assertFalse(success)
  }

  @Test
  fun `markMultipleAsRead succeeds with empty list`() = runTest {
    val mockBatch = mock(WriteBatch::class.java)
    whenever(mockFirestore.batch()).thenReturn(mockBatch)
    whenever(mockBatch.commit()).thenReturn(Tasks.forResult(null))

    val success = repository.markMultipleAsRead(emptyList())

    assertTrue(success)
    // Verify no update operations were invoked for empty list (but batch/commit still called)
    verify(mockBatch, never()).update(any<DocumentReference>(), anyString(), any())
    verify(mockBatch).commit()
  }

  @Test
  fun `markAllAsRead returns false on error`() = runTest {
    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forException(Exception("Error")))

    val success = repository.markAllAsRead("user123")

    assertFalse(success)
  }

  @Test
  fun `deleteAllNotifications returns false on error`() = runTest {
    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forException(Exception("Error")))

    val success = repository.deleteAllNotifications("user123")

    assertFalse(success)
  }

  @Test
  fun `getNotificationsForUser returns empty list on error`() = runTest {
    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.orderBy(anyString(), any<Query.Direction>())).thenReturn(mockQuery)
    whenever(mockQuery.limit(any<Long>())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forException(Exception("Error")))

    val notifications = repository.getNotificationsForUser("user123")

    assertTrue(notifications.isEmpty())
  }

  @Test
  fun `getNotificationsForUser with custom limit`() = runTest {
    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.orderBy(anyString(), any<Query.Direction>())).thenReturn(mockQuery)
    whenever(mockQuery.limit(any<Long>())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(emptyList())

    repository.getNotificationsForUser("user123", includeRead = true, limit = 100)

    verify(mockQuery).limit(100L)
  }

  @Test
  fun `deleteAllNotifications with empty query succeeds`() = runTest {
    val mockBatch = mock(WriteBatch::class.java)

    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(emptyList())
    whenever(mockFirestore.batch()).thenReturn(mockBatch)
    whenever(mockBatch.commit()).thenReturn(Tasks.forResult(null))

    val success = repository.deleteAllNotifications("user123")

    assertTrue(success)
    verify(mockBatch, never()).delete(any())
  }

  @Test
  fun `send notification with generated ID handles firestore exception`() = runTest {
    whenever(mockDocumentReference.set(any()))
        .thenReturn(Tasks.forException(Exception("Network error")))

    val notification = Notification(title = "Test", message = "Test", recipientId = "user123")
    val result = repository.send(notification)

    assertTrue(result is NotificationResult.Error)
    if (result is NotificationResult.Error) {
      assertTrue(result.message.contains("Failed to send notification"))
      assertNotNull(result.exception)
    }
  }

  @Test
  fun `markAllAsRead with batch commit failure`() = runTest {
    val mockBatch = mock(WriteBatch::class.java)
    val mockDoc = mock(DocumentSnapshot::class.java)
    val mockDocRef = mock(DocumentReference::class.java)

    whenever(mockDoc.reference).thenReturn(mockDocRef)
    whenever(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDoc))
    whenever(mockFirestore.batch()).thenReturn(mockBatch)
    whenever(mockBatch.update(any<DocumentReference>(), anyString(), any())).thenReturn(mockBatch)
    whenever(mockBatch.commit()).thenReturn(Tasks.forException(Exception("Commit failed")))

    val success = repository.markAllAsRead("user123")

    assertFalse(success)
  }

  @Test
  fun `getNotification with document conversion returns null`() = runTest {
    whenever(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockDocumentSnapshot))
    whenever(mockDocumentSnapshot.toObject(Notification::class.java)).thenReturn(null)

    val result = repository.getNotification("notif123")

    assertNull(result)
  }
}

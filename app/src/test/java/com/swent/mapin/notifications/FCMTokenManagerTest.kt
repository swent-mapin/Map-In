package com.swent.mapin.notifications

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/** Unit tests for FCMTokenManager. */
class FCMTokenManagerTest {

  @Mock private lateinit var mockMessaging: FirebaseMessaging
  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockAuth: FirebaseAuth
  @Mock private lateinit var mockUser: FirebaseUser
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockDocumentReference: DocumentReference

  private lateinit var tokenManager: FCMTokenManager

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    tokenManager = FCMTokenManager(mockMessaging, mockFirestore, mockAuth)

    // Setup default mocks
    whenever(mockFirestore.collection(anyString())).thenReturn(mockCollectionReference)
    whenever(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference)
  }

  @Test
  fun `getToken returns token successfully`() = runTest {
    val expectedToken = "test_fcm_token_123"
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(expectedToken))

    val token = tokenManager.getToken()

    assertEquals(expectedToken, token)
    verify(mockMessaging).token
  }

  @Test
  fun `getToken returns null on error`() = runTest {
    whenever(mockMessaging.token).thenReturn(Tasks.forException(Exception("Error")))

    val token = tokenManager.getToken()

    assertNull(token)
  }

  @Test
  fun `saveTokenForCurrentUser saves token when user exists`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_token_456"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null))

    val success = tokenManager.saveTokenForCurrentUser()

    assertTrue(success)
    verify(mockDocumentReference).update("fcmToken", fcmToken)
  }

  @Test
  fun `saveTokenForCurrentUser returns false when no user`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)

    val success = tokenManager.saveTokenForCurrentUser()

    assertFalse(success)
  }

  @Test
  fun `saveTokenForCurrentUser uses set with merge when update fails`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_token_789"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any()))
        .thenReturn(Tasks.forException(Exception("Update failed")))
    whenever(mockDocumentReference.set(any<Map<String, String>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    val success = tokenManager.saveTokenForCurrentUser()

    assertTrue(success)
    verify(mockDocumentReference).set(any<Map<String, String>>(), any<SetOptions>())
  }

  @Test
  fun `saveTokenForCurrentUser with custom token`() = runTest {
    val userId = "user123"
    val customToken = "custom_fcm_token"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null))

    val success = tokenManager.saveTokenForCurrentUser(customToken)

    assertTrue(success)
    verify(mockDocumentReference).update("fcmToken", customToken)
  }

  @Test
  fun `addTokenForCurrentUser adds token to array`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_token_multi"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null))

    val success = tokenManager.addTokenForCurrentUser()

    assertTrue(success)
    verify(mockDocumentReference).update(eq("fcmTokens"), any())
  }

  @Test
  fun `removeTokenForCurrentUser removes token from array`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_token_remove"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null))

    val success = tokenManager.removeTokenForCurrentUser()

    assertTrue(success)
    verify(mockDocumentReference).update(eq("fcmTokens"), any())
  }

  @Test
  fun `subscribeToTopic subscribes successfully`() = runTest {
    val topic = "all_users"
    whenever(mockMessaging.subscribeToTopic(topic)).thenReturn(Tasks.forResult(null))

    val success = tokenManager.subscribeToTopic(topic)

    assertTrue(success)
    verify(mockMessaging).subscribeToTopic(topic)
  }

  @Test
  fun `subscribeToTopic returns false on error`() = runTest {
    val topic = "test_topic"
    whenever(mockMessaging.subscribeToTopic(topic))
        .thenReturn(Tasks.forException(Exception("Error")))

    val success = tokenManager.subscribeToTopic(topic)

    assertFalse(success)
  }

  @Test
  fun `unsubscribeFromTopic unsubscribes successfully`() = runTest {
    val topic = "all_users"
    whenever(mockMessaging.unsubscribeFromTopic(topic)).thenReturn(Tasks.forResult(null))

    val success = tokenManager.unsubscribeFromTopic(topic)

    assertTrue(success)
    verify(mockMessaging).unsubscribeFromTopic(topic)
  }

  @Test
  fun `unsubscribeFromTopic returns false on error`() = runTest {
    val topic = "test_topic"
    whenever(mockMessaging.unsubscribeFromTopic(topic))
        .thenReturn(Tasks.forException(Exception("Error")))

    val success = tokenManager.unsubscribeFromTopic(topic)

    assertFalse(success)
  }

  @Test
  fun `deleteToken deletes successfully`() = runTest {
    whenever(mockMessaging.deleteToken()).thenReturn(Tasks.forResult(null))

    val success = tokenManager.deleteToken()

    assertTrue(success)
    verify(mockMessaging).deleteToken()
  }

  @Test
  fun `deleteToken returns false on error`() = runTest {
    whenever(mockMessaging.deleteToken()).thenReturn(Tasks.forException(Exception("Error")))

    val success = tokenManager.deleteToken()

    assertFalse(success)
  }

  @Test
  fun `initializeForCurrentUser gets token and saves it`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_init_token"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any())).thenReturn(Tasks.forResult(null))

    val success = tokenManager.initializeForCurrentUser()

    assertTrue(success)
    verify(mockMessaging).token
    verify(mockDocumentReference).update("fcmToken", fcmToken)
  }

  @Test
  fun `initializeForCurrentUser returns false when token is null`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("user123")
    whenever(mockMessaging.token).thenReturn(Tasks.forException(Exception("No token")))

    val success = tokenManager.initializeForCurrentUser()

    assertFalse(success)
  }

  @Test
  fun `saveTokenForCurrentUser handles null token gracefully`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("user123")
    whenever(mockMessaging.token).thenReturn(Tasks.forException(Exception("No token")))

    val success = tokenManager.saveTokenForCurrentUser()

    assertFalse(success)
  }

  @Test
  fun `saveTokenForCurrentUser returns false when both update and set fail`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_token_fail"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any()))
        .thenReturn(Tasks.forException(Exception("Update failed")))
    whenever(mockDocumentReference.set(any<Map<String, String>>(), any<SetOptions>()))
        .thenReturn(Tasks.forException(Exception("Set failed")))

    val success = tokenManager.saveTokenForCurrentUser()

    assertFalse(success)
  }

  @Test
  fun `addTokenForCurrentUser uses set with merge when update fails`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_token_add_fail"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any()))
        .thenReturn(Tasks.forException(Exception("Update failed")))
    whenever(mockDocumentReference.set(any<Map<String, List<String>>>(), any<SetOptions>()))
        .thenReturn(Tasks.forResult(null))

    val success = tokenManager.addTokenForCurrentUser()

    assertTrue(success)
    verify(mockDocumentReference).set(any<Map<String, List<String>>>(), any<SetOptions>())
  }

  @Test
  fun `addTokenForCurrentUser returns false when both update and set fail`() = runTest {
    val userId = "user123"
    val fcmToken = "fcm_token_add_both_fail"

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(userId)
    whenever(mockMessaging.token).thenReturn(Tasks.forResult(fcmToken))
    whenever(mockDocumentReference.update(anyString(), any()))
        .thenReturn(Tasks.forException(Exception("Update failed")))
    whenever(mockDocumentReference.set(any<Map<String, List<String>>>(), any<SetOptions>()))
        .thenReturn(Tasks.forException(Exception("Set failed")))

    val success = tokenManager.addTokenForCurrentUser()

    assertFalse(success)
  }
}

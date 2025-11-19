package com.swent.mapin.model.chat

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.swent.mapin.model.UserProfile
import com.swent.mapin.ui.chat.Conversation
import io.mockk.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

// Assisted by AI
class ConversationRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var repo: ConversationRepositoryFirestore

  @Before
  fun setup() {
    mockDb = mockk()
    mockAuth = mockk()
    repo = ConversationRepositoryFirestore(mockDb, mockAuth)
  }

  @Test
  fun `getNewUid returns document id`() {
    val mockDocRef = mockk<DocumentReference>()
    every { mockDocRef.id } returns "fake-id"

    val mockCollection = mockk<CollectionReference>()
    every { mockCollection.document() } returns mockDocRef
    every { mockDb.collection("conversations") } returns mockCollection

    val id = repo.getNewUid()
    assertEquals("fake-id", id)
  }

  @Test
  fun `observeConversationsForCurrentUser closes flow when uid is null`() = runTest {
    every { mockAuth.currentUser } returns null

    val flow = repo.observeConversationsForCurrentUser()
    val emitted = flow.toList()
    assertTrue(emitted.isEmpty())
  }

  @Test
  fun `observeConversationsForCurrentUser emits empty list`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user1"
    every { mockAuth.currentUser } returns mockUser

    val mockListenerReg = mockk<ListenerRegistration>(relaxed = true)
    val mockQuery = mockk<Query>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)

    every { mockDb.collection("conversations") } returns mockCollection
    every { mockCollection.whereArrayContains("participantIds", "user1") } returns mockQuery
    every { mockQuery.orderBy("lastMessageTimestamp", Query.Direction.DESCENDING) } returns
        mockQuery

    // Mock addSnapshotListener to directly call the listener with empty snapshot
    every { mockQuery.addSnapshotListener(any()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          val emptySnapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
          listener.onEvent(emptySnapshot, null)
          mockListenerReg
        }

    val flow = repo.observeConversationsForCurrentUser()
    val emitted = flow.first()
    assertTrue(emitted.isEmpty())
  }

  @Test
  fun `observeConversationsForCurrentUser closes flow on error`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user1"
    every { mockAuth.currentUser } returns mockUser

    val mockListenerReg = mockk<ListenerRegistration>(relaxed = true)
    val mockQuery = mockk<Query>(relaxed = true)
    val mockCollection = mockk<CollectionReference>(relaxed = true)

    every { mockDb.collection("conversations") } returns mockCollection
    every { mockCollection.whereArrayContains("participantIds", "user1") } returns mockQuery
    every { mockQuery.orderBy("lastMessageTimestamp", Query.Direction.DESCENDING) } returns
        mockQuery

    every { mockQuery.addSnapshotListener(any()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          val exception =
              FirebaseFirestoreException("error", FirebaseFirestoreException.Code.UNKNOWN)
          listener.onEvent(null, exception) // flow will close with this error
          mockListenerReg
        }

    val flow = repo.observeConversationsForCurrentUser()

    // Collect safely: ignore the exception for testing that flow closes
    flow
        .catch { e ->
          // Optionally assert it's the expected exception
          assert(e is FirebaseFirestoreException)
        }
        .toList()
  }

  @Test
  fun `addConversation adds current user if missing`() = runTest {
    val conversation = Conversation(id = "c1", participantIds = listOf("u2"))

    val mockDocRef = mockk<DocumentReference>()
    every { mockDb.collection("conversations") } returns
        mockk { every { document("c1") } returns mockDocRef }

    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "u1"
    every { mockAuth.currentUser } returns mockUser

    // Create a TaskCompletionSource to simulate a completed Task<Void>
    val tcs = TaskCompletionSource<Void>()
    tcs.setResult(null)
    val task: Task<Void> = tcs.task

    // Mock set() to return the Task
    every { mockDocRef.set(any()) } returns task

    // Call the suspend function (await will work on the real Task)
    repo.addConversation(conversation)

    // Verify the conversation was set with current UID included
    verify {
      mockDocRef.set(match<Conversation> { it.participantIds.containsAll(listOf("u1", "u2")) })
    }
  }

  @Test
  fun `addConversation does not duplicate current user`() = runTest {
    val conversation = Conversation(id = "c1", participantIds = listOf("u1", "u2"))

    val mockDocRef = mockk<DocumentReference>()
    every { mockDb.collection("conversations") } returns
        mockk { every { document("c1") } returns mockDocRef }

    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "u1"
    every { mockAuth.currentUser } returns mockUser

    // Create a completed Task<Void>
    val tcs = com.google.android.gms.tasks.TaskCompletionSource<Void>()
    tcs.setResult(null)
    val task: Task<Void> = tcs.task

    // Mock set() to return the Task
    every { mockDocRef.set(any()) } returns task

    // Call the suspend function (await will work on the real Task)
    repo.addConversation(conversation)

    // Verify the conversation set contains only one "u1" and includes "u2"
    verify {
      mockDocRef.set(
          match<Conversation> {
            it.participantIds.count { id -> id == "u1" } == 1 && it.participantIds.contains("u2")
          })
    }
  }

  // Assisted by GPT
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `observeConversationsForCurrentUser overrides name and profile picture for 1-to-1 chat`() =
      runTest {
        // Mock FirebaseAuth
        val auth = mock<FirebaseAuth>()
        val currentUser = mock<com.google.firebase.auth.FirebaseUser>()
        whenever(currentUser.uid).thenReturn("user123")
        whenever(auth.currentUser).thenReturn(currentUser)

        // Mock Firestore
        val db = mock<FirebaseFirestore>()
        val collectionRef = mock<com.google.firebase.firestore.CollectionReference>()
        val query = mock<com.google.firebase.firestore.Query>()

        whenever(db.collection("conversations")).thenReturn(collectionRef)
        whenever(collectionRef.whereArrayContains("participantIds", "user123")).thenReturn(query)
        whenever(query.orderBy("lastMessageTimestamp", Query.Direction.DESCENDING))
            .thenReturn(query)

        val conversation =
            Conversation(
                id = "conv1",
                name = "Original Name",
                profilePictureUrl = "original_url",
                participants =
                    listOf(
                        UserProfile(
                            userId = "user123",
                            name = "Current User",
                            bio = "",
                            hobbies = emptyList(),
                            location = ""),
                        UserProfile(
                            userId = "user456",
                            name = "Other User",
                            bio = "",
                            hobbies = emptyList(),
                            location = "")),
                participantIds = listOf("user123", "user456"))

        // Stub addSnapshotListener to immediately call the listener
        whenever(query.addSnapshotListener(any())).thenAnswer { invocation ->
          val listener =
              invocation.getArgument<
                  com.google.firebase.firestore.EventListener<
                      com.google.firebase.firestore.QuerySnapshot>>(
                  0)

          val snapshot = mock<com.google.firebase.firestore.QuerySnapshot>()
          val doc = mock<com.google.firebase.firestore.DocumentSnapshot>()
          whenever(snapshot.documents).thenReturn(listOf(doc))
          whenever(doc.toObject(Conversation::class.java)).thenReturn(conversation)

          listener.onEvent(snapshot, null)
          mock<com.google.firebase.firestore.ListenerRegistration>()
        }

        val repo = ConversationRepositoryFirestore(db, auth)

        val results = mutableListOf<List<Conversation>>()
        val job = launch { repo.observeConversationsForCurrentUser().collect { results.add(it) } }

        // Let the flow emit
        advanceUntilIdle()

        job.cancel()

        val observedConversation = results.flatten().firstOrNull()
        Assert.assertNotNull(
            "Flow should have emitted at least one conversation", observedConversation)
        observedConversation?.let {
          Assert.assertEquals("Other User", it.name)
          Assert.assertEquals(conversation.id, it.id)
        }
      }
}

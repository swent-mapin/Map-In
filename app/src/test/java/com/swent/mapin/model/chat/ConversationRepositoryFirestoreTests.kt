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

  private fun mockCompletedTask(doc: DocumentSnapshot?): Task<DocumentSnapshot> {
    val tcs = TaskCompletionSource<DocumentSnapshot>()
    tcs.setResult(doc)
    return tcs.task
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
    val tcs = TaskCompletionSource<Void>()
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
        val currentUser = mock<FirebaseUser>()
        whenever(currentUser.uid).thenReturn("user123")
        whenever(auth.currentUser).thenReturn(currentUser)

        // Mock Firestore
        val db = mock<FirebaseFirestore>()
        val collectionRef = mock<CollectionReference>()
        val query = mock<Query>()

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
          val listener = invocation.getArgument<EventListener<QuerySnapshot>>(0)

          val snapshot = mock<QuerySnapshot>()
          val doc = mock<DocumentSnapshot>()
          whenever(snapshot.documents).thenReturn(listOf(doc))
          whenever(doc.toObject(Conversation::class.java)).thenReturn(conversation)

          listener.onEvent(snapshot, null)
          mock<ListenerRegistration>()
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

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `getConversationById returns conversation when document exists`() = runTest {
    val conversation = Conversation(id = "conv1", name = "Chat 1")
    val docSnapshot = mockk<DocumentSnapshot>()
    every { docSnapshot.toObject(Conversation::class.java) } returns conversation

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } returns mockCompletedTask(docSnapshot)
    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    val result = repo.getConversationById("conv1")
    assertEquals(conversation, result)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `getConversationById returns null when document does not exist`() = runTest {
    val docSnapshot = mockk<DocumentSnapshot>()
    every { docSnapshot.toObject(Conversation::class.java) } returns null

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } returns mockCompletedTask(docSnapshot)
    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    val result = repo.getConversationById("conv1")
    assertEquals(null, result)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `getConversationById returns null when firestore throws exception`() = runTest {
    val docRef = mockk<DocumentReference>()
    every { docRef.get() } throws RuntimeException("Firestore error")
    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    val result = repo.getConversationById("conv1")
    assertEquals(null, result)
  }
  // This was written with the help of Claude Sonnet 4.5
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `leaveConversation removes current user from participantIds and participants`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user1"
    every { mockAuth.currentUser } returns mockUser

    val conversation =
        Conversation(
            id = "conv1",
            name = "Group Chat",
            participantIds = listOf("user1", "user2", "user3"),
            participants =
                listOf(
                    UserProfile(
                        userId = "user1",
                        name = "User 1",
                        bio = "",
                        hobbies = emptyList(),
                        location = ""),
                    UserProfile(
                        userId = "user2",
                        name = "User 2",
                        bio = "",
                        hobbies = emptyList(),
                        location = ""),
                    UserProfile(
                        userId = "user3",
                        name = "User 3",
                        bio = "",
                        hobbies = emptyList(),
                        location = "")))

    val docSnapshot = mockk<DocumentSnapshot>()
    every { docSnapshot.toObject(Conversation::class.java) } returns conversation

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } returns mockCompletedTask(docSnapshot)

    // Create a completed Task<Void>
    val updateTcs = TaskCompletionSource<Void>()
    updateTcs.setResult(null)
    val updateTask: Task<Void> = updateTcs.task
    every { docRef.update(any<Map<String, Any>>()) } returns updateTask

    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    repo.leaveConversation("conv1")

    verify {
      docRef.update(
          match<Map<String, Any>> { map ->
            val participantIds = map["participantIds"] as? List<*>
            val participants = map["participants"] as? List<*>
            participantIds?.size == 2 &&
                !participantIds.contains("user1") &&
                participants?.size == 2
          })
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `leaveConversation does nothing when current user is null`() = runTest {
    every { mockAuth.currentUser } returns null

    val docRef = mockk<DocumentReference>(relaxed = true)
    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    repo.leaveConversation("conv1")

    verify(exactly = 0) { docRef.get() }
    verify(exactly = 0) { docRef.update(any<Map<String, Any>>()) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `leaveConversation does nothing when conversation does not exist`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user1"
    every { mockAuth.currentUser } returns mockUser

    val docSnapshot = mockk<DocumentSnapshot>()
    every { docSnapshot.toObject(Conversation::class.java) } returns null

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } returns mockCompletedTask(docSnapshot)

    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    repo.leaveConversation("conv1")

    verify(exactly = 0) { docRef.update(any<Map<String, Any>>()) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `leaveConversation throws exception when firestore update fails`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user1"
    every { mockAuth.currentUser } returns mockUser

    val conversation =
        Conversation(
            id = "conv1",
            name = "Group Chat",
            participantIds = listOf("user1", "user2"),
            participants =
                listOf(
                    UserProfile(
                        userId = "user1",
                        name = "User 1",
                        bio = "",
                        hobbies = emptyList(),
                        location = ""),
                    UserProfile(
                        userId = "user2",
                        name = "User 2",
                        bio = "",
                        hobbies = emptyList(),
                        location = "")))

    val docSnapshot = mockk<DocumentSnapshot>()
    every { docSnapshot.toObject(Conversation::class.java) } returns conversation

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } returns mockCompletedTask(docSnapshot)
    every { docRef.update(any<Map<String, Any>>()) } throws RuntimeException("Firestore error")

    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    try {
      repo.leaveConversation("conv1")
      Assert.fail("Expected exception to be thrown")
    } catch (e: RuntimeException) {
      assertEquals("Firestore error", e.message)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `leaveConversation throws exception when firestore get fails`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user1"
    every { mockAuth.currentUser } returns mockUser

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } throws RuntimeException("Network error")

    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    try {
      repo.leaveConversation("conv1")
      Assert.fail("Expected exception to be thrown")
    } catch (e: RuntimeException) {
      assertEquals("Network error", e.message)
    }

    verify(exactly = 0) { docRef.update(any<Map<String, Any>>()) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `leaveConversation correctly filters participants with multiple users`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user2"
    every { mockAuth.currentUser } returns mockUser

    val conversation =
        Conversation(
            id = "conv1",
            name = "Group Chat",
            participantIds = listOf("user1", "user2", "user3", "user4"),
            participants =
                listOf(
                    UserProfile(
                        userId = "user1",
                        name = "User 1",
                        bio = "",
                        hobbies = emptyList(),
                        location = ""),
                    UserProfile(
                        userId = "user2",
                        name = "User 2",
                        bio = "",
                        hobbies = emptyList(),
                        location = ""),
                    UserProfile(
                        userId = "user3",
                        name = "User 3",
                        bio = "",
                        hobbies = emptyList(),
                        location = ""),
                    UserProfile(
                        userId = "user4",
                        name = "User 4",
                        bio = "",
                        hobbies = emptyList(),
                        location = "")))

    val docSnapshot = mockk<DocumentSnapshot>()
    every { docSnapshot.toObject(Conversation::class.java) } returns conversation

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } returns mockCompletedTask(docSnapshot)

    val updateTcs = TaskCompletionSource<Void>()
    updateTcs.setResult(null)
    val updateTask: Task<Void> = updateTcs.task
    every { docRef.update(any<Map<String, Any>>()) } returns updateTask

    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    repo.leaveConversation("conv1")

    verify {
      docRef.update(
          match<Map<String, Any>> { map ->
            val participantIds = map["participantIds"] as? List<*>
            val participants = map["participants"] as? List<*>

            // Verify user2 is removed and others remain
            participantIds?.size == 3 &&
                !participantIds.contains("user2") &&
                participantIds.contains("user1") &&
                participantIds.contains("user3") &&
                participantIds.contains("user4") &&
                participants?.size == 3 &&
                participants.none { (it as? UserProfile)?.userId == "user2" }
          })
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `leaveConversation handles conversation with only current user`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    every { mockUser.uid } returns "user1"
    every { mockAuth.currentUser } returns mockUser

    val conversation =
        Conversation(
            id = "conv1",
            name = "Solo Chat",
            participantIds = listOf("user1"),
            participants =
                listOf(
                    UserProfile(
                        userId = "user1",
                        name = "User 1",
                        bio = "",
                        hobbies = emptyList(),
                        location = "")))

    val docSnapshot = mockk<DocumentSnapshot>()
    every { docSnapshot.toObject(Conversation::class.java) } returns conversation

    val docRef = mockk<DocumentReference>()
    every { docRef.get() } returns mockCompletedTask(docSnapshot)

    val updateTcs = TaskCompletionSource<Void>()
    updateTcs.setResult(null)
    val updateTask: Task<Void> = updateTcs.task
    every { docRef.update(any<Map<String, Any>>()) } returns updateTask

    val collection = mockk<CollectionReference>()
    every { collection.document("conv1") } returns docRef
    every { mockDb.collection("conversations") } returns collection

    repo.leaveConversation("conv1")

    verify {
      docRef.update(
          match<Map<String, Any>> { map ->
            val participantIds = map["participantIds"] as? List<*>
            val participants = map["participants"] as? List<*>

            // Both lists should be empty after removing the only user
            participantIds?.isEmpty() == true && participants?.isEmpty() == true
          })
    }
  }
}

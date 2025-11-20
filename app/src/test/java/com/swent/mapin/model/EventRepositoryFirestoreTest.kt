package com.swent.mapin.model

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryFirestore
import com.swent.mapin.model.event.FirestoreSchema.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.USERS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.UserFields.JOINED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.OWNED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.SAVED_EVENT_IDS
import com.swent.mapin.ui.filters.Filters
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var repo: EventRepositoryFirestore
  private lateinit var document: DocumentReference
  private lateinit var collection: CollectionReference
  private lateinit var usersCollection: CollectionReference
  private lateinit var userDocRef: DocumentReference
  private lateinit var friendRepo: FriendRequestRepository
  private lateinit var transaction: Transaction

  @Before
  fun setup() {
    db = mock()
    friendRepo = mock()
    repo = EventRepositoryFirestore(db, friendRequestRepository = friendRepo)
    document = mock()
    transaction = mock()
    collection =
        mock<CollectionReference>().apply {
          whenever(this.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
              .thenReturn(mock())
          whenever(this.orderBy(any<String>())).thenReturn(mock())
          whenever(this.orderBy(any<String>(), any<Query.Direction>())).thenReturn(mock())
        }
    usersCollection = mock()
    userDocRef = mock()

    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(collection)
    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(collection.document()).thenReturn(document)
    whenever(collection.document(any())).thenReturn(document)
    whenever(usersCollection.document(any())).thenReturn(userDocRef)
  }

  // Helper methods
  private fun createEvent(
      uid: String = "",
      title: String = "Event",
      url: String = "",
      description: String = "Description",
      date: Timestamp = Timestamp.now(),
      location: Location = Location("Location", 0.0, 0.0),
      tags: List<String> = emptyList(),
      public: Boolean = true,
      ownerId: String = "owner",
      imageUrl: String? = null,
      capacity: Int = 10,
      participantIds: List<String> = emptyList(),
      price: Double = 0.0
  ): Event {
    return Event(
        uid = uid,
        title = title,
        url = url,
        description = description,
        date = date,
        location = location,
        tags = tags,
        public = public,
        ownerId = ownerId,
        imageUrl = imageUrl,
        capacity = capacity,
        participantIds = participantIds,
        price = price)
  }

  private fun doc(id: String, event: Event? = null): QueryDocumentSnapshot {
    val snapshot = mock<QueryDocumentSnapshot>()
    whenever(snapshot.id).thenReturn(id)
    whenever(snapshot.toObject(Event::class.java)).thenReturn(event)
    whenever(snapshot.exists()).thenReturn(event != null)
    return snapshot
  }

  private fun userDoc(id: String, userProfile: UserProfile? = null): DocumentSnapshot {
    val snapshot = mock<DocumentSnapshot>()
    whenever(snapshot.id).thenReturn(id)
    whenever(snapshot.toObject(UserProfile::class.java)).thenReturn(userProfile)
    whenever(snapshot.get(SAVED_EVENT_IDS))
        .thenReturn(userProfile?.savedEventIds ?: emptyList<String>())
    whenever(snapshot.get(JOINED_EVENT_IDS))
        .thenReturn(userProfile?.joinedEventIds ?: emptyList<String>())
    whenever(snapshot.get(OWNED_EVENT_IDS))
        .thenReturn(userProfile?.ownedEventIds ?: emptyList<String>())
    whenever(snapshot.exists()).thenReturn(userProfile != null)
    return snapshot
  }

  private fun qs(vararg docs: DocumentSnapshot): QuerySnapshot {
    val snapshot = mock<QuerySnapshot>()
    whenever(snapshot.documents).thenReturn(docs.toList())
    return snapshot
  }

  private fun <T> taskOf(value: T): Task<T> = Tasks.forResult(value)

  private fun voidTask(): Task<Void> = Tasks.forResult(null)

  // ========== BASIC CRUD TESTS ==========

  @Test
  fun getNewUid_returnsUniqueId() {
    whenever(document.id).thenReturn("RANDOM_ID")
    val id = repo.getNewUid()
    assertEquals("RANDOM_ID", id)
  }

  @Test
  fun getEvent_success_returnsMappedEvent() = runTest {
    val event =
        createEvent(
            uid = "E1",
            title = "Title",
            location = Location("Zurich", 47.3769, 8.5417),
            tags = listOf("music"),
            capacity = 50)
    val docSnapshot = doc("E1", event)
    whenever(document.get()).thenReturn(taskOf(docSnapshot))

    val result = repo.getEvent("E1")
    assertEquals("E1", result.uid)
    assertEquals("Title", result.title)
  }

  @Test
  fun getEvent_throwsException_whenEventNotFound() = runTest {
    val docSnapshot = mock<DocumentSnapshot>()
    whenever(docSnapshot.exists()).thenReturn(false)
    whenever(document.get()).thenReturn(Tasks.forResult(docSnapshot))

    val exception = assertFailsWith<Exception> { repo.getEvent("MISSING") }
    assertTrue(exception.message!!.contains("Event not found"))
  }

  @Test
  fun getEvent_catchesFirestoreException_andWrapsIt() = runTest {
    whenever(document.get()).thenReturn(Tasks.forException(RuntimeException("firestore broken")))

    val exception = assertFailsWith<Exception> { repo.getEvent("E1") }
    assertTrue(exception.message!!.contains("Failed to fetch event"))
  }

  // ========== ADD EVENT TESTS ==========

  @Test
  fun addEvent_success_createsEventAndUpdatesOwner() = runTest {
    val input =
        createEvent(
            uid = "", title = "New Event", ownerId = "owner123", participantIds = emptyList())

    val newEventDocRef = mock<DocumentReference>()
    whenever(collection.document()).thenReturn(newEventDocRef)
    whenever(newEventDocRef.id).thenReturn("E123")

    whenever(db.runBatch(any())).thenReturn(voidTask())

    repo.addEvent(input)

    verify(db).runBatch(any())
    verify(collection).document()
  }

  @Test
  fun addEvent_throwsException_whenInvalidEvent() = runTest {
    val invalidEvent = createEvent(title = "", ownerId = "")
    assertFailsWith<IllegalArgumentException> { repo.addEvent(invalidEvent) }
  }

  @Test
  fun addEvent_throwsException_whenParticipantsNotEmpty() = runTest {
    val invalidEvent = createEvent(ownerId = "owner123", participantIds = listOf("user1"))
    assertFailsWith<IllegalArgumentException> { repo.addEvent(invalidEvent) }
  }

  @Test
  fun addEvent_catchesFirestoreError() = runTest {
    val event = createEvent(ownerId = "owner123", participantIds = emptyList())
    whenever(collection.document()).thenReturn(document)
    whenever(document.id).thenReturn("E123")
    whenever(db.runBatch(any())).thenReturn(Tasks.forException(RuntimeException("boom")))

    val exception = assertFailsWith<Exception> { repo.addEvent(event) }
    assertTrue(exception.message!!.contains("Failed to add event"))
  }

  // ========== EDIT EVENT AS OWNER TESTS ==========

  @Test
  fun editEventAsOwner_success_updatesEvent() = runTest {
    val existing =
        createEvent(uid = "E1", title = "Old", ownerId = "owner", participantIds = listOf("user1"))
    val updated = existing.copy(title = "Updated", description = "New desc")
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.set(any<DocumentReference>(), any<Event>())).thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsOwner("E1", updated)

    verify(transaction).set(any<DocumentReference>(), any<Event>())
  }

  @Test
  fun editEventAsOwner_throwsException_whenEventNotFound() = runTest {
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      val missingSnapshot = mock<DocumentSnapshot>()
      whenever(missingSnapshot.exists()).thenReturn(false)
      whenever(transaction.get(eventDocRef)).thenReturn(missingSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val updated = createEvent(uid = "E1", ownerId = "owner")
    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Event not found"))
  }

  @Test
  fun editEventAsOwner_throwsException_whenOwnerMismatch() = runTest {
    val existing = createEvent(uid = "E1", ownerId = "owner1")
    val updated = existing.copy(ownerId = "owner2")
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Only the owner can call editEventAsOwner"))
  }

  @Test
  fun editEventAsOwner_throwsException_whenChangingParticipants() = runTest {
    val existing = createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1"))
    val updated = existing.copy(participantIds = listOf("user1", "user2"))
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Owner cannot change participants list"))
  }

  @Test
  fun editEventAsOwner_throwsException_whenReducingCapacityBelowParticipants() = runTest {
    val existing =
        createEvent(
            uid = "E1", ownerId = "owner", capacity = 10, participantIds = listOf("u1", "u2", "u3"))
    val updated = existing.copy(capacity = 2)
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Capacity cannot be lower than current participants"))
  }

  // ========== EDIT EVENT AS USER TESTS ==========

  @Test
  fun editEventAsUser_join_success() = runTest {
    val event =
        createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1"), capacity = 10)
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user2", join = true)

    verify(transaction, times(2)).update(any<DocumentReference>(), any<String>(), any())
  }

  @Test
  fun editEventAsUser_join_throwsException_whenEventFull() = runTest {
    val event =
        createEvent(
            uid = "E1", ownerId = "owner", participantIds = listOf("u1", "u2"), capacity = 2)
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsUser("E1", "user3", join = true) }
    assertTrue(exception.message!!.contains("Event is full"))
  }

  @Test
  fun editEventAsUser_leave_success() = runTest {
    val event =
        createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1", "user2"))
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user2", join = false)

    verify(transaction, times(2)).update(any<DocumentReference>(), any<String>(), any())
  }

  // ========== DELETE EVENT TESTS ==========

  @Test
  fun deleteEvent_success_removesEventAndUpdatesUsers() = runTest {
    val event = createEvent(uid = "E1", ownerId = "owner123", participantIds = listOf("user1"))
    val eventDocRef = mock<DocumentReference>()
    val eventSnapshot = doc("E1", event)

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(eventDocRef.get()).thenReturn(taskOf(eventSnapshot))
    whenever(db.runBatch(any())).thenReturn(voidTask())

    // Mock savedEventIds query
    val savedQuery = mock<Query>()
    val emptySnapshot = qs()
    whenever(usersCollection.whereArrayContains(SAVED_EVENT_IDS, "E1")).thenReturn(savedQuery)
    whenever(savedQuery.get()).thenReturn(taskOf(emptySnapshot))

    repo.deleteEvent("E1")

    verify(db, atLeastOnce()).runBatch(any())
    verify(eventDocRef, never()).delete() // Delete is inside the batch
    verify(savedQuery).get()
  }

  @Test
  fun deleteEvent_eventNotFound_throwsException() = runTest {
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E404")).thenReturn(eventDocRef)
    val missingSnapshot = mock<DocumentSnapshot>()
    whenever(missingSnapshot.exists()).thenReturn(false)
    whenever(eventDocRef.get()).thenReturn(Tasks.forResult(missingSnapshot))

    val exception = assertFailsWith<Exception> { repo.deleteEvent("E404") }
    assertTrue(exception.message!!.contains("Event not found"))
  }

  // ========== GET USER EVENTS TESTS ==========

  @Test
  fun getSavedEvents_returnsSortedList() = runTest {
    val userProfile = UserProfile(userId = "user123", savedEventIds = listOf("E1", "E2"))
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val e1 = createEvent(uid = "E1", title = "First")
    val e2 = createEvent(uid = "E2", title = "Second")
    val doc1 = doc("E1", e1)
    val doc2 = doc("E2", e2)
    val eventSnapshot = qs(doc1, doc2)

    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)
    whenever(eventQuery.get()).thenReturn(taskOf(eventSnapshot))

    val result = repo.getSavedEvents("user123")

    assertEquals(2, result.size)
  }

  @Test
  fun getJoinedEvents_returnsSortedList() = runTest {
    val userProfile = UserProfile(userId = "user123", joinedEventIds = listOf("E1"))
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val e1 = createEvent(uid = "E1", title = "Event")
    val doc1 = doc("E1", e1)
    val eventSnapshot = qs(doc1)

    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)
    whenever(eventQuery.get()).thenReturn(taskOf(eventSnapshot))

    val result = repo.getJoinedEvents("user123")

    assertEquals(1, result.size)
    assertEquals("Event", result.first().title)
  }

  @Test
  fun getOwnedEvents_returnsSortedList() = runTest {
    val userProfile = UserProfile(userId = "owner123", ownedEventIds = listOf("E1"))
    val userSnapshot = userDoc("owner123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("owner123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))
    whenever(userSnapshot.get(any<String>())).thenReturn(listOf("E1"))

    val e1 = createEvent(uid = "E1", title = "My Event", ownerId = "owner123")
    val doc1 = doc("E1", e1)
    val eventSnapshot = qs(doc1)

    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)
    whenever(eventQuery.get()).thenReturn(taskOf(eventSnapshot))

    val result = repo.getOwnedEvents("owner123")

    assertEquals(1, result.size)
    assertEquals("My Event", result.first().title)
  }

  @Test
  fun getSavedEvents_returnsEmptyList_whenUserHasNoSavedEvents() = runTest {
    val userProfile = UserProfile(userId = "user123", savedEventIds = emptyList())
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val result = repo.getSavedEvents("user123")

    assertTrue(result.isEmpty())
  }

  @Test
  fun getJoinedEvents_returnsEmptyList_whenUserHasNoJoinedEvents() = runTest {
    val userProfile = UserProfile(userId = "user123", joinedEventIds = emptyList())
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val result = repo.getJoinedEvents("user123")

    assertTrue(result.isEmpty())
  }

  @Test
  fun getOwnedEvents_returnsEmptyList_whenUserHasNoOwnedEvents() = runTest {
    val userProfile = UserProfile(userId = "owner123", ownedEventIds = emptyList())
    val userSnapshot = userDoc("owner123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("owner123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val result = repo.getOwnedEvents("owner123")

    assertTrue(result.isEmpty())
  }

  // ========== GET FILTERED EVENTS BASIC TESTS ==========

  @Test
  fun getFilteredEvents_appliesBasicDateFilters() = runTest {
    val filters =
        Filters(startDate = LocalDate.now().minusDays(1), endDate = LocalDate.now().plusDays(1))
    val event = createEvent(uid = "E1", title = "Event")
    val doc = doc("E1", event)
    val snap = qs(doc)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)

    assertTrue(result.isNotEmpty())
    assertEquals("Event", result.first().title)
  }

  @Test
  fun getFilteredEvents_throwsException_whenQueryFails() = runTest {
    val query = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(query)
    whenever(query.orderBy(any<String>(), any<Query.Direction>())).thenReturn(query)
    whenever(query.get()).thenReturn(Tasks.forException(RuntimeException("broken")))

    val filters = Filters(startDate = LocalDate.now())
    val exception = assertFailsWith<Exception> { repo.getFilteredEvents(filters) }

    assertTrue(exception.message!!.contains("Failed to fetch filtered events"))
  }

  // ========== LISTEN TO FILTERED EVENTS BASIC TESTS ==========

  @Test
  fun listenToFilteredEvents_basicImplementation_triggersCallback() = runTest {
    val filters = Filters(startDate = LocalDate.now())

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)

    val listenerCaptor = argumentCaptor<EventListener<QuerySnapshot>>()
    val listenerRegistration = mock<ListenerRegistration>()
    whenever(queryMock.addSnapshotListener(listenerCaptor.capture()))
        .thenReturn(listenerRegistration)

    var addedEvents: List<Event> = emptyList()

    val registration = repo.listenToFilteredEvents(filters) { added, _, _ -> addedEvents = added }

    verify(queryMock).addSnapshotListener(any())

    val event1 = createEvent(uid = "E1", title = "Concert")
    val doc1 = doc("E1", event1)

    val snapshot = mock<QuerySnapshot>()
    whenever(snapshot.documents).thenReturn(listOf(doc1))

    listenerCaptor.firstValue.onEvent(snapshot, null)

    assertEquals(1, addedEvents.size)
    assertEquals("Concert", addedEvents.first().title)

    registration.remove()
    verify(listenerRegistration).remove()
  }

  @Test
  fun listenToFilteredEvents_onError_triggersCallbackWithEmptyLists() = runTest {
    val filters = Filters()

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)

    val listenerCaptor = argumentCaptor<EventListener<QuerySnapshot>>()
    val listenerRegistration = mock<ListenerRegistration>()
    whenever(queryMock.addSnapshotListener(listenerCaptor.capture()))
        .thenReturn(listenerRegistration)

    var addedEvents: List<Event>? = null
    var modifiedEvents: List<Event>? = null
    var removedEvents: List<Event>? = null

    repo.listenToFilteredEvents(filters) { added, modified, removed ->
      addedEvents = added
      modifiedEvents = modified
      removedEvents = removed
    }

    val error = mock<FirebaseFirestoreException>()
    listenerCaptor.firstValue.onEvent(null, error)

    assertTrue(addedEvents!!.isEmpty())
    assertTrue(modifiedEvents!!.isEmpty())
    assertTrue(removedEvents!!.isEmpty())
  }
}

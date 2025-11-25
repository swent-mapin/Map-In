package com.swent.mapin.model

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
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
  private lateinit var firebaseAuthMock: MockedStatic<FirebaseAuth>

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

  @Test
  fun getEvent_throwsException_whenConversionFails() = runTest {
    val docSnapshot = mock<DocumentSnapshot>()
    whenever(docSnapshot.exists()).thenReturn(true)
    whenever(docSnapshot.id).thenReturn("E1")
    whenever(docSnapshot.toObject(Event::class.java))
        .thenThrow(RuntimeException("Conversion error"))
    whenever(document.get()).thenReturn(taskOf(docSnapshot))

    val exception = assertFailsWith<Exception> { repo.getEvent("E1") }
    assertTrue(
        exception.message!!.contains("Failed to fetch event") ||
            exception.message!!.contains("Conversion error"))
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

  @Test
  fun deleteEvent_withManyParticipants_chunksCorrectly() = runTest {
    // Create event with 500 participants (requires chunking at 450)
    val participantIds = (1..500).map { "user$it" }
    val event = createEvent(uid = "E1", ownerId = "owner123", participantIds = participantIds)
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

    // Should call runBatch twice: once for first 450 participants + event deletion,
    // once for remaining 50 participants
    verify(db, times(2)).runBatch(any())
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

  @Test
  fun fetchEventsByIds_withMultipleChunks_mergesAndSorts() = runTest {
    // Create 25 event IDs (will be chunked into 3 groups: 10, 10, 5)
    val eventIds = (1..25).map { "E$it" }
    val userProfile = UserProfile(userId = "user123", savedEventIds = eventIds)
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    // Mock three separate query results
    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)

    // Create events with different dates for sorting test
    val events =
        eventIds.mapIndexed { index, id ->
          createEvent(uid = id, title = "Event$index", date = Timestamp(1000L + index, 0))
        }

    val docs = events.map { doc(it.uid, it) }
    val snap1 = qs(*docs.take(10).toTypedArray())
    val snap2 = qs(*docs.drop(10).take(10).toTypedArray())
    val snap3 = qs(*docs.drop(20).toTypedArray())

    val eventQuery1 = mock<Query>()
    val eventQuery2 = mock<Query>()
    val eventQuery3 = mock<Query>()

    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>()))
        .thenReturn(eventQuery1, eventQuery2, eventQuery3)

    whenever(eventQuery1.get()).thenReturn(taskOf(snap1))
    whenever(eventQuery2.get()).thenReturn(taskOf(snap2))
    whenever(eventQuery3.get()).thenReturn(taskOf(snap3))

    val result = repo.getSavedEvents("user123")

    assertEquals(25, result.size)
    // Verify events are sorted by date (descending)
    for (i in 0 until result.size - 1) {
      assertTrue(
          (result[i].date?.seconds ?: Long.MIN_VALUE) >=
              (result[i + 1].date?.seconds ?: Long.MIN_VALUE))
    }
  }

  // ========== GET FILTERED EVENTS TESTS ==========

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
  fun getFilteredEvents_appliesPopularOnlyFilter() = runTest {
    val filters = Filters(popularOnly = true)
    val popular = createEvent("E1", "Popular", participantIds = List(35) { "user$it" })
    val unpopular = createEvent("E2", "Unpopular", participantIds = listOf("user1"))
    val doc1 = doc("E1", popular)
    val doc2 = doc("E2", unpopular)
    val snap = qs(doc1, doc2)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)

    // Only popular event should be returned (>30 participants)
    assertEquals(1, result.size)
    assertEquals("Popular", result.first().title)
    assertTrue(result.first().participantIds.size > 30)
  }

  @Test
  fun getFilteredEvents_tagFilter_skipsEventsWithoutTags() = runTest {
    val filters = Filters(tags = setOf("art"))

    val taggedEvent = createEvent("E1", "Tagged", tags = listOf("art"))
    val untaggedEvent = createEvent("E2", "No Tags")
    val doc1 = doc("E1", taggedEvent)
    val doc2 = doc("E2", untaggedEvent)
    val snap = qs(doc1)

    val filteredQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(eq("date"), any())).thenReturn(filteredQuery)
    whenever(filteredQuery.whereArrayContainsAny(any<String>(), any<List<String>>()))
        .thenReturn(filteredQuery)
    whenever(filteredQuery.orderBy("date", Query.Direction.DESCENDING)).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)

    // Both events returned by query, but only tagged one should match
    assertEquals(1, result.size)
    assertTrue("art" in result.first().tags)
  }

  @Test
  fun getFilteredEvents_maxPrice_filtersCorrectly() = runTest {
    val filters = Filters(maxPrice = 50)

    val cheapEvent = createEvent("E1", "Cheap", price = 30.0)
    val expensiveEvent = createEvent("E2", "Expensive", price = 100.0)
    val doc1 = doc("E1", cheapEvent)
    val doc2 = doc("E2", expensiveEvent)
    val snap = qs(doc1, doc2)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(eq("price"), eq(50.0))).thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)

    // Should include both in mock, but in real scenario only cheap would be returned by query
    verify(queryMock).whereLessThanOrEqualTo("price", 50.0)
  }

  @Test
  fun getFilteredEvents_maxPriceWithTags_combinesFilters() = runTest {
    val filters = Filters(tags = setOf("music"), maxPrice = 50)

    val event = createEvent("E1", "Concert", tags = listOf("music"), price = 30.0)
    val doc1 = doc("E1", event)
    val snap = qs(doc1)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(eq("price"), any())).thenReturn(queryMock)
    whenever(queryMock.whereArrayContainsAny(any<String>(), any<List<String>>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)

    verify(queryMock).whereLessThanOrEqualTo("price", 50.0)
    verify(queryMock).whereArrayContainsAny("tags", listOf("music"))
    assertEquals(1, result.size)
  }

  @Test
  fun getFilteredEvents_friendsOnly_success() = runTest {
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)
    try {
      val mockAuth = mock<FirebaseAuth>()
      val mockUser = mock<FirebaseUser>()
      whenever(mockUser.uid).thenReturn("currentUser")
      whenever(mockAuth.currentUser).thenReturn(mockUser)
      firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

      val filters = Filters(friendsOnly = true)

      // Mock friends
      val friend1 =
          FriendWithProfile(
              userProfile = UserProfile(userId = "friend1", name = "Friend1"),
              friendshipStatus = FriendshipStatus.ACCEPTED)
      val friend2 =
          FriendWithProfile(
              userProfile = UserProfile(userId = "friend2", name = "Friend2"),
              friendshipStatus = FriendshipStatus.ACCEPTED)
      whenever(friendRepo.getFriends("currentUser")).thenReturn(listOf(friend1, friend2))

      val event1 = createEvent("E1", "Friend Event 1", ownerId = "friend1")
      val event2 = createEvent("E2", "Friend Event 2", ownerId = "friend2")
      val doc1 = doc("E1", event1)
      val doc2 = doc("E2", event2)
      val snap = qs(doc1, doc2)

      val queryMock = mock<Query>()
      whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
          .thenReturn(queryMock)
      whenever(queryMock.whereIn(eq("ownerId"), any<List<String>>())).thenReturn(queryMock)
      whenever(queryMock.get()).thenReturn(taskOf(snap))

      val result = repo.getFilteredEvents(filters)

      verify(friendRepo).getFriends("currentUser")
      verify(queryMock).whereIn(eq("ownerId"), any<List<String>>())
      assertEquals(2, result.size)
    } finally {
      firebaseAuthMock.close()
    }
  }

  @Test
  fun getFilteredEvents_friendsOnly_withNoFriends_returnsEmpty() = runTest {
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)
    try {
      val mockAuth = mock<FirebaseAuth>()
      val mockUser = mock<FirebaseUser>()
      whenever(mockUser.uid).thenReturn("currentUser")
      whenever(mockAuth.currentUser).thenReturn(mockUser)
      firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

      val filters = Filters(friendsOnly = true)

      // No friends
      whenever(friendRepo.getFriends("currentUser")).thenReturn(emptyList())

      val result = repo.getFilteredEvents(filters)

      verify(friendRepo).getFriends("currentUser")
      assertTrue(result.isEmpty())
    } finally {
      firebaseAuthMock.close()
    }
  }

  @Test
  fun getFilteredEvents_friendsOnly_withTags_appliesTagsClientSide() = runTest {
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)
    try {
      val mockAuth = mock<FirebaseAuth>()
      val mockUser = mock<FirebaseUser>()
      whenever(mockUser.uid).thenReturn("currentUser")
      whenever(mockAuth.currentUser).thenReturn(mockUser)
      firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

      val filters = Filters(friendsOnly = true, tags = setOf("music"))

      val friend1 =
          FriendWithProfile(
              userProfile = UserProfile(userId = "friend1", name = "Friend1"),
              friendshipStatus = FriendshipStatus.ACCEPTED)
      whenever(friendRepo.getFriends("currentUser")).thenReturn(listOf(friend1))

      val musicEvent = createEvent("E1", "Concert", ownerId = "friend1", tags = listOf("music"))
      val sportEvent = createEvent("E2", "Game", ownerId = "friend1", tags = listOf("sport"))
      val doc1 = doc("E1", musicEvent)
      val doc2 = doc("E2", sportEvent)
      val snap = qs(doc1, doc2)

      val queryMock = mock<Query>()
      whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
          .thenReturn(queryMock)
      whenever(queryMock.whereIn(eq("ownerId"), any<List<String>>())).thenReturn(queryMock)
      whenever(queryMock.get()).thenReturn(taskOf(snap))

      val result = repo.getFilteredEvents(filters)

      // Tags should be filtered CLIENT-SIDE (not in whereArrayContainsAny)
      verify(queryMock, never()).whereArrayContainsAny(any<String>(), any<List<String>>())
      assertEquals(1, result.size)
      assertEquals("Concert", result.first().title)
      assertTrue("music" in result.first().tags)
    } finally {
      firebaseAuthMock.close()
    }
  }

  @Test
  fun getFilteredEvents_friendsOnly_withManyFriends_chunksCorrectly() = runTest {
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)
    try {
      val mockAuth = mock<FirebaseAuth>()
      val mockUser = mock<FirebaseUser>()
      whenever(mockUser.uid).thenReturn("currentUser")
      whenever(mockAuth.currentUser).thenReturn(mockUser)
      firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

      val filters = Filters(friendsOnly = true)

      // Create 25 friends (will be chunked into 3 groups: 10, 10, 5)
      val friends =
          (1..25).map {
            FriendWithProfile(
                userProfile = UserProfile(userId = "friend$it", name = "Friend$it"),
                friendshipStatus = FriendshipStatus.ACCEPTED)
          }
      whenever(friendRepo.getFriends("currentUser")).thenReturn(friends)

      val events = (1..25).map { createEvent("E$it", "Event$it", ownerId = "friend$it") }
      val docs = events.map { doc(it.uid, it) }

      val snap1 = qs(*docs.take(10).toTypedArray())
      val snap2 = qs(*docs.drop(10).take(10).toTypedArray())
      val snap3 = qs(*docs.drop(20).toTypedArray())

      val baseQuery = mock<Query>()
      val query1 = mock<Query>()
      val query2 = mock<Query>()
      val query3 = mock<Query>()

      whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
          .thenReturn(baseQuery)
      whenever(baseQuery.whereIn(eq("ownerId"), any<List<String>>()))
          .thenReturn(query1, query2, query3)

      whenever(query1.get()).thenReturn(taskOf(snap1))
      whenever(query2.get()).thenReturn(taskOf(snap2))
      whenever(query3.get()).thenReturn(taskOf(snap3))

      val result = repo.getFilteredEvents(filters)

      // Should call whereIn 3 times (once per chunk)
      verify(baseQuery, times(3)).whereIn(eq("ownerId"), any<List<String>>())
      assertEquals(25, result.size)
    } finally {
      firebaseAuthMock.close()
    }
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

  @Test
  fun getFilteredEvents_complexCombination_friendsAndPopular() = runTest {
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)
    try {
      val mockAuth = mock<FirebaseAuth>()
      val mockUser = mock<FirebaseUser>()
      whenever(mockUser.uid).thenReturn("currentUser")
      whenever(mockAuth.currentUser).thenReturn(mockUser)
      firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

      val filters = Filters(friendsOnly = true, popularOnly = true)

      val friend1 =
          FriendWithProfile(
              userProfile = UserProfile(userId = "friend1", name = "Friend1"),
              friendshipStatus = FriendshipStatus.ACCEPTED)
      whenever(friendRepo.getFriends("currentUser")).thenReturn(listOf(friend1))

      val popularEvent =
          createEvent("E1", "Popular", ownerId = "friend1", participantIds = List(35) { "user$it" })
      val unpopularEvent =
          createEvent("E2", "Unpopular", ownerId = "friend1", participantIds = listOf("user1"))
      val doc1 = doc("E1", popularEvent)
      val doc2 = doc("E2", unpopularEvent)
      val snap = qs(doc1, doc2)

      val queryMock = mock<Query>()
      whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
          .thenReturn(queryMock)
      whenever(queryMock.whereIn(eq("ownerId"), any<List<String>>())).thenReturn(queryMock)
      whenever(queryMock.get()).thenReturn(taskOf(snap))

      val result = repo.getFilteredEvents(filters)

      // popularOnly is applied client-side
      assertEquals(1, result.size)
      assertEquals("Popular", result.first().title)
      assertTrue(result.first().participantIds.size > 30)
    } finally {
      firebaseAuthMock.close()
    }
  }

  @Test
  fun getFilteredEvents_allFilters_combinesCorrectly() = runTest {
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)
    try {
      val mockAuth = mock<FirebaseAuth>()
      val mockUser = mock<FirebaseUser>()
      whenever(mockUser.uid).thenReturn("currentUser")
      whenever(mockAuth.currentUser).thenReturn(mockUser)
      firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

      val filters =
          Filters(
              friendsOnly = true,
              tags = setOf("music"),
              popularOnly = true,
              maxPrice = 50,
              startDate = LocalDate.now(),
              endDate = LocalDate.now().plusDays(7))

      val friend1 =
          FriendWithProfile(
              userProfile = UserProfile(userId = "friend1", name = "Friend1"),
              friendshipStatus = FriendshipStatus.ACCEPTED)
      whenever(friendRepo.getFriends("currentUser")).thenReturn(listOf(friend1))

      val validEvent =
          createEvent(
              uid = "E1",
              title = "Valid Event",
              ownerId = "friend1",
              tags = listOf("music"),
              participantIds = List(35) { "user$it" },
              price = 30.0)

      val invalidEvent1 =
          createEvent(
              uid = "E2",
              title = "Wrong Tag",
              ownerId = "friend1",
              tags = listOf("sport"),
              participantIds = List(35) { "user$it" },
              price = 30.0)

      val invalidEvent2 =
          createEvent(
              uid = "E3",
              title = "Not Popular",
              ownerId = "friend1",
              tags = listOf("music"),
              participantIds = listOf("user1"),
              price = 30.0)

      val doc1 = doc("E1", validEvent)
      val doc2 = doc("E2", invalidEvent1)
      val doc3 = doc("E3", invalidEvent2)
      val snap = qs(doc1, doc2, doc3)

      val queryMock = mock<Query>()
      whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
          .thenReturn(queryMock)
      whenever(queryMock.whereLessThanOrEqualTo(eq("date"), any<Timestamp>())).thenReturn(queryMock)
      whenever(queryMock.whereLessThanOrEqualTo(eq("price"), any())).thenReturn(queryMock)
      whenever(queryMock.whereIn(eq("ownerId"), any<List<String>>())).thenReturn(queryMock)
      whenever(queryMock.get()).thenReturn(taskOf(snap))

      val result = repo.getFilteredEvents(filters)

      // Only validEvent should pass all filters
      assertEquals(1, result.size)
      assertEquals("Valid Event", result.first().title)
    } finally {
      firebaseAuthMock.close()
    }
  }

  // ========== LISTEN TO FILTERED EVENTS TESTS ==========

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

    val change1 = mock<DocumentChange>()
    whenever(change1.type).thenReturn(DocumentChange.Type.ADDED)
    whenever(change1.document).thenReturn(doc1)

    val snapshot = mock<QuerySnapshot>()
    whenever(snapshot.documentChanges).thenReturn(listOf(change1))

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

  @Test
  fun listenToFilteredEvents_appliesClientSideFilters() = runTest {
    val filters =
        Filters(
            tags = setOf("music"), popularOnly = true // Client-side filter
            )

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereArrayContainsAny(any<String>(), any<List<String>>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)

    val listenerCaptor = argumentCaptor<EventListener<QuerySnapshot>>()
    val listenerRegistration = mock<ListenerRegistration>()
    whenever(queryMock.addSnapshotListener(listenerCaptor.capture()))
        .thenReturn(listenerRegistration)

    var addedEvents: List<Event> = emptyList()

    repo.listenToFilteredEvents(filters) { added, _, _ -> addedEvents = added }

    val popularEvent =
        createEvent(
            uid = "E1",
            title = "Popular Concert",
            tags = listOf("music"),
            participantIds = List(35) { "user$it" })

    val unpopularEvent =
        createEvent(
            uid = "E2",
            title = "Small Concert",
            tags = listOf("music"),
            participantIds = listOf("user1"))

    val doc1 = doc("E1", popularEvent)
    val doc2 = doc("E2", unpopularEvent)

    val change1 = mock<DocumentChange>()
    whenever(change1.type).thenReturn(DocumentChange.Type.ADDED)
    whenever(change1.document).thenReturn(doc1)

    val change2 = mock<DocumentChange>()
    whenever(change2.type).thenReturn(DocumentChange.Type.ADDED)
    whenever(change2.document).thenReturn(doc2)

    val snapshot = mock<QuerySnapshot>()
    whenever(snapshot.documentChanges).thenReturn(listOf(change1, change2))

    listenerCaptor.firstValue.onEvent(snapshot, null)

    assertEquals(1, addedEvents.size)
    assertEquals("Popular Concert", addedEvents.first().title)
  }

  // ========== LISTEN TO SAVED EVENTS TESTS ==========

  @Test
  fun listenToSavedEvents_detectsEventDeletion() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    // Setup event listener
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerReg = mock<ListenerRegistration>()
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture()))
        .thenReturn(eventListenerReg)

    var removedEvents: List<Event> = emptyList()

    val registration = repo.listenToSavedEvents(userId) { _, _, removed -> removedEvents = removed }

    // Simulate user document update with saved event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(SAVED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Simulate event deletion (event document no longer exists)
    val deletedEventSnapshot = mock<DocumentSnapshot>()
    whenever(deletedEventSnapshot.exists()).thenReturn(false)
    whenever(deletedEventSnapshot.id).thenReturn("E1")
    eventListenerCaptor.firstValue.onEvent(deletedEventSnapshot, null)

    // Verify removal was detected
    assertEquals(1, removedEvents.size)
    assertEquals("E1", removedEvents.first().uid)

    registration.remove()
    verify(userListenerReg).remove()
    verify(eventListenerReg).remove()
  }

  @Test
  fun listenToSavedEvents_detectsEventModification() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerReg = mock<ListenerRegistration>()
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture()))
        .thenReturn(eventListenerReg)

    var modifiedEvents: List<Event> = emptyList()

    val registration =
        repo.listenToSavedEvents(userId) { _, modified, _ -> modifiedEvents = modified }

    // Simulate user document with saved event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(SAVED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Simulate event modification
    val modifiedEvent = createEvent(uid = "E1", title = "Modified Title")
    val modifiedEventSnapshot = doc("E1", modifiedEvent)
    whenever(modifiedEventSnapshot.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(modifiedEventSnapshot, null)

    // Verify modification was detected
    assertEquals(1, modifiedEvents.size)
    assertEquals("Modified Title", modifiedEvents.first().title)

    registration.remove()
  }

  @Test
  fun listenToSavedEvents_handlesUserDocumentNotFound() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    var addedEvents: List<Event> = emptyList()
    var modifiedEvents: List<Event> = emptyList()
    var removedEvents: List<Event> = emptyList()

    val registration =
        repo.listenToSavedEvents(userId) { added, modified, removed ->
          addedEvents = added
          modifiedEvents = modified
          removedEvents = removed
        }

    // Simulate user document not found
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(false)
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Should trigger callback with empty lists
    assertTrue(addedEvents.isEmpty())
    assertTrue(modifiedEvents.isEmpty())
    assertTrue(removedEvents.isEmpty())

    registration.remove()
  }

  @Test
  fun listenToSavedEvents_handlesListenerError() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    var addedEvents: List<Event> = emptyList()
    var modifiedEvents: List<Event> = emptyList()
    var removedEvents: List<Event> = emptyList()

    val registration =
        repo.listenToSavedEvents(userId) { added, modified, removed ->
          addedEvents = added
          modifiedEvents = modified
          removedEvents = removed
        }

    // Simulate error
    val error = mock<FirebaseFirestoreException>()
    userListenerCaptor.firstValue.onEvent(null, error)

    // Should trigger callback with empty lists
    assertTrue(addedEvents.isEmpty())
    assertTrue(modifiedEvents.isEmpty())
    assertTrue(removedEvents.isEmpty())

    registration.remove()
  }

  @Test
  fun listenToSavedEvents_addsAndRemovesEventListenersDynamically() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef1 = mock<DocumentReference>()
    val eventDocRef2 = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef1)
    whenever(collection.document("E2")).thenReturn(eventDocRef2)

    val eventListenerReg1 = mock<ListenerRegistration>()
    val eventListenerReg2 = mock<ListenerRegistration>()
    whenever(eventDocRef1.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(eventListenerReg1)
    whenever(eventDocRef2.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(eventListenerReg2)

    val registration = repo.listenToSavedEvents(userId) { _, _, _ -> }

    // Step 1: User saves E1
    val userSnapshot1 = mock<DocumentSnapshot>()
    whenever(userSnapshot1.exists()).thenReturn(true)
    whenever(userSnapshot1.get(SAVED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot1, null)

    // Verify listener added for E1
    verify(eventDocRef1).addSnapshotListener(any<EventListener<DocumentSnapshot>>())

    // Step 2: User adds E2
    val userSnapshot2 = mock<DocumentSnapshot>()
    whenever(userSnapshot2.exists()).thenReturn(true)
    whenever(userSnapshot2.get(SAVED_EVENT_IDS)).thenReturn(listOf("E1", "E2"))
    userListenerCaptor.firstValue.onEvent(userSnapshot2, null)

    // Verify listener added for E2
    verify(eventDocRef2).addSnapshotListener(any<EventListener<DocumentSnapshot>>())

    // Step 3: User removes E1
    val userSnapshot3 = mock<DocumentSnapshot>()
    whenever(userSnapshot3.exists()).thenReturn(true)
    whenever(userSnapshot3.get(SAVED_EVENT_IDS)).thenReturn(listOf("E2"))
    userListenerCaptor.firstValue.onEvent(userSnapshot3, null)

    // Verify listener removed for E1
    verify(eventListenerReg1).remove()

    registration.remove()
  }

  // ========== LISTEN TO JOINED EVENTS TESTS ==========

  @Test
  fun listenToJoinedEvents_detectsEventDeletion() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerReg = mock<ListenerRegistration>()
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture()))
        .thenReturn(eventListenerReg)

    var removedEvents: List<Event> = emptyList()

    val registration =
        repo.listenToJoinedEvents(userId) { _, _, removed -> removedEvents = removed }

    // Simulate user document with joined event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(JOINED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Simulate event deletion
    val deletedEventSnapshot = mock<DocumentSnapshot>()
    whenever(deletedEventSnapshot.exists()).thenReturn(false)
    whenever(deletedEventSnapshot.id).thenReturn("E1")
    eventListenerCaptor.firstValue.onEvent(deletedEventSnapshot, null)

    // Verify removal was detected
    assertEquals(1, removedEvents.size)
    assertEquals("E1", removedEvents.first().uid)

    registration.remove()
  }

  @Test
  fun listenToJoinedEvents_detectsEventModification() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerReg = mock<ListenerRegistration>()
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture()))
        .thenReturn(eventListenerReg)

    var modifiedEvents: List<Event> = emptyList()

    val registration =
        repo.listenToJoinedEvents(userId) { _, modified, _ -> modifiedEvents = modified }

    // Simulate user document with joined event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(JOINED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Simulate event modification
    val modifiedEvent = createEvent(uid = "E1", title = "Updated Event")
    val modifiedEventSnapshot = doc("E1", modifiedEvent)
    whenever(modifiedEventSnapshot.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(modifiedEventSnapshot, null)

    // Verify modification was detected
    assertEquals(1, modifiedEvents.size)
    assertEquals("Updated Event", modifiedEvents.first().title)

    registration.remove()
  }

  @Test
  fun listenToJoinedEvents_detectsConsecutiveModifications() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerReg = mock<ListenerRegistration>()
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture()))
        .thenReturn(eventListenerReg)

    val modifiedTitles = mutableListOf<String>()

    val registration =
        repo.listenToJoinedEvents(userId) { _, modified, _ ->
          modifiedTitles.addAll(modified.map { it.title })
        }

    // Simulate user document with joined event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(JOINED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // First modification
    val event1 = createEvent(uid = "E1", title = "Version 1")
    val eventSnapshot1 = doc("E1", event1)
    whenever(eventSnapshot1.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(eventSnapshot1, null)

    // Second modification
    val event2 = createEvent(uid = "E1", title = "Version 2")
    val eventSnapshot2 = doc("E1", event2)
    whenever(eventSnapshot2.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(eventSnapshot2, null)

    // Both modifications should be detected
    assertEquals(2, modifiedTitles.size)
    assertEquals("Version 1", modifiedTitles[0])
    assertEquals("Version 2", modifiedTitles[1])

    registration.remove()
  }

  @Test
  fun listenToJoinedEvents_handlesMultipleEvents() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef1 = mock<DocumentReference>()
    val eventDocRef2 = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef1)
    whenever(collection.document("E2")).thenReturn(eventDocRef2)

    whenever(eventDocRef1.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(mock())
    whenever(eventDocRef2.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(mock())

    val registration = repo.listenToJoinedEvents(userId) { _, _, _ -> }

    // User joins two events
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(JOINED_EVENT_IDS)).thenReturn(listOf("E1", "E2"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Verify both event listeners are set up
    verify(eventDocRef1).addSnapshotListener(any<EventListener<DocumentSnapshot>>())
    verify(eventDocRef2).addSnapshotListener(any<EventListener<DocumentSnapshot>>())

    registration.remove()
  }

  @Test
  fun listenToJoinedEvents_removesAllListenersOnCleanup() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef1 = mock<DocumentReference>()
    val eventDocRef2 = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef1)
    whenever(collection.document("E2")).thenReturn(eventDocRef2)

    val eventListenerReg1 = mock<ListenerRegistration>()
    val eventListenerReg2 = mock<ListenerRegistration>()
    whenever(eventDocRef1.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(eventListenerReg1)
    whenever(eventDocRef2.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(eventListenerReg2)

    val registration = repo.listenToJoinedEvents(userId) { _, _, _ -> }

    // User joins two events
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(JOINED_EVENT_IDS)).thenReturn(listOf("E1", "E2"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Remove registration (cleanup)
    registration.remove()

    // Verify all listeners are removed
    verify(userListenerReg).remove()
    verify(eventListenerReg1).remove()
    verify(eventListenerReg2).remove()
  }
}

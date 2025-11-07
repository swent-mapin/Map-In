package com.swent.mapin.model

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.swent.mapin.model.event.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryFirestore
import com.swent.mapin.model.event.FIELD_SAVED_AT
import com.swent.mapin.model.event.SAVED_SUBCOLLECTION
import com.swent.mapin.model.event.USERS_COLLECTION_PATH
import com.swent.mapin.ui.map.Filters
import java.time.LocalDate
import java.time.ZoneId
import junit.framework.TestCase.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var repo: EventRepositoryFirestore
  private lateinit var document: DocumentReference
  private lateinit var collection: CollectionReference
  private lateinit var usersCollection: CollectionReference
  private lateinit var savedCollection: CollectionReference
  private lateinit var userDocRef: DocumentReference

  @Before
  fun setup() {
    db = mock()
    repo = EventRepositoryFirestore(db)
    document = mock()
    collection =
        mock<CollectionReference>().apply {
          whenever(this.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
              .thenReturn(mock())
          whenever(this.orderBy(any<String>())).thenReturn(mock())
        }
    usersCollection = mock()
    savedCollection = mock()
    userDocRef = mock()

    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(collection)
    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(collection.document()).thenReturn(document)
    whenever(collection.document(any())).thenReturn(document)
    whenever(usersCollection.document(any())).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
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

  private fun doc(id: String, event: Event? = null): DocumentSnapshot {
    val snapshot = mock<DocumentSnapshot>()
    whenever(snapshot.id).thenReturn(id)
    whenever(snapshot.toObject(Event::class.java)).thenReturn(event)
    whenever(snapshot.getString("location.name")).thenReturn(event?.location?.name)
    whenever(snapshot.getDouble("location.latitude")).thenReturn(event?.location?.latitude)
    whenever(snapshot.getDouble("location.longitude")).thenReturn(event?.location?.longitude)
    whenever(snapshot.getDouble("price")).thenReturn(event?.price)
    whenever(snapshot.exists()).thenReturn(event != null)
    return snapshot
  }

  private fun userDoc(id: String, userProfile: UserProfile? = null): DocumentSnapshot {
    val snapshot = mock<DocumentSnapshot>()
    whenever(snapshot.id).thenReturn(id)
    whenever(snapshot.toObject(UserProfile::class.java)).thenReturn(userProfile)
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
  fun getSearchedEvents_filtersByTitle() = runTest {
    val filters =
        Filters(
            startDate = LocalDate.now(),
            endDate = null,
            place = null,
            radiusKm = 10,
            maxPrice = null,
            tags = emptySet())
    val tomorrow =
        Timestamp(
            LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .epochSecond,
            nanoseconds = 0,
        )

    val e1 = createEvent(uid = "1", title = "Party", date = tomorrow)
    val e2 = createEvent(uid = "2", title = "Meeting", date = tomorrow)
    val doc1 = doc("1", e1)
    val doc2 = doc("2", e2)
    val querySnapshot = qs(doc1, doc2)
    val filteredQuery = mock<Query>()
    val orderedQuery = mock<Query>()

    // Stub the query chain for getFilteredEvents
    whenever(collection.whereGreaterThanOrEqualTo(eq("date"), any())).thenReturn(filteredQuery)
    whenever(filteredQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(querySnapshot))
    whenever(querySnapshot.documents).thenReturn(listOf(doc1, doc2))

    val results = repo.getSearchedEvents("Party", filters)

    assertEquals(1, results.size)
    assertEquals("Party", results.first().title)
  }

  @Test
  fun addEvent_autoAddsOwnerToParticipants() = runTest {
    val input =
        createEvent(
            uid = "",
            title = "New Event",
            ownerId = "owner123",
            participantIds = listOf("user1", "user2"))

    val ownerSnapshot = userDoc("owner123", UserProfile())
    whenever(usersCollection.document("owner123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(ownerSnapshot))
    whenever(userDocRef.update(any<Map<String, Any>>())).thenReturn(voidTask())
    whenever(userDocRef.update(any<String>(), any())).thenReturn(voidTask())

    val user1DocRef = mock<DocumentReference>()
    val user1Snapshot = userDoc("user1", UserProfile())
    whenever(usersCollection.document("user1")).thenReturn(user1DocRef)
    whenever(user1DocRef.get()).thenReturn(taskOf(user1Snapshot))
    whenever(user1DocRef.update(any<Map<String, Any>>())).thenReturn(voidTask())
    whenever(user1DocRef.update(any<String>(), any())).thenReturn(voidTask())

    val user2DocRef = mock<DocumentReference>()
    val user2Snapshot = userDoc("user2", UserProfile())
    whenever(usersCollection.document("user2")).thenReturn(user2DocRef)
    whenever(user2DocRef.get()).thenReturn(taskOf(user2Snapshot))
    whenever(user2DocRef.update(any<Map<String, Any>>())).thenReturn(voidTask())
    whenever(user2DocRef.update(any<String>(), any())).thenReturn(voidTask())

    val newEventDocRef = mock<DocumentReference>()
    whenever(collection.document()).thenReturn(newEventDocRef)
    whenever(newEventDocRef.id).thenReturn("E123")
    whenever(newEventDocRef.set(any<Event>())).thenReturn(voidTask())

    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E123")).thenReturn(eventDocRef)
    whenever(eventDocRef.set(any<Event>())).thenReturn(voidTask())

    repo.addEvent(input)

    argumentCaptor<Event>().apply {
      verify(eventDocRef).set(capture())
      assertTrue(firstValue.participantIds.contains("owner123"))
      assertEquals("E123", firstValue.uid)
    }
  }

  @Test
  fun editEvent_updatesEvent() = runTest {
    val existing =
        createEvent(uid = "E1", title = "Old", ownerId = "owner", participantIds = listOf("owner"))
    val updated = existing.copy(title = "Updated")
    val docSnapshot = doc(existing.uid, existing)
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document(existing.uid)).thenReturn(eventDocRef)
    whenever(eventDocRef.get()).thenReturn(taskOf(docSnapshot))
    whenever(eventDocRef.set(any<Event>())).thenReturn(voidTask())

    // If not enough coverage remove the owner from participants in existing
    // and try to find why the hell the bellow update return null
    /*
    val addedUserDocRef = mock<DocumentReference>()
    whenever(usersCollection.document("owner")).thenReturn(addedUserDocRef)
    whenever(addedUserDocRef.update("participatingEventIds", FieldValue.arrayUnion("E1")))
      .thenReturn(voidTask())

    val removedUserDocRef = mock<DocumentReference>()
    whenever(usersCollection.document("owner")).thenReturn(removedUserDocRef)
    whenever(removedUserDocRef.update("participatingEventIds", FieldValue.arrayRemove("E1")))
      .thenReturn(voidTask())
    */

    repo.editEvent("E1", updated)

    argumentCaptor<Event>().apply {
      verify(eventDocRef).set(capture())
      assertEquals("Updated", firstValue.title)
      assertTrue(firstValue.participantIds.contains("owner"))
    }
  }

  @Test
  fun deleteEvent_removesEventAndUpdatesUserProfile() = runTest {
    val userProfileOwner =
        UserProfile(
            userId = "owner123",
            ownedEventIds = mutableListOf("E1"),
            participatingEventIds = mutableListOf("E1"))
    val userProfileParticipant =
        UserProfile(
            userId = "user1",
            ownedEventIds = mutableListOf(),
            participatingEventIds = mutableListOf("E1"))
    val ownerDocRef = mock<DocumentReference>()
    val participantDocRef = mock<DocumentReference>()
    val ownerSnapshot = userDoc("owner123", userProfileOwner)
    val participantSnapshot = userDoc("user1", userProfileParticipant)
    whenever(usersCollection.document("owner123")).thenReturn(ownerDocRef)
    whenever(usersCollection.document("user1")).thenReturn(participantDocRef)
    whenever(ownerDocRef.get()).thenReturn(taskOf(ownerSnapshot))
    whenever(participantDocRef.get()).thenReturn(taskOf(participantSnapshot))
    whenever(ownerDocRef.update(any<String>(), any())).thenReturn(voidTask())
    whenever(participantDocRef.update(any<String>(), any())).thenReturn(voidTask())

    val event = createEvent(uid = "E1", ownerId = "owner123", participantIds = listOf("user1"))
    val eventDocRef = mock<DocumentReference>()
    val eventSnapshot = doc("E1", event)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(eventDocRef.get()).thenReturn(taskOf(eventSnapshot))
    whenever(eventDocRef.delete()).thenReturn(voidTask())

    repo.deleteEvent("E1")

    verify(eventDocRef).delete()
    verify(ownerDocRef).update(eq("ownedEventIds"), any<FieldValue>())
    verify(participantDocRef).update(eq("participatingEventIds"), any<FieldValue>())
  }

  @Test
  fun getSavedEventIds_returnsDocumentIds() = runTest {
    val doc1 = doc("E1")
    val doc2 = doc("E2")
    val snap = qs(doc1, doc2)
    val userDocRef = mock<DocumentReference>()
    val query = mock<Query>()
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.orderBy(FIELD_SAVED_AT)).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val ids = repo.getSavedEventIds("user123")

    assertEquals(setOf("E1", "E2"), ids)
  }

  @Test
  fun saveEvent_addsDocument() = runTest {
    val userDocRef = mock<DocumentReference>()
    val savedDocRef = mock<DocumentReference>()
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.document("E1")).thenReturn(savedDocRef)
    whenever(savedDocRef.set(any())).thenReturn(voidTask())

    repo.saveEventForUser("user123", "E1")

    verify(savedDocRef).set(any())
  }

  @Test
  fun unsaveEvent_removesDocument() = runTest {
    val userDocRef = mock<DocumentReference>()
    val savedDocRef = mock<DocumentReference>()
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.document("E1")).thenReturn(savedDocRef)
    whenever(savedDocRef.delete()).thenReturn(voidTask())

    repo.unsaveEventForUser("user123", "E1")

    verify(savedDocRef).delete()
  }

  @Test
  fun getNewUid_returnsUniqueId() {
    whenever(document.id).thenReturn("RANDOM_ID")
    val id = repo.getNewUid()
    assertEquals("RANDOM_ID", id)
  }

  @Test
  fun getAllEvents_returnsAllDocuments() = runTest {
    val e1 = createEvent(uid = "1", title = "T1")
    val e2 = createEvent(uid = "2", title = "T2")
    val doc1 = doc("1", e1)
    val doc2 = doc("2", e2)
    val snap = qs(doc1, doc2)
    val query = mock<Query>()
    whenever(collection.orderBy("date")).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getAllEvents()
    assertEquals(2, result.size)
    assertEquals("T1", result[0].title)
  }

  @Test
  fun getFilteredEvents_appliesTagAndDateFilters() = runTest {
    val filters =
        Filters(
            startDate = LocalDate.now().minusDays(1),
            endDate = LocalDate.now().plusDays(1),
            place = null,
            radiusKm = 10,
            maxPrice = null,
            tags = setOf("music"))
    val event = createEvent(uid = "E1", title = "Music Event", tags = listOf("music"))
    val doc = doc("E1", event)
    val snap = qs(doc)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereArrayContainsAny(any<String>(), any<List<String>>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)
    assertTrue(result.isNotEmpty())
    assertEquals("Music Event", result.first().title)
  }

  @Test
  fun getSavedEvents_returnsSortedList() = runTest {
    val e1 = createEvent(uid = "E1", title = "First")
    val e2 = createEvent(uid = "E2", title = "Second")
    val doc1 = doc("E1", e1)
    val doc2 = doc("E2", e2)
    val querySnapshot = qs(doc1, doc2)
    val userDocRef = mock<DocumentReference>()
    val query = mock<Query>()
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.orderBy(FIELD_SAVED_AT)).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(querySnapshot))

    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)
    whenever(eventQuery.get()).thenReturn(taskOf(querySnapshot))

    val result = repo.getSavedEvents("user123")
    assertEquals(2, result.size)
    assertEquals("First", result.first().title)
  }

  @Test
  fun getFilteredEvents_withFriendsOnly_noFriends_returnsEmptyList() = runTest {
    val filters =
        Filters(
            startDate = LocalDate.now(),
            endDate = null,
            place = null,
            radiusKm = 10,
            maxPrice = null,
            tags = emptySet(),
            friendsOnly = true)

    // Mock FirebaseAuth and current user
    mockStatic(FirebaseAuth::class.java).use { authMock ->
      val auth = mock<FirebaseAuth>()
      val user = mock<FirebaseUser>()
      whenever(auth.currentUser).thenReturn(user)
      whenever(user.uid).thenReturn("user123")
      authMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(auth)

      // Mock FriendRequestRepository construction
      mockConstruction(FriendRequestRepository::class.java) { mock, _ ->
            runBlocking { whenever(mock.getFriends("user123")).thenReturn(emptyList()) }

            // Inside the block, the mock is active
            val repo = EventRepositoryFirestore(db)

            // Mock Firestore query chain
            val query = mock<Query>()
            whenever(collection.orderBy("date")).thenReturn(query)
            val emptySnapshot = mock<QuerySnapshot>()
            whenever(query.get()).thenReturn(taskOf(emptySnapshot))
            whenever(emptySnapshot.documents).thenReturn(emptyList())

            // Execute
            val result = runBlocking { repo.getFilteredEvents(filters) }

            // Verify
            assertTrue(result.isEmpty())
            verify(query).whereEqualTo("participantIds", "_NO_MATCH_")
          }
          .use { /* construction block is automatically closed */}
    }
  }

  @Test
  fun documentToEvent_returnsValidEvent() {
    val event =
        createEvent(
            uid = "E1",
            title = "Concert",
            location = Location("Z", 47.3, 8.5),
            tags = listOf("music"))
    val doc =
        doc("E1", event).apply {
          whenever(getString("location.name")).thenReturn("Z")
          whenever(get("location.latitude")).thenReturn(47.3)
          whenever(get("location.longitude")).thenReturn(8.5)
          whenever(get("price")).thenReturn(20.0)
        }

    val result =
        repo.run {
          val method =
              this::class.java.getDeclaredMethod("documentToEvent", DocumentSnapshot::class.java)
          method.isAccessible = true
          method.invoke(this, doc) as Event
        }

    assertEquals("E1", result.uid)
    assertEquals("Concert", result.title)
  }

  @Test
  fun deleteEvent_eventNotFound_throwsNoSuchElementException() = runTest {
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E404")).thenReturn(eventDocRef)

    val missingSnapshot = mock<DocumentSnapshot>()
    whenever(missingSnapshot.exists()).thenReturn(false)
    whenever(eventDocRef.get()).thenReturn(Tasks.forResult(missingSnapshot))

    val exception = assertFailsWith<NoSuchElementException> { repo.deleteEvent("E404") }

    assertTrue(exception.message!!.contains("Event not found"))
    verify(eventDocRef, never()).delete()
  }

  @Test
  fun getFilteredEvents_appliesMaxPriceFilter() = runTest {
    val filters = Filters(maxPrice = 30)

    val e1 = createEvent(uid = "E1", title = "Cheap", description = "Under budget", price = 20.0)
    val e2 = createEvent(uid = "E2", title = "Expensive", description = "Over budget", price = 50.0)
    val doc1 = doc("E1", e1)
    val doc2 = doc("E2", e2)
    val snap = qs(doc1, doc2)

    val filteredQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(eq("date"), any())).thenReturn(filteredQuery)
    whenever(filteredQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)

    assertEquals(1, result.size)
    assertEquals("Cheap", result.first().title)
  }

  @Test
  fun getFilteredEvents_appliesLocationRadiusFilter() = runTest {
    val filters = Filters(place = Location("Center", 47.3769, 8.5417), radiusKm = 5)

    val nearEvent = createEvent("E1", "Nearby", location = Location("Near", 47.377, 8.542))
    val farEvent = createEvent("E2", "Far", location = Location("Far", 48.0, 8.0))
    val doc1 = doc("E1", nearEvent)
    val doc2 = doc("E2", farEvent)
    val snap = qs(doc1, doc2)

    val filteredQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(eq("date"), any())).thenReturn(filteredQuery)
    whenever(filteredQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters)

    assertTrue(result.any { it.title == "Nearby" })
    assertTrue(result.none { it.title == "Far" })
  }

  @Test
  fun getFilteredEvents_appliesStartAndEndDate() = runTest {
    val filters =
        Filters(startDate = LocalDate.now().minusDays(1), endDate = LocalDate.now().plusDays(1))

    val validEvent =
        createEvent(
            "E1",
            "Valid Date",
            date =
                Timestamp(
                    LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .epochSecond,
                    nanoseconds = 0,
                ))
    val futureEvent =
        createEvent(
            "E2",
            "Too Early",
            date =
                Timestamp(
                    LocalDate.now()
                        .plusDays(10)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .epochSecond,
                    0))
    val doc1 = doc("E1", validEvent)
    val doc2 = doc("E2", futureEvent)
    val snap = qs(doc1, doc2)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenAnswer {
      val filteredDocs =
          snap.documents.filter {
            val event = it.toObject(Event::class.java)
            event
                ?.date
                ?.toDate()
                ?.toInstant()
                ?.isBefore(
                    filters.endDate!!.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                ?: false
          }
      taskOf(qs(*filteredDocs.toTypedArray()))
    }

    val result = repo.getFilteredEvents(filters)

    assertEquals(1, result.size)
    assertEquals("Valid Date", result.first().title)
  }

  @Test
  fun getFilteredEvents_tagFilter_skipsEventsWithoutTags() = runTest {
    val filters = Filters(tags = setOf("art"))

    val taggedEvent = createEvent("E1", "Tagged", tags = listOf("art"))
    val untaggedEvent = createEvent("E2", "No Tags")
    val doc1 = doc("E1", taggedEvent)
    val doc2 = doc("E2", untaggedEvent)
    val snap = qs(doc1, doc2)

    val filteredQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(eq("date"), any())).thenReturn(filteredQuery)
    whenever(filteredQuery.whereArrayContainsAny(any<String>(), any<List<String>>()))
        .thenReturn(filteredQuery)
    whenever(filteredQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenAnswer {
      val filteredDocs =
          snap.documents.filter {
            val event = it.toObject(Event::class.java)
            event?.tags?.contains("art") == true
          }
      taskOf(qs(*filteredDocs.toTypedArray()))
    }

    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { "art" in it.tags })
  }

  @Test
  fun getAllEvents_throwsException_whenFirestoreFails() = runTest {
    val query = mock<Query>()
    whenever(collection.orderBy("date")).thenReturn(query)
    whenever(query.get()).thenReturn(Tasks.forException(RuntimeException("firestore error")))

    val exception = assertFailsWith<Exception> { repo.getAllEvents() }

    assertTrue(exception.message!!.contains("Failed to fetch all events"))
  }

  @Test
  fun getEvent_throwsNoSuchElement_whenEventNotFound() = runTest {
    val docSnapshot = mock<DocumentSnapshot>()
    whenever(docSnapshot.exists()).thenReturn(false)
    whenever(document.get()).thenReturn(Tasks.forResult(docSnapshot))

    assertFailsWith<NoSuchElementException> { repo.getEvent("MISSING") }
  }

  @Test
  fun getEvent_catchesFirestoreException_andWrapsIt() = runTest {
    whenever(document.get()).thenReturn(Tasks.forException(RuntimeException("firestore broken")))

    val exception = assertFailsWith<Exception> { repo.getEvent("E1") }
    assertTrue(exception.message!!.contains("Failed to fetch event"))
  }

  @Test
  fun getFilteredEvents_throwsException_whenQueryFails() = runTest {
    val query = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(query)
    whenever(query.orderBy(any<String>())).thenReturn(query)
    whenever(query.get()).thenReturn(Tasks.forException(RuntimeException("broken")))

    val filters = Filters(startDate = LocalDate.now())
    val exception = assertFailsWith<Exception> { repo.getFilteredEvents(filters) }

    assertTrue(exception.message!!.contains("Failed to fetch filtered events"))
  }

  @Test
  fun getSearchedEvents_catchesException_andWrapsIt() = runTest {
    val filters = Filters()

    val exception = assertFailsWith<Exception> { repo.getSearchedEvents("test", filters) }
    assertTrue(exception.message!!.contains("Failed to fetch events by title search"))
  }

  @Test
  fun addEvent_throwsException_whenInvalidEvent() = runTest {
    val invalidEvent = createEvent(title = "", ownerId = "")
    assertFailsWith<IllegalArgumentException> { repo.addEvent(invalidEvent) }
  }

  @Test
  fun addEvent_catchesFirestoreError() = runTest {
    val event = createEvent(ownerId = "owner123")
    whenever(collection.document()).thenReturn(document)
    whenever(document.id).thenReturn("E123")
    whenever(document.set(any<Event>())).thenReturn(Tasks.forException(RuntimeException("boom")))

    val exception = assertFailsWith<Exception> { repo.addEvent(event) }
    assertTrue(exception.message!!.contains("Failed to add event"))
  }

  @Test
  fun editEvent_throwsException_whenInvalidEvent() = runTest {
    val invalid = createEvent(title = "", ownerId = "")
    assertFailsWith<IllegalArgumentException> { repo.editEvent("E1", invalid) }
  }

  @Test
  fun editEvent_catchesFirestoreException() = runTest {
    val existing = createEvent(uid = "E1", ownerId = "owner")
    val docSnapshot = doc(existing.uid, existing)
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document(existing.uid)).thenReturn(eventDocRef)
    whenever(eventDocRef.get()).thenReturn(taskOf(docSnapshot))
    whenever(eventDocRef.set(any<Event>())).thenReturn(Tasks.forException(RuntimeException("fail")))

    val exception = assertFailsWith<Exception> { repo.editEvent("E1", existing) }
    assertTrue(exception.message!!.contains("Failed to edit event"))
  }

  @Test
  fun deleteEvent_catchesFirestoreException() = runTest {
    val event = createEvent(uid = "E1", ownerId = "owner123")
    val eventDocRef = mock<DocumentReference>()
    val doc = doc("E1", event)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(eventDocRef.get()).thenReturn(taskOf(doc))
    whenever(eventDocRef.delete()).thenReturn(Tasks.forException(RuntimeException("broken")))

    val exception = assertFailsWith<Exception> { repo.deleteEvent("E1") }
    assertTrue(exception.message!!.contains("Failed to delete event"))
  }

  @Test
  fun getSavedEventIds_returnsEmptySet_whenFirestoreFails() = runTest {
    val userDocRef = mock<DocumentReference>()
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.orderBy(FIELD_SAVED_AT)).thenThrow(RuntimeException("firestore down"))

    val result = repo.getSavedEventIds("user123")
    assertTrue(result.isEmpty())
  }

  @Test
  fun getSavedEvents_returnsEmptyList_whenFirestoreFails() = runTest {
    val repoSpy = spy(repo)
    whenever(repoSpy.getSavedEventIds("user123")).thenThrow(RuntimeException("fail"))

    val result = repoSpy.getSavedEvents("user123")
    assertTrue(result.isEmpty())
  }

  @Test
  fun saveEventForUser_returnsFalse_whenBothRemoteAndLocalFail() = runTest {
    val userDocRef = mock<DocumentReference>()
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    val savedDocRef = mock<DocumentReference>()
    whenever(savedCollection.document("E1")).thenReturn(savedDocRef)
    whenever(savedDocRef.set(any()))
        .thenReturn(Tasks.forException(RuntimeException("firestore failed")))

    val result = repo.saveEventForUser("user123", "E1")
    assertFalse(result)
  }

  @Test
  fun unsaveEventForUser_returnsFalse_whenFirestoreFails() = runTest {
    val userDocRef = mock<DocumentReference>()
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    val savedDocRef = mock<DocumentReference>()
    whenever(savedCollection.document("E1")).thenReturn(savedDocRef)
    whenever(savedDocRef.delete()).thenReturn(Tasks.forException(RuntimeException("delete failed")))

    val result = repo.unsaveEventForUser("user123", "E1")
    assertFalse(result)
  }

  @Test
  fun documentToEvent_returnsNull_onMissingFields() {
    val doc = mock<DocumentSnapshot>()
    whenever(doc.id).thenReturn("E1")
    whenever(doc.toObject(Event::class.java)).thenReturn(Event(uid = "E1"))
    whenever(doc.getString("location.name")).thenReturn(null)

    val result =
        repo.run {
          val method =
              this::class.java.getDeclaredMethod("documentToEvent", DocumentSnapshot::class.java)
          method.isAccessible = true
          method.invoke(this, doc)
        }

    assertEquals(null, result)
  }
}

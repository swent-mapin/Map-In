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
import com.swent.mapin.ui.filters.Filters
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
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*

// Assisted by AI

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var repo: EventRepositoryFirestore
  private lateinit var document: DocumentReference
  private lateinit var collection: CollectionReference
  private lateinit var usersCollection: CollectionReference
  private lateinit var savedCollection: CollectionReference
  private lateinit var userDocRef: DocumentReference
  private lateinit var friendRepo: FriendRequestRepository

  @Before
  fun setup() {
    db = mock()
    friendRepo = mock()
    repo = EventRepositoryFirestore(db, friendRequestRepository = friendRepo)
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
  fun getNewUid_returnsUniqueId() {
    whenever(document.id).thenReturn("RANDOM_ID")
    val id = repo.getNewUid()
    assertEquals("RANDOM_ID", id)
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
    val filters = Filters(friendsOnly = true)

    val event = createEvent(uid = "E1", title = "Event")
    val doc = doc("E1", event)
    val query = mock<Query>()
    val querySnapshot = qs(doc)

    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(query)
    whenever(query.orderBy("date")).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(querySnapshot))

    runBlocking { whenever(friendRepo.getFriends("user123")).thenReturn(emptyList()) }

    mockStatic(FirebaseAuth::class.java).use { authMock ->
      val auth = mock<FirebaseAuth>()
      val user = mock<FirebaseUser>()
      whenever(auth.currentUser).thenReturn(user)
      whenever(user.uid).thenReturn("user123")
      authMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(auth)

      val result = repo.getFilteredEvents(filters)

      assertTrue(result.isEmpty())
      verify(friendRepo).getFriends("user123")
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
  fun getEventsByOwner_returnsEventsForSpecificOwner() = runTest {
    val ownerId = "owner123"
    val ownedEvent1 = createEvent(uid = "E1", title = "Event 1", ownerId = ownerId)
    val ownedEvent2 = createEvent(uid = "E2", title = "Event 2", ownerId = ownerId)

    val doc1 = doc("E1", ownedEvent1)
    val doc2 = doc("E2", ownedEvent2)
    val snap = qs(doc1, doc2)

    val ownerQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereEqualTo("ownerId", ownerId)).thenReturn(ownerQuery)
    whenever(ownerQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByOwner(ownerId)

    assertEquals(2, result.size)
    assertTrue(result.all { it.ownerId == ownerId })
    assertEquals("Event 1", result[0].title)
    assertEquals("Event 2", result[1].title)
  }

  @Test
  fun getEventsByOwner_returnsEmptyList_whenNoEventsOwned() = runTest {
    val ownerId = "owner123"
    val snap = qs()

    val ownerQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereEqualTo("ownerId", ownerId)).thenReturn(ownerQuery)
    whenever(ownerQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByOwner(ownerId)

    assertTrue(result.isEmpty())
  }

  @Test
  fun getEventsByOwner_throwsException_whenFirestoreFails() = runTest {
    val ownerId = "owner123"
    val ownerQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereEqualTo("ownerId", ownerId)).thenReturn(ownerQuery)
    whenever(ownerQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(Tasks.forException(RuntimeException("firestore error")))

    val exception = assertFailsWith<Exception> { repo.getEventsByOwner(ownerId) }

    assertTrue(exception.message!!.contains("Failed to fetch events by owner"))
  }

  @Test
  fun getEventsByOwner_filtersCorrectly_withMultipleOwners() = runTest {
    val ownerId = "owner123"
    val ownedEvent = createEvent(uid = "E1", title = "My Event", ownerId = ownerId)
    val doc1 = doc("E1", ownedEvent)
    val snap = qs(doc1)

    val ownerQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereEqualTo("ownerId", ownerId)).thenReturn(ownerQuery)
    whenever(ownerQuery.orderBy("date")).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByOwner(ownerId)

    assertEquals(1, result.size)
    assertEquals(ownerId, result[0].ownerId)
    assertEquals("My Event", result[0].title)
  }
}

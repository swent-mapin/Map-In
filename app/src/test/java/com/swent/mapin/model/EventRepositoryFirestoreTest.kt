package com.swent.mapin.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.swent.mapin.model.event.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryFirestore
import com.swent.mapin.model.event.SAVED_SUBCOLLECTION
import com.swent.mapin.model.event.USERS_COLLECTION_PATH
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var repo: EventRepositoryFirestore
  private lateinit var document: DocumentReference
  private lateinit var collection: CollectionReference

  @Before
  fun setup() {
    db = mock()
    repo = EventRepositoryFirestore(db)
    document = mock()
    collection = mock()

    whenever(db.collection(any())).thenReturn(collection)
    whenever(collection.document(any())).thenReturn(document)
  }

  @Test
  fun getEvent_success_returnsMappedEvent() = runTest {
    val e =
        Event(
            uid = "",
            title = "Title",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("Zurich", 47.3769, 8.5417),
            tags = listOf("music"),
            public = true,
            ownerId = "owner",
            imageUrl = null,
            capacity = 50)

    val docSnapshot =
        mock<DocumentSnapshot> {
          on { id } doReturn "E1"
          on { toObject(Event::class.java) } doReturn e
        }

    whenever(document.get()).thenReturn(Tasks.forResult(docSnapshot))

    val result = repo.getEvent("E1")
    assertEquals("E1", result.uid)
    assertEquals("Title", result.title)
  }

  @Test
  fun getSearchedEvents_filtersByTitle() = runTest {
    val e1 =
        Event(
            uid = "1",
            title = "Party",
            url = "",
            description = "",
            date = Timestamp.now(),
            location = Location("L", 0.0, 0.0),
            tags = listOf(),
            public = true,
            ownerId = "owner")
    val e2 =
        Event(
            uid = "2",
            title = "Meeting",
            url = "",
            description = "",
            date = Timestamp.now(),
            location = Location("L", 0.0, 0.0),
            tags = listOf(),
            public = true,
            ownerId = "owner")

    val doc1 =
        mock<DocumentSnapshot> {
          on { id } doReturn "1"
          on { toObject(Event::class.java) } doReturn e1
        }
    val doc2 =
        mock<DocumentSnapshot> {
          on { id } doReturn "2"
          on { toObject(Event::class.java) } doReturn e2
        }

    val querySnapshot = mock<QuerySnapshot>()
    whenever(querySnapshot.documents).thenReturn(listOf(doc1, doc2))

    val query = mock<Query>()
    whenever(collection.orderBy("date")).thenReturn(query)
    whenever(query.get()).thenReturn(Tasks.forResult(querySnapshot))

    val results = repo.getSearchedEvents("Party")

    assertEquals(1, results.size)
    assertEquals("Party", results.first().title)
  }

  @Test
  fun addEvent_autoAddsOwnerToParticipants() = runTest {
    val input =
        Event(
            uid = "",
            title = "New Event",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("L", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "owner123",
            imageUrl = null,
            capacity = 10,
            participantIds = listOf("user1", "user2"))

    // Mock user side
    val usersCollection = mock<CollectionReference>()
    val userDocRef = mock<DocumentReference>()
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.toObject(UserProfile::class.java)).thenReturn(UserProfile())

    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(usersCollection.document(any())).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(Tasks.forResult(userSnapshot))
    whenever(userDocRef.update(any<Map<String, Any>>())).thenReturn(Tasks.forResult(mock()))

    // Mock events side
    val eventsCollection = mock<CollectionReference>()
    val eventDocRef = mock<DocumentReference>()

    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(eventsCollection)
    whenever(eventsCollection.document()).thenReturn(eventDocRef)
    whenever(eventsCollection.document(any())).thenReturn(eventDocRef)
    whenever(eventDocRef.id).thenReturn("E123")
    whenever(eventDocRef.set(any<Event>())).thenReturn(Tasks.forResult(mock()))
    whenever(userDocRef.update(any<String>(), any<Any>())).thenReturn(Tasks.forResult(mock()))

    // Run method
    repo.addEvent(input)

    // Verify
    argumentCaptor<Event>().apply {
      verify(eventDocRef).set(capture())
      assertTrue(firstValue.participantIds.contains("owner123"))
      assertEquals("E123", firstValue.uid)
    }
  }

  @Test
  fun editEvent_updatesEvent() = runTest {
    val existing =
        Event(
            uid = "E1",
            title = "Old",
            url = "",
            description = "",
            date = Timestamp.now(),
            location = Location("L", 0.0, 0.0),
            tags = listOf(),
            public = true,
            ownerId = "owner")
    val updated = existing.copy(title = "Updated")

    val docSnapshot =
        mock<DocumentSnapshot> {
          on { id } doReturn "E1"
          on { exists() } doReturn true
          on { toObject(Event::class.java) } doReturn existing
        }

    val eventsCollection = mock<CollectionReference>()
    val eventsDocRef = mock<DocumentReference>()
    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(eventsCollection)
    whenever(eventsCollection.document("E1")).thenReturn(eventsDocRef)
    whenever(eventsDocRef.get()).thenReturn(Tasks.forResult(docSnapshot))
    whenever(eventsDocRef.set(any<Event>())).thenReturn(Tasks.forResult(mock()))

    val usersCollection = mock<CollectionReference>()
    val userDocRef = mock<DocumentReference>()
    val userSnapshot =
        mock<DocumentSnapshot> {
          on { exists() } doReturn true
          on { toObject(UserProfile::class.java) } doReturn UserProfile()
        }
    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(usersCollection.document(any())).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(Tasks.forResult(userSnapshot))
    whenever(userDocRef.update(any<String>(), any<Any>())).thenReturn(Tasks.forResult(mock()))
    whenever(userDocRef.update(any<Map<String, Any>>())).thenReturn(Tasks.forResult(mock()))

    repo.editEvent("E1", updated)

    argumentCaptor<Event>().apply {
      verify(eventsDocRef).set(capture())
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

    val usersCollection = mock<CollectionReference>()
    val ownerDocRef = mock<DocumentReference>()
    val participantDocRef = mock<DocumentReference>()
    val ownerSnapshot =
        mock<DocumentSnapshot> {
          on { exists() } doReturn true
          on { toObject(UserProfile::class.java) } doReturn userProfileOwner
        }
    val participantSnapshot =
        mock<DocumentSnapshot> {
          on { exists() } doReturn true
          on { toObject(UserProfile::class.java) } doReturn userProfileParticipant
        }

    // Mock Firestore document selection
    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(usersCollection.document("owner123")).thenReturn(ownerDocRef)
    whenever(usersCollection.document("user1")).thenReturn(participantDocRef)

    // Mock gets and updates
    whenever(ownerDocRef.get()).thenReturn(Tasks.forResult(ownerSnapshot))
    whenever(ownerDocRef.update(any<String>(), any())).thenReturn(Tasks.forResult(mock()))

    whenever(participantDocRef.get()).thenReturn(Tasks.forResult(participantSnapshot))
    whenever(participantDocRef.update(any<String>(), any())).thenReturn(Tasks.forResult(mock()))

    // Event mocks
    val eventsCollection = mock<CollectionReference>()
    val eventDocRef = mock<DocumentReference>()
    val eventSnapshot =
        mock<DocumentSnapshot> {
          on { exists() } doReturn true
          on { id } doReturn "E1"
          on { toObject(Event::class.java) } doReturn
              Event(
                  uid = "E1",
                  title = "Title",
                  url = "",
                  description = "",
                  date = com.google.firebase.Timestamp.now(),
                  location = Location("L", 0.0, 0.0),
                  tags = emptyList(),
                  public = true,
                  ownerId = "owner123",
                  participantIds = listOf("user1"))
        }

    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(eventsCollection)
    whenever(eventsCollection.document("E1")).thenReturn(eventDocRef)
    whenever(eventDocRef.get()).thenReturn(Tasks.forResult(eventSnapshot))
    whenever(eventDocRef.delete()).thenReturn(Tasks.forResult(mock()))

    // Run
    repo.deleteEvent("E1")

    // Verify
    verify(eventDocRef).delete()
    verify(ownerDocRef).update(eq("ownedEventIds"), eq(emptyList<String>()))
    verify(participantDocRef).update(eq("participatingEventIds"), eq(emptyList<String>()))
  }

  @Test
  fun getSavedEventIds_returnsDocumentIds() = runTest {
    // Create individual mocks for DocumentSnapshot (avoid mock { ... })
    val doc1 = mock<DocumentSnapshot>()
    whenever(doc1.id).thenReturn("E1")

    val doc2 = mock<DocumentSnapshot>()
    whenever(doc2.id).thenReturn("E2")

    // Mock QuerySnapshot and return list of docs
    val snap = mock<QuerySnapshot>()
    whenever(snap.documents).thenReturn(listOf(doc1, doc2))

    // Mock Firestore references
    val usersCollection = mock<CollectionReference>()
    val userDocRef = mock<DocumentReference>()
    val savedCollection = mock<CollectionReference>()
    val query = mock<Query>()

    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.orderBy("savedAt")).thenReturn(query)
    whenever(query.get()).thenReturn(Tasks.forResult(snap))

    // Run the repo method
    val ids = repo.getSavedEventIds("user123")

    // Verify
    assertEquals(setOf("E1", "E2"), ids)
  }

  @Test
  fun saveEvent_addsDocument() = runTest {
    val usersCollection = mock<CollectionReference>()
    val userDocRef = mock<DocumentReference>()
    val savedCollection = mock<CollectionReference>()
    val savedDocRef = mock<DocumentReference>()

    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.document("E1")).thenReturn(savedDocRef)
    whenever(savedDocRef.set(any())).thenReturn(Tasks.forResult(mock()))

    repo.saveEventForUser("user123", "E1")

    verify(savedDocRef).set(any())
  }

  @Test
  fun unsaveEvent_removesDocument() = runTest {
    val usersCollection = mock<CollectionReference>()
    val userDocRef = mock<DocumentReference>()
    val savedCollection = mock<CollectionReference>()
    val savedDocRef = mock<DocumentReference>()

    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.collection(SAVED_SUBCOLLECTION)).thenReturn(savedCollection)
    whenever(savedCollection.document("E1")).thenReturn(savedDocRef)
    whenever(savedDocRef.delete()).thenReturn(Tasks.forResult(mock()))

    repo.unsaveEventForUser("user123", "E1")

    verify(savedDocRef).delete()
  }
}

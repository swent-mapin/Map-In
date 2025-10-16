package com.swent.mapin.model

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.swent.mapin.model.event.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// Test writing and documentation assisted by AI tools
/**
 * Tests for [EventRepositoryFirestore]. Uses Mockito to mock Firestore interactions. Uses
 * kotlinx-coroutines-test to test suspend functions.
 *
 * These tests validate Firestore interactions for event data management.
 *
 * Test coverage includes:
 * - Correct mapping of Firestore documents to Event objects
 * - Handling of non-existent documents
 * - Client-side filtering by title
 * - Adding events with generated UIDs
 * - Editing events
 * - Deleting events
 * - Skipping invalid documents during list retrieval
 */
@RunWith(MockitoJUnitRunner::class)
class EventRepositoryFirestoreTest {

  @Mock lateinit var db: FirebaseFirestore
  @Mock lateinit var collection: CollectionReference
  @Mock lateinit var document: DocumentReference
  @Mock lateinit var query: Query

  private lateinit var repo: EventRepositoryFirestore

  @Before
  fun setUp() {
    repo = EventRepositoryFirestore(db)

    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(collection)
    whenever(collection.document()).thenReturn(document)
    whenever(collection.document(any<String>())).thenReturn(document)
  }

  // Helpers
  private fun <T> taskOf(value: T): Task<T> = Tasks.forResult(value)

  private fun voidTask(): Task<Void> = Tasks.forResult(null)

  private fun doc(id: String, event: Event?): DocumentSnapshot {
    val d = mock<DocumentSnapshot>()
    whenever(d.id).thenReturn(id)
    whenever(d.toObject(Event::class.java)).thenReturn(event)
    return d
  }

  private fun qs(vararg docs: DocumentSnapshot): QuerySnapshot {
    val snap = mock<QuerySnapshot>()
    whenever(snap.documents).thenReturn(docs.toList())
    return snap
  }

  // --- Tests ---

  @Test
  fun getNewUid_returnsNonEmpty() {
    whenever(document.id).thenReturn("abc123")
    val id = repo.getNewUid()
    assertEquals("abc123", id)
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
            capacity = 50,
            attendeeCount = 0)

    val snap: DocumentSnapshot = mock()
    whenever(snap.id).thenReturn("E1")
    whenever(snap.toObject(Event::class.java)).thenReturn(e)
    whenever(document.get()).thenReturn(taskOf(snap))

    val out = repo.getEvent("E1")
    assertEquals("E1", out.uid)
    assertEquals("Title", out.title)
  }

  @Test(expected = NoSuchElementException::class)
  fun getEvent_notFound_throws() = runTest {
    val snap: DocumentSnapshot = mock()
    whenever(snap.toObject(Event::class.java)).thenReturn(null)
    whenever(document.get()).thenReturn(taskOf(snap))

    repo.getEvent("missing")
  }

  @Test
  fun getAllEvents_mapsValidDocs_andSkipsBadOnes() = runTest {
    val valid =
        Event(
            uid = "",
            title = "Ok",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("Zurich", 47.37, 8.54),
            tags = listOf("tag"),
            public = true,
            ownerId = "o",
            imageUrl = null,
            capacity = 10,
            attendeeCount = 1)
    val goodDoc = doc("GOOD", valid)

    // Simulate a doc that throws during toObject -> documentToEvent returns null
    val badDoc =
        mock<DocumentSnapshot>().also {
          whenever(it.id).thenReturn("BAD")
          whenever(it.toObject(Event::class.java)).thenThrow(RuntimeException("boom"))
        }

    val snap = qs(goodDoc, badDoc)

    // Only this test needs ordering & query.get()
    whenever(collection.orderBy(eq("date"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val list = repo.getAllEvents()
    assertEquals(1, list.size)
    assertEquals("GOOD", list[0].uid)
  }

  @Test
  fun getEventsByTitle_filtersClientSide_caseInsensitive_trim() = runTest {
    val e1 =
        Event(
            uid = "",
            title = " Rock Night ",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("ZRH", 0.0, 0.0),
            tags = listOf("m"),
            public = true,
            ownerId = "a",
            imageUrl = null,
            capacity = 1,
            attendeeCount = 0)
    val e2 =
        Event(
            uid = "",
            title = "rock night",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("ZRH", 0.0, 0.0),
            tags = listOf("m"),
            public = true,
            ownerId = "b",
            imageUrl = null,
            capacity = 1,
            attendeeCount = 0)
    val e3 =
        Event(
            uid = "",
            title = "Jazz",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("ZRH", 0.0, 0.0),
            tags = listOf("m"),
            public = true,
            ownerId = "c",
            imageUrl = null,
            capacity = 1,
            attendeeCount = 0)

    val snap = qs(doc("1", e1), doc("2", e2), doc("3", e3))

    whenever(collection.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByTitle("  ROCK NIGHT ")
    assertEquals(listOf("1", "2"), result.map { it.uid })
  }

  @Test
  fun addEvent_withBlankUid_generatesAndPersists_withFilledUid() = runTest {
    whenever(document.id).thenReturn("NEWID")
    whenever(document.set(any<Event>())).thenReturn(voidTask())

    val input =
        Event(
            uid = "",
            title = "T",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("L", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "o",
            imageUrl = null,
            capacity = 1,
            attendeeCount = 0)

    repo.addEvent(input)

    argumentCaptor<Event>().apply {
      verify(document).set(capture())
      assertEquals("NEWID", firstValue.uid)
      assertEquals("T", firstValue.title)
    }
  }

  @Test
  fun editEvent_setsNewValueOnDocument_withSameId() = runTest {
    whenever(document.set(any<Event>())).thenReturn(voidTask())

    val updated =
        Event(
            uid = "E1",
            title = "Updated",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("L", 0.0, 0.0),
            tags = listOf("x"),
            public = false,
            ownerId = "o",
            imageUrl = null,
            capacity = 5,
            attendeeCount = 2)

    repo.editEvent("E1", updated)

    argumentCaptor<Event>().apply {
      verify(document).set(capture())
      assertEquals("E1", firstValue.uid)
      assertEquals("Updated", firstValue.title)
      assertFalse(firstValue.public)
    }
  }

  @Test
  fun deleteEvent_callsDeleteOnDocument() = runTest {
    whenever(document.delete()).thenReturn(voidTask())
    repo.deleteEvent("E1")
    verify(document).delete()
  }

  @Test
  fun getEventsByParticipant_returnsMatchingEvents() = runTest {
    val e1 =
        Event(
            uid = "",
            title = "Event 1",
            url = "u1",
            description = "d1",
            date = Timestamp.now(),
            location = Location("Zurich", 8.5417, 47.3769),
            tags = listOf("music"),
            public = true,
            ownerId = "owner1",
            imageUrl = null,
            capacity = 50,
            attendeeCount = 2,
            participantIds = listOf("user1", "user2"))

    val e2 =
        Event(
            uid = "",
            title = "Event 2",
            url = "u2",
            description = "d2",
            date = Timestamp.now(),
            location = Location("Zurich", 8.5417, 47.3769),
            tags = listOf("sports"),
            public = true,
            ownerId = "owner2",
            imageUrl = null,
            capacity = 30,
            attendeeCount = 1,
            participantIds = listOf("user1", "user3"))

    val snap = qs(doc("E1", e1), doc("E2", e2))

    whenever(collection.whereArrayContains(eq("participantIds"), eq("user1"))).thenReturn(query)
    whenever(query.orderBy(eq("date"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByParticipant("user1")
    assertEquals(2, result.size)
    assertEquals(listOf("E1", "E2"), result.map { it.uid })
    assertTrue(result.all { it.participantIds.contains("user1") })
  }

  @Test
  fun addEvent_autoAddsOwnerToParticipants_whenNotPresent() = runTest {
    whenever(document.id).thenReturn("NEWID")
    whenever(document.set(any<Event>())).thenReturn(voidTask())

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
            attendeeCount = 0,
            participantIds = listOf("user1", "user2"))

    repo.addEvent(input)

    argumentCaptor<Event>().apply {
      verify(document).set(capture())
      assertEquals("NEWID", firstValue.uid)
      assertEquals("New Event", firstValue.title)
      assertTrue(firstValue.participantIds.contains("owner123"))
      assertEquals(3, firstValue.participantIds.size)
    }
  }

  @Test
  fun addEvent_doesNotDuplicateOwnerInParticipants_whenAlreadyPresent() = runTest {
    whenever(document.id).thenReturn("NEWID")
    whenever(document.set(any<Event>())).thenReturn(voidTask())

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
            attendeeCount = 0,
            participantIds = listOf("user1", "owner123", "user2"))

    repo.addEvent(input)

    argumentCaptor<Event>().apply {
      verify(document).set(capture())
      assertEquals(3, firstValue.participantIds.size)
      assertEquals(1, firstValue.participantIds.count { it == "owner123" })
    }
  }

  @Test
  fun getEventsByTags_returnsAllWhenEmptyTags() = runTest {
    val e =
        Event(
            uid = "E1",
            title = "Title",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("ZRH", 0.0, 0.0),
            tags = listOf("music"),
            public = true,
            ownerId = "owner",
            imageUrl = null,
            capacity = 10,
            attendeeCount = 1)
    val snap = qs(doc("E1", e))
    whenever(collection.orderBy("date")).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByTags(emptyList())
    assertEquals(1, result.size)
    assertEquals("E1", result[0].uid)
  }

  @Test
  fun getEventsOnDay_returnsEventsWithinRange() = runTest {
    val start = Timestamp.now()
    val end = Timestamp(start.seconds + 3600, 0)

    val e1 =
        Event(
            uid = "E1",
            title = "Event 1",
            url = "u",
            description = "d",
            date = start,
            location = Location("ZRH", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "owner",
            imageUrl = null,
            capacity = 5,
            attendeeCount = 0)
    val snap = qs(doc("E1", e1))

    whenever(collection.whereGreaterThanOrEqualTo(eq("date"), eq(start))).thenReturn(query)
    whenever(query.whereLessThan(eq("date"), eq(end))).thenReturn(query)
    whenever(query.orderBy(eq("date"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsOnDay(start, end)
    assertEquals(1, result.size)
    assertEquals("E1", result[0].uid)
  }

  @Test
  fun getEventsByOwner_returnsMatchingEvents() = runTest {
    val e1 =
        Event(
            uid = "E1",
            title = "Event 1",
            url = "u",
            description = "d",
            date = Timestamp.now(),
            location = Location("ZRH", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "owner1",
            imageUrl = null,
            capacity = 5,
            attendeeCount = 0)
    val snap = qs(doc("E1", e1))

    whenever(collection.whereEqualTo(eq("ownerId"), eq("owner1"))).thenReturn(query)
    whenever(query.orderBy(eq("date"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByOwner("owner1")
    assertEquals(1, result.size)
    assertEquals("E1", result[0].uid)
  }
}

package com.swent.mapin.model

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.swent.mapin.model.event.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryFirestore
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

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
            location = Location("Zurich", 8.5417, 47.3769),
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
            location = Location("Zurich", 8.54, 47.37),
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
            "",
            " Rock Night ",
            "u",
            "d",
            Timestamp.now(),
            Location("ZRH", 0.0, 0.0),
            listOf("m"),
            true,
            "a",
            null,
            1,
            0)
    val e2 =
        Event(
            "",
            "rock night",
            "u",
            "d",
            Timestamp.now(),
            Location("ZRH", 0.0, 0.0),
            listOf("m"),
            true,
            "b",
            null,
            1,
            0)
    val e3 =
        Event(
            "",
            "Jazz",
            "u",
            "d",
            Timestamp.now(),
            Location("ZRH", 0.0, 0.0),
            listOf("m"),
            true,
            "c",
            null,
            1,
            0)

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
}

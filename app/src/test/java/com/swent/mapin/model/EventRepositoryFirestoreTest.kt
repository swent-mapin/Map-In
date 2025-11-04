package com.swent.mapin.model

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
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
 * - Querying by participant IDs
 * - Ensuring owner is included in participant list when adding events
 * - Querying by tags with Firestore limits
 * - Querying events within a date range
 * - Managing saved events for users, including chunked queries
 */
@RunWith(MockitoJUnitRunner::class)
class EventRepositoryFirestoreTest {

  @Mock lateinit var db: FirebaseFirestore
  @Mock lateinit var collection: CollectionReference
  @Mock lateinit var document: DocumentReference
  @Mock lateinit var query: Query

  @Mock lateinit var usersCollection: CollectionReference
  @Mock lateinit var userDoc: DocumentReference
  @Mock lateinit var savedCollection: CollectionReference
  @Mock lateinit var savedDoc: DocumentReference

  // For whereIn chunking
  @Mock lateinit var queryChunk1: Query
  @Mock lateinit var queryChunk2: Query
  private lateinit var repo: EventRepositoryFirestore

  @Before
  fun setUp() {
    repo = EventRepositoryFirestore(db)

    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(collection)
    whenever(collection.document()).thenReturn(document)
    whenever(collection.document(any<String>())).thenReturn(document)
    whenever(db.collection("users")).thenReturn(usersCollection)
    whenever(usersCollection.document(any())).thenReturn(userDoc)
    whenever(userDoc.collection("savedEvents")).thenReturn(savedCollection)
    // Many tests expect ordering on the savedEvents subcollection; return a Query mock for orderBy
    whenever(savedCollection.orderBy(eq("savedAt"))).thenReturn(query)
    whenever(savedCollection.document(any())).thenReturn(savedDoc)
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

  private fun <T> taskException(e: Exception): Task<T> = Tasks.forException(e)

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
            capacity = 50)

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
            capacity = 10)
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
            capacity = 1)
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
            capacity = 1)
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
            capacity = 1)

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
            capacity = 1)

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
            capacity = 5)

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
            capacity = 10)
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
            capacity = 5)
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
            capacity = 5)
    val snap = qs(doc("E1", e1))

    whenever(collection.whereEqualTo(eq("ownerId"), eq("owner1"))).thenReturn(query)
    whenever(query.orderBy(eq("date"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getEventsByOwner("owner1")
    assertEquals(1, result.size)
    assertEquals("E1", result[0].uid)
  }
  // ======== Saved Events: getSavedEventIds ========

  @Test
  fun getSavedEventIds_returnsDocumentIds() = runTest {
    val snap =
        qs(
            // Only the id matters for this call
            doc("E1", null),
            doc("E2", null))
    whenever(query.get()).thenReturn(taskOf(snap))

    val ids = repo.getSavedEventIds("user123")
    assertEquals(setOf("E1", "E2"), ids)
  }

  @Test
  fun getSavedEventIds_handlesFailure_returnsEmptySet() = runTest {
    whenever(query.get()).thenReturn(taskException<QuerySnapshot>(RuntimeException("boom")))
    val ids = repo.getSavedEventIds("user123")
    assertEquals(emptySet<String>(), ids)
  }

  // ======== Saved Events: save & unsave ========

  @Test
  fun saveEventForUser_writesDocAndReturnsTrue() = runTest {
    whenever(savedDoc.set(any<Map<String, Any?>>())).thenReturn(Tasks.forResult(null))
    val ok = repo.saveEventForUser("user123", "E1")
    assertTrue(ok)
    argumentCaptor<Map<String, Any?>>().apply {
      verify(savedDoc).set(capture())
      assertTrue(firstValue.containsKey("savedAt"))
    }
  }

  @Test
  fun saveEventForUser_failureReturnsFalse() = runTest {
    whenever(savedDoc.set(any<Map<String, Any?>>()))
        .thenReturn(taskException<Void>(RuntimeException("write failed")))
    val ok = repo.saveEventForUser("user123", "E1")
    assertFalse(ok)
  }

  @Test
  fun unsaveEventForUser_deletesDocAndReturnsTrue() = runTest {
    whenever(savedDoc.delete()).thenReturn(Tasks.forResult(null))
    val ok = repo.unsaveEventForUser("user123", "E1")
    assertTrue(ok)
    verify(savedDoc).delete()
  }

  @Test
  fun unsaveEventForUser_failureReturnsFalse() = runTest {
    whenever(savedDoc.delete()).thenReturn(taskException<Void>(RuntimeException("delete failed")))
    val ok = repo.unsaveEventForUser("user123", "E1")
    assertFalse(ok)
  }

  // ======== Saved Events: getSavedEvents (no chunking) ========

  @Test
  fun getSavedEvents_emptyWhenNoIds() = runTest {
    val emptySnap = qs() // no docs in savedEvents subcollection
    whenever(query.get()).thenReturn(taskOf(emptySnap))

    val out = repo.getSavedEvents("user123")
    assertEquals(emptyList<Event>(), out)
  }

  @Test
  fun getSavedEvents_fetchesByIds_andSortsByDateAscending() = runTest {
    // saved ids: E2, E1
    val savedIdsSnap = qs(doc("E2", null), doc("E1", null))
    whenever(query.get()).thenReturn(taskOf(savedIdsSnap))

    // events collection lookups (whereIn single chunk)
    val e1 =
        Event(
            uid = "E1",
            title = "T1",
            url = "u",
            description = "d",
            date = Timestamp(100L, 0), // older
            location = Location("X", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "o",
            imageUrl = null,
            capacity = 1)
    val e2 =
        Event(
            uid = "E2",
            title = "T2",
            url = "u",
            description = "d",
            date = Timestamp(200L, 0), // newer
            location = Location("X", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "o",
            imageUrl = null,
            capacity = 1)

    val chunkDocs: Array<DocumentSnapshot> = arrayOf(doc("E2", e2), doc("E1", e1))
    val chunkSnap = qs(*chunkDocs)

    // Stub whereIn with the exact saved ids chunk
    whenever(collection.whereIn(eq(FieldPath.documentId()), eq(listOf("E2", "E1"))))
        .thenReturn(queryChunk1)
    whenever(queryChunk1.get()).thenReturn(taskOf(chunkSnap))

    val out = repo.getSavedEvents("user123")
    // Repository sorts results by saved-order (ids), so expect E2 then E1
    assertEquals(listOf("E2", "E1"), out.map { it.uid })
  }

  @Test
  fun getSavedEvents_chunksWhereInQueries_whenOverLimit() = runTest {
    // Build LIMIT+1 ids to force chunking (limit mirrored from repo)
    val limit = 10
    val ids = (1..(limit + 1)).map { "E$it" }

    // saved IDs snapshot (subcollection)
    val savedDocs: Array<DocumentSnapshot> = ids.map { id -> doc(id, null) }.toTypedArray()
    val savedIdsSnapshot = qs(*savedDocs)
    whenever(query.get()).thenReturn(taskOf(savedIdsSnapshot))

    // Prepare first chunk (E1..E10)
    val firstChunkIds = (1..limit).map { "E$it" }
    val firstChunkEvents =
        firstChunkIds.map { id ->
          Event(
              uid = id,
              title = id,
              url = "u",
              description = "d",
              date = Timestamp(100L + id.removePrefix("E").toLong(), 0),
              location = Location("X", 0.0, 0.0),
              tags = emptyList(),
              public = true,
              ownerId = "o",
              imageUrl = null,
              capacity = 1)
        }
    val firstDocs = firstChunkEvents.map { ev -> doc(ev.uid, ev) }.toTypedArray()
    val firstSnap = qs(*firstDocs)

    // Second chunk (E11)
    val e11 =
        Event(
            uid = "E11",
            title = "E11",
            url = "u",
            description = "d",
            date = Timestamp(1000L, 0),
            location = Location("X", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "o",
            imageUrl = null,
            capacity = 1)
    val secondSnap = qs(doc("E11", e11))

    // Make whereIn return different queries depending on the chunk argument
    whenever(collection.whereIn(eq(FieldPath.documentId()), any<List<String>>())).thenAnswer { inv
      ->
      val chunk = inv.getArgument<List<String>>(1)
      if (chunk.contains("E11") || chunk.size == 1) queryChunk2 else queryChunk1
    }
    whenever(queryChunk1.get()).thenReturn(taskOf(firstSnap))
    whenever(queryChunk2.get()).thenReturn(taskOf(secondSnap))

    val out = repo.getSavedEvents("user123")
    // verify contents (order not important) and no duplicates
    assertEquals(ids.size, out.size)
    assertEquals(ids.toSet(), out.map { it.uid }.toSet())

    // Capture the whereIn calls to ensure repo chunked the ids without duplicating
    val captor = argumentCaptor<List<String>>()
    verify(collection, org.mockito.kotlin.times(2))
        .whereIn(eq(FieldPath.documentId()), captor.capture())
    val calls = captor.allValues
    // merged ids from both calls should equal the original ids set
    val merged = calls.flatten().toSet()
    assertEquals(ids.toSet(), merged)
  }
}

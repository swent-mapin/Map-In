package com.swent.mapin.model.memory

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

// Assisted by AI

@RunWith(MockitoJUnitRunner::class)
class MemoryRepositoryFirestoreTest {

  @Mock lateinit var db: FirebaseFirestore
  @Mock lateinit var collection: CollectionReference
  @Mock lateinit var document: DocumentReference
  @Mock lateinit var query: Query

  private lateinit var repo: MemoryRepositoryFirestore

  @Before
  fun setUp() {
    repo = MemoryRepositoryFirestore(db)

    whenever(db.collection(MEMORIES_COLLECTION_PATH)).thenReturn(collection)
    whenever(collection.document()).thenReturn(document)
    whenever(collection.document(any<String>())).thenReturn(document)
  }

  private fun <T> taskOf(value: T): Task<T> = Tasks.forResult(value)

  private fun voidTask(): Task<Void> = Tasks.forResult(null)

  private fun doc(id: String, memory: Memory?): DocumentSnapshot {
    val d = mock<DocumentSnapshot>()
    whenever(d.id).thenReturn(id)
    whenever(d.toObject(Memory::class.java)).thenReturn(memory)
    return d
  }

  private fun qs(vararg docs: DocumentSnapshot): QuerySnapshot {
    val snap = mock<QuerySnapshot>()
    whenever(snap.documents).thenReturn(docs.toList())
    return snap
  }

  @Test
  fun getNewUid_returnsNonEmpty() {
    whenever(document.id).thenReturn("mem123")
    val id = repo.getNewUid()
    assertEquals("mem123", id)
  }

  @Test
  fun getMemory_success_returnsMappedMemory() = runTest {
    val memory =
        Memory(
            uid = "",
            title = "Beach Day",
            description = "Fun at the beach",
            ownerId = "user1",
            eventId = "event1",
            isPublic = true,
            createdAt = Timestamp.now(),
            mediaUrls = listOf("url1"),
            taggedUserIds = listOf("user2"))

    val snap: DocumentSnapshot = mock()
    whenever(snap.id).thenReturn("M1")
    whenever(snap.toObject(Memory::class.java)).thenReturn(memory)
    whenever(document.get()).thenReturn(taskOf(snap))

    val out = repo.getMemory("M1")
    assertEquals("M1", out.uid)
    assertEquals("Beach Day", out.title)
    assertEquals("user1", out.ownerId)
  }

  @Test(expected = NoSuchElementException::class)
  fun getMemory_notFound_throws() = runTest {
    val snap: DocumentSnapshot = mock()
    whenever(snap.toObject(Memory::class.java)).thenReturn(null)
    whenever(document.get()).thenReturn(taskOf(snap))

    repo.getMemory("missing")
  }

  @Test
  fun getAllMemories_mapsValidDocs_andSkipsBadOnes() = runTest {
    val valid =
        Memory(
            uid = "",
            title = "Good Memory",
            description = "Description",
            ownerId = "owner1",
            createdAt = Timestamp.now())
    val goodDoc = doc("GOOD", valid)

    val badDoc =
        mock<DocumentSnapshot>().also {
          whenever(it.id).thenReturn("BAD")
          whenever(it.toObject(Memory::class.java)).thenThrow(RuntimeException("boom"))
        }

    val snap = qs(goodDoc, badDoc)

    whenever(collection.orderBy(eq("createdAt"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val list = repo.getAllMemories()
    assertEquals(1, list.size)
    assertEquals("GOOD", list[0].uid)
  }

  @Test
  fun getMemoriesByEvent_returnsMatchingMemories() = runTest {
    val m1 =
        Memory(
            uid = "",
            title = "Memory 1",
            description = "Description",
            ownerId = "user1",
            eventId = "event1",
            createdAt = Timestamp.now())
    val m2 =
        Memory(
            uid = "",
            title = "Memory 2",
            description = "Description",
            ownerId = "user2",
            eventId = "event1",
            createdAt = Timestamp.now())

    val snap = qs(doc("1", m1), doc("2", m2))

    whenever(collection.whereEqualTo(eq("eventId"), eq("event1"))).thenReturn(query)
    whenever(query.orderBy(eq("createdAt"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getMemoriesByEvent("event1")
    assertEquals(2, result.size)
    assertEquals(listOf("1", "2"), result.map { it.uid })
  }

  @Test
  fun getPublicMemoriesByEvent_returnsOnlyPublicMemories() = runTest {
    val m1 =
        Memory(
            uid = "",
            title = "Public Memory",
            description = "Description",
            ownerId = "user1",
            eventId = "event1",
            isPublic = true,
            createdAt = Timestamp.now())

    val snap = qs(doc("1", m1))

    whenever(collection.whereEqualTo(eq("eventId"), eq("event1"))).thenReturn(query)
    whenever(query.whereEqualTo(eq("isPublic"), eq(true))).thenReturn(query)
    whenever(query.orderBy(eq("createdAt"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getPublicMemoriesByEvent("event1")
    assertEquals(1, result.size)
    assertTrue(result[0].isPublic)
  }

  @Test
  fun getMemoriesByOwner_returnsMatchingMemories() = runTest {
    val m1 =
        Memory(
            uid = "",
            title = "Memory 1",
            description = "Description",
            ownerId = "owner1",
            createdAt = Timestamp.now())
    val m2 =
        Memory(
            uid = "",
            title = "Memory 2",
            description = "Description",
            ownerId = "owner1",
            createdAt = Timestamp.now())

    val snap = qs(doc("1", m1), doc("2", m2))

    whenever(collection.whereEqualTo(eq("ownerId"), eq("owner1"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getMemoriesByOwner("owner1")
    assertEquals(2, result.size)
    assertTrue(result.all { it.ownerId == "owner1" })
  }

  @Test
  fun getMemoriesByTimeRange_returnsMemoriesInRange() = runTest {
    val now = Timestamp.now()
    val past = Timestamp(now.seconds - 3600, 0)
    val future = Timestamp(now.seconds + 3600, 0)

    val memory =
        Memory(
            uid = "",
            title = "Recent Memory",
            description = "Description",
            ownerId = "user1",
            createdAt = now)

    val snap = qs(doc("1", memory))

    whenever(collection.whereGreaterThanOrEqualTo(eq("createdAt"), eq(past))).thenReturn(query)
    whenever(query.whereLessThan(eq("createdAt"), eq(future))).thenReturn(query)
    whenever(query.orderBy(eq("createdAt"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getMemoriesByTimeRange(past, future)
    assertEquals(1, result.size)
  }

  @Test
  fun getMemoriesByTaggedUser_returnsMatchingMemories() = runTest {
    val memory =
        Memory(
            uid = "",
            title = "Tagged Memory",
            description = "Description",
            ownerId = "user1",
            createdAt = Timestamp.now(),
            taggedUserIds = listOf("user2", "user3"))

    val snap = qs(doc("1", memory))

    whenever(collection.whereArrayContains(eq("taggedUserIds"), eq("user2"))).thenReturn(query)
    whenever(query.orderBy(eq("createdAt"))).thenReturn(query)
    whenever(query.get()).thenReturn(taskOf(snap))

    val result = repo.getMemoriesByTaggedUser("user2")
    assertEquals(1, result.size)
    assertTrue(result[0].taggedUserIds.contains("user2"))
  }

  @Test
  fun addMemory_withBlankUid_generatesAndPersists_withFilledUid() = runTest {
    whenever(document.id).thenReturn("NEWID")
    whenever(document.set(any<Memory>())).thenReturn(voidTask())

    val input =
        Memory(
            uid = "",
            title = "New Memory",
            description = "Description",
            ownerId = "user1",
            createdAt = Timestamp.now())

    repo.addMemory(input)

    argumentCaptor<Memory>().apply {
      verify(document).set(capture())
      assertEquals("NEWID", firstValue.uid)
      assertEquals("New Memory", firstValue.title)
    }
  }

  @Test
  fun addMemory_withProvidedUid_keepsUid() = runTest {
    whenever(document.set(any<Memory>())).thenReturn(voidTask())

    val input =
        Memory(
            uid = "custom123",
            title = "Custom ID Memory",
            description = "Description",
            ownerId = "user1",
            createdAt = Timestamp.now())

    repo.addMemory(input)

    argumentCaptor<Memory>().apply {
      verify(document).set(capture())
      assertEquals("custom123", firstValue.uid)
      assertEquals("Custom ID Memory", firstValue.title)
    }
  }

  @Test
  fun editMemory_setsNewValueOnDocument_withSameId() = runTest {
    whenever(document.set(any<Memory>())).thenReturn(voidTask())

    val updated =
        Memory(
            uid = "M1",
            title = "Updated Title",
            description = "Updated description",
            ownerId = "user1",
            eventId = "event1",
            isPublic = false,
            createdAt = Timestamp.now())

    repo.editMemory("M1", updated)

    argumentCaptor<Memory>().apply {
      verify(document).set(capture())
      assertEquals("M1", firstValue.uid)
      assertEquals("Updated Title", firstValue.title)
      assertEquals(false, firstValue.isPublic)
    }
  }

  @Test
  fun deleteMemory_callsDeleteOnDocument() = runTest {
    whenever(document.delete()).thenReturn(voidTask())
    repo.deleteMemory("M1")
    verify(document).delete()
  }
}

package com.swent.mapin.model.memory

import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Assisted by AI

class LocalMemoryRepositoryTest {

  private lateinit var repository: LocalMemoryRepository

  @Before
  fun setup() {
    repository = LocalMemoryRepository()
  }

  @Test
  fun getNewUid_returnsNonEmpty() {
    val uid = repository.getNewUid()
    assertTrue(uid.isNotBlank())
  }

  @Test
  fun getNewUid_returnsUniqueIds() {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()
    assertNotEquals(uid1, uid2)
  }

  @Test
  fun getAllMemories_returnsAllMemories() = runTest {
    val memories = repository.getAllMemories()
    assertTrue(memories.isNotEmpty())
    assertTrue(memories.any { it.uid == "mem1" })
  }

  @Test
  fun getMemory_existingId_returnsMemory() = runTest {
    val memory = repository.getMemory("mem1")
    assertEquals("mem1", memory.uid)
    assertEquals("Amazing Beach Day", memory.title)
  }

  @Test(expected = NoSuchElementException::class)
  fun getMemory_nonExistentId_throws() = runTest { repository.getMemory("nonexistent") }

  @Test
  fun getMemoriesByEvent_returnsMatchingMemories() = runTest {
    val memories = repository.getMemoriesByEvent("2")
    assertEquals(1, memories.size)
    assertEquals("mem1", memories[0].uid)
  }

  @Test
  fun getMemoriesByEvent_noMatches_returnsEmpty() = runTest {
    val memories = repository.getMemoriesByEvent("nonexistent")
    assertTrue(memories.isEmpty())
  }

  @Test
  fun getPublicMemoriesByEvent_returnsOnlyPublicMemories() = runTest {
    val memories = repository.getPublicMemoriesByEvent("1")
    assertTrue(memories.all { it.isPublic })
    assertTrue(memories.any { it.uid == "mem2" })
  }

  @Test
  fun getMemoriesByOwner_returnsMatchingMemories() = runTest {
    val memories = repository.getMemoriesByOwner("user1")
    assertEquals(2, memories.size)
    assertTrue(memories.all { it.ownerId == "user1" })
  }

  @Test
  fun getMemoriesByTimeRange_returnsMemoriesInRange() = runTest {
    val now = Timestamp.now()
    val past = Timestamp(now.seconds - 3600, 0)
    val future = Timestamp(now.seconds + 3600, 0)

    val memories = repository.getMemoriesByTimeRange(past, future)
    assertTrue(memories.isNotEmpty())
  }

  @Test
  fun getMemoriesByTaggedUser_returnsMatchingMemories() = runTest {
    val memories = repository.getMemoriesByTaggedUser("user1")
    assertTrue(memories.any { it.uid == "mem4" })
  }

  @Test
  fun addMemory_withBlankUid_generatesUid() = runTest {
    val newMemory =
        Memory(
            uid = "",
            title = "New Memory",
            description = "Test description",
            ownerId = "testUser",
            createdAt = Timestamp.now())

    repository.addMemory(newMemory)

    val allMemories = repository.getAllMemories()
    val addedMemory = allMemories.find { it.title == "New Memory" }
    assertTrue(addedMemory != null)
    assertTrue(addedMemory?.uid?.isNotBlank() == true)
  }

  @Test
  fun addMemory_withProvidedUid_keepsUid() = runTest {
    val newMemory =
        Memory(
            uid = "custom123",
            title = "Custom ID Memory",
            description = "Test description",
            ownerId = "testUser",
            createdAt = Timestamp.now())

    repository.addMemory(newMemory)

    val retrieved = repository.getMemory("custom123")
    assertEquals("custom123", retrieved.uid)
    assertEquals("Custom ID Memory", retrieved.title)
  }

  @Test
  fun editMemory_updatesExistingMemory() = runTest {
    val updated =
        Memory(
            uid = "mem1",
            title = "Updated Title",
            description = "Updated description",
            ownerId = "user1",
            eventId = "2",
            isPublic = false,
            createdAt = Timestamp.now())

    repository.editMemory("mem1", updated)

    val retrieved = repository.getMemory("mem1")
    assertEquals("Updated Title", retrieved.title)
    assertEquals("Updated description", retrieved.description)
  }

  @Test(expected = NoSuchElementException::class)
  fun editMemory_nonExistentId_throws() = runTest {
    val memory =
        Memory(
            uid = "nonexistent",
            description = "Test",
            ownerId = "user1",
            createdAt = Timestamp.now())
    repository.editMemory("nonexistent", memory)
  }

  @Test
  fun deleteMemory_removesMemory() = runTest {
    repository.deleteMemory("mem1")

    try {
      repository.getMemory("mem1")
      throw AssertionError("Expected NoSuchElementException")
    } catch (e: NoSuchElementException) {}
  }

  @Test(expected = NoSuchElementException::class)
  fun deleteMemory_nonExistentId_throws() = runTest { repository.deleteMemory("nonexistent") }

  @Test
  fun addMemory_canRetrieve() = runTest {
    val memory =
        Memory(
            uid = "test123",
            title = "Test Memory",
            description = "Description",
            ownerId = "testUser",
            eventId = null,
            isPublic = true,
            createdAt = Timestamp.now(),
            mediaUrls = listOf("url1", "url2"),
            taggedUserIds = listOf("user1", "user2"))

    repository.addMemory(memory)

    val retrieved = repository.getMemory("test123")
    assertEquals("Test Memory", retrieved.title)
    assertEquals("Description", retrieved.description)
    assertEquals(2, retrieved.mediaUrls.size)
    assertEquals(2, retrieved.taggedUserIds.size)
  }
}

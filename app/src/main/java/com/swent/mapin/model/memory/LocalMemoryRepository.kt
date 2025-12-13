package com.swent.mapin.model.memory

import com.google.firebase.Timestamp

// Assisted by AI
/**
 * Local in-memory repository for Memory items. Useful for testing and development without Firestore
 * dependencies.
 */
class LocalMemoryRepository : MemoryRepository {
  private val memories = mutableMapOf<String, Memory>()
  private var idCounter = 1

  init {
    // Populate with sample data for testing
    val sampleMemories =
        listOf(
            Memory(
                uid = "mem1",
                title = "Amazing Beach Day",
                description = "Had an incredible time playing volleyball with friends!",
                eventId = "2",
                ownerId = "user1",
                public = true,
                createdAt = Timestamp.now(),
                mediaUrls = listOf("https://example.com/photo1.jpg"),
                taggedUserIds = listOf("user2", "user3")),
            Memory(
                uid = "mem2",
                title = "Summer Vibes",
                description =
                    "The festival was packed with amazing food and music. Best summer ever!",
                eventId = "1", // Summer Festival 2024
                ownerId = "user2",
                public = true,
                createdAt = Timestamp.now(),
                mediaUrls =
                    listOf("https://example.com/photo2.jpg", "https://example.com/photo3.jpg"),
                taggedUserIds = listOf("user1", "user4")),
            Memory(
                uid = "mem3",
                title = "Quiet Moment",
                description = "Just enjoying the sunset alone. Sometimes peace is all you need.",
                eventId = null, // Not linked to any event
                ownerId = "user1",
                public = false,
                createdAt = Timestamp.now(),
                mediaUrls = emptyList(),
                taggedUserIds = emptyList()),
            Memory(
                uid = "mem4",
                title = "Hiking Adventure",
                description =
                    "Reached the summit after 6 hours of hiking. The view was worth every step!",
                eventId = "3", // Mountain Hiking Adventure
                ownerId = "user3",
                public = true,
                createdAt = Timestamp.now(),
                mediaUrls = listOf("https://example.com/photo4.jpg"),
                taggedUserIds = listOf("user1")),
            Memory(
                uid = "mem5",
                title = "",
                description =
                    "Concert was absolutely mind-blowing! Best live performance I've seen.",
                eventId = "4", // Concert at Park
                ownerId = "user2",
                public = true,
                createdAt = Timestamp.now(),
                mediaUrls =
                    listOf("https://example.com/video1.mp4", "https://example.com/photo5.jpg"),
                taggedUserIds = listOf("user3", "user4", "user5")))

    // Add sample memories to the repository
    sampleMemories.forEach { memories[it.uid] = it }
    idCounter = sampleMemories.size + 1
  }

  override fun getNewUid(): String = "mem${idCounter++}"

  override suspend fun getAllMemories(): List<Memory> {
    return memories.values.sortedByDescending { it.createdAt }
  }

  override suspend fun getMemory(memoryId: String): Memory {
    return memories[memoryId]
        ?: throw NoSuchElementException("LocalMemoryRepository: Memory not found (id=$memoryId)")
  }

  override suspend fun getMemoriesByEvent(eventId: String): List<Memory> {
    return memories.values.filter { it.eventId == eventId }.sortedByDescending { it.createdAt }
  }

  override suspend fun getPublicMemoriesByEvent(eventId: String): List<Memory> {
    return memories.values
        .filter { it.eventId == eventId && it.public }
        .sortedByDescending { it.createdAt }
  }

  override suspend fun getMemoriesByOwner(ownerId: String): List<Memory> {
    return memories.values.filter { it.ownerId == ownerId }.sortedByDescending { it.createdAt }
  }

  override suspend fun getMemoriesByTimeRange(
      startTime: Timestamp,
      endTime: Timestamp
  ): List<Memory> {
    return memories.values
        .filter { memory -> memory.createdAt?.let { it >= startTime && it < endTime } ?: false }
        .sortedByDescending { it.createdAt }
  }

  override suspend fun getMemoriesByTaggedUser(userId: String): List<Memory> {
    return memories.values
        .filter { it.taggedUserIds.contains(userId) }
        .sortedByDescending { it.createdAt }
  }

  override suspend fun addMemory(memory: Memory) {
    val id = memory.uid.ifBlank { getNewUid() }
    memories[id] = memory.copy(uid = id)
  }

  override suspend fun editMemory(memoryId: String, newValue: Memory) {
    if (!memories.containsKey(memoryId)) {
      throw NoSuchElementException("LocalMemoryRepository: Memory not found (id=$memoryId)")
    }
    memories[memoryId] = newValue.copy(uid = memoryId)
  }

  override suspend fun deleteMemory(memoryId: String) {
    if (memories.remove(memoryId) == null) {
      throw NoSuchElementException("LocalMemoryRepository: Memory not found (id=$memoryId)")
    }
  }
}

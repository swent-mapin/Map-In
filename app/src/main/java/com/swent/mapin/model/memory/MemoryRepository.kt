package com.swent.mapin.model.memory

import com.google.firebase.Timestamp

interface MemoryRepository {
  /** Generates and returns a new unique identifier for a Memory item. */
  fun getNewUid(): String

  /**
   * Retrieves all Memory items from the repository.
   *
   * @return A list of all Memory items.
   */
  suspend fun getAllMemories(): List<Memory>

  /**
   * Retrieves a specific Memory item by its unique identifier.
   *
   * @param memoryId The unique identifier of the Memory item to retrieve.
   * @return The Memory item with the specified identifier.
   * @throws NoSuchElementException if the Memory item is not found.
   */
  suspend fun getMemory(memoryId: String): Memory

  /**
   * Retrieves all Memory items linked to a specific event.
   *
   * @param eventId The unique identifier of the event.
   * @return A list of Memory items linked to the specified event.
   */
  suspend fun getMemoriesByEvent(eventId: String): List<Memory>

  /**
   * Retrieves public Memory items linked to a specific event. Useful for displaying on event pages.
   *
   * @param eventId The unique identifier of the event.
   * @return A list of public Memory items linked to the specified event.
   */
  suspend fun getPublicMemoriesByEvent(eventId: String): List<Memory>

  /**
   * Retrieves Memory items created by the specified owner.
   *
   * @param ownerId The unique identifier of the owner.
   * @return A list of Memory items created by the specified owner.
   */
  suspend fun getMemoriesByOwner(ownerId: String): List<Memory>

  /**
   * Retrieves Memory items created within a specific time range.
   *
   * @param startTime The start timestamp of the range.
   * @param endTime The end timestamp of the range.
   * @return A list of Memory items created within the specified time range.
   */
  suspend fun getMemoriesByTimeRange(startTime: Timestamp, endTime: Timestamp): List<Memory>

  /**
   * Retrieves Memory items where a specific user is tagged.
   *
   * @param userId The unique identifier of the tagged user.
   * @return A list of Memory items where the specified user is tagged.
   */
  suspend fun getMemoriesByTaggedUser(userId: String): List<Memory>

  /**
   * Adds a new Memory item to the repository.
   *
   * @param memory The Memory item to add.
   */
  suspend fun addMemory(memory: Memory)

  /**
   * Edits an existing Memory item in the repository.
   *
   * @param memoryId The unique identifier of the Memory item to edit.
   * @param newValue The new value for the Memory item.
   * @throws NoSuchElementException if the Memory item is not found.
   */
  suspend fun editMemory(memoryId: String, newValue: Memory)

  /**
   * Deletes a Memory item from the repository.
   *
   * @param memoryId The unique identifier of the Memory item to delete.
   * @throws NoSuchElementException if the Memory item is not found.
   */
  suspend fun deleteMemory(memoryId: String)
}

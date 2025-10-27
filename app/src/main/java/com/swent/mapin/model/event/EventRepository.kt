package com.swent.mapin.model.event

import com.google.firebase.Timestamp

interface EventRepository {
  /** Generates and returns a new unique identifier for an Event item. */
  fun getNewUid(): String

  /**
   * Retrieves all Event items from the repository.
   *
   * @return A list of all Event items.
   */
  suspend fun getAllEvents(): List<Event>

  /**
   * Retrieves a specific Event item by its unique identifier.
   *
   * @param eventID The unique identifier of the Event item to retrieve.
   * @return The Event item with the specified identifier.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun getEvent(eventID: String): Event

  /**
   * Retrieves Event items that match any of the specified tags.
   *
   * @param tags A list of tags to filter Event items.
   * @return A list of Event items that match any of the specified tags or all Event items if the
   *   list is empty.
   */
  suspend fun getEventsByTags(tags: List<String>): List<Event>

  /**
   * Retrieves Event items occurring within the specified day.
   *
   * @param dayStart The start timestamp of the day.
   * @param dayEnd The end timestamp of the day.
   * @return A list of Event items occurring within the specified day.
   */
  suspend fun getEventsOnDay(dayStart: Timestamp, dayEnd: Timestamp): List<Event>

  /**
   * Retrieves Event items created by the specified owner.
   *
   * @param ownerId The unique identifier of the owner.
   * @return A list of Event items created by the specified owner.
   */
  suspend fun getEventsByOwner(ownerId: String): List<Event>

  /**
   * Retrieves Event items that match the specified title.
   *
   * @param title The title to filter Event items.
   * @return A list of Event items that match the specified title.
   */
  suspend fun getEventsByTitle(title: String): List<Event>

  /**
   * Retrieves Event items where the specified user is a participant.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items where the specified user is participating.
   */
  suspend fun getEventsByParticipant(userId: String): List<Event>

  /**
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  suspend fun addEvent(event: Event)

  /**
   * Edits an existing Event item in the repository.
   *
   * @param eventID The unique identifier of the Event item to edit.
   * @param newValue The new value for the Event item.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun editEvent(eventID: String, newValue: Event)

  /**
   * Deletes a Event item from the repository.
   *
   * @param eventID The unique identifier of the Event item to delete.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun deleteEvent(eventID: String)

  /**
   * Retrieves the IDs of Event items saved by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A set of IDs of Event items saved by the specified user.
   */
  suspend fun getSavedEventIds(userId: String): Set<String>
  /**
   * Retrieves Event items saved by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items saved by the specified user.
   */
  suspend fun getSavedEvents(userId: String): List<Event>

    /**
     * Saves an Event item for the specified user.
     *
     * @param userId The unique identifier of the user.
     * @param eventId The unique identifier of the Event item to save.
     * @return True if the Event item was successfully saved for the user, false otherwise.
     */
  suspend fun saveEventForUser(userId: String, eventId: String): Boolean
    /**
     * Removes a saved Event item for the specified user.
     *
     * @param userId The unique identifier of the user.
     * @param eventId The unique identifier of the Event item to remove.
     * @return True if the Event item was successfully removed from the user's saved list, false
     *   otherwise.
     */
  suspend fun unsaveEventForUser(userId: String, eventId: String): Boolean
}

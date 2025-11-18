package com.swent.mapin.model.event

import com.swent.mapin.ui.filters.Filters

/** Repository interface for managing Event items in the MapIn application. */
interface EventRepository {
  /**
   * Generates and returns a new unique identifier for an Event item.
   *
   * @return A unique string identifier.
   */
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
   * @param eventId The unique identifier of the Event item to retrieve.
   * @return The Event item with the specified identifier.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun getEvent(eventId: String): Event

  /**
   * Retrieves Event items based on the specified filters.
   *
   * @param filters The filtering criteria (e.g., tags, date range, location, etc.).
   * @return A list of Event items matching the filters.
   */
  suspend fun getFilteredEvents(filters: Filters): List<Event>

  /**
   * Retrieves Event items whose titles match the specified search query (case-insensitive).
   *
   * @param title The search query to match against event titles.
   * @param filters Additional filtering criteria to apply.
   * @return A list of Event items whose titles contain the query.
   */
  suspend fun getSearchedEvents(title: String, filters: Filters): List<Event>

  /**
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  suspend fun addEvent(event: Event)

  /**
   * Edits an existing Event item in the repository.
   *
   * @param eventId The unique identifier of the Event item to edit.
   * @param newValue The updated Event item.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun editEvent(eventId: String, newValue: Event)

  /**
   * Deletes an Event item from the repository.
   *
   * @param eventId The unique identifier of the Event item to delete.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun deleteEvent(eventId: String)

  /**
   * Retrieves the IDs of Event items saved by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A set of IDs of Event items saved by the user.
   */
  suspend fun getSavedEventIds(userId: String): Set<String>

  /**
   * Retrieves Event items saved by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items saved by the user.
   */
  suspend fun getSavedEvents(userId: String): List<Event>

  /**
   * Saves an Event item for the specified user.
   *
   * @param userId The unique identifier of the user.
   * @param eventId The unique identifier of the Event item to save.
   * @return True if the Event item was successfully saved, false if already saved.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun saveEventForUser(userId: String, eventId: String): Boolean

  /**
   * Removes a saved Event item for the specified user.
   *
   * @param userId The unique identifier of the user.
   * @param eventId The unique identifier of the Event item to remove.
   * @return True if the Event item was successfully removed, false if not saved.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun unsaveEventForUser(userId: String, eventId: String): Boolean

  /**
   * Retrieves Event items owned by the specified user.
   *
   * @param ownerId The unique identifier of the owner.
   * @return A list of Event items owned by the user.
   */
  suspend fun getEventsByOwner(ownerId: String): List<Event>
}

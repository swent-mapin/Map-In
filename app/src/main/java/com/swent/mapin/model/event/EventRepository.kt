package com.swent.mapin.model.event

import com.google.firebase.firestore.ListenerRegistration
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
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  suspend fun addEvent(event: Event)

  /**
   * Edits an existing Event content in the repository. This is limited to the event owner and to
   * not restrict other users.
   *
   * @param eventId The unique identifier of the Event item to edit.
   * @param newValue The updated Event item.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun editEventAsOwner(eventId: String, newValue: Event)

  /**
   * Edits an existing Event content in the repository. This is limited to updating one's
   * participation to the event.
   *
   * @param eventId The unique identifier of the Event item to edit.
   * @param userId The unique identifier of the user performing the action.
   * @param join Boolean indicating whether the user is joining (true) or leaving (false) the event.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun editEventAsUser(eventId: String, userId: String, join: Boolean)

  /**
   * Deletes an Event item from the repository.
   *
   * @param eventId The unique identifier of the Event item to delete.
   * @throws NoSuchElementException if the Event item is not found.
   */
  suspend fun deleteEvent(eventId: String)

  /**
   * Retrieves Event items saved by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items saved by the user.
   */
  suspend fun getSavedEvents(userId: String): List<Event>

  /**
   * Retrieves Event items joined by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items joined by the user.
   */
  suspend fun getJoinedEvents(userId: String): List<Event>

  /**
   * Retrieves Event items owned by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items owned by the user.
   */
  suspend fun getOwnedEvents(userId: String): List<Event>

  /**
   * Listens for real-time updates to Event items based on the specified filters.
   *
   * @param filters The filtering criteria (e.g., tags, date range, location, etc.).
   * @param onUpdate A callback function that is invoked with three lists of Event items:
   *     - The first list contains newly added events.
   *     - The second list contains updated events.
   *     - The third list contains removed events.
   */
  fun listenToFilteredEvents(
      filters: Filters,
      onUpdate: (List<Event>, List<Event>, List<Event>) -> Unit
  ): ListenerRegistration
}

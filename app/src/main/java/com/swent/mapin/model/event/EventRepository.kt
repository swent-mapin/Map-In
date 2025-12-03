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
   * @param userId The unique identifier of the user requesting the events (to identify his
   *   friends).
   * @return A list of Event items matching the filters.
   */
  suspend fun getFilteredEvents(filters: Filters, userId: String): List<Event>

  /**
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  suspend fun addEvent(event: Event)

  /**
   * Updates an existing event as its owner.
   *
   * Only the event owner is allowed to call this method. It can modify any event field except:
   * - uid (ignored)
   * - participantIds (cannot be changed by owner)
   * - public flag (cannot be changed from `true` to `false`)
   * - capacity cannot be reduced below the current number of participants
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

  /**
   * Listen to real-time changes in the user's saved events.
   *
   * BEHAVIOR:
   * - On first invocation after registration, ALL existing events in Firestore will be reported as
   *   "added" in the onUpdate callback. This is by design to sync initial state.
   * - Subsequent invocations report only actual changes (additions, modifications, deletions).
   * - Callers do NOT need to call getSavedEvents() separately - this listener provides complete
   *   state synchronization.
   *
   * USAGE:
   * 1. Register this listener BEFORE accessing any saved events
   * 2. The first callback will populate your initial state with all existing events
   * 3. Further callbacks will update your state incrementally
   *
   * TECHNICAL DETAILS:
   * - Callbacks are delivered on a Firestore background thread (not the Android main thread).
   * - In case of transient errors, empty updates may be emitted; permanent failures are logged.
   * - Removed events are represented by a minimal Event object (only uid is guaranteed valid).
   *
   * @param userId The user ID
   * @param onUpdate Callback receiving changes: first call contains all existing events as "added",
   *   subsequent calls contain only incremental changes
   * @return ListenerRegistration to remove the listener
   */
  fun listenToSavedEvents(
      userId: String,
      onUpdate: (List<Event>, List<Event>, List<String>) -> Unit
  ): ListenerRegistration

  /**
   * Listen to real-time changes in the user's owned events.
   *
   * BEHAVIOR:
   * - On first invocation after registration, ALL existing events in Firestore will be reported as
   *   "added" in the onUpdate callback. This is by design to sync initial state.
   * - Subsequent invocations report only actual changes (additions, modifications, deletions).
   * - Callers do NOT need to call getOwnedEvents() separately - this listener provides complete
   *   state synchronization.
   *
   * USAGE:
   * 1. Register this listener BEFORE accessing any owned events
   * 2. The first callback will populate your initial state with all existing events
   * 3. Further callbacks will update your state incrementally
   *
   * TECHNICAL DETAILS:
   * - Callbacks are delivered on a Firestore background thread (not the Android main thread).
   * - In case of transient errors, empty updates may be emitted; permanent failures are logged.
   * - Removed events are represented by a minimal Event object (only uid is guaranteed valid).
   *
   * @param userId The user ID
   * @param onUpdate Callback receiving changes: first call contains all existing events as "added",
   *   subsequent calls contain only incremental changes
   * @return ListenerRegistration to remove the listener
   */
  fun listenToOwnedEvents(
      userId: String,
      onUpdate: (List<Event>, List<Event>, List<String>) -> Unit
  ): ListenerRegistration

  /**
   * Listen to real-time changes in the user's joined events.
   *
   * BEHAVIOR:
   * - On first invocation after registration, ALL existing events in Firestore will be reported as
   *   "added" in the onUpdate callback. This is by design to sync initial state.
   * - Subsequent invocations report only actual changes (additions, modifications, deletions).
   * - Callers do NOT need to call getJoinedEvents() separately - this listener provides complete
   *   state synchronization.
   *
   * USAGE:
   * 1. Register this listener BEFORE accessing any joined events
   * 2. The first callback will populate your initial state with all existing events
   * 3. Further callbacks will update your state incrementally
   *
   * TECHNICAL DETAILS:
   * - Callbacks are delivered on a Firestore background thread (not the Android main thread).
   * - In case of transient errors, empty updates may be emitted; permanent failures are logged.
   * - Removed events are represented by a minimal Event object (only uid is guaranteed valid).
   *
   * @param userId The user ID
   * @param onUpdate Callback receiving changes: first call contains all existing events as "added",
   *   subsequent calls contain only incremental changes
   * @return ListenerRegistration to remove the listener
   */
  fun listenToJoinedEvents(
      userId: String,
      onUpdate: (List<Event>, List<Event>, List<String>) -> Unit
  ): ListenerRegistration
}

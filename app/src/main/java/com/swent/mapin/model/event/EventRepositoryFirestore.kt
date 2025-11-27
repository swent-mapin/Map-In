package com.swent.mapin.model.event

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.Location
import com.swent.mapin.model.NotificationService
import com.swent.mapin.model.event.FirestoreSchema.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.USERS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.UserFields.JOINED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.OWNED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.SAVED_EVENT_IDS
import com.swent.mapin.ui.filters.Filters
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

object FirestoreSchema {
  const val EVENTS_COLLECTION_PATH = "events"
  const val USERS_COLLECTION_PATH = "users"

  object UserFields {
    const val SAVED_EVENT_IDS = "savedEventIds"
    const val JOINED_EVENT_IDS = "joinedEventIds"
    const val OWNED_EVENT_IDS = "ownedEventIds"
  }
}

const val FIRESTORE_QUERY_LIMIT: Int = 10

/**
 * Enum to specify the source of user events (saved, joined, owned). Defines the field name in the
 * user document where event IDs are stored as arrays.
 */
enum class UserEventSource(val fieldName: String) {
  SAVED("savedEventIds"),
  JOINED("joinedEventIds"),
  OWNED("ownedEventIds")
}

/**
 * Firestore implementation of [EventRepository] with optimized queries.
 *
 * @param db Firestore instance to use.
 * @param friendRequestRepository Repository for friend-related queries.
 */
class EventRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val friendRequestRepository: FriendRequestRepository =
        FriendRequestRepository(notificationService = NotificationService())
) : EventRepository {

  /**
   * Generates and returns a new unique identifier for an Event item.
   *
   * @return A unique string identifier.
   */
  override fun getNewUid(): String = db.collection(EVENTS_COLLECTION_PATH).document().id

  /**
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  override suspend fun addEvent(event: Event) {
    require(event.isValidEvent()) { "Failed to add event: Invalid event data" }
    require(event.participantIds.isEmpty()) {
      "Failed to add event: participantIds should be empty"
    }
    try {
      val id = event.uid.ifBlank { getNewUid() }
      val eventToSave = event.copy(uid = id)

      db.runBatch { batch ->
            // Write the event document
            batch.set(db.collection(EVENTS_COLLECTION_PATH).document(id), eventToSave)
            // Update the owner's ownedEventIds
            batch.update(
                db.collection(USERS_COLLECTION_PATH).document(eventToSave.ownerId),
                OWNED_EVENT_IDS,
                FieldValue.arrayUnion(id))
          }
          .await()
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to add event (id=${event.uid}): ${e.message}", e)
      throw Exception("Failed to add event: ${e.message}", e)
    }
  }

  /**
   * Edits an existing event as the owner.
   *
   * Owner cannot:
   * - change participants
   * - change public value
   * - reduce capacity below current number of participants
   */
  override suspend fun editEventAsOwner(eventId: String, newValue: Event) {
    require(newValue.isValidEvent()) { "Failed to edit event: Invalid event data" }
    try {
      db.runTransaction { transaction ->
            val docRef = db.collection(EVENTS_COLLECTION_PATH).document(eventId)
            val snapshot = transaction.get(docRef)

            if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")

            val oldEvent = snapshot.toEvent()!!
            if (oldEvent.ownerId != newValue.ownerId)
                throw IllegalArgumentException("Only the owner can call editEventAsOwner")

            // Owner cannot change participants
            if (newValue.participantIds != oldEvent.participantIds)
                throw IllegalArgumentException("Owner cannot change participants list")

            // Owner cannot change 'public'
            if (newValue.public != oldEvent.public && !newValue.public)
                throw IllegalArgumentException("Owner cannot change from public to private")

            // Capacity cannot go below existing participant count
            val currentParticipants = oldEvent.participantIds.size
            if (newValue.capacity != null && newValue.capacity < currentParticipants)
                throw IllegalArgumentException(
                    "Capacity cannot be lower than current participants ($currentParticipants)")

            // Save validated event
            val eventToSave = newValue.copy(uid = eventId)
            transaction.set(docRef, eventToSave)
          }
          .await()
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed editEventAsOwner(id=$eventId): ${e.message}", e)
      throw Exception("Failed to edit event (id=$eventId) as owner: ${e.message}", e)
    }
  }

  /** Allows a non-owner user to join or leave an event. Users can ONLY change participantIds. */
  override suspend fun editEventAsUser(eventId: String, userId: String, join: Boolean) {
    try {
      db.runTransaction { transaction ->
            val docRef = db.collection(EVENTS_COLLECTION_PATH).document(eventId)
            val snapshot = transaction.get(docRef)

            if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
            val oldEvent = snapshot.toEvent()!!

            val isParticipant = userId in oldEvent.participantIds

            val updatedParticipants =
                if (join) {
                  if (isParticipant) return@runTransaction // no changes
                  if (oldEvent.capacity != null &&
                      oldEvent.participantIds.size >= oldEvent.capacity)
                      throw IllegalStateException("Event is full")
                  oldEvent.participantIds + userId
                } else {
                  if (!isParticipant) return@runTransaction // no changes
                  oldEvent.participantIds - userId
                }

            // Update event
            transaction.update(docRef, "participantIds", updatedParticipants)

            // Update user joinedEventIds
            val userRef = db.collection(USERS_COLLECTION_PATH).document(userId)
            transaction.update(
                userRef,
                JOINED_EVENT_IDS,
                if (join) FieldValue.arrayUnion(eventId) else FieldValue.arrayRemove(eventId))
          }
          .await()
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed editEventAsUser(id=$eventId): ${e.message}", e)
      throw Exception("Failed to edit event (id=$eventId) as user: ${e.message}", e)
    }
  }

  /**
   * Deletes an Event item from the repository.
   *
   * @param eventId The unique identifier of the Event item to delete.
   * @throws Exception if the Event item is not found.
   */
  override suspend fun deleteEvent(eventId: String) {
    try {
      // Read the event to get participantIds and ownerId
      val snapshot = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
      val event = snapshot.toEvent()!!

      // Chunk participant updates, with the first chunk included in the initial batch if small
      // enough
      val participantChunks = event.participantIds.chunked(450)
      val firstChunk = participantChunks.firstOrNull() ?: emptyList()
      val remainingChunks = participantChunks.drop(1)

      // First batch: Delete event, update owner's ownedEventIds, and handle first chunk of
      // participants
      db.runBatch { batch ->
            // Delete the event
            batch.delete(db.collection(EVENTS_COLLECTION_PATH).document(eventId))

            // Update owner's ownedEventIds
            if (event.ownerId.isNotBlank()) {
              batch.update(
                  db.collection(USERS_COLLECTION_PATH).document(event.ownerId),
                  OWNED_EVENT_IDS,
                  FieldValue.arrayRemove(eventId))
            }

            // Update joinedEventIds for the first chunk of participants
            for (userId in firstChunk) {
              batch.update(
                  db.collection(USERS_COLLECTION_PATH).document(userId),
                  JOINED_EVENT_IDS,
                  FieldValue.arrayRemove(eventId))
            }
          }
          .await()

      // Handle remaining participant chunks in separate batches
      for (chunk in remainingChunks) {
        db.runBatch { batch ->
              for (userId in chunk) {
                batch.update(
                    db.collection(USERS_COLLECTION_PATH).document(userId),
                    JOINED_EVENT_IDS,
                    FieldValue.arrayRemove(eventId))
              }
            }
            .await()
      }

      // Handle savedEventIds separately
      val usersSnapSaved =
          db.collection(USERS_COLLECTION_PATH)
              .whereArrayContains(SAVED_EVENT_IDS, eventId)
              .get()
              .await()

      if (usersSnapSaved.documents.isNotEmpty()) {
        db.runBatch { batch ->
              for (userDoc in usersSnapSaved.documents) {
                batch.update(
                    db.collection(USERS_COLLECTION_PATH).document(userDoc.id),
                    SAVED_EVENT_IDS,
                    FieldValue.arrayRemove(eventId))
              }
            }
            .await()
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to delete event (id=$eventId): ${e.message}", e)
      throw e as? NoSuchElementException
          ?: Exception("Failed to delete event (id=$eventId): ${e.message}", e)
    }
  }

  /**
   * Retrieves a specific Event item by its unique identifier.
   *
   * @param eventId The unique identifier of the Event item to retrieve.
   * @return The Event item with the specified identifier.
   * @throws Exception if the Event item is not found.
   */
  override suspend fun getEvent(eventId: String): Event {
    return try {
      val doc = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      doc.toEvent() ?: throw NoSuchElementException("Event not found (id=$eventId)")
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch event $eventId", e)
      throw Exception("Failed to fetch event $eventId: ${e.message}", e)
    }
  }

  /**
   * Retrieves Event items based on the specified filters.
   *
   * Basic implementation for PR #1 - will be optimized with query strategies in PR #2.
   *
   * @param filters The filtering criteria (e.g., tags, date range, location, etc.).
   * @return A list of Event items matching the filters.
   */
  override suspend fun getFilteredEvents(filters: Filters): List<Event> {
    return try {
      val query = buildBaseQuery(filters)
      val snap = query.orderBy("date", Query.Direction.DESCENDING).get().await()
      snap.documents.mapNotNull { it.toEvent() }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch filtered events", e)
      throw Exception("Failed to fetch filtered events: ${e.message}", e)
    }
  }

  /**
   * Get the saved events for a user, sorted by event date.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items saved by the user.
   */
  override suspend fun getSavedEvents(userId: String): List<Event> {
    return getUserEvents(userId, UserEventSource.SAVED)
  }

  /**
   * Get the joined events for a user, sorted by event date.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items the user has joined.
   */
  override suspend fun getJoinedEvents(userId: String): List<Event> {
    return getUserEvents(userId, UserEventSource.JOINED)
  }

  /**
   * Get the owned events for a user, sorted by event date.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items owned by the user.
   */
  override suspend fun getOwnedEvents(userId: String): List<Event> {
    return getUserEvents(userId, UserEventSource.OWNED)
  }

  /**
   * Listen to changes in the events collection with filters.
   *
   * Basic implementation for PR #1 - will be enhanced with advanced filtering in PR #2.
   *
   * @param filters The filtering criteria.
   * @param onUpdate Callback with added, modified, and removed events.
   * @return ListenerRegistration to manage the listener.
   */
  override fun listenToFilteredEvents(
      filters: Filters,
      onUpdate: (added: List<Event>, modified: List<Event>, removed: List<Event>) -> Unit
  ): ListenerRegistration {
    val query = buildBaseQuery(filters)

    return query.orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error
      ->
      if (error != null || snapshot == null) {
        Log.e("EventRepositoryFirestore", "Filtered events listener error", error)
        onUpdate(emptyList(), emptyList(), emptyList())
        return@addSnapshotListener
      }

      // Simple implementation: treat all documents as added
      // PR #2 will differentiate between added/modified/removed
      val events = snapshot.documents.mapNotNull { it.toEvent() }
      onUpdate(events, emptyList(), emptyList())
    }
  }

  /** Builds the base query with date filters and optional price filter. */
  private fun buildBaseQuery(filters: Filters): Query {
    var query: Query = db.collection(EVENTS_COLLECTION_PATH)

    // Apply startDate filter
    val startTimestamp =
        Timestamp(filters.startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
    query = query.whereGreaterThanOrEqualTo("date", startTimestamp)

    // Apply endDate filter if provided
    if (filters.endDate != null) {
      val endTimestamp =
          Timestamp(
              filters.endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond(), 0)
      query = query.whereLessThanOrEqualTo("date", endTimestamp)
    }

    return query
  }

  /**
   * Generic method to retrieve event IDs from a user's array field.
   *
   * @param userId The user ID.
   * @param source The source type (SAVED, JOINED, OWNED).
   * @return A list of event IDs.
   */
  private suspend fun getUserEventIds(userId: String, source: UserEventSource): List<String> {
    return try {
      val snap = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
      if (!snap.exists()) {
        Log.w("EventRepositoryFirestore", "User $userId not found")
        throw Exception("User $userId not found")
      } else {
        @Suppress("UNCHECKED_CAST")
        (snap.get(source.fieldName) as? List<String>) ?: emptyList()
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch event IDs for $source", e)
      throw e
    }
  }

  /**
   * Generic method to retrieve full events for a user.
   *
   * @param userId The user ID.
   * @param source The source type (SAVED, JOINED, OWNED).
   * @return A list of events, sorted by date.
   */
  private suspend fun getUserEvents(userId: String, source: UserEventSource): List<Event> {
    val ids = getUserEventIds(userId, source)
    return fetchEventsByIds(ids)
  }

  /**
   * Fetch events by IDs, handling Firestore query limits with parallel execution.
   *
   * @param ids List of event IDs.
   * @return List of events, sorted by date.
   */
  private suspend fun fetchEventsByIds(ids: List<String>): List<Event> = coroutineScope {
    if (ids.isEmpty()) return@coroutineScope emptyList()

    try {
      val chunks = ids.chunked(FIRESTORE_QUERY_LIMIT)

      // Execute all chunks in parallel for better performance
      val deferredResults =
          chunks.map { chunk ->
            async {
              try {
                val snap =
                    db.collection(EVENTS_COLLECTION_PATH)
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                snap.documents.mapNotNull { it.toEvent() }
              } catch (e: Exception) {
                Log.e("EventRepositoryFirestore", "Failed to fetch chunk: ${e.message}")
                emptyList()
              }
            }
          }

      // Collect all results and sort
      deferredResults.flatMap { it.await() }.sortedByDescending { it.date }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch events by IDs", e)
      emptyList()
    }
  }

  /**
   * Convert DocumentSnapshot to Event.
   *
   * @return The Event object or null if conversion fails.
   */
  private fun DocumentSnapshot.toEvent(): Event? =
      try {
        this.toObject(Event::class.java)?.copy(uid = this.id)
      } catch (e: Exception) {
        Log.e("EventRepositoryFirestore", "Error converting document to Event (id=${this.id})", e)
        throw e
      }

  /**
   * Parse Location to GeoPoint.
   *
   * @param place The location.
   * @return GeoPoint or null if invalid.
   */
  private fun parsePlaceToGeoPoint(place: Location?): GeoPoint? {
    return try {
      if (place == null) null else GeoPoint(place.latitude, place.longitude)
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "Invalid location coordinates: $place", e)
      null
    }
  }
}

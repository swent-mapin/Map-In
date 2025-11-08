package com.swent.mapin.model.event

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.Location
import com.swent.mapin.ui.map.Filters
import com.swent.mapin.util.EventUtils.calculateHaversineDistance
import java.time.ZoneId
import kotlinx.coroutines.tasks.await

// Assisted by AI
const val EVENTS_COLLECTION_PATH = "events"
const val USERS_COLLECTION_PATH = "users"
const val SAVED_SUBCOLLECTION = "savedEvents"
const val FIELD_SAVED_AT = "savedAt"
const val FIRESTORE_QUERY_LIMIT: Int = 10
const val POPULAR_EVENT: Int = 30

/**
 * Firestore implementation of [EventRepository].
 *
 * @param db Firestore instance to use.
 * @param localCache Optional local cache for saved events to support offline access.
 */
class EventRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val localCache: EventLocalCache? = null,
    private val friendRequestRepository: FriendRequestRepository = FriendRequestRepository()
) : EventRepository {

  /**
   * Generates and returns a new unique identifier for an Event item.
   *
   * @return A unique string identifier.
   */
  override fun getNewUid(): String = db.collection(EVENTS_COLLECTION_PATH).document().id

  /**
   * Retrieves all Event items from the repository.
   *
   * @return A list of all Event items.
   */
  override suspend fun getAllEvents(): List<Event> {
    return try {
      val snap = db.collection(EVENTS_COLLECTION_PATH).orderBy("date").get().await()
      snap.documents.mapNotNull { documentToEvent(it) }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch all events", e)
      throw Exception("Failed to fetch all events: ${e.message}", e)
    }
  }

  /**
   * Retrieves a specific Event item by its unique identifier.
   *
   * @param eventId The unique identifier of the Event item to retrieve.
   * @return The Event item with the specified identifier.
   * @throws NoSuchElementException if the Event item is not found.
   */
  override suspend fun getEvent(eventId: String): Event {
    return try {
      val doc = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      documentToEvent(doc) ?: throw NoSuchElementException("Event not found (id=$eventId)")
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch event $eventId", e)
      throw e as? NoSuchElementException
          ?: Exception("Failed to fetch event $eventId: ${e.message}", e)
    }
  }

  /**
   * Retrieves Event items based on the specified filters.
   *
   * @param filters The filtering criteria (e.g., tags, date range, location, etc.).
   * @return A list of Event items matching the filters.
   */
  override suspend fun getFilteredEvents(filters: Filters): List<Event> {
    return try {
      var query: Query = db.collection(EVENTS_COLLECTION_PATH)

      // Apply startDate filter
      val startTimestamp =
          Timestamp(filters.startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
      query = query.whereGreaterThanOrEqualTo("date", startTimestamp)

      // Apply endDate filter only if provided
      if (filters.endDate != null) {
        val endTimestamp =
            Timestamp(
                filters.endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond(),
                0)
        query = query.whereLessThanOrEqualTo("date", endTimestamp)
      }

      // Apply tags filter
      if (filters.tags.isNotEmpty()) {
        val limitedTags =
            if (filters.tags.size > FIRESTORE_QUERY_LIMIT) {
              Log.w(
                  "EventRepositoryFirestore",
                  "Tags exceeded limit: ${filters.tags.size}, truncating to $FIRESTORE_QUERY_LIMIT")
              filters.tags.take(FIRESTORE_QUERY_LIMIT)
            } else {
              filters.tags.toList()
            }
        query = query.whereArrayContainsAny("tags", limitedTags)
      }

      // Fetch events
      val snap = query.orderBy("date").get().await()
      var events = snap.documents.mapNotNull { documentToEvent(it) }

      // Apply friendsOnly filter (placeholder until friendIds is added)
      if (filters.friendsOnly) {
        val userId =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalArgumentException("User must be logged in for friendsOnly filter")

        // Get the friend IDs (example call to repository)
        val friendIds = friendRequestRepository.getFriends(userId).map { it.userProfile.userId }

        if (friendIds.isEmpty()) {
          return emptyList()
        }
        events = events.filter { event -> friendIds.contains(event.ownerId) }
      }

      // Apply popularOnly filter (client-side)
      if (filters.popularOnly) {
        events = events.filter { event -> event.participantIds.size > POPULAR_EVENT }
      }

      // Apply maxPrice filter (client-side)
      if (filters.maxPrice != null) {
        events = events.filter { event -> event.price <= filters.maxPrice.toDouble() }
      }

      // Apply geospatial filter (place and radius) client-side
      if (filters.place != null) {
        val placeGeoPoint =
            parsePlaceToGeoPoint(filters.place)
                ?: throw IllegalArgumentException("Invalid place coordinates")
        events =
            events.filter { event ->
              val eventGeoPoint =
                  GeoPoint(
                      (event.location.latitude as? Number)?.toDouble() ?: 0.0,
                      (event.location.longitude as? Number)?.toDouble() ?: 0.0)
              val distance = calculateHaversineDistance(eventGeoPoint, placeGeoPoint)
              distance <= filters.radiusKm
            }
      }

      events
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch filtered events", e)
      throw Exception("Failed to fetch filtered events: ${e.message}", e)
    }
  }

  /**
   * Retrieves Event items whose titles match the specified search query (case-insensitive).
   *
   * @param title The search query to match against event titles.
   * @param filters The filtering criteria (e.g., tags, date range, location, etc.).
   * @return A list of Event items whose titles contain the query.
   */
  override suspend fun getSearchedEvents(title: String, filters: Filters): List<Event> {
    return try {
      val lowerTitle = title.trim().lowercase()
      val filteredEvents = getFilteredEvents(filters)
      filteredEvents.filter { it.title.trim().lowercase().contains(lowerTitle) }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch events by title search", e)
      throw Exception("Failed to fetch events by title search: ${e.message}", e)
    }
  }

  /**
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  override suspend fun addEvent(event: Event) {
    require(event.isValidEvent()) { "Invalid event data" }
    try {
      val id = event.uid.ifBlank { getNewUid() }
      // Automatically add owner to participants if not included
      val participantIds =
          if (!event.participantIds.contains(event.ownerId)) {
            event.participantIds + event.ownerId
          } else {
            event.participantIds
          }
      val eventToSave = event.copy(uid = id, participantIds = participantIds)
      db.collection(EVENTS_COLLECTION_PATH).document(id).set(eventToSave).await()

      // Update UserProfile.ownedEventIds
      val userSnapshot =
          db.collection(USERS_COLLECTION_PATH).document(eventToSave.ownerId).get().await()
      if (userSnapshot.exists()) {
        db.collection(USERS_COLLECTION_PATH)
            .document(eventToSave.ownerId)
            .update("ownedEventIds", FieldValue.arrayUnion(id))
            .await()
      }
      // Update UserProfile.participatingEventIds
      for (userId in eventToSave.participantIds) {
        val userSnapshot = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
        if (userSnapshot.exists()) {
          db.collection(USERS_COLLECTION_PATH)
              .document(userId)
              .update("participatingEventIds", FieldValue.arrayUnion(id))
              .await()
        }
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to add event", e)
      throw Exception("Failed to add event: ${e.message}", e)
    }
  }

  /**
   * Edits an existing Event item in the repository.
   *
   * @param eventId The unique identifier of the Event item to edit.
   * @param newValue The updated Event item.
   * @throws NoSuchElementException if the Event item is not found.
   */
  override suspend fun editEvent(eventId: String, newValue: Event) {
    require(newValue.isValidEvent()) { "Invalid event data" }
    try {
      val snapshot = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
      val oldValue = documentToEvent(snapshot)!!
      require(oldValue.ownerId != newValue.ownerId) {
        "Cannot transfer event ownership via editEvent"
      }
      // Ensure owner is in participantIds
      val participantIds =
          if (newValue.ownerId.isNotBlank() &&
              !newValue.participantIds.contains(newValue.ownerId)) {
            newValue.participantIds + newValue.ownerId
          } else {
            newValue.participantIds
          }
      val eventToSave = newValue.copy(uid = eventId, participantIds = participantIds)
      db.collection(EVENTS_COLLECTION_PATH).document(eventId).set(eventToSave).await()

      // Update UserProfile.participatingEventIds
      val oldParticipants = oldValue.participantIds.toSet()
      val newParticipants = eventToSave.participantIds.toSet()
      val addedParticipants = newParticipants - oldParticipants
      val removedParticipants = oldParticipants - newParticipants
      for (userId in removedParticipants) {
        db.collection(USERS_COLLECTION_PATH)
            .document(userId)
            .update("participatingEventIds", FieldValue.arrayRemove(eventId))
            .await()
      }
      for (userId in addedParticipants) {
        db.collection(USERS_COLLECTION_PATH)
            .document(userId)
            .update("participatingEventIds", FieldValue.arrayUnion(eventId))
            .await()
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to edit event $eventId", e)
      throw e as? NoSuchElementException
          ?: Exception("Failed to edit event $eventId: ${e.message}", e)
    }
  }

  /**
   * Deletes an Event item from the repository.
   *
   * @param eventId The unique identifier of the Event item to delete.
   * @throws NoSuchElementException if the Event item is not found.
   */
  override suspend fun deleteEvent(eventId: String) {
    try {
      val snapshot = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
      val event = documentToEvent(snapshot)!!
      db.collection(EVENTS_COLLECTION_PATH).document(eventId).delete().await()

      // Update UserProfile.ownedEventIds
      if (event.ownerId.isNotBlank()) {
        val userSnapshot =
            db.collection(USERS_COLLECTION_PATH).document(event.ownerId).get().await()
        if (userSnapshot.exists()) {
          db.collection(USERS_COLLECTION_PATH)
              .document(event.ownerId)
              .update("ownedEventIds", FieldValue.arrayRemove(eventId))
              .await()
        }
      }
      // Update UserProfile.participatingEventIds
      for (userId in event.participantIds) {
        val userSnapshot = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
        if (userSnapshot.exists()) {
          db.collection(USERS_COLLECTION_PATH)
              .document(userId)
              .update("participatingEventIds", FieldValue.arrayRemove(eventId))
              .await()
        }
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to delete event $eventId", e)
      throw e as? NoSuchElementException
          ?: Exception("Failed to delete event $eventId: ${e.message}", e)
    }
  }

  /**
   * Retrieves the IDs of Event items saved by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A set of IDs of Event items saved by the user.
   */
  override suspend fun getSavedEventIds(userId: String): Set<String> {
    return try {
      val snap =
          db.collection(USERS_COLLECTION_PATH)
              .document(userId)
              .collection(SAVED_SUBCOLLECTION)
              .orderBy("savedAt")
              .get()
              .await()
      snap.documents.map { it.id }.toSet()
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "getSavedEventIds failed, falling back to local cache", e)
      try {
        localCache?.getSavedEvents(userId)?.map { it.uid }?.toSet() ?: emptySet()
      } catch (ex: Exception) {
        Log.w("EventRepositoryFirestore", "local cache read failed", ex)
        emptySet()
      }
    }
  }

  /**
   * Get the saved events for a user. Returns events sorted by the order in which they were saved.
   * Updates local cache on success for offline availability. On failure, falls back to local cache
   * if available. Implemented with the help of AI generated code.
   */
  override suspend fun getSavedEvents(userId: String): List<Event> {
    try {
      val ids = getSavedEventIds(userId).toList()
      if (ids.isEmpty()) return emptyList()

      // Firestore whereIn limit is 10; chunk and merge, then sort locally by date.
      val chunks = ids.chunked(FIRESTORE_QUERY_LIMIT)
      val results = mutableListOf<Event>()
      for (chunk in chunks) {
        val snap =
            db.collection(EVENTS_COLLECTION_PATH)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
        results += snap.documents.mapNotNull { documentToEvent(it) }
      }
      val order = ids.withIndex().associate { (i, id) -> id to i }
      val sorted = results.sortedBy { order[it.uid] ?: Int.MAX_VALUE }

      // update local cache for offline availability
      try {
        localCache?.cacheSavedEvents(userId, sorted)
      } catch (e: Exception) {
        Log.w("EventRepositoryFirestore", "failed to update local cache", e)
      }

      return sorted
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "getSavedEvents failed, falling back to local cache", e)
      return try {
        localCache?.getSavedEvents(userId) ?: emptyList()
      } catch (ex: Exception) {
        Log.w("EventRepositoryFirestore", "local cache read failed", ex)
        emptyList()
      }
    }
  }

  /**
   * Mark an event as saved for a user (idempotent). Updates local cache on success for offline
   * availability. Returns true if either remote or local operation succeeded. On failure, returns
   * false. Implemented with the help of AI generated code.
   */
  override suspend fun saveEventForUser(userId: String, eventId: String): Boolean {
    var remoteOk = false
    try {
      db.collection(USERS_COLLECTION_PATH)
          .document(userId)
          .collection(SAVED_SUBCOLLECTION)
          .document(eventId)
          .set(mapOf(FIELD_SAVED_AT to FieldValue.serverTimestamp()))
          .await()
      remoteOk = true
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "saveEventForUser remote failed", e)
    }

    // Ensure local cache is updated so user can view saved events offline.
    var localOk = false
    try {
      val event =
          try {
            getEvent(eventId)
          } catch (_: Exception) {
            null
          }
      if (localCache != null) {
        if (event != null) localCache.saveEventLocally(userId, event, Timestamp.now())
        else localCache.saveEventLocally(userId, Event(uid = eventId), Timestamp.now())
        localOk = true
      }
    } catch (_: Exception) {
      Log.w("EventRepositoryFirestore", "saveEventForUser local cache update failed")
    }

    return remoteOk || localOk
  }

  /**
   * Remove an event from a user's saved list (idempotent). Updates local cache on success for
   * offline availability. Returns true if either remote or local operation succeeded. On failure,
   * returns false. Implemented with the help of AI generated code.
   */
  override suspend fun unsaveEventForUser(userId: String, eventId: String): Boolean {
    var remoteOk = false
    try {
      db.collection(USERS_COLLECTION_PATH)
          .document(userId)
          .collection(SAVED_SUBCOLLECTION)
          .document(eventId)
          .delete()
          .await()
      remoteOk = true
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "unsaveEventForUser remote failed", e)
    }

    var localOk = false
    try {
      if (localCache != null) {
        localCache.unsaveEventLocally(userId, eventId)
        localOk = true
      }
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "unsaveEventForUser local cache update failed", e)
      throw Exception("Failed to unsave event for user: ${e.message}", e)
    }
    return remoteOk || localOk
  }

  /**
   * Converts a Firestore DocumentSnapshot to an Event object.
   *
   * @param document The Firestore DocumentSnapshot to convert.
   * @return The corresponding Event object, or null if conversion fails.
   */
  private fun documentToEvent(document: DocumentSnapshot): Event? =
      try {
        document.toObject(Event::class.java)?.copy(uid = document.id)
      } catch (e: Exception) {
        Log.e(
            "EventRepositoryFirestore", "Error converting document to Event (id=${document.id})", e)
        null
      }

  private fun parsePlaceToGeoPoint(place: Location?): GeoPoint? {
    return try {
      if (place == null) return null
      GeoPoint(place.latitude, place.longitude)
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "Invalid location coordinates: $place", e)
      null
    }
  }
}

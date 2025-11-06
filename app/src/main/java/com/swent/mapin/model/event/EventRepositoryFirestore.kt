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
import com.swent.mapin.model.Location
import com.swent.mapin.model.UserProfile
import com.swent.mapin.ui.map.Filters
import java.time.ZoneId
import kotlin.math.*
import kotlinx.coroutines.tasks.await

const val EVENTS_COLLECTION_PATH = "events"
const val USERS_COLLECTION_PATH = "users"
const val SAVED_SUBCOLLECTION = "savedEvents"
const val FIELD_SAVED_AT = "savedAt"
const val FIRESTORE_QUERY_LIMIT: Int = 30

class EventRepositoryFirestore(private val db: FirebaseFirestore) : EventRepository {

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
              filters.tags.take(FIRESTORE_QUERY_LIMIT)
            } else {
              filters.tags.toList()
            }
        query = query.whereArrayContainsAny("tags", limitedTags)
      }

      // Apply friendsOnly filter (placeholder until friendIds is added)
      if (filters.friendsOnly) {
        val userId =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalArgumentException("User must be logged in for friendsOnly filter")
        query = query.whereArrayContains("participantIds", userId)
      }

      // Fetch events
      val snap = query.orderBy("date").get().await()
      var events = snap.documents.mapNotNull { documentToEvent(it) }

      // Apply popularOnly filter (client-side)
      if (filters.popularOnly) {
        events = events.filter { event -> event.participantIds.size > 100 }
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
   * @return A list of Event items whose titles contain the query.
   */
  override suspend fun getSearchedEvents(title: String): List<Event> {
    return try {
      val lowerTitle = title.trim().lowercase()
      val snap = db.collection(EVENTS_COLLECTION_PATH).orderBy("date").get().await()
      val events = snap.documents.mapNotNull { documentToEvent(it) }
      events.filter { it.title.trim().lowercase().contains(lowerTitle) }
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
    try {
      val id = event.uid.ifBlank { getNewUid() }
      // Automatically add owner to participants if not included
      val participantIds =
          if (event.ownerId.isNotBlank() && !event.participantIds.contains(event.ownerId)) {
            event.participantIds + event.ownerId
          } else {
            event.participantIds
          }
      val eventToSave = event.copy(uid = id, participantIds = participantIds)
      db.collection(EVENTS_COLLECTION_PATH).document(id).set(eventToSave).await()

      // Update UserProfile.ownedEventIds
      if (eventToSave.ownerId.isNotBlank()) {
        val userSnapshot =
            db.collection(USERS_COLLECTION_PATH).document(eventToSave.ownerId).get().await()
        if (userSnapshot.exists()) {
          val userProfile = userSnapshot.toObject(UserProfile::class.java)
          if (userProfile != null) {
            val updatedOwnedEventIds = userProfile.ownedEventIds + id
            db.collection(USERS_COLLECTION_PATH)
                .document(eventToSave.ownerId)
                .update("ownedEventIds", updatedOwnedEventIds)
                .await()
          }
        }
      }
      // Update UserProfile.participatingEventIds
      for (userId in eventToSave.participantIds) {
        val userSnapshot = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
        if (userSnapshot.exists()) {
          val userProfile = userSnapshot.toObject(UserProfile::class.java)
          if (userProfile != null) {
            val updatedParticipatingEventIds = userProfile.participatingEventIds + id
            db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .update("participatingEventIds", updatedParticipatingEventIds)
                .await()
          }
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
    try {
      val snapshot = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
      val oldEvent = documentToEvent(snapshot)!!

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

      // Update UserProfile.ownedEventIds if ownerId changed
      if (newValue.ownerId != oldEvent.ownerId) {
        if (oldEvent.ownerId.isNotBlank()) {
          val oldUserSnapshot =
              db.collection(USERS_COLLECTION_PATH).document(oldEvent.ownerId).get().await()
          if (oldUserSnapshot.exists()) {
            val oldUserProfile = oldUserSnapshot.toObject(UserProfile::class.java)
            if (oldUserProfile != null) {
              val updatedOwnedEventIds = oldUserProfile.ownedEventIds - eventId
              db.collection(USERS_COLLECTION_PATH)
                  .document(oldEvent.ownerId)
                  .update("ownedEventIds", updatedOwnedEventIds)
                  .await()
            }
          }
        }
        if (newValue.ownerId.isNotBlank()) {
          val newUserSnapshot =
              db.collection(USERS_COLLECTION_PATH).document(newValue.ownerId).get().await()
          if (newUserSnapshot.exists()) {
            val newUserProfile = newUserSnapshot.toObject(UserProfile::class.java)
            if (newUserProfile != null) {
              val updatedOwnedEventIds = newUserProfile.ownedEventIds + eventId
              db.collection(USERS_COLLECTION_PATH)
                  .document(newValue.ownerId)
                  .update("ownedEventIds", updatedOwnedEventIds)
                  .await()
            }
          }
        }
      }
      // Update UserProfile.participatingEventIds
      val oldParticipants = oldEvent.participantIds.toSet()
      val newParticipants = eventToSave.participantIds.toSet()
      val addedParticipants = newParticipants - oldParticipants
      val removedParticipants = oldParticipants - newParticipants
      for (userId in addedParticipants) {
        val userSnapshot = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
        if (userSnapshot.exists()) {
          val userProfile = userSnapshot.toObject(UserProfile::class.java)
          if (userProfile != null) {
            val updatedParticipatingEventIds = userProfile.participatingEventIds + eventId
            db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .update("participatingEventIds", updatedParticipatingEventIds)
                .await()
          }
        }
      }
      for (userId in removedParticipants) {
        val userSnapshot = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
        if (userSnapshot.exists()) {
          val userProfile = userSnapshot.toObject(UserProfile::class.java)
          if (userProfile != null) {
            val updatedParticipatingEventIds = userProfile.participatingEventIds - eventId
            db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .update("participatingEventIds", updatedParticipatingEventIds)
                .await()
          }
        }
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
          val userProfile = userSnapshot.toObject(UserProfile::class.java)
          if (userProfile != null) {
            val updatedOwnedEventIds = userProfile.ownedEventIds - eventId
            db.collection(USERS_COLLECTION_PATH)
                .document(event.ownerId)
                .update("ownedEventIds", updatedOwnedEventIds)
                .await()
          }
        }
      }
      // Update UserProfile.participatingEventIds
      for (userId in event.participantIds) {
        val userSnapshot = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
        if (userSnapshot.exists()) {
          val userProfile = userSnapshot.toObject(UserProfile::class.java)
          if (userProfile != null) {
            val updatedParticipatingEventIds = userProfile.participatingEventIds - eventId
            db.collection(USERS_COLLECTION_PATH)
                .document(userId)
                .update("participatingEventIds", updatedParticipatingEventIds)
                .await()
          }
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
              .get()
              .await()
      snap.documents.map { it.id }.toSet()
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "getSavedEventIds failed", e)
      emptySet()
    }
  }

  /**
   * Retrieves Event items saved by the specified user.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items saved by the user.
   */
  override suspend fun getSavedEvents(userId: String): List<Event> {
    return try {
      val ids = getSavedEventIds(userId).toList()
      if (ids.isEmpty()) return emptyList()
      val chunks = ids.chunked(FIRESTORE_QUERY_LIMIT)
      val results = mutableListOf<Event>()
      for (chunk in chunks) {
        val snap =
            db.collection(EVENTS_COLLECTION_PATH)
                .whereIn(FieldPath.documentId(), chunk)
                .orderBy("date")
                .get()
                .await()
        results += snap.documents.mapNotNull { documentToEvent(it) }
      }
      results
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch saved events", e)
      throw Exception("Failed to fetch saved events: ${e.message}", e)
    }
  }

  /**
   * Saves an Event item for the specified user.
   *
   * @param userId The unique identifier of the user.
   * @param eventId The unique identifier of the Event item to save.
   * @return True if the Event item was successfully saved, false if already saved.
   * @throws NoSuchElementException if the Event item is not found.
   */
  override suspend fun saveEventForUser(userId: String, eventId: String): Boolean {
    return try {
      val snapshot = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
      val savedEventIds = getSavedEventIds(userId)
      if (eventId in savedEventIds) return false
      db.collection(USERS_COLLECTION_PATH)
          .document(userId)
          .collection(SAVED_SUBCOLLECTION)
          .document(eventId)
          .set(mapOf(FIELD_SAVED_AT to FieldValue.serverTimestamp()))
          .await()
      true
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "saveEventForUser failed", e)
      throw e as? NoSuchElementException
          ?: Exception("Failed to save event for user: ${e.message}", e)
    }
  }

  /**
   * Removes a saved Event item for the specified user.
   *
   * @param userId The unique identifier of the user.
   * @param eventId The unique identifier of the Event item to remove.
   * @return True if the Event item was successfully removed, false if not saved.
   * @throws NoSuchElementException if the Event item is not found.
   */
  override suspend fun unsaveEventForUser(userId: String, eventId: String): Boolean {
    return try {
      val savedEventIds = getSavedEventIds(userId)
      if (eventId !in savedEventIds) return false
      db.collection(USERS_COLLECTION_PATH)
          .document(userId)
          .collection(SAVED_SUBCOLLECTION)
          .document(eventId)
          .delete()
          .await()
      true
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "unsaveEventForUser failed", e)
      throw Exception("Failed to unsave event for user: ${e.message}", e)
    }
  }

  /**
   * Converts a Firestore DocumentSnapshot to an Event object.
   *
   * @param document The Firestore DocumentSnapshot to convert.
   * @return The corresponding Event object, or null if conversion fails.
   */
  private fun documentToEvent(document: DocumentSnapshot): Event? {
    return try {
      document
          .toObject(Event::class.java)
          ?.copy(
              uid = document.id,
              location =
                  document
                      .toObject(Event::class.java)!!
                      .location
                      .copy(
                          latitude =
                              (document.get("location.latitude") as? Number)?.toDouble() ?: 0.0,
                          longitude =
                              (document.get("location.longitude") as? Number)?.toDouble() ?: 0.0),
              capacity = (document.get("capacity") as? Number)?.toInt(),
              price = (document.get("price") as? Number)?.toDouble() ?: 0.0)
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Error converting document to Event (id=${document.id})", e)
      null
    }
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

  private fun calculateHaversineDistance(geoPoint1: GeoPoint, geoPoint2: GeoPoint): Double {
    val lat1 = Math.toRadians(geoPoint1.latitude)
    val lon1 = Math.toRadians(geoPoint1.longitude)
    val lat2 = Math.toRadians(geoPoint2.latitude)
    val lon2 = Math.toRadians(geoPoint2.longitude)
    val dLat = lat2 - lat1
    val dLon = lon2 - lon1
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    val r = 6371.0 // Earth radius in kilometers
    return c * r
  }
}

package com.swent.mapin.model.event

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val EVENTS_COLLECTION_PATH = "events"

/** Number of tags to limit in queries, due to Firestore constraints. */
const val LIMIT: Int = 10

// Saved-events constants
private const val USERS_COLLECTION_PATH = "users"
private const val SAVED_SUBCOLLECTION = "savedEvents"
private const val FIELD_SAVED_AT = "savedAt"

class EventRepositoryFirestore(private val db: FirebaseFirestore) : EventRepository {

  override fun getNewUid(): String = db.collection(EVENTS_COLLECTION_PATH).document().id

  override suspend fun getAllEvents(): List<Event> {
    val snap = db.collection(EVENTS_COLLECTION_PATH).orderBy("date").get().await()
    return snap.documents.mapNotNull { documentToEvent(it) }
  }

  override suspend fun getEvent(eventID: String): Event {
    val doc = db.collection(EVENTS_COLLECTION_PATH).document(eventID).get().await()
    return documentToEvent(doc)
        ?: throw NoSuchElementException("EventRepositoryFirestore: Event not found (id=$eventID)")
  }

  override suspend fun getEventsByTags(tags: List<String>): List<Event> {
    if (tags.isEmpty()) return getAllEvents()
    // Firestore allows a maximum of 10 elements in whereArrayContainsAny
    val limited = if (tags.size > LIMIT) tags.take(LIMIT) else tags
    val snap =
        db.collection(EVENTS_COLLECTION_PATH)
            .whereArrayContainsAny("tags", limited)
            .orderBy("date")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToEvent(it) }
  }

  override suspend fun getEventsOnDay(dayStart: Timestamp, dayEnd: Timestamp): List<Event> {
    val snap =
        db.collection(EVENTS_COLLECTION_PATH)
            .whereGreaterThanOrEqualTo("date", dayStart)
            .whereLessThan("date", dayEnd)
            .orderBy("date")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToEvent(it) }
  }

  override suspend fun getEventsByOwner(ownerId: String): List<Event> {
    val snap =
        db.collection(EVENTS_COLLECTION_PATH)
            .whereEqualTo("ownerId", ownerId)
            .orderBy("date")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToEvent(it) }
  }

  override suspend fun getEventsByTitle(title: String): List<Event> {
    val lowerTitle = title.trim().lowercase()
    val snap = db.collection(EVENTS_COLLECTION_PATH).get().await()
    val events = snap.documents.mapNotNull { documentToEvent(it) }
    return events.filter { it.title.trim().lowercase() == lowerTitle }
  }

  override suspend fun getEventsByParticipant(userId: String): List<Event> {
    val snap =
        db.collection(EVENTS_COLLECTION_PATH)
            .whereArrayContains("participantIds", userId)
            .orderBy("date")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToEvent(it) }
  }

  override suspend fun addEvent(event: Event) {
    val id = event.uid.ifBlank { getNewUid() }

    // Automatically add owner to participants if not already included
    val participantIds =
        if (event.ownerId.isNotBlank() && !event.participantIds.contains(event.ownerId)) {
          event.participantIds + event.ownerId
        } else {
          event.participantIds
        }

    val eventToSave = event.copy(uid = id, participantIds = participantIds)
    db.collection(EVENTS_COLLECTION_PATH).document(id).set(eventToSave).await()
  }

  override suspend fun editEvent(eventID: String, newValue: Event) {
    db.collection(EVENTS_COLLECTION_PATH)
        .document(eventID)
        .set(newValue.copy(uid = eventID))
        .await()
  }

  override suspend fun deleteEvent(eventID: String) {
    db.collection(EVENTS_COLLECTION_PATH).document(eventID).delete().await()
  }

  private fun documentToEvent(document: DocumentSnapshot): Event? =
      try {
        document.toObject(Event::class.java)?.copy(uid = document.id)
      } catch (e: Exception) {
        Log.e(
            "EventRepositoryFirestore", "Error converting document to Event (id=${document.id})", e)
        null
      }

  // ============================
  // Saved Events (per-user)
  // ============================

  /** Get the set of saved event IDs for a user. */
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

  /** Get the saved events for a user (sorted by date ascending). */
  override suspend fun getSavedEvents(userId: String): List<Event> {
    val ids = getSavedEventIds(userId).toList()
    if (ids.isEmpty()) return emptyList()

    // Firestore whereIn limit is 10; chunk and merge, then sort locally by date.
    val chunks = ids.chunked(LIMIT)
    val results = mutableListOf<Event>()
    for (chunk in chunks) {
      val snap =
          db.collection(EVENTS_COLLECTION_PATH).whereIn(FieldPath.documentId(), chunk).get().await()
      results += snap.documents.mapNotNull { documentToEvent(it) }
    }
    return results.sortedBy { it.date } // sort in-memory by event date
  }

  /** Mark an event as saved for a user (idempotent). */
  override suspend fun saveEventForUser(userId: String, eventId: String): Boolean {
    return try {
      db.collection(USERS_COLLECTION_PATH)
          .document(userId)
          .collection(SAVED_SUBCOLLECTION)
          .document(eventId)
          .set(mapOf(FIELD_SAVED_AT to FieldValue.serverTimestamp()))
          .await()
      true
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "saveEventForUser failed", e)
      false
    }
  }

  /** Remove an event from a user's saved list (idempotent). */
  override suspend fun unsaveEventForUser(userId: String, eventId: String): Boolean {
    return try {
      db.collection(USERS_COLLECTION_PATH)
          .document(userId)
          .collection(SAVED_SUBCOLLECTION)
          .document(eventId)
          .delete()
          .await()
      true
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "unsaveEventForUser failed", e)
      false
    }
  }
}

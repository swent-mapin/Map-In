package com.swent.mapin.model.event

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val EVENTS_COLLECTION_PATH = "events"

/** Number of tags to limit in queries, due to Firestore constraints. */
const val TAG_LIMIT: Int = 10

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
    val limited = if (tags.size > TAG_LIMIT) tags.take(TAG_LIMIT) else tags
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

  override suspend fun addEvent(event: Event) {
    val id = event.uid.ifBlank { getNewUid() }
    db.collection(EVENTS_COLLECTION_PATH).document(id).set(event.copy(uid = id)).await()
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
}

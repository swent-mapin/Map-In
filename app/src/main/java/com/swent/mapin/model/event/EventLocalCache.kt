package com.swent.mapin.model.event

import android.content.Context
import com.google.firebase.Timestamp
import com.swent.mapin.model.location.Location

/**
 * Local cache wrapper for saved events using Room. Provides simple conversion between Room entities
 * and the domain Event. Implemented with the help of AI.
 */
class EventLocalCache(private val dao: SavedEventDao) {

  companion object {
    fun forContext(context: Context): EventLocalCache {
      val db = EventsDatabase.getInstance(context)
      return EventLocalCache(db.savedEventDao())
    }
  }

  suspend fun getSavedEvents(userId: String): List<Event> {
    val entities = dao.getSavedForUser(userId)
    return entities.map { it.toEvent() }
  }

  suspend fun cacheSavedEvents(userId: String, events: List<Event>) {
    dao.clearForUser(userId)
    val entities = events.map { savedEventEntityFrom(it, userId) }
    dao.insertAll(entities)
  }

  suspend fun saveEventLocally(userId: String, event: Event, savedAt: Timestamp?) {
    val entity = savedEventEntityFrom(event, userId, savedAt)
    dao.insert(entity)
  }

  suspend fun unsaveEventLocally(userId: String, eventId: String) {
    dao.delete(eventId, userId)
  }
}

// Mapping helpers
private fun SavedEventEntity.toEvent(): Event {
  val ts = dateSeconds?.let { Timestamp(it, dateNanoseconds ?: 0) }
  val endTs = endDateSeconds?.let { Timestamp(it, endDateNanoseconds ?: 0) }
  return Event(
      uid = id,
      title = title,
      url = null,
      description = description,
      date = ts,
      endDate = endTs,
      location = Location.from(locationName, locationLat, locationLng),
      tags = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(","),
      public = isPublic,
      ownerId = ownerId,
      imageUrl = imageUrl,
      capacity = capacity,
      participantIds =
          if (participantIdsCsv.isBlank()) emptyList() else participantIdsCsv.split(","))
}

private fun savedEventEntityFrom(
    event: Event,
    userId: String,
    savedAt: Timestamp? = null
): SavedEventEntity =
    SavedEventEntity(
        id = event.uid,
        userId = userId,
        title = event.title,
        description = event.description,
        dateSeconds = event.date?.seconds,
        dateNanoseconds = event.date?.nanoseconds,
        endDateSeconds = event.endDate?.seconds,
        endDateNanoseconds = event.endDate?.nanoseconds,
        locationName = event.location.name,
        locationLat = event.location.latitude,
        locationLng = event.location.longitude,
        tagsCsv = event.tags.joinToString(","),
        isPublic = event.public,
        ownerId = event.ownerId,
        imageUrl = event.imageUrl,
        capacity = event.capacity,
        participantIdsCsv = event.participantIds.joinToString(","),
        savedAtSeconds = savedAt?.seconds)

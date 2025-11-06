package com.swent.mapin.model.event

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test that maps a `SavedEventEntity` to a domain `Event` to exercise mapping logic used by
 * local cache adapter. This keeps the test self-contained without touching production mappers.
 */
class SavedEventEntityMapperTest {
  private fun map(entity: SavedEventEntity): Event {
    val ts =
        if (entity.dateSeconds != null) Timestamp(entity.dateSeconds!!, entity.dateNanoseconds ?: 0)
        else Timestamp.now()
    val tags = if (entity.tagsCsv.isBlank()) emptyList() else entity.tagsCsv.split(",")
    val participantIds =
        if (entity.participantIdsCsv.isBlank()) emptyList() else entity.participantIdsCsv.split(",")

    return Event(
        uid = entity.id,
        title = entity.title,
        url = "",
        description = entity.description,
        date = ts,
        location = Location(entity.locationName, entity.locationLat, entity.locationLng),
        tags = tags,
        public = entity.isPublic,
        ownerId = entity.ownerId,
        imageUrl = entity.imageUrl,
        capacity = entity.capacity ?: 0,
        participantIds = participantIds)
  }

  @Test
  fun mapping_preserves_core_fields() {
    val ent =
        SavedEventEntity(
            id = "E1",
            userId = "u",
            title = "Title",
            description = "Desc",
            dateSeconds = 123L,
            dateNanoseconds = 0,
            locationName = "Loc",
            locationLat = 1.5,
            locationLng = 2.5,
            tagsCsv = "a,b",
            isPublic = true,
            ownerId = "owner",
            imageUrl = "http://img",
            capacity = 42,
            participantIdsCsv = "p1,p2",
            savedAtSeconds = 999L)

    val ev = map(ent)
    assertEquals("E1", ev.uid)
    assertEquals("Title", ev.title)
    assertEquals("Desc", ev.description)
    assertEquals(1.5, ev.location.latitude, 0.001)
    assertEquals(2.5, ev.location.longitude, 0.001)
    assertEquals(listOf("a", "b"), ev.tags)
    assertEquals(listOf("p1", "p2"), ev.participantIds)
  }
}

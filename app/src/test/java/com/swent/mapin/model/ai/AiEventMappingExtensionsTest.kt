package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test

class AiEventMappingExtensionsTest {

  @Test
  fun `toAiEventSummary maps basic event fields correctly`() {
    val event =
        Event(
            uid = "event123",
            title = "Tech Conference",
            date = Timestamp.now(),
            endDate = null,
            tags = listOf("tech", "networking"),
            location = Location.from("EPFL", 46.5197, 6.5657),
            capacity = null,
            participantIds = emptyList(),
            price = 0.0)

    val summary = event.toAiEventSummary()

    assertEquals("event123", summary.id)
    assertEquals("Tech Conference", summary.title)
    assertNotNull(summary.startTime)
    assertNull(summary.endTime)
    assertEquals(listOf("tech", "networking"), summary.tags)
    assertEquals("EPFL", summary.locationDescription)
    assertNull(summary.capacityRemaining)
    assertEquals(0.0, summary.price, 0.01)
    assertNull(summary.distanceKm)
  }

  @Test
  fun `toAiEventSummary calculates capacity remaining correctly`() {
    val event =
        Event(
            uid = "event456",
            title = "Workshop",
            date = Timestamp.now(),
            location = Location.from("Room 101", 0.0, 0.0),
            capacity = 50,
            participantIds = listOf("user1", "user2", "user3"),
            price = 25.0)

    val summary = event.toAiEventSummary()

    assertEquals(47, summary.capacityRemaining)
    assertEquals(25.0, summary.price, 0.01)
  }

  @Test
  fun `toAiEventSummary handles full capacity`() {
    val event =
        Event(
            uid = "event789",
            title = "Full Event",
            date = Timestamp.now(),
            location = Location.from("Venue", 0.0, 0.0),
            capacity = 3,
            participantIds = listOf("user1", "user2", "user3"))

    val summary = event.toAiEventSummary()

    assertEquals(0, summary.capacityRemaining)
  }

  @Test
  fun `toAiEventSummary uses custom distance and location description`() {
    val event =
        Event(
            uid = "event111",
            title = "Local Event",
            date = Timestamp.now(),
            location = Location.from("Default Location", 0.0, 0.0))

    val summary = event.toAiEventSummary(distanceKm = 2.5, locationDescription = "Near city center")

    assertEquals(2.5, summary.distanceKm!!, 0.01)
    assertEquals("Near city center", summary.locationDescription)
  }

  @Test
  fun `toAiEventSummaries converts list of events`() {
    val events =
        listOf(
            Event(
                uid = "e1",
                title = "Event 1",
                date = Timestamp.now(),
                location = Location.from("Loc1", 0.0, 0.0)),
            Event(
                uid = "e2",
                title = "Event 2",
                date = Timestamp.now(),
                location = Location.from("Loc2", 0.0, 0.0)),
            Event(
                uid = "e3",
                title = "Event 3",
                date = Timestamp.now(),
                location = Location.from("Loc3", 0.0, 0.0)))

    val summaries = events.toAiEventSummaries()

    assertEquals(3, summaries.size)
    assertEquals("e1", summaries[0].id)
    assertEquals("Event 1", summaries[0].title)
    assertEquals("e2", summaries[1].id)
    assertEquals("Event 2", summaries[1].title)
  }

  @Test
  fun `toAiEventSummaries with distance provider`() {
    val events =
        listOf(
            Event(
                uid = "e1",
                title = "Event 1",
                date = Timestamp.now(),
                location = Location.from("Loc1", 0.0, 0.0)),
            Event(
                uid = "e2",
                title = "Event 2",
                date = Timestamp.now(),
                location = Location.from("Loc2", 0.0, 0.0)))

    val distanceProvider: (String) -> Double? = { eventId ->
      when (eventId) {
        "e1" -> 1.5
        "e2" -> 3.2
        else -> null
      }
    }

    val summaries = events.toAiEventSummaries(distanceProvider = distanceProvider)

    assertEquals(1.5, summaries[0].distanceKm!!, 0.01)
    assertEquals(3.2, summaries[1].distanceKm!!, 0.01)
  }

  @Test
  fun `toAiEventSummaries with location description provider`() {
    val events =
        listOf(
            Event(
                uid = "e1",
                title = "Event 1",
                date = Timestamp.now(),
                location = Location.from("Loc1", 0.0, 0.0)),
            Event(
                uid = "e2",
                title = "Event 2",
                date = Timestamp.now(),
                location = Location.from("Loc2", 0.0, 0.0)))

    val locationProvider: (String) -> String? = { eventId ->
      when (eventId) {
        "e1" -> "Downtown Lausanne"
        "e2" -> "Geneva Center"
        else -> null
      }
    }

    val summaries = events.toAiEventSummaries(locationDescriptionProvider = locationProvider)

    assertEquals("Downtown Lausanne", summaries[0].locationDescription)
    assertEquals("Geneva Center", summaries[1].locationDescription)
  }
}

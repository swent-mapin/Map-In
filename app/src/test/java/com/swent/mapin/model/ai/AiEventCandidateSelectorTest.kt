package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import java.util.Date
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class AiEventCandidateSelectorTest {

  private fun timestamp(daysFromNow: Int): Timestamp {
    val millis = System.currentTimeMillis() + (daysFromNow * 24 * 60 * 60 * 1000L)
    return Timestamp(Date(millis))
  }

  private fun createEvent(
      id: String,
      title: String,
      daysFromNow: Int,
      location: Location = Location("Test Location", 46.5197, 6.5657),
      tags: List<String> = emptyList(),
      capacity: Int? = null,
      participantIds: List<String> = emptyList(),
      endDaysFromNow: Int? = null
  ): Event {
    return Event(
        uid = id,
        title = title,
        date = timestamp(daysFromNow),
        endDate = endDaysFromNow?.let { timestamp(it) },
        location = location,
        tags = tags,
        capacity = capacity,
        participantIds = participantIds,
        price = 0.0,
        ownerId = "owner123",
        description = "Test description")
  }

  private class FakeDistanceCalculator(
      private val distanceMap: Map<Pair<Location, Location>, Double>
  ) : DistanceCalculator {
    override fun distanceKm(from: Location, to: Location): Double? {
      return distanceMap[Pair(from, to)]
    }
  }

  @Test
  fun `selectCandidates returns all events sorted by time when no filters provided`() {
    val selector = AiEventCandidateSelector()
    val events =
        listOf(
            createEvent("1", "Event 1", 3),
            createEvent("2", "Event 2", 1),
            createEvent("3", "Event 3", 2))

    val candidates = selector.selectCandidates(allEvents = events)

    assertEquals(3, candidates.size)
    assertEquals("2", candidates[0].id)
    assertEquals("3", candidates[1].id)
    assertEquals("1", candidates[2].id)
  }

  @Test
  fun `selectCandidates filters by time window`() {
    val selector = AiEventCandidateSelector()
    val events =
        listOf(
            createEvent("1", "Event 1", 1),
            createEvent("2", "Event 2", 5),
            createEvent("3", "Event 3", 10),
            createEvent("4", "Event 4", -1))

    val timeWindow = timestamp(0)..timestamp(7)
    val candidates = selector.selectCandidates(allEvents = events, userQueryTimeWindow = timeWindow)

    assertEquals(2, candidates.size)
    assertTrue(candidates.any { it.id == "1" })
    assertTrue(candidates.any { it.id == "2" })
  }

  @Test
  fun `selectCandidates does not filter by time when window is null`() {
    val selector = AiEventCandidateSelector()
    val events =
        listOf(
            createEvent("1", "Event 1", 1),
            createEvent("2", "Event 2", 5),
            createEvent("3", "Event 3", 10))

    val candidates = selector.selectCandidates(allEvents = events, userQueryTimeWindow = null)

    assertEquals(3, candidates.size)
  }

  @Test
  fun `selectCandidates sorts by distance when user location is available`() {
    val userLocation = Location("User", 46.5197, 6.5657)
    val location1 = Location("Far", 47.0, 7.0)
    val location2 = Location("Near", 46.52, 6.57)
    val location3 = Location("Medium", 46.7, 6.8)

    val distanceMap =
        mapOf(
            Pair(userLocation, location1) to 50.0,
            Pair(userLocation, location2) to 2.0,
            Pair(userLocation, location3) to 25.0)

    val distanceCalculator = FakeDistanceCalculator(distanceMap)
    val selector =
        AiEventCandidateSelector(distanceCalculator = distanceCalculator, maxDistanceKm = 100.0)

    val events =
        listOf(
            createEvent("1", "Far Event", 1, location = location1),
            createEvent("2", "Near Event", 2, location = location2),
            createEvent("3", "Medium Event", 3, location = location3))

    val candidates = selector.selectCandidates(allEvents = events, userLocation = userLocation)

    assertEquals(3, candidates.size)
    assertEquals("2", candidates[0].id)
    assertEquals(2.0, candidates[0].distanceKm)
    assertEquals("3", candidates[1].id)
    assertEquals(25.0, candidates[1].distanceKm)
    assertEquals("1", candidates[2].id)
    assertEquals(50.0, candidates[2].distanceKm)
  }

  @Test
  fun `selectCandidates clips results to maxCandidates`() {
    val selector = AiEventCandidateSelector(maxCandidates = 2)

    val events =
        listOf(
            createEvent("1", "Event 1", 1),
            createEvent("2", "Event 2", 2),
            createEvent("3", "Event 3", 3),
            createEvent("4", "Event 4", 4))

    val candidates = selector.selectCandidates(allEvents = events)

    assertEquals(2, candidates.size)
    assertEquals("1", candidates[0].id)
    assertEquals("2", candidates[1].id)
  }

  @Test
  fun `selectCandidates filters by maxDistanceKm`() {
    val userLocation = Location("User", 46.5197, 6.5657)
    val location1 = Location("Far", 47.0, 7.0)
    val location2 = Location("Near", 46.52, 6.57)
    val location3 = Location("TooFar", 48.0, 8.0)

    val distanceMap =
        mapOf(
            Pair(userLocation, location1) to 15.0,
            Pair(userLocation, location2) to 2.0,
            Pair(userLocation, location3) to 150.0)

    val distanceCalculator = FakeDistanceCalculator(distanceMap)
    val selector = AiEventCandidateSelector(distanceCalculator = distanceCalculator)

    val events =
        listOf(
            createEvent("1", "Far Event", 1, location = location1),
            createEvent("2", "Near Event", 2, location = location2),
            createEvent("3", "TooFar Event", 3, location = location3))

    val candidates = selector.selectCandidates(allEvents = events, userLocation = userLocation)

    assertEquals(2, candidates.size)
    assertTrue(candidates.all { it.distanceKm!! <= 20.0 })
    assertTrue(candidates.any { it.id == "1" })
    assertTrue(candidates.any { it.id == "2" })
  }

  @Test
  fun `selectCandidates maps event fields to AiEventSummary correctly`() {
    val selector = AiEventCandidateSelector()
    val location = Location("EPFL", 46.5197, 6.5657)
    val events =
        listOf(
            createEvent(
                id = "event123",
                title = "Tech Conference",
                daysFromNow = 1,
                location = location,
                tags = listOf("tech", "networking"),
                capacity = 50,
                participantIds = listOf("user1", "user2")))

    val candidates = selector.selectCandidates(allEvents = events)

    assertEquals(1, candidates.size)
    val summary = candidates[0]
    assertEquals("event123", summary.id)
    assertEquals("Tech Conference", summary.title)
    assertNotNull(summary.startTime)
    assertEquals(listOf("tech", "networking"), summary.tags)
    assertEquals("EPFL", summary.locationDescription)
    assertEquals(48, summary.capacityRemaining)
    assertEquals(0.0, summary.price)
  }

  @Test
  fun `selectCandidates includes distance when location is provided`() {
    val userLocation = Location("User", 46.5197, 6.5657)
    val eventLocation = Location("Event", 46.52, 6.57)

    val distanceMap = mapOf(Pair(userLocation, eventLocation) to 5.5)
    val distanceCalculator = FakeDistanceCalculator(distanceMap)
    val selector = AiEventCandidateSelector(distanceCalculator = distanceCalculator)

    val events = listOf(createEvent("1", "Event 1", 1, location = eventLocation))

    val candidates = selector.selectCandidates(allEvents = events, userLocation = userLocation)

    assertEquals(1, candidates.size)
    assertEquals(5.5, candidates[0].distanceKm)
  }

  @Test
  fun `selectCandidates does not include distance when user location is null`() {
    val selector = AiEventCandidateSelector()
    val events = listOf(createEvent("1", "Event 1", 1))

    val candidates = selector.selectCandidates(allEvents = events, userLocation = null)

    assertEquals(1, candidates.size)
    assertNull(candidates[0].distanceKm)
  }

  @Test
  fun `selectCandidates combines time and distance filtering`() {
    val userLocation = Location("User", 46.5197, 6.5657)
    val nearLocation = Location("Near", 46.52, 6.57)
    val farLocation = Location("Far", 48.0, 8.0)

    val distanceMap =
        mapOf(Pair(userLocation, nearLocation) to 5.0, Pair(userLocation, farLocation) to 150.0)

    val distanceCalculator = FakeDistanceCalculator(distanceMap)
    val selector = AiEventCandidateSelector(distanceCalculator = distanceCalculator)

    val events =
        listOf(
            createEvent("1", "Near Future", 1, location = nearLocation),
            createEvent("2", "Far Future", 2, location = farLocation),
            createEvent("3", "Near Past", -5, location = nearLocation))

    val timeWindow = timestamp(0)..timestamp(7)
    val candidates =
        selector.selectCandidates(
            allEvents = events, userLocation = userLocation, userQueryTimeWindow = timeWindow)

    assertEquals(1, candidates.size)
    assertEquals("1", candidates[0].id)
  }
}

package com.swent.mapin.model.event

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.ui.filters.Filters
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LocalEventRepositoryTest {

  // ---------------------------------------------------------------------
  // defaultSampleEvents tests – still valid (the function still exists)
  // ---------------------------------------------------------------------
  @Test
  fun defaultSampleEvents_returnsNonEmptyList() {
    val events = LocalEventRepository.defaultSampleEvents()
    assertNotNull(events)
    assertTrue(events.isNotEmpty())
  }

  @Test
  fun defaultSampleEvents_containsMusicFestival() {
    val events = LocalEventRepository.defaultSampleEvents()
    val musicFestival = events.find { it.title == "Music Festival" }
    assertNotNull(musicFestival)
    assertEquals("event1", musicFestival?.uid)
    assertEquals("Live music and food", musicFestival?.description)
  }

  // ---------------------------------------------------------------------
  // Saved / joined / owned events – the repository now stores them internally
  // ---------------------------------------------------------------------

  @Test
  fun `join adds user to participants and to joined list`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user99"

    repo.editEventAsUser("event1", userId, join = true)

    val event = repo.getEvent("event1")
    assertTrue(event.participantIds.contains(userId))
    assertEquals(listOf("event1"), repo.getJoinedEvents(userId).map { it.uid })
  }

  @Test
  fun `leave removes user from participants and joined list`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user99"

    repo.editEventAsUser("event1", userId, join = true)
    repo.editEventAsUser("event1", userId, join = false)

    val event = repo.getEvent("event1")
    assertFalse(event.participantIds.contains(userId))
    assertTrue(repo.getJoinedEvents(userId).isEmpty())
  }

  @Test
  fun `join does not add user if already participant`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user99"

    repo.editEventAsUser("event1", userId, join = true)
    val sizeBefore = repo.getEvent("event1").participantIds.size

    // Try to join again
    repo.editEventAsUser("event1", userId, join = true)
    val sizeAfter = repo.getEvent("event1").participantIds.size

    assertEquals(sizeBefore, sizeAfter)
  }

  @Test
  fun `leave does not remove user if not participant`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user99"

    // Try to leave without joining first
    repo.editEventAsUser("event1", userId, join = false)

    val event = repo.getEvent("event1")
    assertFalse(event.participantIds.contains(userId))
  }

  @Test(expected = IllegalStateException::class)
  fun `join throws exception when event is at full capacity`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user999"

    // Event2 has capacity 500 and 386 participants (user2 + 385 generated)
    // We need to fill it up first
    val event = repo.getEvent("event2")
    val spotsLeft = event.capacity!! - event.participantIds.size

    // Fill remaining spots
    for (i in 1..spotsLeft) {
      repo.editEventAsUser("event2", "filler$i", join = true)
    }

    // Now try to join when full
    repo.editEventAsUser("event2", userId, join = true)
  }

  @Test
  fun `getSavedEvents returns empty list for user with no saved events`() = runTest {
    val repo = LocalEventRepository()
    val saved = repo.getSavedEvents("nonExistentUser")
    assertTrue(saved.isEmpty())
  }

  @Test
  fun `getJoinedEvents returns empty list for user with no joined events`() = runTest {
    val repo = LocalEventRepository()
    val joined = repo.getJoinedEvents("nonExistentUser")
    assertTrue(joined.isEmpty())
  }

  @Test
  fun `getOwnedEvents returns empty list for user with no owned events`() = runTest {
    val repo = LocalEventRepository()
    val owned = repo.getOwnedEvents("nonExistentUser")
    assertTrue(owned.isEmpty())
  }

  @Test
  fun `getOwnedEvents returns owned events for owner`() = runTest {
    val repo = LocalEventRepository()
    val newEvent =
        Event(
            uid = "",
            title = "Owner Test Event",
            description = "test",
            date = Timestamp.now(),
            location = Location("Test", 0.0, 0.0),
            tags = listOf("test"),
            public = true,
            ownerId = "testOwner",
            participantIds = emptyList())

    repo.addEvent(newEvent)

    val owned = repo.getOwnedEvents("testOwner")
    assertEquals(1, owned.size)
    assertEquals("Owner Test Event", owned[0].title)
  }

  @Test
  fun `deleteEvent removes event from all user lists`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user99"

    // Join and save the event
    repo.editEventAsUser("event1", userId, join = true)
    assertTrue(repo.getJoinedEvents(userId).any { it.uid == "event1" })

    repo.editEventAsUser("event1", userId, join = false)
    assertFalse(repo.getJoinedEvents(userId).any { it.uid == "event1" })

    // Delete the event
    repo.deleteEvent("event1")

    // Verify it's removed from all lists
    assertFalse(repo.getJoinedEvents(userId).any { it.uid == "event1" })
    assertFalse(repo.getSavedEvents(userId).any { it.uid == "event1" })
    assertFalse(repo.getAllEventsForTestsOnly().any { it.uid == "event1" })
  }

  // ---------------------------------------------------------------------
  // getEvent
  // ---------------------------------------------------------------------
  @Test
  fun `getEvent returns existing event`() = runTest {
    val repo = LocalEventRepository()
    val event = repo.getEvent("event1")
    assertEquals("event1", event.uid)
  }

  @Test(expected = NoSuchElementException::class)
  fun `getEvent throws for missing event`() = runTest {
    val repo = LocalEventRepository()
    repo.getEvent("missing")
  }

  // ---------------------------------------------------------------------
  // addEvent
  // ---------------------------------------------------------------------
  @Test
  fun `addEvent stores event with generated UID and adds owner to participants`() = runTest {
    val repo = LocalEventRepository()
    val newEvent =
        Event(
            uid = "",
            title = "My new event",
            description = "test",
            date = Timestamp.now(),
            location = Location("Somewhere", 0.0, 0.0),
            tags = listOf("test"),
            public = true,
            ownerId = "user42",
            participantIds = emptyList())

    repo.addEvent(newEvent)

    val found = repo.getAllEventsForTestsOnly().find { it.title == "My new event" }!!
    assertTrue(found.uid.startsWith("event"))
    assertTrue(found.participantIds.contains("user42"))
  }

  @Test
  fun `addEvent with existing UID uses that UID`() = runTest {
    val repo = LocalEventRepository()
    val newEvent =
        Event(
            uid = "customEvent123",
            title = "Custom ID Event",
            description = "test",
            date = Timestamp.now(),
            location = Location("Test", 0.0, 0.0),
            tags = listOf("test"),
            public = true,
            ownerId = "user42",
            participantIds = emptyList())

    repo.addEvent(newEvent)

    val event = repo.getEvent("customEvent123")
    assertEquals("Custom ID Event", event.title)
  }

  @Test
  fun `addEvent does not duplicate owner in participants if already present`() = runTest {
    val repo = LocalEventRepository()
    val newEvent =
        Event(
            uid = "",
            title = "Test Event",
            description = "test",
            date = Timestamp.now(),
            location = Location("Test", 0.0, 0.0),
            tags = listOf("test"),
            public = true,
            ownerId = "user42",
            participantIds = listOf("user42")) // Owner already in participants

    repo.addEvent(newEvent)

    val found = repo.getAllEventsForTestsOnly().find { it.title == "Test Event" }!!
    assertEquals(1, found.participantIds.filter { it == "user42" }.size)
  }

  @Test
  fun `getNewUid generates sequential IDs`() {
    val repo = LocalEventRepository()
    val id1 = repo.getNewUid()
    val id2 = repo.getNewUid()

    assertTrue(id1.startsWith("event"))
    assertTrue(id2.startsWith("event"))
    assertNotEquals(id1, id2)
  }

  // ---------------------------------------------------------------------
  // editEventAsOwner
  // ---------------------------------------------------------------------
  @Test
  fun `editEventAsOwner updates existing event`() = runTest {
    val repo = LocalEventRepository()
    val original = repo.getEvent("event1")
    val updated = original.copy(title = "Updated Title")

    repo.editEventAsOwner("event1", updated)

    val fetched = repo.getEvent("event1")
    assertEquals("Updated Title", fetched.title)
  }

  @Test(expected = NoSuchElementException::class)
  fun `editEventAsOwner throws for missing event`() = runTest {
    val repo = LocalEventRepository()
    val fakeEvent =
        Event(
            uid = "missing",
            title = "Test",
            description = "test",
            date = Timestamp.now(),
            location = Location("Test", 0.0, 0.0),
            tags = emptyList(),
            public = true,
            ownerId = "user1",
            participantIds = emptyList())

    repo.editEventAsOwner("missing", fakeEvent)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `editEventAsOwner throws when non-owner tries to edit`() = runTest {
    val repo = LocalEventRepository()
    val original = repo.getEvent("event1")
    val modified = original.copy(ownerId = "differentUser")

    repo.editEventAsOwner("event1", modified)
  }

  @Test
  fun `editEventAsOwner adds owner to participants if not present`() = runTest {
    val repo = LocalEventRepository()
    val original = repo.getEvent("event1")
    // Remove owner from participants
    val withoutOwner = original.copy(participantIds = emptyList())

    repo.editEventAsOwner("event1", withoutOwner)

    val fetched = repo.getEvent("event1")
    assertTrue(fetched.participantIds.contains(original.ownerId))
  }

  // ---------------------------------------------------------------------
  // editEventAsUser
  // ---------------------------------------------------------------------
  @Test(expected = NoSuchElementException::class)
  fun `editEventAsUser throws for missing event`() = runTest {
    val repo = LocalEventRepository()
    repo.editEventAsUser("missing", "user99", join = true)
  }

  // ---------------------------------------------------------------------
  // deleteEvent
  // ---------------------------------------------------------------------
  @Test
  fun `deleteEvent removes existing event`() = runTest {
    val repo = LocalEventRepository()
    repo.deleteEvent("event1")
    assertFalse(repo.getAllEventsForTestsOnly().any { it.uid == "event1" })
  }

  @Test(expected = NoSuchElementException::class)
  fun `deleteEvent throws for missing event`() = runTest {
    val repo = LocalEventRepository()
    repo.deleteEvent("missing")
  }

  // ---------------------------------------------------------------------
  // getFilteredEvents - Basic filters
  // ---------------------------------------------------------------------
  @Test
  fun `getFilteredEvents filters by tag`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(tags = setOf("Music"))
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { it.tags.contains("Music") })
    assertTrue(result.any { it.title == "Music Festival" })
  }

  @Test
  fun `getFilteredEvents filters by multiple tags`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(tags = setOf("Music", "Technology"))
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { it.tags.contains("Music") || it.tags.contains("Technology") })
  }

  @Test
  fun `getFilteredEvents limits tags to 10`() = runTest {
    val repo = LocalEventRepository()
    val manyTags = (1..15).map { "Tag$it" }.toSet()
    val filters = Filters(tags = manyTags)

    // Should not crash and should apply limit
    val result = repo.getFilteredEvents(filters)
    assertNotNull(result)
  }

  @Test
  fun `getFilteredEvents filters by maxPrice`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(maxPrice = 60)
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { it.price <= 60.0 })
  }

  @Test
  fun `getFilteredEvents filters by startDate`() = runTest {
    val repo = LocalEventRepository()
    val futureDate = LocalDate.of(2026, 1, 1)
    val filters = Filters(startDate = futureDate)
    val result = repo.getFilteredEvents(filters)

    // All events are in 2025, so should be filtered out
    assertTrue(result.all { it.title == "Music Festival" })
  }

  @Test
  fun `getFilteredEvents filters by endDate`() = runTest {
    val repo = LocalEventRepository()
    val pastDate = LocalDate.of(2024, 12, 31)
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), endDate = pastDate)
    val result = repo.getFilteredEvents(filters)

    // All events are in 2025, so should be filtered out
    assertTrue(result.isEmpty())
  }

  @Test
  fun `getFilteredEvents filters by date range`() = runTest {
    val repo = LocalEventRepository()
    val filters =
        Filters(startDate = LocalDate.of(2024, 1, 1), endDate = LocalDate.of(2026, 12, 31))
    val result = repo.getFilteredEvents(filters)

    // Should include events from 2025
    assertTrue(result.isNotEmpty())
  }

  @Test
  fun `getFilteredEvents filters by place and radius`() = runTest {
    val repo = LocalEventRepository()
    val place = Location("EPFL Campus", 46.5197, 6.5668)
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), place = place, radiusKm = 1)
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.isNotEmpty())
    assertTrue(result.any { it.title == "Music Festival" })
  }

  @Test
  fun `getFilteredEvents with small radius excludes distant events`() = runTest {
    val repo = LocalEventRepository()
    val place = Location("Far Away", 0.0, 0.0) // Far from EPFL
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), place = place, radiusKm = 1)
    val result = repo.getFilteredEvents(filters)

    // No events should be within 1km of (0, 0)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `getFilteredEvents returns empty list for invalid location`() = runTest {
    val repo = LocalEventRepository()
    val invalidPlace = Location("Invalid", Double.NaN, Double.NaN)
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), place = invalidPlace, radiusKm = 10)
    val result = repo.getFilteredEvents(filters)

    // Should handle invalid coordinates gracefully
    assertTrue(result.isEmpty())
  }

  @Test
  fun `getFilteredEvents filters by popularOnly`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), popularOnly = true)
    val result = repo.getFilteredEvents(filters)

    // Popular events have > 10 participants
    // All default events have many participants, so should be included
    assertTrue(result.all { it.participantIds.size > 10 })
  }

  @Test
  fun `getFilteredEvents popularOnly excludes small events`() = runTest {
    val repo = LocalEventRepository()

    // Add a small event
    val smallEvent =
        Event(
            uid = "",
            title = "Small Gathering",
            description = "Just a few friends",
            date = Timestamp.now(),
            location = Location("Test", 46.5, 6.5),
            tags = listOf("Social"),
            public = true,
            ownerId = "user99",
            capacity = null,
            participantIds = listOf("user99", "user100"), // Only 2 participants
            price = 0.0)

    repo.addEvent(smallEvent)

    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), popularOnly = true)
    val result = repo.getFilteredEvents(filters)

    assertFalse(result.any { it.title == "Small Gathering" })
  }

  @Test
  fun `getFilteredEvents with friendsOnly placeholder logic`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user1"

    // Join an event as user1
    repo.editEventAsUser("event1", userId, join = true)

    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), friendsOnly = true)
    val result = repo.getFilteredEvents(filters)

    // The placeholder logic checks if events have participants in user1's joinedEventIds
    // This is a simplified implementation for testing
    assertNotNull(result)
  }

  @Test
  fun `getFilteredEvents combines multiple filters`() = runTest {
    val repo = LocalEventRepository()
    val place = Location("EPFL Campus", 46.5197, 6.5668)
    val filters =
        Filters(
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
            place = place,
            radiusKm = 2,
            maxPrice = 60,
            tags = setOf("Music"))

    val result = repo.getFilteredEvents(filters)

    // Should only include Music Festival (Music tag, 50.0 price, at EPFL, in 2025)
    assertTrue(result.any { it.title == "Music Festival" })
    assertFalse(result.any { it.title == "Tech Conference" }) // Price too high
    assertFalse(result.any { it.title == "Food Festival" }) // Wrong tag
  }

  @Test
  fun `getFilteredEvents returns sorted by date`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1))
    val result = repo.getFilteredEvents(filters)

    // Check that results are sorted by date
    for (i in 0 until result.size - 1) {
      val current = result[i].date?.seconds ?: 0
      val next = result[i + 1].date?.seconds ?: 0
      assertTrue(current <= next)
    }
  }

  @Test
  fun `getFilteredEvents handles null event dates gracefully`() = runTest {
    val repo = LocalEventRepository()
    val eventWithoutDate =
        Event(
            uid = "",
            title = "Dateless Event",
            description = "No date",
            date = null,
            location = Location("Test", 0.0, 0.0),
            tags = listOf("test"),
            public = true,
            ownerId = "user99",
            participantIds = emptyList())

    repo.addEvent(eventWithoutDate)

    val filters = Filters(startDate = LocalDate.of(1970, 1, 1))
    val result = repo.getFilteredEvents(filters)

    // Events without dates should be filtered out
    assertFalse(result.any { it.title == "Dateless Event" })
  }

  // ---------------------------------------------------------------------
  // listenToFilteredEvents
  // ---------------------------------------------------------------------
  @Test
  fun `listenToFilteredEvents immediately invokes callback with current data`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(tags = setOf("Music"))

    var addedEvents: List<Event>? = null
    var modifiedEvents: List<Event>? = null
    var removedEvents: List<Event>? = null

    val registration =
        repo.listenToFilteredEvents(filters) { added, modified, removed ->
          addedEvents = added
          modifiedEvents = modified
          removedEvents = removed
        }

    assertNotNull(addedEvents)
    assertTrue(addedEvents!!.any { it.title == "Music Festival" })
    assertTrue(modifiedEvents!!.isEmpty())
    assertTrue(removedEvents!!.isEmpty())

    registration.remove() // Should be no-op
  }

  // ---------------------------------------------------------------------
  // getOwnedEvents
  // ---------------------------------------------------------------------
  @Test
  fun `getOwnedEvents returns events for specific owner`() = runTest {
    val repo = LocalEventRepository()
    val events = repo.getOwnedEvents("user1")

    assertTrue(events.all { it.ownerId == "user1" })
    assertTrue(events.any { it.title == "Music Festival" })
  }

  @Test
  fun `getOwnedEvents returns empty list for non-existent owner`() = runTest {
    val repo = LocalEventRepository()
    val events = repo.getOwnedEvents("nonExistentUser")
    assertTrue(events.isEmpty())
  }

  @Test
  fun `getOwnedEvents returns sorted by date`() = runTest {
    val repo = LocalEventRepository()
    val events = repo.getOwnedEvents("user1")

    for (i in 0 until events.size - 1) {
      val current = events[i].date?.seconds ?: 0
      val next = events[i + 1].date?.seconds ?: 0
      assertTrue(current <= next)
    }
  }

  // ---------------------------------------------------------------------
  // Edge cases and integration tests
  // ---------------------------------------------------------------------
  @Test
  fun `repository handles multiple users independently`() = runTest {
    val repo = LocalEventRepository()
    val user1 = "user100"
    val user2 = "user200"

    // User1 joins event1
    repo.editEventAsUser("event1", user1, join = true)

    // User2 joins event2
    repo.editEventAsUser("event2", user2, join = true)

    val joined1 = repo.getJoinedEvents(user1)
    val joined2 = repo.getJoinedEvents(user2)

    assertEquals(1, joined1.size)
    assertEquals("event1", joined1[0].uid)

    assertEquals(1, joined2.size)
    assertEquals("event2", joined2[0].uid)
  }

  @Test
  fun `repository initializes with owner in participants`() = runTest {
    val repo = LocalEventRepository()

    // Check that all default events have owner in participants
    val allEvents = repo.getAllEventsForTestsOnly()
    allEvents.forEach { event ->
      if (event.ownerId.isNotBlank()) {
        assertTrue(
            "Event ${event.uid} should have owner ${event.ownerId} in participants",
            event.participantIds.contains(event.ownerId))
      }
    }
  }
}

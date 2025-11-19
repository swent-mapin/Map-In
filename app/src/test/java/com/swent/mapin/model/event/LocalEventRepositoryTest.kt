package com.swent.mapin.model.event

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

  // (the other defaultSampleEvents tests you already have are still fine)

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
  fun `saved events are toggled with editEventAsUser(join = false)`() = runTest {
    val repo = LocalEventRepository()
    val userId = "user99"

    // First "leave" (join = false) → toggles save
    repo.editEventAsUser("event1", userId, join = false)
    var saved = repo.getSavedEvents(userId)
    assertEquals(1, saved.size)
    assertEquals("event1", saved[0].uid)

    // Doing it again unsaves it
    repo.editEventAsUser("event1", userId, join = false)
    saved = repo.getSavedEvents(userId)
    assertTrue(saved.isEmpty())
  }

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
            date = com.google.firebase.Timestamp.now(),
            location = Location("Somewhere", 0.0, 0.0),
            tags = listOf("test"),
            public = true,
            ownerId = "user42",
            participantIds = emptyList())

    repo.addEvent(newEvent)

    val added = repo.getEvent(repo.getNewUid().let { repo.getAllEventsForTestsOnly().last().uid })
    // Simpler: just check that an event with the title exists and owner is participant
    val found = repo.getAllEventsForTestsOnly().find { it.title == "My new event" }!!
    assertTrue(found.uid.startsWith("event"))
    assertTrue(found.participantIds.contains("user42"))
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

  // ---------------------------------------------------------------------
  // deleteEvent
  // ---------------------------------------------------------------------
  @Test
  fun `deleteEvent removes existing event`() = runTest {
    val repo = LocalEventRepository()
    repo.deleteEvent("event1")
    assertFalse(repo.getAllEventsForTestsOnly().any { it.uid == "event1" })
  }

  // ---------------------------------------------------------------------
  // getFilteredEvents
  // ---------------------------------------------------------------------
  @Test
  fun `getFilteredEvents filters by tag`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(tags = setOf("Music"))
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { it.tags.contains("Music") })
  }

  @Test
  fun `getFilteredEvents filters by maxPrice`() = runTest {
    val repo = LocalEventRepository()
    val filters = Filters(maxPrice = 60)
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { it.price <= 60.0 })
  }

  @Test
  fun `getFilteredEvents filters by place and radius`() = runTest {
    val repo = LocalEventRepository()
    val place = Location("EPFL Campus", 46.5197, 6.5668)
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), place = place, radiusKm = 1)
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.isNotEmpty())
    // Music Festival is at EPFL → must be present
    assertTrue(result.any { it.title == "Music Festival" })
  }
}

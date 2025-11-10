package com.swent.mapin.model.event

import com.swent.mapin.model.Location
import com.swent.mapin.ui.filters.Filters
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEventRepositoryTest {

  @Test
  fun defaultSampleEvents_returnsNonEmptyList() {
    val events = LocalEventRepository.defaultSampleEvents()
    assertNotNull(events)
    assertTrue(events.isNotEmpty())
  }

  @Test
  fun defaultSampleEvents_returnsExactly50Events() {
    val events = LocalEventRepository.defaultSampleEvents()
    assertEquals(50, events.size)
  }

  @Test
  fun defaultSampleEvents_allEventsHaveValidIds() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertNotNull(event.uid)
      assertTrue(event.uid.isNotEmpty())
    }
  }

  @Test
  fun defaultSampleEvents_allEventsHaveValidTitles() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertNotNull(event.title)
      assertTrue(event.title.isNotEmpty())
    }
  }

  @Test
  fun defaultSampleEvents_allEventsHaveValidDescriptions() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertNotNull(event.description)
      assertTrue(event.description.isNotEmpty())
    }
  }

  @Test
  fun defaultSampleEvents_allEventsHaveValidDates() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event -> assertNotNull(event.date) }
  }

  @Test
  fun defaultSampleEvents_allEventsHaveValidLocations() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertNotNull(event.location.name)
      assertTrue(event.location.name.isNotEmpty())
      assertTrue(event.location.latitude != 0.0 || event.location.longitude != 0.0)
    }
  }

  @Test
  fun defaultSampleEvents_allEventsHaveValidCoordinates() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertTrue(event.location.latitude >= -90.0 && event.location.latitude <= 90.0)
      assertTrue(event.location.longitude >= -180.0 && event.location.longitude <= 180.0)
    }
  }

  @Test
  fun defaultSampleEvents_allEventsArePublic() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event -> assertTrue(event.public) }
  }

  @Test
  fun defaultSampleEvents_allEventsHaveOwners() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertNotNull(event.ownerId)
      assertTrue(event.ownerId.isNotEmpty())
    }
  }

  @Test
  fun defaultSampleEvents_allEventsHaveTags() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertNotNull(event.tags)
      assertTrue(event.tags.isNotEmpty())
    }
  }

  @Test
  fun defaultSampleEvents_containsMusicFestival() {
    val events = LocalEventRepository.defaultSampleEvents()
    val musicFestival = events.find { it.title == "Music Festival" }
    assertNotNull(musicFestival)
    assertEquals("event1", musicFestival?.uid)
    assertEquals("Live music and food", musicFestival?.description)
  }

  @Test
  fun defaultSampleEvents_containsBasketballGame() {
    val events = LocalEventRepository.defaultSampleEvents()
    val basketballGame = events.find { it.title == "Basketball Game" }
    assertNotNull(basketballGame)
  }

  // ==== Saved Events (LocalEventRepository) ====

  @Test
  fun `saved events initial state is empty`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val user = "userA"
    assertEquals(emptySet<String>(), repo.getSavedEventIds(user))
    assertEquals(emptyList<Event>(), repo.getSavedEvents(user))
  }

  @Test
  fun `saveEventForUser adds id and getSavedEvents returns event`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val user = "userA"
    val id = "event1"

    val ok = repo.saveEventForUser(user, id)
    assertEquals(true, ok)
    assertEquals(setOf(id), repo.getSavedEventIds(user))
    assertEquals(listOf(id), repo.getSavedEvents(user).map { it.uid })
  }

  @Test
  fun `saveEventForUser is idempotent`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val user = "userA"
    val id = "event2"

    repo.saveEventForUser(user, id)
    repo.saveEventForUser(user, id) // no duplicate
    assertEquals(setOf(id), repo.getSavedEventIds(user))
  }

  @Test
  fun `unsaveEventForUser removes id`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val user = "userA"
    val id = "event3"

    repo.saveEventForUser(user, id)
    val removed = repo.unsaveEventForUser(user, id)
    assertEquals(true, removed)
    assertEquals(emptySet<String>(), repo.getSavedEventIds(user))
    assertEquals(emptyList<String>(), repo.getSavedEvents(user).map { it.uid })
  }

  @Test
  fun `per-user isolation for saved events`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val userA = "userA"
    val userB = "userB"

    repo.saveEventForUser(userA, "event1")
    repo.saveEventForUser(userA, "event2")

    assertEquals(setOf("event1", "event2"), repo.getSavedEventIds(userA))
    assertEquals(emptySet<String>(), repo.getSavedEventIds(userB))
  }

  @Test
  fun `saveEventForUser returns false for missing event id`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val user = "userA"
    val ok = repo.saveEventForUser(user, "does-not-exist")
    assertEquals(false, ok)
    assertEquals(emptySet<String>(), repo.getSavedEventIds(user))
  }

  @Test
  fun `getSavedEvents preserves insertion order`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val user = "userA"

    // Local repository keeps a LinkedHashSet under the hood
    repo.saveEventForUser(user, "event2")
    repo.saveEventForUser(user, "event1")

    assertEquals(listOf("event2", "event1"), repo.getSavedEvents(user).map { it.uid })
  }

  // ==== getEvent ====
  @Test
  fun `getEvent returns existing event`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val event = repo.getEvent("event1")
    assertEquals("event1", event.uid)
  }

  @Test(expected = NoSuchElementException::class)
  fun `getEvent throws for missing event`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    repo.getEvent("missing")
  }

  // ==== addEvent ====
  @Test
  fun `addEvent stores event with new UID`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val newEvent = LocalEventRepository.defaultSampleEvents().first().copy(uid = "")
    repo.addEvent(newEvent)
    val added = repo.getAllEvents().find { it.title == newEvent.title }
    assertNotNull(added)
    assertTrue(added!!.uid.isNotEmpty())
    assertTrue(added.participantIds.contains(added.ownerId))
  }

  // ==== editEvent ====
  @Test
  fun `editEvent updates existing event`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val updatedEvent = repo.getEvent("event1").copy(title = "Updated Title")
    repo.editEvent("event1", updatedEvent)
    val fetched = repo.getEvent("event1")
    assertEquals("Updated Title", fetched.title)
  }

  @Test(expected = NoSuchElementException::class)
  fun `editEvent throws for missing event`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val newEvent = LocalEventRepository.defaultSampleEvents().first()
    repo.editEvent("missing", newEvent)
  }

  // ==== deleteEvent ====
  @Test
  fun `deleteEvent removes existing event`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    repo.deleteEvent("event1")
    assertFalse(repo.getAllEvents().any { it.uid == "event1" })
  }

  @Test(expected = NoSuchElementException::class)
  fun `deleteEvent throws for missing event`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    repo.deleteEvent("missing")
  }

  // ==== getFilteredEvents ====
  @Test
  fun `getFilteredEvents filters by tag`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val filters = Filters(tags = setOf("Music"))
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { it.tags.contains("Music") })
  }

  @Test
  fun `getFilteredEvents filters by maxPrice`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val filters = Filters(maxPrice = 60)
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { it.price <= 60 })
  }

  @Test
  fun `getFilteredEvents filters by friendsOnly`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val filters = Filters(friendsOnly = true)
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.all { "user1" in it.participantIds })
  }

  @Test
  fun `getFilteredEvents filters by place and radius`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val place = Location("EPFL Campus", 46.5197, 6.5668)
    // Ensure the startDate is early so that sample events (in 2025) are included
    val filters = Filters(startDate = LocalDate.of(1970, 1, 1), place = place, radiusKm = 0)
    val result = repo.getFilteredEvents(filters)
    assertTrue(result.isNotEmpty())
    assertTrue(result.all { it.location.name == "EPFL Campus" })
  }

  @Test
  fun `getSearchedEvents filters by title`() = runTest {
    val repo = LocalEventRepository(LocalEventRepository.defaultSampleEvents())
    val filters = Filters()
    val result = repo.getSearchedEvents("Music", filters)
    assertTrue(result.all { it.title.contains("Music") })
  }
}

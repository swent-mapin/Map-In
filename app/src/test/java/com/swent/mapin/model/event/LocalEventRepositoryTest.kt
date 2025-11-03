package com.swent.mapin.model.event

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    assertEquals(emptyList<com.swent.mapin.model.event.Event>(), repo.getSavedEvents(user))
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
}

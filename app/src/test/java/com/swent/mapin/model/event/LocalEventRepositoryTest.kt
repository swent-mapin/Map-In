package com.swent.mapin.model

import com.swent.mapin.model.event.LocalEventRepository
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
}

package com.swent.mapin.model.event

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
  fun defaultSampleEvents_allEventsHaveAttendeeCount() {
    val events = LocalEventRepository.defaultSampleEvents()
    events.forEach { event ->
      assertNotNull(event.attendeeCount)
      assertTrue(event.attendeeCount!! > 0)
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
    assertEquals("event4", basketballGame?.uid)
    assertTrue(basketballGame?.tags?.contains("Sports") == true)
  }

  @Test
  fun defaultSampleEvents_containsArtExhibition() {
    val events = LocalEventRepository.defaultSampleEvents()
    val artExhibition = events.find { it.title == "Art Exhibition" }
    assertNotNull(artExhibition)
    assertEquals("event7", artExhibition?.uid)
    assertTrue(artExhibition?.tags?.contains("Art") == true)
  }

  @Test
  fun defaultSampleEvents_containsFoodMarket() {
    val events = LocalEventRepository.defaultSampleEvents()
    val foodMarket = events.find { it.title == "Food Market" }
    assertNotNull(foodMarket)
    assertEquals("event12", foodMarket?.uid)
    assertTrue(foodMarket?.tags?.contains("Food") == true)
  }

  @Test
  fun defaultSampleEvents_containsYogaClass() {
    val events = LocalEventRepository.defaultSampleEvents()
    val yogaClass = events.find { it.title == "Yoga Class" }
    assertNotNull(yogaClass)
    assertEquals("event16", yogaClass?.uid)
    assertTrue(yogaClass?.tags?.contains("Yoga") == true)
  }

  @Test
  fun defaultSampleEvents_allUidsAreUnique() {
    val events = LocalEventRepository.defaultSampleEvents()
    val uids = events.map { it.uid }
    assertEquals(uids.size, uids.toSet().size)
  }

  @Test
  fun defaultSampleEvents_eventsAreAroundEPFL() {
    val events = LocalEventRepository.defaultSampleEvents()
    val epflLatitude = 46.5197
    val epflLongitude = 6.5668

    events.forEach { event ->
      val latDiff = kotlin.math.abs(event.location.latitude - epflLatitude)
      val lonDiff = kotlin.math.abs(event.location.longitude - epflLongitude)
      assertTrue(latDiff < 0.01)
      assertTrue(lonDiff < 0.01)
    }
  }

  @Test
  fun defaultSampleEvents_multipleCalls_returnsSameGeneralData() {
    val events1 = LocalEventRepository.defaultSampleEvents()
    val events2 = LocalEventRepository.defaultSampleEvents()

    assertEquals(events1.size, events2.size)
    for (i in events1.indices) {
      assertEquals(events1[i].uid, events2[i].uid)
      assertEquals(events1[i].title, events2[i].title)
    }
  }

  @Test
  fun defaultSampleEvents_hasVariedAttendeeCount() {
    val events = LocalEventRepository.defaultSampleEvents()
    val attendeeCounts = events.map { it.attendeeCount }

    assertTrue(attendeeCounts.distinct().size > 1)
  }
}

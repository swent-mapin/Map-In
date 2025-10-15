package com.swent.mapin.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleEventRepositoryTest {

  @Test
  fun getSampleEvents_returnsNonEmptyList() {
    val events = SampleEventRepository.getSampleEvents()
    assertNotNull(events)
    assertTrue(events.isNotEmpty())
  }

  @Test
  fun getSampleEvents_returnsExactly5Events() {
    val events = SampleEventRepository.getSampleEvents()
    assertEquals(50, events.size)
  }

  @Test
  fun getSampleEvents_allEventsHaveValidIds() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertNotNull(event.uid)
      assertTrue(event.uid.isNotEmpty())
    }
  }

  @Test
  fun getSampleEvents_allEventsHaveValidTitles() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertNotNull(event.title)
      assertTrue(event.title.isNotEmpty())
    }
  }

  @Test
  fun getSampleEvents_allEventsHaveValidDescriptions() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertNotNull(event.description)
      assertTrue(event.description.isNotEmpty())
    }
  }

  @Test
  fun getSampleEvents_allEventsHaveValidDates() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event -> assertNotNull(event.date) }
  }

  @Test
  fun getSampleEvents_allEventsHaveValidLocations() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertNotNull(event.location.name)
      assertTrue(event.location.name.isNotEmpty())
      assertTrue(event.location.latitude != 0.0 || event.location.longitude != 0.0)
    }
  }

  @Test
  fun getSampleEvents_allEventsHaveValidCoordinates() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertTrue(event.location.latitude >= -90.0 && event.location.latitude <= 90.0)
      assertTrue(event.location.longitude >= -180.0 && event.location.longitude <= 180.0)
    }
  }

  @Test
  fun getSampleEvents_allEventsArePublic() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event -> assertTrue(event.public) }
  }

  @Test
  fun getSampleEvents_allEventsHaveOwners() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertNotNull(event.ownerId)
      assertTrue(event.ownerId.isNotEmpty())
    }
  }

  @Test
  fun getSampleEvents_allEventsHaveTags() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertNotNull(event.tags)
      assertTrue(event.tags.isNotEmpty())
    }
  }

  @Test
  fun getSampleEvents_allEventsHaveAttendeeCount() {
    val events = SampleEventRepository.getSampleEvents()
    events.forEach { event ->
      assertNotNull(event.attendeeCount)
      assertTrue(event.attendeeCount!! > 0)
    }
  }

  @Test
  fun getSampleEvents_containsMusicFestival() {
    val events = SampleEventRepository.getSampleEvents()
    val musicFestival = events.find { it.title == "Music Festival" }
    assertNotNull(musicFestival)
    assertEquals("event1", musicFestival?.uid)
    assertEquals("Live music and food", musicFestival?.description)
  }

  @Test
  fun getSampleEvents_containsBasketballGame() {
    val events = SampleEventRepository.getSampleEvents()
    val basketballGame = events.find { it.title == "Basketball Game" }
    assertNotNull(basketballGame)
    assertEquals("event4", basketballGame?.uid)
    assertTrue(basketballGame?.tags?.contains("Sports") == true)
  }

  @Test
  fun getSampleEvents_containsArtExhibition() {
    val events = SampleEventRepository.getSampleEvents()
    val artExhibition = events.find { it.title == "Art Exhibition" }
    assertNotNull(artExhibition)
    assertEquals("event7", artExhibition?.uid)
    assertTrue(artExhibition?.tags?.contains("Art") == true)
  }

  @Test
  fun getSampleEvents_containsFoodMarket() {
    val events = SampleEventRepository.getSampleEvents()
    val foodMarket = events.find { it.title == "Food Market" }
    assertNotNull(foodMarket)
    assertEquals("event12", foodMarket?.uid)
    assertTrue(foodMarket?.tags?.contains("Food") == true)
  }

  @Test
  fun getSampleEvents_containsYogaClass() {
    val events = SampleEventRepository.getSampleEvents()
    val yogaClass = events.find { it.title == "Yoga Class" }
    assertNotNull(yogaClass)
    assertEquals("event16", yogaClass?.uid)
    assertTrue(yogaClass?.tags?.contains("Yoga") == true)
  }

  @Test
  fun getSampleEvents_allUidsAreUnique() {
    val events = SampleEventRepository.getSampleEvents()
    val uids = events.map { it.uid }
    assertEquals(uids.size, uids.toSet().size)
  }

  @Test
  fun getSampleEvents_eventsAreAroundEPFL() {
    val events = SampleEventRepository.getSampleEvents()
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
  fun getSampleEvents_multipleCalls_returnsSameData() {
    val events1 = SampleEventRepository.getSampleEvents()
    val events2 = SampleEventRepository.getSampleEvents()

    assertEquals(events1.size, events2.size)
    for (i in events1.indices) {
      assertEquals(events1[i].uid, events2[i].uid)
      assertEquals(events1[i].title, events2[i].title)
    }
  }

  @Test
  fun getSampleEvents_hasVariedAttendeeCount() {
    val events = SampleEventRepository.getSampleEvents()
    val attendeeCounts = events.map { it.attendeeCount }

    assertTrue(attendeeCounts.distinct().size > 1)
  }
}

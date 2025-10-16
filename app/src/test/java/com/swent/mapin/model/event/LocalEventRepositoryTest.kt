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
  }

  // Tests for getTopTags functionality
  @Test
  fun getTopTags_returnsCorrectNumberOfTags() {
    val topTags = SampleEventRepository.getTopTags(5)
    assertEquals(5, topTags.size)
  }

  @Test
  fun getTopTags_returnsCustomNumberOfTags() {
    val topTags = SampleEventRepository.getTopTags(3)
    assertEquals(3, topTags.size)
  }

  @Test
  fun getTopTags_returnsTagsInDescendingOrder() {
    val topTags = SampleEventRepository.getTopTags()
    val events = SampleEventRepository.getSampleEvents()
    val tagCounts = mutableMapOf<String, Int>()

    events.forEach { event ->
      event.tags.forEach { tag -> tagCounts[tag] = tagCounts.getOrDefault(tag, 0) + 1 }
    }

    // Verify that returned tags are sorted by frequency
    for (i in 0 until topTags.size - 1) {
      val currentCount = tagCounts[topTags[i]] ?: 0
      val nextCount = tagCounts[topTags[i + 1]] ?: 0
      assertTrue("Tags should be sorted by frequency", currentCount >= nextCount)
    }
  }

  @Test
  fun getTopTags_returnsNonEmptyTags() {
    val topTags = SampleEventRepository.getTopTags()
    assertTrue(topTags.isNotEmpty())
    topTags.forEach { tag -> assertTrue(tag.isNotEmpty()) }
  }

  @Test
  fun getTopTags_containsMostFrequentTags() {
    val topTags = SampleEventRepository.getTopTags()
    val events = SampleEventRepository.getSampleEvents()
    val tagCounts = mutableMapOf<String, Int>()

    events.forEach { event ->
      event.tags.forEach { tag -> tagCounts[tag] = tagCounts.getOrDefault(tag, 0) + 1 }
    }

    // Sports appears in many events (Basketball, Volleyball, Running, Beach Volleyball, etc.)
    assertTrue("Sports should be in top tags", topTags.contains("Sports"))
  }

  @Test
  fun getTopTags_doesNotReturnDuplicates() {
    val topTags = SampleEventRepository.getTopTags()
    val uniqueTags = topTags.toSet()
    assertEquals("No duplicate tags should be returned", topTags.size, uniqueTags.size)
  }

  @Test
  fun getTopTags_handlesRequestForMoreTagsThanAvailable() {
    val events = SampleEventRepository.getSampleEvents()
    val allUniqueTags = events.flatMap { it.tags }.toSet()
    val topTags = SampleEventRepository.getTopTags(allUniqueTags.size + 10)

    // Should return all available unique tags, not more
    assertTrue(topTags.size <= allUniqueTags.size)
  }

  @Test
  fun getTopTags_returnsEmptyListWhenCountIsZero() {
    val topTags = SampleEventRepository.getTopTags(0)
    assertTrue(topTags.isEmpty())
  }

  @Test
  fun getTopTags_allReturnedTagsExistInEvents() {
    val topTags = SampleEventRepository.getTopTags()
    val events = SampleEventRepository.getSampleEvents()
    val allEventTags = events.flatMap { it.tags }.toSet()

    topTags.forEach { tag ->
      assertTrue("Tag $tag should exist in events", allEventTags.contains(tag))
    }
  }
}

package com.swent.mapin.model.event

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

private const val EVENT_LIST_SIZE = 50

class LocalEventListTest {

  // ---------------------------------------------------------------------
  // defaultSampleEvents tests – still valid (the function still exists)
  // ---------------------------------------------------------------------
  @Test
  fun defaultSampleEvents_returnsNonEmptyList() {
    val events = LocalEventList.defaultSampleEvents()
    assertNotNull(events)
    assertTrue(events.isNotEmpty())
  }

  @Test
  fun defaultSampleEvents_containsMusicFestival() {
    val events = LocalEventList.defaultSampleEvents()
    val musicFestival = events.find { it.title == "Music Festival" }
    assertNotNull(musicFestival)
    assertEquals("event1", musicFestival?.uid)
    assertEquals("Live music and food", musicFestival?.description)
  }

  @Test
  fun defaultSampleEvents_containsAllDefaultEvents() {
    val events = LocalEventList.defaultSampleEvents()
    assertEquals(EVENT_LIST_SIZE, events.size)
  }

  @Test
  fun `repository initializes with owner in participants`() = runTest {
    val repo = LocalEventList()

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

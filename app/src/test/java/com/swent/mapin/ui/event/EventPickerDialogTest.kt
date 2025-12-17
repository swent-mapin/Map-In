package com.swent.mapin.ui.event

import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventPickerDialogTest {

  private var defaultLocale: Locale = Locale.getDefault()

  @Before
  fun setUp() {
    defaultLocale = Locale.getDefault()
    Locale.setDefault(Locale.US)
  }

  @After
  fun tearDown() {
    Locale.setDefault(defaultLocale)
  }

  @Test
  fun filterEvents_returnsAllWhenQueryBlank() {
    val events = listOf(sampleEvent(title = "Sample"))

    val result = filterEvents(events, "")

    assertEquals(events, result)
  }

  @Test
  fun filterEvents_matchesTitleDescriptionAndLocation() {
    val events =
        listOf(
            sampleEvent(title = "Morning Run"),
            sampleEvent(description = "Great food here"),
            sampleEvent(location = Location(name = "Library", latitude = 0.0, longitude = 0.0)))

    val byTitle = filterEvents(events, "run")
    val byDescription = filterEvents(events, "food")
    val byLocation = filterEvents(events, "library")

    assertEquals(1, byTitle.size)
    assertEquals("Morning Run", byTitle.first().title)

    assertEquals(1, byDescription.size)
    assertTrue(byDescription.first().description.contains("food", ignoreCase = true))

    assertEquals(1, byLocation.size)
    assertEquals("Library", byLocation.first().location.name)
  }

  @Test
  fun formatEventDate_formatsAsMonthDayYear() {
    val timestamp = Timestamp(1704067200, 0) // Jan 1, 2024 00:00:00 UTC

    val formatted = formatEventDate(timestamp)

    assertEquals("Jan 01, 2024", formatted)
  }

  private fun sampleEvent(
      title: String = "",
      description: String = "",
      location: Location = Location(name = null, latitude = null, longitude = null, geohash = null)
  ): Event {
    return Event(
        uid = title.ifBlank { "id_${description.hashCode()}" },
        title = title,
        description = description,
        location = location)
  }
}

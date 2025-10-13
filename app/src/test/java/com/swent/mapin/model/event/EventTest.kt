package com.swent.mapin.model.event

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTest {

  @Test
  fun event_canBeCreatedWithAllParameters() {
    val timestamp = Timestamp(Date())
    val event =
        Event(
            uid = "event123",
            title = "Test Event",
            url = "https://example.com",
            description = "Test description",
            date = timestamp,
            locationName = "Test Location",
            latitude = 46.5197,
            longitude = 6.5668,
            tags = listOf("Music", "Festival"),
            public = true,
            ownerId = "owner123",
            imageUrl = "https://example.com/image.jpg",
            capacity = 100,
            attendeeCount = 50)

    assertEquals("event123", event.uid)
    assertEquals("Test Event", event.title)
    assertEquals("https://example.com", event.url)
    assertEquals("Test description", event.description)
    assertEquals(timestamp, event.date)
    assertEquals("Test Location", event.locationName)
    assertEquals(46.5197, event.latitude, 0.0001)
    assertEquals(6.5668, event.longitude, 0.0001)
    assertEquals(listOf("Music", "Festival"), event.tags)
    assertTrue(event.public)
    assertEquals("owner123", event.ownerId)
    assertEquals("https://example.com/image.jpg", event.imageUrl)
    assertEquals(100, event.capacity)
    assertEquals(50, event.attendeeCount)
  }

  @Test
  fun event_canBeCreatedWithDefaultValues() {
    val event = Event()

    assertEquals("", event.uid)
    assertEquals("", event.title)
    assertNull(event.url)
    assertEquals("", event.description)
    assertNull(event.date)
    assertEquals("", event.locationName)
    assertEquals(0.0, event.latitude, 0.0001)
    assertEquals(0.0, event.longitude, 0.0001)
    assertEquals(emptyList<String>(), event.tags)
    assertTrue(event.public)
    assertEquals("", event.ownerId)
    assertNull(event.imageUrl)
    assertNull(event.capacity)
    assertEquals(0, event.attendeeCount)
  }

  @Test
  fun event_canBeCreatedWithPartialParameters() {
    val event =
        Event(
            uid = "event456",
            title = "Partial Event",
            locationName = "Some Location",
            public = false)

    assertEquals("event456", event.uid)
    assertEquals("Partial Event", event.title)
    assertEquals("Some Location", event.locationName)
    assertFalse(event.public)
    assertEquals("", event.description)
    assertEquals("", event.ownerId)
  }

  @Test
  fun event_copyCreatesNewInstanceWithModifiedFields() {
    val original =
        Event(
            uid = "original123",
            title = "Original Title",
            description = "Original Description",
            locationName = "Original Location")

    val modified = original.copy(title = "Modified Title", description = "Modified Description")

    assertEquals("original123", modified.uid)
    assertEquals("Modified Title", modified.title)
    assertEquals("Modified Description", modified.description)
    assertEquals("Original Location", modified.locationName)
    assertEquals("Original Title", original.title)
    assertEquals("Original Description", original.description)
  }

  @Test
  fun event_withEmptyTags() {
    val event = Event(uid = "event789", title = "No Tags Event", tags = emptyList())

    assertNotNull(event.tags)
    assertTrue(event.tags.isEmpty())
  }

  @Test
  fun event_withMultipleTags() {
    val tags = listOf("Music", "Outdoor", "Free", "Family-Friendly")
    val event = Event(uid = "event101", title = "Multi Tag Event", tags = tags)

    assertEquals(4, event.tags.size)
    assertTrue(event.tags.contains("Music"))
    assertTrue(event.tags.contains("Outdoor"))
    assertTrue(event.tags.contains("Free"))
    assertTrue(event.tags.contains("Family-Friendly"))
  }

  @Test
  fun event_withNullOptionalFields() {
    val event =
        Event(
            uid = "event202",
            title = "Minimal Event",
            url = null,
            date = null,
            imageUrl = null,
            capacity = null)

    assertNull(event.url)
    assertNull(event.date)
    assertNull(event.imageUrl)
    assertNull(event.capacity)
  }

  @Test
  fun event_withZeroCoordinates() {
    val event =
        Event(uid = "event303", title = "Null Island Event", latitude = 0.0, longitude = 0.0)

    assertEquals(0.0, event.latitude, 0.0001)
    assertEquals(0.0, event.longitude, 0.0001)
  }

  @Test
  fun event_withNegativeCoordinates() {
    val event =
        Event(
            uid = "event404",
            title = "Southern Hemisphere Event",
            latitude = -33.8688,
            longitude = 151.2093)

    assertEquals(-33.8688, event.latitude, 0.0001)
    assertEquals(151.2093, event.longitude, 0.0001)
  }

  @Test
  fun event_publicEventByDefault() {
    val event = Event(uid = "event505", title = "Public Event")
    assertTrue(event.public)
  }

  @Test
  fun event_canBePrivate() {
    val event = Event(uid = "event606", title = "Private Event", public = false)

    assertFalse(event.public)
  }

  @Test
  fun event_withZeroAttendeeCount() {
    val event = Event(uid = "event707", title = "No Attendees", attendeeCount = 0)

    assertEquals(0, event.attendeeCount)
  }

  @Test
  fun event_withLargeAttendeeCount() {
    val event = Event(uid = "event808", title = "Big Event", attendeeCount = 10000)

    assertEquals(10000, event.attendeeCount)
  }

  @Test
  fun event_withCapacity() {
    val event = Event(uid = "event909", title = "Limited Event", capacity = 50, attendeeCount = 30)

    assertEquals(50, event.capacity)
    assertEquals(30, event.attendeeCount)
  }

  @Test
  fun event_equalityCheck() {
    val timestamp = Timestamp(Date())
    val event1 =
        Event(
            uid = "event111",
            title = "Same Event",
            date = timestamp,
            locationName = "Same Location")
    val event2 =
        Event(
            uid = "event111",
            title = "Same Event",
            date = timestamp,
            locationName = "Same Location")

    assertEquals(event1, event2)
  }

  @Test
  fun event_toStringContainsFields() {
    val event =
        Event(uid = "event222", title = "String Test Event", locationName = "String Test Location")

    val eventString = event.toString()
    assertTrue(eventString.contains("event222"))
    assertTrue(eventString.contains("String Test Event"))
  }

  @Test
  fun event_withSpecialCharactersInFields() {
    val event =
        Event(
            uid = "event333",
            title = "Café & Restaurant 🍕",
            description = "Special chars: @#$%^&*()",
            locationName = "Zürich, Café ☕")

    assertEquals("Café & Restaurant 🍕", event.title)
    assertTrue(event.description.contains("@#$%^&*()"))
    assertTrue(event.locationName.contains("☕"))
  }

  @Test
  fun event_withVeryLongDescription() {
    val longDescription = "A".repeat(1000)
    val event =
        Event(uid = "event444", title = "Long Description Event", description = longDescription)

    assertEquals(1000, event.description.length)
  }

  @Test
  fun event_copyPreservesUnchangedFields() {
    val timestamp = Timestamp(Date())
    val original =
        Event(
            uid = "event555",
            title = "Original",
            description = "Original Description",
            date = timestamp,
            locationName = "Original Location",
            latitude = 46.5197,
            longitude = 6.5668,
            tags = listOf("Tag1", "Tag2"),
            public = true,
            ownerId = "owner555",
            imageUrl = "https://example.com/image.jpg",
            capacity = 100,
            attendeeCount = 50)

    val modified = original.copy(title = "Modified")

    assertEquals("Modified", modified.title)
    assertEquals("Original Description", modified.description)
    assertEquals(timestamp, modified.date)
    assertEquals("Original Location", modified.locationName)
    assertEquals(46.5197, modified.latitude, 0.0001)
    assertEquals(6.5668, modified.longitude, 0.0001)
    assertEquals(listOf("Tag1", "Tag2"), modified.tags)
    assertTrue(modified.public)
    assertEquals("owner555", modified.ownerId)
    assertEquals("https://example.com/image.jpg", modified.imageUrl)
    assertEquals(100, modified.capacity)
    assertEquals(50, modified.attendeeCount)
  }

  @Test
  fun event_withEmptyStrings() {
    val event = Event(uid = "", title = "", description = "", locationName = "", ownerId = "")

    assertEquals("", event.uid)
    assertEquals("", event.title)
    assertEquals("", event.description)
    assertEquals("", event.locationName)
    assertEquals("", event.ownerId)
  }
}

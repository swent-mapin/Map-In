package com.swent.mapin.model.event

import com.google.firebase.Timestamp
import com.swent.mapin.model.location.Location
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
            location = Location.from("Test Location", 46.5197, 6.5668),
            tags = listOf("Music", "Festival"),
            public = true,
            ownerId = "owner123",
            imageUrl = "https://example.com/image.jpg",
            capacity = 100,
            participantIds = List(50) { "user$it" })

    assertEquals("event123", event.uid)
    assertEquals("Test Event", event.title)
    assertEquals("https://example.com", event.url)
    assertEquals("Test description", event.description)
    assertEquals(timestamp, event.date)
    assertTrue(event.location.isDefined())
    assertEquals("Test Location", event.location.name)
    assertEquals(46.5197, event.location.latitude!!, 0.0001)
    assertEquals(6.5668, event.location.longitude!!, 0.0001)
    assertEquals(listOf("Music", "Festival"), event.tags)
    assertTrue(event.public)
    assertEquals("owner123", event.ownerId)
    assertEquals("https://example.com/image.jpg", event.imageUrl)
    assertEquals(100, event.capacity)
    assertEquals(50, event.participantIds.size)
  }

  @Test
  fun event_canBeCreatedWithDefaultValues() {
    val event = Event()

    assertEquals("", event.uid)
    assertEquals("", event.title)
    assertNull(event.url)
    assertEquals("", event.description)
    assertNull(event.date)
    assertTrue(event.location == Location.UNDEFINED)
    assertNull(event.location.name)
    assertEquals(emptyList<String>(), event.tags)
    assertTrue(event.public)
    assertEquals("", event.ownerId)
    assertNull(event.imageUrl)
    assertNull(event.capacity)
    assertEquals(0, event.participantIds.size)
  }

  @Test
  fun event_canBeCreatedWithPartialParameters() {
    val event =
        Event(
            uid = "event456",
            title = "Partial Event",
            location = Location.from("Some Location", 0.0, 0.0),
            public = false)

    assertEquals("event456", event.uid)
    assertEquals("Partial Event", event.title)
    assertEquals("Some Location", event.location.name)
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
            location = Location.from("Original Location", 0.0, 0.0))

    val modified = original.copy(title = "Modified Title", description = "Modified Description")

    assertEquals("original123", modified.uid)
    assertEquals("Modified Title", modified.title)
    assertEquals("Modified Description", modified.description)
    assertEquals("Original Location", modified.location.name)
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
        Event(uid = "event303", title = "Null Island Event", location = Location.from("", 0.0, 0.0))

    assertTrue(event.location.isDefined())
    assertEquals(0.0, event.location.latitude!!, 0.0001)
    assertEquals(0.0, event.location.longitude!!, 0.0001)
  }

  @Test
  fun event_withNegativeCoordinates() {
    val event =
        Event(
            uid = "event404",
            title = "Southern Hemisphere Event",
            location = Location.from("", -33.8688, 151.2093))

    assertTrue(event.location.isDefined())
    assertEquals(-33.8688, event.location.latitude!!, 0.0001)
    assertEquals(151.2093, event.location.longitude!!, 0.0001)
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
  fun event_withZeroParticipantCount() {
    val event = Event(uid = "event707", title = "No Attendees", participantIds = emptyList())

    assertEquals(0, event.participantIds.size)
  }

  @Test
  fun event_withLargeParticipantCount() {
    val event =
        Event(uid = "event808", title = "Big Event", participantIds = List(10000) { "user$it" })

    assertEquals(10000, event.participantIds.size)
  }

  @Test
  fun event_withCapacity() {
    val event =
        Event(
            uid = "event909",
            title = "Limited Event",
            capacity = 50,
            participantIds = List(30) { "user$it" })

    assertEquals(50, event.capacity)
    assertEquals(30, event.participantIds.size)
  }

  @Test
  fun event_equalityCheck() {
    val timestamp = Timestamp(Date())
    val event1 =
        Event(
            uid = "event111",
            title = "Same Event",
            date = timestamp,
            location = Location.from("Same Location", 0.0, 0.0))
    val event2 =
        Event(
            uid = "event111",
            title = "Same Event",
            date = timestamp,
            location = Location.from("Same Location", 0.0, 0.0))

    assertEquals(event1, event2)
  }

  @Test
  fun event_toStringContainsFields() {
    val event =
        Event(
            uid = "event222",
            title = "String Test Event",
            location = Location.from("String Test Location", 0.0, 0.0))

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
            location = Location.from("Zürich, Café ☕", 0.0, 0.0))

    assertEquals("Café & Restaurant 🍕", event.title)
    assertTrue(event.description.contains("@#$%^&*()"))
    assertTrue(event.location.name?.contains("☕") ?: false)
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
            location = Location.from("Original Location", 46.5197, 6.5668),
            tags = listOf("Tag1", "Tag2"),
            public = true,
            ownerId = "owner555",
            imageUrl = "https://example.com/image.jpg",
            capacity = 100,
            participantIds = List(50) { "user$it" })

    val modified = original.copy(title = "Modified")

    assertEquals("Modified", modified.title)
    assertEquals("Original Description", modified.description)
    assertEquals(timestamp, modified.date)
    assertTrue(modified.location.isDefined())
    assertEquals("Original Location", modified.location.name)
    assertEquals(46.5197, modified.location.latitude!!, 0.0001)
    assertEquals(6.5668, modified.location.longitude!!, 0.0001)
    assertEquals(listOf("Tag1", "Tag2"), modified.tags)
    assertTrue(modified.public)
    assertEquals("owner555", modified.ownerId)
    assertEquals("https://example.com/image.jpg", modified.imageUrl)
    assertEquals(100, modified.capacity)
    assertEquals(50, modified.participantIds.size)
  }

  @Test
  fun event_withEmptyStrings() {
    val event =
        Event(
            uid = "",
            title = "",
            description = "",
            location = Location.from("", 0.0, 0.0),
            ownerId = "")

    assertEquals("", event.uid)
    assertEquals("", event.title)
    assertEquals("", event.description)
    assertEquals("", event.location.name)
    assertEquals("", event.ownerId)
  }

  // New tests for isValidEvent()
  @Test
  fun isValidEvent_returnsTrue_forValidEvent() {
    val now = Date()
    val event =
        Event(
            uid = "e1",
            title = "Valid",
            description = "Desc",
            date = Timestamp(now),
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "owner")

    assertTrue(event.isValidEvent())
  }

  @Test
  fun isValidEvent_returnsFalse_whenOwnerBlank() {
    val now = Date()
    val event =
        Event(
            uid = "e2",
            title = "Valid",
            description = "Desc",
            date = Timestamp(now),
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "")

    assertFalse(event.isValidEvent())
  }

  @Test
  fun isValidEvent_returnsFalse_whenTitleBlank() {
    val now = Date()
    val event =
        Event(
            uid = "e3",
            title = "",
            description = "Desc",
            date = Timestamp(now),
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "owner")

    assertFalse(event.isValidEvent())
  }

  @Test
  fun isValidEvent_returnsFalse_whenDescriptionBlank() {
    val now = Date()
    val event =
        Event(
            uid = "e4",
            title = "Title",
            description = "",
            date = Timestamp(now),
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "owner")

    assertFalse(event.isValidEvent())
  }

  @Test
  fun isValidEvent_returnsFalse_whenDateMissing() {
    val event =
        Event(
            uid = "e5",
            title = "Title",
            description = "Desc",
            date = null,
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "owner")

    assertFalse(event.isValidEvent())
  }

  @Test
  fun isValidEvent_returnsFalse_whenLocationNameBlank() {
    val now = Date()
    val event =
        Event(
            uid = "e6",
            title = "Title",
            description = "Desc",
            date = Timestamp(now),
            location = Location.from("", 1.0, 2.0),
            ownerId = "owner")

    assertFalse(event.isValidEvent())
  }

  @Test
  fun isValidEvent_returnsFalse_whenEndDateBeforeStart() {
    val now = Date()
    val earlier = Date(now.time - 24 * 60 * 60 * 1000)
    val event =
        Event(
            uid = "e7",
            title = "Title",
            description = "Desc",
            date = Timestamp(now),
            endDate = Timestamp(earlier),
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "owner")

    assertFalse(event.isValidEvent())
  }

  @Test
  fun isValidEvent_returnsTrue_whenEndDateEqualOrAfterStart() {
    val now = Date()
    val equal = Date(now.time)
    val later = Date(now.time + 24 * 60 * 60 * 1000)

    val eventEqual =
        Event(
            uid = "e8",
            title = "Title",
            description = "Desc",
            date = Timestamp(now),
            endDate = Timestamp(equal),
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "owner")

    val eventLater =
        Event(
            uid = "e9",
            title = "Title",
            description = "Desc",
            date = Timestamp(now),
            endDate = Timestamp(later),
            location = Location.from("Loc", 1.0, 2.0),
            ownerId = "owner")

    assertTrue(eventEqual.isValidEvent())
    assertTrue(eventLater.isValidEvent())
  }
}

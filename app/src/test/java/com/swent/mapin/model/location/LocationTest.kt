package com.swent.mapin.model.location

import org.junit.Assert.*
import org.junit.Test

class LocationTest {

  @Test
  fun `location is created with correct parameters using factory method`() {
    val location = Location.from("Paris", 48.8566, 2.3522)

    assertEquals("Paris", location.name)
    assertEquals(48.8566, location.latitude!!, 0.0001)
    assertEquals(2.3522, location.longitude!!, 0.0001)
    assertNotNull(location.geohash)
    assertTrue(location.isDefined())
  }

  @Test
  fun `location with zero coordinates`() {
    val location = Location.from("Null Island", 0.0, 0.0)

    assertEquals("Null Island", location.name)
    assertEquals(0.0, location.latitude!!, 0.0001)
    assertEquals(0.0, location.longitude!!, 0.0001)
    assertNotNull(location.geohash)
    assertTrue(location.isDefined())
  }

  @Test
  fun `location with negative coordinates`() {
    val location = Location.from("South Pole", -90.0, 0.0)

    assertEquals("South Pole", location.name)
    assertEquals(-90.0, location.latitude!!, 0.0001)
    assertEquals(0.0, location.longitude!!, 0.0001)
    assertNotNull(location.geohash)
    assertTrue(location.isDefined())
  }

  @Test
  fun `location equality check`() {
    val location1 = Location.from("EPFL", 46.5197, 6.5668)
    val location2 = Location.from("EPFL", 46.5197, 6.5668)

    assertEquals(location1, location2)
  }

  @Test
  fun `location inequality check with different coordinates`() {
    val location1 = Location.from("EPFL", 46.5197, 6.5668)
    val location2 = Location.from("UNIL", 46.5220, 6.5788)

    assertNotEquals(location1, location2)
  }

  @Test
  fun `location inequality check with different names but same coordinates`() {
    val location1 = Location.from("EPFL", 46.5197, 6.5668)
    val location2 = Location.from("Different Name", 46.5197, 6.5668)

    assertNotEquals(location1, location2)
  }

  @Test
  fun `location copy creates new instance`() {
    val original = Location.from("Original", 10.0, 20.0)
    val copied = original.copy(name = "Copied")

    assertEquals("Copied", copied.name)
    assertEquals(10.0, copied.latitude!!, 0.0001)
    assertEquals(20.0, copied.longitude!!, 0.0001)
    assertEquals("Original", original.name)
    assertEquals(original.geohash, copied.geohash)
  }

  @Test
  fun `location toString contains all fields`() {
    val location = Location.from("Test Location", 12.34, 56.78)
    val locationString = location.toString()

    assertTrue(locationString.contains("Test Location"))
    assertTrue(locationString.contains("12.34"))
    assertTrue(locationString.contains("56.78"))
    assertTrue(locationString.contains("geohash"))
  }

  @Test
  fun `location with special characters in name`() {
    val location = Location.from("Zürich, Café ☕", 47.3769, 8.5417)

    assertEquals("Zürich, Café ☕", location.name)
    assertNotNull(location.geohash)
  }

  @Test
  fun `location with null name returns UNDEFINED`() {
    val location = Location.from(null, 0.0, 0.0)

    assertEquals(Location.UNDEFINED, location)
    assertFalse(location.isDefined())
  }

  @Test
  fun `location with null latitude returns UNDEFINED`() {
    val location = Location.from("Test", null, 0.0)

    assertEquals(Location.UNDEFINED, location)
    assertFalse(location.isDefined())
  }

  @Test
  fun `location with null longitude returns UNDEFINED`() {
    val location = Location.from("Test", 0.0, null)

    assertEquals(Location.UNDEFINED, location)
    assertFalse(location.isDefined())
  }

  @Test
  fun `UNDEFINED location has all null fields`() {
    val location = Location.UNDEFINED

    assertNull(location.name)
    assertNull(location.latitude)
    assertNull(location.longitude)
    assertNull(location.geohash)
    assertFalse(location.isDefined())
  }

  @Test
  fun `isDefined returns true for complete location`() {
    val location = Location.from("Complete", 10.0, 20.0)

    assertTrue(location.isDefined())
  }

  @Test
  fun `isDefined returns false for partially defined location`() {
    val location = Location(name = "Partial", latitude = 10.0, longitude = null, geohash = null)

    assertFalse(location.isDefined())
  }

  @Test
  fun `location with very long name`() {
    val longName = "A".repeat(1000)
    val location = Location.from(longName, 0.0, 0.0)

    assertEquals(longName, location.name)
    assertEquals(1000, location.name?.length)
    assertTrue(location.isDefined())
  }

  @Test
  fun `geohash is consistent for same coordinates`() {
    val location1 = Location.from("Place1", 48.8566, 2.3522)
    val location2 = Location.from("Place2", 48.8566, 2.3522)

    assertEquals(location1.geohash, location2.geohash)
  }

  @Test
  fun `geohash is different for different coordinates`() {
    val location1 = Location.from("Place1", 48.8566, 2.3522)
    val location2 = Location.from("Place2", 46.5197, 6.5668)

    assertNotEquals(location1.geohash, location2.geohash)
  }

  @Test
  fun `NO_NAME constant has expected value`() {
    assertEquals("Unknown Location", Location.NO_NAME)
  }

  @Test
  fun `direct constructor allows manual creation`() {
    val location =
        Location(name = "Manual", latitude = 10.0, longitude = 20.0, geohash = "customhash")

    assertEquals("Manual", location.name)
    assertEquals(10.0, location.latitude!!, 0.0001)
    assertEquals(20.0, location.longitude!!, 0.0001)
    assertEquals("customhash", location.geohash)
    assertTrue(location.isDefined())
  }
}

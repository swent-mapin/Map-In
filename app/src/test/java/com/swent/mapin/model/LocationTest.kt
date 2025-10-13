package com.swent.mapin.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LocationTest {

  @Test
  fun `location is created with correct parameters`() {
    val location = Location("Paris", 48.8566, 2.3522)

    assertEquals("Paris", location.name)
    assertEquals(48.8566, location.latitude, 0.0001)
    assertEquals(2.3522, location.longitude, 0.0001)
  }

  @Test
  fun `location with zero coordinates`() {
    val location = Location("Null Island", 0.0, 0.0)

    assertEquals("Null Island", location.name)
    assertEquals(0.0, location.latitude, 0.0001)
    assertEquals(0.0, location.longitude, 0.0001)
  }

  @Test
  fun `location with negative coordinates`() {
    val location = Location("South Pole", -90.0, 0.0)

    assertEquals("South Pole", location.name)
    assertEquals(-90.0, location.latitude, 0.0001)
    assertEquals(0.0, location.longitude, 0.0001)
  }

  @Test
  fun `location equality check`() {
    val location1 = Location("EPFL", 46.5197, 6.5668)
    val location2 = Location("EPFL", 46.5197, 6.5668)

    assertEquals(location1, location2)
  }

  @Test
  fun `location inequality check`() {
    val location1 = Location("EPFL", 46.5197, 6.5668)
    val location2 = Location("UNIL", 46.5220, 6.5788)

    assertNotEquals(location1, location2)
  }

  @Test
  fun `location copy creates new instance`() {
    val original = Location("Original", 10.0, 20.0)
    val copied = original.copy(name = "Copied")

    assertEquals("Copied", copied.name)
    assertEquals(10.0, copied.latitude, 0.0001)
    assertEquals(20.0, copied.longitude, 0.0001)
    assertEquals("Original", original.name)
  }

  @Test
  fun `location toString contains all fields`() {
    val location = Location("Test Location", 12.34, 56.78)
    val locationString = location.toString()

    assert(locationString.contains("Test Location"))
    assert(locationString.contains("12.34"))
    assert(locationString.contains("56.78"))
  }

  @Test
  fun `location with special characters in name`() {
    val location = Location("Zürich, Café ☕", 47.3769, 8.5417)

    assertEquals("Zürich, Café ☕", location.name)
  }

  @Test
  fun `location with empty name`() {
    val location = Location("", 0.0, 0.0)

    assertEquals("", location.name)
  }

  @Test
  fun `location with very long name`() {
    val longName = "A".repeat(1000)
    val location = Location(longName, 0.0, 0.0)

    assertEquals(longName, location.name)
    assertEquals(1000, location.name.length)
  }
}


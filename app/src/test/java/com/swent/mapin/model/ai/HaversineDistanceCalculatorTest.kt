package com.swent.mapin.model.ai

// Assisted by AI

import com.swent.mapin.model.Location
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.math.abs
import org.junit.Test

class HaversineDistanceCalculatorTest {

  private val calculator = HaversineDistanceCalculator()

  @Test
  fun `distanceKm calculates correct distance between two valid locations`() {
    val epfl = Location("EPFL", 46.5197, 6.5657)
    val lausanneStation = Location("Lausanne Station", 46.5167, 6.6291)

    val distance = calculator.distanceKm(epfl, lausanneStation)

    assertNotNull(distance)
    // Expected distance is approximately 4.5 km
    assertTrue(abs(distance!! - 4.5) < 1.0) // Allow 1 km tolerance
  }

  @Test
  fun `distanceKm returns null for invalid from location`() {
    val invalidLocation = Location("Invalid", 0.0, 0.0)
    val validLocation = Location("Valid", 46.5197, 6.5657)

    val distance = calculator.distanceKm(invalidLocation, validLocation)

    assertNull(distance)
  }

  @Test
  fun `distanceKm returns null for invalid to location`() {
    val validLocation = Location("Valid", 46.5197, 6.5657)
    val invalidLocation = Location("Invalid", 0.0, 0.0)

    val distance = calculator.distanceKm(validLocation, invalidLocation)

    assertNull(distance)
  }

  @Test
  fun `distanceKm returns null when both locations are invalid`() {
    val invalidLocation1 = Location("Invalid1", 0.0, 0.0)
    val invalidLocation2 = Location("Invalid2", 0.0, 0.0)

    val distance = calculator.distanceKm(invalidLocation1, invalidLocation2)

    assertNull(distance)
  }

  @Test
  fun `distanceKm returns zero for same location`() {
    val location = Location("EPFL", 46.5197, 6.5657)

    val distance = calculator.distanceKm(location, location)

    assertNotNull(distance)
    assertEquals(0.0, distance!!, 0.001) // Should be very close to 0
  }

  @Test
  fun `distanceKm calculates distance for locations far apart`() {
    val zurich = Location("Zurich", 47.3769, 8.5417)
    val geneva = Location("Geneva", 46.2044, 6.1432)

    val distance = calculator.distanceKm(zurich, geneva)

    assertNotNull(distance)
    // Expected distance is approximately 230 km
    assertTrue(abs(distance!! - 230.0) < 20.0) // Allow 20 km tolerance
  }

  @Test
  fun `distanceKm handles locations with negative coordinates`() {
    val location1 = Location("South", -33.8688, 151.2093) // Sydney
    val location2 = Location("North", 51.5074, -0.1278) // London

    val distance = calculator.distanceKm(location1, location2)

    assertNotNull(distance)
    // Sydney to London is approximately 17,000 km
    assertTrue(distance!! > 16000.0 && distance < 18000.0)
  }

  @Test
  fun `distanceKm is symmetric`() {
    val location1 = Location("Location1", 46.5197, 6.5657)
    val location2 = Location("Location2", 47.0, 7.0)

    val distance1 = calculator.distanceKm(location1, location2)
    val distance2 = calculator.distanceKm(location2, location1)

    assertNotNull(distance1)
    assertNotNull(distance2)
    assertEquals(distance1!!, distance2!!, 0.001)
  }
}

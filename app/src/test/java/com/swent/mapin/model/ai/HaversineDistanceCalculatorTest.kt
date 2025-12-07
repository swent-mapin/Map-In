package com.swent.mapin.model.ai

// Assisted by AI

import com.swent.mapin.model.location.Location
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlin.math.abs
import org.junit.Test

class HaversineDistanceCalculatorTest {

  private val calculator = HaversineDistanceCalculator()

  @Test
  fun `distanceKm calculates correct distance between two valid locations`() {
    val epfl = Location.from("EPFL", 46.5197, 6.5657)
    val lausanneStation = Location.from("Lausanne Station", 46.5167, 6.6291)

    val distance = calculator.distanceKm(epfl, lausanneStation)

    assertNotNull(distance)
    // Expected distance is approximately 4.5 km
    assertTrue(abs(distance!! - 4.5) < 1.0) // Allow 1 km tolerance
  }

  @Test
  fun `distanceKm calculates distance for zero coordinates from location`() {
    val zeroLocation = Location.from("Zero", 0.0, 0.0)
    val validLocation = Location.from("Valid", 46.5197, 6.5657)

    val distance = calculator.distanceKm(zeroLocation, validLocation)

    assertNotNull(distance)
    assertTrue(distance!! > 0.0)
  }

  @Test
  fun `distanceKm calculates distance for zero coordinates to location`() {
    val validLocation = Location.from("Valid", 46.5197, 6.5657)
    val zeroLocation = Location.from("Zero", 0.0, 0.0)

    val distance = calculator.distanceKm(validLocation, zeroLocation)

    assertNotNull(distance)
    assertTrue(distance!! > 0.0)
  }

  @Test
  fun `distanceKm calculates zero distance when both locations are at zero coordinates`() {
    val zeroLocation1 = Location.from("Zero1", 0.0, 0.0)
    val zeroLocation2 = Location.from("Zero2", 0.0, 0.0)

    val distance = calculator.distanceKm(zeroLocation1, zeroLocation2)

    assertNotNull(distance)
    assertEquals(0.0, distance!!, 0.001)
  }

  @Test
  fun `distanceKm returns zero for same location`() {
    val location = Location.from("EPFL", 46.5197, 6.5657)

    val distance = calculator.distanceKm(location, location)

    assertNotNull(distance)
    assertEquals(0.0, distance!!, 0.001) // Should be very close to 0
  }

  @Test
  fun `distanceKm calculates distance for locations far apart`() {
    val zurich = Location.from("Zurich", 47.3769, 8.5417)
    val geneva = Location.from("Geneva", 46.2044, 6.1432)

    val distance = calculator.distanceKm(zurich, geneva)

    assertNotNull(distance)
    // Expected distance is approximately 230 km
    assertTrue(abs(distance!! - 230.0) < 20.0) // Allow 20 km tolerance
  }

  @Test
  fun `distanceKm handles locations with negative coordinates`() {
    val location1 = Location.from("South", -33.8688, 151.2093) // Sydney
    val location2 = Location.from("North", 51.5074, -0.1278) // London

    val distance = calculator.distanceKm(location1, location2)

    assertNotNull(distance)
    // Sydney to London is approximately 17,000 km
    assertTrue(distance!! > 16000.0 && distance < 18000.0)
  }

  @Test
  fun `distanceKm is symmetric`() {
    val location1 = Location.from("Location1", 46.5197, 6.5657)
    val location2 = Location.from("Location2", 47.0, 7.0)

    val distance1 = calculator.distanceKm(location1, location2)
    val distance2 = calculator.distanceKm(location2, location1)

    assertNotNull(distance1)
    assertNotNull(distance2)
    assertEquals(distance1!!, distance2!!, 0.001)
  }
}

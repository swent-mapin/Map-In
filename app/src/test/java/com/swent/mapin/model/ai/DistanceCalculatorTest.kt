// Assisted by AI
package com.swent.mapin.model.ai

import com.swent.mapin.model.Location
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DistanceCalculator.
 *
 * Tests the Haversine distance calculation between two locations.
 */
class DistanceCalculatorTest {

  private lateinit var calculator: DistanceCalculator

  @Before
  fun setup() {
    calculator = DistanceCalculator()
  }

  @Test
  fun `distanceKm calculates distance between EPFL and Lausanne center correctly`() {
    // EPFL coordinates
    val epfl = Location("EPFL", 46.5197, 6.5657)
    // Lausanne center (Place de la Palud)
    val lausanne = Location("Lausanne", 46.5227, 6.6323)

    val distance = calculator.distanceKm(epfl, lausanne)

    assertNotNull(distance)
    // Expected distance: ~5.5 km (allowing 1 km tolerance for rounding)
    assertEquals(5.5, distance!!, 1.0)
  }

  @Test
  fun `distanceKm calculates distance between Geneva and Zurich correctly`() {
    // Geneva coordinates
    val geneva = Location("Geneva", 46.2044, 6.1432)
    // Zurich coordinates
    val zurich = Location("Zurich", 47.3769, 8.5417)

    val distance = calculator.distanceKm(geneva, zurich)

    assertNotNull(distance)
    // Expected distance: ~225 km (allowing 10 km tolerance)
    assertEquals(225.0, distance!!, 10.0)
  }

  @Test
  fun `distanceKm returns zero for same location`() {
    val location = Location("Same Place", 46.5197, 6.5657)

    val distance = calculator.distanceKm(location, location)

    assertNotNull(distance)
    assertEquals(0.0, distance!!, 0.01)
  }

  @Test
  fun `distanceKm calculates short distances accurately`() {
    // Two locations very close together (100 meters apart in reality)
    val loc1 = Location("Point A", 46.5197, 6.5657)
    val loc2 = Location("Point B", 46.5207, 6.5667) // ~100m apart

    val distance = calculator.distanceKm(loc1, loc2)

    assertNotNull(distance)
    // Should be close to 0.1 km (100 meters)
    assertEquals(0.1, distance!!, 0.05)
  }

  @Test
  fun `distanceKm is symmetric`() {
    val loc1 = Location("Location 1", 46.5197, 6.5657)
    val loc2 = Location("Location 2", 46.5227, 6.6323)

    val distance1to2 = calculator.distanceKm(loc1, loc2)
    val distance2to1 = calculator.distanceKm(loc2, loc1)

    assertNotNull(distance1to2)
    assertNotNull(distance2to1)
    assertEquals(distance1to2!!, distance2to1!!, 0.001)
  }

  @Test
  fun `distanceKm handles locations across equator`() {
    // One location in northern hemisphere
    val north = Location("North", 45.0, 10.0)
    // One location in southern hemisphere
    val south = Location("South", -45.0, 10.0)

    val distance = calculator.distanceKm(north, south)

    assertNotNull(distance)
    // Distance should be around 10,000 km (quarter of Earth's circumference)
    assertEquals(10000.0, distance!!, 500.0)
  }

  @Test
  fun `distanceKm handles locations across date line`() {
    // Location west of date line
    val west = Location("West", 0.0, 179.0)
    // Location east of date line
    val east = Location("East", 0.0, -179.0)

    val distance = calculator.distanceKm(west, east)

    assertNotNull(distance)
    // Should be a small distance (not wrapping around the globe)
    // 2 degrees at equator ≈ 222 km
    assertEquals(222.0, distance!!, 50.0)
  }
}

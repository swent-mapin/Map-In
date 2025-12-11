// Assisted by AI
package com.swent.mapin.model.ai

import com.swent.mapin.model.location.Location
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test

class DistanceCalculatorTest {

  private lateinit var calculator: DistanceCalculator

  @Before
  fun setup() {
    calculator = DistanceCalculator()
  }

  @Test
  fun `distanceKm calculates distance between EPFL and Lausanne center correctly`() {
    val epfl = Location.from("EPFL", 46.5197, 6.5657)
    val lausanne = Location.from("Lausanne", 46.5227, 6.6323)

    val distance = calculator.distanceKm(epfl, lausanne)

    assertNotNull(distance)
    assertEquals(5.5, distance!!, 1.0)
  }

  @Test
  fun `distanceKm calculates distance between Geneva and Zurich correctly`() {
    val geneva = Location.from("Geneva", 46.2044, 6.1432)
    val zurich = Location.from("Zurich", 47.3769, 8.5417)

    val distance = calculator.distanceKm(geneva, zurich)

    assertNotNull(distance)
    assertEquals(225.0, distance!!, 10.0)
  }

  @Test
  fun `distanceKm returns zero for same location`() {
    val location = Location.from("Same Place", 46.5197, 6.5657)

    val distance = calculator.distanceKm(location, location)

    assertNotNull(distance)
    assertEquals(0.0, distance!!, 0.01)
  }

  @Test
  fun `distanceKm calculates short distances accurately`() {
    val loc1 = Location.from("Point A", 46.5197, 6.5657)
    val loc2 = Location.from("Point B", 46.5207, 6.5667)

    val distance = calculator.distanceKm(loc1, loc2)

    assertNotNull(distance)
    assertEquals(0.1, distance!!, 0.05)
  }

  @Test
  fun `distanceKm is symmetric`() {
    val loc1 = Location.from("Location 1", 46.5197, 6.5657)
    val loc2 = Location.from("Location 2", 46.5227, 6.6323)

    val distance1to2 = calculator.distanceKm(loc1, loc2)
    val distance2to1 = calculator.distanceKm(loc2, loc1)

    assertNotNull(distance1to2)
    assertNotNull(distance2to1)
    assertEquals(distance1to2!!, distance2to1!!, 0.001)
  }

  @Test
  fun `distanceKm handles locations across equator`() {
    val north = Location.from("North", 45.0, 10.0)
    val south = Location.from("South", -45.0, 10.0)

    val distance = calculator.distanceKm(north, south)

    assertNotNull(distance)
    assertEquals(10000.0, distance!!, 500.0)
  }

  @Test
  fun `distanceKm handles locations across date line`() {
    val west = Location.from("West", 0.0, 179.0)
    val east = Location.from("East", 0.0, -179.0)

    val distance = calculator.distanceKm(west, east)

    assertNotNull(distance)
    assertEquals(222.0, distance!!, 50.0)
  }
}

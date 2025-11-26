// Assisted by AI
package com.swent.mapin.ui.map.directions

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteInfoTest {

  @Test
  fun `RouteInfo ZERO has zero distance and duration`() {
    assertEquals(0.0, RouteInfo.ZERO.distance, 0.0)
    assertEquals(0.0, RouteInfo.ZERO.duration, 0.0)
  }

  @Test
  fun `formatDistance returns meters for distance less than 1000`() {
    val routeInfo = RouteInfo(distance = 500.0, duration = 60.0)
    assertEquals("500 m", routeInfo.formatDistance())
  }

  @Test
  fun `formatDistance returns meters with zero decimals for exact values`() {
    val routeInfo = RouteInfo(distance = 123.0, duration = 60.0)
    assertEquals("123 m", routeInfo.formatDistance())
  }

  @Test
  fun `formatDistance returns kilometers for distance 1000 or more`() {
    val routeInfo = RouteInfo(distance = 1500.0, duration = 300.0)
    assertEquals("1.5 km", routeInfo.formatDistance())
  }

  @Test
  fun `formatDistance returns kilometers with one decimal place`() {
    val routeInfo = RouteInfo(distance = 2345.6, duration = 400.0)
    assertEquals("2.3 km", routeInfo.formatDistance())
  }

  @Test
  fun `formatDistance handles exactly 1 kilometer`() {
    val routeInfo = RouteInfo(distance = 1000.0, duration = 200.0)
    assertEquals("1.0 km", routeInfo.formatDistance())
  }

  @Test
  fun `formatDistance handles zero distance`() {
    val routeInfo = RouteInfo(distance = 0.0, duration = 0.0)
    assertEquals("0 m", routeInfo.formatDistance())
  }

  @Test
  fun `formatDuration returns minutes for duration less than 1 hour`() {
    val routeInfo = RouteInfo(distance = 1000.0, duration = 600.0) // 10 minutes
    assertEquals("10 min", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration returns minutes for 59 minutes`() {
    val routeInfo = RouteInfo(distance = 5000.0, duration = 3540.0) // 59 minutes
    assertEquals("59 min", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration returns hours and minutes for duration 1 hour or more`() {
    val routeInfo = RouteInfo(distance = 10000.0, duration = 5400.0) // 1h 30min
    assertEquals("1 h 30 min", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration returns only hours when minutes are zero`() {
    val routeInfo = RouteInfo(distance = 15000.0, duration = 7200.0) // 2 hours exactly
    assertEquals("2 h", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration handles multiple hours with minutes`() {
    val routeInfo = RouteInfo(distance = 25000.0, duration = 9900.0) // 2h 45min
    assertEquals("2 h 45 min", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration handles zero duration`() {
    val routeInfo = RouteInfo(distance = 0.0, duration = 0.0)
    assertEquals("0 min", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration rounds down seconds`() {
    val routeInfo = RouteInfo(distance = 1000.0, duration = 659.0) // 10min 59sec
    assertEquals("10 min", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration handles 1 minute`() {
    val routeInfo = RouteInfo(distance = 100.0, duration = 60.0) // 1 minute
    assertEquals("1 min", routeInfo.formatDuration())
  }

  @Test
  fun `formatDuration handles less than 1 minute`() {
    val routeInfo = RouteInfo(distance = 50.0, duration = 30.0) // 30 seconds
    assertEquals("0 min", routeInfo.formatDuration())
  }

  @Test
  fun `RouteInfo can be created with custom values`() {
    val routeInfo = RouteInfo(distance = 1234.5, duration = 567.8)
    assertEquals(1234.5, routeInfo.distance, 0.0)
    assertEquals(567.8, routeInfo.duration, 0.0)
  }

  @Test
  fun `RouteInfo data class equality works correctly`() {
    val routeInfo1 = RouteInfo(distance = 1000.0, duration = 600.0)
    val routeInfo2 = RouteInfo(distance = 1000.0, duration = 600.0)
    val routeInfo3 = RouteInfo(distance = 1000.0, duration = 601.0)

    assertEquals(routeInfo1, routeInfo2)
    assert(routeInfo1 != routeInfo3)
  }

  @Test
  fun `formatDistance handles very large distances`() {
    val routeInfo = RouteInfo(distance = 123456.0, duration = 10000.0)
    assertEquals("123.5 km", routeInfo.formatDistance())
  }

  @Test
  fun `formatDuration handles very long durations`() {
    val routeInfo = RouteInfo(distance = 100000.0, duration = 36000.0) // 10 hours
    assertEquals("10 h", routeInfo.formatDuration())
  }
}

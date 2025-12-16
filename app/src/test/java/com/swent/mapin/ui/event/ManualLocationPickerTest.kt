package com.swent.mapin.ui.event

import com.mapbox.geojson.Point
import com.swent.mapin.model.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualLocationPickerTest {

  @Test
  fun `formatPinnedLocationLabel renders with 5 decimals`() {
    val label = formatPinnedLocationLabel(46.5197321, 6.6322738)
    assertTrue(label.startsWith("Pinned location (46.51973, 6.63227"))
  }

  @Test
  fun `computeStartPoint prefers recenter point`() {
    val recenter = Point.fromLngLat(1.0, 2.0)
    val start = computeStartPoint(recenter, initialLocation = null, searchResults = emptyList())
    assertEquals(recenter, start)
  }

  @Test
  fun `computeStartPoint falls back to initial location`() {
    val initial = Location.from("x", 10.0, 20.0)
    val start = computeStartPoint(null, initialLocation = initial, searchResults = emptyList())
    assertEquals(Point.fromLngLat(20.0, 10.0), start)
  }

  @Test
  fun `computeStartPoint falls back to first search result`() {
    val searchLoc = Location.from("res", 30.0, 40.0)
    val start = computeStartPoint(null, initialLocation = null, searchResults = listOf(searchLoc))
    assertEquals(Point.fromLngLat(40.0, 30.0), start)
  }
}

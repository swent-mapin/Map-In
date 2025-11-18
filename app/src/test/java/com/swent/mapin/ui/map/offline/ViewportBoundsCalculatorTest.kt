package com.swent.mapin.ui.map.offline

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for ViewportBoundsCalculator. */
class ViewportBoundsCalculatorTest {

  @Test
  fun `calculateBounds returns valid bounds for center point`() {
    val center = Point.fromLngLat(0.0, 0.0)
    val zoom = 10.0
    val widthPx = 1080
    val heightPx = 1920

    val bounds = ViewportBoundsCalculator.calculateBounds(center, zoom, widthPx, heightPx)

    assertNotNull(bounds)
    assertNotNull(bounds.southwest)
    assertNotNull(bounds.northeast)

    // Southwest should be south and west of center
    assertTrue(bounds.southwest.latitude() < center.latitude())
    assertTrue(bounds.southwest.longitude() < center.longitude())

    // Northeast should be north and east of center
    assertTrue(bounds.northeast.latitude() > center.latitude())
    assertTrue(bounds.northeast.longitude() > center.longitude())
  }

  @Test
  fun `calculateBounds produces symmetrical bounds around center`() {
    val center = Point.fromLngLat(6.5668, 46.5191) // Lausanne
    val zoom = 12.0
    val widthPx = 1080
    val heightPx = 1920

    val bounds = ViewportBoundsCalculator.calculateBounds(center, zoom, widthPx, heightPx)

    val latDiff = center.latitude() - bounds.southwest.latitude()
    val latDiff2 = bounds.northeast.latitude() - center.latitude()

    // Latitude offsets should be equal (symmetrical)
    assertEquals(latDiff, latDiff2, 0.0001)
  }

  @Test
  fun `calculateBounds accounts for viewport aspect ratio`() {
    val center = Point.fromLngLat(0.0, 0.0)
    val zoom = 10.0

    // Square viewport
    val boundsSquare = ViewportBoundsCalculator.calculateBounds(center, zoom, 1000, 1000)
    val latRangeSquare = boundsSquare.northeast.latitude() - boundsSquare.southwest.latitude()
    val lngRangeSquare = boundsSquare.northeast.longitude() - boundsSquare.southwest.longitude()

    // Wide viewport
    val boundsWide = ViewportBoundsCalculator.calculateBounds(center, zoom, 2000, 1000)
    val latRangeWide = boundsWide.northeast.latitude() - boundsWide.southwest.latitude()
    val lngRangeWide = boundsWide.northeast.longitude() - boundsWide.southwest.longitude()

    // Wide viewport should have wider longitude range
    assertTrue(lngRangeWide > lngRangeSquare)
    // But same latitude range (height unchanged)
    assertEquals(latRangeWide, latRangeSquare, 0.0001)
  }

  @Test
  fun `calculateBounds produces smaller area at higher zoom`() {
    val center = Point.fromLngLat(0.0, 0.0)
    val widthPx = 1080
    val heightPx = 1920

    val boundsLowZoom = ViewportBoundsCalculator.calculateBounds(center, 5.0, widthPx, heightPx)
    val boundsHighZoom = ViewportBoundsCalculator.calculateBounds(center, 15.0, widthPx, heightPx)

    val areaLowZoom =
        (boundsLowZoom.northeast.latitude() - boundsLowZoom.southwest.latitude()) *
            (boundsLowZoom.northeast.longitude() - boundsLowZoom.southwest.longitude())
    val areaHighZoom =
        (boundsHighZoom.northeast.latitude() - boundsHighZoom.southwest.latitude()) *
            (boundsHighZoom.northeast.longitude() - boundsHighZoom.southwest.longitude())

    // Higher zoom = smaller visible area
    assertTrue(areaHighZoom < areaLowZoom)
  }

  @Test
  fun `calculateBounds handles equator correctly`() {
    val center = Point.fromLngLat(0.0, 0.0) // Equator
    val zoom = 10.0
    val widthPx = 1080
    val heightPx = 1920

    val bounds = ViewportBoundsCalculator.calculateBounds(center, zoom, widthPx, heightPx)

    // Should produce valid bounds at equator
    assertTrue(bounds.southwest.latitude() < 0.0)
    assertTrue(bounds.northeast.latitude() > 0.0)
    assertTrue(bounds.southwest.longitude() < 0.0)
    assertTrue(bounds.northeast.longitude() > 0.0)
  }

  @Test
  fun `calculateBounds handles high latitude correctly`() {
    val center = Point.fromLngLat(0.0, 60.0) // High latitude
    val zoom = 10.0
    val widthPx = 1080
    val heightPx = 1920

    val bounds = ViewportBoundsCalculator.calculateBounds(center, zoom, widthPx, heightPx)

    // Should produce valid bounds at high latitude
    assertNotNull(bounds)
    assertTrue(bounds.southwest.latitude() < center.latitude())
    assertTrue(bounds.northeast.latitude() > center.latitude())
  }
}

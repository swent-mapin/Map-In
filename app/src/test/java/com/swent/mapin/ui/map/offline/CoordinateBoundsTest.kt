package com.swent.mapin.ui.map.offline

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Unit tests for CoordinateBounds. */
class CoordinateBoundsTest {

  @Test
  fun `CoordinateBounds stores southwest and northeast points`() {
    val sw = Point.fromLngLat(0.0, 0.0)
    val ne = Point.fromLngLat(1.0, 1.0)

    val bounds = CoordinateBounds(southwest = sw, northeast = ne)

    assertEquals(sw, bounds.southwest)
    assertEquals(ne, bounds.northeast)
  }

  @Test
  fun `toPolygon creates valid polygon from bounds`() {
    val sw = Point.fromLngLat(0.0, 0.0)
    val ne = Point.fromLngLat(1.0, 1.0)
    val bounds = CoordinateBounds(southwest = sw, northeast = ne)

    val polygon = bounds.toPolygon()

    assertNotNull(polygon)
    assertNotNull(polygon.coordinates())
  }

  @Test
  fun `toPolygon creates closed ring with 5 points`() {
    val sw = Point.fromLngLat(0.0, 0.0)
    val ne = Point.fromLngLat(1.0, 1.0)
    val bounds = CoordinateBounds(southwest = sw, northeast = ne)

    val polygon = bounds.toPolygon()
    val ring = polygon.coordinates()[0]

    // Closed ring has 5 points (4 corners + closing point)
    assertEquals(5, ring.size)

    // First and last points should be identical (closed ring)
    assertEquals(ring[0].longitude(), ring[4].longitude(), 0.0001)
    assertEquals(ring[0].latitude(), ring[4].latitude(), 0.0001)
  }

  @Test
  fun `toPolygon creates polygon with correct corner coordinates`() {
    val sw = Point.fromLngLat(0.0, 0.0)
    val ne = Point.fromLngLat(1.0, 1.0)
    val bounds = CoordinateBounds(southwest = sw, northeast = ne)

    val polygon = bounds.toPolygon()
    val ring = polygon.coordinates()[0]

    // Verify corner points
    // SW corner
    assertEquals(0.0, ring[0].longitude(), 0.0001)
    assertEquals(0.0, ring[0].latitude(), 0.0001)

    // SE corner
    assertEquals(1.0, ring[1].longitude(), 0.0001)
    assertEquals(0.0, ring[1].latitude(), 0.0001)

    // NE corner
    assertEquals(1.0, ring[2].longitude(), 0.0001)
    assertEquals(1.0, ring[2].latitude(), 0.0001)

    // NW corner
    assertEquals(0.0, ring[3].longitude(), 0.0001)
    assertEquals(1.0, ring[3].latitude(), 0.0001)
  }

  @Test
  fun `toPolygon handles negative coordinates`() {
    val sw = Point.fromLngLat(-1.0, -1.0)
    val ne = Point.fromLngLat(1.0, 1.0)
    val bounds = CoordinateBounds(southwest = sw, northeast = ne)

    val polygon = bounds.toPolygon()
    val ring = polygon.coordinates()[0]

    assertNotNull(ring)
    assertEquals(5, ring.size)

    // Verify SW corner with negative coordinates
    assertEquals(-1.0, ring[0].longitude(), 0.0001)
    assertEquals(-1.0, ring[0].latitude(), 0.0001)
  }

  @Test
  fun `toPolygon handles bounds crossing antimeridian`() {
    val sw = Point.fromLngLat(170.0, -10.0)
    val ne = Point.fromLngLat(-170.0, 10.0)
    val bounds = CoordinateBounds(southwest = sw, northeast = ne)

    val polygon = bounds.toPolygon()

    // Should create valid polygon even with antimeridian crossing
    assertNotNull(polygon)
    assertNotNull(polygon.coordinates())
    assertEquals(5, polygon.coordinates()[0].size)
  }

  @Test
  fun `hashCode is consistent for same bounds`() {
    val sw = Point.fromLngLat(0.0, 0.0)
    val ne = Point.fromLngLat(1.0, 1.0)
    val bounds1 = CoordinateBounds(southwest = sw, northeast = ne)
    val bounds2 = CoordinateBounds(southwest = sw, northeast = ne)

    assertEquals(bounds1.hashCode(), bounds2.hashCode())
  }

  @Test
  fun `equals returns true for identical bounds`() {
    val sw = Point.fromLngLat(0.0, 0.0)
    val ne = Point.fromLngLat(1.0, 1.0)
    val bounds1 = CoordinateBounds(southwest = sw, northeast = ne)
    val bounds2 = CoordinateBounds(southwest = sw, northeast = ne)

    assertEquals(bounds1, bounds2)
  }
}

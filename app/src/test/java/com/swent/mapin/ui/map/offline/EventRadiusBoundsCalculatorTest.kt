package com.swent.mapin.ui.map.offline

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class EventRadiusBoundsCalculatorTest {

  @Test
  fun `calculateBoundsForRadius with 2km radius at EPFL coordinates`() {
    val centerLat = 46.5197 // EPFL latitude
    val centerLng = 6.5660 // EPFL longitude
    val radiusKm = 2.0

    val bounds = calculateBoundsForRadius(centerLat, centerLng, radiusKm)

    // Verify southwest is south and west of center
    assertTrue(bounds.southwest.latitude() < centerLat)
    assertTrue(bounds.southwest.longitude() < centerLng)

    // Verify northeast is north and east of center
    assertTrue(bounds.northeast.latitude() > centerLat)
    assertTrue(bounds.northeast.longitude() > centerLng)

    // Verify approximate distance (should be close to 2km in degrees)
    val latDiff = bounds.northeast.latitude() - bounds.southwest.latitude()
    val lngDiff = bounds.northeast.longitude() - bounds.southwest.longitude()

    // ~2km radius = ~4km diameter
    // At ~46° latitude, 1° lat ≈ 111km, so 4km ≈ 0.036°
    // At ~46° latitude, 1° lng ≈ 77km, so 4km ≈ 0.052°
    assertTrue("Latitude difference should be ~0.036° (got $latDiff)", abs(latDiff - 0.036) < 0.005)
    assertTrue("Longitude difference should be ~0.052° (got $lngDiff)", abs(lngDiff - 0.052) < 0.01)
  }

  @Test
  fun `calculateBoundsForRadius with different radius sizes`() {
    val centerLat = 40.7128 // New York latitude
    val centerLng = -74.0060 // New York longitude

    // Test 1km radius
    val bounds1km = calculateBoundsForRadius(centerLat, centerLng, 1.0)
    val latDiff1km = bounds1km.northeast.latitude() - bounds1km.southwest.latitude()

    // Test 5km radius
    val bounds5km = calculateBoundsForRadius(centerLat, centerLng, 5.0)
    val latDiff5km = bounds5km.northeast.latitude() - bounds5km.southwest.latitude()

    // 5km radius should have ~5x larger bounds than 1km
    assertTrue("5km bounds should be ~5x larger than 1km", abs(latDiff5km / latDiff1km - 5.0) < 0.1)
  }

  @Test
  fun `calculateBoundsForRadius at equator has equal lat and lng differences`() {
    val centerLat = 0.0 // Equator
    val centerLng = 0.0
    val radiusKm = 2.0

    val bounds = calculateBoundsForRadius(centerLat, centerLng, radiusKm)

    val latDiff = bounds.northeast.latitude() - bounds.southwest.latitude()
    val lngDiff = bounds.northeast.longitude() - bounds.southwest.longitude()

    // At equator, latitude and longitude degree distances are equal
    // Both should be ~0.036° for 2km radius (4km diameter)
    assertTrue("At equator, lat and lng diffs should be similar", abs(latDiff - lngDiff) < 0.001)
  }

  @Test
  fun `calculateBoundsForRadius clamps to valid latitude range`() {
    val centerLat = 89.0 // Near north pole
    val centerLng = 0.0
    val radiusKm = 200.0 // Large radius that would exceed 90°

    val bounds = calculateBoundsForRadius(centerLat, centerLng, radiusKm)

    // Northeast latitude should be clamped to 90°
    assertEquals(90.0, bounds.northeast.latitude(), 0.0001)

    // Southwest should still be valid
    assertTrue(bounds.southwest.latitude() >= -90.0)
    assertTrue(bounds.southwest.latitude() <= 90.0)
  }

  @Test
  fun `calculateBoundsForRadius clamps to valid longitude range`() {
    val centerLat = 0.0
    val centerLng = 179.0 // Near date line
    val radiusKm = 200.0 // Large radius

    val bounds = calculateBoundsForRadius(centerLat, centerLng, radiusKm)

    // Longitude should be clamped to [-180, 180]
    assertTrue(bounds.southwest.longitude() >= -180.0)
    assertTrue(bounds.southwest.longitude() <= 180.0)
    assertTrue(bounds.northeast.longitude() >= -180.0)
    assertTrue(bounds.northeast.longitude() <= 180.0)
  }

  @Test
  fun `calculateBoundsForRadius with zero radius returns point`() {
    val centerLat = 46.5197
    val centerLng = 6.5660
    val radiusKm = 0.0

    val bounds = calculateBoundsForRadius(centerLat, centerLng, radiusKm)

    // With zero radius, bounds should be the center point
    assertEquals(centerLat, bounds.southwest.latitude(), 0.0001)
    assertEquals(centerLng, bounds.southwest.longitude(), 0.0001)
    assertEquals(centerLat, bounds.northeast.latitude(), 0.0001)
    assertEquals(centerLng, bounds.northeast.longitude(), 0.0001)
  }

  @Test
  fun `calculateBoundsForRadius in southern hemisphere`() {
    val centerLat = -33.8688 // Sydney latitude
    val centerLng = 151.2093 // Sydney longitude
    val radiusKm = 2.0

    val bounds = calculateBoundsForRadius(centerLat, centerLng, radiusKm)

    // Verify southwest is south and west of center (more negative for south)
    assertTrue(bounds.southwest.latitude() < centerLat)
    assertTrue(bounds.southwest.longitude() < centerLng)

    // Verify northeast is north and east of center (less negative for north)
    assertTrue(bounds.northeast.latitude() > centerLat)
    assertTrue(bounds.northeast.longitude() > centerLng)

    // Verify bounds are roughly symmetric around center
    val centerToSW = centerLat - bounds.southwest.latitude()
    val centerToNE = bounds.northeast.latitude() - centerLat
    assertTrue("Bounds should be roughly symmetric", abs(centerToSW - centerToNE) < 0.0001)
  }

  @Test
  fun `calculateBoundsForRadius at high latitude has larger longitude difference`() {
    val centerLatLow = 10.0 // Low latitude
    val centerLatHigh = 70.0 // High latitude
    val centerLng = 0.0
    val radiusKm = 2.0

    val boundsLow = calculateBoundsForRadius(centerLatLow, centerLng, radiusKm)
    val boundsHigh = calculateBoundsForRadius(centerLatHigh, centerLng, radiusKm)

    val lngDiffLow = boundsLow.northeast.longitude() - boundsLow.southwest.longitude()
    val lngDiffHigh = boundsHigh.northeast.longitude() - boundsHigh.southwest.longitude()

    // At higher latitudes, longitude degrees represent shorter distances,
    // so we need more degrees to cover the same km
    assertTrue(
        "High latitude should have larger lng difference (got low=$lngDiffLow, high=$lngDiffHigh)",
        lngDiffHigh > lngDiffLow)
  }

  @Test
  fun `calculateBoundsForRadius guards against pole division by zero`() {
    val bounds = calculateBoundsForRadius(90.0, 10.0, 2.0)

    val latDiff = bounds.northeast.latitude() - bounds.southwest.latitude()
    val lngDiff = bounds.northeast.longitude() - bounds.southwest.longitude()

    assertFalse("Latitude diff should be finite", latDiff.isNaN() || latDiff.isInfinite())
    assertFalse("Longitude diff should be finite", lngDiff.isNaN() || lngDiff.isInfinite())
    assertTrue(bounds.southwest.longitude() >= -180.0 && bounds.northeast.longitude() <= 180.0)
  }
}

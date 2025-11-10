package com.swent.mapin.ui.map

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class MapConstantsTest {

  @Test
  fun collapsedHeight_hasCorrectValue() {
    assertEquals(120.dp, MapConstants.COLLAPSED_HEIGHT)
  }

  @Test
  fun mediumHeight_hasCorrectValue() {
    assertEquals(400.dp, MapConstants.MEDIUM_HEIGHT)
  }

  @Test
  fun fullHeightPercentage_hasCorrectValue() {
    assertEquals(0.98f, MapConstants.FULL_HEIGHT_PERCENTAGE)
  }

  @Test
  fun maxScrimAlpha_hasCorrectValue() {
    assertEquals(0.5f, MapConstants.MAX_SCRIM_ALPHA)
  }

  @Test
  fun zoomChangeThreshold_hasCorrectValue() {
    assertEquals(0.5f, MapConstants.ZOOM_CHANGE_THRESHOLD)
  }

  @Test
  fun sheetProximityThresholdDp_hasCorrectValue() {
    assertEquals(5f, MapConstants.SHEET_PROXIMITY_THRESHOLD_DP)
  }

  @Test
  fun overscrollAllowanceDp_hasCorrectValue() {
    assertEquals(60f, MapConstants.OVERSCROLL_ALLOWANCE_DP)
  }

  @Test
  fun defaultLatitude_hasCorrectValue() {
    assertEquals(46.5197, MapConstants.DEFAULT_LATITUDE, 0.0001)
  }

  @Test
  fun defaultLongitude_hasCorrectValue() {
    assertEquals(6.5668, MapConstants.DEFAULT_LONGITUDE, 0.0001)
  }

  @Test
  fun defaultZoom_hasCorrectValue() {
    assertEquals(15f, MapConstants.DEFAULT_ZOOM)
  }

  @Test
  fun locationCenterMinPadding_hasCorrectValue() {
    assertEquals(8f, MapConstants.LOCATION_CENTER_MIN_PADDING_DP)
  }

  @Test
  fun locationCenterMediumWeight_hasCorrectValue() {
    assertEquals(0.85f, MapConstants.LOCATION_CENTER_MEDIUM_WEIGHT)
  }

  @Test
  fun locationCenterMediumExtra_hasCorrectValue() {
    assertEquals(16f, MapConstants.LOCATION_CENTER_MEDIUM_EXTRA_DP)
  }

  @Test
  fun defaultCoordinates_areAtEPFL() {
    val epflLat = 46.5197
    val epflLon = 6.5668

    assertEquals(epflLat, MapConstants.DEFAULT_LATITUDE, 0.0001)
    assertEquals(epflLon, MapConstants.DEFAULT_LONGITUDE, 0.0001)
  }

  @Test
  fun collapsedHeight_isLessThanMediumHeight() {
    assert(MapConstants.COLLAPSED_HEIGHT < MapConstants.MEDIUM_HEIGHT)
  }

  @Test
  fun maxScrimAlpha_isValidOpacity() {
    assert(MapConstants.MAX_SCRIM_ALPHA >= 0f)
    assert(MapConstants.MAX_SCRIM_ALPHA <= 1f)
  }

  @Test
  fun fullHeightPercentage_isValidPercentage() {
    assert(MapConstants.FULL_HEIGHT_PERCENTAGE > 0f)
    assert(MapConstants.FULL_HEIGHT_PERCENTAGE <= 1f)
  }

  @Test
  fun zoomChangeThreshold_isPositive() {
    assert(MapConstants.ZOOM_CHANGE_THRESHOLD > 0f)
  }

  @Test
  fun defaultZoom_isReasonableForCityView() {
    assert(MapConstants.DEFAULT_ZOOM >= 10f)
    assert(MapConstants.DEFAULT_ZOOM <= 20f)
  }

  @Test
  fun heatmapConfig_hasExpectedValues() {
    assertEquals(18L, MapConstants.HeatmapConfig.MAX_ZOOM_LEVEL)
    assertEquals(0.65, MapConstants.HeatmapConfig.OPACITY, 0.0001)
    assertEquals(
        listOf(0.0 to 18.0, 14.0 to 32.0, 22.0 to 48.0), MapConstants.HeatmapConfig.RADIUS_STOPS)
    assertEquals(
        listOf(0.0 to 0.0, 5.0 to 0.4, 25.0 to 0.8, 100.0 to 1.0),
        MapConstants.HeatmapConfig.WEIGHT_STOPS)
  }

  @Test
  fun cameraConfig_hasExpectedValues() {
    assertEquals(0.01f, MapConstants.CameraConfig.ZOOM_DELTA_THRESHOLD)
    assertEquals(300L, MapConstants.CameraConfig.SCALE_BAR_HIDE_DELAY_MS)
    assertEquals(1100L, MapConstants.CameraConfig.PROGRAMMATIC_ZOOM_RESET_DELAY_MS)
  }
}

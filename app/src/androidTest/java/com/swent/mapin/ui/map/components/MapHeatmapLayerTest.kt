package com.swent.mapin.ui.map.components

import androidx.compose.ui.test.junit4.createComposeRule
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import org.junit.Rule
import org.junit.Test

@OptIn(MapboxExperimental::class)
class MapHeatmapLayerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun createHeatmapLayer_rendersWithoutCrashing() {
    composeTestRule.setContent {
      val geoJsonSourceState = rememberGeoJsonSourceState()
      MapboxMap(
          mapViewportState = rememberMapViewportState(),
          style = { MapStyle(style = "mapbox://styles/mapbox/light-v11") }) {
            CreateHeatmapLayer(heatmapSource = geoJsonSourceState)
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun createHeatmapLayer_rendersWithEmptyFeatureCollection() {
    composeTestRule.setContent {
      val geoJsonSourceState = rememberGeoJsonSourceState()
      val emptyGeoJson = FeatureCollection.fromFeatures(emptyList()).toJson()
      geoJsonSourceState.data = GeoJSONData(emptyGeoJson)
      MapboxMap(
          mapViewportState = rememberMapViewportState(),
          style = { MapStyle(style = "mapbox://styles/mapbox/light-v11") }) {
            CreateHeatmapLayer(heatmapSource = geoJsonSourceState)
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun createHeatmapLayer_rendersWithSingleFeature() {
    composeTestRule.setContent {
      val geoJsonSourceState = rememberGeoJsonSourceState()
      val feature =
          Feature.fromGeometry(
              Point.fromLngLat(6.5668, 46.5197), JsonObject().apply { addProperty("weight", 10.0) })
      val geoJson = FeatureCollection.fromFeatures(listOf(feature)).toJson()
      geoJsonSourceState.data = GeoJSONData(geoJson)

      MapboxMap(
          mapViewportState = rememberMapViewportState(),
          style = { MapStyle(style = "mapbox://styles/mapbox/light-v11") }) {
            CreateHeatmapLayer(heatmapSource = geoJsonSourceState)
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun createHeatmapLayer_rendersWithMultipleFeatures() {
    composeTestRule.setContent {
      val geoJsonSourceState = rememberGeoJsonSourceState()
      val features =
          listOf(
              Feature.fromGeometry(
                  Point.fromLngLat(6.5668, 46.5197),
                  JsonObject().apply { addProperty("weight", 5.0) }),
              Feature.fromGeometry(
                  Point.fromLngLat(6.5700, 46.5200),
                  JsonObject().apply { addProperty("weight", 15.0) }),
              Feature.fromGeometry(
                  Point.fromLngLat(6.5650, 46.5190),
                  JsonObject().apply { addProperty("weight", 25.0) }))
      val geoJson = FeatureCollection.fromFeatures(features).toJson()
      geoJsonSourceState.data = GeoJSONData(geoJson)

      MapboxMap(
          mapViewportState = rememberMapViewportState(),
          style = { MapStyle(style = "mapbox://styles/mapbox/light-v11") }) {
            CreateHeatmapLayer(heatmapSource = geoJsonSourceState)
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun createHeatmapLayer_handlesWeightBoundaryValues() {
    composeTestRule.setContent {
      val geoJsonSourceState = rememberGeoJsonSourceState()
      // Test weight interpolation stops: 0.0, 5.0, 25.0, 100.0
      val features =
          listOf(
              Feature.fromGeometry(
                  Point.fromLngLat(6.5668, 46.5197),
                  JsonObject().apply { addProperty("weight", 0.0) }),
              Feature.fromGeometry(
                  Point.fromLngLat(6.5670, 46.5198),
                  JsonObject().apply { addProperty("weight", 5.0) }),
              Feature.fromGeometry(
                  Point.fromLngLat(6.5672, 46.5199),
                  JsonObject().apply { addProperty("weight", 25.0) }),
              Feature.fromGeometry(
                  Point.fromLngLat(6.5674, 46.5200),
                  JsonObject().apply { addProperty("weight", 100.0) }))
      val geoJson = FeatureCollection.fromFeatures(features).toJson()
      geoJsonSourceState.data = GeoJSONData(geoJson)

      MapboxMap(
          mapViewportState = rememberMapViewportState(),
          style = { MapStyle(style = "mapbox://styles/mapbox/light-v11") }) {
            CreateHeatmapLayer(heatmapSource = geoJsonSourceState)
          }
    }

    composeTestRule.waitForIdle()
  }
}

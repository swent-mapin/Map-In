package com.swent.mapin.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.swent.mapin.model.event.Event

/**
 * Displays semi-transparent circles on the map showing cached offline regions.
 *
 * Each circle represents a 2km radius around a saved or joined event location, helping users
 * visualize which map areas are available offline.
 *
 * @param events List of events (saved + joined) that have offline regions downloaded
 * @param visible Whether to show the cached regions overlay
 * @param radiusKm The radius of each cached region in kilometers (default: 2.0)
 */
@Composable
fun CachedRegionsOverlay(events: List<Event>, visible: Boolean, radiusKm: Double = 2.0) {
  if (!visible || events.isEmpty()) return

  // Convert events to point features
  val features =
      remember(events) {
        events.map { event ->
          Feature.fromGeometry(Point.fromLngLat(event.location.longitude, event.location.latitude))
        }
      }

  val cachedRegionsSource =
      rememberGeoJsonSourceState(key = "cached-regions-source") { data = GeoJSONData(features) }

  val greenColor = Color(0xFF4CAF50)

  // Draw small visual indicators at event locations to show cached areas
  // Note: Draws a fixed-size circle marker (20px), not the actual 2km geographic boundary
  CircleLayer(sourceState = cachedRegionsSource, layerId = "cached-regions-layer") {
    circleRadius = DoubleValue(20.0) // 20 pixels - visible but not overwhelming
    circleColor = ColorValue(greenColor)
    circleOpacity = DoubleValue(0.2) // Subtle fill
    circleStrokeColor = ColorValue(greenColor)
    circleStrokeWidth = DoubleValue(2.0)
    circleStrokeOpacity = DoubleValue(0.6) // More visible stroke
  }
}

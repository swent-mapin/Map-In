package com.swent.mapin.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.compose.style.layers.generated.LineLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState

/**
 * Composable that renders a direction line on the map between two points.
 *
 * Displays a Google Maps-styled route with:
 * - A main blue line (#4285F4 - Google Maps signature color)
 * - A shadow layer for depth effect
 * - White circular markers at start and end points with blue borders
 *
 * The visual style is designed to match Google Maps' direction lines while maintaining consistency
 * with modern map UI patterns.
 *
 * @param routePoints Ordered list of geographic points forming the route path. Must contain at
 *   least one point. First point is the origin, last point is the destination.
 * @note This uses Mapbox's MapboxDelicateApi for low-level layer management. Colors may be affected
 *   by map theme (dark/light mode) due to Mapbox Standard Style's global transformations.
 */
@OptIn(MapboxDelicateApi::class)
@Composable
fun DirectionOverlay(routePoints: List<Point>) {
  if (routePoints.isEmpty()) return

  val lineString = remember(routePoints) { LineString.fromLngLats(routePoints) }

  val directionSource =
      rememberGeoJsonSourceState(key = "direction-line-source") { data = GeoJSONData(lineString) }

  val endpointsSource =
      rememberGeoJsonSourceState(key = "direction-endpoints-source") {
        data =
            GeoJSONData(
                listOf(
                    com.mapbox.geojson.Feature.fromGeometry(routePoints.first()),
                    com.mapbox.geojson.Feature.fromGeometry(routePoints.last())))
      }

  val shadowBlue = Color(0xFF1A73E8)
  val mainBlue = Color(0xFF4285F4)
  val white = Color(0xFFFFFFFF)

  LineLayer(sourceState = directionSource, layerId = "direction-line-shadow") {
    lineColor = ColorValue(shadowBlue)
    lineWidth = DoubleValue(10.0)
    lineOpacity = DoubleValue(0.3)
    lineBlur = DoubleValue(5.0)
  }

  LineLayer(sourceState = directionSource, layerId = "direction-line-layer") {
    lineColor = ColorValue(mainBlue)
    lineWidth = DoubleValue(5.0)
    lineOpacity = DoubleValue(0.95)
  }

  CircleLayer(sourceState = endpointsSource, layerId = "direction-endpoints-shadow") {
    circleRadius = DoubleValue(8.0)
    circleColor = ColorValue(shadowBlue)
    circleOpacity = DoubleValue(0.3)
    circleBlur = DoubleValue(3.0)
  }

  CircleLayer(sourceState = endpointsSource, layerId = "direction-endpoints") {
    circleRadius = DoubleValue(5.0)
    circleColor = ColorValue(white)
    circleStrokeWidth = DoubleValue(2.0)
    circleStrokeColor = ColorValue(mainBlue)
    circleOpacity = DoubleValue(1.0)
  }
}

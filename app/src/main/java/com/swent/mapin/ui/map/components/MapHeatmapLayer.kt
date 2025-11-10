package com.swent.mapin.ui.map.components

import androidx.compose.runtime.Composable
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.LongValue
import com.mapbox.maps.extension.compose.style.layers.generated.HeatmapLayer
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.swent.mapin.ui.map.MapConstants

/**
 * Renders a heatmap layer showing location density.
 *
 * Uses interpolated colors, radius, and weight based on zoom level and location data.
 *
 * @param heatmapSource GeoJSON source containing location data
 */
@Composable
fun CreateHeatmapLayer(heatmapSource: GeoJsonSourceState) {
  HeatmapLayer(sourceState = heatmapSource, layerId = "locations-heatmap") {
    maxZoom = LongValue(MapConstants.HeatmapConfig.MAX_ZOOM_LEVEL)
    heatmapOpacity = DoubleValue(MapConstants.HeatmapConfig.OPACITY)
    heatmapRadius =
        DoubleValue(
            interpolate {
              linear()
              zoom()
              MapConstants.HeatmapConfig.RADIUS_STOPS.forEach { (zoom, radius) ->
                stop {
                  literal(zoom)
                  literal(radius)
                }
              }
            })
    heatmapWeight =
        DoubleValue(
            interpolate {
              linear()
              get { literal("weight") }
              MapConstants.HeatmapConfig.WEIGHT_STOPS.forEach { (weight, output) ->
                stop {
                  literal(weight)
                  literal(output)
                }
              }
            })
    heatmapColor =
        ColorValue(
            interpolate {
              linear()
              heatmapDensity()
              MapConstants.HeatmapColors.COLOR_STOPS.forEach { (position, color) ->
                stop {
                  literal(position)
                  if (color.a == 0.0) {
                    rgba(color.r, color.g, color.b, color.a)
                  } else {
                    rgb(color.r, color.g, color.b)
                  }
                }
              }
            })
  }
}

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
    maxZoom = LongValue(18L)
    heatmapOpacity = DoubleValue(0.65)
    heatmapRadius =
        DoubleValue(
            interpolate {
              linear()
              zoom()
              stop {
                literal(0.0)
                literal(18.0)
              }
              stop {
                literal(14.0)
                literal(32.0)
              }
              stop {
                literal(22.0)
                literal(48.0)
              }
            })
    heatmapWeight =
        DoubleValue(
            interpolate {
              linear()
              get { literal("weight") }
              stop {
                literal(0.0)
                literal(0.0)
              }
              stop {
                literal(5.0)
                literal(0.4)
              }
              stop {
                literal(25.0)
                literal(0.8)
              }
              stop {
                literal(100.0)
                literal(1.0)
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

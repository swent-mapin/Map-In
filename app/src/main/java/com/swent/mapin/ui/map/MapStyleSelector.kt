package com.swent.mapin.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Compact collapsible segmented button control for selecting map style.
 *
 * Shows a FAB when collapsed, expands to show all three options with spring animation.
 * - Standard: Default map view with Map icon
 * - Satellite: Satellite imagery with Satellite icon
 * - Heatmap: Heatmap overlay with Fire/Heat icon
 *
 * @param selectedStyle Currently selected map style
 * @param onStyleSelected Callback when a style is selected
 * @param modifier Modifier for customization
 */
@Composable
fun MapStyleSelector(
    selectedStyle: MapScreenViewModel.MapStyle,
    onStyleSelected: (MapScreenViewModel.MapStyle) -> Unit,
    modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  Row(
      modifier = modifier.testTag("mapStyleSelector").verticalScroll(rememberScrollState()),
      verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = isExpanded,
            enter =
                expandHorizontally(
                    animationSpec = tween(durationMillis = 150), expandFrom = Alignment.End) +
                    fadeIn(animationSpec = tween(durationMillis = 100)),
            exit =
                shrinkHorizontally(
                    animationSpec = tween(durationMillis = 120), shrinkTowards = Alignment.End) +
                    fadeOut(animationSpec = tween(durationMillis = 100))) {
              SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = selectedStyle == MapScreenViewModel.MapStyle.STANDARD,
                    onClick = {
                      onStyleSelected(MapScreenViewModel.MapStyle.STANDARD)
                      isExpanded = false
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = {
                      SegmentedButtonDefaults.Icon(
                          active = selectedStyle == MapScreenViewModel.MapStyle.STANDARD) {
                            Icon(imageVector = Icons.Filled.Map, contentDescription = "Map view")
                          }
                    },
                    modifier = Modifier.testTag("mapStyleOption_STANDARD")) {
                      Text("Map")
                    }

                SegmentedButton(
                    selected = selectedStyle == MapScreenViewModel.MapStyle.SATELLITE,
                    onClick = {
                      onStyleSelected(MapScreenViewModel.MapStyle.SATELLITE)
                      isExpanded = false
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = {
                      SegmentedButtonDefaults.Icon(
                          active = selectedStyle == MapScreenViewModel.MapStyle.SATELLITE) {
                            Icon(
                                imageVector = Icons.Filled.Satellite,
                                contentDescription = "Satellite view")
                          }
                    },
                    modifier = Modifier.testTag("mapStyleOption_SATELLITE")) {
                      Text("Satellite")
                    }

                SegmentedButton(
                    selected = selectedStyle == MapScreenViewModel.MapStyle.HEATMAP,
                    onClick = {
                      onStyleSelected(MapScreenViewModel.MapStyle.HEATMAP)
                      isExpanded = false
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = {
                      SegmentedButtonDefaults.Icon(
                          active = selectedStyle == MapScreenViewModel.MapStyle.HEATMAP) {
                            Icon(
                                imageVector = Icons.Filled.Whatshot,
                                contentDescription = "Heatmap view")
                          }
                    },
                    modifier = Modifier.testTag("mapStyleOption_HEATMAP")) {
                      Text("Heat")
                    }
              }
            }

        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("mapStyleToggle")) {
              if (isExpanded) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close map style selector")
              } else {
                Icon(
                    imageVector =
                        when (selectedStyle) {
                          MapScreenViewModel.MapStyle.STANDARD -> Icons.Filled.Map
                          MapScreenViewModel.MapStyle.SATELLITE -> Icons.Filled.Satellite
                          MapScreenViewModel.MapStyle.HEATMAP -> Icons.Filled.Whatshot
                        },
                    contentDescription = "Map style: ${selectedStyle.name}")
              }
            }
      }
}

package com.swent.mapin.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Minimal map style selector composed of a compact icon button with a dropdown menu.
 *
 * Keeps the selector aligned with other map controls while retaining the full set of style choices.
 */
@Composable
fun MapStyleSelector(
    selectedStyle: MapScreenViewModel.MapStyle,
    onStyleSelected: (MapScreenViewModel.MapStyle) -> Unit,
    modifier: Modifier = Modifier.size(48.dp)
) {
  var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

  val currentStyleIcon =
      when (selectedStyle) {
        MapScreenViewModel.MapStyle.STANDARD -> Icons.Filled.Map
        MapScreenViewModel.MapStyle.SATELLITE -> Icons.Filled.Satellite
        MapScreenViewModel.MapStyle.HEATMAP -> Icons.Filled.Whatshot
      }

  Box(modifier = modifier.defaultMinSize(48.dp, 48.dp).testTag("mapStyleSelector")) {
    FilledIconButton(
        onClick = { isMenuExpanded = !isMenuExpanded },
        modifier = Modifier.fillMaxSize().testTag("mapStyleToggle"),
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface)) {
          Icon(imageVector = currentStyleIcon, contentDescription = "Change map style")
        }

    val onStyleChosen: (MapScreenViewModel.MapStyle) -> Unit = { style ->
      onStyleSelected(style)
      isMenuExpanded = false
    }

    DropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = { isMenuExpanded = false },
        modifier = Modifier.testTag("mapStyleMenu")) {
          MapStyleMenuItem(
              label = "Map",
              icon = Icons.Filled.Map,
              style = MapScreenViewModel.MapStyle.STANDARD,
              isSelected = selectedStyle == MapScreenViewModel.MapStyle.STANDARD,
              onClick = onStyleChosen)

          MapStyleMenuItem(
              label = "Satellite",
              icon = Icons.Filled.Satellite,
              style = MapScreenViewModel.MapStyle.SATELLITE,
              isSelected = selectedStyle == MapScreenViewModel.MapStyle.SATELLITE,
              onClick = onStyleChosen)

          MapStyleMenuItem(
              label = "Heatmap",
              icon = Icons.Filled.Whatshot,
              style = MapScreenViewModel.MapStyle.HEATMAP,
              isSelected = selectedStyle == MapScreenViewModel.MapStyle.HEATMAP,
              onClick = onStyleChosen)
        }
  }
}

@Composable
private fun MapStyleMenuItem(
    label: String,
    icon: ImageVector,
    style: MapScreenViewModel.MapStyle,
    isSelected: Boolean,
    onClick: (MapScreenViewModel.MapStyle) -> Unit
) {
  DropdownMenuItem(
      text = {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal))
      },
      leadingIcon = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
                if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                })
      },
      trailingIcon = {
        if (isSelected) {
          Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary)
        }
      },
      onClick = { onClick(style) },
      modifier = Modifier.testTag("mapStyleOption_${style.name}"))
}

package com.swent.mapin.ui.map

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swent.mapin.R

/**
 * Toggle button for showing/hiding cached offline regions on the map.
 *
 * Displays a layers icon that changes color based on whether cached regions are currently visible.
 *
 * @param showCachedRegions Whether cached regions are currently visible
 * @param onClick Callback when the toggle button is clicked
 * @param modifier Modifier for styling and layout
 */
@Composable
fun CachedRegionsToggle(
    showCachedRegions: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  FilledIconButton(
      onClick = onClick,
      modifier = modifier.testTag("cachedRegionsToggle").size(48.dp),
      colors =
          if (showCachedRegions) {
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
          } else {
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface)
          }) {
        Icon(
            imageVector = Icons.Default.Layers,
            contentDescription =
                if (showCachedRegions) {
                  stringResource(R.string.hide_cached_regions)
                } else {
                  stringResource(R.string.show_cached_regions)
                })
      }
}

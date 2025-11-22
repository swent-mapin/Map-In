package com.swent.mapin.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swent.mapin.R

/**
 * Displays a small offline indicator when the device is offline.
 *
 * Shows a non-intrusive banner with cloud-off icon and status text in the top-right corner. The
 * indicator automatically appears when offline and disappears when connectivity is restored.
 *
 * Shows different states:
 * - Offline in cached region: Green "Offline - Cached"
 * - Offline not in cached region: Red/Orange "Offline"
 *
 * @param isOffline Whether the device is currently offline
 * @param isInCachedRegion Whether viewport center is within a cached offline region
 * @param modifier Modifier for positioning and styling
 */
@Composable
fun OfflineIndicator(isOffline: Boolean, isInCachedRegion: Boolean, modifier: Modifier = Modifier) {
  AnimatedVisibility(visible = isOffline, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val containerColor =
        if (isInCachedRegion) {
          // Green for cached, theme-aware
          Color(0xFF4CAF50).copy(alpha = if (isDarkTheme) 0.8f else 1f)
        } else {
          MaterialTheme.colorScheme.errorContainer // Red/Orange for not cached
        }

    val contentColor =
        if (isInCachedRegion) {
          Color.White.copy(alpha = if (isDarkTheme) 0.9f else 1f)
        } else {
          MaterialTheme.colorScheme.onErrorContainer
        }

    val text =
        if (isInCachedRegion) {
          stringResource(R.string.offline_mode) + " - " + stringResource(R.string.cached_region)
        } else {
          stringResource(R.string.offline_mode)
        }

    Row(
        modifier =
            Modifier.testTag("offlineIndicator")
                .background(color = containerColor, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.CloudOff,
              contentDescription = text,
              tint = contentColor,
              modifier = Modifier.padding(end = 4.dp))
          Text(text = text, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
  }
}

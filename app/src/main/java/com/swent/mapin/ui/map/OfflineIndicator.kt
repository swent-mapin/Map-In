package com.swent.mapin.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swent.mapin.R

/**
 * Displays a small offline indicator when the device is offline.
 *
 * Shows a non-intrusive banner with cloud-off icon and "Offline" text in the top-right corner. The
 * indicator automatically appears when offline and disappears when connectivity is restored.
 *
 * @param isOffline Whether the device is currently offline
 * @param modifier Modifier for positioning and styling
 */
@Composable
fun OfflineIndicator(isOffline: Boolean, modifier: Modifier = Modifier) {
  AnimatedVisibility(visible = isOffline, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
    Row(
        modifier =
            Modifier.testTag("offlineIndicator")
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.CloudOff,
              contentDescription = stringResource(R.string.offline_mode),
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(end = 4.dp))
          Text(
              text = stringResource(R.string.offline_mode),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onErrorContainer)
        }
  }
}

package com.swent.mapin.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swent.mapin.R
import com.swent.mapin.ui.map.offline.DownloadProgress
import kotlinx.coroutines.delay

/**
 * Displays download status for offline map regions.
 *
 * Shows a badge indicating the current download status:
 * - "Downloading offline maps" while downloading
 * - "Offline maps ready" when complete (auto-hides after 3 seconds)
 *
 * @param downloadProgress The current download progress state
 * @param modifier Modifier for styling
 */
@Composable
fun DownloadProgressIndicator(downloadProgress: DownloadProgress, modifier: Modifier = Modifier) {
  var isVisible by remember { mutableStateOf(false) }
  var isComplete by remember { mutableStateOf(false) }

  // Update visibility based on download progress
  LaunchedEffect(downloadProgress) {
    when (downloadProgress) {
      is DownloadProgress.Idle -> {
        isVisible = false
        isComplete = false
      }
      is DownloadProgress.Downloading -> {
        isVisible = true
        isComplete = false
      }
      is DownloadProgress.Complete -> {
        isVisible = true
        isComplete = true
        // Auto-hide after 3 seconds
        delay(3000)
        isVisible = false
      }
    }
  }

  AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
    val containerColor =
        if (isComplete) {
          Color(0xFF4CAF50) // Green for complete
        } else {
          MaterialTheme.colorScheme.primaryContainer
        }

    val contentColor =
        if (isComplete) {
          Color.White
        } else {
          MaterialTheme.colorScheme.onPrimaryContainer
        }

    val icon =
        if (isComplete) {
          Icons.Default.CheckCircle
        } else {
          Icons.Default.Download
        }

    val text =
        if (isComplete) {
          stringResource(R.string.offline_maps_ready)
        } else {
          stringResource(R.string.downloading_offline_maps)
        }

    Row(
        modifier =
            Modifier.testTag("downloadProgressIndicator")
                .background(color = containerColor, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = icon,
              contentDescription = text,
              tint = contentColor,
              modifier = Modifier.padding(end = 4.dp).testTag("downloadProgressIcon"))
          Text(
              text = text,
              style = MaterialTheme.typography.labelMedium,
              color = contentColor,
              modifier = Modifier.testTag("downloadProgressText"))
        }
  }
}

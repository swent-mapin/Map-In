package com.swent.mapin.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.event.Event

/**
 * Displays download progress and completion status for offline map regions.
 *
 * Shows two states:
 * - Downloading: Event name with progress bar
 * - Complete: Generic "Downloads complete" message with checkmark (auto-dismisses after 3s)
 *
 * @param downloadingEvent The event currently being downloaded (null if none)
 * @param downloadProgress Progress value from 0.0 to 1.0
 * @param showDownloadComplete Whether to show the download completion message
 * @param modifier Modifier for positioning
 */
@Composable
fun DownloadIndicator(
    downloadingEvent: Event?,
    downloadProgress: Float,
    showDownloadComplete: Boolean,
    modifier: Modifier = Modifier
) {
  // Show download progress
  AnimatedVisibility(
      visible = downloadingEvent != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        downloadingEvent?.let { event ->
          Column(
              modifier =
                  Modifier.testTag("downloadIndicatorProgress")
                      .background(
                          color = MaterialTheme.colorScheme.primaryContainer,
                          shape = RoundedCornerShape(8.dp))
                      .padding(12.dp)
                      .width(200.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector = Icons.Default.Download,
                      contentDescription = "Downloading",
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(end = 8.dp))
                  Text(
                      text = "Downloading ${event.title}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      maxLines = 1)
                }
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
              }
        }
      }

  // Show completion message
  AnimatedVisibility(
      visible = showDownloadComplete && downloadingEvent == null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = modifier) {
        Row(
            modifier =
                Modifier.testTag("downloadIndicatorComplete")
                    .background(
                        color = Color(0xFF4CAF50), // Green for success
                        shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Downloads complete",
                  tint = Color.White,
                  modifier = Modifier.padding(end = 8.dp))
              Text(
                  text = "Downloads complete",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                  maxLines = 1)
            }
      }
}

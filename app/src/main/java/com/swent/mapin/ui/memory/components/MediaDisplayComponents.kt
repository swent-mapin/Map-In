package com.swent.mapin.ui.memory.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

const val MAX_MEDIA_COUNT = 5

private val MEDIA_THUMBNAIL_SIZE = 100.dp

/**
 * Checks if a given URI is a video file.
 *
 * @param uri The URI to check
 */
fun isVideoUri(context: Context, uri: Uri): Boolean {
  val type = context.contentResolver.getType(uri)
  return type?.startsWith("video/") == true
}

@Composable
fun MediaThumbnail(
    uri: Uri,
    onRemove: () -> Unit,
) {
  val context = LocalContext.current
  val video = remember(uri) { isVideoUri(context, uri) }
  Log.d("MediaThumbnail", "uri: $uri, video: $video")

  Box(modifier = Modifier.size(MEDIA_THUMBNAIL_SIZE)) {
    AsyncImage(
        model = uri,
        contentDescription = "Selected media",
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)))
    // Video overlay
    if (video) {
      Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Video",
          modifier =
              Modifier.align(Alignment.Center)
                  .size(32.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
                  .padding(6.dp),
          tint = MaterialTheme.colorScheme.onSurface)
    }
    // Remove button
    IconButton(
        onClick = onRemove,
        modifier =
            Modifier.align(Alignment.TopEnd)
                .size(24.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)) {
          Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remove",
              modifier = Modifier.size(16.dp))
        }
  }
}

/**
 * Empty media box for memory form
 *
 * @param onLaunchMediaPicker Callback to launch media picker
 */
@Composable
fun emptyMediaBox(onLaunchMediaPicker: () -> Unit) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(120.dp)
              .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(8.dp))
              .clickable { onLaunchMediaPicker() }
              .padding(16.dp)
              .testTag("addMediaButton"),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add media",
              modifier = Modifier.size(32.dp),
              tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = "Tap to add photos or videos",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
}

/**
 * Add media box for memory form
 *
 * @param onLaunchMediaPicker Callback to launch media picker
 */
@Composable
fun addMediaBox(onLaunchMediaPicker: () -> Unit) {
  Box(
      modifier =
          Modifier.size(MEDIA_THUMBNAIL_SIZE)
              .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(8.dp))
              .clickable { onLaunchMediaPicker() },
      contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add more",
            tint = MaterialTheme.colorScheme.primary)
      }
}

/**
 * Media selection section for memory form
 *
 * @param selectedMediaUris List of selected media URIs
 * @param onLaunchMediaPicker Callback to launch media picker
 * @param onRemoveMedia Callback when media is removed
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaSelectionSection(
    selectedMediaUris: List<Uri>,
    onLaunchMediaPicker: () -> Unit,
    onRemoveMedia: (Uri) -> Unit,
    maxMediaCount: Int = MAX_MEDIA_COUNT,
    modifier: Modifier = Modifier
) {
  Text(
      text = "Photos or videos (up to $maxMediaCount)",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier.padding(bottom = 8.dp))

  if (selectedMediaUris.isEmpty()) {
    emptyMediaBox(onLaunchMediaPicker)
  } else {
    Column(modifier = Modifier.fillMaxWidth()) {
      FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedMediaUris.forEach { uri ->
              MediaThumbnail(uri = uri, onRemove = { onRemoveMedia(uri) })
            }
            if (selectedMediaUris.size < maxMediaCount) {
              Log.d("MediaSelectionSection", "size ${selectedMediaUris.size} < $maxMediaCount")
              addMediaBox(onLaunchMediaPicker)
            }
          }
    }
  }
}

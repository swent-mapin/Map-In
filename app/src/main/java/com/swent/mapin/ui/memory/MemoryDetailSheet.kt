package com.swent.mapin.ui.memory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.ui.map.BottomSheetState
import java.text.SimpleDateFormat
import java.util.*

// Assisted by AI

/**
 * Bottom sheet showing memory details (photos, tags, description, etc.)
 *
 * @param memory Memory to display
 * @param sheetState Current bottom sheet state
 * @param ownerName Human-readable name of the owner
 * @param taggedUserNames List of names for tagged users
 * @param onClose Called when close icon is tapped
 * @param onOpenLinkedEvent Navigate to the event if memory is linked to one
 */
@Composable
fun MemoryDetailSheet(
    memory: Memory,
    sheetState: BottomSheetState,
    ownerName: String,
    taggedUserNames: List<String>,
    onClose: () -> Unit,
    onOpenLinkedEvent: () -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxWidth().testTag("memoryDetailSheet")) {
    when (sheetState) {
      BottomSheetState.COLLAPSED -> CollapsedMemoryContent(memory = memory, onClose = onClose)
      BottomSheetState.MEDIUM -> {
        MemoryDetailHeader(onClose = onClose)
        MediumMemoryContent(
            memory = memory, ownerName = ownerName, taggedUserNames = taggedUserNames)
      }
      BottomSheetState.FULL -> {
        MemoryDetailHeader(onClose = onClose)
        FullMemoryContent(memory = memory, ownerName = ownerName, taggedUserNames = taggedUserNames)
      }
    }
  }
}

/* ------------------------ HEADER ------------------------ */

@Composable
private fun MemoryDetailHeader(onClose: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose, modifier = Modifier.testTag("closeMemoryButton")) {
          Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurface)
        }
      }
}

/* ------------------------ COLLAPSED ------------------------ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollapsedMemoryContent(memory: Memory, onClose: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = memory.title.ifBlank { "Memory" },
                  maxLines = 1,
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.basicMarquee().testTag("memoryTitleCollapsed"))

              Text(
                  text = "Memory",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.testTag("collapsedMemoryLabel"))
            }

        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
      }
}

/* ------------------------ MEDIUM ------------------------ */

@Composable
private fun MediumMemoryContent(memory: Memory, ownerName: String, taggedUserNames: List<String>) {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    Text(
        text = memory.title.ifBlank { "Memory" },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("memoryTitleMedium"))

    Spacer(Modifier.height(4.dp))

    MemoryMetadata(memory, ownerName)

    Spacer(Modifier.height(8.dp))

    if (memory.mediaUrls.isNotEmpty()) {
      MemoryMediaPreview(memory.mediaUrls)
      Spacer(Modifier.height(12.dp))
    }

    if (memory.description.isNotBlank()) {
      Text(
          memory.description,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.testTag("memoryDescriptionPreview"))
    }

    if (taggedUserNames.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      TaggedUsersSection(taggedUserNames)
    }
  }
}

/* ------------------------ FULL ------------------------ */

@Composable
private fun FullMemoryContent(memory: Memory, ownerName: String, taggedUserNames: List<String>) {
  val scrollState = rememberScrollState()

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .verticalScroll(scrollState)
              .padding(horizontal = 16.dp)
              .padding(bottom = 16.dp)) {
        // Media gallery
        MemoryMediaGallery(memory.mediaUrls)

        Spacer(Modifier.height(16.dp))

        Text(
            text = memory.title.ifBlank { "Memory" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("memoryTitleFull"))

        Spacer(Modifier.height(8.dp))

        MemoryMetadata(memory, ownerName)

        Spacer(Modifier.height(16.dp))

        if (memory.description.isNotBlank()) {
          Text(
              "Description",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(8.dp))
          Text(
              memory.description,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.testTag("memoryDescriptionFull"))
        }

        if (taggedUserNames.isNotEmpty()) {
          Spacer(Modifier.height(16.dp))
          TaggedUsersSection(taggedUserNames)
        }
      }
}

/* ------------------------ SUBCOMPONENTS ------------------------ */

@Composable
private fun MemoryMetadata(memory: Memory, ownerName: String) {
  val dateText = memory.createdAt?.let { formatMemoryDate(it) } ?: "Unknown date"

  Column {
    Row {
      Text("By ", style = MaterialTheme.typography.bodySmall)
      Text(
          ownerName,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary)
      Text(" • $dateText", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(4.dp))

    if (memory.public) {
      Text(
          "Public memory",
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodySmall)
    } else {
      Text(
          "Private memory",
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun TaggedUsersSection(names: List<String>) {
  Column {
    Text("Tagged people", fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(
        names.joinToString(", "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("taggedUsersText"))
  }
}

// ----- Video Player -----
@Composable
private fun MemoryVideoPlayer(url: String) {
  val context = LocalContext.current
  val exoPlayer = remember { ExoPlayer.Builder(context).build() }

  LaunchedEffect(url) {
    exoPlayer.setMediaItem(ExoMediaItem.fromUri(url))
    exoPlayer.prepare()
    exoPlayer.playWhenReady = false
  }

  AndroidView(
      factory = {
        PlayerView(it).apply {
          player = exoPlayer
          useController = true
        }
      },
      modifier = Modifier.fillMaxSize())

  DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
}

// ----- Mixed Media Model -----
sealed class MediaItem {
  data class Image(val url: String) : MediaItem()

  data class Video(val url: String) : MediaItem()
}

// Convert URLs to MediaItem
fun parseMediaItems(urls: List<String>): List<MediaItem> {
  val videoExtensions = listOf(".mp4", ".mov", ".avi", ".mkv", ".webm")

  return urls.map { url ->
    val lower = url.lowercase()

    if (videoExtensions.any { lower.contains(it) }) {
      MediaItem.Video(url)
    } else {
      MediaItem.Image(url)
    }
  }
}

/* ------------------------ MEDIA ------------------------ */

@Composable
private fun MemoryMediaPreview(urls: List<String>) {
  if (urls.isEmpty()) return
  // Parse all media items
  val items = parseMediaItems(urls)
  // Find the first image
  val firstImage = items.firstOrNull { it is MediaItem.Image } as? MediaItem.Image

  firstImage?.let { image ->
    AsyncImage(
        model = image.url,
        contentDescription = "Memory photo",
        modifier =
            Modifier.fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .testTag("memoryMediaPreview"),
        contentScale = ContentScale.Crop)
  }
}

@Composable
private fun MemoryMediaGallery(urls: List<String>) {
  if (urls.isEmpty()) {
    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
      Text(
          "No media available",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("noMediaText"))
    }
    return
  }

  val items = parseMediaItems(urls)
  val pagerState = rememberPagerState(initialPage = 0) { items.size }

  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(420.dp).testTag("mediaPager")) { page ->
          when (val media = items[page]) {
            is MediaItem.Image -> {
              Box(
                  modifier =
                      Modifier.fillMaxSize()
                          .padding(horizontal = 12.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .testTag("mediaItem_$page")) {
                    AsyncImage(
                        model = media.url,
                        contentDescription = "Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                  }
            }
            is MediaItem.Video -> {
              Box(
                  modifier =
                      Modifier.fillMaxSize()
                          .padding(horizontal = 12.dp)
                          .clip(RoundedCornerShape(12.dp))) {
                    MemoryVideoPlayer(media.url)
                  }
            }
          }
        }

    Spacer(Modifier.height(8.dp))

    Row(modifier = Modifier.testTag("mediaIndicatorsRow")) {
      repeat(items.size) { i ->
        val selected = pagerState.currentPage == i
        Box(
            modifier =
                Modifier.padding(3.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .testTag("mediaIndicator_$i"))
      }
    }
  }
}

/* ------------------------ Helpers ------------------------ */

private fun formatMemoryDate(timestamp: Timestamp): String {
  val date = timestamp.toDate()
  val sdf = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
  return sdf.format(date)
}

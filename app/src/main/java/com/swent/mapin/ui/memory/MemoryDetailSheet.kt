package com.swent.mapin.ui.memory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
 * @param isOwner Whether the current user is the owner of the memory
 * @param taggedUserNames List of names for tagged users
 * @param onShare Called when share icon is tapped
 * @param onClose Called when close icon is tapped
 * @param onEdit Called when editing is requested
 * @param onDelete Called when delete is requested
 * @param onOpenLinkedEvent Navigate to the event if memory is linked to one
 */
@Composable
fun MemoryDetailSheet(
    memory: Memory,
    sheetState: BottomSheetState,
    ownerName: String,
    isOwner: Boolean = false,
    taggedUserNames: List<String>,
    onShare: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onOpenLinkedEvent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth().testTag("memoryDetailSheet")) {
    when (sheetState) {
      BottomSheetState.COLLAPSED ->
          CollapsedMemoryContent(memory = memory, onShare = onShare, onClose = onClose)
      BottomSheetState.MEDIUM -> {
        MemoryDetailHeader(onShare = onShare, onClose = onClose)
        MediumMemoryContent(
            memory = memory,
            ownerName = ownerName,
            taggedUserNames = taggedUserNames,
            onOpenLinkedEvent = onOpenLinkedEvent)
      }
      BottomSheetState.FULL -> {
        MemoryDetailHeader(onShare = onShare, onClose = onClose)
        FullMemoryContent(
            memory = memory,
            ownerName = ownerName,
            isOwner = isOwner,
            taggedUserNames = taggedUserNames,
            onEdit = onEdit,
            onDelete = onDelete,
            onOpenLinkedEvent = onOpenLinkedEvent)
      }
    }
  }
}

/* ------------------------ HEADER ------------------------ */

@Composable
private fun MemoryDetailHeader(onShare: () -> Unit, onClose: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onShare, modifier = Modifier.testTag("shareMemoryButton")) {
          Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share memory",
              tint = MaterialTheme.colorScheme.primary)
        }

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
private fun CollapsedMemoryContent(memory: Memory, onShare: () -> Unit, onClose: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onShare) {
          Icon(
              Icons.Default.Share,
              contentDescription = "Share",
              tint = MaterialTheme.colorScheme.primary)
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = memory.title.ifBlank { "Memory" },
                  maxLines = 1,
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.basicMarquee().testTag("memoryTitleCollapsed"))
              if (memory.eventId != null) {
                Text(
                    text = "Memory",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
              }
            }

        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
      }
}

/* ------------------------ MEDIUM ------------------------ */

@Composable
private fun MediumMemoryContent(
    memory: Memory,
    ownerName: String,
    taggedUserNames: List<String>,
    onOpenLinkedEvent: () -> Unit
) {
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

    if (memory.eventId != null) {
      Spacer(Modifier.height(16.dp))
      LinkedEventChip(onOpenLinkedEvent)
    }
  }
}

/* ------------------------ FULL ------------------------ */

@Composable
private fun FullMemoryContent(
    memory: Memory,
    ownerName: String,
    isOwner: Boolean,
    taggedUserNames: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenLinkedEvent: () -> Unit
) {
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

        if (memory.eventId != null) {
          Spacer(Modifier.height(16.dp))
          LinkedEventChip(onOpenLinkedEvent)
        }

        Spacer(Modifier.height(24.dp))

        if (isOwner) {
          // Actions
          Button(onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("editMemoryButton")) {
            Text("Edit Memory")
          }

          Spacer(Modifier.height(8.dp))

          OutlinedButton(
              onClick = onDelete,
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.fillMaxWidth().testTag("deleteMemoryButton")) {
                Text("Delete Memory")
              }
        }
      }
}

/* ------------------------ SUBCOMPONENTS ------------------------ */

@Composable
private fun MemoryMetadata(memory: Memory, ownerName: String) {
  val dateText = memory.createdAt?.let { formatMemoryDate(it) } ?: "Unknown date"

  Column {
    Text("By $ownerName • $dateText", style = MaterialTheme.typography.bodySmall)

    if (memory.isPublic) {
      Text(
          "Public memory",
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodySmall)
    } else {
      Text(
          "Private memory",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun TaggedUsersSection(names: List<String>) {
  Column {
    Text("Tagged people", fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(names.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun LinkedEventChip(onClick: () -> Unit) {
  AssistChip(
      onClick = onClick,
      label = { Text("View linked event") },
      modifier = Modifier.testTag("linkedEventChip"))
}

// ----- Pinch-to-zoom modifier -----
fun Modifier.pinchToZoom(): Modifier = composed {
  var scale by remember { mutableStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  this.pointerInput(Unit) {
        awaitEachGesture {
          awaitFirstDown()
          var zoom = 1f
          var pan = Offset.Zero

          awaitTouchSlopOrCancellation(currentEvent.changes.first().id) { _, _ ->
            val event = currentEvent
            if (event.changes.size >= 2) {
              val zoomChange = event.calculateZoom()
              val panChange = event.calculatePan()

              zoom *= zoomChange
              pan += panChange

              event.changes.forEach { it.consume() }
            }
          }

          scale = (scale * zoom).coerceIn(1f, 4f)
          offset += pan
        }
      }
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = offset.x
        translationY = offset.y
      }
}

// ----- Video Player -----
@Composable
fun MemoryVideoPlayer(url: String) {
  val context = LocalContext.current
  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(ExoMediaItem.fromUri(url))
      prepare()
      playWhenReady = false
    }
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

/* ------------------------ MEDIA ------------------------ */

@Composable
private fun MemoryMediaPreview(urls: List<String>) {
  AsyncImage(
      model = urls.first(),
      contentDescription = "Memory photo",
      modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)),
      contentScale = ContentScale.Crop)
}

@Composable
fun MemoryMediaGallery(urls: List<String>) {
  if (urls.isEmpty()) {
    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
      Text("No media available", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    return
  }

  // Convert URLs to MediaItem
  val items =
      urls.map {
        if (it.endsWith(".mp4") || it.contains("video")) MediaItem.Video(it)
        else MediaItem.Image(it)
      }

  val pagerState = rememberPagerState(initialPage = 0) { items.size }

  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(420.dp)) { page ->
      when (val media = items[page]) {
        is MediaItem.Image -> {
          AsyncImage(
              model = media.url,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = 12.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .pinchToZoom())
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

    Row {
      repeat(items.size) { i ->
        val selected = pagerState.currentPage == i
        Box(
            modifier =
                Modifier.padding(3.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
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

package com.swent.mapin.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.swent.mapin.R
import com.swent.mapin.model.memory.Memory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formats a [Timestamp] to a human-readable date string. */
private val memoryDateFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault())

/**
 * Memories screen displaying a list of the user's memories.
 *
 * @param onNavigateBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(onNavigateBack: () -> Unit = {}, viewModel: MemoriesViewModel = viewModel()) {
  LaunchedEffect(Unit) { viewModel.refresh() }

  val memories by viewModel.memories.collectAsState()
  val error by viewModel.error.collectAsState()
  val displayMode by viewModel.displayMode.collectAsState()

  DisposableEffect(Unit) {
    onDispose {
      if (displayMode == MemoryDisplayMode.NEARBY_MEMORIES) {
        viewModel.returnToOwnerMemories()
      }
    }
  }
  var hasNavigatedBack by remember { mutableStateOf(false) }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        MemoriesTopBar(
            displayMode = displayMode,
            onNavigateBack = {
              if (!hasNavigatedBack) {
                hasNavigatedBack = true
                onNavigateBack()
              }
            })
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize().padding(paddingValues).background(getBackgroundColor())) {
              MemoriesContent(
                  memories = memories,
                  displayMode = displayMode,
                  viewModel = viewModel,
                  onNavigateBack = onNavigateBack)

              error?.let { err ->
                Text(
                    err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
              }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoriesTopBar(displayMode: MemoryDisplayMode, onNavigateBack: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            text = stringResource(getTitleResource(displayMode)),
            modifier = Modifier.testTag("memoriesScreenTitle"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
      },
      navigationIcon = {
        IconButton(onClick = onNavigateBack) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back_button))
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}

@Composable
private fun MemoriesContent(
    memories: List<Memory>,
    displayMode: MemoryDisplayMode,
    viewModel: MemoriesViewModel,
    onNavigateBack: () -> Unit
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      contentPadding = PaddingValues(bottom = 16.dp)) {
        item { MemoriesHeader(displayMode = displayMode, onRefresh = { viewModel.refresh() }) }

        if (memories.isEmpty()) {
          item { EmptyMemoriesState(displayMode = displayMode) }
        } else {
          items(memories, key = { it.uid }) { memory ->
            MemoryItem(
                memory = memory,
                onClick = {
                  viewModel.selectMemoryToView(memory.uid)
                  onNavigateBack()
                },
                mediaUrls = memory.mediaUrls,
                taggedNames = memory.taggedUserIds)
            Spacer(modifier = Modifier.height(12.dp))
          }
        }
      }
}

@Composable
private fun MemoriesHeader(displayMode: MemoryDisplayMode, onRefresh: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    Text(
        text = stringResource(getSectionTitleResource(displayMode)),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f).testTag("yourMemoriesMessage"))

    TextButton(onClick = onRefresh, modifier = Modifier.testTag("refreshAllButton")) {
      Text(stringResource(R.string.memories_refresh_all))
    }
  }
  Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun EmptyMemoriesState(displayMode: MemoryDisplayMode) {
  Spacer(modifier = Modifier.height(24.dp))
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp).testTag("noMemoriesMessage")) {
          Text(
              text = stringResource(getEmptyTitleResource(displayMode)),
              style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = stringResource(getEmptyMessageResource(displayMode)),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis)
        }
  }
}

// Helper functions to extract conditional logic
@Composable
private fun getBackgroundColor(): Color {
  return if (MaterialTheme.colorScheme.background == MaterialTheme.colorScheme.surface) {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
  } else {
    MaterialTheme.colorScheme.background
  }
}

private fun getTitleResource(displayMode: MemoryDisplayMode): Int {
  return when (displayMode) {
    MemoryDisplayMode.OWNER_MEMORIES -> R.string.memories_screen_title_my
    MemoryDisplayMode.NEARBY_MEMORIES -> R.string.memories_screen_title_nearby
  }
}

private fun getSectionTitleResource(displayMode: MemoryDisplayMode): Int {
  return when (displayMode) {
    MemoryDisplayMode.OWNER_MEMORIES -> R.string.memories_your_memories
    MemoryDisplayMode.NEARBY_MEMORIES -> R.string.memories_nearby_area
  }
}

private fun getEmptyTitleResource(displayMode: MemoryDisplayMode): Int {
  return when (displayMode) {
    MemoryDisplayMode.OWNER_MEMORIES -> R.string.memories_no_memories_yet
    MemoryDisplayMode.NEARBY_MEMORIES -> R.string.memories_no_memories_area
  }
}

private fun getEmptyMessageResource(displayMode: MemoryDisplayMode): Int {
  return when (displayMode) {
    MemoryDisplayMode.OWNER_MEMORIES -> R.string.memories_empty_message_owner
    MemoryDisplayMode.NEARBY_MEMORIES -> R.string.memories_empty_message_nearby
  }
}

/**
 * Formats a [Memory] to a human-readable date string.
 *
 * @param memory Memory to format.
 */
private fun formatMemoryDate(memory: Memory): String =
    memory.createdAt?.toDate()?.time?.let { millis ->
      memoryDateFormatter.format(Instant.ofEpochMilli(millis))
    } ?: ""

/**
 * Thumbnail for a memory.
 *
 * @param imageUrl URL of the image to display.
 */
@Composable
private fun MemoryThumbnail(imageUrl: String?) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(16f / 9f)
              .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center) {
        if (imageUrl != null) {
          AsyncImage(
              model = imageUrl,
              contentDescription = stringResource(R.string.memories_memory_photo),
              modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
              contentScale = ContentScale.Crop)
        } else {
          Icon(
              imageVector = Icons.Default.Image,
              contentDescription = stringResource(R.string.memories_placeholder),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
              modifier = Modifier.size(48.dp))
        }
      }
}

/**
 * Footer for a memory.
 *
 * @param isPublic Whether the memory is public.
 * @param dateText Date of the memory.
 */
@Composable
private fun MemoryFooter(isPublic: Boolean, dateText: String) {
  Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(
        imageVector = if (isPublic) Icons.Default.LockOpen else Icons.Default.Lock,
        contentDescription =
            stringResource(if (isPublic) R.string.memories_public else R.string.memories_private),
        tint = MaterialTheme.colorScheme.primary)
    Text(
        text =
            stringResource(if (isPublic) R.string.memories_public else R.string.memories_private),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp))
    Text(
        text = "  •  " + stringResource(R.string.memories_created, dateText),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun MemoryItem(
    memory: Memory,
    onClick: () -> Unit,
    mediaUrls: List<String>,
    taggedNames: List<String>
) {
  val dateText = formatMemoryDate(memory)

  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {

          // --- 16:9 thumbnail ---
          MemoryThumbnail(mediaUrls.firstOrNull())

          Column(modifier = Modifier.padding(16.dp)) {
            MemoryTitle(memory = memory)
            MemoryTaggedUsers(memory = memory, taggedNames = taggedNames)
            MemoryDescription(memory = memory)
            MemoryFooter(memory.isPublic, dateText)
          }
        }
      }
}

@Composable
private fun MemoryTitle(memory: Memory) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
        text = memory.title.ifBlank { stringResource(R.string.memories_default_title) },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f))
  }
}

@Composable
private fun MemoryTaggedUsers(memory: Memory, taggedNames: List<String>) {
  if (memory.taggedUserIds.isNotEmpty()) {
    Text(
        text = stringResource(R.string.memories_tagged, taggedNames.joinToString { it }),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp))
  }
}

@Composable
private fun MemoryDescription(memory: Memory) {
  if (!memory.description.isBlank()) {
    Text(
        text = memory.description,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp))
  }
}

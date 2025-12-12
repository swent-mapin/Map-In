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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
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
 * @param onOpenEvent Callback to navigate to the event detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    onNavigateBack: () -> Unit = {},
    onOpenEvent: (eventId: String) -> Unit = {},
    viewModel: MemoriesViewModel = viewModel()
) {

  val memories by viewModel.memories.collectAsState()
  val error by viewModel.error.collectAsState()

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "Memories",
                  modifier = Modifier.testTag("memoriesScreenTitle"),
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        if (MaterialTheme.colorScheme.background ==
                            MaterialTheme.colorScheme.surface) {
                          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        } else {
                          MaterialTheme.colorScheme.background
                        })) {
              LazyColumn(
                  modifier = Modifier.fillMaxSize().padding(16.dp),
                  contentPadding = PaddingValues(bottom = 16.dp)) {
                    item {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Your Memories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).testTag("yourMemoriesMessage"))

                            TextButton(
                                onClick = { viewModel.refresh() },
                                modifier = Modifier.testTag("refreshAllButton")) {
                                  Text("Refresh All")
                                }
                          }
                      Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (memories.isEmpty()) {
                      item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center) {
                              Column(
                                  horizontalAlignment = Alignment.CenterHorizontally,
                                  modifier =
                                      Modifier.padding(horizontal = 24.dp)
                                          .testTag("noMemoriesMessage")) {
                                    Text(
                                        text = "No memories yet",
                                        style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text =
                                            "Create memories for attended events and they'll appear here",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis)
                                  }
                            }
                      }
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

              error?.let { err ->
                Text(
                    err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
              }
            }
      }
}

@Composable
private fun MemoryItem(
    memory: Memory,
    onClick: () -> Unit,
    mediaUrls: List<String>,
    taggedNames: List<String>
) {
  val dateText =
      memory.createdAt?.toDate()?.time?.let { millis ->
        memoryDateFormatter.format(Instant.ofEpochMilli(millis))
      } ?: ""
  val items = parseMediaItems(mediaUrls)
  // Find the first image
  val firstImage = items.firstOrNull { it is MediaItem.Image } as? MediaItem.Image

  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {

          // --- 16:9 thumbnail ---
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .aspectRatio(16f / 9f)
                      .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center) {
                if (firstImage != null) {
                  AsyncImage(
                      model = firstImage.url,
                      contentDescription = "Memory photo",
                      modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                      contentScale = ContentScale.Crop)
                } else {
                  Icon(
                      imageVector = Icons.Default.Image,
                      contentDescription = "Placeholder",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                      modifier = Modifier.size(48.dp))
                }
              }

          Column(modifier = Modifier.padding(16.dp)) {

            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = memory.title.ifBlank { "Memory ${memory.uid}" },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f))
            }

            // Tagged users
            if (memory.taggedUserIds.isNotEmpty()) {
              Text(
                  text = "Tagged: " + taggedNames.joinToString { it },
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(top = 4.dp))
            }

            // Description preview (2 lines)
            if (!memory.description.isBlank()) {
              Text(
                  text = memory.description,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(top = 8.dp))
            }

            // Footer row: Public + Date
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector =
                          if (memory.isPublic) Icons.Default.LockOpen else Icons.Default.Lock,
                      contentDescription = if (memory.isPublic) "Public" else "Private",
                      tint = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                      text = if (memory.isPublic) "Public" else "Private",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(start = 4.dp))

                  Text(
                      text = "  •  Created $dateText",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
          }
        }
      }
}

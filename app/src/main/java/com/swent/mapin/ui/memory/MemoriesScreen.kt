package com.swent.mapin.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.swent.mapin.model.memory.Memory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Memories screen displaying a list of the user's memories.
 *
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param memories List of memories to display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    onNavigateBack: () -> Unit,
    memories: List<Memory> = com.swent.mapin.ui.memory.memories
    /*viewModel: MemoryViewModel TODO make the viewModel*/
) {
  // val memories by viewModel.memories.collectAsState()
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
              Column(
                  modifier =
                      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(
                        "Your Memories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp).testTag("yourMemoriesMessage"))
                    if (memories.isEmpty()) {
                      Spacer(modifier = Modifier.height(24.dp))
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier.padding(horizontal = 24.dp).testTag("noMemoriesMessage")) {
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
                    } else {
                      memories.forEach { memory ->
                        MemoryItem(
                            memory = memory,
                            onClick = { /*onMemoryClick(memory)*/},
                            isPublic = memory.isPublic,
                            firstMediaUrl = memory.mediaUrls.firstOrNull())

                        Spacer(modifier = Modifier.height(12.dp))
                      }
                    }
                  }
            }
      }
}

@Composable
private fun MemoryItem(
    memory: Memory,
    onClick: () -> Unit,
    isPublic: Boolean,
    firstMediaUrl: String?
) {
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault())
  val dateText =
      memory.createdAt?.toDate()?.time?.let { millis ->
        formatter.format(Instant.ofEpochMilli(millis))
      } ?: ""

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
                if (firstMediaUrl != null) {
                  AsyncImage(
                      model = firstMediaUrl,
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

            // Title + menu
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = memory.title.ifBlank { "Memory ${memory.uid}" },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f))

              Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "Menu",
                  tint = MaterialTheme.colorScheme.primary)
            }

            // Tagged users
            if (memory.taggedUserIds.isNotEmpty()) {
              Text(
                  text = "Tagged: " + memory.taggedUserIds.joinToString { it },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                      imageVector = if (isPublic) Icons.Default.LockOpen else Icons.Default.Lock,
                      contentDescription = if (isPublic) "Public" else "Private",
                      tint = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                      text = if (isPublic) "Public" else "Private",
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

val memories =
    listOf(
        Memory(
            uid = "mem1",
            title = "Amazing Beach Day",
            description = "Had an incredible time playing volleyball with friends!",
            eventId = "2",
            ownerId = "user1",
            isPublic = true,
            createdAt = Timestamp.now(),
            mediaUrls = listOf("https://picsum.photos/id/69/200"),
            taggedUserIds = listOf("user2", "user3")),
        Memory(
            uid = "mem2",
            title = "Summer Vibes",
            description = "The festival was packed with amazing food and music. Best summer ever!",
            eventId = "1", // Summer Festival 2024
            ownerId = "user2",
            isPublic = true,
            createdAt = Timestamp.now(),
            mediaUrls =
                listOf("https://picsum.photos/id/256/200", "https://picsum.photos/id/139/200"),
            taggedUserIds = listOf("user1", "user4")),
        Memory(
            uid = "mem3",
            title = "",
            description = "Just enjoying the sunset alone. Sometimes peace is all you need.",
            eventId = null,
            ownerId = "user1",
            isPublic = false,
            createdAt = Timestamp.now(),
            mediaUrls = emptyList(),
            taggedUserIds = emptyList()))

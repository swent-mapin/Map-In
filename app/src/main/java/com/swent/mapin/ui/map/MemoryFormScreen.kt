package com.swent.mapin.ui.map

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swent.mapin.model.event.Event
import java.text.SimpleDateFormat
import java.util.Locale

// Assisted by AI

/** Data class to hold memory form input */
data class MemoryFormData(
    val title: String,
    val description: String,
    val eventId: String?,
    val isPublic: Boolean,
    val mediaUris: List<android.net.Uri>,
    val taggedUserIds: List<String>
)

/**
 * Memory creation form screen displayed in full mode bottom sheet. Allows users to create memories
 * with optional event association, title, description, photos/videos, friends, and visibility
 * settings.
 *
 * @param scrollState ScrollState for the scrollable content
 * @param availableEvents List of existing events user can associate with this memory
 * @param onSave Callback when user saves the memory with form data
 * @param onCancel Callback when user cancels memory creation
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryFormScreen(
    scrollState: ScrollState,
    availableEvents: List<Event>,
    onSave: (MemoryFormData) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
  // Form state
  var selectedEvent by remember { mutableStateOf<Event?>(null) }
  var showEventPicker by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var isPublic by remember { mutableStateOf(false) }
  val selectedMediaUris = remember { mutableStateListOf<Uri>() }
  var showUserPicker by remember { mutableStateOf(false) }
  val taggedUserIds = remember { mutableStateListOf<String>() }

  // Media picker launcher
  val mediaPickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)) { uris ->
            selectedMediaUris.clear()
            selectedMediaUris.addAll(uris)
          }

  // Check if form is valid (description is filled)
  val isFormValid = description.isNotBlank()

  Column(modifier = modifier.fillMaxSize()) {
    // Header with Cancel and Save buttons
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          // Cancel button
          IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.onSurface)
          }

          Text(
              text = "New Memory",
              style = MaterialTheme.typography.titleLarge,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f))

          // Save button with dark rounded background (changes color when form is valid)
          IconButton(
              onClick = {
                onSave(
                    MemoryFormData(
                        title = title,
                        description = description,
                        eventId = selectedEvent?.uid,
                        isPublic = isPublic,
                        mediaUris = selectedMediaUris.toList(),
                        taggedUserIds = taggedUserIds.toList()))
              },
              enabled = isFormValid,
              modifier =
                  Modifier.size(48.dp)
                      .background(
                          color =
                              if (isFormValid) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surfaceVariant,
                          shape = CircleShape)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint =
                        if (isFormValid) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant)
              }
        }

    Spacer(modifier = Modifier.height(16.dp))

    // Scrollable content
    Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {

      // 1. Event Picker
      Text(
          text = "Link to event (optional)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp))

      // Selected event display / picker trigger
      Card(
          modifier = Modifier.fillMaxWidth().clickable { showEventPicker = true },
          colors =
              CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  if (selectedEvent != null) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                          text = selectedEvent!!.title,
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onSurface)
                      Spacer(modifier = Modifier.height(4.dp))
                      val dateStr =
                          selectedEvent!!.date?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(it.toDate())
                          } ?: "No date"
                      Text(
                          text = "$dateStr • ${selectedEvent!!.location.name}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { selectedEvent = null }) {
                      Icon(
                          imageVector = Icons.Default.Close,
                          contentDescription = "Clear selection",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  } else {
                    Text(
                        text = "Tap to select an event",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Select event",
                        tint = MaterialTheme.colorScheme.primary)
                  }
                }
          }

      // Event selection dialog
      if (showEventPicker) {
        EventPickerDialog(
            events = availableEvents,
            onEventSelected = {
              selectedEvent = it
              showEventPicker = false
            },
            onDismiss = { showEventPicker = false })
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 2. Title
      Text(
          text = "Title (optional)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp))

      OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          placeholder = { Text("Name this memory") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true)

      Spacer(modifier = Modifier.height(24.dp))

      // 3. Description (mandatory)
      Text(
          text = "Description *",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp))

      OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          placeholder = { Text("What happened?") },
          modifier = Modifier.fillMaxWidth().height(120.dp),
          maxLines = 6)

      Spacer(modifier = Modifier.height(24.dp))

      // 4. Photos/Videos Picker
      Text(
          text = "Photos or videos (up to 5)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp))

      if (selectedMediaUris.isEmpty()) {
        // Empty state - show add button
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(120.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp))
                    .clickable {
                      mediaPickerLauncher.launch(
                          PickVisualMediaRequest(
                              ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                    .padding(16.dp),
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
      } else {
        // Show selected media in a grid
        Column(modifier = Modifier.fillMaxWidth()) {
          FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedMediaUris.forEach { uri ->
                  Box(modifier = Modifier.size(100.dp)) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected media",
                        modifier =
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop)
                    // Remove button
                    IconButton(
                        onClick = { selectedMediaUris.remove(uri) },
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    shape = CircleShape)) {
                          Icon(
                              imageVector = Icons.Default.Close,
                              contentDescription = "Remove",
                              modifier = Modifier.size(16.dp),
                              tint = MaterialTheme.colorScheme.onSurface)
                        }
                  }
                }
                // Add more button if less than 5
                if (selectedMediaUris.size < 5) {
                  Box(
                      modifier =
                          Modifier.size(100.dp)
                              .border(
                                  width = 1.dp,
                                  color = MaterialTheme.colorScheme.outline,
                                  shape = RoundedCornerShape(8.dp))
                              .clickable {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                              },
                      contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add more",
                            tint = MaterialTheme.colorScheme.primary)
                      }
                }
              }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 5. Add People
      Text(
          text = "Tag people",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp))

      FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Add person button
            Box(
                modifier =
                    Modifier.size(56.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape)
                        .clickable { showUserPicker = true }
                        .padding(8.dp),
                contentAlignment = Alignment.Center) {
                  Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = "Add person",
                      tint = MaterialTheme.colorScheme.primary)
                }

            // Tagged users chips
            taggedUserIds.forEach { userId ->
              Box(
                  modifier =
                      Modifier.clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primaryContainer)
                          .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                          Icon(
                              imageVector = Icons.Default.Person,
                              contentDescription = null,
                              modifier = Modifier.size(16.dp),
                              tint = MaterialTheme.colorScheme.onPrimaryContainer)
                          Text(
                              text = userId, // TODO: Show user name instead of ID
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onPrimaryContainer)
                          IconButton(
                              onClick = { taggedUserIds.remove(userId) },
                              modifier = Modifier.size(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                              }
                        }
                  }
            }
          }

      // User picker dialog
      if (showUserPicker) {
        UserPickerDialog(
            onUserSelected = { userId ->
              if (!taggedUserIds.contains(userId)) {
                taggedUserIds.add(userId)
              }
            },
            onDismiss = { showUserPicker = false })
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 6. Visibility Toggle
      Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "Make this memory public", style = MaterialTheme.typography.bodyLarge)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = "Others can see this memory on the event page",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
          }

      // Bottom spacing
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

/**
 * Searchable event picker dialog allowing users to search and select from existing events.
 *
 * @param events List of available events to choose from
 * @param onEventSelected Callback when user selects an event
 * @param onDismiss Callback when dialog is dismissed without selection
 */
@Composable
fun EventPickerDialog(
    events: List<Event>,
    onEventSelected: (Event) -> Unit,
    onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }

  // Filter events based on search query
  val filteredEvents =
      remember(events, searchQuery) {
        if (searchQuery.isBlank()) {
          events
        } else {
          events.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.location.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
          }
        }
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Select an event", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Search bar
          OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              modifier = Modifier.fillMaxWidth(),
              placeholder = { Text("Search events...") },
              leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                  }
                }
              },
              singleLine = true)

          Spacer(modifier = Modifier.height(16.dp))

          // Events list
          if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center) {
                  Text(
                      text =
                          if (searchQuery.isBlank()) "No events available" else "No events found",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
          } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
              items(filteredEvents) { event ->
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                          onEventSelected(event)
                        },
                    colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)) {
                      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                              val dateStr =
                                  event.date?.let {
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        .format(it.toDate())
                                  } ?: "No date"
                              Text(
                                  text = dateStr,
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant)
                              Text(
                                  text = "•",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant)
                              Text(
                                  text = event.location.name,
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                      }
                    }
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/**
 * Simple user picker dialog for tagging users in memories.
 *
 * @param onUserSelected Callback when user selects a user to tag
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun UserPickerDialog(onUserSelected: (String) -> Unit, onDismiss: () -> Unit) {
  var userIdInput by remember { mutableStateOf("") }

  // TODO: Replace with actual friend list from repository
  val sampleUsers = listOf("user1", "user2", "user3", "user4", "user5")

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Tag people", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Search/input field
          OutlinedTextField(
              value = userIdInput,
              onValueChange = { userIdInput = it },
              modifier = Modifier.fillMaxWidth(),
              placeholder = { Text("Enter user ID or search...") },
              leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
              },
              singleLine = true)

          Spacer(modifier = Modifier.height(16.dp))

          // User list
          LazyColumn(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            items(sampleUsers) { userId ->
              Card(
                  modifier =
                      Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        onUserSelected(userId)
                      },
                  colors =
                      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                          Box(
                              modifier =
                                  Modifier.size(40.dp)
                                      .clip(CircleShape)
                                      .background(MaterialTheme.colorScheme.primaryContainer),
                              contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                              }
                          Text(
                              text = userId,
                              style = MaterialTheme.typography.bodyLarge,
                              color = MaterialTheme.colorScheme.onSurface)
                        }
                  }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}

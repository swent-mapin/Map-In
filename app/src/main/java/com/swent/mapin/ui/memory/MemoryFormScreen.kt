package com.swent.mapin.ui.memory

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.components.UserPickerDialog
import java.text.SimpleDateFormat
import java.util.Locale

// Assisted by AI

// Constants
private const val MAX_MEDIA_COUNT = 5
private val MEDIA_THUMBNAIL_SIZE = 100.dp
private val DESCRIPTION_MIN_HEIGHT = 120.dp
private val USER_AVATAR_SIZE = 56.dp

/**
 * Data class to hold memory form input data.
 *
 * This represents the complete state of the memory creation form before it's submitted.
 *
 * @property title The title/headline of the memory
 * @property description Detailed description of what happened
 * @property eventId Optional ID of the event this memory is associated with
 * @property isPublic Whether the memory should be visible to others
 * @property mediaUris List of URIs for photos/videos to attach
 * @property taggedUserIds List of user IDs tagged in this memory
 */
data class MemoryFormData(
    val title: String,
    val description: String,
    val eventId: String?,
    val isPublic: Boolean,
    val mediaUris: List<Uri>,
    val taggedUserIds: List<String>
)

/**
 * Event selection section for memory form
 *
 * @param selectedEvent Currently selected event (non-null)
 * @param onEventClick Callback when event picker is clicked
 */
@Composable
private fun EventSelectionSection(
    selectedEvent: Event?,
    onEventClick: (Event) -> Unit,
) {
  Text(
      text = "Link to event",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp))

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .clickable { if (selectedEvent != null) onEventClick(selectedEvent) }
              .testTag("eventSelectionCard"),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedEvent?.title ?: "",
                    modifier = Modifier.testTag("memoryForm_selectedEventTitle"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr =
                    remember(selectedEvent?.date) {
                      selectedEvent?.date?.toDate()?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(it)
                      } ?: "No date"
                    }
                Text(
                    text = "$dateStr • ${selectedEvent?.location?.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
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
internal fun MediaSelectionSection(
    selectedMediaUris: List<Uri>,
    onLaunchMediaPicker: () -> Unit,
    onRemoveMedia: (Uri) -> Unit
) {
  Text(
      text = "Photos or videos (up to $MAX_MEDIA_COUNT)",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp))

  if (selectedMediaUris.isEmpty()) {
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
  } else {
    Column(modifier = Modifier.fillMaxWidth()) {
      FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedMediaUris.forEach { uri ->
              Box(modifier = Modifier.size(MEDIA_THUMBNAIL_SIZE)) {
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
                IconButton(
                    onClick = { onRemoveMedia(uri) },
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
            if (selectedMediaUris.size < MAX_MEDIA_COUNT) {
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
          }
    }
  }
}

/**
 * User tagging section for memory form
 *
 * @param taggedUserIds List of tagged user IDs
 * @param onAddUserClick Callback when add user button is clicked
 * @param onRemoveUser Callback when a user tag is removed
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserTaggingSection(
    taggedUserIds: List<String>,
    onAddUserClick: () -> Unit,
    onRemoveUser: (String) -> Unit
) {
  Text(
      text = "Tag people",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp))

  FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier =
                Modifier.size(USER_AVATAR_SIZE)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape)
                    .clickable { onAddUserClick() }
                    .padding(8.dp)
                    .testTag("addUserButton"),
            contentAlignment = Alignment.Center) {
              Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Add person",
                  tint = MaterialTheme.colorScheme.primary)
            }

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
                          onClick = { onRemoveUser(userId) }, modifier = Modifier.size(16.dp)) {
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
}

/**
 * UI switch to toggle Public/Private
 *
 * @param isPublic boolean indicating the state of public (true) or private (false)
 * @param onPublicChange callback for when the switch is toggled
 * @param title The title text to display
 * @param subTitle The subtitle text to display
 * @param modifier Optional modifier for the switch
 * @param textTestTag Optional modifier for the text (for testing)
 */
@Composable
private fun PublicSwitch(
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    title: String,
    subTitle: String,
    modifier: Modifier = Modifier,
    textTestTag: Modifier = Modifier,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = textTestTag)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
              text = subTitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(checked = isPublic, onCheckedChange = onPublicChange, modifier = modifier)
      }
}

/**
 * Memory creation form screen displayed in full mode bottom sheet. Allows users to create memories
 * with event association, title, description, photos/videos, friends, and visibility settings.
 *
 * @param scrollState ScrollState for the scrollable content
 * @param availableEvents List of existing events user can associate with this memory
 * @param onSave Callback when user saves the memory with form data
 * @param onCancel Callback when user cancels memory creation
 * @param onEventClick Callback when an event is clicked
 * @param modifier Modifier for the screen
 * @param initialSelectedEvent Optional initial selected event (non-null)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryFormScreen(
    scrollState: ScrollState,
    availableEvents: List<Event>,
    onSave: (MemoryFormData) -> Unit,
    onCancel: () -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedEvent: Event? = null,
) {
  // Form state
  var selectedEvent by remember { mutableStateOf(initialSelectedEvent) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var isPublic by remember { mutableStateOf(false) }
  val selectedMediaUris = remember { mutableStateListOf<Uri>() }
  var showUserPicker by remember { mutableStateOf(false) }
  val taggedUserIds = remember { mutableStateListOf<String>() }

  val mediaPickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_MEDIA_COUNT)) {
              uris ->
            uris.forEach { uri ->
              // Avoid duplicates
              if (!selectedMediaUris.contains(uri)) {
                selectedMediaUris.add(uri)
              }
            }
          }

  val isFormValid = description.isNotBlank()

  Column(modifier = modifier.fillMaxSize().testTag("memoryFormScreen")) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onCancel, modifier = Modifier.size(48.dp).testTag("cancelButton")) {
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

          IconButton(
              onClick = {
                onSave(
                    MemoryFormData(
                        title = title,
                        description = description,
                        eventId = selectedEvent?.uid,
                        isPublic = isPublic,
                        mediaUris = selectedMediaUris.toList(),
                        taggedUserIds = taggedUserIds))
              },
              enabled = isFormValid,
              modifier =
                  Modifier.size(48.dp)
                      .background(
                          color =
                              if (isFormValid) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surfaceVariant,
                          shape = CircleShape)
                      .testTag("saveButton")) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint =
                        if (isFormValid) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant)
              }
        }

    Spacer(modifier = Modifier.height(16.dp))

    Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
      EventSelectionSection(selectedEvent = selectedEvent, onEventClick = onEventClick)

      Spacer(modifier = Modifier.height(24.dp))

      Text(
          text = "Title (optional)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp))

      OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          placeholder = { Text("Name this memory") },
          modifier = Modifier.fillMaxWidth().testTag("titleField"),
          singleLine = true)

      Spacer(modifier = Modifier.height(24.dp))

      // Description (mandatory)
      Text(
          text = "Description *",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp))

      OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          placeholder = { Text("What happened?") },
          modifier =
              Modifier.fillMaxWidth().height(DESCRIPTION_MIN_HEIGHT).testTag("descriptionField"),
          maxLines = 6)

      Spacer(modifier = Modifier.height(24.dp))

      MediaSelectionSection(
          selectedMediaUris = selectedMediaUris,
          onLaunchMediaPicker = {
            mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
          },
          onRemoveMedia = { uri -> selectedMediaUris.remove(uri) })

      Spacer(modifier = Modifier.height(24.dp))

      UserTaggingSection(
          taggedUserIds = taggedUserIds.toList(),
          onAddUserClick = { showUserPicker = true },
          onRemoveUser = { userId -> taggedUserIds.remove(userId) })

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

      PublicSwitch(
          isPublic = isPublic,
          onPublicChange = { isPublic = it },
          "Make this memory public",
          "Others can see this memory on the event page",
          Modifier.testTag("publicSwitch"))

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

package com.swent.mapin.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swent.mapin.model.event.Event
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Composable that displays event details in the bottom sheet. Content changes based on the sheet
 * state (COLLAPSED, MEDIUM, FULL).
 *
 * @param event The event to display
 * @param sheetState Current bottom sheet state
 * @param isParticipating Whether the current user is already participating in this event
 * @param organizerName Name of the event organizer
 * @param onJoinEvent Callback when user clicks "Join event"
 * @param onUnregisterEvent Callback when user clicks "Unregister"
 * @param onSaveForLater Callback when user clicks "Save for later"
 * @param onClose Callback when user clicks the close button
 * @param onShare Callback when user clicks the share button
 */

// Assisted by AI
@Composable
fun EventDetailSheet(
    event: Event,
    sheetState: BottomSheetState,
    isParticipating: Boolean,
    organizerName: String,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    onSaveForLater: () -> Unit,
    onClose: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth().testTag("eventDetailSheet")) {
    when (sheetState) {
      BottomSheetState.COLLAPSED ->
          CollapsedEventContent(event = event, onShare = onShare, onClose = onClose)
      BottomSheetState.MEDIUM -> {
        // Header with close and share buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onShare, modifier = Modifier.testTag("shareButton")) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share event",
                    tint = MaterialTheme.colorScheme.primary)
              }

              IconButton(onClick = onClose, modifier = Modifier.testTag("closeButton")) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface)
              }
            }
        MediumEventContent(
            event = event,
            isParticipating = isParticipating,
            onJoinEvent = onJoinEvent,
            onUnregisterEvent = onUnregisterEvent)
      }
      BottomSheetState.FULL -> {
        // Header with close and share buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onShare, modifier = Modifier.testTag("shareButton")) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share event",
                    tint = MaterialTheme.colorScheme.primary)
              }

              IconButton(onClick = onClose, modifier = Modifier.testTag("closeButton")) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface)
              }
            }
        FullEventContent(
            event = event,
            isParticipating = isParticipating,
            organizerName = organizerName,
            onJoinEvent = onJoinEvent,
            onUnregisterEvent = onUnregisterEvent,
            onSaveForLater = onSaveForLater)
      }
    }
  }
}

/** Collapsed state: Shows only title and category/tag */
@Composable
private fun CollapsedEventContent(
    event: Event,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onShare, modifier = Modifier.testTag("shareButton")) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share event",
                tint = MaterialTheme.colorScheme.primary)
          }

          Column(
              modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("eventTitleCollapsed"))

                if (event.tags.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text = event.tags.first(),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.testTag("eventTagCollapsed"))
                }
              }

          IconButton(onClick = onClose, modifier = Modifier.testTag("closeButton")) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface)
          }
        }
  }
}

/** Medium state: Title, date, category, attendee count, capacity, and Join/Unregister button */
@Composable
private fun MediumEventContent(
    event: Event,
    isParticipating: Boolean,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
    // Title
    Text(
        text = event.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("eventTitleMedium"))

    Spacer(modifier = Modifier.height(8.dp))

    // Date
    event.date?.let { timestamp ->
      val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
      Text(
          text = dateFormat.format(timestamp.toDate()),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("eventDate"))
      Spacer(modifier = Modifier.height(4.dp))
    }

    // Category/Tags
    if (event.tags.isNotEmpty()) {
      Text(
          text = event.tags.joinToString(", "),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("eventTags"))
      Spacer(modifier = Modifier.height(8.dp))
    }

    // Location
    Text(
        text = "📍 ${event.location.name}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("eventLocation"))

    Spacer(modifier = Modifier.height(8.dp))

    // Description preview
    if (event.description.isNotBlank()) {
      Text(
          text = event.description,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("eventDescriptionPreview"))
      Spacer(modifier = Modifier.height(8.dp))
    }

    // Attendee count and capacity
    val attendees = event.participantIds.size
    val spotsLeft = event.capacity?.let { it - attendees }

    if (event.capacity != null && spotsLeft != null && spotsLeft > 0) {
      Column {
        Text(
            text = "$attendees attending",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("attendeeCount"))
        Text(
            text = "$spotsLeft spots left",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("capacityInfo"))
      }
    } else {
      Text(
          text = "$attendees attending",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.testTag("attendeeCount"))
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Join/Unregister button
    if (isParticipating) {
      OutlinedButton(
          onClick = onUnregisterEvent,
          modifier = Modifier.fillMaxWidth().testTag("unregisterButton"),
          colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text("Unregister")
          }
    } else {
      val isFull = event.capacity?.let { attendees >= it } ?: false
      Button(
          onClick = onJoinEvent,
          modifier = Modifier.fillMaxWidth().testTag("joinEventButton"),
          enabled = !isFull) {
            Text(if (isFull) "Event is full" else "Join Event")
          }
    }
  }
}

/**
 * Full state: All medium content + description, organizer name, optional image, and "Save for
 * later" button
 */
@Composable
private fun FullEventContent(
    event: Event,
    isParticipating: Boolean,
    organizerName: String,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    onSaveForLater: () -> Unit,
    modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val attendees = event.participantIds.size
  val spotsLeft = event.capacity?.let { it - attendees }

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .verticalScroll(scrollState)
              .padding(horizontal = 16.dp)
              .padding(bottom = 16.dp)) {
        // Event image (with placeholder if no image)
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp).testTag("eventImage"),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (event.imageUrl == null) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface)) {
              if (event.imageUrl != null) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = "Event image",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop)
              } else {
                // Placeholder when no image
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                      text = "No image available",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("eventTitleFull"))

        Spacer(modifier = Modifier.height(8.dp))

        // Date
        event.date?.let { timestamp ->
          val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
          Text(
              text = dateFormat.format(timestamp.toDate()),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("eventDateFull"))
          Spacer(modifier = Modifier.height(4.dp))
        }

        // Category/Tags
        if (event.tags.isNotEmpty()) {
          Text(
              text = event.tags.joinToString(" • "),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.testTag("eventTagsFull"))
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Organizer
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "Organized by: ",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
              text = organizerName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.testTag("organizerName"))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Location
        Text(
            text = "📍 ${event.location.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("eventLocationFull"))

        Spacer(modifier = Modifier.height(8.dp))

        // Attendee count and capacity
        if (event.capacity != null && spotsLeft != null && spotsLeft > 0) {
          Column {
            Text(
                text = "$attendees attending",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("attendeeCountFull"))
            Text(
                text = "$spotsLeft spots left",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("capacityInfoFull"))
          }
        } else {
          Text(
              text = "$attendees attending",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.testTag("attendeeCountFull"))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        if (event.description.isNotBlank()) {
          Text(
              text = "Description",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = event.description,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.testTag("eventDescription"))
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Action buttons
        if (isParticipating) {
          OutlinedButton(
              onClick = onUnregisterEvent,
              modifier = Modifier.fillMaxWidth().testTag("unregisterButtonFull"),
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error)) {
                Text("Unregister")
              }
        } else {
          val isFull = event.capacity?.let { attendees >= it } ?: false
          Button(
              onClick = onJoinEvent,
              modifier = Modifier.fillMaxWidth().testTag("joinEventButtonFull"),
              enabled = !isFull) {
                Text(if (isFull) "Event is full" else "Join Event")
              }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save for later button
        OutlinedButton(
            onClick = onSaveForLater,
            modifier = Modifier.fillMaxWidth().testTag("saveForLaterButton")) {
              Text("Save for Later")
            }
      }
}

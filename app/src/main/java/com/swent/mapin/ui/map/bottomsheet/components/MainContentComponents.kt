package com.swent.mapin.ui.map.bottomsheet.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.event.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Creates a button to create a new event. */
@Composable
fun CreateEventSection(modifier: Modifier = Modifier, onCreateEventClick: () -> Unit) {
  Button(
      onClick = onCreateEventClick,
      modifier = modifier.height(56.dp),
      shape = RoundedCornerShape(16.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(text = "Create Event", style = MaterialTheme.typography.labelLarge, maxLines = 1)

              Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = null)
            }
      }
}

@Composable
fun EventsSection(
    events: List<Event>,
    onEventClick: (Event) -> Unit = {},
    onAddMemoryClick: ((Event) -> Unit)? = null,
    onEditEvent: ((Event) -> Unit)? = null,
    onDeleteEvent: ((Event) -> Unit)? = null
) {

  var expanded by remember { mutableStateOf(false) }
  val visible = if (expanded) events else events.take(3)

  Column(modifier = Modifier.fillMaxWidth()) {
    visible.forEach { event ->
      EventRow(
          event = event,
          modifier = Modifier.padding(horizontal = 0.dp),
          onRowClick = { onEventClick(event) },
          onAddMemoryClick = onAddMemoryClick,
          onEditEvent = onEditEvent,
          onDeleteEvent = onDeleteEvent)
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (events.size > 3) {
      Spacer(modifier = Modifier.height(4.dp))
      TextButton(
          onClick = { expanded = !expanded },
          modifier =
              Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("eventsShowMoreButton")) {
            Text(if (expanded) "Show less" else "Show more (${events.size - 3} more)")
          }
    }
  }
}

/** Attended Events section consuming a pre-filtered list of attended events (newest first). */
@Composable
fun AttendedEventsSection(
    attendedEvents: List<Event>,
    onEventClick: (Event) -> Unit,
    // Accepts the selected event to prefill memory creation (or null)
    onCreateMemoryClick: (Event) -> Unit
) {
  if (attendedEvents.isEmpty()) {
    NoEventsMessage(
        title = "No past events yet.", subtitle = "Once you attend events, they’ll appear here.")
  } else {
    EventsSection(attendedEvents, onEventClick, onCreateMemoryClick)
  }
}

@Composable
fun SavedEventsSection(savedEvents: List<Event>, onEventClick: (Event) -> Unit) {
  if (savedEvents.isEmpty()) {
    NoEventsMessage(
        title = "No saved events yet.",
        subtitle = "Start exploring and save the ones you don’t want to miss.")
  } else {
    EventsSection(savedEvents, onEventClick)
  }
}

@Composable
fun UpcomingEventsSection(upcomingEvents: List<Event>, onEventClick: (Event) -> Unit) {
  if (upcomingEvents.isEmpty()) {
    NoEventsMessage(
        title = "No upcoming events yet.",
        subtitle = "Find something interesting and join to see it here.")
  } else {
    EventsSection(upcomingEvents, onEventClick)
  }
}

/**
 * A single row representing an event with title, formatted date, location and a '+' button to
 * create a memory quickly or an edit/delete button.
 *
 * @param event The event to display
 * @param modifier Modifier to apply to the row
 * @param onRowClick Callback invoked when the row is clicked
 * @param onAddMemoryClick Callback invoked when the '+' button is clicked
 * @param onEditEvent Callback invoked when the edit button is clicked
 * @param onDeleteEvent Callback invoked when the delete button is clicked
 */
@Composable
fun EventRow(
    event: Event,
    modifier: Modifier = Modifier,
    onRowClick: (Event) -> Unit,
    onAddMemoryClick: ((Event) -> Unit)? = null,
    onEditEvent: ((Event) -> Unit)? = null,
    onDeleteEvent: ((Event) -> Unit)? = null
) {
  var expanded by remember { mutableStateOf(false) }

  // Simple date formatter for the endDate (local zone)
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault())
  val dateText =
      try {
        event.date?.toDate()?.time?.let { millis -> formatter.format(Instant.ofEpochMilli(millis)) }
            ?: ""
      } catch (_: Exception) {
        ""
      }

  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { onRowClick(event) }
              .padding(vertical = 12.dp, horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = event.title,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = dateText, style = MaterialTheme.typography.bodySmall)
          Spacer(modifier = Modifier.height(2.dp))
          if (event.location.name.isNotBlank()) {
            Text(
                text = event.location.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
          }
        }

        Spacer(modifier = Modifier.width(8.dp))
        if (onAddMemoryClick != null) {
          IconButton(
              onClick = { onAddMemoryClick(event) },
              modifier = Modifier.testTag("event_add_memory_${event.uid}"),
              colors =
                  IconButtonDefaults.iconButtonColors(
                      contentColor = MaterialTheme.colorScheme.primary)) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
              }
        }

        if (onEditEvent != null && onDeleteEvent != null) {
          Box(
              modifier =
                  Modifier.size(32.dp)
                      .clickable(
                          indication = null,
                          interactionSource = remember { MutableInteractionSource() }) {
                            expanded = true
                          }
                      .testTag("eventOptionsIcon_${event.uid}"),
              contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.testTag("eventOptionsMenu_${event.uid}")) {
                      DropdownMenuItem(
                          text = { Text("Edit") },
                          onClick = {
                            onEditEvent(event)
                            expanded = false
                          })
                      HorizontalDivider()
                      DropdownMenuItem(
                          text = { Text("Delete") },
                          onClick = {
                            onDeleteEvent(event)
                            expanded = false
                          })
                    }
              }
        }
      }
}
/**
 * Displays a section for owned events with proper state handling.
 *
 * This composable handles three main states:
 * - Loading: Shows a progress indicator while fetching events
 * - Error: Displays error message with retry button
 * - Success: Shows list of events or empty state message
 *
 * The event list reuses EventsSection for consistent UI with other tabs. Each event item is
 * clickable and displays key details (name, date, location).
 *
 * @param events List of events owned by the current user
 * @param loading Whether events are currently being loaded
 * @param error Error message to display, or null if no error
 * @param onEventClick Callback invoked when user taps an event item
 * @param onRetry Callback invoked when user taps the retry button in error state
 * @see EventsSection for the list rendering implementation
 * @see SearchResultItem for individual event item display
 */
@Composable
fun OwnedEventsSection(
    events: List<Event>,
    loading: Boolean,
    error: String?,
    onEventClick: (Event) -> Unit,
    onEditEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onRetry: () -> Unit
) {
  when {
    loading -> {
      Column(
          modifier =
              Modifier.fillMaxWidth().padding(16.dp).semantics {
                contentDescription = "Loading owned events"
              },
          verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(
                modifier =
                    Modifier.semantics {
                      contentDescription = "Loading indicator for owned events"
                    })
          }
    }
    error != null -> {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Error: $error",
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier.semantics { contentDescription = "Error loading owned events: $error" })
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            modifier =
                Modifier.fillMaxWidth().semantics {
                  contentDescription = "Retry loading owned events"
                }) {
              Text("Retry")
            }
      }
    }
    events.isEmpty() -> {
      NoEventsMessage(
          title = "You haven’t created any events yet.",
          subtitle = "Organize your first event and it will show up here.")
    }
    else -> {
      // Reuse EventsSection behaviour for listing
      Column(
          modifier =
              Modifier.semantics { contentDescription = "List of ${events.size} owned events" }) {
            EventsSection(
                events = events,
                onEventClick = onEventClick,
                onEditEvent = onEditEvent,
                onDeleteEvent = onDeleteEvent)
          }
    }
  }
}

@Composable
fun NoEventsMessage(title: String, subtitle: String) {
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center)
    }
  }
}

@Composable
fun MenuListItem(icon: ImageVector, label: String, onClick: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp))

        Spacer(Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp).testTag("menuItemArrow"))
      }
}

@Composable
fun MenuDivider() {
  HorizontalDivider(
      modifier = Modifier.fillMaxWidth().padding(start = 40.dp), // aligns with text
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

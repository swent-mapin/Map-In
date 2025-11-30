package com.swent.mapin.ui.map.bottomsheet.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.event.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Row of quick action buttons (Create Memory, Create Event, Friends). */
@Composable
fun QuickActionsSection(modifier: Modifier = Modifier, onCreateEventClick: () -> Unit) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      QuickActionButton(
          text = "Create Event", modifier = Modifier.weight(1f), onClick = onCreateEventClick)
      // TODO : Add show memories button that switches to a map with past events and their memories
      QuickActionButton(text = "Show Memories", modifier = Modifier.weight(1f), onClick = {})
    }
  }
}

/** button for quick actions - modern, minimalist with consistent height */
@Composable
private fun QuickActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      modifier = modifier.height(56.dp),
      shape = RoundedCornerShape(16.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary)) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = MaterialTheme.typography.labelLarge)
      }
}

@Composable
fun EventsSection(
    events: List<Event>,
    onEventClick: (Event) -> Unit = {},
    onEditEvent: ((Event) -> Unit)? = null,
    onDeleteEvent: ((Event) -> Unit)? = null
) {
  if (events.isEmpty()) {
    NoResultsMessage(query = "", modifier = Modifier)
    return
  }
  val invertedEvents = events.reversed()
  var expanded by remember { mutableStateOf(false) }
  val visible = if (expanded) invertedEvents else invertedEvents.take(3)

  Column(modifier = Modifier.fillMaxWidth()) {
    visible.forEach { event ->
      SearchResultItem(
          event = event,
          modifier = Modifier.padding(horizontal = 16.dp),
          onClick = { onEventClick(event) },
          onEditEvent = onEditEvent,
          onDeleteEvent = onDeleteEvent) // To be changed with deleteEvent backend
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
  var expanded by remember { mutableStateOf(false) }
  val visible = if (expanded) attendedEvents else attendedEvents.take(3)

  Text(
      text = "Attended Events",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(bottom = 8.dp))

  if (attendedEvents.isEmpty()) {
    Text(
        text = "No attended events yet",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp))
  } else {
    Column(modifier = Modifier.fillMaxWidth()) {
      visible.forEach { event ->
        EventRow(
            event = event,
            modifier = Modifier.padding(horizontal = 0.dp),
            onRowClick = { onEventClick(it) },
            onAddMemoryClick = {
              // trigger memory creation for this event
              onCreateMemoryClick(it)
            })
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
    if (attendedEvents.size > 3) {
      Spacer(modifier = Modifier.height(4.dp))
      TextButton(
          onClick = { expanded = !expanded },
          modifier =
              Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("eventsShowMoreButton")) {
            Text(if (expanded) "Show less" else "Show more (${attendedEvents.size - 3} more)")
          }
    }
  }
}

/**
 * A single row representing an event with title, formatted date, location and a '+' button to
 * create a memory quickly.
 */
@Composable
fun EventRow(
    event: Event,
    modifier: Modifier = Modifier,
    onRowClick: (Event) -> Unit,
    onAddMemoryClick: (Event) -> Unit
) {
  // Simple date formatter for the endDate (local zone)
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault())
  val dateText =
      try {
        event.endDate?.toDate()?.time?.let { millis ->
          formatter.format(Instant.ofEpochMilli(millis))
        } ?: ""
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
          Text(text = event.title, style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = dateText, style = MaterialTheme.typography.bodySmall)
          Spacer(modifier = Modifier.height(2.dp))
          Text(text = event.location.name, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = { onAddMemoryClick(event) },
            modifier = Modifier.testTag("event_add_memory_${event.uid}"),
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary)) {
              Text(text = "+", style = MaterialTheme.typography.titleLarge)
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
      NoResultsMessage(
          query = "You have not created any events yet.",
          modifier =
              Modifier.semantics {
                contentDescription = "No owned events. You have not created any events yet."
              })
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

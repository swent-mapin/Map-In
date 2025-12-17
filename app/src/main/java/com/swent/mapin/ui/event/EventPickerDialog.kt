package com.swent.mapin.ui.event

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import java.text.SimpleDateFormat
import java.util.Locale

// Assisted by AI

private val EVENT_DIALOG_LIST_HEIGHT = 300.dp

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

  val filteredEvents = remember(events, searchQuery) { filterEvents(events, searchQuery) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Select an event", style = MaterialTheme.typography.titleLarge) },
      text = {
        EventPickerContent(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            filteredEvents = filteredEvents,
            onEventSelected = onEventSelected)
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun EventPickerContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredEvents: List<Event>,
    onEventSelected: (Event) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    EventSearchField(searchQuery, onSearchQueryChange)
    Spacer(modifier = Modifier.height(16.dp))
    EventPickerList(filteredEvents, searchQuery, onEventSelected)
  }
}

@Composable
private fun EventPickerList(
    events: List<Event>,
    searchQuery: String,
    onEventSelected: (Event) -> Unit
) {
  if (events.isEmpty()) {
    EmptyEventState(searchQuery)
    return
  }

  LazyColumn(modifier = Modifier.fillMaxWidth().height(EVENT_DIALOG_LIST_HEIGHT)) {
    items(events) { event -> EventListItem(event, onEventSelected) }
  }
}

@Composable
private fun EventListItem(event: Event, onEventSelected: (Event) -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEventSelected(event) },
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text(
                    text = event.date?.let { formatEventDate(it) } ?: "No date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = event.location.name ?: Location.NO_NAME,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
        }
      }
}

@Composable
private fun EventSearchField(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
  OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Search events...") },
      leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
      trailingIcon = { ClearSearchIcon(searchQuery, onSearchQueryChange) },
      singleLine = true)
}

@Composable
private fun ClearSearchIcon(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
  if (searchQuery.isEmpty()) return
  IconButton(onClick = { onSearchQueryChange("") }) {
    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
  }
}

@Composable
private fun EmptyEventState(searchQuery: String) {
  val message = if (searchQuery.isBlank()) "No events available" else "No events found"
  Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@VisibleForTesting
internal fun filterEvents(events: List<Event>, query: String): List<Event> {
  if (query.isBlank()) return events
  return events.filter {
    it.title.contains(query, ignoreCase = true) ||
        it.location.name?.contains(query, ignoreCase = true) ?: false ||
        it.description.contains(query, ignoreCase = true)
  }
}

@VisibleForTesting
internal fun formatEventDate(timestamp: com.google.firebase.Timestamp): String {
  return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timestamp.toDate())
}

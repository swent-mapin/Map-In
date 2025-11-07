package com.swent.mapin.ui.map.dialogs

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
            LazyColumn(modifier = Modifier.fillMaxWidth().height(EVENT_DIALOG_LIST_HEIGHT)) {
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

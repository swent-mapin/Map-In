package com.swent.mapin.ui.map.bottomsheet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.event.Event

/** Row of quick action buttons (Create Memory, Create Event, Friends). */
@Composable
fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onCreateMemoryClick: () -> Unit,
    onCreateEventClick: () -> Unit,
    onNavigateToFriends: () -> Unit
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      QuickActionButton(
          text = "Create Memory", modifier = Modifier.weight(1f), onClick = onCreateMemoryClick)
      QuickActionButton(
          text = "Create Event", modifier = Modifier.weight(1f), onClick = onCreateEventClick)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      QuickActionButton(
          text = "Friends", modifier = Modifier.weight(1f), onClick = onNavigateToFriends)
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
fun EventsSection(events: List<Event>, onEventClick: (Event) -> Unit) {
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
          onClick = { onEventClick(event) })
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

@Composable
fun NoActivitiesMessage(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = "No recent events.", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text =
              "Your recent activity will appear here once you interact with events or create memories.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center)
    }
  }
}

package com.swent.mapin.ui.event

import android.R.attr.timeZone
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.Directions
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.map.BottomSheetState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
 * @param onGetDirections Callback when user clicks "Get Directions"
 * @param showDirections Whether directions are currently being shown
 */

// Assisted by AI
@Composable
fun EventDetailSheet(
    event: Event,
    sheetState: BottomSheetState,
    isParticipating: Boolean,
    isSaved: Boolean,
    organizerName: String,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    onSaveForLater: () -> Unit,
    onUnsaveForLater: () -> Unit,
    onClose: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onGetDirections: () -> Unit = {},
    showDirections: Boolean = false
) {
  Column(modifier = modifier.fillMaxWidth().testTag("eventDetailSheet")) {
    when (sheetState) {
      BottomSheetState.COLLAPSED ->
          CollapsedEventContent(event = event, onShare = onShare, onClose = onClose)
      BottomSheetState.MEDIUM -> {
        EventDetailHeader(onShare = onShare, onClose = onClose)
        MediumEventContent(
            event = event,
            isParticipating = isParticipating,
            onJoinEvent = onJoinEvent,
            onUnregisterEvent = onUnregisterEvent,
            onGetDirections = onGetDirections,
            showDirections = showDirections)
      }
      BottomSheetState.FULL -> {
        EventDetailHeader(onShare = onShare, onClose = onClose)
        FullEventContent(
            event = event,
            isParticipating = isParticipating,
            isSaved = isSaved,
            organizerName = organizerName,
            onJoinEvent = onJoinEvent,
            onUnregisterEvent = onUnregisterEvent,
            onSaveForLater = onSaveForLater,
            onUnsaveForLater = onUnsaveForLater,
            onGetDirections = onGetDirections,
            showDirections = showDirections)
      }
    }
  }
}

/** Header with share and close buttons used in MEDIUM and FULL states */
@Composable
private fun EventDetailHeader(onShare: () -> Unit, onClose: () -> Unit) {
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
}

/** Collapsed state: Shows only title and category/tag */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollapsedEventContent(
    event: Event,
    modifier: Modifier = Modifier,
    onShare: () -> Unit,
    onClose: () -> Unit
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
                    modifier = Modifier.basicMarquee().testTag("eventTitleCollapsed"))

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
    modifier: Modifier = Modifier,
    onGetDirections: () -> Unit = {},
    showDirections: Boolean = false
) {
  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
    // Title with direction icon
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = event.title,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f).testTag("eventTitleMedium"))

          // Direction icon button
          IconButton(
              onClick = onGetDirections,
              modifier =
                  Modifier.testTag(com.swent.mapin.testing.UiTestTags.GET_DIRECTIONS_BUTTON)) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription =
                        if (showDirections) "Clear Directions" else "Get Directions",
                    tint =
                        if (showDirections) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary)
              }
        }

    Spacer(modifier = Modifier.height(8.dp))

    // Date (smart range)
    event.date?.let { startTimestamp ->
      val dateText = formatEventDateRangeMedium(startTimestamp, event.endDate)
      Text(
          text = dateText,
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
    AttendeeInfo(event = event, testTagSuffix = "")

    Spacer(modifier = Modifier.height(16.dp))

    val joinButtonUi =
        remember(event.participantIds, event.capacity, isParticipating) {
          resolveJoinButtonUi(event, isParticipating)
        }

    if (!joinButtonUi.showJoinButton) {
      OutlinedButton(
          onClick = onUnregisterEvent,
          modifier = Modifier.fillMaxWidth().testTag("unregisterButton"),
          colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text("Unregister")
          }
    } else {
      Button(
          onClick = onJoinEvent,
          modifier = Modifier.fillMaxWidth().testTag("joinEventButton"),
          enabled = joinButtonUi.enabled) {
            Text(joinButtonUi.label)
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
    isSaved: Boolean,
    organizerName: String,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    onSaveForLater: () -> Unit,
    onUnsaveForLater: () -> Unit,
    modifier: Modifier = Modifier,
    onGetDirections: () -> Unit = {},
    showDirections: Boolean = false
) {
  val scrollState = rememberScrollState()

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

        // Title with direction icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = event.title,
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f).testTag("eventTitleFull"))

              // Direction icon button
              IconButton(
                  onClick = onGetDirections,
                  modifier =
                      Modifier.testTag(com.swent.mapin.testing.UiTestTags.GET_DIRECTIONS_BUTTON)) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription =
                            if (showDirections) "Clear Directions" else "Get Directions",
                        tint =
                            if (showDirections) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary)
                  }
            }

        Spacer(modifier = Modifier.height(8.dp))

        // Date (smart range, full)
        event.date?.let { startTimestamp ->
          val dateText = formatEventDateRangeFull(startTimestamp, event.endDate)
          Text(
              text = dateText,
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
        AttendeeInfo(event = event, testTagSuffix = "Full")

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

        val joinButtonUi =
            remember(event.participantIds, event.capacity, isParticipating) {
              resolveJoinButtonUi(event, isParticipating)
            }

        if (!joinButtonUi.showJoinButton) {
          OutlinedButton(
              onClick = onUnregisterEvent,
              modifier = Modifier.fillMaxWidth().testTag("unregisterButtonFull"),
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error)) {
                Text("Unregister")
              }
        } else {
          Button(
              onClick = onJoinEvent,
              modifier = Modifier.fillMaxWidth().testTag("joinEventButtonFull"),
              enabled = joinButtonUi.enabled) {
                Text(joinButtonUi.label)
              }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val saveButtonUi = remember(isSaved) { resolveSaveButtonUi(isSaved) }

        if (!saveButtonUi.showSaveButton) {
          OutlinedButton(
              onClick = onUnsaveForLater,
              modifier = Modifier.fillMaxWidth().testTag("unsaveButtonFull"),
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error)) {
                Text(saveButtonUi.label)
              }
        } else {
          Button(
              onClick = onSaveForLater,
              modifier = Modifier.fillMaxWidth().testTag("saveButtonFull")) {
                Text(saveButtonUi.label)
              }
        }
      }
}

/**
 * Data class representing the state of the save button.
 *
 * @param showSaveButton Whether the save button should be shown
 * @param label The label to display on the button
 */
@VisibleForTesting internal data class SaveButtonUi(val showSaveButton: Boolean, val label: String)

/**
 * Resolves the UI state for the save button based on whether the event is already saved.
 *
 * @param isSaved Whether the event is already saved
 * @return The UI state for the save button
 */
@VisibleForTesting
internal fun resolveSaveButtonUi(isSaved: Boolean): SaveButtonUi {
  return if (isSaved) {
    SaveButtonUi(showSaveButton = false, label = "Unsave")
  } else {
    SaveButtonUi(showSaveButton = true, label = "Save for later")
  }
}

@VisibleForTesting
internal data class JoinButtonUi(
    val showJoinButton: Boolean,
    val label: String,
    val enabled: Boolean
)

@VisibleForTesting
internal fun resolveJoinButtonUi(event: Event, isParticipating: Boolean): JoinButtonUi {
  if (isParticipating) {
    return JoinButtonUi(showJoinButton = false, label = "", enabled = false)
  }

  val isFull = event.capacity?.let { capacity -> capacity <= event.participantIds.size } ?: false
  val label = if (isFull) "Event is full" else "Join Event"
  return JoinButtonUi(showJoinButton = true, label = label, enabled = !isFull)
}

@VisibleForTesting
internal data class AttendeeInfoUi(val attendeeText: String, val capacityText: String?)

@VisibleForTesting
internal fun buildAttendeeInfoUi(event: Event): AttendeeInfoUi {
  val attendees = event.participantIds.size
  val spotsLeft = event.capacity?.let { capacity -> capacity - attendees }
  val attendeeText = "$attendees attending"
  val capacityText = spotsLeft?.takeIf { it > 0 }?.let { "$it spots left" }
  return AttendeeInfoUi(attendeeText = attendeeText, capacityText = capacityText)
}

/** Reusable attendee count and capacity display */
@Composable
private fun AttendeeInfo(event: Event, testTagSuffix: String) {
  val attendeeInfo = remember(event.participantIds, event.capacity) { buildAttendeeInfoUi(event) }

  Column {
    Text(
        text = attendeeInfo.attendeeText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("attendeeCount$testTagSuffix"))

    attendeeInfo.capacityText?.let {
      Text(
          text = it,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.testTag("capacityInfo$testTagSuffix"))
    }
  }
}

/** Format helpers for smart start-end date/time display. */
@VisibleForTesting
internal fun formatEventDateRangeMedium(start: Timestamp, end: Timestamp?): String {
  val zone = TimeZone.getDefault()
  val locale = Locale.getDefault()

  val startDate = start.toDate()
  // Treat end equal to start as no end
  val endDateRaw = end?.toDate()
  val endDate = if (endDateRaw != null && endDateRaw.time == startDate.time) null else endDateRaw

  val calStart = Calendar.getInstance(zone, locale).apply { time = startDate }
  // Determine if we should show year for single events
  val currentYear = Calendar.getInstance(zone, locale).get(Calendar.YEAR)
  val startYear = calStart.get(Calendar.YEAR)
  val showYearSingle = startYear != currentYear

  // For ranges, when endDate is non-null create a calEnd and compare
  val sameYearRange: Boolean
  val sameDayRange: Boolean
  if (endDate != null) {
    val calEndLocal = Calendar.getInstance(zone, locale).apply { time = endDate }
    sameYearRange = calStart.get(Calendar.YEAR) == calEndLocal.get(Calendar.YEAR)
    sameDayRange =
        sameYearRange && calStart.get(Calendar.DAY_OF_YEAR) == calEndLocal.get(Calendar.DAY_OF_YEAR)
  } else {
    sameYearRange = false
    sameDayRange = false
  }

  val dateFmtNoYear = SimpleDateFormat("MMM d", locale).apply { timeZone = zone }
  val dateFmtWithYear = SimpleDateFormat("MMM d, yyyy", locale).apply { timeZone = zone }

  // 24-hour format always showing minutes (e.g., 13:00 or 13:30)
  fun timeShort(date: Date, tz: TimeZone = TimeZone.getDefault()): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.ROOT).apply { timeZone = tz }
    return sdf.format(date)
  }

  return if (endDate == null) {
    // Single time
    val dateStr =
        if (showYearSingle) dateFmtWithYear.format(startDate) else dateFmtNoYear.format(startDate)
    "$dateStr, ${timeShort(startDate)}"
  } else {
    // Range
    if (sameDayRange) {
      val dateStr = dateFmtNoYear.format(startDate) // same day -> no year needed
      "$dateStr, ${timeShort(startDate)} - ${timeShort(endDate)}"
    } else {
      // determine if start/end are same year
      val startStr =
          if (sameYearRange) dateFmtNoYear.format(startDate) else dateFmtWithYear.format(startDate)
      val endStr =
          if (sameYearRange) dateFmtNoYear.format(endDate) else dateFmtWithYear.format(endDate)
      "$startStr, ${timeShort(startDate)} - $endStr, ${timeShort(endDate)}"
    }
  }
}

@VisibleForTesting
internal fun formatEventDateRangeFull(start: Timestamp, end: Timestamp?): String {
  val startDate = start.toDate()
  // Treat end equal to start as no end
  val endDateRaw = end?.toDate()
  val endDate = if (endDateRaw != null && endDateRaw.time == startDate.time) null else endDateRaw

  val calStart = Calendar.getInstance().apply { time = startDate }
  // For ranges, when endDate is non-null create a calEnd and compare
  val calEnd = endDate?.let { Calendar.getInstance().apply { time = it } }

  // For ranges, only compare year/day if calEnd is provided
  val sameYearRange = calEnd != null && calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)
  val sameDayRange =
      calEnd != null &&
          sameYearRange &&
          calStart.get(Calendar.DAY_OF_YEAR) == calEnd.get(Calendar.DAY_OF_YEAR)

  val dateFullFmt = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())

  // 24-hour format always showing minutes
  fun timeFull(d: Date): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
  }

  return if (endDate == null) {
    // Single time, include weekday and year
    "${dateFullFmt.format(startDate)} at ${timeFull(startDate)}"
  } else {
    if (sameDayRange) {
      "${dateFullFmt.format(startDate)} at ${timeFull(startDate)} - ${timeFull(endDate)}"
    } else {
      val startStr = dateFullFmt.format(startDate)
      val endStr = dateFullFmt.format(endDate)
      "$startStr at ${timeFull(startDate)} - $endStr at ${timeFull(endDate)}"
    }
  }
}

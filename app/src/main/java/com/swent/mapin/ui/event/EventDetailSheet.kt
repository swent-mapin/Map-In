package com.swent.mapin.ui.event

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import com.swent.mapin.ui.map.OrganizerState
import com.swent.mapin.ui.memory.MediaItem
import com.swent.mapin.ui.memory.MemoryVideoPlayer
import com.swent.mapin.ui.memory.parseMediaItems
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
 * @param isSaved Whether the event is currently saved
 * @param organizerState State wrapper with organizer loading/name/error information
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
    organizerState: OrganizerState,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    onSaveForLater: () -> Unit,
    onUnsaveForLater: () -> Unit,
    onClose: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onGetDirections: () -> Unit = {},
    showDirections: Boolean = false,
    hasLocationPermission: Boolean = false,
    onOrganizerClick: (String) -> Unit = {}
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
            showDirections = showDirections,
            hasLocationPermission = hasLocationPermission)
      }
      BottomSheetState.FULL -> {
        EventDetailHeader(onShare = onShare, onClose = onClose)
        FullEventContent(
            event = event,
            isParticipating = isParticipating,
            isSaved = isSaved,
            organizerState = organizerState,
            onJoinEvent = onJoinEvent,
            onUnregisterEvent = onUnregisterEvent,
            onSaveForLater = onSaveForLater,
            onUnsaveForLater = onUnsaveForLater,
            onGetDirections = onGetDirections,
            showDirections = showDirections,
            hasLocationPermission = hasLocationPermission,
            onOrganizerClick = onOrganizerClick)
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

/** Title with direction icon button - shared by MEDIUM and FULL states */
@Composable
private fun EventTitleWithDirections(
    title: String,
    titleStyle: androidx.compose.ui.text.TextStyle,
    titleTestTag: String,
    onGetDirections: () -> Unit,
    showDirections: Boolean,
    hasLocationPermission: Boolean
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = titleStyle,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).testTag(titleTestTag))

        // Direction icon button
        IconButton(
            onClick = onGetDirections,
            enabled = hasLocationPermission || showDirections,
            modifier = Modifier.testTag(com.swent.mapin.testing.UiTestTags.GET_DIRECTIONS_BUTTON)) {
              Icon(
                  imageVector = Icons.Default.Directions,
                  contentDescription =
                      if (showDirections) "Clear Directions"
                      else if (hasLocationPermission) "Get Directions"
                      else "Get Directions (Location permission required)",
                  tint =
                      if (showDirections) MaterialTheme.colorScheme.error
                      else if (hasLocationPermission) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
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
    showDirections: Boolean = false,
    hasLocationPermission: Boolean = false
) {
  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
    // Title with direction icon
    EventTitleWithDirections(
        title = event.title,
        titleStyle = MaterialTheme.typography.headlineSmall,
        titleTestTag = "eventTitleMedium",
        onGetDirections = onGetDirections,
        showDirections = showDirections,
        hasLocationPermission = hasLocationPermission)

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

/** Reusable attendee count and capacity display */
@Composable
private fun AttendeeInfo(event: Event, testTagSuffix: String) {
  val info = remember(event.participantIds, event.capacity) { buildAttendeeInfoUi(event) }
  Column {
    Text(
        info.attendeeText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("attendeeCount$testTagSuffix"))
    info.capacityText?.let {
      Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.testTag("capacityInfo$testTagSuffix"))
    }
  }
}

/** Event image card with placeholder */
@Composable
private fun EventImageCard(imageUrl: String?) {
  Card(
      modifier = Modifier.fillMaxWidth().height(200.dp).testTag("eventImage"),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (imageUrl == null) MaterialTheme.colorScheme.surfaceVariant
                  else MaterialTheme.colorScheme.surface)) {
        if (imageUrl == null) {
          NoImageBox()
        } else {
          val media = parseMediaItems(listOf(imageUrl))
          when (val mediaUrl = media.firstOrNull()) {
            is MediaItem.Image -> {
              AsyncImage(
                  model = mediaUrl.url,
                  contentDescription = "Event image",
                  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                  contentScale = ContentScale.Crop)
            }
            is MediaItem.Video -> {
              Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                MemoryVideoPlayer(mediaUrl.url)
              }
            }
            else -> {
              NoImageBox()
            }
          }
        }
      }
}

/** No image available text */
@Composable
private fun NoImageBox() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
        "No image available",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** Organizer name text based on state */
@Composable
private fun OrganizerRow(organizerState: OrganizerState, onOrganizerClick: (String) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
        "Organized by: ",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    val (text, color, alpha) =
        when (organizerState) {
          is OrganizerState.Loading ->
              Triple("Loading...", MaterialTheme.colorScheme.onSurfaceVariant, 0.6f)
          is OrganizerState.Loaded ->
              Triple(organizerState.name, MaterialTheme.colorScheme.primary, 1f)
          is OrganizerState.Error -> Triple("Unknown", MaterialTheme.colorScheme.error, 1f)
        }
    val clickMod =
        if (organizerState is OrganizerState.Loaded && organizerState.userId.isNotEmpty())
            Modifier.clickable { onOrganizerClick(organizerState.userId) }
        else Modifier
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = color.copy(alpha = alpha),
        modifier = Modifier.testTag("organizerName").then(clickMod))
  }
}

/** Join/Unregister button section */
@Composable
private fun JoinButtonSection(
    joinButtonUi: JoinButtonUi,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    testTagSuffix: String = ""
) {
  if (!joinButtonUi.showJoinButton)
      OutlinedButton(
          onClick = onUnregisterEvent,
          modifier = Modifier.fillMaxWidth().testTag("unregisterButton$testTagSuffix"),
          colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text("Unregister")
          }
  else
      Button(
          onClick = onJoinEvent,
          modifier = Modifier.fillMaxWidth().testTag("joinEventButton$testTagSuffix"),
          enabled = joinButtonUi.enabled) {
            Text(joinButtonUi.label)
          }
}

/** Save/Unsave button section */
@Composable
private fun SaveButtonSection(
    saveButtonUi: SaveButtonUi,
    onSaveForLater: () -> Unit,
    onUnsaveForLater: () -> Unit
) {
  if (!saveButtonUi.showSaveButton)
      OutlinedButton(
          onClick = onUnsaveForLater,
          modifier = Modifier.fillMaxWidth().testTag("unsaveButtonFull"),
          colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text(saveButtonUi.label)
          }
  else
      Button(
          onClick = onSaveForLater, modifier = Modifier.fillMaxWidth().testTag("saveButtonFull")) {
            Text(saveButtonUi.label)
          }
}

/** Description section */
@Composable
private fun DescriptionSection(description: String) {
  if (description.isNotBlank()) {
    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        description,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("eventDescription"))
    Spacer(modifier = Modifier.height(16.dp))
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
    organizerState: OrganizerState,
    onJoinEvent: () -> Unit,
    onUnregisterEvent: () -> Unit,
    onSaveForLater: () -> Unit,
    onUnsaveForLater: () -> Unit,
    modifier: Modifier = Modifier,
    onGetDirections: () -> Unit = {},
    showDirections: Boolean = false,
    hasLocationPermission: Boolean = false,
    onOrganizerClick: (String) -> Unit = {}
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
        EventImageCard(imageUrl = event.imageUrl)
        Spacer(modifier = Modifier.height(16.dp))

        // Title with direction icon
        EventTitleWithDirections(
            title = event.title,
            titleStyle = MaterialTheme.typography.headlineMedium,
            titleTestTag = "eventTitleFull",
            onGetDirections = onGetDirections,
            showDirections = showDirections,
            hasLocationPermission = hasLocationPermission)

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
        OrganizerRow(organizerState = organizerState, onOrganizerClick = onOrganizerClick)

        Spacer(modifier = Modifier.height(8.dp))

        // Location
        Text(
            text = "📍 ${event.location.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("eventLocationFull"))

        // Price section
        if (event.price > 0.0) {
          Spacer(modifier = Modifier.height(12.dp))
          PriceSection(price = event.price)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Attendee count and capacity
        AttendeeInfo(event = event, testTagSuffix = "Full")

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        DescriptionSection(description = event.description)

        val joinButtonUi =
            remember(event.participantIds, event.capacity, isParticipating) {
              resolveJoinButtonUi(event, isParticipating)
            }

        JoinButtonSection(
            joinButtonUi = joinButtonUi,
            onJoinEvent = onJoinEvent,
            onUnregisterEvent = onUnregisterEvent,
            testTagSuffix = "Full")

        Spacer(modifier = Modifier.height(12.dp))

        val saveButtonUi = remember(isSaved) { resolveSaveButtonUi(isSaved) }

        SaveButtonSection(
            saveButtonUi = saveButtonUi,
            onSaveForLater = onSaveForLater,
            onUnsaveForLater = onUnsaveForLater)
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

/** Format helpers for smart start-end date/time display. */
@VisibleForTesting
internal fun formatEventDateRangeMedium(start: Timestamp, end: Timestamp?): String {
  val zone = TimeZone.getDefault()
  val locale = Locale.US

  val startDate = start.toDate()
  // Treat end equal to start as no end
  val endDate = end?.toDate()?.takeUnless { it.time == startDate.time }

  val calStart = Calendar.getInstance(zone, locale).apply { time = startDate }
  val currentYear = Calendar.getInstance(zone, locale).get(Calendar.YEAR)
  val showYearSingle = calStart.get(Calendar.YEAR) != currentYear

  val dateFmtNoYear = SimpleDateFormat("MMM d", locale).apply { timeZone = zone }
  val dateFmtWithYear = SimpleDateFormat("MMM d, yyyy", locale).apply { timeZone = zone }
  val timeFormatter = timeShortFormatter(zone)

  if (endDate == null) {
    return buildSingleDate(startDate, showYearSingle, dateFmtNoYear, dateFmtWithYear, timeFormatter)
  }

  val calEnd = Calendar.getInstance(zone, locale).apply { time = endDate }
  val sameYearRange = calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)
  val sameDayRange = sameYearRange && calStart.isSameDay(calEnd)

  return if (sameDayRange) {
    val dateStr = dateFmtNoYear.format(startDate) // same day -> no year needed
    "$dateStr, ${timeFormatter.format(startDate)} - ${timeFormatter.format(endDate)}"
  } else {
    val startStr = formatDateForRange(startDate, sameYearRange, dateFmtNoYear, dateFmtWithYear)
    val endStr = formatDateForRange(endDate, sameYearRange, dateFmtNoYear, dateFmtWithYear)
    "$startStr, ${timeFormatter.format(startDate)} - $endStr, ${timeFormatter.format(endDate)}"
  }
}

private fun formatDateForRange(
    date: Date,
    sameYearRange: Boolean,
    dateFmtNoYear: SimpleDateFormat,
    dateFmtWithYear: SimpleDateFormat
): String {
  return if (sameYearRange) dateFmtNoYear.format(date) else dateFmtWithYear.format(date)
}

private fun buildSingleDate(
    startDate: Date,
    showYear: Boolean,
    dateFmtNoYear: SimpleDateFormat,
    dateFmtWithYear: SimpleDateFormat,
    timeFormatter: SimpleDateFormat
): String {
  val dateStr = if (showYear) dateFmtWithYear.format(startDate) else dateFmtNoYear.format(startDate)
  return "$dateStr, ${timeFormatter.format(startDate)}"
}

private fun Calendar.isSameDay(other: Calendar): Boolean {
  return get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun timeShortFormatter(tz: TimeZone): SimpleDateFormat {
  return SimpleDateFormat("HH:mm", Locale.ROOT).apply { timeZone = tz }
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

  val dateFullFmt = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US)

  // 24-hour format always showing minutes
  fun timeFull(d: Date): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(d)
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

@SuppressLint("DefaultLocale")
@Composable
fun PriceSection(price: Double) {
  Card(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("priceSection"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "\uD83D\uDCB5",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(end = 8.dp))

          Text(
              text = String.format(Locale.US, "%.2f CHF", price),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface)
        }
      }
}

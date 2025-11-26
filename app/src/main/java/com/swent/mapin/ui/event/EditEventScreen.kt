package com.swent.mapin.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import com.swent.mapin.R
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.event.Event
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EditEventScreenTestTags {
  const val SCREEN = "EditEventScreen"
  const val EVENT_CANCEL = "EDIT_EVENT_CANCEL"
  const val EVENT_SAVE = "EDIT_EVENT_SAVE"
  const val ERROR_MESSAGE = "EDIT_ERROR_MESSAGE"
  const val INPUT_EVENT_TITLE = "EDIT_INPUT_EVENT_TITLE"
  const val INPUT_EVENT_DESCRIPTION = "EDIT_INPUT_EVENT_DESCRIPTION"
  const val INPUT_EVENT_TAG = "EDIT_INPUT_EVENT_TAG"
  const val DATE_TIME_ERROR = "EDIT_DATE_TIME_ERROR"
}

fun Timestamp.toDateString(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this.toDate())

fun Timestamp.toTimeString(): String =
    SimpleDateFormat("HHmm", Locale.getDefault()).format(this.toDate())

@Composable
fun EditEventScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    event: Event = Event(),
    onCancel: () -> Unit = {},
    onDone: () -> Unit = {},
) {

  val title = remember { mutableStateOf(event.title) }
  val description = remember { mutableStateOf(event.description) }
  val location = remember { mutableStateOf(event.location.name) }

  val dateString = event.date?.toDateString() ?: ""
  val date = remember { mutableStateOf(dateString) }

  val endDateString = event.endDate?.toDateString() ?: ""
  val endDate = remember { mutableStateOf(endDateString) }

  val tagString = event.tags.joinToString(separator = " ")
  val tag = remember { mutableStateOf(tagString) }

  val timeString = event.date?.toTimeString() ?: ""
  val time = remember { mutableStateOf(timeString) }

  val endTimeString = event.endDate?.toTimeString() ?: ""
  val endTime = remember { mutableStateOf(endTimeString) }

  val price = remember { mutableStateOf(event.price.toString()) }

  val dateError = remember { mutableStateOf(false) }
  val endDateError = remember { mutableStateOf(false) }
  val timeError = remember { mutableStateOf(false) }
  val endTimeError = remember { mutableStateOf(false) }
  val titleError = remember { mutableStateOf(false) }
  val descriptionError = remember { mutableStateOf(false) }
  val locationError = remember { mutableStateOf(false) }
  val tagError = remember { mutableStateOf(false) }
  val priceError = remember { mutableStateOf(false) }
  val isLoggedIn = remember { mutableStateOf((Firebase.auth.currentUser != null)) }

  val locationExpanded = remember { mutableStateOf(false) }
  val gotLocation = remember {
    mutableStateOf(Location(location.value, LATITUDE_DEFAULT, LONGITUDE_DEFAULT))
  }
  val locations by locationViewModel.locations.collectAsState()

  val scrollState = rememberScrollState()

  fun validateStartEnd() {
    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)
  }

  val error =
      titleError.value ||
          title.value.isBlank() ||
          descriptionError.value ||
          description.value.isBlank() ||
          locationError.value ||
          location.value.isBlank() ||
          timeError.value ||
          time.value.isBlank() ||
          dateError.value ||
          date.value.isBlank() ||
          tagError.value ||
          endDateError.value ||
          endDate.value.isBlank() ||
          endTimeError.value ||
          endTime.value.isBlank() ||
          priceError.value

  val errorFields =
      listOfNotNull(
          if (titleError.value || title.value.isBlank()) stringResource(R.string.title_field)
          else null,
          if (dateError.value || date.value.isBlank()) stringResource(R.string.date_field)
          else null,
          if (locationError.value || location.value.isBlank())
              stringResource(R.string.location_field)
          else null,
          if (descriptionError.value || description.value.isBlank())
              stringResource(R.string.description_field)
          else null,
          if (tagError.value) stringResource(R.string.tag_field) else null,
          if (timeError.value || time.value.isBlank()) stringResource(R.string.time) else null,
          if (endDateError.value || endDate.value.isBlank()) "End date" else null,
          if (endTimeError.value || endTime.value.isBlank()) "End time" else null,
          if (priceError.value) stringResource(R.string.price_field) else null)

  val isEventValid = !error && isLoggedIn.value
  val isDateAndTimeValid =
      dateError.value ||
          timeError.value ||
          date.value.isBlank() ||
          time.value.isBlank() ||
          endDateError.value ||
          endTimeError.value ||
          endDate.value.isBlank() ||
          endTime.value.isBlank()
  val showValidation = remember { mutableStateOf(false) }

  Scaffold(contentWindowInsets = WindowInsets.ime) { padding ->
    Column(
        modifier =
            modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()) {
          // TopBar
          Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(48.dp).testTag(EditEventScreenTestTags.EVENT_CANCEL)) {
                      Icon(
                          imageVector = Icons.Default.Close,
                          contentDescription = "Cancel",
                          tint = MaterialTheme.colorScheme.onSurface)
                    }

                Text(
                    text = "New Event",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                      // Show validation feedback when user attempts to save
                      showValidation.value = true

                      // update per-field error flags from current values so UI shows them
                      // immediately
                      titleError.value = title.value.isBlank()
                      descriptionError.value = description.value.isBlank()
                      locationError.value = location.value.isBlank()
                      dateError.value = date.value.isBlank()
                      timeError.value = time.value.isBlank()
                      endDateError.value = endDate.value.isBlank()
                      endTimeError.value = endTime.value.isBlank()
                      tagError.value = !isValidTagInput(tag.value)
                      priceError.value = !isValidPriceInput(price.value)

                      // Run relational validation for start/end (may clear or set end errors)
                      validateStartEnd()

                      // compute validity based on flags (fresh values)
                      val nowValid =
                          !(titleError.value ||
                              descriptionError.value ||
                              locationError.value ||
                              dateError.value ||
                              timeError.value ||
                              endDateError.value ||
                              endTimeError.value ||
                              tagError.value ||
                              priceError.value) && isLoggedIn.value
                      if (!nowValid) return@IconButton

                      val sdf = SimpleDateFormat("dd/MM/yyyyHHmm", Locale.getDefault())
                      sdf.timeZone = TimeZone.getDefault()
                      val rawTime =
                          if (time.value.contains("h")) time.value.replace("h", "") else time.value
                      val rawEndTime =
                          if (endTime.value.contains("h")) endTime.value.replace("h", "")
                          else endTime.value

                      val parsedStart = runCatching { sdf.parse(date.value + rawTime) }.getOrNull()
                      val parsedEnd =
                          runCatching { sdf.parse(endDate.value + rawEndTime) }.getOrNull()

                      if (parsedStart == null) {
                        dateError.value = true
                        return@IconButton
                      }
                      if (parsedEnd == null) {
                        endDateError.value = true
                        return@IconButton
                      }

                      val startTs = Timestamp(parsedStart)
                      val endTs = Timestamp(parsedEnd)

                      if (!endTs.toDate().after(startTs.toDate())) {
                        // end must be strictly after start
                        // mark end date invalid (don't force changing time)
                        endDateError.value = true
                        endTimeError.value = false
                        return@IconButton
                      }
                      val editedEvent =
                          event.copy(
                              title = title.value,
                              description = description.value,
                              location = gotLocation.value,
                              date = startTs,
                              endDate = endTs,
                              tags = extractTags(tag.value))

                      if (Firebase.auth.currentUser?.uid == event.ownerId) {
                        eventViewModel.editEvent(event.uid, editedEvent)
                        onDone()
                      } else {
                        throw FirebaseAuthException("000", "You are not the owner of the event!")
                      }
                    },
                    modifier =
                        Modifier.size(48.dp)
                            .background(
                                color =
                                    if (isEventValid) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape)
                            .testTag(EditEventScreenTestTags.EVENT_SAVE)) {
                      Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = "Save",
                          tint =
                              if (isEventValid) MaterialTheme.colorScheme.onPrimaryContainer
                              else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
              }

          // Prominent validation banner shown right after the top bar when user attempted to save
          if (showValidation.value && !isEventValid) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(color = colorResource(R.color.red))
                        .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector = Icons.Outlined.Info,
                      contentDescription = "Validation",
                      tint = Color.White,
                      modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                      text =
                          stringResource(
                              R.string.validation_banner_prefix, errorFields.joinToString(", ")),
                      color = Color.White,
                      style = MaterialTheme.typography.bodySmall,
                      modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                }
          }

          Spacer(modifier = Modifier.padding(5.dp))
          // Title field
          Text(
              text = stringResource(R.string.title_text),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
          AddEventTextField(
              title,
              titleError,
              stringResource(R.string.title_place_holder),
              modifier = Modifier.testTag(EditEventScreenTestTags.INPUT_EVENT_TITLE),
              singleLine = true)

          Spacer(modifier = Modifier.padding(10.dp))
          // Date and time fields

          Text(
              stringResource(R.string.date_text),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
          if (isDateAndTimeValid) {
            Text(
                stringResource(R.string.date_time_error),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Red,
                modifier =
                    Modifier.padding(bottom = 8.dp)
                        .testTag(EditEventScreenTestTags.DATE_TIME_ERROR))
          }
          Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically) {
                DatePickerButton(
                    date,
                    onDateChanged = {
                      dateError.value = (date.value.isBlank())
                      validateStartEnd()
                    },
                )

                TimePickerButton(
                    time,
                    onTimeChanged = {
                      timeError.value = (time.value.isBlank())
                      validateStartEnd()
                    },
                )
              }
          Spacer(modifier = Modifier.height(8.dp))
          // End date/time pickers
          Text(
              stringResource(R.string.end_date_text),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
          Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically) {
                DatePickerButton(
                    endDate,
                    onDateChanged = {
                      endDateError.value = (endDate.value.isBlank())
                      validateStartEnd()
                    },
                )

                TimePickerButton(
                    endTime,
                    onTimeChanged = {
                      endTimeError.value = (endTime.value.isBlank())
                      validateStartEnd()
                    },
                )
              }
          // Location Field
          Text(
              stringResource(R.string.location_text),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
          LocationDropDownMenu(
              location,
              locationError,
              locationViewModel,
              locationExpanded,
              locations,
              gotLocation,
          )

          Spacer(modifier = Modifier.padding(10.dp))
          // Description field
          Text(
              stringResource(R.string.description_text),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
          AddEventTextField(
              description,
              descriptionError,
              stringResource(R.string.description_place_holder),
              modifier =
                  Modifier.height(120.dp).testTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION),
          )

          Spacer(modifier = Modifier.padding(10.dp))
          // Tag Field
          Text(
              stringResource(R.string.tag_text),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
          AddEventTextField(
              tag,
              tagError,
              stringResource(R.string.add_tag_place_holder),
              modifier = Modifier.height(80.dp).testTag(EditEventScreenTestTags.INPUT_EVENT_TAG),
              isTag = true)
          Spacer(modifier = Modifier.padding(bottom = 5.dp))
          Text(
              "Accessibility and Price cannot be modified to avoid being unfair to participants!",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
        }
  }
}

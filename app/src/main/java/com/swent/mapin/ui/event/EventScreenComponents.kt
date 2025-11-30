package com.swent.mapin.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.swent.mapin.R
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Interface class for event screen's test tags */
@Suppress("PropertyName")
interface EventScreenTestTag {
  val INPUT_EVENT_TITLE: String
  val DATE_TIME_ERROR: String
  val INPUT_EVENT_DESCRIPTION: String
  val INPUT_EVENT_TAG: String
  val EVENT_CANCEL: String
  val EVENT_SAVE: String
}

/** Extension function for TimeStamp to convert a timestamp to a dd/MM/yyyy date string */
fun Timestamp.toDateString(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this.toDate())

/** Extension function for TimeStamp to convert a timestamp to a HHmm time string */
fun Timestamp.toTimeString(): String =
    SimpleDateFormat("HHmm", Locale.getDefault()).format(this.toDate())

/**
 * With help of GPT: A composable button that opens a [DatePickerDialog] allowing the user to select
 * a date from today onwards.
 *
 * When the user selects a date, the chosen value is formatted as `dd/MM/yyyy` and stored in
 * [selectedDate].
 *
 * @param selectedDate A [MutableState] holding the selected date as a string in the format
 *   `dd/MM/yyyy`.
 * @param onDateClick Callback triggered when the user clicks the button
 * @param onDateChanged Callback changed when the date value changes
 */
@Composable
fun DatePickerButton(
    selectedDate: MutableState<String>,
    onDateClick: (() -> Unit)? = null,
    onDateChanged: (() -> Unit)? = null
) {
  val context = LocalContext.current

  Button(
      onClick = {
        if (onDateClick != null) {
          onDateClick()
        } else {
          val calendar = Calendar.getInstance()
          val year = calendar[Calendar.YEAR]
          val month = calendar[Calendar.MONTH]
          val day = calendar[Calendar.DAY_OF_MONTH]

          val datePickerDialog =
              DatePickerDialog(
                  context,
                  { _, pickedYear, pickedMonth, pickedDay ->
                    // Format as dd/MM/yyyy
                    selectedDate.value =
                        String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            pickedDay,
                            pickedMonth + 1,
                            pickedYear)
                    onDateChanged?.invoke()
                  },
                  year,
                  month,
                  day)

          datePickerDialog.datePicker.minDate = calendar.timeInMillis

          datePickerDialog.show()
        }
      },
      shape = RoundedCornerShape(4.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = Color.Unspecified,
              disabledContentColor = Color.Unspecified,
              disabledContainerColor = Color.Unspecified),
      modifier = Modifier.width(200.dp).testTag(AddEventScreenTestTags.PICK_EVENT_DATE)) {
        Text("Select Date: ${selectedDate.value}", color = MaterialTheme.colorScheme.onPrimary)
      }
}

/**
 * With help of GPT: A button that shows a [TimePickerDialog] when clicked.
 *
 * Updates the given [selectedTime] state with the time selected by the user in 24-hour format.
 *
 * @param selectedTime The [MutableState] storing the currently selected time as a string (formatted
 *   as HHmm).
 * @param onTimeClick Callback triggered when the user clicks the button
 * @param onTimeChanged Callback triggered when the time value changes
 */
@Composable
fun TimePickerButton(
    selectedTime: MutableState<String>,
    onTimeClick: (() -> Unit)? = null,
    onTimeChanged: (() -> Unit)? = null
) {
  val context = LocalContext.current

  Button(
      onClick = {
        if (onTimeClick != null) {
          onTimeClick()
        } else {
          val calendar = Calendar.getInstance()
          val hour = calendar[Calendar.HOUR_OF_DAY]
          val minute = calendar[Calendar.MINUTE]

          TimePickerDialog(
                  context,
                  { _, pickedHour, pickedMinute ->
                    selectedTime.value =
                        pickedHour.toString().padStart(2, '0') +
                            pickedMinute.toString().padStart(2, '0') // internal "HHmm"
                    onTimeChanged?.invoke()
                  },
                  hour,
                  minute,
                  true)
              .show()
        }
      },
      modifier = Modifier.width(200.dp).testTag(AddEventScreenTestTags.PICK_EVENT_TIME),
      shape = RoundedCornerShape(4.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = Color.Unspecified,
              disabledContentColor = Color.Unspecified,
              disabledContainerColor = Color.Unspecified)) {
        Text(
            ("Select Time: ${selectedTime.value.chunked(2).joinToString("h")}"),
            color = MaterialTheme.colorScheme.onPrimary)
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
fun PublicSwitch(
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
 * Validation banner which displays error fields when a user attemps to save
 *
 * @param errorFields List of error fields as strings
 */
@Composable
fun ValidationBanner(errorFields: List<String>) {
  Row(
      modifier =
          Modifier.fillMaxWidth().background(color = colorResource(R.color.red)).padding(8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Validation",
            tint = Color.White,
            modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text =
                stringResource(R.string.validation_banner_prefix, errorFields.joinToString(", ")),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag(AddEventScreenTestTags.ERROR_MESSAGE))
      }
}

/**
 * Top bar Composable for event screens
 *
 * @param title The title for the top bar
 * @param testTags The testTag object for testing
 * @param isEventValid Boolean indicating if the current event can be validated
 * @param onCancel Callback invoked when pressing cancel
 * @param onSave Callback invoked when pressing save
 */
@Composable
fun EventTopBar(
    title: String,
    testTags: EventScreenTestTag,
    isEventValid: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        // Cancel button
        IconButton(
            onClick = onCancel, modifier = Modifier.size(48.dp).testTag(testTags.EVENT_CANCEL)) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Cancel",
                  tint = MaterialTheme.colorScheme.onSurface)
            }

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f))

        // Save button
        IconButton(
            onClick = onSave,
            modifier =
                Modifier.size(48.dp)
                    .background(
                        color =
                            if (isEventValid) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape)
                    .testTag(testTags.EVENT_SAVE)) {
              Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Save",
                  tint =
                      if (isEventValid) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant)
            }
      }
}

/**
 * Body of the Event form composable which contains main components. Shared by both AddEvent and
 * EditEvent screens.
 *
 * @param title Mutable state for the event title text.
 * @param titleError Mutable state indicating whether the title is invalid.
 * @param isDateAndTimeValid Overall validity flag for date and time fields.
 * @param date Mutable state for the event start date.
 * @param dateError Mutable state indicating whether the start date is invalid.
 * @param time Mutable state for the event start time.
 * @param timeError Mutable state indicating whether the start time is invalid.
 * @param endDate Mutable state for the event end date.
 * @param endDateError Mutable state indicating whether the end date is invalid.
 * @param endTime Mutable state for the event end time.
 * @param endTimeError Mutable state indicating whether the end time is invalid.
 * @param location Mutable state for the location text input.
 * @param locationError Mutable state indicating whether the location input is invalid.
 * @param locations List of selectable `Location` objects.
 * @param gotLocation Mutable state holding the currently selected `Location`.
 * @param locationExpanded Mutable state controlling whether the location dropdown is expanded.
 * @param locationViewModel ViewModel used for fetching or managing location data.
 * @param description Mutable state for the event description text.
 * @param descriptionError Mutable state indicating whether the description is invalid.
 * @param tag Mutable state for the event tag text.
 * @param tagError Mutable state indicating whether the tag is invalid.
 */
@Composable
fun EventFormBody(
    title: MutableState<String>,
    titleError: MutableState<Boolean>,
    isDateAndTimeValid: Boolean,
    date: MutableState<String>,
    dateError: MutableState<Boolean>,
    time: MutableState<String>,
    timeError: MutableState<Boolean>,
    endDate: MutableState<String>,
    endDateError: MutableState<Boolean>,
    endTime: MutableState<String>,
    endTimeError: MutableState<Boolean>,
    location: MutableState<String>,
    locationError: MutableState<Boolean>,
    locations: List<Location>,
    gotLocation: MutableState<Location>,
    locationExpanded: MutableState<Boolean>,
    locationViewModel: LocationViewModel,
    description: MutableState<String>,
    descriptionError: MutableState<Boolean>,
    tag: MutableState<String>,
    tagError: MutableState<Boolean>,
    testTags: EventScreenTestTag
) {

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
      modifier = Modifier.testTag(testTags.INPUT_EVENT_TITLE),
      singleLine = true)

  Spacer(modifier = Modifier.padding(10.dp))
  // Date and time fields

  Text(
      stringResource(R.string.date_text),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp))
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        DatePickerButton(
            date,
            onDateChanged = {
              dateError.value = (date.value.isBlank())
              validateStartEndLogic(
                  date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)
            },
        )

        TimePickerButton(
            time,
            onTimeChanged = {
              timeError.value = (time.value.isBlank())
              validateStartEndLogic(
                  date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)
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
              validateStartEndLogic(
                  date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)
            },
        )

        TimePickerButton(
            endTime,
            onTimeChanged = {
              endTimeError.value = (endTime.value.isBlank())
              validateStartEndLogic(
                  date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)
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
      modifier = Modifier.height(120.dp).testTag(testTags.INPUT_EVENT_DESCRIPTION),
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
      modifier = Modifier.height(80.dp).testTag(testTags.INPUT_EVENT_TAG),
      isTag = true)
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.swent.mapin.R
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.ui.map.PublicSwitch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val LONGITUDE_DEFAULT = 0.0
const val LATITUDE_DEFAULT = 0.0

object AddEventScreenTestTags {
  const val INPUT_EVENT_TITLE = "inputEventTitle"
  const val PICK_EVENT_DATE = "pickEventDate"
  const val PICK_EVENT_TIME = "pickEventTime"
  const val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  const val INPUT_EVENT_TAG = "inputEventTag"
  const val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val EVENT_CANCEL = "eventCancel"
  const val EVENT_SAVE = "eventSave"
  const val ERROR_MESSAGE = "errorMessage"

  const val PUBLIC_SWITCH = "publicSwitch"

  const val PUBLIC_TEXT = "publicText"

  const val SCREEN = "AddEventPopUp"
}

/**
 * A reusable text field composable used in the Add Event dialog.
 *
 * This composable wraps a [TextField] and handles error highlighting, placeholders, and optional
 * field types such as location, day, month, year, and tag.
 *
 * @param textField The [MutableState] holding the current text value of the field.
 * @param error The [MutableState] indicating whether this field has an error.
 * @param placeholderString The placeholder text to display when the field is empty.
 * @param modifier [Modifier] for customizing layout or styling.
 * @param isLocation Whether this field is a location field (affects validation/formatting).
 * @param isTag Whether this field represents a tag input (affects validation/formatting).
 */
@Composable
fun AddEventTextField(
    textField: MutableState<String>,
    error: MutableState<Boolean>,
    placeholderString: String,
    modifier: Modifier = Modifier,
    isLocation: Boolean = false,
    isTag: Boolean = false,
    locationQuery: () -> Unit = {},
    singleLine: Boolean = false
) {

  OutlinedTextField(
      modifier = modifier.fillMaxWidth(),
      value = textField.value,
      onValueChange = {
        textField.value = it
        if (isLocation) {
          locationQuery()
        } else if (isTag) {
          error.value = !isValidTagInput(it)
        } else {
          error.value = textField.value.isBlank()
        }
      },
      isError = error.value,
      placeholder = { Text(placeholderString, fontSize = 14.sp) },
      singleLine = singleLine)
}
/**
 * With help of GPT: A composable button that opens a [DatePickerDialog] allowing the user to select
 * a date from today onwards.
 *
 * When the user selects a date, the chosen value is formatted as `dd/MM/yyyy` and stored in
 * [selectedDate].
 *
 * @param selectedDate A [MutableState] holding the selected date as a string in the format
 *   `dd/MM/yyyy`.
 */
@Composable
fun FutureDatePickerButton(
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
          ButtonColors(
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
                        "${pickedHour.toString().padStart(2, '0')}h" +
                            pickedMinute.toString().padStart(2, '0')
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
          ButtonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = Color.Unspecified,
              disabledContentColor = Color.Unspecified,
              disabledContainerColor = Color.Unspecified)) {
        Text("Select Time: ${selectedTime.value}", color = MaterialTheme.colorScheme.onPrimary)
      }
}

/**
 * Displays a pop-up dialog for adding a new event, including fields for title, date, time,
 * location, description, and tags. Handles error checking and displays an error message if any
 * required fields are missing or invalid.
 *
 * @param modifier [Modifier] to customize the pop-up layout.
 * @param onDone callback triggered when the user is done with the event creation popup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    onCancel: () -> Unit = {},
    onDone: () -> Unit = {},
) {
  val title = remember { mutableStateOf("") }
  val description = remember { mutableStateOf("") }
  val location = remember { mutableStateOf("") }
  val date = remember { mutableStateOf("") }
  val tag = remember { mutableStateOf("") }
  val time = remember { mutableStateOf("") }
  val isPublic = remember { mutableStateOf(true) }

  val dateError = remember { mutableStateOf(true) }
  val timeError = remember { mutableStateOf(true) }
  val titleError = remember { mutableStateOf(true) }
  val descriptionError = remember { mutableStateOf(true) }
  val locationError = remember { mutableStateOf(true) }
  val tagError = remember { mutableStateOf(false) }
  val isLoggedIn = remember { mutableStateOf((Firebase.auth.currentUser != null)) }

  val locationExpanded = remember { mutableStateOf(false) }
  val gotLocation = remember {
    mutableStateOf(Location(location.value, LATITUDE_DEFAULT, LONGITUDE_DEFAULT))
  }
  val locations by locationViewModel.locations.collectAsState()

  val error =
      titleError.value ||
          descriptionError.value ||
          locationError.value ||
          timeError.value ||
          dateError.value

  val errorFields =
      listOfNotNull(
          if (titleError.value) stringResource(R.string.title_field) else null,
          if (dateError.value) stringResource(R.string.date_field) else null,
          if (locationError.value) stringResource(R.string.location_field) else null,
          if (descriptionError.value) stringResource(R.string.description_field) else null,
          if (tagError.value) stringResource(R.string.tag_field) else null,
          if (timeError.value) stringResource(R.string.time) else null)

  val isEventValid = !error && isLoggedIn.value

  val scrollState = rememberScrollState()

  Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
    // TopBar
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          IconButton(
              onClick = onCancel,
              modifier = Modifier.size(48.dp).testTag(AddEventScreenTestTags.EVENT_CANCEL)) {
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
                val sdf = SimpleDateFormat("dd/MM/yyyyHHmm", Locale.getDefault())
                val dateTime = sdf.parse(date.value + time.value.replace("h", ""))
                val timestamp = if (dateTime != null) Timestamp(dateTime) else Timestamp.now()
                saveEvent(
                    eventViewModel,
                    title.value,
                    description.value,
                    gotLocation.value,
                    timestamp,
                    Firebase.auth.currentUser?.uid,
                    extractTags(tag.value),
                    isPublic.value,
                    onDone)
              },
              enabled = isEventValid,
              modifier =
                  Modifier.size(48.dp)
                      .background(
                          color =
                              if (isEventValid) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surfaceVariant,
                          shape = CircleShape)
                      .testTag(AddEventScreenTestTags.EVENT_SAVE)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint =
                        if (isEventValid) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant)
              }
        }

    Spacer(modifier = Modifier.padding(5.dp))
    // Title field
    Text(
        text = stringResource(R.string.title_field),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp))
    AddEventTextField(
        title,
        titleError,
        stringResource(R.string.title_place_holder),
        modifier = Modifier.testTag(AddEventScreenTestTags.INPUT_EVENT_TITLE),
        singleLine = true)

    Spacer(modifier = Modifier.padding(10.dp))
    // Date and time fields

    Text(
        stringResource(R.string.date_field),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
          FutureDatePickerButton(
              date,
              onDateChanged = { dateError.value = (date.value.isBlank()) },
          )

          TimePickerButton(
              time,
              onTimeChanged = { timeError.value = (time.value.isBlank()) },
          )
        }
    // Location Field
    Text(
        stringResource(R.string.location_field),
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
        stringResource(R.string.description_field),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp))
    AddEventTextField(
        description,
        descriptionError,
        stringResource(R.string.description_place_holder),
        modifier = Modifier.height(120.dp).testTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION),
    )

    Spacer(modifier = Modifier.padding(10.dp))
    // Tag Field
    AddEventTextField(
        tag,
        tagError,
        stringResource(R.string.add_tag_place_holder),
        modifier = Modifier.height(80.dp).testTag(AddEventScreenTestTags.INPUT_EVENT_TAG),
        isTag = true)

    Spacer(modifier = Modifier.padding(bottom = 5.dp))
    // Public/Private switch
    if (isPublic.value) {
      PublicSwitch(
          isPublic = isPublic.value,
          onPublicChange = { isPublic.value = it },
          "This event will be public",
          "Others can see this event on the map",
          Modifier.testTag(AddEventScreenTestTags.PUBLIC_SWITCH),
          Modifier.testTag(AddEventScreenTestTags.PUBLIC_TEXT))
    } else {
      PublicSwitch(
          isPublic = isPublic.value,
          onPublicChange = { isPublic.value = it },
          "This event will be private",
          "Others will not see this event on the map",
          Modifier.testTag(AddEventScreenTestTags.PUBLIC_SWITCH),
          Modifier.testTag(AddEventScreenTestTags.PUBLIC_TEXT))
    }

    // Error displaying
    if (error) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(60.dp)) {
        Spacer(modifier = Modifier.padding(2.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Info",
            tint = colorResource(R.color.red),
            modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.padding(3.dp))
        Text(
            "The following fields are missing/incorrect: " + errorFields.joinToString(", "),
            fontSize = 12.sp,
            color = colorResource(R.color.red),
            lineHeight = 14.sp,
            modifier = Modifier.testTag(AddEventScreenTestTags.ERROR_MESSAGE))
      }
    }
  }
}

package com.swent.mapin.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val LONGITUDE_DEFAULT = 0.0
const val LATITUDE_DEFAULT = 0.0

object AddEventScreenTestTags {
  const val INPUT_EVENT_TITLE = "inputEventTitle"

  const val DATE_TIME_ERROR = "dateTimeErrorMessage"
  const val PICK_EVENT_DATE = "pickEventDate"
  const val PICK_EVENT_TIME = "pickEventTime"
  const val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  const val INPUT_EVENT_TAG = "inputEventTag"
  const val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val INPUT_EVENT_PRICE = "inputEventPrice"
  const val EVENT_CANCEL = "eventCancel"
  const val EVENT_SAVE = "eventSave"
  const val ERROR_MESSAGE = "errorMessage"

  const val PUBLIC_SWITCH = "publicSwitch"

  const val PUBLIC_TEXT = "publicText"

  const val SCREEN = "AddEventScreen"
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
 * @param isPrice Whether this field is a price field
 * @param isTag Whether this field represents a tag input (affects validation/formatting).
 * @param locationQuery Query callback for the location
 * @param singleLine Whether the text field should show text in a single line or not
 */
@Composable
fun AddEventTextField(
    textField: MutableState<String>,
    error: MutableState<Boolean>,
    placeholderString: String,
    modifier: Modifier = Modifier,
    isLocation: Boolean = false,
    isPrice: Boolean = false,
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
        } else if (isPrice) {
          error.value = !isValidPriceInput(it)
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
private fun PublicSwitch(
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
 * Displays a pop-up dialog for adding a new event, including fields for title, date, time,
 * location, description, and tags. Handles error checking and displays an error message if any
 * required fields are missing or invalid.
 *
 * @param modifier [Modifier] to customize the pop-up layout.
 * @param eventViewModel ViewModel for events
 * @param locationViewModel ViewModel for Locations
 * @param onCancel callback triggered when the user cancels the event creation
 * @param onDone callback triggered when the user is done with the event creation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel,
    locationViewModel: LocationViewModel = viewModel(),
    onCancel: () -> Unit = {},
    onDone: () -> Unit = {},
) {
  val title = remember { mutableStateOf("") }
  val description = remember { mutableStateOf("") }
  val location = remember { mutableStateOf("") }
  val date = remember { mutableStateOf("") }
  val endDate = remember { mutableStateOf("") }
  val tag = remember { mutableStateOf("") }
  val time = remember { mutableStateOf("") }
  val endTime = remember { mutableStateOf("") }
  val price = remember { mutableStateOf("") }
  val isPublic = remember { mutableStateOf(true) }

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

  // Helper to validate start/end together and set appropriate error flags.
  fun validateStartEnd() {
    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)
  }
  // Show missing/incorrect fields either when the user requested validation (clicked Save)
  // or when a per-field error flag is set, or when a required field is empty.
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

  val scrollState = rememberScrollState()

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
                      sdf.timeZone = java.util.TimeZone.getDefault()
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

                      saveEvent(
                          eventViewModel,
                          title.value,
                          description.value,
                          gotLocation.value,
                          startTs,
                          endTs,
                          Firebase.auth.currentUser?.uid,
                          extractTags(tag.value),
                          isPublic.value,
                          onDone,
                          price.value.toDoubleOrNull() ?: 0.0)
                    },
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
                      modifier = Modifier.testTag(AddEventScreenTestTags.ERROR_MESSAGE))
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
              modifier = Modifier.testTag(AddEventScreenTestTags.INPUT_EVENT_TITLE),
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
                    Modifier.padding(bottom = 8.dp).testTag(AddEventScreenTestTags.DATE_TIME_ERROR))
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
                  Modifier.height(120.dp).testTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION),
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
              modifier = Modifier.height(80.dp).testTag(AddEventScreenTestTags.INPUT_EVENT_TAG),
              isTag = true)

          Spacer(modifier = Modifier.padding(bottom = 10.dp))
          // Price field
          Text(
              stringResource(R.string.price_text),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            AddEventTextField(
                price,
                priceError,
                stringResource(R.string.price_place_holder),
                modifier =
                    Modifier.fillMaxWidth(0.3f).testTag(AddEventScreenTestTags.INPUT_EVENT_PRICE),
                singleLine = true,
                isPrice = true)
            Spacer(modifier = Modifier.padding(horizontal = 5.dp))
            Text(
                stringResource(R.string.currency_switzerland),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

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
        }
  }
}

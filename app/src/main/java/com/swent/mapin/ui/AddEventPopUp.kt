package com.swent.mapin.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.swent.mapin.R
import com.swent.mapin.ui.theme.MapInTheme
import java.util.Calendar
import java.util.Locale

object AddEventPopUpTestTags {
  const val INPUT_EVENT_TITLE = "inputEventTitle"
  const val PICK_EVENT_DATE = "pickEventDate"
  const val PICK_EVENT_TIME = "pickEventTime"
  const val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  const val INPUT_EVENT_TAG = "inputEventTag"
  const val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val EVENT_CANCEL = "eventCancel"
  const val EVENT_SAVE = "eventSave"
  const val ERROR_MESSAGE = "errorMessage"
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
    isTag: Boolean = false
) {
  TextField(
      modifier = modifier.border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
      value = textField.value,
      onValueChange = {
        textField.value = it
        if (isLocation) {
          // TODO add Nominatim location logic
        } else if (isTag) {
          error.value = !isValidTagInput(it)
        } else {
          error.value = textField.value.isBlank()
        }
      },
      isError = error.value,
      placeholder = { Text(placeholderString, fontSize = 14.sp) },
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent,
              disabledContainerColor = Color.Transparent,
              errorContainerColor = Color.Transparent,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent),
  )
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
fun FutureDatePickerButton(selectedDate: MutableState<String>, onDateClick: (() -> Unit)? = null) {
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
                  },
                  year,
                  month,
                  day)

          datePickerDialog.datePicker.minDate = calendar.timeInMillis

          datePickerDialog.show()
        }
      },
      modifier = Modifier.width(200.dp).testTag(AddEventPopUpTestTags.PICK_EVENT_DATE)) {
        Text("Select Date: ${selectedDate.value}")
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
fun TimePickerButton(selectedTime: MutableState<String>, onTimeClick: (() -> Unit)? = null) {
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
                  },
                  hour,
                  minute,
                  true)
              .show()
        }
      },
      modifier = Modifier.width(180.dp).testTag(AddEventPopUpTestTags.PICK_EVENT_TIME),
      colors =
          ButtonColors(
              containerColor = colorResource(R.color.turquoise),
              contentColor = Color.Unspecified,
              disabledContentColor = Color.Unspecified,
              disabledContainerColor = Color.Unspecified)) {
        Text("Select Time: ${selectedTime.value}")
      }
}

/**
 * Displays a pop-up dialog for adding a new event, including fields for title, date, time,
 * location, description, and tags. Handles error checking and displays an error message if any
 * required fields are missing or invalid.
 *
 * @param modifier [Modifier] to customize the pop-up layout.
 * @param onBack Callback triggered when the user clicks the back/close button.
 * @param onSave Callback triggered when the user clicks the Save button. Only called if no errors
 *   are present.
 * @param onCancel Callback triggered when the user clicks the Cancel button.
 * @param onDismiss Callback triggered when the dialog is dismissed by clicking outside or pressing
 *   back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventPopUp(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {

  val title = remember { mutableStateOf("") }
  val description = remember { mutableStateOf("") }
  val location = remember { mutableStateOf("") }
  val date = remember { mutableStateOf("") }
  val tag = remember { mutableStateOf("") }
  val time = remember { mutableStateOf("") }

  val titleError = remember { mutableStateOf(true) }
  val descriptionError = remember { mutableStateOf(true) }
  val locationError = remember { mutableStateOf(true) }
  val dateError = remember { mutableStateOf(true) }
  val tagError = remember { mutableStateOf(false) }

  val error =
      titleError.value ||
          descriptionError.value ||
          locationError.value ||
          time.value.isBlank() ||
          date.value.isBlank()

  val errorFields =
      listOfNotNull(
          if (titleError.value) stringResource(R.string.title_field) else null,
          if (dateError.value) stringResource(R.string.date_field) else null,
          if (locationError.value) stringResource(R.string.location_field) else null,
          if (descriptionError.value) stringResource(R.string.description_field) else null,
          if (tagError.value) stringResource(R.string.tag_field) else null,
          if (time.value.isBlank()) stringResource(R.string.time) else null)

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)) {
        Card(
            modifier =
                Modifier.fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .heightIn(min = 400.dp)
                    .background(Color.Transparent)
                    .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
              Spacer(modifier = Modifier.padding(10.dp))
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                      IconButton(onClick = onBack, Modifier.padding(start = 10.dp).size(25.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                      }
                      Spacer(modifier = Modifier.padding(10.dp))
                      Text(stringResource(R.string.event_creation_title), fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.padding(5.dp))
                    Text(
                        stringResource(R.string.title_field),
                        modifier = Modifier.padding(end = 180.dp).padding(bottom = 2.dp),
                        fontSize = 16.sp)
                    AddEventTextField(
                        title,
                        titleError,
                        stringResource(R.string.title_place_holder),
                        modifier =
                            Modifier.padding(horizontal = 32.dp)
                                .testTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE))
                    Spacer(modifier = Modifier.padding(10.dp))
                    Text(
                        stringResource(R.string.date_field),
                        modifier = Modifier.padding(end = 180.dp).padding(bottom = 2.dp),
                        fontSize = 16.sp)
                    FutureDatePickerButton(date)
                    Spacer(modifier = Modifier.padding(10.dp))
                    TimePickerButton(time)
                    Spacer(modifier = Modifier.padding(5.dp))
                    Text(
                        stringResource(R.string.location_field),
                        modifier = Modifier.padding(end = 150.dp).padding(bottom = 2.dp),
                        fontSize = 16.sp)
                    AddEventTextField(
                        location,
                        locationError,
                        stringResource(R.string.location_place_holder),
                        isLocation = true,
                        modifier =
                            Modifier.padding(horizontal = 32.dp)
                                .testTag(AddEventPopUpTestTags.INPUT_EVENT_LOCATION))
                    Spacer(modifier = Modifier.padding(10.dp))
                    Text(
                        stringResource(R.string.description_field),
                        modifier = Modifier.padding(end = 130.dp).padding(bottom = 2.dp),
                        fontSize = 16.sp)
                    AddEventTextField(
                        description,
                        descriptionError,
                        stringResource(R.string.description_place_holder),
                        modifier =
                            Modifier.height(100.dp)
                                .padding(horizontal = 32.dp)
                                .testTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION))
                    Spacer(modifier = Modifier.padding(10.dp))
                    AddEventTextField(
                        tag,
                        tagError,
                        stringResource(R.string.add_tag_place_holder),
                        modifier =
                            Modifier.height(80.dp)
                                .padding(horizontal = 32.dp)
                                .testTag(AddEventPopUpTestTags.INPUT_EVENT_TAG),
                        isTag = true)
                    if (error) {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.height(60.dp)) {
                            Spacer(modifier = Modifier.padding(2.dp))
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = colorResource(R.color.red),
                                modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.padding(3.dp))
                            Text(
                                "The following fields are missing/incorrect: " +
                                    errorFields.joinToString(", "),
                                fontSize = 12.sp,
                                color = colorResource(R.color.red),
                                lineHeight = 14.sp,
                                modifier = Modifier.testTag(AddEventPopUpTestTags.ERROR_MESSAGE))
                          }
                    }
                    Row {
                      ElevatedButton(
                          onClick = {
                            // TODO add backend store operation
                            onSave()
                          },
                          enabled = !error,
                          colors =
                              ButtonColors(
                                  containerColor = colorResource(R.color.sage_green),
                                  contentColor = Color.Black,
                                  disabledContentColor = Color.Unspecified,
                                  disabledContainerColor = Color.Gray),
                          modifier =
                              Modifier.width(95.dp).testTag(AddEventPopUpTestTags.EVENT_SAVE)) {
                            Text(stringResource(R.string.save_button))
                          }
                      Spacer(modifier = Modifier.padding(10.dp))
                      ElevatedButton(
                          onClick = onCancel,
                          colors =
                              ButtonColors(
                                  containerColor = colorResource(R.color.salmon),
                                  contentColor = Color.Black,
                                  disabledContentColor = Color.Unspecified,
                                  disabledContainerColor = Color.Gray),
                          modifier =
                              Modifier.width(95.dp).testTag(AddEventPopUpTestTags.EVENT_CANCEL)) {
                            Text(stringResource(R.string.cancel_button))
                          }
                    }
                    Spacer(modifier = Modifier.padding(5.dp))
                  }
            }
      }
}

@Preview(showBackground = true)
@Composable
fun AddEventPopUpPreview() {
  MapInTheme { AddEventPopUp() }
}

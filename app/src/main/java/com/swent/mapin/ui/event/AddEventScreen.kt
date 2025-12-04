package com.swent.mapin.ui.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.swent.mapin.R
import com.swent.mapin.model.LocationViewModel
import kotlin.String

const val LONGITUDE_DEFAULT = 0.0
const val LATITUDE_DEFAULT = 0.0

object AddEventScreenTestTags : EventScreenTestTag {
  override val INPUT_EVENT_TITLE = "inputEventTitle"
  override val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  override val INPUT_EVENT_TAG = "inputEventTag"

  override val EVENT_CANCEL = "eventCancel"
  override val EVENT_SAVE = "eventSave"
  override val ERROR_MESSAGE = "errorMessage"

  override val PICK_EVENT_DATE = "pickEventDate"
  override val PICK_END_DATE = "pickEventEndDate"
  override val PICK_EVENT_TIME = "pickEventTime"
  override val PICK_END_TIME = "pickEventEndTime"
  override val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val INPUT_EVENT_PRICE = "inputEventPrice"

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
  val formState = rememberEventFormState()
  val locations by locationViewModel.locations.collectAsState()
  val scrollState = rememberScrollState()

  val errorFields =
      formState.getErrorFields(
          titleFieldName = stringResource(R.string.title_field),
          dateFieldName = stringResource(R.string.date_field),
          timeFieldName = stringResource(R.string.time),
          locationFieldName = stringResource(R.string.location_field),
          descriptionFieldName = stringResource(R.string.description_field),
          tagFieldName = stringResource(R.string.tag_field),
          priceFieldName = stringResource(R.string.price_field))

  Scaffold(contentWindowInsets = WindowInsets.ime) { padding ->
    Column(modifier = modifier.padding(padding).fillMaxWidth().navigationBarsPadding()) {
      // TopBar
      EventTopBar(
          title = "New Event",
          testTags = AddEventScreenTestTags,
          isEventValid = formState.isValid(),
          onCancel = onCancel,
          onSave = {
            formState.validateAllFields()

            if (!formState.isValid()) return@EventTopBar

            val timestamps = formState.parseTimestamps() ?: return@EventTopBar

            val (startTs, endTs) = timestamps

            saveEvent(
                eventViewModel,
                formState.title.value,
                formState.description.value,
                formState.gotLocation.value,
                startTs,
                endTs,
                Firebase.auth.currentUser?.uid,
                extractTags(formState.tag.value),
                formState.isPublic.value,
                onDone,
                formState.price.value.toDoubleOrNull() ?: 0.0)
          })

      // Prominent validation banner shown right after the top bar when user attempted to save
      if (formState.showValidation.value && !formState.isValid()) {
        ValidationBanner(errorFields, AddEventScreenTestTags)
      }

      Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        Spacer(modifier = Modifier.padding(5.dp))

        EventFormBody(
            title = formState.title,
            titleError = formState.titleError,
            date = formState.date,
            dateError = formState.dateError,
            time = formState.time,
            timeError = formState.timeError,
            endDate = formState.endDate,
            endDateError = formState.endDateError,
            endTime = formState.endTime,
            endTimeError = formState.endTimeError,
            location = formState.location,
            locationError = formState.locationError,
            locations = locations,
            gotLocation = formState.gotLocation,
            locationExpanded = formState.locationExpanded,
            locationViewModel = locationViewModel,
            description = formState.description,
            descriptionError = formState.descriptionError,
            tag = formState.tag,
            tagError = formState.tagError,
            testTags = AddEventScreenTestTags)

        Spacer(modifier = Modifier.padding(bottom = 10.dp))

        // Price field
        Text(
            stringResource(R.string.price_text),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          AddEventTextField(
              formState.price,
              formState.priceError,
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
        if (formState.isPublic.value) {
          PublicSwitch(
              isPublic = formState.isPublic.value,
              onPublicChange = { formState.isPublic.value = it },
              "This event will be public",
              "Others can see this event on the map",
              Modifier.testTag(AddEventScreenTestTags.PUBLIC_SWITCH),
              Modifier.testTag(AddEventScreenTestTags.PUBLIC_TEXT))
        } else {
          PublicSwitch(
              isPublic = formState.isPublic.value,
              onPublicChange = { formState.isPublic.value = it },
              "This event will be private",
              "Others will not see this event on the map",
              Modifier.testTag(AddEventScreenTestTags.PUBLIC_SWITCH),
              Modifier.testTag(AddEventScreenTestTags.PUBLIC_TEXT))
        }
      }
    }
  }
}

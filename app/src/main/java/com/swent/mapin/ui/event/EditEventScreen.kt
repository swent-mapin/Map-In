package com.swent.mapin.ui.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.R
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.event.Event

object EditEventScreenTestTags : EventScreenTestTag {

  override val EVENT_CANCEL = "EDIT_EVENT_CANCEL"
  override val EVENT_SAVE = "EDIT_EVENT_SAVE"
  override val INPUT_EVENT_TITLE = "EDIT_INPUT_EVENT_TITLE"
  override val INPUT_EVENT_DESCRIPTION = "EDIT_INPUT_EVENT_DESCRIPTION"
  override val INPUT_EVENT_TAG = "EDIT_INPUT_EVENT_TAG"

  override val ERROR_MESSAGE = "EDIT_ERROR_MESSAGE"
  override val PICK_EVENT_DATE = "EDIT_PICK_DATE"
  override val PICK_END_DATE = "EDIT_PICK_END_DATE"
  override val PICK_EVENT_TIME = "EDIT_PICK_TIME"
  override val PICK_END_TIME = "eEDIT_PICK_END_TIME"
  override val INPUT_EVENT_LOCATION = "EDIT_INPUT_LOCATION"
  const val SCREEN = "EditEventScreen"
}
/**
 * Edit Event Screen composable, allows event owners to edit events
 *
 * @param modifier Modifier for the composable
 * @param eventViewModel ViewModel for event functionalities
 * @param locationViewModel ViewModel for location functionalities
 * @param event The Event the owner wants to edit
 * @param onCancel Callback triggered when pressing Cancel
 * @param onDone Callback triggered when pressing Save
 */
@Composable
fun EditEventScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    event: Event = Event(),
    onCancel: () -> Unit = {},
    onDone: () -> Unit = {},
) {
  val formState = rememberEventFormState(event)
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
          title = "Edit Event",
          testTags = EditEventScreenTestTags,
          isEventValid = formState.isValid(),
          onCancel = onCancel,
          onSave = {
            formState.validateAllFields()

            if (!formState.isValid()) return@EventTopBar

            val timestamps = formState.parseTimestamps() ?: return@EventTopBar

            val (startTs, endTs) = timestamps

            eventViewModel.saveEditedEvent(
                originalEvent = event,
                title = formState.title.value,
                description = formState.description.value,
                location = formState.gotLocation.value,
                startTs = startTs,
                endTs = endTs,
                tagsString = formState.tag.value,
                onSuccess = { onDone() },
            )
          })

      // Prominent validation banner shown right after the top bar when user attempted to save
      if (formState.showValidation.value && !formState.isValid()) {
        ValidationBanner(errorFields, EditEventScreenTestTags)
      }

      // Contenu scrollable
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).imePadding()) {
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
            testTags = EditEventScreenTestTags)

        Spacer(modifier = Modifier.padding(bottom = 5.dp))

        Text(
            "Accessibility and Price cannot be modified to avoid being unfair to participants!",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp))
      }
    }
  }
}

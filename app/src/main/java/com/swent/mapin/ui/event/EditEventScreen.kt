package com.swent.mapin.ui.event

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

  BaseEventScreen(
      modifier = modifier,
      formState = formState,
      locationViewModel = locationViewModel,
      title = "Edit Event",
      testTags = EditEventScreenTestTags,
      onCancel = onCancel,
      onCommit = { startTs, endTs ->
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
      },
      extraContent = {
        Text(
            "Accessibility and Price cannot be modified to avoid being unfair to participants!",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp))
      })
}

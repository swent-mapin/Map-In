package com.swent.mapin.ui.event

import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.swent.mapin.R
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.location.LocationViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
  override val PICK_MEDIA = "EDIT_PICK_MEDIA"
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
    locationViewModel: LocationViewModel,
    event: Event = Event(),
    onCancel: () -> Unit = {},
    onDone: () -> Unit = {},
) {

  val context = LocalContext.current

  val title = remember { mutableStateOf(event.title) }
  val description = remember { mutableStateOf(event.description) }
  val location = remember { mutableStateOf(event.location.name ?: Location.NO_NAME) }
  val mediaUri = remember { mutableStateOf<Uri?>(null) }

  val firebaseUrl: String = event.imageUrl ?: ""

  LaunchedEffect(firebaseUrl) {
    if (firebaseUrl.isNotBlank()) {
      mediaUri.value = Uri.parse(firebaseUrl)
    }
  }

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
  val gotLocation = remember { mutableStateOf(event.location) }
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
          EventTopBar(
              title = "Edit Event",
              testTags = EditEventScreenTestTags,
              isEventValid = isEventValid,
              onCancel = onCancel,
              onSave = {
                handleEditEventSave(
                    showValidation = showValidation,
                    titleError = titleError,
                    title = title,
                    descriptionError = descriptionError,
                    description = description,
                    locationError = locationError,
                    location = location,
                    dateError = dateError,
                    date = date,
                    timeError = timeError,
                    time = time,
                    endDateError = endDateError,
                    endDate = endDate,
                    endTimeError = endTimeError,
                    endTime = endTime,
                    tagError = tagError,
                    tag = tag,
                    priceError = priceError,
                    price = price,
                    validateStartEnd = { validateStartEnd() },
                    isLoggedIn = isLoggedIn,
                    eventViewModel = eventViewModel,
                    event = event,
                    context = context,
                    gotLocation = gotLocation,
                    onDone = onDone,
                    mediaUri = mediaUri)
              })

          // Prominent validation banner shown right after the top bar when user attempted to save
          if (showValidation.value && !isEventValid) {
            ValidationBanner(errorFields, EditEventScreenTestTags)
          }

          Spacer(modifier = Modifier.padding(5.dp))

          EventFormBody(
              title = title,
              titleError = titleError,
              date = date,
              dateError = dateError,
              time = time,
              timeError = timeError,
              endDate = endDate,
              endDateError = endDateError,
              endTime = endTime,
              endTimeError = endTimeError,
              location = location,
              locationError = locationError,
              locations = locations,
              gotLocation = gotLocation,
              locationExpanded = locationExpanded,
              locationViewModel = locationViewModel,
              description = description,
              descriptionError = descriptionError,
              tag = tag,
              tagError = tagError,
              testTags = EditEventScreenTestTags,
              mediaUri = mediaUri)

          Spacer(modifier = Modifier.padding(bottom = 5.dp))
          Text(
              "Accessibility and Price cannot be modified to avoid being unfair to participants!",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp))
        }
  }
}

private fun handleEditEventSave(
    showValidation: MutableState<Boolean>,
    titleError: MutableState<Boolean>,
    title: MutableState<String>,
    descriptionError: MutableState<Boolean>,
    description: MutableState<String>,
    locationError: MutableState<Boolean>,
    location: MutableState<String>,
    dateError: MutableState<Boolean>,
    date: MutableState<String>,
    timeError: MutableState<Boolean>,
    time: MutableState<String>,
    endDateError: MutableState<Boolean>,
    endDate: MutableState<String>,
    endTimeError: MutableState<Boolean>,
    endTime: MutableState<String>,
    tagError: MutableState<Boolean>,
    tag: MutableState<String>,
    priceError: MutableState<Boolean>,
    price: MutableState<String>,
    validateStartEnd: () -> Unit,
    isLoggedIn: MutableState<Boolean>,
    eventViewModel: EventViewModel,
    event: Event,
    context: android.content.Context,
    gotLocation: MutableState<Location>,
    onDone: () -> Unit,
    mediaUri: MutableState<Uri?>
) {
  // Show validation feedback when user attempts to save
  showValidation.value = true

  // update per-field error flags from current values so UI shows them immediately
  updateEditFieldErrors(
      titleError, title, descriptionError, description, locationError, location,
      dateError, date, timeError, time, endDateError, endDate, endTimeError, endTime,
      tagError, tag, priceError, price)

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
  if (!nowValid) return

  val timestamps = parseEditTimestamps(date.value, time.value, endDate.value, endTime.value)
  if (timestamps == null) {
    if (timestamps == null) dateError.value = true
    return
  }

  val (startTs, endTs) = timestamps

  if (!validateEditEndAfterStart(startTs, endTs, endDateError, endTimeError)) return

  eventViewModel.saveEditedEvent(
      originalEvent = event,
      context = context,
      title = title.value,
      description = description.value,
      location = gotLocation.value,
      startTs = startTs,
      endTs = endTs,
      tagsString = tag.value,
      onSuccess = { onDone() },
      mediaUri = mediaUri.value)
}

private fun updateEditFieldErrors(
    titleError: MutableState<Boolean>,
    title: MutableState<String>,
    descriptionError: MutableState<Boolean>,
    description: MutableState<String>,
    locationError: MutableState<Boolean>,
    location: MutableState<String>,
    dateError: MutableState<Boolean>,
    date: MutableState<String>,
    timeError: MutableState<Boolean>,
    time: MutableState<String>,
    endDateError: MutableState<Boolean>,
    endDate: MutableState<String>,
    endTimeError: MutableState<Boolean>,
    endTime: MutableState<String>,
    tagError: MutableState<Boolean>,
    tag: MutableState<String>,
    priceError: MutableState<Boolean>,
    price: MutableState<String>
) {
  titleError.value = title.value.isBlank()
  descriptionError.value = description.value.isBlank()
  locationError.value = location.value.isBlank()
  dateError.value = date.value.isBlank()
  timeError.value = time.value.isBlank()
  endDateError.value = endDate.value.isBlank()
  endTimeError.value = endTime.value.isBlank()
  tagError.value = !isValidTagInput(tag.value)
  priceError.value = !isValidPriceInput(price.value)
}

private fun parseEditTimestamps(
    date: String,
    time: String,
    endDate: String,
    endTime: String
): Pair<Timestamp, Timestamp>? {
  val sdf = SimpleDateFormat("dd/MM/yyyyHHmm", Locale.getDefault())
  sdf.timeZone = TimeZone.getDefault()

  val rawTime = if (time.contains("h")) time.replace("h", "") else time
  val rawEndTime = if (endTime.contains("h")) endTime.replace("h", "") else endTime

  val parsedStart = runCatching { sdf.parse(date + rawTime) }.getOrNull() ?: return null
  val parsedEnd = runCatching { sdf.parse(endDate + rawEndTime) }.getOrNull() ?: return null

  return Pair(Timestamp(parsedStart), Timestamp(parsedEnd))
}

private fun validateEditEndAfterStart(
    startTs: Timestamp,
    endTs: Timestamp,
    endDateError: MutableState<Boolean>,
    endTimeError: MutableState<Boolean>
): Boolean {
  if (!endTs.toDate().after(startTs.toDate())) {
    // end must be strictly after start
    // mark end date invalid (don't force changing time)
    endDateError.value = true
    endTimeError.value = false
    return false
  }
  return true
}

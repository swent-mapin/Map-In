package com.swent.mapin.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.swent.mapin.R
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.location.LocationViewModel
import com.swent.mapin.ui.map.MapConstants
import com.swent.mapin.ui.map.components.drawableToBitmap
import java.text.SimpleDateFormat
import java.util.Locale

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
    locationValidator: ((String) -> Boolean)? = null,
    locationSuggestions: List<Location> = emptyList(),
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
          val isValid =
              locationValidator?.invoke(textField.value)
                  ?: isValidLocation(textField.value, locationSuggestions)
          error.value = !isValid
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
    locationViewModel: LocationViewModel,
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
  val gotLocation = remember { mutableStateOf(Location.UNDEFINED) }
  val manualLocation = remember { mutableStateOf<Location?>(null) }
  val showLocationPicker = remember { mutableStateOf(false) }
  val locations by locationViewModel.locations.collectAsState()

  if (showLocationPicker.value) {
    ManualLocationPickerDialog(
        initialLocation = manualLocation.value ?: gotLocation.value,
        onDismiss = { showLocationPicker.value = false },
        onLocationPicked = { picked ->
          manualLocation.value = picked
          gotLocation.value = picked
          val label = formatPinnedLocationLabel(picked.latitude!!, picked.longitude!!)
          location.value = label
          locationError.value = false
          locationExpanded.value = false
          showLocationPicker.value = false
        })
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
          if (timeError.value || time.value.isBlank()) stringResource(R.string.time) else null,
          if (endDateError.value || endDate.value.isBlank()) "End date" else null,
          if (endTimeError.value || endTime.value.isBlank()) "End time" else null,
          if (locationError.value || location.value.isBlank())
              stringResource(R.string.location_field)
          else null,
          if (descriptionError.value || description.value.isBlank())
              stringResource(R.string.description_field)
          else null,
          if (tagError.value) stringResource(R.string.tag_field) else null,
          if (priceError.value) stringResource(R.string.price_field) else null)

  val isEventValid = !error && isLoggedIn.value
  val showValidation = remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Scaffold(contentWindowInsets = WindowInsets.ime) { padding ->
    Column(
        modifier =
            modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .navigationBarsPadding()) {
          // TopBar
          EventTopBar(
              title = "New Event",
              testTags = AddEventScreenTestTags,
              isEventValid = isEventValid,
              onCancel = onCancel,
              onSave = {
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
                validateStartEndLogic(
                    date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

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
                if (!nowValid) return@EventTopBar

                val sdf = SimpleDateFormat("dd/MM/yyyyHHmm", Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getDefault()
                val rawTime =
                    if (time.value.contains("h")) time.value.replace("h", "") else time.value
                val rawEndTime =
                    if (endTime.value.contains("h")) endTime.value.replace("h", "")
                    else endTime.value

                val parsedStart = runCatching { sdf.parse(date.value + rawTime) }.getOrNull()
                val parsedEnd = runCatching { sdf.parse(endDate.value + rawEndTime) }.getOrNull()

                if (parsedStart == null) {
                  dateError.value = true
                  return@EventTopBar
                }
                if (parsedEnd == null) {
                  endDateError.value = true
                  return@EventTopBar
                }

                val startTs = Timestamp(parsedStart)
                val endTs = Timestamp(parsedEnd)

                if (!endTs.toDate().after(startTs.toDate())) {
                  // end must be strictly after start
                  // mark end date invalid (don't force changing time)
                  endDateError.value = true
                  endTimeError.value = false
                  return@EventTopBar
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
              })
          // Prominent validation banner shown right after the top bar when user attempted to save
          if (showValidation.value && !isEventValid) {
            ValidationBanner(errorFields, AddEventScreenTestTags)
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
              manualLocation = manualLocation,
              onPickLocationOnMap = { showLocationPicker.value = true },
              description = description,
              descriptionError = descriptionError,
              tag = tag,
              tagError = tagError,
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

private fun formatPinnedLocationLabel(lat: Double, lng: Double): String {
  return "Pinned location (${String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng)})"
}

@OptIn(MapboxDelicateApi::class)
@Composable
private fun ManualLocationPickerDialog(
    initialLocation: Location?,
    onDismiss: () -> Unit,
    onLocationPicked: (Location) -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val startPoint =
      initialLocation?.let { loc ->
        if (loc.latitude != null && loc.longitude != null) {
          Point.fromLngLat(loc.longitude, loc.latitude)
        } else null
      } ?: Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE)

  val initialPickedPoint =
      initialLocation?.let { loc ->
        if (loc.latitude != null && loc.longitude != null) {
          Point.fromLngLat(loc.longitude, loc.latitude)
        } else null
      }
  var pickedPoint by remember(startPoint) { mutableStateOf<Point?>(initialPickedPoint) }
  val mapViewportState = rememberMapViewportState {
    setCameraOptions {
      center(startPoint)
      zoom(MapConstants.DEFAULT_ZOOM.toDouble())
    }
  }
  val standardStyleState = rememberStandardStyleState()
  val markerBitmap = remember(context) { context.drawableToBitmap(R.drawable.ic_map_marker) }

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 6.dp) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Select location on map", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
              val annotations =
                  remember(pickedPoint, markerBitmap) {
                    pickedPoint?.let { point ->
                      val opts = PointAnnotationOptions().withPoint(point)
                      markerBitmap?.let { opts.withIconImage(it) }
                      listOf(opts)
                    } ?: emptyList()
                  }
              MapboxMap(
                  modifier = Modifier.fillMaxWidth().height(360.dp),
                  mapViewportState = mapViewportState,
                  style = { MapboxStandardStyle(standardStyleState = standardStyleState) }) {
                    MapEffect(Unit) { mapView ->
                      val listener = OnMapClickListener { point ->
                        pickedPoint = point
                        true
                      }
                      mapView.gestures.addOnMapClickListener(listener)
                    }

                    if (annotations.isNotEmpty()) {
                      PointAnnotationGroup(annotations = annotations) {
                        iconAnchor = IconAnchor.BOTTOM
                      }
                    }
                  }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                  TextButton(onClick = onDismiss) { Text(text = "Cancel") }
                  Spacer(modifier = Modifier.width(8.dp))
                  Button(
                      onClick = {
                        pickedPoint?.let { point ->
                          val lat = point.latitude()
                          val lng = point.longitude()
                          val label = formatPinnedLocationLabel(lat, lng)
                          val loc = Location.from(label, lat, lng)
                          onLocationPicked(loc)
                        }
                      },
                      enabled = pickedPoint != null) {
                        Text(text = "Use this location")
                      }
                }
          }
        }
      }
}

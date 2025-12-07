package com.swent.mapin.ui.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.PopupProperties
import com.swent.mapin.R
import com.swent.mapin.model.Location
import com.swent.mapin.model.location.LocationViewModel

@Composable
fun LocationDropDownMenu(
    location: MutableState<String>,
    locationError: MutableState<Boolean>,
    locationViewModel: LocationViewModel,
    testTag: EventScreenTestTag,
    expanded: MutableState<Boolean>,
    locations: List<Location>,
    gotLocation: MutableState<Location>,
) {
  Column {
    AddEventTextField(
        location,
        locationError,
        stringResource(R.string.location_place_holder),
        isLocation = true,
        modifier = Modifier.testTag(testTag.INPUT_EVENT_LOCATION),
        locationQuery = {
          locationViewModel.onQueryChanged(location.value)
          expanded.value = true
          locationError.value = !isValidLocation(location.value, locations)
        },
        singleLine = true)
    DropdownMenu(
        properties =
            PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        expanded = expanded.value && locations.isNotEmpty(),
        onDismissRequest = { expanded.value = false },
        modifier = Modifier.fillMaxWidth()) {
          locations.forEachIndexed { index, loc ->
            DropdownMenuItem(
                modifier = Modifier.testTag("locationItem$index"),
                text = { Text(loc.name) },
                onClick = {
                  location.value = loc.name
                  gotLocation.value = loc
                  expanded.value = false
                  locationError.value = false
                })
          }
        }
  }
}

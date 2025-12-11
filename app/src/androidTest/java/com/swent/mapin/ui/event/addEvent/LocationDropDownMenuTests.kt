package com.swent.mapin.ui.event.addEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.location.LocationViewModel
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.LocationDropDownMenu
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationDropDownMenuTests {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testTypingAndSelectingLocation() {
    val location = mutableStateOf("")
    val locationError = mutableStateOf(false)
    val expanded = mutableStateOf(false)
    val gotLocation = mutableStateOf(Location.from("Initial", 0.0, 0.0))
    val locations = listOf(Location.from("Paris", 0.0, 0.0), Location.from("Tokyo", 0.0, 0.0))
    val fakeViewModel = mockk<LocationViewModel>(relaxed = true)

    composeTestRule.setContent {
      LocationDropDownMenu(
          location = location,
          locationError = locationError,
          locationViewModel = fakeViewModel,
          testTag = AddEventScreenTestTags,
          expanded = expanded,
          locations = locations,
          gotLocation = gotLocation)
    }

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput("Par")

    verify { fakeViewModel.onQueryChanged("Par") }

    composeTestRule.onNodeWithText("Paris").performClick()

    assertEquals("Paris", location.value)
    assertEquals(false, expanded.value)
    assertEquals(false, locationError.value)
    assertEquals("Paris", gotLocation.value.name)
  }
}

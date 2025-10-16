package com.swent.mapin.ui.components.AddEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.github.se.bootcamp.ui.map.LocationViewModel
import com.swent.mapin.model.Location
import com.swent.mapin.ui.components.AddEventPopUpTestTags
import com.swent.mapin.ui.components.LocationDropDownMenu
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
    val gotLocation = mutableStateOf(Location("Initial", 0.0, 0.0))
    val locations = listOf(Location("Paris", 0.0, 0.0), Location("Tokyo", 0.0, 0.0))
    val fakeViewModel = mockk<LocationViewModel>(relaxed = true)

    composeTestRule.setContent {
      LocationDropDownMenu(
          location = location,
          locationError = locationError,
          locationViewModel = fakeViewModel,
          expanded = expanded,
          locations = locations,
          gotLocation = gotLocation)
    }

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_LOCATION)
        .performTextInput("Par")

    verify { fakeViewModel.onQueryChanged("Par") }

    composeTestRule.onNodeWithText("Paris").performClick()

    assertEquals("Paris", location.value)
    assertEquals(false, expanded.value)
    assertEquals(false, locationError.value)
    assertEquals("Paris", gotLocation.value.name)
  }
}

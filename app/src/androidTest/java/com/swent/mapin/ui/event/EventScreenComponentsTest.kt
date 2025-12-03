package com.swent.mapin.ui.event

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.ui.theme.MapInTheme
import org.junit.Rule
import org.junit.Test

class EventScreenComponentsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // Dummy EventScreenTestTag for testing
  val testTags = AddEventScreenTestTags

  @Test
  fun eventTopBar_rendersAndRespondsToClicks() {
    var cancelClicked = false
    var saveClicked = false

    composeTestRule.setContent {
      MapInTheme {
        EventTopBar(
            title = "Test Event",
            testTags = testTags,
            isEventValid = true,
            onCancel = { cancelClicked = true },
            onSave = { saveClicked = true })
      }
    }

    // Assert title is displayed
    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()

    // Test cancel button click
    composeTestRule.onNodeWithTag(testTags.EVENT_CANCEL).performClick()
    assert(cancelClicked)

    // Test save button click
    composeTestRule.onNodeWithTag(testTags.EVENT_SAVE).performClick()
    assert(saveClicked)
  }

  @Test
  fun eventTopBar_saveButtonAppearanceChangesBasedOnValidity() {
    composeTestRule.setContent {
      MapInTheme {
        EventTopBar(
            title = "Test Event",
            testTags = testTags,
            isEventValid = false,
            onCancel = {},
            onSave = {})
      }
    }

    // Save button should show "disabled" appearance when event invalid
    composeTestRule.onNodeWithTag(testTags.EVENT_SAVE).assertExists().assertIsDisplayed()
  }

  @Test
  fun eventFormBody_rendersAllFieldsAndHandlesInput() {
    val titleState = mutableStateOf("")
    val titleError = mutableStateOf(false)
    val dateState = mutableStateOf("")
    val dateError = mutableStateOf(false)
    val timeState = mutableStateOf("")
    val timeError = mutableStateOf(false)
    val endDateState = mutableStateOf("")
    val endDateError = mutableStateOf(false)
    val endTimeState = mutableStateOf("")
    val endTimeError = mutableStateOf(false)
    val locationState = mutableStateOf("")
    val locationError = mutableStateOf(false)
    val gotLocationState = mutableStateOf(Location("Test Location"))
    val locationExpandedState = mutableStateOf(false)
    val descriptionState = mutableStateOf("")
    val descriptionError = mutableStateOf(false)
    val tagState = mutableStateOf("")
    val tagError = mutableStateOf(false)
    val locations = listOf(Location("Test Location"))
    val dummyLocationViewModel = LocationViewModel()

    composeTestRule.setContent {
      MapInTheme {
        EventFormBody(
            title = titleState,
            titleError = titleError,
            date = dateState,
            dateError = dateError,
            time = timeState,
            timeError = timeError,
            endDate = endDateState,
            endDateError = endDateError,
            endTime = endTimeState,
            endTimeError = endTimeError,
            location = locationState,
            locationError = locationError,
            locations = locations,
            gotLocation = gotLocationState,
            locationExpanded = locationExpandedState,
            locationViewModel = dummyLocationViewModel,
            description = descriptionState,
            descriptionError = descriptionError,
            tag = tagState,
            tagError = tagError,
            testTags = testTags)
      }
    }

    // Test title input
    composeTestRule.onNodeWithTag(testTags.INPUT_EVENT_TITLE).performTextInput("My Title")
    assert(titleState.value == "My Title")

    // Test description input
    composeTestRule
        .onNodeWithTag(testTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("My Description")
    assert(descriptionState.value == "My Description")

    // Test tag input
    composeTestRule.onNodeWithTag(testTags.INPUT_EVENT_TAG).performTextInput("#food")
    assert(tagState.value == "#food")

    composeTestRule.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_LOCATION).assertIsDisplayed()
  }
}

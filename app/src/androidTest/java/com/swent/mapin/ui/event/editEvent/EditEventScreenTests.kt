package com.swent.mapin.ui.event.editEvent

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.ui.event.EditEventScreen
import com.swent.mapin.ui.event.EditEventScreenTestTags
import com.swent.mapin.ui.event.EventViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class EditEventScreenTests {
  @get:Rule val composeTestRule = createComposeRule()

  lateinit var eventViewModel: EventViewModel
  lateinit var locationViewModel: LocationViewModel

  private fun setEditEventScreen(onCancel: () -> Unit = {}, onDone: () -> Unit = {}) {
    val testLocations = listOf(Location("Test", 0.0, 0.0))
    val locationFlow = MutableStateFlow(testLocations)

    locationViewModel = mockk(relaxed = true)
    every { locationViewModel.locations } returns locationFlow

    eventViewModel = mockk(relaxed = true)

    composeTestRule.setContent {
      EditEventScreen(
          eventViewModel = eventViewModel,
          locationViewModel = locationViewModel,
          onCancel = onCancel,
          onDone = onDone)
    }
  }

  @Test
  fun displaysAllFields() {
    setEditEventScreen()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_CANCEL).assertIsDisplayed()
  }

  @Test
  fun cancelButtonWorks() {
    var cancelled = false
    setEditEventScreen(onCancel = { cancelled = true })
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_CANCEL).performClick()
    assert(cancelled)
  }

  @Test
  fun saveDoesNotCallOnDoneWhenErrorsExist() {
    var doneCalled = false
    setEditEventScreen(onDone = { doneCalled = true })

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).performClick()
    assert(!doneCalled)
  }

  @Test
  fun emptyFieldsShowValidationError() {
    setEditEventScreen()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).performClick()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun invalidTagInputShowsError() {
    setEditEventScreen()
    val tagNode = composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TAG)
    tagNode.performTextInput("invalid tag !!")
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).performClick()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE)
        .assert(hasText("Tags", substring = true, ignoreCase = true))
  }
}

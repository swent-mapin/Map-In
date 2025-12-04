package com.swent.mapin.ui.event.editEvent

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.event.EditEventScreen
import com.swent.mapin.ui.event.EditEventScreenTestTags
import com.swent.mapin.ui.event.EventViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

// Assisted by ChatGPT

class EditEventScreenTests {
  @get:Rule val composeTestRule = createComposeRule()

  lateinit var eventViewModel: EventViewModel
  lateinit var locationViewModel: LocationViewModel

  private val testEvent =
      Event(
          uid = "123",
          title = "Test Event",
          description = "Test Description",
          location = Location("Test Location", 0.0, 0.0),
          date = Timestamp.now(),
          endDate = Timestamp.now(),
          tags = listOf("tag1", "tag2"),
          price = 10.0,
          ownerId = "owner123")

  private fun setEditEventScreen(onCancel: () -> Unit = {}, onDone: () -> Unit = {}) {
    val testLocations = listOf(Location("Test", 0.0, 0.0))
    val locationFlow = MutableStateFlow(testLocations)

    locationViewModel = mockk(relaxed = true)
    every { locationViewModel.locations } returns locationFlow

    eventViewModel = mockk(relaxed = true)
    val eventFlow = MutableStateFlow(testEvent)
    every { eventViewModel.eventToEdit } returns eventFlow

    composeTestRule.setContent {
      EditEventScreen(
          eventViewModel = eventViewModel,
          locationViewModel = locationViewModel,
          onCancel = onCancel,
          onDone = onDone,
          event = testEvent)
    }
  }

  @Test
  fun displaysAllFields() {
    setEditEventScreen()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TAG)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.EVENT_CANCEL)
        .performScrollTo()
        .assertIsDisplayed()
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
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE)
        .performScrollTo()
        .performClick()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun invalidTagInputShowsError() {
    setEditEventScreen()
    val tagNode = composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TAG)
    tagNode.performScrollTo().performTextInput("invalid tag !!")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE)
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE)
        .performScrollTo()
        .assert(hasText("Tags", substring = true, ignoreCase = true))
  }

  @Test
  fun fieldsLoadExistingEventValues() {
    setEditEventScreen()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .performScrollTo()
        .assertTextEquals(testEvent.title)

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performScrollTo()
        .assertTextEquals(testEvent.description)

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TAG)
        .performScrollTo()
        .assertTextEquals(testEvent.tags.joinToString(" "))
  }

  @Test
  fun topBarRemainsStickyWhenScrolling() {
    setEditEventScreen()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_CANCEL).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TAG)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_CANCEL).assertIsDisplayed()
  }

  @Test
  fun validationBannerRemainsStickyWhenScrolling() {
    setEditEventScreen()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TAG)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsDisplayed()
  }
}

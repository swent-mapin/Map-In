package com.swent.mapin.ui.components.AddEvent

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.components.AddEventPopUp
import com.swent.mapin.ui.components.AddEventPopUpTestTags
import com.swent.mapin.ui.components.EventViewModel
import com.swent.mapin.ui.components.saveEvent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddEventPopUpTests {
  private var saveClicked = false
  private var cancelClicked = false
  private var backClicked = false
  private var dismissCalled = false

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    composeTestRule.setContent {
      AddEventPopUp(
          onBack = { backClicked = true },
          onDismiss = { dismissCalled = true },
          onDone = { saveClicked = true },
          onCancel = { cancelClicked = true })
    }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.EVENT_SAVE)
        .performScrollTo()
        .assertTextContains("Save", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.EVENT_CANCEL)
        .performScrollTo()
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TAG)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_DATE)
        .performScrollTo()
        .assertTextContains("Select Date:", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_TIME)
        .performScrollTo()
        .assertTextContains("Select Time:", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_LOCATION)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun showsErrorMessageInitially() {
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun nonEmptyTitleRemovesTitleError() {
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE)
        .performTextInput("This is a valid title")
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE)
        .assert(!hasText("Title", substring = true, ignoreCase = true))
  }

  @Test
  fun nonEmptyDescriptionRemovesDescriptionError() {
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("This is a valid Description")
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE)
        .assert(!hasText("Description", substring = true, ignoreCase = true))
  }

  @Test
  fun clickingCancelInvokesCallback() {
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.EVENT_CANCEL)
        .performScrollTo()
        .performClick()
    assert(cancelClicked)
  }

  @Test
  fun publicPrivateSwitchTogglesCorrectly() {

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PUBLIC_TEXT)
        .performScrollTo()
        .assertTextContains("Public", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PUBLIC_SWITCH)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PUBLIC_TEXT)
        .performScrollTo()
        .assertTextContains("Private", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PUBLIC_SWITCH)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PUBLIC_TEXT)
        .performScrollTo()
        .assertTextContains("Public", substring = true, ignoreCase = true)
  }

  @Test
  fun clickingCloseButtonInvokesBackCallback() {
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    assert(backClicked)
  }

  @Test
  fun datePickerButtonDisplaysDefaultText() {
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_DATE)
        .assertTextContains("Select Date:", substring = true, ignoreCase = true)
  }

  @Test
  fun timePickerButtonDisplaysDefaultText() {
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_TIME)
        .assertTextContains("Select Time:", substring = true, ignoreCase = true)
  }

  @Test
  fun tagInputValidationWorks() {
    val tagNode = composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TAG)
    tagNode.performTextInput("Invalid Tag !!")
    tagNode.assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE)
        .assertTextContains("Tag", substring = true, ignoreCase = true)
  }

  @Test
  fun tagValidSpaceInputValidationWorks() {
    val tagNode = composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TAG)
    tagNode.performTextInput("#ValidTag #ValidTag2")
    tagNode.assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE)
        .assert(!hasText("Tag", substring = true, ignoreCase = true))
  }

  @Test
  fun tagValiCommaInputValidationWorks() {
    val tagNode = composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TAG)
    tagNode.performTextInput("#ValidTag, #ValidTag2")
    tagNode.assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE)
        .assert(!hasText("Tag", substring = true, ignoreCase = true))
  }

  @Test
  fun invalidInputsKeepSaveButtonDisabled() {
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE).performTextInput("")
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("This is a valid description")
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.EVENT_SAVE).assertIsNotEnabled()
  }
}

class SaveEventTests {

  @Test
  fun saveEventTest() {
    val mockViewModel = mockk<EventViewModel>(relaxed = true)
    val testLocation = Location("Test Location", 0.0, 0.0)
    val testTitle = "Test Event"
    val testDescription = "Some description"
    val testTags = listOf("tag1", "tag2")
    val isPublic = true

    var onDoneCalled = false
    val onDone = { onDoneCalled = true }

    saveEvent(
        viewModel = mockViewModel,
        title = testTitle,
        description = testDescription,
        location = testLocation,
        tags = testTags,
        isPublic = isPublic,
        onDone = onDone)

    verify { mockViewModel.addEvent(any<Event>()) }
    assert(onDoneCalled)
  }
}

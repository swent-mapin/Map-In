package com.swent.mapin.ui.AddEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.swent.mapin.ui.AddEventPopUp
import com.swent.mapin.ui.AddEventPopUpTestTags
import com.swent.mapin.ui.FutureDatePickerButton
import com.swent.mapin.ui.TimePickerButton
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddEventPopUpTests {
  private var saveClicked = false
  private var cancelClicked = false
  private var backClicked = false
  private var dismissCalled = false

  @get:Rule
  val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    composeTestRule.setContent {
        AddEventPopUp(
            onBack = { backClicked = true },
            onDismiss = { dismissCalled = true },
            onSave = { saveClicked = true },
            onCancel = { cancelClicked = true })
    }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.EVENT_SAVE)
        .assertTextContains("Save", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.EVENT_CANCEL)
        .assertTextContains("Cancel", substring = true, ignoreCase = true)
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_DATE)
        .assertTextContains("Select Date:", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_TIME)
        .assertTextContains("Select Time:", substring = true, ignoreCase = true)
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_LOCATION).assertIsDisplayed()
  }

  @Test
  fun showsErrorMessageInitially() {
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun nonEmptyTitleRemovesTitleError() {
      composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
      composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_TITLE).performTextInput("This is a valid title")
      composeTestRule.onNodeWithTag(AddEventPopUpTestTags.ERROR_MESSAGE).assert(!hasText("Title", substring = true, ignoreCase = true))
  }

  @Test
  fun nonEmptyDescriptionRemovesDescriptionError() {
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION).performTextInput("This is a valid Description")
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION).assert(!hasText("Description", substring = true, ignoreCase = true))
  }

  @Test
  fun clickingCancelInvokesCallback() {
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.EVENT_CANCEL).performClick()
    assert(cancelClicked)
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
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.INPUT_EVENT_DESCRIPTION).performTextInput("This is a valid description")
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.EVENT_SAVE).assertIsNotEnabled()
  }
}
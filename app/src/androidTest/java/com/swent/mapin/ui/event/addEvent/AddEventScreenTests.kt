package com.swent.mapin.ui.event.addEvent

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.event.AddEventScreen
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.event.saveEvent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddEventScreenTests {
  private var saveClicked = false
  private var cancelClicked = false

  @get:Rule val composeTestRule = createComposeRule()
  val eventViewModel = mockk<EventViewModel>(relaxed = true)

  @Before
  fun setUp() {
    composeTestRule.setContent {
      AddEventScreen(
          onDone = { saveClicked = true },
          eventViewModel = eventViewModel,
          onCancel = { cancelClicked = true })
    }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.EVENT_SAVE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.EVENT_CANCEL)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TAG)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE)
        .performScrollTo()
        .assertTextContains("Select Date:", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME)
        .performScrollTo()
        .assertTextContains("Select Time:", substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_PRICE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_LOCATION)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun doesNotShowErrorMessageInitially() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE).assertIsNotDisplayed()
  }

  @Test
  fun dateAndTimeErrorMessageShowsInitially() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.DATE_TIME_ERROR).assertIsDisplayed()
  }

  @Test
  fun nonEmptyTitleRemovesTitleError() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("a")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextClearance()
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("a")
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("This is a valid title")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE)
        .assert(!hasText("Title", substring = true, ignoreCase = true))
  }

  @Test
  fun nonEmptyDescriptionRemovesDescriptionError() {
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("a")
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("a")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("This is a valid Description")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE)
        .assert(!hasText("Description", substring = true, ignoreCase = true))
  }

  @Test
  fun clickingCancelInvokesCallback() {
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.EVENT_CANCEL)
        .performScrollTo()
        .performClick()
    assert(cancelClicked)
  }

  @Test
  fun publicPrivateSwitchTogglesCorrectly() {

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PUBLIC_TEXT)
        .performScrollTo()
        .assertTextContains("Public", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PUBLIC_SWITCH)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PUBLIC_TEXT)
        .performScrollTo()
        .assertTextContains("Private", substring = true, ignoreCase = true)

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PUBLIC_SWITCH)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PUBLIC_TEXT)
        .performScrollTo()
        .assertTextContains("Public", substring = true, ignoreCase = true)
  }

  @Test
  fun datePickerButtonDisplaysDefaultText() {
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE)
        .assertTextContains("Select Date:", substring = true, ignoreCase = true)
  }

  @Test
  fun timePickerButtonDisplaysDefaultText() {
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME)
        .assertTextContains("Select Time:", substring = true, ignoreCase = true)
  }

  @Test
  fun tagInputValidationWorks() {
    val tagNode = composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TAG)
    tagNode.performTextInput("Invalid Tag !!")
    tagNode.assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE)
        .assertTextContains("Tag", substring = true, ignoreCase = true)
  }

  @Test
  fun tagValidSpaceInputValidationWorks() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("a")
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    val tagNode = composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TAG)
    tagNode.assertIsDisplayed()
    tagNode.performTextInput("InvalidTag")
    tagNode.performTextClearance()
    tagNode.performTextInput("#ValidTag #ValidTag2")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE)
        .assert(!hasText("Tag", substring = true, ignoreCase = true))
  }

  @Test
  fun tagValiCommaInputValidationWorks() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("a")
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    val tagNode = composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TAG)
    tagNode.assertIsDisplayed()
    tagNode.performTextInput("InvalidTag")
    tagNode.performTextClearance()
    tagNode.performTextInput("#ValidTag, #ValidTag2")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE)
        .assert(!hasText("Tag", substring = true, ignoreCase = true))
  }

  @Test
  fun validPriceInputWorks() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("a")
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    val tagNode = composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_PRICE)
    tagNode.performScrollTo()
    tagNode.assertIsDisplayed()
    tagNode.performTextInput("InvalidPrice")
    tagNode.performTextClearance()
    tagNode.performTextInput("3.0")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE)
        .assert(!hasText("Price", substring = true, ignoreCase = true))
  }

  @Test
  fun invalidPriceInputDoesNotWork() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("a")
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    val tagNode = composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_PRICE)
    tagNode.performScrollTo()
    tagNode.assertIsDisplayed()
    tagNode.performTextInput("InvalidPrice")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE)
        .assert(hasText("Price", substring = true, ignoreCase = true))
  }

  @Test
  fun invalidInputsKeepSaveButtonDisabled() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("")
    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("This is a valid description")
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.EVENT_SAVE).assertIsNotEnabled()
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
    val price = 0.0
    val currentUserId = "FakeUserId"

    var onDoneCalled = false
    val onDone = { onDoneCalled = true }

    saveEvent(
        viewModel = mockViewModel,
        title = testTitle,
        description = testDescription,
        date = Timestamp(10000, 200),
        location = testLocation,
        currentUserId = currentUserId,
        tags = testTags,
        isPublic = isPublic,
        onDone = onDone,
        price = price)

    verify { mockViewModel.addEvent(any<Event>()) }
    assert(onDoneCalled)
  }

  @Test
  fun saveEvent_whenUserNotLoggedIn_doesNotCallAddEventOrOnDone() {
    val mockViewModel = mockk<EventViewModel>(relaxed = true)
    val testLocation = Location("Test Location", 0.0, 0.0)
    val testTitle = "Test Event"
    val testDescription = "Some description"
    val testTags = listOf("tag1", "tag2")
    val isPublic = true
    val price = 0.0

    val currentUserId: String? = null // simulate not logged in

    var onDoneCalled = false
    val onDone = { onDoneCalled = true }

    saveEvent(
        viewModel = mockViewModel,
        title = testTitle,
        description = testDescription,
        date = Timestamp(10000, 200),
        location = testLocation,
        currentUserId = currentUserId,
        tags = testTags,
        isPublic = isPublic,
        onDone = onDone,
        price = price)

    verify(exactly = 0) { mockViewModel.addEvent(any<Event>()) } // should NOT be called
    assert(!onDoneCalled)
  }
}

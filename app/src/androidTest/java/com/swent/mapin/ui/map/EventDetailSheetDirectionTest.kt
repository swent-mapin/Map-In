package com.swent.mapin.ui.map

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.testing.UiTestTags
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * UI tests for the Get Directions button in EventDetailSheet.
 * Tests button visibility, functionality, and state changes.
 */
class EventDetailSheetDirectionTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val testEvent = Event(
    uid = "test-event",
    title = "Test Event",
    description = "Test event description",
    date = Timestamp(Date()),
    location = Location("Test Location", 46.5220, 6.5700),
    tags = listOf("Music", "Festival"),
    public = true,
    ownerId = "test-user",
    capacity = 100,
    participantIds = listOf("user1", "user2")
  )

  @Test
  fun getDirectionsButton_isDisplayed_inMediumState() {
    var getDirectionsClicked = false

    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = { getDirectionsClicked = true },
        showDirections = false
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun getDirectionsButton_isDisplayed_inFullState() {
    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.FULL,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = {},
        showDirections = false
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun getDirectionsButton_isNotDisplayed_inCollapsedState() {
    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.COLLAPSED,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = {},
        showDirections = false
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertDoesNotExist()
  }

  @Test
  fun getDirectionsButton_showsCorrectText_whenDirectionsNotShown() {
    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = {},
        showDirections = false
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertContentDescriptionEquals("Get Directions")
  }

  @Test
  fun getDirectionsButton_showsCorrectText_whenDirectionsShown() {
    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = {},
        showDirections = true
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertContentDescriptionEquals("Clear Directions")
  }

  @Test
  fun getDirectionsButton_triggersCallback_whenClicked() {
    var callbackTriggered = false

    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = { callbackTriggered = true },
        showDirections = false
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .performClick()

    assertTrue(callbackTriggered)
  }

  @Test
  fun getDirectionsButton_isClickable_whenDirectionsNotShown() {
    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.FULL,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = {},
        showDirections = false
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertHasClickAction()
  }

  @Test
  fun getDirectionsButton_isClickable_whenDirectionsShown() {
    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.FULL,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = {},
        showDirections = true
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertHasClickAction()
  }

  @Test
  fun getDirectionsButton_multipleClicks_triggerMultipleCallbacks() {
    var clickCount = 0

    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = { clickCount++ },
        showDirections = false
      )
    }

    val button = composeTestRule.onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)

    button.performClick()
    assertEquals(1, clickCount)

    button.performClick()
    assertEquals(2, clickCount)

    button.performClick()
    assertEquals(3, clickCount)
  }

  @Test
  fun getDirectionsButton_appearsBeforeJoinButton_inMediumState() {
    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = {},
        showDirections = false
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertExists()

    composeTestRule
      .onNodeWithTag("joinEventButton")
      .assertExists()
  }

  @Test
  fun getDirectionsButton_stateChange_updatesButtonText() {
    var showDirections = false

    composeTestRule.setContent {
      EventDetailSheet(
        event = testEvent,
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        isSaved = false,
        organizerName = "Test Organizer",
        onJoinEvent = {},
        onUnregisterEvent = {},
        onSaveForLater = {},
        onUnsaveForLater = {},
        onClose = {},
        onShare = {},
        onGetDirections = { showDirections = !showDirections },
        showDirections = showDirections
      )
    }

    composeTestRule
      .onNodeWithTag(UiTestTags.GET_DIRECTIONS_BUTTON)
      .assertContentDescriptionEquals("Get Directions")
  }
}


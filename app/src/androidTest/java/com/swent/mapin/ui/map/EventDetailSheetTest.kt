package com.swent.mapin.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import java.util.Calendar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Assisted by AI
class EventDetailSheetTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testEvent =
      Event(
          uid = "test-event-1",
          title = "Test Event",
          description = "This is a test event description",
          location = Location(name = "Paris", latitude = 48.8566, longitude = 2.3522),
          date = Timestamp(Calendar.getInstance().apply { set(2025, 9, 20, 14, 30) }.time),
          ownerId = "owner123",
          participantIds = listOf("user1", "user2"),
          attendeeCount = 2,
          capacity = 10,
          tags = listOf("Music", "Concert"),
          imageUrl = "https://example.com/image.jpg",
          url = "https://example.com/event")

  @Test
  fun collapsedState_displaysOnlyTitleAndFirstTag() {
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.COLLAPSED,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventDetailSheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleCollapsed").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleCollapsed").assertTextEquals("Test Event")
    composeTestRule.onNodeWithTag("eventTagCollapsed").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTagCollapsed").assertTextEquals("Music")
  }

  @Test
  fun collapsedState_withNoTags_doesNotDisplayTag() {
    val eventNoTags = testEvent.copy(tags = emptyList())
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoTags,
          sheetState = BottomSheetState.COLLAPSED,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventTitleCollapsed").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTagCollapsed").assertDoesNotExist()
  }

  @Test
  fun mediumState_displaysAllRequiredInformation() {
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventTitleMedium").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleMedium").assertTextEquals("Test Event")
    composeTestRule.onNodeWithTag("eventDate").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTags").assertIsDisplayed()
    // Le texte affiché est "Music, Concert" sans crochets
    composeTestRule.onNodeWithTag("eventTags").assertTextEquals("Music, Concert")
    composeTestRule.onNodeWithTag("eventLocation").assertIsDisplayed()
    // Vérifier que le texte contient "Paris" - le format exact peut varier
    composeTestRule.onNodeWithTag("eventLocation").assertTextEquals("📍 Paris")
    composeTestRule.onNodeWithTag("eventDescriptionPreview").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCount").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCount").assertTextEquals("2 / 10 attendees")
  }

  @Test
  fun mediumState_notParticipating_showsJoinButton() {
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("joinEventButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("unregisterButton").assertDoesNotExist()
  }

  @Test
  fun mediumState_participating_showsUnregisterButton() {
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = true,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("unregisterButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButton").assertDoesNotExist()
  }

  @Test
  fun mediumState_eventAtCapacity_disablesJoinButton() {
    val fullEvent = testEvent.copy(attendeeCount = 10, capacity = 10)
    composeTestRule.setContent {
      EventDetailSheet(
          event = fullEvent,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("joinEventButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButton").assertIsNotEnabled()
  }

  @Test
  fun mediumState_joinButtonClick_triggersCallback() {
    var joinCalled = false
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = { joinCalled = true },
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("joinEventButton").performClick()
    assertTrue(joinCalled)
  }

  @Test
  fun mediumState_unregisterButtonClick_triggersCallback() {
    var unregisterCalled = false
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = true,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = { unregisterCalled = true },
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("unregisterButton").performClick()
    assertTrue(unregisterCalled)
  }

  @Test
  fun fullState_displaysAllInformation() {
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventImage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleFull").assertTextEquals("Test Event")
    composeTestRule.onNodeWithTag("eventDateFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTagsFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("organizerName").assertIsDisplayed()
    composeTestRule.onNodeWithTag("organizerName").assertTextEquals("John Doe")
    composeTestRule.onNodeWithTag("eventLocationFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCountFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCountFull").assertTextEquals("2 / 10 attendees")
    composeTestRule.onNodeWithTag("eventDescription").assertIsDisplayed()
  }

  @Test
  fun fullState_noImage_displaysPlaceholder() {
    val eventNoImage = testEvent.copy(imageUrl = null)
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoImage,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventImage").assertIsDisplayed()
    composeTestRule.onNodeWithText("No image available").assertIsDisplayed()
  }

  @Test
  fun fullState_noDescription_doesNotShowDescriptionSection() {
    val eventNoDescription = testEvent.copy(description = "")
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoDescription,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventDescription").assertDoesNotExist()
  }

  @Test
  fun fullState_notParticipating_showsJoinAndSaveButtons() {
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("joinEventButtonFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("saveForLaterButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("unregisterButtonFull").assertDoesNotExist()
  }

  @Test
  fun fullState_participating_showsUnregisterButton() {
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.FULL,
          isParticipating = true,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("unregisterButtonFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButtonFull").assertDoesNotExist()
  }

  @Test
  fun fullState_saveForLaterButtonClick_triggersCallback() {
    var saveCalled = false
    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = { saveCalled = true },
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("saveForLaterButton").performClick()
    assertTrue(saveCalled)
  }

  @Test
  fun allStates_shareButtonClick_triggersCallback() {
    var shareCalled = false

    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.COLLAPSED,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = { shareCalled = true })
    }

    composeTestRule.onNodeWithTag("shareButton").performClick()
    assertTrue(shareCalled)
  }

  @Test
  fun allStates_closeButtonClick_triggersCallback() {
    var closeCalled = false

    composeTestRule.setContent {
      EventDetailSheet(
          event = testEvent,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = { closeCalled = true },
          onShare = {})
    }

    composeTestRule.onNodeWithTag("closeButton").performClick()
    assertTrue(closeCalled)
  }

  @Test
  fun mediumState_noDate_doesNotDisplayDate() {
    val eventNoDate = testEvent.copy(date = null)
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoDate,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventDate").assertDoesNotExist()
  }

  @Test
  fun fullState_noDate_doesNotDisplayDate() {
    val eventNoDate = testEvent.copy(date = null)
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoDate,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventDateFull").assertDoesNotExist()
  }

  @Test
  fun mediumState_noTags_doesNotDisplayTags() {
    val eventNoTags = testEvent.copy(tags = emptyList())
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoTags,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventTags").assertDoesNotExist()
  }

  @Test
  fun fullState_noTags_doesNotDisplayTags() {
    val eventNoTags = testEvent.copy(tags = emptyList())
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoTags,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventTagsFull").assertDoesNotExist()
  }

  @Test
  fun mediumState_blankDescription_doesNotShowPreview() {
    val eventBlankDesc = testEvent.copy(description = "   ")
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventBlankDesc,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("eventDescriptionPreview").assertDoesNotExist()
  }

  @Test
  fun mediumState_nullCapacity_showsAttendeeCount() {
    val eventNoCapacity = testEvent.copy(capacity = null, attendeeCount = 5)
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNoCapacity,
          sheetState = BottomSheetState.MEDIUM,
          isParticipating = false,
          organizerName = "Test Organizer",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("attendeeCount").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCount").assertTextEquals("5 / 0 attendees")
    composeTestRule.onNodeWithTag("joinEventButton").assertIsEnabled()
  }

  @Test
  fun fullState_nullAttendeeCount_displaysZero() {
    val eventNullAttendees = testEvent.copy(attendeeCount = null, capacity = 10)
    composeTestRule.setContent {
      EventDetailSheet(
          event = eventNullAttendees,
          sheetState = BottomSheetState.FULL,
          isParticipating = false,
          organizerName = "John Doe",
          onJoinEvent = {},
          onUnregisterEvent = {},
          onSaveForLater = {},
          onClose = {},
          onShare = {})
    }

    composeTestRule.onNodeWithTag("attendeeCountFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCountFull").assertTextEquals("0 / 10 attendees")
  }
}

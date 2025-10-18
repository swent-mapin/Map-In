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
          capacity = 10,
          tags = listOf("Music", "Concert"),
          imageUrl = "https://example.com/image.jpg",
          url = "https://example.com/event")

  // Helper function to reduce boilerplate
  private fun setEventDetailSheet(
      event: Event = testEvent,
      sheetState: BottomSheetState,
      isParticipating: Boolean = false,
      organizerName: String = "Test Organizer",
      onJoinEvent: () -> Unit = {},
      onUnregisterEvent: () -> Unit = {},
      onSaveForLater: () -> Unit = {},
      onClose: () -> Unit = {},
      onShare: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      EventDetailSheet(
          event = event,
          sheetState = sheetState,
          isParticipating = isParticipating,
          organizerName = organizerName,
          onJoinEvent = onJoinEvent,
          onUnregisterEvent = onUnregisterEvent,
          onSaveForLater = onSaveForLater,
          onClose = onClose,
          onShare = onShare)
    }
  }

  // COLLAPSED STATE TESTS
  @Test
  fun collapsedState_displaysCorrectContent() {
    setEventDetailSheet(sheetState = BottomSheetState.COLLAPSED)

    composeTestRule.onNodeWithTag("eventDetailSheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleCollapsed").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleCollapsed").assertTextEquals("Test Event")
    composeTestRule.onNodeWithTag("eventTagCollapsed").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTagCollapsed").assertTextEquals("Music")
  }

  @Test
  fun collapsedState_withNoTags_hidesTag() {
    setEventDetailSheet(
        event = testEvent.copy(tags = emptyList()), sheetState = BottomSheetState.COLLAPSED)

    composeTestRule.onNodeWithTag("eventTitleCollapsed").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTagCollapsed").assertDoesNotExist()
  }

  // MEDIUM STATE TESTS
  @Test
  fun mediumState_displaysAllRequiredInformation() {
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM)

    composeTestRule.onNodeWithTag("eventTitleMedium").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleMedium").assertTextEquals("Test Event")
    composeTestRule.onNodeWithTag("eventDate").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTags").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTags").assertTextEquals("Music, Concert")
    composeTestRule.onNodeWithTag("eventLocation").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventLocation").assertTextEquals("📍 Paris")
    composeTestRule.onNodeWithTag("eventDescriptionPreview").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCount").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCount").assertTextEquals("2 / 10 attendees")
  }

  @Test
  fun mediumState_notParticipating_showsJoinButton() {
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM, isParticipating = false)

    composeTestRule.onNodeWithTag("joinEventButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButton").assertIsEnabled()
    composeTestRule.onNodeWithTag("unregisterButton").assertDoesNotExist()
  }

  @Test
  fun mediumState_participating_showsUnregisterButton() {
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM, isParticipating = true)

    composeTestRule.onNodeWithTag("unregisterButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButton").assertDoesNotExist()
  }

  @Test
  fun mediumState_eventAtCapacity_disablesJoinButton() {
    setEventDetailSheet(
        event = testEvent.copy(participantIds = List(10) { "user$it" }, capacity = 10),
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false)

    composeTestRule.onNodeWithTag("joinEventButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButton").assertIsNotEnabled()
  }

  @Test
  fun mediumState_joinButtonCallback_works() {
    var joinCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = false,
        onJoinEvent = { joinCalled = true })

    composeTestRule.onNodeWithTag("joinEventButton").performClick()
    assertTrue(joinCalled)
  }

  @Test
  fun mediumState_unregisterButtonCallback_works() {
    var unregisterCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.MEDIUM,
        isParticipating = true,
        onUnregisterEvent = { unregisterCalled = true })

    composeTestRule.onNodeWithTag("unregisterButton").performClick()
    assertTrue(unregisterCalled)
  }

  @Test
  fun mediumState_noDate_hidesDateField() {
    setEventDetailSheet(event = testEvent.copy(date = null), sheetState = BottomSheetState.MEDIUM)

    composeTestRule.onNodeWithTag("eventDate").assertDoesNotExist()
  }

  @Test
  fun mediumState_noTags_hidesTagsField() {
    setEventDetailSheet(
        event = testEvent.copy(tags = emptyList()), sheetState = BottomSheetState.MEDIUM)

    composeTestRule.onNodeWithTag("eventTags").assertDoesNotExist()
  }

  @Test
  fun mediumState_blankDescription_hidesDescriptionPreview() {
    setEventDetailSheet(
        event = testEvent.copy(description = "   "), sheetState = BottomSheetState.MEDIUM)

    composeTestRule.onNodeWithTag("eventDescriptionPreview").assertDoesNotExist()
  }

  @Test
  fun mediumState_nullCapacity_showsAttendeeCountWithZero() {
    setEventDetailSheet(
        event = testEvent.copy(capacity = null, participantIds = List(5) { "user$it" }),
        sheetState = BottomSheetState.MEDIUM)

    composeTestRule.onNodeWithTag("attendeeCount").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCount").assertTextEquals("5 / 0 attendees")
    composeTestRule.onNodeWithTag("joinEventButton").assertIsEnabled()
  }

  // FULL STATE TESTS
  @Test
  fun fullState_displaysAllInformation() {
    setEventDetailSheet(sheetState = BottomSheetState.FULL, organizerName = "John Doe")

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
  fun fullState_notParticipating_showsJoinAndSaveButtons() {
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL, isParticipating = false, organizerName = "John Doe")

    composeTestRule.onNodeWithTag("joinEventButtonFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("saveForLaterButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("unregisterButtonFull").assertDoesNotExist()
  }

  @Test
  fun fullState_participating_showsUnregisterButton() {
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL, isParticipating = true, organizerName = "John Doe")

    composeTestRule.onNodeWithTag("unregisterButtonFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButtonFull").assertDoesNotExist()
  }

  @Test
  fun fullState_saveForLaterButton_triggersCallback() {
    var saveCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        isParticipating = false,
        organizerName = "John Doe",
        onSaveForLater = { saveCalled = true })

    composeTestRule.onNodeWithTag("saveForLaterButton").performClick()
    assertTrue(saveCalled)
  }

  @Test
  fun fullState_noImage_displaysPlaceholder() {
    setEventDetailSheet(
        event = testEvent.copy(imageUrl = null),
        sheetState = BottomSheetState.FULL,
        organizerName = "John Doe")

    composeTestRule.onNodeWithTag("eventImage").assertIsDisplayed()
    composeTestRule.onNodeWithText("No image available").assertIsDisplayed()
  }

  @Test
  fun fullState_noDescription_hidesDescriptionField() {
    setEventDetailSheet(
        event = testEvent.copy(description = ""),
        sheetState = BottomSheetState.FULL,
        organizerName = "John Doe")

    composeTestRule.onNodeWithTag("eventDescription").assertDoesNotExist()
  }

  @Test
  fun fullState_noDate_hidesDateField() {
    setEventDetailSheet(
        event = testEvent.copy(date = null),
        sheetState = BottomSheetState.FULL,
        organizerName = "John Doe")

    composeTestRule.onNodeWithTag("eventDateFull").assertDoesNotExist()
  }

  @Test
  fun fullState_noTags_hidesTagsField() {
    setEventDetailSheet(
        event = testEvent.copy(tags = emptyList()),
        sheetState = BottomSheetState.FULL,
        organizerName = "John Doe")

    composeTestRule.onNodeWithTag("eventTagsFull").assertDoesNotExist()
  }

  @Test
  fun fullState_emptyParticipants_displaysZero() {
    setEventDetailSheet(
        event = testEvent.copy(participantIds = emptyList(), capacity = 10),
        sheetState = BottomSheetState.FULL,
        organizerName = "John Doe")

    composeTestRule.onNodeWithTag("attendeeCountFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCountFull").assertTextEquals("0 / 10 attendees")
  }

  // COMMON FUNCTIONALITY TESTS (applicable to all states)
  @Test
  fun allStates_shareButton_triggersCallback() {
    var shareCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.COLLAPSED,
        organizerName = "John Doe",
        onShare = { shareCalled = true })

    composeTestRule.onNodeWithTag("shareButton").performClick()
    assertTrue(shareCalled)
  }

  @Test
  fun allStates_closeButton_triggersCallback() {
    var closeCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.MEDIUM,
        organizerName = "John Doe",
        onClose = { closeCalled = true })

    composeTestRule.onNodeWithTag("closeButton").performClick()
    assertTrue(closeCalled)
  }
}

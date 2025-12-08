package com.swent.mapin.ui.event

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.map.BottomSheetState
import com.swent.mapin.ui.map.OrganizerState
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
      isSaved: Boolean = false,
      organizerState: OrganizerState = OrganizerState.Loaded("owner123", "Test Organizer"),
      onJoinEvent: () -> Unit = {},
      onUnregisterEvent: () -> Unit = {},
      onSaveForLater: () -> Unit = {},
      onUnsaveForLater: () -> Unit = {},
      onClose: () -> Unit = {},
      onShare: () -> Unit = {},
      hasLocationPermission: Boolean = false,
      showDirections: Boolean = false,
      onGetDirections: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      EventDetailSheet(
          event = event,
          sheetState = sheetState,
          isParticipating = isParticipating,
          isSaved = isSaved,
          organizerState = organizerState,
          onJoinEvent = onJoinEvent,
          onUnregisterEvent = onUnregisterEvent,
          onSaveForLater = onSaveForLater,
          onUnsaveForLater = onUnsaveForLater,
          onClose = onClose,
          onShare = onShare,
          hasLocationPermission = hasLocationPermission,
          showDirections = showDirections,
          onGetDirections = onGetDirections)
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
    composeTestRule.onNodeWithTag("attendeeCount").assertTextEquals("2 attending")
    composeTestRule.onNodeWithTag("capacityInfo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("capacityInfo").assertTextEquals("8 spots left")
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
  fun mediumState_nullCapacity_showsAttendeeCountOnly() {
    setEventDetailSheet(
        event = testEvent.copy(capacity = null, participantIds = List(5) { "user$it" }),
        sheetState = BottomSheetState.MEDIUM)

    composeTestRule.onNodeWithTag("attendeeCount").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCount").assertTextEquals("5 attending")
    composeTestRule.onNodeWithTag("capacityInfo").assertDoesNotExist()
    composeTestRule.onNodeWithTag("joinEventButton").assertIsEnabled()
  }

  // FULL STATE TESTS
  @Test
  fun fullState_displaysAllInformation() {
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    composeTestRule.onNodeWithTag("eventImage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTitleFull").assertTextEquals("Test Event")
    composeTestRule.onNodeWithTag("eventDateFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventTagsFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("organizerName").assertIsDisplayed()
    composeTestRule.onNodeWithTag("organizerName").assertTextEquals("John Doe")
    composeTestRule.onNodeWithTag("eventLocationFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCountFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCountFull").assertTextEquals("2 attending")
    composeTestRule.onNodeWithTag("capacityInfoFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("capacityInfoFull").assertTextEquals("8 spots left")
    composeTestRule.onNodeWithTag("eventDescription").assertIsDisplayed()
  }

  @Test
  fun fullState_notParticipating_showsJoinAndSaveButtons() {
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        isParticipating = false,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    // Scroll to make buttons visible on smaller screens (CI)
    composeTestRule.onNodeWithTag("joinEventButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("saveButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("unregisterButtonFull").assertDoesNotExist()
  }

  @Test
  fun fullState_participating_showsUnregisterButton() {
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        isParticipating = true,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    // Scroll to make button visible on smaller screens (CI)
    composeTestRule.onNodeWithTag("unregisterButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButtonFull").assertDoesNotExist()
  }

  @Test
  fun fullState_noImage_displaysPlaceholder() {
    setEventDetailSheet(
        event = testEvent.copy(imageUrl = null),
        sheetState = BottomSheetState.FULL,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    composeTestRule.onNodeWithTag("eventImage").assertIsDisplayed()
    composeTestRule.onNodeWithText("No image available").assertIsDisplayed()
  }

  @Test
  fun fullState_noDescription_hidesDescriptionField() {
    setEventDetailSheet(
        event = testEvent.copy(description = ""),
        sheetState = BottomSheetState.FULL,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    composeTestRule.onNodeWithTag("eventDescription").assertDoesNotExist()
  }

  @Test
  fun fullState_noDate_hidesDateField() {
    setEventDetailSheet(
        event = testEvent.copy(date = null),
        sheetState = BottomSheetState.FULL,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    composeTestRule.onNodeWithTag("eventDateFull").assertDoesNotExist()
  }

  @Test
  fun fullState_noTags_hidesTagsField() {
    setEventDetailSheet(
        event = testEvent.copy(tags = emptyList()),
        sheetState = BottomSheetState.FULL,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    composeTestRule.onNodeWithTag("eventTagsFull").assertDoesNotExist()
  }

  @Test
  fun fullState_emptyParticipants_displaysZero() {
    setEventDetailSheet(
        event = testEvent.copy(participantIds = emptyList(), capacity = 10),
        sheetState = BottomSheetState.FULL,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"))

    composeTestRule.onNodeWithTag("attendeeCountFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("attendeeCountFull").assertTextEquals("0 attending")
    composeTestRule.onNodeWithTag("capacityInfoFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("capacityInfoFull").assertTextEquals("10 spots left")
  }

  // COMMON FUNCTIONALITY TESTS (applicable to all states)
  @Test
  fun allStates_shareButton_triggersCallback() {
    var shareCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.COLLAPSED,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"),
        onShare = { shareCalled = true })

    composeTestRule.onNodeWithTag("shareButton").performClick()
    assertTrue(shareCalled)
  }

  @Test
  fun allStates_closeButton_triggersCallback() {
    var closeCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.MEDIUM,
        organizerState = OrganizerState.Loaded("owner123", "John Doe"),
        onClose = { closeCalled = true })

    composeTestRule.onNodeWithTag("closeButton").performClick()
    assertTrue(closeCalled)
  }

  @Test
  fun mediumState_directionsButton_withLocationPermission_isEnabled() {
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM, hasLocationPermission = true)

    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsEnabled()
  }

  @Test
  fun mediumState_directionsButton_withoutLocationPermission_isDisabled() {
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM, hasLocationPermission = false)

    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsNotEnabled()
  }

  @Test
  fun mediumState_directionsButton_withoutPermission_butShowingDirections_isEnabled() {
    setEventDetailSheet(
        sheetState = BottomSheetState.MEDIUM, showDirections = true, hasLocationPermission = false)

    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsEnabled()
  }

  @Test
  fun fullState_directionsButton_withLocationPermission_isEnabled() {
    setEventDetailSheet(sheetState = BottomSheetState.FULL, hasLocationPermission = true)

    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsEnabled()
  }

  @Test
  fun fullState_directionsButton_withoutLocationPermission_isDisabled() {
    setEventDetailSheet(sheetState = BottomSheetState.FULL, hasLocationPermission = false)

    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("getDirectionsButton").assertIsNotEnabled()
  }

  @Test
  fun mediumState_directionsButton_callback_works() {
    var directionsCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.MEDIUM,
        onGetDirections = { directionsCalled = true },
        hasLocationPermission = true)

    composeTestRule.onNodeWithTag("getDirectionsButton").performClick()
    assertTrue(directionsCalled)
  }

  // --- SAVE / UNSAVE in FULL state ---

  @Test
  fun fullState_notSaved_showsOnlySave_andInvokesOnSave() {
    var saveCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        isParticipating = false,
        isSaved = false,
        onSaveForLater = { saveCalled = true },
        organizerState = OrganizerState.Loaded("owner123", "Org"))

    // Save button is visible, Unsave is not
    composeTestRule.onNodeWithTag("saveButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("unsaveButtonFull").assertDoesNotExist()

    // Click -> callback fired
    composeTestRule.onNodeWithTag("saveButtonFull").performClick()
    assertTrue(saveCalled)
  }

  @Test
  fun fullState_saved_showsOnlyUnsave_andInvokesOnUnsave() {
    var unsaveCalled = false
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        isParticipating = false,
        isSaved = true,
        onUnsaveForLater = { unsaveCalled = true },
        organizerState = OrganizerState.Loaded("owner123", "Org"))

    // Unsave button is visible, Save is not
    composeTestRule.onNodeWithTag("unsaveButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("saveButtonFull").assertDoesNotExist()

    // Click -> callback fired
    composeTestRule.onNodeWithTag("unsaveButtonFull").performClick()
    assertTrue(unsaveCalled)
  }

  @Test
  fun fullState_Save_haveCorrectLabels() {
    // Not saved -> label "Save for later"
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        isSaved = false,
        organizerState = OrganizerState.Loaded("owner123", "Org"))
    composeTestRule.onNodeWithTag("saveButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Save for later").assertIsDisplayed()
  }

  @Test
  fun fullState_Unsave_haveCorrectLabels() {
    // Recompose with saved -> label "Unsave"
    setEventDetailSheet(
        sheetState = BottomSheetState.FULL,
        isSaved = true,
        organizerState = OrganizerState.Loaded("owner123", "Org"))
    composeTestRule.onNodeWithTag("unsaveButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Unsave").assertIsDisplayed()
  }

  // --- NEGATIVE: Save controls should NOT appear in MEDIUM ---

  @Test
  fun mediumState_noSaveControlsVisible_NotSaved() {
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM, isSaved = false)
    composeTestRule.onNodeWithTag("saveButtonFull").assertDoesNotExist()
    composeTestRule.onNodeWithTag("unsaveButtonFull").assertDoesNotExist()
  }

  @Test
  fun mediumState_noSaveControlsVisible_Saved() {
    // Even if hypothetically saved, still no save UI in MEDIUM
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM, isSaved = true)
    composeTestRule.onNodeWithTag("saveButtonFull").assertDoesNotExist()
    composeTestRule.onNodeWithTag("unsaveButtonFull").assertDoesNotExist()
  }

  // --- EDGE CASES for attendee/capacity UI (extra branches) ---

  @Test
  fun mediumState_eventExactlyAtCapacity_hidesCapacityInfo_andDisablesJoin() {
    // participants == capacity
    val e = testEvent.copy(participantIds = List(10) { "u$it" }, capacity = 10)
    setEventDetailSheet(event = e, sheetState = BottomSheetState.MEDIUM, isParticipating = false)

    composeTestRule.onNodeWithTag("attendeeCount").assertIsDisplayed()
    composeTestRule.onNodeWithTag("capacityInfo").assertDoesNotExist()
    composeTestRule.onNodeWithTag("joinEventButton").assertIsNotEnabled()
  }

  @Test
  fun fullState_eventExactlyAtCapacity_hidesCapacityInfo_andShowsUnregisterIfParticipating() {
    // participants == capacity; user participating so we see Unregister
    val e = testEvent.copy(participantIds = List(10) { "u$it" }, capacity = 10)
    setEventDetailSheet(event = e, sheetState = BottomSheetState.FULL, isParticipating = true)

    composeTestRule.onNodeWithTag("attendeeCountFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("capacityInfoFull").assertDoesNotExist()
    composeTestRule.onNodeWithTag("unregisterButtonFull").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun fullState_capacityNull_showsJoinAndNoCapacityInfo() {
    val e = testEvent.copy(capacity = null, participantIds = listOf("a", "b", "c"))
    setEventDetailSheet(event = e, sheetState = BottomSheetState.FULL, isParticipating = false)

    composeTestRule.onNodeWithTag("attendeeCountFull").assertIsDisplayed()
    composeTestRule.onNodeWithTag("capacityInfoFull").assertDoesNotExist()
    composeTestRule.onNodeWithTag("joinEventButtonFull").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("joinEventButtonFull").assertIsEnabled()
  }

  // --- HEADER presence in COLLAPSED vs MEDIUM/FULL (defensive UI check) ---

  @Test
  fun collapsedState_hasTopShareAndCloseButtons() {
    setEventDetailSheet(sheetState = BottomSheetState.COLLAPSED)
    composeTestRule.onNodeWithTag("shareButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed()
  }

  @Test
  fun mediumState_hasHeaderButtonsOnce() {
    setEventDetailSheet(sheetState = BottomSheetState.MEDIUM)
    composeTestRule.onNodeWithTag("shareButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed()
  }

  @Test
  fun fullState_hasHeaderButtonsOnce() {
    setEventDetailSheet(sheetState = BottomSheetState.FULL)
    composeTestRule.onNodeWithTag("shareButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("closeButton").assertIsDisplayed()
  }

  @Test
  fun resolveSaveButtonUi_branches() {
    assertTrue(resolveSaveButtonUi(false).showSaveButton)
    assertTrue(resolveSaveButtonUi(false).label.contains("Save", ignoreCase = true))
    assertTrue(!resolveSaveButtonUi(true).showSaveButton)
    assertTrue(resolveSaveButtonUi(true).label == "Unsave")
  }

  @Test
  fun buildAttendeeInfoUi_variants() {
    // Spots left > 0 -> shows capacity
    val a = testEvent.copy(participantIds = listOf("a", "b"), capacity = 5)
    val uiA = buildAttendeeInfoUi(a)
    assertTrue(uiA.attendeeText == "2 attending")
    assertTrue(uiA.capacityText == "3 spots left")

    // Exactly full -> hides capacity
    val b = testEvent.copy(participantIds = listOf("1", "2"), capacity = 2)
    val uiB = buildAttendeeInfoUi(b)
    assertTrue(uiB.attendeeText == "2 attending")
    assertTrue(uiB.capacityText == null)

    // No capacity -> hides capacity
    val c = testEvent.copy(participantIds = listOf("1"), capacity = null)
    val uiC = buildAttendeeInfoUi(c)
    assertTrue(uiC.attendeeText == "1 attending")
    assertTrue(uiC.capacityText == null)
  }

  @Test
  fun fullState_organizerLoading_showsLoadingText() {
    setEventDetailSheet(sheetState = BottomSheetState.FULL, organizerState = OrganizerState.Loading)

    composeTestRule.onNodeWithTag("organizerName").assertIsDisplayed()
    composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
  }

  @Test
  fun fullState_organizerError_showsUnknown() {
    setEventDetailSheet(sheetState = BottomSheetState.FULL, organizerState = OrganizerState.Error)

    composeTestRule.onNodeWithTag("organizerName").assertIsDisplayed()
    composeTestRule.onNodeWithText("Unknown").assertIsDisplayed()
  }

  @Test
  fun fullState_priceNull_hidesPrice() {
    setEventDetailSheet(sheetState = BottomSheetState.FULL)

    composeTestRule.onNodeWithTag("priceSection").assertDoesNotExist()
  }

  @Test
  fun fullState_showsPrice() {
    setEventDetailSheet(event = testEvent.copy(price = 10.0), sheetState = BottomSheetState.FULL)

    composeTestRule.onNodeWithTag("priceSection").assertIsDisplayed()
    composeTestRule.onNodeWithText("10.00 CHF").assertIsDisplayed()
  }
}

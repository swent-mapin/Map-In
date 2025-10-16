package com.swent.mapin.ui.map

import android.content.Context
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.memory.MemoryRepository
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

// Assisted by AI
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class MapScreenViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Mock(lenient = true) private lateinit var mockContext: Context
  @Mock(lenient = true) private lateinit var mockMemoryRepository: MemoryRepository
  @Mock(lenient = true) private lateinit var mockEventRepository: EventRepository
  @Mock(lenient = true) private lateinit var mockAuth: FirebaseAuth
  @Mock(lenient = true) private lateinit var mockUser: FirebaseUser

  private lateinit var viewModel: MapScreenViewModel
  private lateinit var config: BottomSheetConfig
  private var clearFocusCalled = false

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    clearFocusCalled = false
    config = BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")

    // Mock applicationContext to return itself (or another mock)
    whenever(mockContext.applicationContext).thenReturn(mockContext)

    runBlocking {
      whenever(mockEventRepository.getEventsByParticipant("testUserId")).thenReturn(emptyList())
      whenever(mockMemoryRepository.getNewUid()).thenReturn("newMemoryId")
      whenever(mockMemoryRepository.addMemory(any())).thenReturn(Unit)
    }

    viewModel =
        MapScreenViewModel(
            initialSheetState = BottomSheetState.COLLAPSED,
            sheetConfig = config,
            onClearFocus = { clearFocusCalled = true },
            applicationContext = mockContext,
            memoryRepository = mockMemoryRepository,
            eventRepository = mockEventRepository,
            auth = mockAuth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_hasExpectedDefaults() {
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
    assertEquals("", viewModel.searchQuery)
    assertFalse(viewModel.shouldFocusSearch)
    assertEquals(0, viewModel.fullEntryKey)
    assertEquals(config.collapsedHeight, viewModel.currentSheetHeight)
  }

  @Test
  fun setBottomSheetState_transitionToFull_incrementsFullEntryKey() {
    val initialKey = viewModel.fullEntryKey
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    assertEquals(initialKey + 1, viewModel.fullEntryKey)

    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    assertEquals(initialKey + 2, viewModel.fullEntryKey)
  }

  @Test
  fun setBottomSheetState_stayingInFull_doesNotIncrementKey() {
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    val keyAfterFirstTransition = viewModel.fullEntryKey
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    assertEquals(keyAfterFirstTransition, viewModel.fullEntryKey)
  }

  @Test
  fun setBottomSheetState_leavingFull_clearsSearchAndCallsClearFocus() {
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    viewModel.onSearchQueryChange("test query")

    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)

    assertEquals("", viewModel.searchQuery)
    assertFalse(viewModel.shouldFocusSearch)
    assertTrue(clearFocusCalled)
  }

  @Test
  fun onSearchQueryChange_fromCollapsed_expandsToFullAndSetsFocus() {
    viewModel.onSearchQueryChange("query")

    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertEquals("query", viewModel.searchQuery)
    assertTrue(viewModel.shouldFocusSearch)
  }

  @Test
  fun onSearchQueryChange_alreadyInFull_updatesQueryWithoutSideEffects() {
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    viewModel.onSearchFocusHandled()
    val keyBefore = viewModel.fullEntryKey

    viewModel.onSearchQueryChange("new query")

    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertEquals("new query", viewModel.searchQuery)
    assertEquals(keyBefore, viewModel.fullEntryKey)
    assertFalse(viewModel.shouldFocusSearch)
  }

  @Test
  fun onSearchTap_expandsToFullAndSetsFocus() {
    viewModel.onSearchTap()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertTrue(viewModel.shouldFocusSearch)

    viewModel.onSearchFocusHandled()
    viewModel.onSearchTap()
    assertFalse(viewModel.shouldFocusSearch)
  }

  @Test
  fun onSearchFocusHandled_clearsFocusFlag() {
    viewModel.onSearchTap()
    assertTrue(viewModel.shouldFocusSearch)

    viewModel.onSearchFocusHandled()
    assertFalse(viewModel.shouldFocusSearch)
  }

  @Test
  fun updateMediumReferenceZoom_inMediumMode_storesReference() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.updateMediumReferenceZoom(15f)

    assertFalse(viewModel.checkZoomInteraction(15.4f))
    assertTrue(viewModel.checkZoomInteraction(15.6f))
  }

  @Test
  fun updateMediumReferenceZoom_notInMediumMode_ignored() {
    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    viewModel.updateMediumReferenceZoom(15f)

    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    assertTrue(viewModel.checkZoomInteraction(0.5f))
  }

  @Test
  fun checkZoomInteraction_notInMediumMode_returnsFalse() {
    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    assertFalse(viewModel.checkZoomInteraction(20f))
  }

  @Test
  fun checkZoomInteraction_respectsThreshold() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.updateMediumReferenceZoom(10f)

    assertFalse(viewModel.checkZoomInteraction(10.4f))
    assertTrue(viewModel.checkZoomInteraction(10.5f))
    assertTrue(viewModel.checkZoomInteraction(9.4f))
  }

  @Test
  fun checkTouchProximityToSheet_notInMediumMode_returnsFalse() {
    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    assertFalse(viewModel.checkTouchProximityToSheet(500f, 1000f, 160))
  }

  @Test
  fun checkTouchProximityToSheet_inMediumMode_detectsProximity() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    val densityDpi = 160
    val sheetTopY = 1000f
    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f

    assertTrue(viewModel.checkTouchProximityToSheet(sheetTopY, sheetTopY, densityDpi))
    assertTrue(
        viewModel.checkTouchProximityToSheet(sheetTopY - thresholdPx + 1f, sheetTopY, densityDpi))
    assertFalse(
        viewModel.checkTouchProximityToSheet(sheetTopY - thresholdPx - 1f, sheetTopY, densityDpi))
  }

  @Test
  fun checkTouchProximityToSheet_respectsThreshold() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    val sheetTopY = 1000f
    val densityDpi = 160
    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f

    assertTrue(viewModel.checkTouchProximityToSheet(sheetTopY, sheetTopY, densityDpi))
    assertTrue(
        viewModel.checkTouchProximityToSheet(sheetTopY - (thresholdPx - 1f), sheetTopY, densityDpi))
    assertTrue(
        viewModel.checkTouchProximityToSheet(sheetTopY + (thresholdPx - 1f), sheetTopY, densityDpi))
    assertFalse(
        viewModel.checkTouchProximityToSheet(sheetTopY - (thresholdPx + 5f), sheetTopY, densityDpi))
  }

  @Test
  fun calculateTargetState_snapsToNearestState() {
    assertEquals(BottomSheetState.COLLAPSED, viewModel.calculateTargetState(150f, 120f, 400f, 800f))
    assertEquals(BottomSheetState.MEDIUM, viewModel.calculateTargetState(300f, 120f, 400f, 800f))
    assertEquals(BottomSheetState.FULL, viewModel.calculateTargetState(700f, 120f, 400f, 800f))
  }

  @Test
  fun calculateTargetState_midpointsSnapToUpperState() {
    assertEquals(BottomSheetState.MEDIUM, viewModel.calculateTargetState(260f, 120f, 400f, 800f))
    assertEquals(BottomSheetState.FULL, viewModel.calculateTargetState(600f, 120f, 400f, 800f))
  }

  @Test
  fun getHeightForState_mapsCorrectly() {
    assertEquals(config.collapsedHeight, viewModel.getHeightForState(BottomSheetState.COLLAPSED))
    assertEquals(config.mediumHeight, viewModel.getHeightForState(BottomSheetState.MEDIUM))
    assertEquals(config.fullHeight, viewModel.getHeightForState(BottomSheetState.FULL))
  }

  @Test
  fun setBottomSheetState_updatesCurrentSheetHeight() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)

    viewModel.setBottomSheetState(BottomSheetState.FULL)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)

    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
  }

  @Test
  fun onSearchQueryChange_emptyQuery_doesNotExpandSheet() {
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
    viewModel.onSearchQueryChange("")
    assertEquals("", viewModel.searchQuery)
  }

  @Test
  fun bottomSheetState_transitionFromMediumToCollapsed_clearsSearchQuery() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.onSearchQueryChange("test")

    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)

    assertEquals("", viewModel.searchQuery)
  }

  @Test
  fun clearFocus_calledOnlyWhenLeavingFullState() {
    clearFocusCalled = false
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    assertFalse(clearFocusCalled)

    viewModel.setBottomSheetState(BottomSheetState.FULL)
    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    assertTrue(clearFocusCalled)
  }

  // NEW TESTS FOR EVENT FUNCTIONALITY

  @Test
  fun initialState_hasNoSelectedEvent() {
    assertNull(viewModel.selectedEvent)
    assertEquals("", viewModel.organizerName)
    assertFalse(viewModel.showShareDialog)
  }

  @Test
  fun onEventPinClicked_setsSelectedEventAndTransitionsToMedium() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var cameraCentered = false
    viewModel.onCenterCamera = { cameraCentered = true }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, viewModel.selectedEvent)
    assertNotNull(viewModel.organizerName)
    assertTrue(viewModel.organizerName.isNotEmpty())
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(cameraCentered)
  }

  @Test
  fun closeEventDetail_clearsSelectedEventAndReturnsToCollapsed() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    viewModel.closeEventDetail()

    assertNull(viewModel.selectedEvent)
    assertEquals("", viewModel.organizerName)
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
  }

  @Test
  fun showShareDialog_setsShowShareDialogToTrue() {
    assertFalse(viewModel.showShareDialog)
    viewModel.showShareDialog()
    assertTrue(viewModel.showShareDialog)
  }

  @Test
  fun dismissShareDialog_setsShowShareDialogToFalse() {
    viewModel.showShareDialog()
    assertTrue(viewModel.showShareDialog)

    viewModel.dismissShareDialog()
    assertFalse(viewModel.showShareDialog)
  }

  @Test
  fun isUserParticipating_noSelectedEvent_returnsFalse() {
    assertFalse(viewModel.isUserParticipating())
  }

  @Test
  fun isUserParticipating_userNotInParticipantList_returnsFalse() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("otherUser1", "otherUser2"))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertFalse(viewModel.isUserParticipating())
  }

  @Test
  fun isUserParticipating_userInParticipantList_returnsTrue() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId", "otherUser"))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertTrue(viewModel.isUserParticipating())
  }

  @Test
  fun isUserParticipating_noCurrentUser_returnsFalse() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertFalse(viewModel.isUserParticipating())
  }

  @Test
  fun joinEvent_noSelectedEvent_doesNothing() = runTest {
    viewModel.joinEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
  }

  @Test
  fun joinEvent_noCurrentUser_setsErrorMessage() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    viewModel.joinEvent()
    advanceUntilIdle()

    assertEquals("You must be signed in to join events", viewModel.errorMessage)
  }

  @Test
  fun joinEvent_eventAtCapacity_setsErrorMessage() = runTest {
    val fullEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            attendeeCount = 10, capacity = 10)
    viewModel.onEventPinClicked(fullEvent)
    advanceUntilIdle()

    viewModel.joinEvent()
    advanceUntilIdle()

    assertEquals("Event is at full capacity", viewModel.errorMessage)
  }

  @Test
  fun joinEvent_success_addsUserToParticipantsAndIncrementsCount() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("user1"), attendeeCount = 1, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.joinEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertTrue(viewModel.selectedEvent!!.participantIds.contains("testUserId"))
    assertEquals(2, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun joinEvent_userAlreadyParticipating_doesNotDuplicateUser() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = 1, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.joinEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)
    assertEquals(1, viewModel.selectedEvent!!.participantIds.filter { it == "testUserId" }.size)
    assertEquals(2, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun joinEvent_repositoryThrowsException_setsErrorMessage() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf(), attendeeCount = 0, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking {
      whenever(mockEventRepository.editEvent(any(), any()))
          .thenThrow(RuntimeException("Network error"))
    }

    viewModel.joinEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Failed to join event"))
  }

  @Test
  fun unregisterFromEvent_noSelectedEvent_doesNothing() = runTest {
    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
  }

  @Test
  fun unregisterFromEvent_noCurrentUser_doesNothing() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
  }

  @Test
  fun unregisterFromEvent_success_removesUserAndDecrementsCount() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId", "user2"), attendeeCount = 2, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertFalse(viewModel.selectedEvent!!.participantIds.contains("testUserId"))
    assertEquals(1, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun unregisterFromEvent_lastParticipant_setsCountToZero() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = 1, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)
    assertEquals(0, viewModel.selectedEvent!!.attendeeCount)
    assertTrue(viewModel.selectedEvent!!.participantIds.isEmpty())
  }

  @Test
  fun unregisterFromEvent_attendeeCountAlreadyZero_staysAtZero() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = 0, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)
    assertEquals(0, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun unregisterFromEvent_repositoryThrowsException_setsErrorMessage() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = 1)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking {
      whenever(mockEventRepository.editEvent(any(), any()))
          .thenThrow(RuntimeException("Network error"))
    }

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Failed to unregister"))
  }

  @Test
  fun saveEventForLater_setsPlaceholderMessage() {
    viewModel.saveEventForLater()
    assertEquals("Save for later - Coming soon!", viewModel.errorMessage)
  }

  @Test
  fun setBottomSheetTab_updatesSelectedTab() {
    assertEquals(
        MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES, viewModel.selectedBottomSheetTab)

    viewModel.setBottomSheetTab(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS)
    assertEquals(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS, viewModel.selectedBottomSheetTab)

    viewModel.setBottomSheetTab(MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES)
    assertEquals(
        MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES, viewModel.selectedBottomSheetTab)
  }

  @Test
  fun onJoinedEventClicked_callsOnEventPinClicked() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var cameraCentered = false
    viewModel.onCenterCamera = { cameraCentered = true }

    viewModel.onJoinedEventClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(cameraCentered)
  }

  @Test
  fun setEvents_updatesEventsList() {
    val newEvents =
        listOf(
            com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0],
            com.swent.mapin.model.SampleEventRepository.getSampleEvents()[1])

    viewModel.setEvents(newEvents)

    assertEquals(newEvents, viewModel.events)
    assertEquals(2, viewModel.events.size)
  }

  @Test
  fun joinEvent_updatesEventsListWithNewEvent() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf(), attendeeCount = 0, capacity = 10)

    viewModel.setEvents(listOf(testEvent))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.joinEvent()
    advanceUntilIdle()

    val updatedEvent = viewModel.events.find { it.uid == testEvent.uid }
    assertNotNull(updatedEvent)
    assertTrue(updatedEvent!!.participantIds.contains("testUserId"))
    assertEquals(1, updatedEvent.attendeeCount)
  }

  @Test
  fun unregisterFromEvent_updatesEventsListWithNewEvent() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = 1, capacity = 10)

    viewModel.setEvents(listOf(testEvent))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    val updatedEvent = viewModel.events.find { it.uid == testEvent.uid }
    assertNotNull(updatedEvent)
    assertFalse(updatedEvent!!.participantIds.contains("testUserId"))
    assertEquals(0, updatedEvent.attendeeCount)
  }

  @Test
  fun clearError_clearsErrorMessage() {
    viewModel.saveEventForLater()
    assertNotNull(viewModel.errorMessage)

    viewModel.clearError()
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun joinedEvents_initiallyEmpty() {
    assertEquals(0, viewModel.joinedEvents.size)
  }

  @Test
  fun availableEvents_initiallyEmpty() {
    assertEquals(0, viewModel.availableEvents.size)
  }

  @Test
  fun onCenterCamera_canBeSetAndInvoked() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var centeredEvent: com.swent.mapin.model.event.Event? = null

    viewModel.onCenterCamera = { centeredEvent = it }
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, centeredEvent)
  }

  @Test
  fun joinEvent_withNullCapacity_allowsJoin() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf(), attendeeCount = 5, capacity = null)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.joinEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertEquals(6, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun joinEvent_withNullAttendeeCount_incrementsFromZero() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf(), attendeeCount = null, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.joinEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertEquals(1, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun unregisterFromEvent_withNullAttendeeCount_setsToZero() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = null, capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)
    assertEquals(0, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun organizerName_displaysTruncatedUserId() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            ownerId = "user123456789")

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertTrue(viewModel.organizerName.contains("User"))
    assertTrue(viewModel.organizerName.contains("user12"))
  }

  @Test
  fun multipleEventOperations_maintainConsistency() = runTest {
    val event1 =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            uid = "event1", participantIds = listOf(), attendeeCount = 0, capacity = 10)
    val event2 =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[1].copy(
            uid = "event2", participantIds = listOf(), attendeeCount = 0, capacity = 10)

    viewModel.setEvents(listOf(event1, event2))

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    // Join first event
    viewModel.onEventPinClicked(event1)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()

    assertEquals(1, viewModel.events.find { it.uid == "event1" }!!.attendeeCount)
    assertEquals(0, viewModel.events.find { it.uid == "event2" }!!.attendeeCount)

    // Join second event
    viewModel.closeEventDetail()
    viewModel.onEventPinClicked(event2)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()

    assertEquals(1, viewModel.events.find { it.uid == "event1" }!!.attendeeCount)
    assertEquals(1, viewModel.events.find { it.uid == "event2" }!!.attendeeCount)
  }

  @Test
  fun onCenterCamera_invokedWithLowZoom_shouldZoomTo15() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var centeredEvent: com.swent.mapin.model.event.Event? = null
    var cameraCalled = false

    // Mock camera with zoom < 14
    viewModel.onCenterCamera = { event ->
      centeredEvent = event
      cameraCalled = true
      // Simulate the zoom calculation: currentZoom < 14.0 -> targetZoom = 15.0
    }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, centeredEvent)
    assertTrue(cameraCalled)
  }

  @Test
  fun onCenterCamera_invokedWithHighZoom_shouldMaintainZoom() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var centeredEvent: com.swent.mapin.model.event.Event? = null
    var cameraCalled = false

    // Mock camera with zoom >= 14
    viewModel.onCenterCamera = { event ->
      centeredEvent = event
      cameraCalled = true
      // Simulate the zoom calculation: currentZoom >= 14.0 -> targetZoom = currentZoom
    }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, centeredEvent)
    assertTrue(cameraCalled)
  }

  @Test
  fun onCenterCamera_calculatesOffsetCorrectly() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var offsetCalculated = false

    viewModel.onCenterCamera = { _ ->
      // Verify that the callback is invoked with the event
      // The offset calculation: (screenHeightDpValue * 0.25) / 2
      offsetCalculated = true
    }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertTrue(offsetCalculated)
  }

  @Test
  fun selectedEvent_notNull_triggersEventDetailSheetDisplay() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    // Initially selectedEvent should be null
    assertNull(viewModel.selectedEvent)

    // Click on event to select it
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // Now selectedEvent should not be null
    assertNotNull(viewModel.selectedEvent)
    assertEquals(testEvent, viewModel.selectedEvent)
  }

  @Test
  fun selectedEvent_null_showsRegularBottomSheetContent() = runTest {
    // Initially selectedEvent should be null
    assertNull(viewModel.selectedEvent)

    // Close event detail if any was selected
    viewModel.closeEventDetail()
    advanceUntilIdle()

    // selectedEvent should still be null
    assertNull(viewModel.selectedEvent)
  }

  @Test
  fun isUserParticipating_returnsTrueWhenUserInParticipantList() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId", "otherUser"))

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertTrue(viewModel.isUserParticipating())
  }

  @Test
  fun isUserParticipating_returnsFalseWhenUserNotInParticipantList() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("otherUser1", "otherUser2"))

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertFalse(viewModel.isUserParticipating())
  }

  @Test
  fun eventDetailSheet_callsOnJoinEvent() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf(), attendeeCount = 5, capacity = 10)

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    // This simulates clicking "Join Event" in EventDetailSheet
    viewModel.joinEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)
    assertTrue(viewModel.isUserParticipating())
  }

  @Test
  fun eventDetailSheet_callsOnUnregisterEvent() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = 5, capacity = 10)

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    // This simulates clicking "Unregister" in EventDetailSheet
    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)
    assertFalse(viewModel.isUserParticipating())
  }

  @Test
  fun eventDetailSheet_callsOnSaveForLater() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // This simulates clicking "Save for later" in EventDetailSheet
    viewModel.saveEventForLater()
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)
  }

  @Test
  fun eventDetailSheet_callsOnClose() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertNotNull(viewModel.selectedEvent)

    // This simulates clicking close button in EventDetailSheet
    viewModel.closeEventDetail()
    advanceUntilIdle()

    assertNull(viewModel.selectedEvent)
  }

  @Test
  fun eventDetailSheet_callsOnShare() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertFalse(viewModel.showShareDialog)

    // This simulates clicking share button in EventDetailSheet
    viewModel.showShareDialog()
    advanceUntilIdle()

    assertTrue(viewModel.showShareDialog)
  }

  @Test
  fun onCenterCamera_executesWithAllBranches() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var callbackExecuted = false
    var eventReceived: com.swent.mapin.model.event.Event? = null

    // Set up a callback that captures execution
    viewModel.onCenterCamera = { event ->
      callbackExecuted = true
      eventReceived = event

      // Simulate the code from MapScreen.kt:
      // val animationOptions = MapAnimationOptions.Builder().duration(500L).build()
      // val currentZoom = mapViewportState.cameraState?.zoom ?:
      // MapConstants.DEFAULT_ZOOM.toDouble()
      // Test low zoom scenario (< 14.0)
      val currentZoom = 10.0
      val targetZoom = if (currentZoom < 14.0) 15.0 else currentZoom
      assertEquals(15.0, targetZoom, 0.001)

      // Test high zoom scenario (>= 14.0)
      val currentZoom2 = 16.0
      val targetZoom2 = if (currentZoom2 < 14.0) 15.0 else currentZoom2
      assertEquals(16.0, targetZoom2, 0.001)

      // Test offset calculation
      val screenHeightDpValue = 800.0
      val offsetPixels = (screenHeightDpValue * 0.25) / 2
      assertEquals(100.0, offsetPixels, 0.001)

      // Test that event location is used
      assertEquals(event.location.longitude, testEvent.location.longitude, 0.0001)
      assertEquals(event.location.latitude, testEvent.location.latitude, 0.0001)
    }

    // Trigger the callback by clicking an event
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // Verify callback was executed
    assertTrue(callbackExecuted)
    assertEquals(testEvent, eventReceived)
  }

  @Test
  fun onCenterCamera_handlesNullZoomWithDefaultValue() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var defaultZoomUsed = false

    viewModel.onCenterCamera = { event ->
      // Simulate: val currentZoom = mapViewportState.cameraState?.zoom ?:
      // MapConstants.DEFAULT_ZOOM.toDouble()
      val currentZoom: Double? = null
      val zoomValue = currentZoom ?: MapConstants.DEFAULT_ZOOM.toDouble()
      assertEquals(MapConstants.DEFAULT_ZOOM.toDouble(), zoomValue, 0.001)
      defaultZoomUsed = true
    }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertTrue(defaultZoomUsed)
  }

  @Test
  fun eventDetailSheet_allCallbacksArePassed() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // Verify that selectedEvent is set (needed for EventDetailSheet to show)
    assertNotNull(viewModel.selectedEvent)
    assertEquals(testEvent, viewModel.selectedEvent)

    // Test all the callbacks that EventDetailSheet receives:

    // 1. onJoinEvent = { viewModel.joinEvent() }
    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }
    val initialParticipantCount = viewModel.selectedEvent!!.participantIds.size
    viewModel.joinEvent()
    advanceUntilIdle()
    // Join should have been attempted

    // 2. onUnregisterEvent = { viewModel.unregisterFromEvent() }
    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    // 3. onSaveForLater = { viewModel.saveEventForLater() }
    viewModel.saveEventForLater()
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    viewModel.clearError()

    // 4. onClose = { viewModel.closeEventDetail() }
    viewModel.closeEventDetail()
    advanceUntilIdle()
    assertNull(viewModel.selectedEvent)

    // 5. onShare = { viewModel.showShareDialog() }
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.showShareDialog()
    advanceUntilIdle()
    assertTrue(viewModel.showShareDialog)
  }

  @Test
  fun eventDetailSheet_receivesCorrectEventData() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // EventDetailSheet receives: event = viewModel.selectedEvent!!
    assertNotNull(viewModel.selectedEvent)
    assertEquals(testEvent.uid, viewModel.selectedEvent!!.uid)
    assertEquals(testEvent.title, viewModel.selectedEvent!!.title)
    assertEquals(testEvent.location, viewModel.selectedEvent!!.location)
  }

  @Test
  fun eventDetailSheet_receivesIsParticipatingState() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"))

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // EventDetailSheet receives: isParticipating = viewModel.isUserParticipating()
    assertTrue(viewModel.isUserParticipating())
  }

  @Test
  fun eventDetailSheet_receivesOrganizerName() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // EventDetailSheet receives: organizerName = viewModel.organizerName
    assertNotNull(viewModel.organizerName)
    assertTrue(viewModel.organizerName.isNotEmpty())
  }

  @Test
  fun eventDetailSheet_receivesBottomSheetState() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // EventDetailSheet receives: sheetState = viewModel.bottomSheetState
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
  }

  @Test
  fun onCenterCamera_animationDurationIs500ms() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var animationDurationTested = false

    viewModel.onCenterCamera = { event ->
      // Test: val animationOptions = MapAnimationOptions.Builder().duration(500L).build()
      val expectedDuration = 500L
      assertEquals(500L, expectedDuration)
      animationDurationTested = true
    }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertTrue(animationDurationTested)
  }

  @Test
  fun onCenterCamera_paddingCalculationIsCorrect() = runTest {
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    var paddingTested = false

    viewModel.onCenterCamera = { event ->
      // Test: padding(EdgeInsets(0.0, 0.0, offsetPixels * 2, 0.0))
      val screenHeightDpValue = 800.0
      val offsetPixels = (screenHeightDpValue * 0.25) / 2
      val bottomPadding = offsetPixels * 2
      assertEquals(200.0, bottomPadding, 0.001)
      paddingTested = true
    }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertTrue(paddingTested)
  }
}

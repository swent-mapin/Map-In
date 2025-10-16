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
    assertNull(viewModel.selectedEvent)
    assertEquals("", viewModel.organizerName)
    assertFalse(viewModel.showShareDialog)
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
  fun calculateTargetState_snapsToNearestState() {
    assertEquals(BottomSheetState.COLLAPSED, viewModel.calculateTargetState(150f, 120f, 400f, 800f))
    assertEquals(BottomSheetState.MEDIUM, viewModel.calculateTargetState(300f, 120f, 400f, 800f))
    assertEquals(BottomSheetState.FULL, viewModel.calculateTargetState(700f, 120f, 400f, 800f))
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
  fun shareDialog_showAndDismiss() {
    assertFalse(viewModel.showShareDialog)
    viewModel.showShareDialog()
    assertTrue(viewModel.showShareDialog)
    viewModel.dismissShareDialog()
    assertFalse(viewModel.showShareDialog)
  }

  @Test
  fun isUserParticipating_variousScenarios() = runTest {
    assertFalse(viewModel.isUserParticipating())

    val notParticipatingEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("otherUser"))
    viewModel.onEventPinClicked(notParticipatingEvent)
    advanceUntilIdle()
    assertFalse(viewModel.isUserParticipating())

    val participatingEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId", "otherUser"))
    viewModel.onEventPinClicked(participatingEvent)
    advanceUntilIdle()
    assertTrue(viewModel.isUserParticipating())
  }

  @Test
  fun joinEvent_errorScenarios() = runTest {
    viewModel.joinEvent()
    advanceUntilIdle()
    assertNull(viewModel.errorMessage)

    whenever(mockAuth.currentUser).thenReturn(null)
    val testEvent = com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()
    assertEquals("You must be signed in to join events", viewModel.errorMessage)

    viewModel.clearError()
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val fullEvent = testEvent.copy(attendeeCount = 10, capacity = 10)
    viewModel.onEventPinClicked(fullEvent)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()
    assertEquals("Event is at full capacity", viewModel.errorMessage)
  }

  @Test
  fun joinEvent_success_updatesEventAndList() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf(), attendeeCount = 0, capacity = 10)

    viewModel.setEvents(listOf(testEvent))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.joinEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertTrue(viewModel.selectedEvent!!.participantIds.contains("testUserId"))
    assertEquals(1, viewModel.selectedEvent!!.attendeeCount)
    assertTrue(
        viewModel.events.find { it.uid == testEvent.uid }!!.participantIds.contains("testUserId"))
  }

  @Test
  fun unregisterFromEvent_success_updatesEventAndList() = runTest {
    val testEvent =
        com.swent.mapin.model.SampleEventRepository.getSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), attendeeCount = 1, capacity = 10)

    viewModel.setEvents(listOf(testEvent))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.unregisterFromEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertFalse(viewModel.selectedEvent!!.participantIds.contains("testUserId"))
    assertEquals(0, viewModel.selectedEvent!!.attendeeCount)
  }

  @Test
  fun joinAndUnregisterEvent_withRepositoryError_setsErrorMessage() = runTest {
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

    viewModel.clearError()
    viewModel.unregisterFromEvent()
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Failed to unregister"))
  }

  @Test
  fun saveEventForLater_setsPlaceholderMessage() {
    viewModel.saveEventForLater()
    assertEquals("Save for later - Coming soon!", viewModel.errorMessage)
    viewModel.clearError()
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun setBottomSheetTab_updatesSelectedTab() {
    assertEquals(
        MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES, viewModel.selectedBottomSheetTab)
    viewModel.setBottomSheetTab(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS)
    assertEquals(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS, viewModel.selectedBottomSheetTab)
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
  fun joinedEvents_initiallyEmpty() {
    assertEquals(0, viewModel.joinedEvents.size)
  }

  @Test
  fun availableEvents_initiallyEmpty() {
    assertEquals(0, viewModel.availableEvents.size)
  }
}

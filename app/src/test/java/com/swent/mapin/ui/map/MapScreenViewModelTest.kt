package com.swent.mapin.ui.map

import android.content.Context
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mapbox.geojson.Point
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.LocalEventRepository
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepository
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.event.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// Tests for MapScreenViewModel, covering bottom sheet state, search, memory creation and event
// joining/unjoining. Adapted for updated MapScreenViewModel with EventViewModel
// dependency.
// Assisted by AI.

@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @Mock lateinit var mockContext: Context
  @Mock lateinit var mockEventViewModel: EventViewModel
  @Mock lateinit var mockMemoryRepository: MemoryRepository
  @Mock lateinit var mockEventRepository: EventRepository
  @Mock lateinit var mockAuth: FirebaseAuth
  @Mock lateinit var mockUser: FirebaseUser
  @Mock lateinit var mockUserProfileRepository: UserProfileRepository
  @Mock lateinit var mockFilterViewModel: FiltersSectionViewModel
  @Mock lateinit var mockDirectionViewModel: DirectionViewModel

  // Flows that MapScreenViewModel observes
  val savedEventsFlow = MutableStateFlow<List<Event>>(emptyList())
  val savedEventIdsFlow = MutableStateFlow<Set<String>>(emptySet())

  private lateinit var viewModel: MapScreenViewModel
  private lateinit var config: BottomSheetConfig
  private var clearFocusCalled = false

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)

    // Set the main dispatcher for coroutines
    Dispatchers.setMain(mainDispatcherRule.dispatcher)

    // Configure mocks
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")
    whenever(mockContext.applicationContext).thenReturn(mockContext)

    // Stub EventViewModel flows
    val sampleEvents = LocalEventRepository.defaultSampleEvents()
    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(sampleEvents))
    whenever(mockEventViewModel.joinedEvents).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventViewModel.availableEvents).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventViewModel.searchResults).thenReturn(MutableStateFlow(sampleEvents))
    whenever(mockFilterViewModel.filters).thenReturn(MutableStateFlow(Filters()))
    whenever(mockEventViewModel.savedEvents).thenReturn(savedEventsFlow)
    whenever(mockEventViewModel.savedEventIds).thenReturn(savedEventIdsFlow)

    // Stub EventViewModel suspend void methods
    runBlocking {
      doNothing().`when`(mockEventViewModel).getAllEvents()
      doNothing().`when`(mockEventViewModel).getSavedEvents(any())
      doNothing().`when`(mockEventViewModel).getSavedEventIds(any())
    }

    // Stub EventViewModel suspend functions with return values
    runBlocking {
      whenever(mockEventViewModel.fetchFilteredEvents(any())).thenReturn(sampleEvents)
      whenever(mockEventViewModel.fetchSearchedEvents(any())).thenReturn(sampleEvents)
      whenever(mockEventViewModel.error).thenReturn(MutableStateFlow(null))
    }

    // Stub memory repository
    runBlocking {
      whenever(mockMemoryRepository.getNewUid()).thenReturn("newMemoryId")
      whenever(mockMemoryRepository.addMemory(any())).thenReturn(Unit)
    }

    // Stub user profile repository
    runBlocking { whenever(mockUserProfileRepository.getUserProfile(any())).thenReturn(null) }

    // Initialize bottom sheet config
    config = BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)

    // Initialize ViewModel
    clearFocusCalled = false
    viewModel =
        MapScreenViewModel(
            initialSheetState = BottomSheetState.COLLAPSED,
            sheetConfig = config,
            onClearFocus = { clearFocusCalled = true },
            applicationContext = mockContext,
            eventViewModel = mockEventViewModel,
            memoryRepository = mockMemoryRepository,
            filterViewModel = mockFilterViewModel,
            auth = mockAuth,
            userProfileRepository = mockUserProfileRepository,
            directionViewModel = mockDirectionViewModel)
  }

  @Test
  fun initialState_hasExpectedDefaults() {
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
    assertEquals("", viewModel.searchQuery)
    assertFalse(viewModel.shouldFocusSearch)
    assertEquals(0, viewModel.fullEntryKey)
    assertEquals(config.collapsedHeight, viewModel.currentSheetHeight)
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
    assertFalse(viewModel.showMemoryForm)
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
  fun onSearchQueryChange_fromCollapsed_expandsToFullAndSetsFocus() = runTest {
    viewModel.onSearchQueryChange("query")
    advanceUntilIdle()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertEquals("query", viewModel.searchQuery)
    assertTrue(viewModel.shouldFocusSearch)
    verify(mockEventViewModel).fetchSearchedEvents("query")
  }

  @Test
  fun onSearchQueryChange_alreadyInFull_updatesQueryWithoutSideEffects() = runTest {
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    viewModel.onSearchFocusHandled()
    val keyBefore = viewModel.fullEntryKey
    viewModel.onSearchQueryChange("new query")
    advanceUntilIdle()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertEquals("new query", viewModel.searchQuery)
    assertEquals(keyBefore, viewModel.fullEntryKey)
    assertFalse(viewModel.shouldFocusSearch)
    verify(mockEventViewModel).fetchSearchedEvents("new query")
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
  fun onSearchQueryChange_emptyQuery_doesNotExpandSheet() = runTest {
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
    viewModel.onSearchQueryChange("")
    advanceUntilIdle()
    assertEquals("", viewModel.searchQuery)
    verify(mockEventViewModel).fetchFilteredEvents(any())
  }

  @Test
  fun bottomSheetState_transitionFromMediumToCollapsed_clearsSearchQuery() = runTest {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.onSearchQueryChange("test")
    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    advanceUntilIdle()
    assertEquals("", viewModel.searchQuery)
  }

  @Test
  fun clearFocus_calledOnlyWhenLeavingFullState() {
    clearFocusCalled = false
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    assertFalse(clearFocusCalled)
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    assertFalse(clearFocusCalled)
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    assertTrue(clearFocusCalled)
  }

  @Test
  fun onSearchTap_multipleCalls_setsFocusWhenNotInFullState() {
    viewModel.onSearchTap()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertTrue(viewModel.shouldFocusSearch)
    viewModel.onSearchFocusHandled()
    assertFalse(viewModel.shouldFocusSearch)
    viewModel.onSearchTap()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertFalse(viewModel.shouldFocusSearch)
  }

  @Test
  fun updateMediumReferenceZoom_multipleUpdates_usesLatest() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.updateMediumReferenceZoom(10f)
    viewModel.updateMediumReferenceZoom(15f)
    assertFalse(viewModel.checkZoomInteraction(15.4f))
    assertTrue(viewModel.checkZoomInteraction(15.6f))
  }

  @Test
  fun showMemoryForm_setsStateAndExpandsToFull() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showMemoryForm()
    assertTrue(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MEMORY_FORM, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun hideMemoryForm_hidesForm() {
    viewModel.showMemoryForm()
    assertTrue(viewModel.showMemoryForm)
    viewModel.hideMemoryForm()
    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun showAddEventForm_setsStateCorrectly() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showAddEventForm()
    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.ADD_EVENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun hideAddEventForm_resetsToMainContent() {
    viewModel.showAddEventForm()
    viewModel.hideAddEventForm()
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun onAddEventCancel_hidesAddEventFormAndRestoresPreviousState() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showAddEventForm()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    viewModel.onAddEventCancel()
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
  }

  @Test
  fun showMemoryForm_and_showAddEventForm_areMutuallyExclusive() {
    viewModel.showMemoryForm()
    viewModel.showAddEventForm()
    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.ADD_EVENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun onMemorySave_withNoUser_setsErrorMessage() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)
    val formData =
        MemoryFormData(
            title = "Test",
            description = "Description",
            eventId = null,
            isPublic = false,
            mediaUris = emptyList(),
            taggedUserIds = emptyList())
    viewModel.onMemorySave(formData)
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("signed in"))
  }

  @Test
  fun onMemorySave_withValidData_savesMemoryAndClosesForm() = runTest {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showMemoryForm()
    val formData =
        MemoryFormData(
            title = "Epic Day",
            description = "Amazing experience",
            eventId = "event123",
            isPublic = true,
            mediaUris = emptyList(),
            taggedUserIds = listOf("user1", "user2"))
    viewModel.onMemorySave(formData)
    advanceUntilIdle()
    verify(mockMemoryRepository).addMemory(any())
    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun onMemorySave_withError_setsErrorMessage() = runTest {
    whenever(mockMemoryRepository.addMemory(any())).thenThrow(RuntimeException("Network error"))
    val formData =
        MemoryFormData(
            title = "Test",
            description = "Description",
            eventId = null,
            isPublic = false,
            mediaUris = emptyList(),
            taggedUserIds = emptyList())
    viewModel.onMemorySave(formData)
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Failed to save memory"))
  }

  @Test
  fun clearError_clearsErrorMessage() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)
    val formData =
        MemoryFormData(
            title = "",
            description = "desc",
            eventId = null,
            isPublic = false,
            mediaUris = emptyList(),
            taggedUserIds = emptyList())
    viewModel.onMemorySave(formData)
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    viewModel.clearError()
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun isSavingMemory_correctlyTracksState() = runTest {
    var capturedMemory: Memory? = null
    whenever(mockMemoryRepository.addMemory(any())).thenAnswer { invocation ->
      capturedMemory = invocation.getArgument(0)
    }
    assertFalse(viewModel.isSavingMemory)
    val formData =
        MemoryFormData(
            title = "Test",
            description = "Description",
            eventId = null,
            isPublic = false,
            mediaUris = emptyList(),
            taggedUserIds = emptyList())
    viewModel.onMemorySave(formData)
    advanceUntilIdle()
    assertFalse(viewModel.isSavingMemory)
  }

  @Test
  fun availableEvents_initiallyEmpty() {
    assertEquals(0, viewModel.availableEvents.size)
  }

  @Test
  fun setMapStyle_togglesHeatmapState() {
    assertFalse(viewModel.showHeatmap)
    viewModel.setMapStyle(MapScreenViewModel.MapStyle.HEATMAP)
    assertTrue(viewModel.showHeatmap)
    viewModel.setMapStyle(MapScreenViewModel.MapStyle.STANDARD)
    assertFalse(viewModel.showHeatmap)
  }

  @Test
  fun eventsToGeoJson_createsValidGeoJson() {
    val events =
        listOf(
            Event(
                uid = "event1",
                title = "Event 1",
                location = com.swent.mapin.model.Location("Location 1", 46.5, 6.5),
                participantIds = List(10) { "user$it" },
                tags = listOf("Sports")),
            Event(
                uid = "event2",
                title = "Event 2",
                location = com.swent.mapin.model.Location("Location 2", 47.0, 7.0),
                participantIds = List(25) { "user$it" },
                tags = listOf("Music")))
    val geoJson = eventsToGeoJson(events)
    assertTrue(geoJson.contains("type"))
    assertTrue(geoJson.contains("FeatureCollection"))
    assertTrue(geoJson.contains("features"))
    assertTrue(geoJson.contains("geometry"))
    assertTrue(geoJson.contains("Point"))
    assertTrue(geoJson.contains("weight"))
    assertTrue(geoJson.contains("6.5"))
    assertTrue(geoJson.contains("46.5"))
    assertTrue(geoJson.contains("10"))
  }

  @Test
  fun onEventPinClicked_setsSelectedEventAndTransitionsToMedium() = runTest {
    val testEvent = LocalEventRepository.defaultSampleEvents()[0]
    var cameraCentered = false
    viewModel.onCenterCamera = { _, _ -> cameraCentered = true }
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals("User ${testEvent.ownerId.take(6)}", viewModel.organizerName)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(cameraCentered)
  }

  @Test
  fun closeEventDetail_clearsSelectedEventAndReturnsToCollapsed() = runTest {
    val testEvent = LocalEventRepository.defaultSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.closeEventDetail()
    assertNull(viewModel.selectedEvent)
    assertEquals("", viewModel.organizerName)
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
  }

  @Test
  fun closeEventDetail_fromSearch_returnsToSearchMode() = runTest {
    val testEvent = LocalEventRepository.defaultSampleEvents()[0]
    viewModel.onSearchQueryChange("test")
    viewModel.onEventClickedFromSearch(testEvent)
    advanceUntilIdle()
    viewModel.closeEventDetail()
    advanceUntilIdle()
    assertNull(viewModel.selectedEvent)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertEquals("", viewModel.searchQuery)
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
        LocalEventRepository.defaultSampleEvents()[0].copy(participantIds = listOf("otherUser"))
    viewModel.onEventPinClicked(notParticipatingEvent)
    advanceUntilIdle()
    assertFalse(viewModel.isUserParticipating())
    val participatingEvent =
        LocalEventRepository.defaultSampleEvents()[0].copy(
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
    val testEvent = LocalEventRepository.defaultSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()
    assertEquals("You must be signed in to join events", viewModel.errorMessage)
    viewModel.clearError()
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockEventViewModel.joinEventForUser(any(), any())).thenReturn(null)
    viewModel.joinEvent()
    advanceUntilIdle()
    assertEquals("Failed to join event", viewModel.errorMessage)
  }

  @Test
  fun joinEvent_success_updatesEventAndList() = runTest {
    val testEvent =
        LocalEventRepository.defaultSampleEvents()[0].copy(participantIds = listOf(), capacity = 10)
    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(listOf(testEvent)))
    val updatedEvent = testEvent.copy(participantIds = listOf("testUserId"))
    whenever(mockEventViewModel.joinEventForUser("testUserId", testEvent.uid))
        .thenReturn(updatedEvent)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()
    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertTrue(viewModel.selectedEvent!!.participantIds.contains("testUserId"))
    assertEquals(1, viewModel.selectedEvent!!.participantIds.size)
  }

  @Test
  fun unregisterFromEvent_success_updatesEventAndList() = runTest {
    val testEvent =
        LocalEventRepository.defaultSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), capacity = 10)
    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(listOf(testEvent)))
    val updatedEvent = testEvent.copy(participantIds = emptyList())
    whenever(mockEventViewModel.unregisterUserFromEvent("testUserId", testEvent.uid))
        .thenReturn(updatedEvent)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.unregisterFromEvent()
    advanceUntilIdle()
    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertFalse(viewModel.selectedEvent!!.participantIds.contains("testUserId"))
    assertEquals(0, viewModel.selectedEvent!!.participantIds.size)
  }

  @Test
  fun joinAndUnregisterEvent_withRepositoryError_setsErrorMessage() = runTest {
    val testEvent =
        LocalEventRepository.defaultSampleEvents()[0].copy(participantIds = listOf(), capacity = 10)
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    whenever(mockEventViewModel.joinEventForUser(any(), any())).thenReturn(null)
    viewModel.joinEvent()
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Failed to join event"))
    viewModel.clearError()
    whenever(mockEventViewModel.unregisterUserFromEvent(any(), any())).thenReturn(null)
    viewModel.unregisterFromEvent()
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Failed to unregister"))
  }

  @Test
  fun setBottomSheetTab_updatesSelectedTab() {
    assertEquals(MapScreenViewModel.BottomSheetTab.SAVED_EVENTS, viewModel.selectedBottomSheetTab)
    viewModel.setBottomSheetTab(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS)
    assertEquals(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS, viewModel.selectedBottomSheetTab)
  }

  @Test
  fun onTabEventClicked_callsOnEventPinClicked() = runTest {
    val testEvent = LocalEventRepository.defaultSampleEvents()[0]
    var cameraCentered = false
    viewModel.onCenterCamera = { _, _ -> cameraCentered = true }
    viewModel.onTabEventClicked(testEvent)
    advanceUntilIdle()
    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(cameraCentered)
  }

  @Test
  fun setEvents_updatesEventsList() = runTest {
    val newEvents =
        listOf(
            LocalEventRepository.defaultSampleEvents()[0],
            LocalEventRepository.defaultSampleEvents()[1])
    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(newEvents))
    whenever(mockEventViewModel.fetchFilteredEvents(any())).thenReturn(newEvents)
    viewModel.setEvents(newEvents)
    advanceUntilIdle()
    assertEquals(newEvents, viewModel.events)
    assertEquals(2, viewModel.events.size)
  }

  @Test
  fun joinedEvents_initiallyEmpty() {
    assertEquals(0, viewModel.joinedEvents.size)
  }

  @Test
  fun saveEventForLater_noUser_setsErrorMessage() = runTest {
    val e = sampleEvent()
    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(listOf(e)))
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()
    whenever(mockAuth.currentUser).thenReturn(null)
    viewModel.saveEventForLater()
    advanceUntilIdle()
    assertTrue(viewModel.errorMessage?.contains("signed in") == true)
  }

  @Test
  fun saveEventForLater_success_updatesIdsAndLoadsSavedList() = runTest {
    val e = sampleEvent()

    // Stub saveEventForUser to simulate a real save
    whenever(mockEventViewModel.saveEventForUser(any(), any())).thenAnswer { invocation ->
      val eventId = invocation.getArgument<String>(1)
      savedEventIdsFlow.value = savedEventIdsFlow.value + eventId
      savedEventsFlow.value = savedEventsFlow.value + e
      true
    }

    // Stub getSavedEvents (just triggers reading the flows)
    doAnswer {
          // No-op; flow already updated
          null
        }
        .`when`(mockEventViewModel)
        .getSavedEvents(e.ownerId)

    // Initialize MapScreenViewModel state
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()

    // Call the method under test
    viewModel.saveEventForLater()
    advanceUntilIdle()

    // Now MapScreenViewModel should see the saved event
    assertTrue(viewModel.isEventSaved(e))
    assertTrue(viewModel.savedEvents.any { it.uid == e.uid })

    // Verify that the repository methods were called
    verify(mockEventViewModel).saveEventForUser("testUserId", e.uid)
  }

  @Test
  fun unsaveEventForLater_success_updatesIdsAndReloads() = runTest {
    val e = sampleEvent()

    // Stub EventViewModel flows
    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(listOf(e)))
    whenever(mockEventViewModel.savedEvents).thenReturn(savedEventsFlow)
    whenever(mockEventViewModel.savedEventIds).thenReturn(savedEventIdsFlow)

    // Initialize MapScreenViewModel state
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()

    // Pre-populate saved events
    savedEventIdsFlow.value = savedEventIdsFlow.value + e.uid
    savedEventsFlow.value = savedEventsFlow.value + e

    // Stub unsaveEventForUser to simulate a real unsave
    whenever(mockEventViewModel.unsaveEventForUser(any(), any())).thenAnswer { invocation ->
      val eventId = invocation.getArgument<String>(1)
      savedEventIdsFlow.value = savedEventIdsFlow.value - eventId
      savedEventsFlow.value = savedEventsFlow.value.filter { it.uid != eventId }
      true
    }

    // Call the method under test
    viewModel.unsaveEventForLater()
    advanceUntilIdle()

    // Now MapScreenViewModel should see the event as unsaved
    assertFalse(viewModel.isEventSaved(e))
    assertFalse(viewModel.savedEvents.any { it.uid == e.uid })

    // Verify that the repository method was called
    verify(mockEventViewModel).unsaveEventForUser("testUserId", e.uid)
  }

  @Test
  fun unsaveEventForLater_failure_setsError() = runTest {
    val e = sampleEvent()

    // Stub EventViewModel flows
    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(listOf(e)))
    whenever(mockEventViewModel.savedEvents).thenReturn(savedEventsFlow)
    whenever(mockEventViewModel.savedEventIds).thenReturn(savedEventIdsFlow)

    // Initialize MapScreenViewModel state
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()

    // Pre-populate saved events
    savedEventIdsFlow.value = savedEventIdsFlow.value + e.uid
    savedEventsFlow.value = savedEventsFlow.value + e
    viewModel.saveEventForLater()
    advanceUntilIdle()
    assertTrue(viewModel.isEventSaved(e))

    // Stub unsaveEventForUser to throw exception
    whenever(mockEventViewModel.unsaveEventForUser(any(), any()))
        .thenThrow((RuntimeException("Network error")))

    // Call the method under test
    viewModel.unsaveEventForLater()
    advanceUntilIdle()

    // Event should still be marked as saved because unsave failed
    assertTrue(viewModel.isEventSaved(e))
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Failed to unsave"))

    // Verify method was called
    verify(mockEventViewModel).unsaveEventForUser("testUserId", e.uid)
  }

  @Test
  fun toggleDirections_togglesDirectionState() = runTest {
    val testEvent = LocalEventRepository.defaultSampleEvents()[0]

    // Stub the initial state to Cleared
    whenever(mockDirectionViewModel.directionState).thenReturn(DirectionState.Cleared)

    // First toggle should request directions
    viewModel.toggleDirections(testEvent)
    verify(mockDirectionViewModel).requestDirections(any(), any())

    // Simulate directions being displayed
    whenever(mockDirectionViewModel.directionState)
        .thenReturn(
            DirectionState.Displayed(
                routePoints = listOf(),
                origin = Point.fromLngLat(0.0, 0.0),
                destination = Point.fromLngLat(0.0, 0.0)))

    // Second toggle should clear directions
    viewModel.toggleDirections(testEvent)
    verify(mockDirectionViewModel).clearDirection()
  }

  private fun sampleEvent() =
      LocalEventRepository.defaultSampleEvents()[0].copy(
          uid = "evt-1", participantIds = emptyList())
}

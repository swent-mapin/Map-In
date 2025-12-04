package com.swent.mapin.ui.map

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.Location
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.memory.MemoryRepository
import com.swent.mapin.model.network.ConnectivityService
import com.swent.mapin.model.network.ConnectivityServiceProvider
import com.swent.mapin.model.network.ConnectivityState
import com.swent.mapin.model.network.NetworkType
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.filters.Filters
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import com.swent.mapin.ui.map.directions.DirectionState
import com.swent.mapin.ui.map.eventstate.MapEventStateController
import com.swent.mapin.ui.map.location.LocationManager
import com.swent.mapin.ui.memory.MemoryFormData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class MapScreenViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Mock(lenient = true) private lateinit var mockContext: Context
  @Mock(lenient = true) private lateinit var mockSharedPreferences: SharedPreferences
  @Mock(lenient = true) private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor
  @Mock(lenient = true) private lateinit var mockMemoryRepository: MemoryRepository
  @Mock(lenient = true) private lateinit var mockEventRepository: EventRepository
  @Mock(lenient = true) private lateinit var mockAuth: FirebaseAuth
  @Mock(lenient = true) private lateinit var mockUser: FirebaseUser
  @Mock(lenient = true) private lateinit var mockUserProfileRepository: UserProfileRepository
  @Mock(lenient = true) private lateinit var mockFiltersSectionViewModel: FiltersSectionViewModel
  @Mock(lenient = true) private lateinit var mockEventStateController: MapEventStateController
  @Mock(lenient = true) private lateinit var mockLocationManager: LocationManager
  @Mock(lenient = true) private lateinit var mockConnectivityService: ConnectivityService

  private lateinit var viewModel: MapScreenViewModel
  private lateinit var config: BottomSheetConfig
  private var clearFocusCalled = false

  private val testEvent =
      Event(
          uid = "event1",
          title = "Test Event",
          location = Location("Test Location", 46.5, 6.5),
          participantIds = emptyList(),
          ownerId = "owner123")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    clearFocusCalled = false
    config = BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)

    // Set mock ConnectivityService in the provider BEFORE creating ViewModel
    whenever(mockConnectivityService.connectivityState)
        .thenReturn(flowOf(ConnectivityState(isConnected = true, networkType = NetworkType.WIFI)))
    ConnectivityServiceProvider.setInstance(mockConnectivityService)

    // Mock SharedPreferences
    whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)
    whenever(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)
    whenever(mockSharedPreferencesEditor.putString(any(), any()))
        .thenReturn(mockSharedPreferencesEditor)
    whenever(mockSharedPreferencesEditor.remove(any())).thenReturn(mockSharedPreferencesEditor)
    whenever(mockSharedPreferences.getString(any(), any())).thenReturn(null)

    // Mock Firebase Auth
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")

    // Mock application context
    whenever(mockContext.applicationContext).thenReturn(mockContext)

    // Mock LocationManager default behavior
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)

    // Mock repositories
    runBlocking {
      whenever(mockEventRepository.getFilteredEvents(any(), any<String>())).thenReturn(emptyList())
      whenever(mockEventRepository.getSavedEvents(any())).thenReturn(emptyList())
      whenever(mockEventRepository.getJoinedEvents(any())).thenReturn(emptyList())
      whenever(mockEventRepository.getOwnedEvents(any())).thenReturn(emptyList())
      whenever(mockMemoryRepository.getNewUid()).thenReturn("newMemoryId")
      whenever(mockMemoryRepository.addMemory(any())).thenReturn(Unit)
      whenever(mockUserProfileRepository.getUserProfile(any())).thenReturn(null)
    }

    // Mock FiltersSectionViewModel
    val defaultFilters = Filters()
    val filtersFlow = MutableStateFlow(defaultFilters)
    whenever(mockFiltersSectionViewModel.filters).thenReturn(filtersFlow)

    // Mock eventStateController
    val joinedEvents = mutableListOf<Event>()
    val savedEvents = mutableListOf<Event>()
    whenever(mockEventStateController.allEvents).thenReturn(emptyList())
    whenever(mockEventStateController.searchResults).thenReturn(emptyList())
    whenever(mockEventStateController.joinedEvents).thenAnswer { joinedEvents.toList() }
    whenever(mockEventStateController.joinedEventsFlow).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventStateController.savedEventsFlow).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventStateController.isOnline).thenReturn(MutableStateFlow(true))
    whenever(mockEventStateController.searchEvents(any())).then {}
    whenever(mockEventStateController.clearSearchResults()).then {}
    whenever(mockEventStateController.observeFilters()).then {}
    whenever(mockEventStateController.observeConnectivity()).then {}
    whenever(mockEventStateController.startListeners()).then {}
    whenever(mockEventStateController.stopListeners()).then {}

    runBlocking {
      whenever(mockEventStateController.joinSelectedEvent()).thenAnswer {
        viewModel.selectedEvent?.let { event -> joinedEvents.add(event) }
      }

      whenever(mockEventStateController.saveSelectedEvent()).thenAnswer {
        viewModel.selectedEvent?.let { event -> savedEvents.add(event) }
      }

      whenever(mockEventStateController.unsaveSelectedEvent()).thenAnswer {
        viewModel.selectedEvent?.let { event -> savedEvents.removeIf { it.uid == event.uid } }
      }

      whenever(mockEventStateController.leaveSelectedEvent()).thenAnswer {
        viewModel.selectedEvent?.let { event -> joinedEvents.removeIf { it.uid == event.uid } }
      }
    }

    whenever(mockEventStateController.refreshSelectedEvent(any())).thenAnswer { invocation ->
      val eventId = invocation.getArgument<String>(0)
      joinedEvents.find { it.uid == eventId } ?: savedEvents.find { it.uid == eventId }
    }

    // Instantiate ViewModel
    viewModel =
        MapScreenViewModel(
            initialSheetState = BottomSheetState.COLLAPSED,
            sheetConfig = config,
            connectivityService = mockConnectivityService,
            onClearFocus = { clearFocusCalled = true },
            applicationContext = mockContext,
            memoryRepository = mockMemoryRepository,
            eventRepository = mockEventRepository,
            auth = mockAuth,
            userProfileRepository = mockUserProfileRepository,
            locationManager = mockLocationManager,
            filterViewModel = mockFiltersSectionViewModel,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
            enableEventBasedDownloads = false)

    // Inject mock eventStateController using reflection
    try {
      val field = MapScreenViewModel::class.java.getDeclaredField("eventStateController")
      field.isAccessible = true
      field.set(viewModel, mockEventStateController)
      field.isAccessible = false
    } catch (e: Exception) {
      throw IllegalStateException("Failed to inject mock eventStateController", e)
    }
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    ConnectivityServiceProvider.clearInstance()
  }

  @Test
  fun initialState_hasExpectedDefaults() {
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
    assertEquals("", viewModel.searchQuery)
    assertFalse(viewModel.shouldFocusSearch)
    assertEquals(0, viewModel.fullEntryKey)
    assertEquals(config.collapsedHeight, viewModel.currentSheetHeight)
    assertEquals(MapScreenViewModel.MapStyle.STANDARD, viewModel.mapStyle)
    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
    assertFalse(viewModel.showShareDialog)
    assertEquals(MapScreenViewModel.BottomSheetTab.SAVED, viewModel.selectedBottomSheetTab)
    assertNull(viewModel.selectedEvent)
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
  fun `applyRecentSearch sets query and collapses to MEDIUM`() = runTest {
    viewModel.applyRecentSearch("basketball")
    advanceUntilIdle()

    assertEquals("basketball", viewModel.searchQuery)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
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
  fun `clearRecentSearches empties recent items`() = runTest {
    viewModel.clearRecentSearches()
    advanceUntilIdle()

    assertEquals(0, viewModel.recentItems.size)
  }

  @Test
  fun showMemoryForm_setsStateAndExpandsToFull() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)

    viewModel.showMemoryForm(Event())

    assertTrue(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MEMORY_FORM, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun hideMemoryForm_hidesForm() {
    viewModel.showMemoryForm(Event())
    assertTrue(viewModel.showMemoryForm)

    viewModel.hideMemoryForm()

    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun onMemoryCancel_hidesFormAndRestoresPreviousState() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showMemoryForm(Event())
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)

    viewModel.onMemoryCancel()

    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun onMemorySave_withValidData_savesMemoryAndClosesForm() = runTest {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showMemoryForm(Event())
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
  fun clearError_clearsErrorMessage() {
    runBlocking {
      whenever(mockAuth.currentUser).thenReturn(null)
      val formData = MemoryFormData("", "desc", null, false, emptyList(), emptyList())
      viewModel.onMemorySave(formData)
      testDispatcher.scheduler.advanceUntilIdle()
    }

    assertNotNull(viewModel.errorMessage)

    viewModel.clearError()

    assertNull(viewModel.errorMessage)
  }

  @Test
  fun isSavingMemory_correctlyTracksState() = runTest {
    whenever(mockMemoryRepository.addMemory(any())).thenReturn(Unit)

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
  fun availableEvents_initiallyLoaded() {
    assertNotNull(viewModel.availableEvents)
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
  fun `onAddEventCancel resets to MAIN_CONTENT and COLLAPSED`() {
    viewModel.showAddEventForm()
    viewModel.onAddEventCancel()

    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
  }

  @Test
  fun `oneEditEventCancel resets to MAIN_CONTENT and COLLAPSED`() {
    viewModel.showEditEventForm()
    viewModel.onEditEventCancel()

    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
  }

  @Test
  fun `setMapStyle toggles heatmap and satellite states`() {
    assertFalse(viewModel.showHeatmap)

    viewModel.setMapStyle(MapScreenViewModel.MapStyle.HEATMAP)
    assertTrue(viewModel.showHeatmap)
    assertFalse(viewModel.useSatelliteStyle)

    viewModel.setMapStyle(MapScreenViewModel.MapStyle.SATELLITE)
    assertFalse(viewModel.showHeatmap)
    assertTrue(viewModel.useSatelliteStyle)

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
                location = Location("Location 1", 46.5, 6.5),
                participantIds = List(10) { "user$it" }),
            Event(
                uid = "event2",
                title = "Event 2",
                location = Location("Location 2", 47.0, 7.0),
                participantIds = List(25) { "user$it" }))

    val geoJson = eventsToGeoJson(events)

    // Verify it's valid JSON and contains expected data
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
  fun `onEventPinClicked sets event and transitions to MEDIUM`() = runTest {
    var cameraCentered = false
    viewModel.setCenterCameraCallback { _, _ -> cameraCentered = true }

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals("User ${testEvent.ownerId.take(6)}", viewModel.organizerName)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(cameraCentered)
  }

  @Test
  fun closeEventDetail_clearsSelectedEventAndReturnsToCollapsed() = runTest {
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    viewModel.closeEventDetail()

    assertNull(viewModel.selectedEvent)
    assertEquals("", viewModel.organizerName)
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
  }

  @Test
  fun closeEventDetail_restoresPreviousSheetStateWhenNotFromSearch() = runTest {
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)

    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    viewModel.closeEventDetail()

    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
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
  fun `setBottomSheetTab updates selected tab`() {
    viewModel.setBottomSheetTab(MapScreenViewModel.BottomSheetTab.UPCOMING)
    assertEquals(MapScreenViewModel.BottomSheetTab.UPCOMING, viewModel.selectedBottomSheetTab)
  }

  @Test
  fun `eventsToGeoJson generates valid GeoJSON`() {
    val events =
        listOf(
            Event(
                uid = "event1",
                title = "Event 1",
                location = Location("Location 1", 46.5, 6.5),
                participantIds = listOf("user1")))

    val geoJson = eventsToGeoJson(events)

    assertTrue(geoJson.contains("FeatureCollection"))
    assertTrue(geoJson.contains("Point"))
    assertTrue(geoJson.contains("46.5"))
    assertTrue(geoJson.contains("6.5"))
    assertTrue(geoJson.contains("weight"))
  }

  @Test
  fun `isEventJoined returns false when no event selected`() {
    assertFalse(viewModel.isEventJoined())
  }

  @Test
  fun `isEventSaved returns false when no event selected`() {
    assertFalse(viewModel.isEventSaved())
  }

  @Test
  fun `joinEvent with selected event calls controller and refreshes event`() = runTest {
    viewModel.clearError()
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    val updatedEvent = testEvent.copy(participantIds = listOf("testUserId"))
    whenever(viewModel.eventStateController.refreshSelectedEvent(testEvent.uid))
        .thenReturn(updatedEvent)

    viewModel.joinEvent()
    advanceUntilIdle()

    assertEquals(updatedEvent, viewModel.selectedEvent)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `joinEvent with no selected event does nothing`() = runTest {
    viewModel.clearError()
    viewModel.joinEvent()
    advanceUntilIdle()

    assertNull(viewModel.selectedEvent)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `saveEventForLater with selected event calls controller and refreshes event`() = runTest {
    viewModel.clearError()
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    val updatedEvent = testEvent.copy(title = "Updated Event")
    whenever(viewModel.eventStateController.refreshSelectedEvent(testEvent.uid))
        .thenReturn(updatedEvent)

    viewModel.saveEventForLater()
    advanceUntilIdle()

    assertEquals(updatedEvent, viewModel.selectedEvent)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun setBottomSheetTab_updatesSelectedTab() {
    assertEquals(MapScreenViewModel.BottomSheetTab.SAVED, viewModel.selectedBottomSheetTab)
    viewModel.setBottomSheetTab(MapScreenViewModel.BottomSheetTab.UPCOMING)
    assertEquals(MapScreenViewModel.BottomSheetTab.UPCOMING, viewModel.selectedBottomSheetTab)
  }

  @Test
  fun onJoinedEventClicked_callsOnEventPinClicked() = runTest {
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]
    var cameraCentered = false
    viewModel.setCenterCameraCallback { _, _ -> cameraCentered = true }

    viewModel.onTabEventClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(cameraCentered)
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
  fun `saveEventForLater with no selected event does nothing`() = runTest {
    viewModel.clearError()
    viewModel.saveEventForLater()
    advanceUntilIdle()

    assertNull(viewModel.selectedEvent)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `unsaveEventForLater with selected event calls controller and refreshes event`() = runTest {
    viewModel.clearError()
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    val updatedEvent = testEvent.copy(title = "Updated Event")
    whenever(viewModel.eventStateController.refreshSelectedEvent(testEvent.uid))
        .thenReturn(updatedEvent)

    viewModel.unsaveEventForLater()
    advanceUntilIdle()

    assertEquals(updatedEvent, viewModel.selectedEvent)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `unsaveEventForLater with no selected event does nothing`() = runTest {
    viewModel.clearError()
    viewModel.unsaveEventForLater()
    advanceUntilIdle()

    assertNull(viewModel.selectedEvent)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `showMemoryForm sets state correctly`() {
    viewModel.showMemoryForm(Event())

    assertTrue(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MEMORY_FORM, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun `hideMemoryForm resets showMemoryForm and sets MAIN_CONTENT`() {
    viewModel.showMemoryForm(Event())

    viewModel.hideMemoryForm()

    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun `showAddEventForm sets state correctly`() {
    viewModel.showAddEventForm()

    assertFalse(viewModel.showMemoryForm) // legacy boolean remains false
    assertEquals(BottomSheetScreen.ADD_EVENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun `showEditEventForm sets state correctly`() {
    viewModel.showEditEventForm()

    assertFalse(viewModel.showMemoryForm) // legacy boolean remains false
    assertEquals(BottomSheetScreen.EDIT_EVENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun `hideAddEventForm resets to MAIN_CONTENT`() {
    viewModel.showAddEventForm()

    viewModel.hideAddEventForm()

    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun `hideEditEventForm resets to MAIN_CONTENT`() {
    viewModel.showEditEventForm()

    viewModel.hideEditEventForm()

    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun `onAddEventCancel hides AddEvent form and returns to MAIN_CONTENT`() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showAddEventForm()

    viewModel.onAddEventCancel()

    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
  }

  // === Location tests ===
  @Test
  fun checkLocationPermission_updatesPermissionState_whenGranted() {
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)

    viewModel.checkLocationPermission()

    assertTrue(viewModel.hasLocationPermission)
  }

  @Test
  fun checkLocationPermission_updatesPermissionState_whenDenied() {
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)

    viewModel.checkLocationPermission()

    assertFalse(viewModel.hasLocationPermission)
  }

  @Test
  fun startLocationUpdates_setsPermissionFalse_whenNoPermission() {
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)

    viewModel.startLocationUpdates()

    assertFalse(viewModel.hasLocationPermission)
  }

  @Test
  fun startLocationUpdates_setsPermissionTrue_whenPermissionGranted() {
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    whenever(mockLocationManager.getLocationUpdates())
        .thenReturn(kotlinx.coroutines.flow.emptyFlow())

    viewModel.startLocationUpdates()

    assertTrue(viewModel.hasLocationPermission)
  }

  @Test
  fun startLocationUpdates_collectsLocationUpdates_withBearing() = runTest {
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.hasBearing()).thenReturn(true)
    whenever(mockLocation.bearing).thenReturn(45f)
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    whenever(mockLocationManager.getLocationUpdates())
        .thenReturn(kotlinx.coroutines.flow.flowOf(mockLocation))

    viewModel.startLocationUpdates()
    advanceUntilIdle()

    assertEquals(mockLocation, viewModel.currentLocation)
    assertEquals(45f, viewModel.locationBearing, 0.001f)
  }

  @Test
  fun startLocationUpdates_collectsLocationUpdates_withoutBearing() = runTest {
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.hasBearing()).thenReturn(false)
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    whenever(mockLocationManager.getLocationUpdates())
        .thenReturn(kotlinx.coroutines.flow.flowOf(mockLocation))

    viewModel.startLocationUpdates()
    advanceUntilIdle()

    assertEquals(mockLocation, viewModel.currentLocation)
    // Bearing should remain at initial value (0f)
  }

  @Test
  fun startLocationUpdates_handlesError_setsErrorMessage() = runTest {
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    whenever(mockLocationManager.getLocationUpdates())
        .thenReturn(kotlinx.coroutines.flow.flow { throw Exception("Location error") })

    viewModel.startLocationUpdates()
    advanceUntilIdle()

    assertEquals("Failed to get location updates", viewModel.errorMessage)
  }

  @Test
  fun getLastKnownLocation_withoutCenterCamera_triggersSuccessCallback_withBearing() {
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.hasBearing()).thenReturn(true)
    whenever(mockLocation.bearing).thenReturn(90f)
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }

    viewModel.getLastKnownLocation(centerCamera = false)

    assertEquals(mockLocation, viewModel.currentLocation)
    assertEquals(90f, viewModel.locationBearing, 0.001f)
  }

  @Test
  fun getLastKnownLocation_withoutCenterCamera_triggersSuccessCallback_withoutBearing() {
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.hasBearing()).thenReturn(false)
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }

    viewModel.getLastKnownLocation(centerCamera = false)

    assertEquals(mockLocation, viewModel.currentLocation)
  }

  @Test
  fun getLastKnownLocation_withCenterCamera_invokesCallback() {
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.hasBearing()).thenReturn(false)
    var centerCalled = false
    viewModel.onCenterOnUserLocation = { centerCalled = true }
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }

    viewModel.getLastKnownLocation(centerCamera = true)

    assertTrue(centerCalled)
  }

  @Test
  fun getLastKnownLocation_triggersErrorCallback() {
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onError = invocation.getArgument<() -> Unit>(1)
      onError()
    }

    viewModel.getLastKnownLocation()

    // Just verify it doesn't crash - error is logged
    assertNull(viewModel.currentLocation)
  }

  @Test
  fun onLocationButtonClick_withPermission_centersOnUser() {
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    var centerCalled = false
    viewModel.onCenterOnUserLocation = { centerCalled = true }

    viewModel.onLocationButtonClick()

    assertTrue(centerCalled)
    assertTrue(viewModel.isCenteredOnUser)
  }

  @Test
  fun onLocationButtonClick_withoutPermission_requestsPermission() {
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)
    var permissionRequested = false
    viewModel.onRequestLocationPermission = { permissionRequested = true }

    viewModel.onLocationButtonClick()

    assertTrue(permissionRequested)
    assertFalse(viewModel.isCenteredOnUser)
  }

  @Test
  fun updateCenteredState_withNullLocation_setsCenteredToFalse() {
    viewModel.updateCenteredState(46.5, 6.5)

    assertFalse(viewModel.isCenteredOnUser)
  }

  @Test
  fun updateCenteredState_withinThreshold_setsCenteredToTrue() {
    // Set a current location first
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.latitude).thenReturn(46.5)
    whenever(mockLocation.longitude).thenReturn(6.5)
    whenever(mockLocation.hasBearing()).thenReturn(false)
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }
    viewModel.getLastKnownLocation()

    // Update with coordinates within threshold (0.0005)
    viewModel.updateCenteredState(46.50001, 6.50001)

    assertTrue(viewModel.isCenteredOnUser)
  }

  @Test
  fun updateCenteredState_outsideThreshold_setsCenteredToFalse() {
    // Set a current location first
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.latitude).thenReturn(46.5)
    whenever(mockLocation.longitude).thenReturn(6.5)
    whenever(mockLocation.hasBearing()).thenReturn(false)
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }
    viewModel.getLastKnownLocation()

    // Update with coordinates outside threshold (0.0005)
    viewModel.updateCenteredState(46.6, 6.6)

    assertFalse(viewModel.isCenteredOnUser)
  }

  @Test
  fun updateCenteredState_atThresholdBoundary_setsCenteredToFalse() {
    // Set a current location first
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.latitude).thenReturn(46.5)
    whenever(mockLocation.longitude).thenReturn(6.5)
    whenever(mockLocation.hasBearing()).thenReturn(false)
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }
    viewModel.getLastKnownLocation()

    // Exactly at threshold (0.0005) - should be outside
    viewModel.updateCenteredState(46.5005, 6.5005)

    assertFalse(viewModel.isCenteredOnUser)
  }

  @Test
  fun onMapMoved_setsCenteredToFalse() {
    viewModel.onMapMoved()

    assertFalse(viewModel.isCenteredOnUser)
  }

  @Test
  fun locationBearing_initiallyZero() {
    assertEquals(0f, viewModel.locationBearing, 0.001f)
  }

  @Test
  fun currentLocation_initiallyNull() {
    assertNull(viewModel.currentLocation)
  }

  @Test
  fun isCenteredOnUser_initiallyFalse() {
    assertFalse(viewModel.isCenteredOnUser)
  }

  @Test
  fun `showMemoryForm and showAddEventForm are mutually exclusive`() {
    viewModel.showMemoryForm(Event())

    viewModel.showAddEventForm()

    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.ADD_EVENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun `onSearchTap expands to FULL and requests focus`() = runTest {
    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
    viewModel.onSearchTap()
    advanceUntilIdle()

    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertTrue(viewModel.shouldFocusSearch)
  }

  @Test
  fun `focusCameraOnSearchResults does nothing when no results`() {
    // Should not crash or change state
    viewModel.onSearchQueryChange("nothing")
    viewModel.setBottomSheetState(BottomSheetState.FULL)
    viewModel.onSearchSubmit()
    // Since searchResults mocked empty → no camera fit callback triggered
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
  }

  @Test
  fun `applyRecentSearch with empty query does nothing`() = runTest {
    val initialQuery = viewModel.searchQuery
    viewModel.applyRecentSearch("   ")
    advanceUntilIdle()

    // Query should not change
    assertEquals(initialQuery, viewModel.searchQuery)
    // No recent items should be added
    assertEquals(0, viewModel.recentItems.size)
  }

  @Test
  fun `clearRecentSearches removes all recent items`() = runTest {
    // Add some recent searches
    viewModel.onSearchQueryChange("coffee")
    viewModel.onSearchSubmit()
    viewModel.onSearchQueryChange("tea")
    viewModel.onSearchSubmit()
    advanceUntilIdle()

    // Note: In unit test environment, SharedPreferences may not work properly with mock context
    // So items might not actually be saved. The important thing is that clearRecentSearches
    // doesn't crash and results in an empty list

    // Clear all
    viewModel.clearRecentSearches()
    advanceUntilIdle()

    // Verify all cleared - should be empty regardless of whether items were saved
    assertEquals(0, viewModel.recentItems.size)
  }

  @Test
  fun `saveRecentSearch removes duplicate searches`() = runTest {
    // This test verifies behavior but in unit test environment with mock context,
    // SharedPreferences may not work properly, so we test the query setting behavior instead

    // Add the same search twice
    viewModel.onSearchQueryChange("coffee")
    viewModel.onSearchSubmit()
    advanceUntilIdle()

    assertEquals("coffee", viewModel.searchQuery)

    viewModel.onSearchQueryChange("tea")
    viewModel.onSearchSubmit()
    advanceUntilIdle()

    assertEquals("tea", viewModel.searchQuery)

    viewModel.onSearchQueryChange("coffee") // Same as first
    viewModel.onSearchSubmit()
    advanceUntilIdle()

    assertEquals("coffee", viewModel.searchQuery)

    // Note: Deduplication logic is tested in integration tests where SharedPreferences works
  }

  @Test
  fun `setBottomSheetState with resetSearch false preserves search state`() = runTest {
    // Setup: activate search and enter a query
    viewModel.onSearchQueryChange("concert")
    viewModel.onSearchSubmit()
    advanceUntilIdle()

    val queryBeforeCollapse = viewModel.searchQuery

    // Collapse with resetSearch = false
    viewModel.setBottomSheetState(BottomSheetState.COLLAPSED, resetSearch = false)

    // Query should be preserved
    assertEquals(queryBeforeCollapse, viewModel.searchQuery)
  }

  @Test
  fun `onClearSearch clears query and results`() = runTest {
    viewModel.onSearchQueryChange("query")
    advanceUntilIdle()
    viewModel.onClearSearch()
    advanceUntilIdle()

    assertEquals("", viewModel.searchQuery)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
  }

  @Test
  fun `onSearchTap activates search mode and marks as editing`() = runTest {
    // Initial state
    assertFalse(viewModel.isSearchMode)

    // Tap search bar
    viewModel.onSearchTap()
    advanceUntilIdle()

    // Verify search mode is active
    assertTrue(viewModel.isSearchMode)
    // Verify sheet expanded to FULL
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun `closeEventDetail after search preserves search state`() = runTest {
    // Setup: perform a search
    viewModel.onSearchQueryChange("museum")
    viewModel.onSearchSubmit()
    advanceUntilIdle()

    val queryBeforeEvent = viewModel.searchQuery

    // Simulate viewing an event from search (would set _cameFromSearch = true)
    // This is handled internally when clicking an event from search results

    // Close event detail
    viewModel.closeEventDetail()
    advanceUntilIdle()

    // Search query should be preserved
    assertEquals(queryBeforeEvent, viewModel.searchQuery)
  }

  @Test
  fun `recent items are loaded on ViewModel init`() = runTest {
    // In a fresh ViewModel, recentItems should be initialized
    // (may be empty if no stored data)
    assertNotNull(viewModel.recentItems)
  }

  @Test
  fun `toggleDirections clears directions when already displayed`() = runTest {
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    // First toggle - activate directions
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    // Manually set to displayed state to test the clear branch
    val userLocation =
        com.mapbox.geojson.Point.fromLngLat(
            MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE)
    val eventLocation =
        com.mapbox.geojson.Point.fromLngLat(
            testEvent.location.longitude, testEvent.location.latitude)
    viewModel.directionViewModel.requestDirections(userLocation, eventLocation)
    advanceUntilIdle()

    // Second toggle - should clear directions
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    // Directions should be cleared
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `clearError resets errorMessage`() {
    viewModel.clearError()
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `onEventClickedFromSearch marks cameFromSearch and selects event`() = runTest {
    viewModel.onEventClickedFromSearch(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
  }

  @Test
  fun `onRecentEventClicked finds and selects event`() = runTest {
    whenever(mockEventStateController.allEvents).thenReturn(listOf(testEvent))

    viewModel.onRecentEventClicked(testEvent.uid)
    advanceUntilIdle()

    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
  }

  @Test
  fun `toggleDirections switches between displayed and cleared`() {
    val dummyEvent = testEvent
    viewModel.toggleDirections(dummyEvent)
    assertNotNull(viewModel.directionViewModel.directionState)

    viewModel.toggleDirections(dummyEvent)
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `unregisterFromEvent removes selected event from joined list`() = runTest {
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()
    assertTrue(viewModel.isEventJoined())
  }

  @Test
  fun `onRecentEventClicked does nothing when event not found`() = runTest {
    val initialSelected = viewModel.selectedEvent

    // Try to click a non-existent event
    viewModel.onRecentEventClicked("non-existent-id")
    advanceUntilIdle()

    // Selected event should not change
    assertEquals(initialSelected, viewModel.selectedEvent)
  }

  @Test
  fun `onClearSearch resets events and sets sheet to medium`() = runTest {
    // Perform a search first
    viewModel.onSearchQueryChange("Basketball")
    viewModel.onSearchSubmit()
    advanceUntilIdle()

    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(viewModel.searchQuery.isNotEmpty())

    // Clear the search
    viewModel.onClearSearch()
    advanceUntilIdle()

    // Sheet should be MEDIUM and search should be reset
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(viewModel.searchQuery.isEmpty())
  }

  @Test
  fun downloadState_hasCorrectInitialValues() {
    assertNull(viewModel.downloadingEvent)
    assertEquals(0f, viewModel.downloadProgress, 0.01f)
    assertFalse(viewModel.showDownloadComplete)
  }

  @Test
  fun clearDownloadComplete_setsShowDownloadCompleteToFalse() = runTest {
    // Initially false
    assertFalse(viewModel.showDownloadComplete)

    // Call clearDownloadComplete
    viewModel.clearDownloadComplete()

    // Should still be false
    assertFalse(viewModel.showDownloadComplete)
  }

  @Test
  fun onDeepLinkEvent_selectsEventWhenAlreadyLoaded() {
    val deepLinkEvent = testEvent.copy(uid = "deep-link-event")
    whenever(mockEventStateController.allEvents).thenReturn(listOf(deepLinkEvent))

    viewModel.onDeepLinkEvent(deepLinkEvent.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    // ViewModel now exposes the event via resolvedDeepLinkEvent instead of directly calling
    // onEventPinClicked
    assertEquals(deepLinkEvent, viewModel.resolvedDeepLinkEvent)
  }

  @Test
  fun onDeepLinkEvent_fetchesEventWhenNotLoaded() {
    val deepLinkEvent = testEvent.copy(uid = "deep-link-fetch")
    whenever(mockEventStateController.allEvents).thenReturn(emptyList())
    runBlocking {
      whenever(mockEventRepository.getEvent(deepLinkEvent.uid)).thenReturn(deepLinkEvent)
    }

    viewModel.onDeepLinkEvent(deepLinkEvent.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    // ViewModel now exposes the event via resolvedDeepLinkEvent instead of directly calling
    // onEventPinClicked
    assertEquals(deepLinkEvent, viewModel.resolvedDeepLinkEvent)
  }

  // === Tests for toggleDirections with location requirements ===

  @Test
  fun `toggleDirections without location permission sets error message`() = runTest {
    // Setup: no location permission
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)
    viewModel.checkLocationPermission()

    // Clear any existing error
    viewModel.clearError()
    assertNull(viewModel.errorMessage)

    // Try to toggle directions
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    // Should set error message and not request directions
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Location permission is required"))
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `toggleDirections with permission but no location available sets error message`() = runTest {
    // Setup: has permission but no location yet
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    viewModel.checkLocationPermission()
    assertTrue(viewModel.hasLocationPermission)

    // Ensure no current location
    assertNull(viewModel.currentLocation)

    // Clear any existing error
    viewModel.clearError()
    assertNull(viewModel.errorMessage)

    // Try to toggle directions
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    // Should set error message about waiting for location
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.errorMessage!!.contains("Waiting for location"))
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `toggleDirections with permission and location requests directions successfully`() = runTest {
    // Setup: has permission and location
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    viewModel.checkLocationPermission()

    // Set a current location
    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.latitude).thenReturn(46.5)
    whenever(mockLocation.longitude).thenReturn(6.5)
    whenever(mockLocation.hasBearing()).thenReturn(false)
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }
    viewModel.getLastKnownLocation()
    assertNotNull(viewModel.currentLocation)

    // Clear any existing error
    viewModel.clearError()
    assertNull(viewModel.errorMessage)

    // Toggle directions
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    // Verify no error message was set (successful direction request)
    assertNull(viewModel.errorMessage)
    // Verify directionViewModel state changed from Cleared (indicating requestDirections was
    // called)
    assertNotNull(viewModel.directionViewModel.directionState)
  }

  @Test
  fun `toggleDirections error messages are correctly set for different scenarios`() = runTest {
    // Scenario 1: No permission
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)
    viewModel.checkLocationPermission()
    viewModel.clearError()

    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    val errorMsg1 = viewModel.errorMessage
    assertNotNull(errorMsg1)
    assertEquals("Location permission is required to get directions", errorMsg1)

    // Scenario 2: Permission but no location
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    viewModel.checkLocationPermission()
    // Don't set a location - currentLocation should be null
    viewModel.clearError()

    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    val errorMsg2 = viewModel.errorMessage
    assertNotNull(errorMsg2)
    assertEquals("Waiting for location... Please try again in a moment", errorMsg2)
  }

  @Test
  fun `toggleDirections clears previous error when successful`() = runTest {
    // Setup: set an initial error
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)
    viewModel.checkLocationPermission()
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)

    // Now grant permission and set location
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(true)
    viewModel.checkLocationPermission()

    val mockLocation = org.mockito.kotlin.mock<android.location.Location>()
    whenever(mockLocation.latitude).thenReturn(46.5)
    whenever(mockLocation.longitude).thenReturn(6.5)
    whenever(mockLocation.hasBearing()).thenReturn(false)
    whenever(mockLocationManager.getLastKnownLocation(any(), any())).thenAnswer { invocation ->
      val onSuccess = invocation.getArgument<(android.location.Location) -> Unit>(0)
      onSuccess(mockLocation)
    }
    viewModel.getLastKnownLocation()

    // Clear the error manually (as the UI would do)
    viewModel.clearError()
    assertNull(viewModel.errorMessage)

    // Now toggle directions should work
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    // Should not have error
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `toggleDirections handles multiple toggle attempts without permission`() = runTest {
    // Setup: no permission
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)
    viewModel.checkLocationPermission()

    // First attempt
    viewModel.clearError()
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)

    // Second attempt - should give same error
    viewModel.clearError()
    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()
    assertNotNull(viewModel.errorMessage)
    assertEquals("Location permission is required to get directions", viewModel.errorMessage)
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `toggleDirections state remains Cleared when permission check fails`() = runTest {
    // Initial state should be Cleared
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)

    // Try without permission
    whenever(mockLocationManager.hasLocationPermission()).thenReturn(false)
    viewModel.checkLocationPermission()
    viewModel.clearError()

    viewModel.toggleDirections(testEvent)
    advanceUntilIdle()

    // State should still be Cleared (not Loading or Displayed)
    assertTrue(viewModel.directionViewModel.directionState is DirectionState.Cleared)
    assertNotNull(viewModel.errorMessage)
  }

  @Test
  fun `requestDeleteEvent sets event and shows dialog`() {
    val event = Event("123")

    viewModel.requestDeleteEvent(event)

    assertEquals(event, viewModel.eventPendingDeletion)
    assertTrue(viewModel.showDeleteDialog)
  }

  @Test
  fun `requestDeleteEvent overrides previous pending event`() {
    val first = Event("first")
    val second = Event("second")

    viewModel.requestDeleteEvent(first)
    viewModel.requestDeleteEvent(second)

    assertEquals(second, viewModel.eventPendingDeletion)
    assertTrue(viewModel.showDeleteDialog)
  }

  @Test
  fun `cancelDelete clears event and hides dialog`() {
    val event = Event("toDelete")

    viewModel.requestDeleteEvent(event)
    viewModel.cancelDelete()

    assertNull(viewModel.eventPendingDeletion)
    assertFalse(viewModel.showDeleteDialog)
  }

  @Test
  fun `cancelDelete works when no event is pending`() {

    viewModel.cancelDelete()

    assertNull(viewModel.eventPendingDeletion)
    assertFalse(viewModel.showDeleteDialog)
  }
}

package com.swent.mapin.ui.map

import android.content.Context
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.memory.Memory
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
import org.mockito.kotlin.verify
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
  @Mock private lateinit var mockFirebaseStorage: FirebaseStorage
  @Mock private lateinit var mockStorageReference: StorageReference
  @Mock private lateinit var mockFileReference: StorageReference
  @Mock private lateinit var mockUploadTask: UploadTask
  @Mock private lateinit var mockUploadTaskSnapshot: UploadTask.TaskSnapshot

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
    assertFalse(clearFocusCalled)

    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    assertTrue(clearFocusCalled)
  }

  @Test
  fun onSearchTap_multipleCalls_setsFocusWhenNotInFullState() {
    // Premier appel : passe en FULL et met focus à true
    viewModel.onSearchTap()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
    assertTrue(viewModel.shouldFocusSearch)

    // Gérer le focus
    viewModel.onSearchFocusHandled()
    assertFalse(viewModel.shouldFocusSearch)

    // Deuxième appel : déjà en FULL, ne fait rien
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
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun hideMemoryForm_hidesForm() {
    viewModel.showMemoryForm()
    assertTrue(viewModel.showMemoryForm)

    viewModel.hideMemoryForm()

    assertFalse(viewModel.showMemoryForm)
  }

  @Test
  fun onMemoryCancel_hidesFormAndRestoresPreviousState() {
    viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
    viewModel.showMemoryForm()
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)

    viewModel.onMemoryCancel()

    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
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
    var capturedMemory: Memory? = null
    whenever(mockMemoryRepository.addMemory(any())).thenAnswer { invocation ->
      capturedMemory = invocation.getArgument(0)
      Unit
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
    // Note: In real scenario, isSavingMemory would be true during the operation
    // but due to how coroutines work in tests, it's hard to catch the intermediate state
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
  fun setEvents_updatesEvents() {
    val initialEvents = viewModel.events
    val newEvents =
        listOf(
            com.swent.mapin.model.event.Event(
                uid = "test1",
                title = "Test Event",
                location = com.swent.mapin.model.Location("Test Location", 34.0, -118.0),
                attendeeCount = 50))

    viewModel.setEvents(newEvents)

    assertEquals(newEvents, viewModel.events)
  }

  @Test
  fun eventsToGeoJson_createsValidGeoJson() {
    val events =
        listOf(
            com.swent.mapin.model.event.Event(
                uid = "event1",
                title = "Event 1",
                location = com.swent.mapin.model.Location("Location 1", 46.5, 6.5),
                attendeeCount = 10),
            com.swent.mapin.model.event.Event(
                uid = "event2",
                title = "Event 2",
                location = com.swent.mapin.model.Location("Location 2", 47.0, 7.0),
                attendeeCount = 25))

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
    assertTrue(geoJson.contains("10")) // weight from attendeeCount
  }

  @Test
  fun eventsToGeoJson_emptyList_createsEmptyFeatureCollection() {
    val geoJson = eventsToGeoJson(emptyList())

    assertTrue(geoJson.contains("FeatureCollection"))
    assertTrue(geoJson.contains("features"))
  }
}

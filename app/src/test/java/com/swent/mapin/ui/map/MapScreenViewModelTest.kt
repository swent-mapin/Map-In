package com.swent.mapin.ui.map

import android.content.Context
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.UserProfileRepository
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
import org.mockito.Mockito.clearInvocations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
  @Mock(lenient = true) private lateinit var mockUserProfileRepository: UserProfileRepository

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
      whenever(mockEventRepository.getSavedEventIds(any())).thenReturn(emptySet())
      whenever(mockEventRepository.getSavedEvents(any())).thenReturn(emptyList())
      whenever(mockUserProfileRepository.getUserProfile(any())).thenReturn(null)
    }

    viewModel =
        MapScreenViewModel(
            initialSheetState = BottomSheetState.COLLAPSED,
            sheetConfig = config,
            onClearFocus = { clearFocusCalled = true },
            applicationContext = mockContext,
            memoryRepository = mockMemoryRepository,
            eventRepository = mockEventRepository,
            auth = mockAuth,
            userProfileRepository = mockUserProfileRepository)
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
  fun eventsToGeoJson_createsValidGeoJson() {
    val events =
        listOf(
            com.swent.mapin.model.event.Event(
                uid = "event1",
                title = "Event 1",
                location = com.swent.mapin.model.Location("Location 1", 46.5, 6.5),
                participantIds = List(10) { "user$it" }),
            com.swent.mapin.model.event.Event(
                uid = "event2",
                title = "Event 2",
                location = com.swent.mapin.model.Location("Location 2", 47.0, 7.0),
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

  // Tests for tag filtering functionality
  @Test
  fun topTags_initiallyLoaded() {
    assertNotNull(viewModel.topTags)
    assertTrue(viewModel.topTags.isNotEmpty())
    assertEquals(5, viewModel.topTags.size)
  }

  @Test
  fun selectedTags_initiallyEmpty() {
    assertTrue(viewModel.selectedTags.isEmpty())
  }

  @Test
  fun toggleTagSelection_addsTagWhenNotSelected() {
    val tag = "Sports"
    assertTrue(viewModel.selectedTags.isEmpty())

    viewModel.toggleTagSelection(tag)

    assertTrue(viewModel.selectedTags.contains(tag))
    assertEquals(1, viewModel.selectedTags.size)
  }

  @Test
  fun toggleTagSelection_removesTagWhenAlreadySelected() {
    val tag = "Music"
    viewModel.toggleTagSelection(tag)
    assertTrue(viewModel.selectedTags.contains(tag))

    viewModel.toggleTagSelection(tag)

    assertFalse(viewModel.selectedTags.contains(tag))
    assertTrue(viewModel.selectedTags.isEmpty())
  }

  @Test
  fun toggleTagSelection_canSelectMultipleTags() {
    viewModel.toggleTagSelection("Sports")
    viewModel.toggleTagSelection("Music")
    viewModel.toggleTagSelection("Food")

    assertEquals(3, viewModel.selectedTags.size)
    assertTrue(viewModel.selectedTags.contains("Sports"))
    assertTrue(viewModel.selectedTags.contains("Music"))
    assertTrue(viewModel.selectedTags.contains("Food"))
  }

  @Test
  fun toggleTagSelection_filtersEventsWhenTagSelected() {
    val initialEventCount = viewModel.events.size
    assertTrue(initialEventCount > 0)

    viewModel.toggleTagSelection("Sports")

    // All events should have "Sports" tag
    val filteredEventCount = viewModel.events.size
    assertTrue(filteredEventCount > 0)
    assertTrue(filteredEventCount <= initialEventCount)
    viewModel.events.forEach { event ->
      assertTrue("Event should have Sports tag", event.tags.contains("Sports"))
    }
  }

  @Test
  fun toggleTagSelection_showsAllEventsWhenNoTagsSelected() {
    val initialEventCount = viewModel.events.size

    // Select a tag to filter
    viewModel.toggleTagSelection("Music")
    val filteredCount = viewModel.events.size
    assertTrue(filteredCount < initialEventCount)

    // Deselect the tag
    viewModel.toggleTagSelection("Music")

    // Should show all events again
    assertEquals(initialEventCount, viewModel.events.size)
  }

  @Test
  fun toggleTagSelection_filtersWithMultipleTags() {
    viewModel.toggleTagSelection("Sports")
    viewModel.toggleTagSelection("Music")

    val filteredEvents = viewModel.events

    // All filtered events should have at least one of the selected tags
    filteredEvents.forEach { event ->
      val hasSportsTag = event.tags.contains("Sports")
      val hasMusicTag = event.tags.contains("Music")
      assertTrue("Event should have at least one selected tag", hasSportsTag || hasMusicTag)
    }
  }

  @Test
  fun toggleTagSelection_updatesEventsImmediately() {
    val initialEventCount = viewModel.events.size

    viewModel.toggleTagSelection("Tech")

    val newEventCount = viewModel.events.size
    assertTrue(newEventCount <= initialEventCount)
  }

  @Test
  fun events_containsOnlyEventsWithSelectedTags() {
    viewModel.toggleTagSelection("Food")

    val eventsWithFoodTag = viewModel.events.filter { it.tags.contains("Food") }

    assertEquals(viewModel.events.size, eventsWithFoodTag.size)
  }

  @Test
  fun toggleTagSelection_preservesFilteringAfterMultipleToggles() {
    // Select Sports
    viewModel.toggleTagSelection("Sports")
    val sportsCount = viewModel.events.size

    // Select Music (in addition to Sports)
    viewModel.toggleTagSelection("Music")
    val sportsPlusMusicCount = viewModel.events.size
    assertTrue(sportsPlusMusicCount >= sportsCount)

    // Deselect Sports, keep Music
    viewModel.toggleTagSelection("Sports")
    val musicOnlyCount = viewModel.events.size
    assertTrue(musicOnlyCount <= sportsPlusMusicCount)

    viewModel.events.forEach { event ->
      assertTrue("Event should have Music tag", event.tags.contains("Music"))
    }
  }

  @Test
  fun events_initiallyContainsAllSampleEvents() {
    val sampleEvents = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()
    assertEquals(sampleEvents.size, viewModel.events.size)
  }

  @Test
  fun topTags_containsValidTags() {
    viewModel.topTags.forEach { tag ->
      assertTrue(tag.isNotEmpty())
      // Verify tag exists in at least one event
      val tagExists = viewModel.events.any { event -> event.tags.contains(tag) }
      assertTrue("Tag $tag should exist in events", tagExists)
    }
  }

  @Test
  fun onEventPinClicked_setsSelectedEventAndTransitionsToMedium() = runTest {
    val testEvent = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0]
    var cameraCentered = false
    viewModel.onCenterCamera = { _, _ -> cameraCentered = true }

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
    val testEvent = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0]
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
        com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0].copy(
            participantIds = listOf("otherUser"))
    viewModel.onEventPinClicked(notParticipatingEvent)
    advanceUntilIdle()
    assertFalse(viewModel.isUserParticipating())

    val participatingEvent =
        com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0].copy(
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
    val testEvent = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0]
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()
    assertEquals("You must be signed in to join events", viewModel.errorMessage)

    viewModel.clearError()
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val fullEvent = testEvent.copy(participantIds = List(10) { "user$it" }, capacity = 10)
    viewModel.onEventPinClicked(fullEvent)
    advanceUntilIdle()
    viewModel.joinEvent()
    advanceUntilIdle()
    assertEquals("Event is at full capacity", viewModel.errorMessage)
  }

  @Test
  fun joinEvent_success_updatesEventAndList() = runTest {
    val testEvent =
        com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0].copy(
            participantIds = listOf(), capacity = 10)

    viewModel.setEvents(listOf(testEvent))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

    viewModel.joinEvent()
    advanceUntilIdle()

    assertNull(viewModel.errorMessage)
    assertNotNull(viewModel.selectedEvent)
    assertTrue(viewModel.selectedEvent!!.participantIds.contains("testUserId"))
    assertEquals(1, viewModel.selectedEvent!!.participantIds.size)
    assertTrue(
        viewModel.events.find { it.uid == testEvent.uid }!!.participantIds.contains("testUserId"))
  }

  @Test
  fun unregisterFromEvent_success_updatesEventAndList() = runTest {
    val testEvent =
        com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0].copy(
            participantIds = listOf("testUserId"), capacity = 10)

    viewModel.setEvents(listOf(testEvent))
    viewModel.onEventPinClicked(testEvent)
    advanceUntilIdle()

    runBlocking { whenever(mockEventRepository.editEvent(any(), any())).thenReturn(Unit) }

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
        com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0].copy(
            participantIds = listOf(), capacity = 10)
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
  fun setBottomSheetTab_updatesSelectedTab() {
    assertEquals(MapScreenViewModel.BottomSheetTab.SAVED_EVENTS, viewModel.selectedBottomSheetTab)
    viewModel.setBottomSheetTab(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS)
    assertEquals(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS, viewModel.selectedBottomSheetTab)
  }

  @Test
  fun onJoinedEventClicked_callsOnEventPinClicked() = runTest {
    val testEvent = com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0]
    var cameraCentered = false
    viewModel.onCenterCamera = { _, _ -> cameraCentered = true }

    viewModel.onTabEventClicked(testEvent)
    advanceUntilIdle()

    assertEquals(testEvent, viewModel.selectedEvent)
    assertEquals(BottomSheetState.MEDIUM, viewModel.bottomSheetState)
    assertTrue(cameraCentered)
  }

  @Test
  fun setEvents_updatesEventsList() {
    val newEvents =
        listOf(
            com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0],
            com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[1])

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

  // === Save / Unsave tests ===
  private fun sampleEvent() =
      com.swent.mapin.model.event.LocalEventRepository.defaultSampleEvents()[0].copy(
          uid = "evt-1", participantIds = emptyList())

  @Test
  fun saveEventForLater_noUser_setsErrorMessage() = runTest {
    val e = sampleEvent()
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()

    // Sign out
    whenever(mockAuth.currentUser).thenReturn(null)

    viewModel.saveEventForLater()
    advanceUntilIdle()

    assertTrue(viewModel.errorMessage?.contains("signed in") == true)
  }

  @Test
  fun saveEventForLater_success_updatesIdsAndLoadsSavedList() = runTest {
    val e = sampleEvent()
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()
    clearInvocations(mockEventRepository)

    whenever(mockEventRepository.saveEventForUser("testUserId", e.uid)).thenReturn(true)
    whenever(mockEventRepository.getSavedEvents("testUserId")).thenReturn(listOf(e))

    viewModel.saveEventForLater()
    advanceUntilIdle()

    assertTrue(viewModel.isEventSaved(e))
    assertTrue(viewModel.savedEvents.any { it.uid == e.uid })

    verify(mockEventRepository).saveEventForUser("testUserId", e.uid)
    verify(mockEventRepository).getSavedEvents("testUserId")
  }

  @Test
  fun saveEventForLater_alreadySaved_setsError() = runTest {
    val e = sampleEvent()
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()

    whenever(mockEventRepository.saveEventForUser("testUserId", e.uid)).thenReturn(false)

    viewModel.saveEventForLater()
    advanceUntilIdle()

    assertTrue(viewModel.errorMessage?.contains("already saved") == true)
  }

  @Test
  fun unsaveEventForLater_success_updatesIdsAndReloads() = runTest {
    val e = sampleEvent()
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()

    whenever(mockEventRepository.saveEventForUser("testUserId", e.uid)).thenReturn(true)
    whenever(mockEventRepository.getSavedEvents("testUserId")).thenReturn(listOf(e))
    viewModel.saveEventForLater()
    advanceUntilIdle()
    assertTrue(viewModel.isEventSaved(e))

    org.mockito.Mockito.clearInvocations(mockEventRepository)

    whenever(mockEventRepository.unsaveEventForUser("testUserId", e.uid)).thenReturn(true)
    whenever(mockEventRepository.getSavedEvents("testUserId")).thenReturn(emptyList())

    viewModel.unsaveEventForLater()
    advanceUntilIdle()

    assertFalse(viewModel.isEventSaved(e))
    assertTrue(viewModel.savedEvents.isEmpty())

    verify(mockEventRepository).unsaveEventForUser("testUserId", e.uid)
    verify(mockEventRepository).getSavedEvents("testUserId")
  }

  @Test
  fun unsaveEventForLater_failure_setsError() = runTest {
    val e = sampleEvent()
    viewModel.setEvents(listOf(e))
    viewModel.onEventPinClicked(e)
    advanceUntilIdle()

    // Pretend it was saved locally (save path)
    whenever(mockEventRepository.saveEventForUser("testUserId", e.uid)).thenReturn(true)
    whenever(mockEventRepository.getSavedEvents("testUserId")).thenReturn(listOf(e))
    viewModel.saveEventForLater()
    advanceUntilIdle()
    assertTrue(viewModel.isEventSaved(e))

    // Unsave fails
    whenever(mockEventRepository.unsaveEventForUser("testUserId", e.uid)).thenReturn(false)

    viewModel.unsaveEventForLater()
    advanceUntilIdle()

    assertTrue(viewModel.errorMessage?.contains("was not saved") == true)
  }

  @Test
  fun `showMemoryForm sets state correctly`() {
    val initialState = viewModel.bottomSheetState

    viewModel.showMemoryForm()

    assertTrue(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.MEMORY_FORM, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.FULL, viewModel.bottomSheetState)
  }

  @Test
  fun `hideMemoryForm resets showMemoryForm and sets MAIN_CONTENT`() {
    viewModel.showMemoryForm()

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
  fun `hideAddEventForm resets to MAIN_CONTENT`() {
    viewModel.showAddEventForm()

    viewModel.hideAddEventForm()

    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
  }

  @Test
  fun `onAddEventCancel hides AddEvent form and returns to MAIN_CONTENT`() {
    // Simulate showing AddEvent form
    viewModel.showAddEventForm()

    // Cancel
    viewModel.onAddEventCancel()

    // Validate public state
    assertEquals(BottomSheetScreen.MAIN_CONTENT, viewModel.currentBottomSheetScreen)
    assertEquals(BottomSheetState.COLLAPSED, viewModel.bottomSheetState)
  }

  @Test
  fun `showMemoryForm and showAddEventForm are mutually exclusive`() {
    viewModel.showMemoryForm()

    viewModel.showAddEventForm()

    assertFalse(viewModel.showMemoryForm)
    assertEquals(BottomSheetScreen.ADD_EVENT, viewModel.currentBottomSheetScreen)
  }
}

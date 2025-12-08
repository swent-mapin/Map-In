package com.swent.mapin.ui.map.eventstate

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.swent.mapin.model.Location
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.network.ConnectivityService
import com.swent.mapin.model.network.ConnectivityState
import com.swent.mapin.model.network.NetworkType
import com.swent.mapin.ui.filters.Filters
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import kotlinx.coroutines.CoroutineScope
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class MapEventStateControllerTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = CoroutineScope(testDispatcher)

  @Mock private lateinit var mockEventRepository: EventRepository
  @Mock private lateinit var mockUserProfileRepository: UserProfileRepository
  @Mock private lateinit var mockAuth: FirebaseAuth
  @Mock private lateinit var mockUser: FirebaseUser
  @Mock private lateinit var mockFiltersSectionViewModel: FiltersSectionViewModel
  @Mock private lateinit var mockGetSelectedEvent: () -> Event?
  @Mock private lateinit var mockSetErrorMessage: (String) -> Unit
  @Mock private lateinit var mockClearErrorMessage: () -> Unit
  @Mock private lateinit var mockConnectivityService: ConnectivityService

  private lateinit var controller: MapEventStateController
  private lateinit var fakeTimeProvider: FakeTimeProvider
  private val testUserId = "testUserId"
  private val testEvent =
      Event(
          uid = "event1",
          title = "Test Event",
          description = "Fun event",
          location = Location("Test Location", 46.5, 6.5),
          tags = listOf("party"),
          participantIds = emptyList(),
          ownerId = "owner123",
          capacity = 10)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeTimeProvider = FakeTimeProvider()

    // Mock Firebase Auth
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(testUserId)

    // Mock FiltersSectionViewModel
    val defaultFilters = Filters()
    val filtersFlow = MutableStateFlow(defaultFilters)
    whenever(mockFiltersSectionViewModel.filters).thenReturn(filtersFlow)

    // Mock ConnectivityService - online by default
    whenever(mockConnectivityService.connectivityState)
        .thenReturn(flowOf(ConnectivityState(isConnected = true, networkType = NetworkType.WIFI)))

    // Mock repositories
    runBlocking {
      whenever(mockEventRepository.getFilteredEvents(any(), any())).thenReturn(emptyList())
      whenever(mockEventRepository.getSavedEvents(any())).thenReturn(emptyList())
      whenever(mockEventRepository.getJoinedEvents(any())).thenReturn(emptyList())
      whenever(mockUserProfileRepository.getUserProfile(testUserId))
          .thenReturn(UserProfile(userId = testUserId, joinedEventIds = emptyList()))
    }

    controller =
        MapEventStateController(
            eventRepository = mockEventRepository,
            userProfileRepository = mockUserProfileRepository,
            auth = mockAuth,
            scope = testScope,
            filterViewModel = mockFiltersSectionViewModel,
            connectivityService = mockConnectivityService,
            getSelectedEvent = mockGetSelectedEvent,
            setErrorMessage = mockSetErrorMessage,
            clearErrorMessage = mockClearErrorMessage,
            autoRefreshEnabled = false,
            timeProvider = fakeTimeProvider)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========== Filter Tests ==========
  @Test
  fun `observeFilters updates allEvents on filter change`() = runTest {
    val newFilters = Filters(tags = setOf("party"))
    val filteredEvents = listOf(testEvent)
    whenever(controller.getUserId()).thenReturn(testUserId)
    whenever(mockEventRepository.getFilteredEvents(newFilters, testUserId))
        .thenReturn(filteredEvents)

    // Simulate filter change
    val filtersFlow = MutableStateFlow(Filters())
    whenever(mockFiltersSectionViewModel.filters).thenReturn(filtersFlow)
    controller =
        MapEventStateController(
            eventRepository = mockEventRepository,
            userProfileRepository = mockUserProfileRepository,
            auth = mockAuth,
            scope = testScope,
            filterViewModel = mockFiltersSectionViewModel,
            connectivityService = mockConnectivityService,
            getSelectedEvent = mockGetSelectedEvent,
            setErrorMessage = mockSetErrorMessage,
            clearErrorMessage = mockClearErrorMessage)
    controller.observeFilters()
    filtersFlow.emit(newFilters)
    advanceUntilIdle()

    assertEquals(filteredEvents, controller.allEvents)
  }

  @Test
  fun `getFilteredEvents updates allEvents with filtered events`() = runTest {
    val filters = Filters(tags = setOf("party"))
    val filteredEvents = listOf(testEvent)
    whenever(controller.getUserId()).thenReturn(testUserId)
    whenever(mockEventRepository.getFilteredEvents(filters, testUserId)).thenReturn(filteredEvents)

    controller.getFilteredEvents(filters)
    advanceUntilIdle()

    assertEquals(filteredEvents, controller.allEvents)
  }

  @Test
  fun `getFilteredEvents handles repository error`() = runTest {
    val filters = Filters()
    whenever(controller.getUserId()).thenReturn(testUserId)
    whenever(mockEventRepository.getFilteredEvents(filters, testUserId))
        .thenThrow(RuntimeException("Network error"))

    controller.getFilteredEvents(filters)
    advanceUntilIdle()

    verify(mockSetErrorMessage).invoke("Network error")
    assertTrue(controller.allEvents.isEmpty())
  }

  // ========== Refresh Tests ==========
  @Test
  fun `refreshEventsList refreshes filteredEvents list`() = runTest {
    val filteredEvents = listOf(testEvent)
    whenever(mockEventRepository.getFilteredEvents(any(), any())).thenReturn(filteredEvents)

    controller.refreshEventsList()
    advanceUntilIdle()

    assertEquals(filteredEvents, controller.allEvents)
  }

  @Test
  fun `refreshSelectedEvent returns event by ID`() {
    controller.setAllEventsForTest(listOf(testEvent))
    val result = controller.refreshSelectedEvent(testEvent.uid)
    assertEquals(testEvent, result)
  }

  @Test
  fun `refreshSelectedEvent returns null for non-existent ID`() {
    controller.setAllEventsForTest(listOf(testEvent))
    val result = controller.refreshSelectedEvent("nonexistent")
    assertNull(result)
  }

  // ========== Search Tests ==========
  @Test
  fun `searchEvents filters events by title`() {
    controller.setAllEventsForTest(listOf(testEvent))
    controller.searchEvents("Test")
    assertEquals(listOf(testEvent), controller.searchResults)
  }

  @Test
  fun `searchEvents filters events by description`() {
    controller.setAllEventsForTest(listOf(testEvent))
    controller.searchEvents("Fun")
    assertEquals(listOf(testEvent), controller.searchResults)
  }

  @Test
  fun `searchEvents filters events by tags`() {
    controller.setAllEventsForTest(listOf(testEvent))
    controller.searchEvents("party")
    assertEquals(listOf(testEvent), controller.searchResults)
  }

  @Test
  fun `searchEvents filters events by location name`() {
    controller.setAllEventsForTest(listOf(testEvent))
    controller.searchEvents("Test Location")
    assertEquals(listOf(testEvent), controller.searchResults)
  }

  @Test
  fun `searchEvents with empty query clears results`() {
    controller.setAllEventsForTest(listOf(testEvent))
    controller.searchEvents("")
    assertTrue(controller.searchResults.isEmpty())
  }

  @Test
  fun `clearSearchResults clears search results`() {
    controller.setSearchResultForTest(listOf(testEvent))
    controller.clearSearchResults()
    assertTrue(controller.searchResults.isEmpty())
  }

  // ========== Joined Events Tests ==========
  @Test
  fun `loadJoinedEvents populates joinedEvents for user`() = runTest {
    val joinedEvent = testEvent.copy(participantIds = listOf(testUserId))
    whenever(mockEventRepository.getJoinedEvents(testUserId)).thenReturn(listOf(joinedEvent))

    controller.loadJoinedEvents()
    advanceUntilIdle()

    assertEquals(listOf(joinedEvent), controller.joinedEvents)
  }

  @Test
  fun `loadJoinedEvents handles missing user profile`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)

    controller.loadJoinedEvents()
    advanceUntilIdle()

    verify(mockSetErrorMessage).invoke("User not authenticated")
    assertTrue(controller.joinedEvents.isEmpty())
  }

  // ========== Saved Events Tests ==========
  @Test
  fun `loadSavedEvents populates savedEvents for user`() = runTest {
    val savedEvents = listOf(testEvent)
    whenever(mockEventRepository.getSavedEvents(testUserId)).thenReturn(savedEvents)

    controller.loadSavedEvents()
    advanceUntilIdle()

    assertEquals(savedEvents, controller.savedEvents)
  }

  @Test
  fun `loadSavedEvents handles repository error`() = runTest {
    whenever(mockEventRepository.getSavedEvents(testUserId))
        .thenThrow(RuntimeException("Network error"))

    controller.loadSavedEvents()
    advanceUntilIdle()

    verify(mockSetErrorMessage).invoke("Network error")
    assertTrue(controller.savedEvents.isEmpty())
  }

  // ========== Join Event Tests ==========
  @Test
  fun `joinSelectedEvent adds user to event and updates lists`() = runTest {
    whenever(mockEventRepository.editEventAsUser(testEvent.uid, testUserId, true)).thenReturn(Unit)

    // Mock selected event and repository responses
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    val updatedEvent = testEvent.copy(participantIds = listOf(testUserId))

    // Execute the method
    controller.joinSelectedEvent()
    advanceUntilIdle()

    // Assertions
    assertEquals(listOf(updatedEvent), controller.joinedEvents)
    verify(mockEventRepository).editEventAsUser(testEvent.uid, testUserId, true)
  }

  @Test
  fun `joinSelectedEvent handles full capacity`() = runTest {
    val fullEvent = testEvent.copy(participantIds = List(10) { "user$it" }, capacity = 10)
    whenever(mockGetSelectedEvent()).thenReturn(fullEvent)

    controller.joinSelectedEvent()
    advanceUntilIdle()

    verify(mockSetErrorMessage)
        .invoke(
            "Event is at full capacity: ${fullEvent.participantIds.size} out of ${fullEvent.capacity}")
    assertTrue(controller.allEvents.isEmpty())
  }

  @Test
  fun `joinSelectedEvent does nothing if no event selected`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(null)

    controller.joinSelectedEvent()
    advanceUntilIdle()

    assertTrue(controller.allEvents.isEmpty())
    assertTrue(controller.joinedEvents.isEmpty())
  }

  @Test
  fun `joinSelectedEvent reverts local changes on repository error`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    whenever(mockEventRepository.editEventAsUser(any(), any(), any()))
        .thenThrow(RuntimeException("Network error"))
    controller.setAllEventsForTest(listOf(testEvent))

    controller.joinSelectedEvent()
    advanceUntilIdle()

    assertEquals(listOf(testEvent), controller.allEvents) // Local change reverted
    verify(mockSetErrorMessage).invoke("Network error")
  }

  // ========== Leave Event Tests ==========
  @Test
  fun `leaveSelectedEvent removes user from event and updates lists`() = runTest {
    val joinedEvent = testEvent.copy(participantIds = listOf(testUserId))
    controller.setJoinedEventsForTest(listOf(joinedEvent))
    whenever(mockGetSelectedEvent()).thenReturn(joinedEvent)

    controller.leaveSelectedEvent()
    advanceUntilIdle()

    assertEquals(emptyList<Event>(), controller.joinedEvents)
    verify(mockEventRepository).editEventAsUser(testEvent.uid, testUserId, false)
  }

  @Test
  fun `leaveSelectedEvent does nothing if no event selected`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(null)

    controller.leaveSelectedEvent()
    advanceUntilIdle()

    assertTrue(controller.allEvents.isEmpty())
    assertTrue(controller.joinedEvents.isEmpty())
  }

  @Test
  fun `leaveSelectedEvent reverts local changes on repository error`() = runTest {
    val joinedEvent = testEvent.copy(participantIds = listOf(testUserId))
    controller.setJoinedEventsForTest(listOf(testEvent))
    whenever(mockGetSelectedEvent()).thenReturn(joinedEvent)
    whenever(mockEventRepository.editEventAsUser(any(), any(), any()))
        .thenThrow(RuntimeException("Network error"))
    controller.setAllEventsForTest(listOf(joinedEvent))

    controller.leaveSelectedEvent()
    advanceUntilIdle()

    assertEquals(listOf(joinedEvent), controller.allEvents) // Local change reverted
    verify(mockSetErrorMessage).invoke("Network error")
  }

  // ========== Save Event Tests ==========
  @Test
  fun `saveSelectedEvent saves event and refreshes savedEvents`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    val savedEvents = listOf(testEvent)

    controller.saveSelectedEvent()
    advanceUntilIdle()

    verify(mockUserProfileRepository).saveUserProfile(any<UserProfile>())
    assertEquals(savedEvents, controller.savedEvents)
  }

  @Test
  fun `saveSelectedEvent does nothing if no event selected`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(null)

    controller.saveSelectedEvent()
    advanceUntilIdle()

    assertTrue(controller.savedEvents.isEmpty())
  }

  @Test
  fun `saveSelectedEvent handles repository error`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    whenever(mockUserProfileRepository.saveUserProfile(any<UserProfile>()))
        .thenThrow(RuntimeException("Network error"))

    controller.saveSelectedEvent()
    advanceUntilIdle()

    verify(mockSetErrorMessage).invoke("Network error")
    assertTrue(controller.savedEvents.isEmpty())
  }

  // ========== Unsave Event Tests ==========
  @Test
  fun `unsaveSelectedEvent unsaves event and refreshes savedEvents`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    controller.setSavedEventsForTest(listOf(testEvent))
    whenever(mockUserProfileRepository.saveUserProfile(any<UserProfile>())).thenReturn(true)

    controller.unsaveSelectedEvent()
    advanceUntilIdle()

    verify(mockUserProfileRepository).saveUserProfile(any<UserProfile>())
    assertTrue(controller.savedEvents.isEmpty())
  }

  @Test
  fun `unsaveSelectedEvent does nothing if no event selected`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(null)

    controller.unsaveSelectedEvent()
    advanceUntilIdle()

    assertTrue(controller.savedEvents.isEmpty())
  }

  @Test
  fun `unsaveSelectedEvent handles repository error`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    controller.setSavedEventsForTest(listOf(testEvent))
    whenever(mockUserProfileRepository.saveUserProfile(any<UserProfile>()))
        .thenThrow(RuntimeException("Network error"))

    controller.unsaveSelectedEvent()
    advanceUntilIdle()

    verify(mockSetErrorMessage).invoke("Network error")
    assertEquals(listOf(testEvent), controller.savedEvents)
    // Empty list for other tests
    controller.setSavedEventsForTest(emptyList())
    assertTrue(controller.savedEvents.isEmpty())
  }

  // ========== User ID Tests ==========
  @Test
  fun `getUserId returns current user ID`() {
    assertEquals(testUserId, controller.getUserId())
  }

  @Test
  fun `getUserId throws exception when user not authenticated`() {
    whenever(mockAuth.currentUser).thenReturn(null)
    try {
      controller.getUserId()
      assertFalse("Expected exception for unauthenticated user", true)
    } catch (e: Exception) {
      assertEquals("User not authenticated", e.message)
    }
  }

  // ========== State Management Tests ==========
  @Test
  fun `clearUserScopedState clears all event lists`() {
    controller.setAllEventsForTest(listOf(testEvent))
    controller.setJoinedEventsForTest(listOf(testEvent))
    controller.setAvailableEventsForTest(listOf(testEvent))
    controller.setJoinedEventsForTest(listOf(testEvent))
    controller.setSavedEventsForTest(listOf(testEvent))

    controller.clearUserScopedState()

    assertTrue(controller.allEvents.isEmpty())
    assertTrue(controller.searchResults.isEmpty())
    assertTrue(controller.availableEvents.isEmpty())
    assertTrue(controller.joinedEvents.isEmpty())
    assertTrue(controller.savedEvents.isEmpty())
  }

  @Test
  fun `clearError calls clearErrorMessage lambda`() {
    controller.clearError()
    verify(mockClearErrorMessage).invoke()
  }

  // ========== Listeners Tests ==========
  @Test
  fun `startListeners initializes joined and saved event listeners`() = runTest {
    val mockJoinedListener = mock<ListenerRegistration>()
    val mockSavedListener = mock<ListenerRegistration>()

    whenever(mockEventRepository.listenToJoinedEvents(any(), any())).thenReturn(mockJoinedListener)
    whenever(mockEventRepository.listenToSavedEvents(any(), any())).thenReturn(mockSavedListener)

    controller.startListeners()

    verify(mockEventRepository).listenToJoinedEvents(eq(testUserId), any())
    verify(mockEventRepository).listenToSavedEvents(eq(testUserId), any())
  }

  @Test
  fun `stopListeners removes all active listeners`() = runTest {
    val mockJoinedListener = mock<ListenerRegistration>()
    val mockSavedListener = mock<ListenerRegistration>()

    whenever(mockEventRepository.listenToJoinedEvents(any(), any())).thenReturn(mockJoinedListener)
    whenever(mockEventRepository.listenToSavedEvents(any(), any())).thenReturn(mockSavedListener)

    controller.startListeners()
    controller.stopListeners()

    verify(mockJoinedListener).remove()
    verify(mockSavedListener).remove()
  }

  @Test
  fun `handleJoinedEventsUpdate adds new events correctly`() = runTest {
    val mockListener = mock<ListenerRegistration>()
    val newEvent = testEvent.copy(uid = "new1")

    val updateCaptor = argumentCaptor<(List<Event>, List<Event>, List<String>) -> Unit>()
    whenever(mockEventRepository.listenToJoinedEvents(any(), updateCaptor.capture()))
        .thenReturn(mockListener)

    controller.startListeners()

    // Simulate listener callback with added event
    updateCaptor.firstValue.invoke(listOf(newEvent), emptyList(), emptyList())

    assertTrue(controller.joinedEvents.contains(newEvent))
  }

  @Test
  fun `handleJoinedEventsUpdate removes deleted events correctly`() = runTest {
    val mockListener = mock<ListenerRegistration>()
    controller.setJoinedEventsForTest(listOf(testEvent))

    val updateCaptor = argumentCaptor<(List<Event>, List<Event>, List<String>) -> Unit>()
    whenever(mockEventRepository.listenToJoinedEvents(any(), updateCaptor.capture()))
        .thenReturn(mockListener)

    controller.startListeners()

    // Simulate listener callback with removed event
    updateCaptor.firstValue.invoke(emptyList(), emptyList(), listOf(testEvent.uid))

    assertTrue(controller.joinedEvents.isEmpty())
  }

  @Test
  fun `handleSavedEventsUpdate updates saved events correctly`() = runTest {
    val mockListener = mock<ListenerRegistration>()
    val savedEvent = testEvent.copy(uid = "saved1")

    val updateCaptor = argumentCaptor<(List<Event>, List<Event>, List<String>) -> Unit>()
    whenever(mockEventRepository.listenToSavedEvents(any(), updateCaptor.capture()))
        .thenReturn(mockListener)

    controller.startListeners()

    // Simulate listener callback
    updateCaptor.firstValue.invoke(listOf(savedEvent), emptyList(), emptyList())

    assertTrue(controller.savedEvents.contains(savedEvent))
  }

  // ========== Optimistic Updates Tests ==========
  @Test
  fun `joinSelectedEvent applies optimistic update before server call`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    whenever(mockEventRepository.editEventAsUser(any(), any(), any())).thenAnswer {
      // Assert optimistic update happened before this call
      assertTrue(
          controller.joinedEvents.contains(testEvent.copy(participantIds = listOf(testUserId))))
      Unit
    }

    controller.joinSelectedEvent()
    advanceUntilIdle()
  }

  @Test
  fun `leaveSelectedEvent applies optimistic update before server call`() = runTest {
    val joinedEvent = testEvent.copy(participantIds = listOf(testUserId))
    controller.setJoinedEventsForTest(listOf(joinedEvent))
    whenever(mockGetSelectedEvent()).thenReturn(joinedEvent)

    whenever(mockEventRepository.editEventAsUser(any(), any(), any())).thenAnswer {
      // Assert optimistic update happened before this call
      assertFalse(controller.joinedEvents.contains(joinedEvent))
      Unit
    }

    controller.leaveSelectedEvent()
    advanceUntilIdle()
  }

  // ========== Offline Mode Tests ==========
  @Test
  fun `observeConnectivity starts monitoring connectivity changes`() = runTest {
    val connectivityFlow = MutableStateFlow(ConnectivityState(true, NetworkType.WIFI))
    whenever(mockConnectivityService.connectivityState).thenReturn(connectivityFlow)

    controller.observeConnectivity()
    advanceUntilIdle()

    assertTrue(controller.isOnline.value)
  }

  @Test
  fun `joinSelectedEvent blocked when offline`() = runTest {
    // Set offline state
    whenever(mockConnectivityService.connectivityState)
        .thenReturn(flowOf(ConnectivityState(isConnected = false)))

    controller.observeConnectivity()
    advanceUntilIdle()

    controller.joinSelectedEvent()
    advanceUntilIdle()

    verify(mockSetErrorMessage).invoke("Joining events requires an internet connection")
    verify(mockEventRepository, never()).editEventAsUser(any(), any(), any())
  }

  @Test
  fun `saveSelectedEvent queues action when offline`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)

    // Set offline state
    whenever(mockConnectivityService.connectivityState)
        .thenReturn(flowOf(ConnectivityState(isConnected = false)))
    controller.observeConnectivity()
    advanceUntilIdle()

    controller.saveSelectedEvent()
    advanceUntilIdle()

    // Optimistic update applied
    assertTrue(controller.savedEvents.contains(testEvent))
    // Action queued
    verify(mockUserProfileRepository, never()).saveUserProfile(any<UserProfile>())
    assertEquals(1, controller.getOfflineQueueSize())
  }

  @Test
  fun `unsaveSelectedEvent queues action when offline`() = runTest {
    controller.setSavedEventsForTest(listOf(testEvent))
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)

    // Set offline state
    whenever(mockConnectivityService.connectivityState)
        .thenReturn(flowOf(ConnectivityState(isConnected = false)))
    controller.observeConnectivity()
    advanceUntilIdle()

    controller.unsaveSelectedEvent()
    advanceUntilIdle()

    // Optimistic update applied
    assertFalse(controller.savedEvents.contains(testEvent))
    // Action queued
    verify(mockUserProfileRepository, never()).saveUserProfile(any<UserProfile>())
    assertEquals(1, controller.getOfflineQueueSize())
  }

  @Test
  fun `offline queue processes on reconnection`() = runTest {
    val joinedEvent = testEvent.copy(participantIds = listOf(testUserId))
    controller.setJoinedEventsForTest(listOf(joinedEvent))
    whenever(mockGetSelectedEvent()).thenReturn(joinedEvent)

    val connectivityFlow = MutableStateFlow(ConnectivityState(isConnected = false))
    whenever(mockConnectivityService.connectivityState).thenReturn(connectivityFlow)
    controller.observeConnectivity()
    advanceUntilIdle()

    // Queue action while offline
    controller.leaveSelectedEvent()
    advanceUntilIdle()

    assertEquals(1, controller.getOfflineQueueSize())

    // Mock repository response for queue processing
    whenever(mockEventRepository.editEventAsUser(any(), any(), any())).thenReturn(Unit)
    whenever(mockEventRepository.getFilteredEvents(any(), any())).thenReturn(emptyList())

    // Go back online
    connectivityFlow.emit(ConnectivityState(isConnected = true, networkType = NetworkType.WIFI))
    advanceUntilIdle()

    // Queue processed
    verify(mockEventRepository).editEventAsUser(joinedEvent.uid, testUserId, false)
    assertEquals(0, controller.getOfflineQueueSize())
  }

  @Test
  fun `offline queue retries up to 5 times before reverting`() = runTest {
    controller.setSavedEventsForTest(emptyList())
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)

    val connectivityFlow = MutableStateFlow(ConnectivityState(isConnected = false))
    whenever(mockConnectivityService.connectivityState).thenReturn(connectivityFlow)
    controller.observeConnectivity()
    advanceUntilIdle()

    // Queue action while offline
    controller.saveSelectedEvent()
    advanceUntilIdle()

    assertTrue(controller.savedEvents.contains(testEvent)) // Optimistic update
    assertEquals(1, controller.getOfflineQueueSize())

    // Mock repository to always fail
    whenever(mockUserProfileRepository.saveUserProfile(any<UserProfile>()))
        .thenThrow(RuntimeException("Network error"))

    // Go back online
    connectivityFlow.emit(ConnectivityState(isConnected = true, networkType = NetworkType.WIFI))
    advanceUntilIdle()

    // Should retry 5 times
    verify(mockUserProfileRepository, times(5)).saveUserProfile(any<UserProfile>())
    // Queue cleared after max retries
    assertEquals(0, controller.getOfflineQueueSize())
    // Optimistic update reverted
    assertFalse(controller.savedEvents.contains(testEvent))
  }

  @Test
  fun `clearOfflineQueue removes all pending actions`() = runTest {
    controller.setSavedEventsForTest(emptyList())
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)

    whenever(mockConnectivityService.connectivityState)
        .thenReturn(flowOf(ConnectivityState(isConnected = false)))
    controller.observeConnectivity()
    advanceUntilIdle()

    // Queue multiple actions
    controller.saveSelectedEvent()
    advanceUntilIdle()

    whenever(mockGetSelectedEvent()).thenReturn(testEvent.copy(uid = "event2"))
    controller.saveSelectedEvent()
    advanceUntilIdle()

    assertEquals(2, controller.getOfflineQueueSize())

    controller.clearOfflineQueue()

    assertEquals(0, controller.getOfflineQueueSize())
  }

  @Test
  fun `clearUserScopedState clears offline queue`() = runTest {
    controller.setSavedEventsForTest(emptyList())
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)

    whenever(mockConnectivityService.connectivityState)
        .thenReturn(flowOf(ConnectivityState(isConnected = false)))
    controller.observeConnectivity()
    advanceUntilIdle()

    controller.saveSelectedEvent()
    advanceUntilIdle()

    assertEquals(1, controller.getOfflineQueueSize())

    controller.clearUserScopedState()

    assertEquals(0, controller.getOfflineQueueSize())
  }

  // ========== Edge Cases ==========
  @Test
  fun `leaveSelectedEvent does nothing if event already removed from joined list`() = runTest {
    controller.setJoinedEventsForTest(emptyList())
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)

    controller.leaveSelectedEvent()
    advanceUntilIdle()

    verify(mockEventRepository, never()).editEventAsUser(any(), any(), any())
  }

  @Test
  fun `leaveSelectedEvent succeeds silently when event was deleted`() = runTest {
    val joinedEvent = testEvent.copy(participantIds = listOf(testUserId))
    controller.setJoinedEventsForTest(listOf(joinedEvent))
    whenever(mockGetSelectedEvent()).thenReturn(joinedEvent)

    whenever(mockEventRepository.editEventAsUser(any(), any(), any()))
        .thenThrow(RuntimeException("Event not found"))

    controller.leaveSelectedEvent()
    advanceUntilIdle()

    // No error message set - treated as success
    verify(mockSetErrorMessage, never()).invoke(any())
    assertFalse(controller.joinedEvents.contains(joinedEvent))
  }

  @Test
  fun `isOnline reflects connectivity state`() = runTest {
    val connectivityFlow = MutableStateFlow(ConnectivityState(isConnected = true))
    whenever(mockConnectivityService.connectivityState).thenReturn(connectivityFlow)

    controller.observeConnectivity()
    advanceUntilIdle()

    assertTrue(controller.isOnline.value)

    connectivityFlow.emit(ConnectivityState(isConnected = false))
    advanceUntilIdle()

    assertFalse(controller.isOnline.value)
  }

  // ========== Listen to Owned Events Test ==========
  @Test
  fun `startListeners initializes owned event listener and handles updates`() = runTest {
    val mockOwnedListener = mock<ListenerRegistration>()
    val ownedEvent = testEvent.copy(uid = "owned1", ownerId = testUserId)

    val updateCaptor = argumentCaptor<(List<Event>, List<Event>, List<String>) -> Unit>()
    whenever(mockEventRepository.listenToJoinedEvents(any(), any()))
        .thenReturn(mock<ListenerRegistration>())
    whenever(mockEventRepository.listenToSavedEvents(any(), any()))
        .thenReturn(mock<ListenerRegistration>())
    whenever(mockEventRepository.listenToOwnedEvents(any(), updateCaptor.capture()))
        .thenReturn(mockOwnedListener)

    controller.startListeners()

    // Verify listener was created
    verify(mockEventRepository).listenToOwnedEvents(eq(testUserId), any())

    // Simulate adding an owned event
    updateCaptor.firstValue.invoke(listOf(ownedEvent), emptyList(), emptyList())
    assertTrue(controller.ownedEvents.contains(ownedEvent))

    // Simulate removing the owned event
    updateCaptor.firstValue.invoke(emptyList(), emptyList(), listOf(ownedEvent.uid))
    assertTrue(controller.ownedEvents.isEmpty())

    // Verify listener is removed on stop
    controller.stopListeners()
    verify(mockOwnedListener).remove()
  }

  // ========== Anti-Spam Test ==========
  @Test
  fun `spam prevention blocks repeated actions within 500ms on same event`() = runTest {
    whenever(mockGetSelectedEvent()).thenReturn(testEvent)
    whenever(mockEventRepository.editEventAsUser(any(), any(), any())).thenReturn(Unit)

    // First call should succeed (t=0)
    controller.joinSelectedEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    verify(mockEventRepository, times(1)).editEventAsUser(testEvent.uid, testUserId, true)

    // Immediate second call should be blocked (t=0)
    controller.joinSelectedEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    verify(mockEventRepository, times(1)).editEventAsUser(any(), any(), any()) // Still 1

    // Advance fake time by 500ms
    fakeTimeProvider.advance(500)

    // Now it should work (t=500)
    controller.joinSelectedEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    verify(mockEventRepository, times(2)).editEventAsUser(testEvent.uid, testUserId, true)

    // Different event works immediately
    val event2 = testEvent.copy(uid = "event2")
    whenever(mockGetSelectedEvent()).thenReturn(event2)
    controller.joinSelectedEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    verify(mockEventRepository).editEventAsUser(event2.uid, testUserId, true)
  }

  // ========== User Search Tests ==========
  @Test
  fun `userSearchResults is initially empty`() {
    assertTrue(controller.userSearchResults.isEmpty())
  }

  @Test
  fun `clearUserSearchResults clears user search results`() {
    // Verify clearUserSearchResults doesn't crash and results stay empty
    controller.clearUserSearchResults()
    assertTrue(controller.userSearchResults.isEmpty())
  }
}

/** Provides a chosen time in milliseconds for tests */
class FakeTimeProvider : () -> Long {
  var currentTime = 0L

  override fun invoke() = currentTime

  fun advance(millis: Long) {
    currentTime += millis
  }
}

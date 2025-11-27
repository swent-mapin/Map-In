package com.swent.mapin.ui.map.eventstate

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.Location
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.filters.Filters
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
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

  private lateinit var controller: MapEventStateController
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

    // Mock Firebase Auth
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn(testUserId)

    // Mock FiltersSectionViewModel
    val defaultFilters = Filters()
    val filtersFlow = MutableStateFlow(defaultFilters)
    whenever(mockFiltersSectionViewModel.filters).thenReturn(filtersFlow)

    // Mock repositories
    runBlocking {
      whenever(mockEventRepository.getFilteredEvents(any(), any())).thenReturn(emptyList())
      whenever(mockEventRepository.getSavedEvents(any())).thenReturn(emptyList())
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
            getSelectedEvent = mockGetSelectedEvent,
            setErrorMessage = mockSetErrorMessage,
            clearErrorMessage = mockClearErrorMessage)
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
  fun `refreshEventsList refreshes all event lists`() = runTest {
    val filteredEvents = listOf(testEvent)
    val joinedEvents = listOf(testEvent.copy(participantIds = listOf(testUserId)))
    val savedEvents = listOf(testEvent)
    whenever(mockEventRepository.getFilteredEvents(any(), any())).thenReturn(filteredEvents)
    whenever(mockEventRepository.getJoinedEvents(testUserId)).thenReturn(joinedEvents)
    whenever(mockEventRepository.getSavedEvents(testUserId)).thenReturn(savedEvents)

    controller.refreshEventsList()
    advanceUntilIdle()

    assertEquals(filteredEvents, controller.allEvents)
    assertEquals(joinedEvents, controller.joinedEvents)
    assertEquals(savedEvents, controller.savedEvents)
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
    whenever(mockEventRepository.getFilteredEvents(any(), any())).thenReturn(listOf(updatedEvent))
    whenever(mockEventRepository.getSavedEvents(testUserId)).thenReturn(emptyList())
    whenever(mockEventRepository.getJoinedEvents(testUserId)).thenReturn(listOf(updatedEvent))

    // Execute the method
    controller.joinSelectedEvent()
    advanceUntilIdle()

    // Assertions
    assertEquals(listOf(updatedEvent), controller.allEvents)
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
    whenever(mockGetSelectedEvent()).thenReturn(joinedEvent)

    val updatedEvent = testEvent.copy(participantIds = emptyList())
    whenever(mockEventRepository.getFilteredEvents(any(), any())).thenReturn(listOf(updatedEvent))
    whenever(mockEventRepository.getJoinedEvents(testUserId)).thenReturn(emptyList())
    whenever(mockEventRepository.getSavedEvents(testUserId)).thenReturn(emptyList())

    controller.leaveSelectedEvent()
    advanceUntilIdle()

    assertEquals(listOf(updatedEvent), controller.allEvents)
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
    whenever(mockEventRepository.getSavedEvents(testUserId)).thenReturn(savedEvents)

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
    whenever(mockEventRepository.getSavedEvents(testUserId)).thenReturn(emptyList())

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
}

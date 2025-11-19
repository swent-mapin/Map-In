package com.swent.mapin.ui.map.offline

import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.network.ConnectivityService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventBasedOfflineRegionManagerTest {

  private lateinit var mockEventRepository: EventRepository
  private lateinit var mockOfflineRegionManager: OfflineRegionManager
  private lateinit var mockConnectivityService: ConnectivityService
  private lateinit var manager: EventBasedOfflineRegionManager

  private lateinit var savedEventsFlow: MutableStateFlow<List<Event>>
  private lateinit var joinedEventsFlow: MutableStateFlow<List<Event>>

  @Before
  fun setup() {
    mockEventRepository = mockk(relaxed = true)
    mockOfflineRegionManager = mockk(relaxed = true)
    mockConnectivityService = mockk(relaxed = true)

    savedEventsFlow = MutableStateFlow(emptyList())
    joinedEventsFlow = MutableStateFlow(emptyList())

    every { mockConnectivityService.isConnected() } returns true
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `observeEvents triggers download when events are added and device is online`() = runTest {
    manager =
        EventBasedOfflineRegionManager(
            eventRepository = mockEventRepository,
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    // Start observing
    manager.observeEvents("user123", savedEventsFlow, joinedEventsFlow)

    // Emit saved events
    val event1 =
        Event(uid = "event1", title = "Event 1", location = Location("Location 1", 46.5197, 6.5660))
    savedEventsFlow.value = listOf(event1)

    testScheduler.advanceUntilIdle()

    // Verify download was triggered
    coVerify(exactly = 1) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }

    // Stop observing to clean up coroutines
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `observeEvents does not trigger download when device is offline`() = runTest {
    every { mockConnectivityService.isConnected() } returns false

    manager =
        EventBasedOfflineRegionManager(
            eventRepository = mockEventRepository,
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    manager.observeEvents("user123", savedEventsFlow, joinedEventsFlow)

    val event1 =
        Event(uid = "event1", title = "Event 1", location = Location("Location 1", 46.5197, 6.5660))
    savedEventsFlow.value = listOf(event1)

    testScheduler.advanceUntilIdle()

    // Verify download was NOT triggered
    coVerify(exactly = 0) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }

    // Stop observing to clean up coroutines
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `observeEvents combines saved and joined events without duplicates`() = runTest {
    manager =
        EventBasedOfflineRegionManager(
            eventRepository = mockEventRepository,
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    manager.observeEvents("user123", savedEventsFlow, joinedEventsFlow)

    val event1 =
        Event(uid = "event1", title = "Event 1", location = Location("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(uid = "event2", title = "Event 2", location = Location("Location 2", 46.5300, 6.5800))

    // Set both saved and joined to contain event1 (duplicate) and event2
    savedEventsFlow.value = listOf(event1, event2)
    joinedEventsFlow.value = listOf(event1) // event1 is both saved and joined

    testScheduler.advanceUntilIdle()

    // Should download 2 regions (event1 and event2), not 3
    coVerify(exactly = 2) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }

    // Stop observing to clean up coroutines
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `stopObserving cancels active downloads`() = runTest {
    manager =
        EventBasedOfflineRegionManager(
            eventRepository = mockEventRepository,
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    manager.observeEvents("user123", savedEventsFlow, joinedEventsFlow)

    manager.stopObserving()

    verify { mockOfflineRegionManager.cancelActiveDownload() }
  }
}

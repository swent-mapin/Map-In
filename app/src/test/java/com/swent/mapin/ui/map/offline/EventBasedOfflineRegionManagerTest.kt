package com.swent.mapin.ui.map.offline

import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
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

  private lateinit var mockOfflineRegionManager: OfflineRegionManager
  private lateinit var mockConnectivityService: ConnectivityService
  private lateinit var manager: EventBasedOfflineRegionManager

  private lateinit var savedEventsFlow: MutableStateFlow<List<Event>>
  private lateinit var joinedEventsFlow: MutableStateFlow<List<Event>>

  @Before
  fun setup() {
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
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    // Start observing
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)

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
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    manager.observeEvents(savedEventsFlow, joinedEventsFlow)

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
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    manager.observeEvents(savedEventsFlow, joinedEventsFlow)

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
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    manager.observeEvents(savedEventsFlow, joinedEventsFlow)

    manager.stopObserving()

    verify { mockOfflineRegionManager.cancelActiveDownload() }
  }

  @Test
  fun `downloadRegionsForEvents respects max regions limit`() = runTest {
    manager =
        EventBasedOfflineRegionManager(
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this,
            maxRegions = 2) // Set max to 2

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    // Create 3 events but only 2 should be downloaded
    val events =
        listOf(
            Event(uid = "event1", title = "Event 1", location = Location("Loc1", 46.5197, 6.5660)),
            Event(uid = "event2", title = "Event 2", location = Location("Loc2", 46.5300, 6.5800)),
            Event(uid = "event3", title = "Event 3", location = Location("Loc3", 46.5400, 6.5900)))

    manager.downloadRegionsForEvents(events)

    // Should only download 2 regions (respecting maxRegions limit)
    coVerify(exactly = 2) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }
  }

  @Test
  fun `downloadRegionsForEvents skips already downloaded events`() = runTest {
    manager =
        EventBasedOfflineRegionManager(
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    manager.observeEvents(savedEventsFlow, joinedEventsFlow)

    val event1 =
        Event(uid = "event1", title = "Event 1", location = Location("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(uid = "event2", title = "Event 2", location = Location("Location 2", 46.5300, 6.5800))

    // First download
    savedEventsFlow.value = listOf(event1)
    testScheduler.advanceUntilIdle()

    // Second download with same event1 + new event2
    savedEventsFlow.value = listOf(event1, event2)
    testScheduler.advanceUntilIdle()

    // Should only trigger 2 downloads total (event1 once, event2 once)
    coVerify(exactly = 2) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `clearDownloadedEventIds clears the download record`() = runTest {
    manager =
        EventBasedOfflineRegionManager(
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    manager.observeEvents(savedEventsFlow, joinedEventsFlow)

    val event1 =
        Event(uid = "event1", title = "Event 1", location = Location("Location 1", 46.5197, 6.5660))

    // First download
    savedEventsFlow.value = listOf(event1)
    testScheduler.advanceUntilIdle()

    assertEquals(1, manager.getDownloadedCount())

    // Clear downloaded IDs
    manager.clearDownloadedEventIds()
    assertEquals(0, manager.getDownloadedCount())

    // Second download should re-download event1 - emit empty first to trigger new collection
    savedEventsFlow.value = emptyList()
    testScheduler.advanceUntilIdle()

    savedEventsFlow.value = listOf(event1)
    testScheduler.advanceUntilIdle()

    // Should trigger 2 downloads total (event1 twice)
    coVerify(exactly = 2) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `getDownloadedCount returns correct count`() = runTest {
    manager =
        EventBasedOfflineRegionManager(
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = this)

    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onComplete = arg<(Result<Unit>) -> Unit>(3)
          onComplete(Result.success(Unit))
        }

    manager.observeEvents(savedEventsFlow, joinedEventsFlow)

    assertEquals(0, manager.getDownloadedCount())

    val event1 =
        Event(uid = "event1", title = "Event 1", location = Location("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(uid = "event2", title = "Event 2", location = Location("Location 2", 46.5300, 6.5800))

    savedEventsFlow.value = listOf(event1, event2)
    testScheduler.advanceUntilIdle()

    assertEquals(2, manager.getDownloadedCount())

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `getMaxRegions returns configured max regions`() {
    val customMaxRegions = 50

    manager =
        EventBasedOfflineRegionManager(
            offlineRegionManager = mockOfflineRegionManager,
            connectivityService = mockConnectivityService,
            scope = kotlinx.coroutines.test.TestScope(),
            maxRegions = customMaxRegions)

    assertEquals(customMaxRegions, manager.getMaxRegions())
  }
}

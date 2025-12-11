package com.swent.mapin.ui.map.offline

import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
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
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))
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
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))
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
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(
            uid = "event2",
            title = "Event 2",
            location = Location.from("Location 2", 46.5300, 6.5800))

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
            Event(
                uid = "event1",
                title = "Event 1",
                location = Location.from("Loc1", 46.5197, 6.5660)),
            Event(
                uid = "event2",
                title = "Event 2",
                location = Location.from("Loc2", 46.5300, 6.5800)),
            Event(
                uid = "event3",
                title = "Event 3",
                location = Location.from("Loc3", 46.5400, 6.5900)))

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
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(
            uid = "event2",
            title = "Event 2",
            location = Location.from("Location 2", 46.5300, 6.5800))

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
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))

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
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(
            uid = "event2",
            title = "Event 2",
            location = Location.from("Location 2", 46.5300, 6.5800))

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

  @Test
  fun `observeEventsForDeletion triggers deletion when events are removed`() = runTest {
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

    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns Result.success(Unit)

    val event1 =
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(
            uid = "event2",
            title = "Event 2",
            location = Location.from("Location 2", 46.5300, 6.5800))

    // Start observing for downloads and deletions
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)

    // Add two events
    savedEventsFlow.value = listOf(event1, event2)
    testScheduler.advanceUntilIdle()

    // Verify downloads happened
    coVerify(exactly = 2) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }
    assertEquals(2, manager.getDownloadedCount())

    // Remove event1 (keep only event2)
    savedEventsFlow.value = listOf(event2)
    testScheduler.advanceUntilIdle()

    // Verify deletion was triggered for event1
    coVerify(exactly = 1) { mockOfflineRegionManager.removeTileRegion(any()) }
    assertEquals(1, manager.getDownloadedCount())

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `observeEventsForDeletion handles multiple removals`() = runTest {
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

    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns Result.success(Unit)

    val event1 =
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))
    val event2 =
        Event(
            uid = "event2",
            title = "Event 2",
            location = Location.from("Location 2", 46.5300, 6.5800))
    val event3 =
        Event(
            uid = "event3",
            title = "Event 3",
            location = Location.from("Location 3", 46.5400, 6.5900))

    // Start observing
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)

    // Add three events
    savedEventsFlow.value = listOf(event1, event2, event3)
    testScheduler.advanceUntilIdle()

    assertEquals(3, manager.getDownloadedCount())

    // Remove two events at once
    savedEventsFlow.value = listOf(event2)
    testScheduler.advanceUntilIdle()

    // Verify both deletions were triggered
    coVerify(exactly = 2) { mockOfflineRegionManager.removeTileRegion(any()) }
    assertEquals(1, manager.getDownloadedCount())

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `observeEventsForDeletion does not trigger on first observation`() = runTest {
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

    val event1 =
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))

    // Start observing for deletions
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)

    // Add event - should NOT trigger deletion on first observation
    savedEventsFlow.value = listOf(event1)
    testScheduler.advanceUntilIdle()

    // Verify no deletion was triggered
    coVerify(exactly = 0) { mockOfflineRegionManager.removeTileRegion(any()) }

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `deleteRegionForEvent removes specific event region`() = runTest {
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

    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns Result.success(Unit)

    val event1 =
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660))

    // Download event first
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    savedEventsFlow.value = listOf(event1)
    testScheduler.advanceUntilIdle()

    assertEquals(1, manager.getDownloadedCount())

    // Delete the event region manually
    manager.deleteRegionForEvent(event1)
    testScheduler.advanceUntilIdle()

    // Verify deletion was called
    coVerify(exactly = 1) { mockOfflineRegionManager.removeTileRegion(any()) }

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `observeEventsForDeletion filters finished events from tracking`() = runTest {
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

    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns Result.success(Unit)

    // Create event with endDate in the past (1 hour ago)
    val now = com.google.firebase.Timestamp.now()
    val pastTime = com.google.firebase.Timestamp(now.seconds - 3600, 0)
    val finishedEvent =
        Event(
            uid = "finished1",
            title = "Finished Event",
            location = Location.from("Location 1", 46.5197, 6.5660),
            date = pastTime,
            endDate = pastTime)

    val activeEvent =
        Event(
            uid = "active1",
            title = "Active Event",
            location = Location.from("Location 2", 46.5300, 6.5800),
            date = com.google.firebase.Timestamp(now.seconds + 3600, 0))

    // Start observing
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)

    // Add both events - download observer will try to download both
    savedEventsFlow.value = listOf(finishedEvent, activeEvent)
    testScheduler.advanceUntilIdle()

    // Both should be attempted for download (download observer doesn't filter)
    coVerify(exactly = 2) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }

    // But deletion observer should filter out finished event from tracking
    // So only active event is in previousEventIds for deletion observer
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `observeEventsForDeletion deletes events when they finish`() = runTest {
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

    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns Result.success(Unit)

    // Create event with future endDate
    val now = com.google.firebase.Timestamp.now()
    val futureTime = com.google.firebase.Timestamp(now.seconds + 3600, 0)
    val event =
        Event(
            uid = "event1",
            title = "Event 1",
            location = Location.from("Location 1", 46.5197, 6.5660),
            date = futureTime,
            endDate = futureTime)

    // Start observing
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)

    // Add event (future, so it's active)
    savedEventsFlow.value = listOf(event)
    testScheduler.advanceUntilIdle()

    coVerify(exactly = 1) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }
    assertEquals(1, manager.getDownloadedCount())

    // Simulate time passing - update event to have past endDate
    val pastTime = com.google.firebase.Timestamp(now.seconds - 3600, 0)
    val finishedEvent = event.copy(endDate = pastTime)

    // Re-emit with finished event (simulates periodic check)
    savedEventsFlow.value = listOf(finishedEvent)
    testScheduler.advanceUntilIdle()

    // Should trigger deletion because event is now finished
    coVerify(exactly = 1) { mockOfflineRegionManager.removeTileRegion(any()) }

    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `observeEventsForDeletion uses start date if no endDate provided`() = runTest {
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

    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns Result.success(Unit)

    // Event with past start date but no endDate
    val now = com.google.firebase.Timestamp.now()
    val pastTime = com.google.firebase.Timestamp(now.seconds - 3600, 0)
    val finishedEvent =
        Event(
            uid = "finished1",
            title = "Finished Event",
            location = Location.from("Location 1", 46.5197, 6.5660),
            date = pastTime,
            endDate = null)

    // Start observing
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)

    // Add finished event - download observer will try to download it
    savedEventsFlow.value = listOf(finishedEvent)
    testScheduler.advanceUntilIdle()

    // Download observer attempts download (doesn't filter by date)
    coVerify(exactly = 1) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }

    // But deletion observer filters it out from tracking (uses start date since no endDate)
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `downloadRegionsForEvents handles download failure`() = runTest {
    val error = Exception("Download failed")
    var capturedEvent: Event? = null
    var capturedResult: Result<Unit>? = null
    manager =
        EventBasedOfflineRegionManager(
            mockOfflineRegionManager,
            mockConnectivityService,
            this,
            onDownloadComplete = { e, r ->
              capturedEvent = e
              capturedResult = r
            })
    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          arg<(Result<Unit>) -> Unit>(3)(Result.failure(error))
        }
    val event = Event(uid = "e1", title = "E1", location = Location.from("L1", 46.5197, 6.5660))
    manager.downloadRegionsForEvents(listOf(event))
    coVerify(exactly = 1) { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) }
    assertNotNull(capturedResult)
    assertTrue(capturedResult!!.isFailure)
    assertEquals(error, capturedResult!!.exceptionOrNull())
    assertEquals(event, capturedEvent)
    assertEquals(0, manager.getDownloadedCount())
  }

  @Test
  fun `downloadRegionSuspend reports progress updates`() = runTest {
    val updates = mutableListOf<Float>()
    manager =
        EventBasedOfflineRegionManager(
            mockOfflineRegionManager,
            mockConnectivityService,
            this,
            onDownloadProgress = { _, p -> updates.add(p) })
    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          val onProgress = arg<(Float) -> Unit>(2)
          onProgress(0.25f)
          onProgress(0.75f)
          arg<(Result<Unit>) -> Unit>(3)(Result.success(Unit))
        }
    manager.downloadRegionsForEvents(
        listOf(Event(uid = "e1", title = "E1", location = Location.from("L1", 46.5197, 6.5660))))
    assertEquals(2, updates.size)
    assertEquals(0.25f, updates[0], 0.01f)
    assertEquals(0.75f, updates[1], 0.01f)
  }

  @Test
  fun `deleteRegionsForEvents handles deletion failure`() = runTest {
    manager =
        EventBasedOfflineRegionManager(mockOfflineRegionManager, mockConnectivityService, this)
    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          arg<(Result<Unit>) -> Unit>(3)(Result.success(Unit))
        }
    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns
        Result.failure(Exception("Failed"))
    val event = Event(uid = "e1", title = "E1", location = Location.from("L1", 46.5197, 6.5660))
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)
    savedEventsFlow.value = listOf(event)
    testScheduler.advanceUntilIdle()
    assertEquals(1, manager.getDownloadedCount())
    savedEventsFlow.value = emptyList()
    testScheduler.advanceUntilIdle()
    coVerify(exactly = 1) { mockOfflineRegionManager.removeTileRegion(any()) }
    assertEquals(0, manager.getDownloadedCount())
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `deleteRegionsForEvents handles event without stored location`() = runTest {
    manager =
        EventBasedOfflineRegionManager(mockOfflineRegionManager, mockConnectivityService, this)
    val event = Event(uid = "e1", title = "E1", location = Location.from("L1", 46.5197, 6.5660))
    manager.observeEventsForDeletion(savedEventsFlow, joinedEventsFlow)
    savedEventsFlow.value = listOf(event)
    testScheduler.advanceUntilIdle()
    savedEventsFlow.value = emptyList()
    testScheduler.advanceUntilIdle()
    coVerify(exactly = 0) { mockOfflineRegionManager.removeTileRegion(any()) }
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun `deleteRegionForEvent handles deletion failure`() = runTest {
    manager =
        EventBasedOfflineRegionManager(mockOfflineRegionManager, mockConnectivityService, this)
    coEvery { mockOfflineRegionManager.downloadRegion(any(), any(), any(), any()) } answers
        {
          arg<(Result<Unit>) -> Unit>(3)(Result.success(Unit))
        }
    coEvery { mockOfflineRegionManager.removeTileRegion(any()) } returns
        Result.failure(Exception("Failed"))
    val event = Event(uid = "e1", title = "E1", location = Location.from("L1", 46.5197, 6.5660))
    manager.observeEvents(savedEventsFlow, joinedEventsFlow)
    savedEventsFlow.value = listOf(event)
    testScheduler.advanceUntilIdle()
    assertEquals(1, manager.getDownloadedCount())
    manager.deleteRegionForEvent(event)
    testScheduler.advanceUntilIdle()
    coVerify(exactly = 1) { mockOfflineRegionManager.removeTileRegion(any()) }
    assertEquals(1, manager.getDownloadedCount())
    manager.stopObserving()
    testScheduler.advanceUntilIdle()
  }
}

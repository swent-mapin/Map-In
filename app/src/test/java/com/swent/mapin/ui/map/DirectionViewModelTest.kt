package com.swent.mapin.ui.map

import android.location.Location
import com.mapbox.geojson.Point
import com.swent.mapin.ui.map.directions.DirectionState
import com.swent.mapin.ui.map.directions.DirectionViewModel
import com.swent.mapin.ui.map.directions.MapboxDirectionsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DirectionViewModelTest {

  private lateinit var viewModel: DirectionViewModel
  private lateinit var mockDirectionsService: MapboxDirectionsService
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var testOrigin: Point
  private lateinit var testDestination: Point
  private lateinit var mockRoutePoints: List<Point>

  private var testTime = 0L
  private val testClock: () -> Long = { testTime }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockDirectionsService = mock(MapboxDirectionsService::class.java)
    testTime = 0L

    testOrigin = Point.fromLngLat(6.5668, 46.5197)
    testDestination = Point.fromLngLat(6.5700, 46.5220)

    mockRoutePoints =
        listOf(
            Point.fromLngLat(6.5668, 46.5197),
            Point.fromLngLat(6.5680, 46.5205),
            Point.fromLngLat(6.5690, 46.5215),
            Point.fromLngLat(6.5700, 46.5220))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Cleared`() {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections transitions to Displayed state on success`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState
    assertTrue(state is DirectionState.Displayed)

    if (state is DirectionState.Displayed) {
      assertEquals(testOrigin, state.origin)
      assertEquals(testDestination, state.destination)
      assertEquals(mockRoutePoints, state.routePoints)
    }
  }

  @Test
  fun `requestDirections calls service with correct parameters`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockDirectionsService).getDirections(testOrigin, testDestination)
  }

  @Test
  fun `requestDirections sets Cleared state when service returns null`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(null)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections sets Cleared state when service returns empty list`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(emptyList())

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `clearDirection sets state to Cleared from Displayed`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.directionState is DirectionState.Displayed)

    viewModel.clearDirection()
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `clearDirection from Cleared state remains Cleared`() {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    assertTrue(viewModel.directionState is DirectionState.Cleared)

    viewModel.clearDirection()
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `multiple consecutive requestDirections updates state correctly`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val destination2 = Point.fromLngLat(6.5800, 46.5300)
    val mockRoutePoints2 =
        listOf(Point.fromLngLat(6.5668, 46.5197), Point.fromLngLat(6.5800, 46.5300))

    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)
    `when`(mockDirectionsService.getDirections(testOrigin, destination2))
        .thenReturn(mockRoutePoints2)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    val state1 = viewModel.directionState as DirectionState.Displayed
    assertEquals(testDestination, state1.destination)
    assertEquals(mockRoutePoints, state1.routePoints)

    viewModel.requestDirections(testOrigin, destination2)
    testDispatcher.scheduler.advanceUntilIdle()

    val state2 = viewModel.directionState as DirectionState.Displayed
    assertEquals(destination2, state2.destination)
    assertEquals(mockRoutePoints2, state2.routePoints)
  }

  @Test
  fun `requestDirections preserves route points order`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState as DirectionState.Displayed
    assertEquals(mockRoutePoints[0], state.routePoints[0])
    assertEquals(
        mockRoutePoints[mockRoutePoints.size - 1], state.routePoints[state.routePoints.size - 1])
  }

  @Test
  fun `viewModel without service stays in Cleared state`() = runTest {
    viewModel = DirectionViewModel(null, testClock)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections with null service does not crash`() = runTest {
    viewModel = DirectionViewModel(null, testClock)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections handles single point route`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val singlePointRoute = listOf(testOrigin)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(singlePointRoute)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState as DirectionState.Displayed
    assertEquals(1, state.routePoints.size)
  }

  @Test
  fun `requestDirections handles large route with many points`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val largeRoute = (0..100).map { i -> Point.fromLngLat(6.5668 + i * 0.001, 46.5197 + i * 0.001) }
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(largeRoute)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState as DirectionState.Displayed
    assertEquals(101, state.routePoints.size)
  }

  // ========== LIVE TRACKING TESTS ==========

  @Test
  fun `onLocationUpdate does nothing when state is Cleared`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val mockLocation = createMockLocation(46.5197, 6.5668)

    viewModel.onLocationUpdate(mockLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockDirectionsService, never()).getDirections(any(), any())
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `onLocationUpdate does nothing when state is Loading`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination)
    // Don't advance - keep in Loading state

    val mockLocation = createMockLocation(46.5197, 6.5668)
    viewModel.onLocationUpdate(mockLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    // Only 1 call from requestDirections, not from onLocationUpdate
    verify(mockDirectionsService, times(1)).getDirections(any(), any())
  }

  @Test
  fun `onLocationUpdate triggers refresh when user moves more than 10 meters`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    // Advance time by 11 seconds
    testTime += 11000

    val newLocation = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(15f)

    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // 1 initial + 1 from location update
    verify(mockDirectionsService, times(2)).getDirections(any(), any())
  }

  @Test
  fun `onLocationUpdate does not trigger refresh when user moves less than 10 meters`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    // Advance time
    testTime += 11000

    val newLocation = createMockLocation(46.5197, 6.5668)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(5f)

    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Only 1 call from initial requestDirections
    verify(mockDirectionsService, times(1)).getDirections(any(), any())
  }

  @Test
  fun `onLocationUpdate respects 10 second time threshold`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    // First update at 5 seconds - should be ignored (time threshold not met)
    testTime += 5000
    val location1 = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(location1)).thenReturn(15f)
    viewModel.onLocationUpdate(location1)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Only initial call
    verify(mockDirectionsService, times(1)).getDirections(any(), any())

    // Second update at 12 seconds total - should trigger (both thresholds met)
    testTime += 7000  // Total = 12 seconds
    val location2 = createMockLocation(46.5199, 6.5670)
    `when`(initialLocation.distanceTo(location2)).thenReturn(15f)
    viewModel.onLocationUpdate(location2)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Initial + 1 update
    verify(mockDirectionsService, times(2)).getDirections(any(), any())
  }

  @Test
  fun `onLocationUpdate triggers on first update when lastUpdateLocation is null`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    // Request without passing location
    viewModel.requestDirections(testOrigin, testDestination, null)
    testDispatcher.scheduler.advanceUntilIdle()

    val newLocation = createMockLocation(46.5197, 6.5668)
    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Initial + first update (because lastUpdateLocation was null)
    verify(mockDirectionsService, times(2)).getDirections(any(), any())
  }

  @Test
  fun `onLocationUpdate updates route with new origin point`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    val newRoutePoints = listOf(
        Point.fromLngLat(6.5669, 46.5198),
        Point.fromLngLat(6.5690, 46.5215),
        Point.fromLngLat(6.5700, 46.5220))

    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)
    `when`(mockDirectionsService.getDirections(any(), any()))
        .thenReturn(newRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    testTime += 11000

    val newLocation = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(15f)

    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState as DirectionState.Displayed
    assertEquals(newRoutePoints, state.routePoints)
    assertEquals(6.5669, state.origin.longitude(), 0.0001)
    assertEquals(46.5198, state.origin.latitude(), 0.0001)
  }

  @Test
  fun `onLocationUpdate preserves destination`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    testTime += 11000

    val newLocation = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(15f)

    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState as DirectionState.Displayed
    assertEquals(testDestination, state.destination)
  }

  @Test
  fun `onLocationUpdate rate limiting prevents concurrent updates`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    testTime += 11000

    // First location update
    val location1 = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(location1)).thenReturn(15f)
    viewModel.onLocationUpdate(location1)
    testDispatcher.scheduler.advanceTimeBy(500)

    // Second location update before first completes (within 1s delay)
    val location2 = createMockLocation(46.5199, 6.5670)
    `when`(initialLocation.distanceTo(location2)).thenReturn(20f)
    viewModel.onLocationUpdate(location2)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Initial + only first update (second ignored due to rate limiting)
    verify(mockDirectionsService, times(2)).getDirections(any(), any())
  }

  @Test
  fun `clearDirection resets lastUpdateLocation`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.clearDirection()

    // Now request again without location
    viewModel.requestDirections(testOrigin, testDestination, null)
    testDispatcher.scheduler.advanceUntilIdle()

    val newLocation = createMockLocation(46.5198, 6.5669)
    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Should trigger update because lastUpdateLocation was reset to null
    verify(mockDirectionsService, times(3)).getDirections(any(), any())
  }

  @Test
  fun `clearDirection cancels pending rate limit job`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    testTime += 11000

    val newLocation = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(15f)
    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(500)

    // Clear while job is pending
    viewModel.clearDirection()
    testDispatcher.scheduler.advanceTimeBy(1000)
    testDispatcher.scheduler.advanceUntilIdle()

    // Only initial call, update was cancelled
    verify(mockDirectionsService, times(1)).getDirections(any(), any())
  }

  @Test
  fun `onLocationUpdate handles API failure gracefully`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    val initialState = viewModel.directionState as DirectionState.Displayed

    // API returns null on update
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(null)

    testTime += 11000

    val newLocation = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(15f)
    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain Displayed with old route
    val currentState = viewModel.directionState as DirectionState.Displayed
    assertEquals(initialState.routePoints, currentState.routePoints)
  }

  @Test
  fun `onLocationUpdate handles empty route from API`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    val initialState = viewModel.directionState as DirectionState.Displayed

    // API returns empty list
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(emptyList())

    testTime += 11000

    val newLocation = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(15f)
    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain Displayed with old route
    val currentState = viewModel.directionState as DirectionState.Displayed
    assertEquals(initialState.routePoints, currentState.routePoints)
  }

  @Test
  fun `requestDirections with location initializes tracking state`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    testTime += 11000

    // Immediately try another update with short distance
    val newLocation = createMockLocation(46.5197, 6.5668)
    `when`(initialLocation.distanceTo(newLocation)).thenReturn(5f)
    viewModel.onLocationUpdate(newLocation)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Should not trigger because lastUpdateLocation was initialized and distance < 10m
    verify(mockDirectionsService, times(1)).getDirections(any(), any())
  }

  @Test
  fun `multiple sequential location updates work correctly`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService, testClock)
    val initialLocation = createMockLocation(46.5197, 6.5668)

    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination, initialLocation)
    testDispatcher.scheduler.advanceUntilIdle()

    // First update
    testTime += 11000
    val location1 = createMockLocation(46.5198, 6.5669)
    `when`(initialLocation.distanceTo(location1)).thenReturn(15f)
    viewModel.onLocationUpdate(location1)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Second update - now we need to mock location1.distanceTo(location2)
    testTime += 11000
    val location2 = createMockLocation(46.5199, 6.5670)
    `when`(location1.distanceTo(location2)).thenReturn(15f)
    viewModel.onLocationUpdate(location2)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Third update - now we need to mock location2.distanceTo(location3)
    testTime += 11000
    val location3 = createMockLocation(46.5200, 6.5671)
    `when`(location2.distanceTo(location3)).thenReturn(15f)
    viewModel.onLocationUpdate(location3)
    testDispatcher.scheduler.advanceTimeBy(1100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Initial + 3 updates
    verify(mockDirectionsService, times(4)).getDirections(any(), any())
  }

  private fun createMockLocation(latitude: Double, longitude: Double): Location {
    val location = mock(Location::class.java)
    `when`(location.latitude).thenReturn(latitude)
    `when`(location.longitude).thenReturn(longitude)
    return location
  }
}

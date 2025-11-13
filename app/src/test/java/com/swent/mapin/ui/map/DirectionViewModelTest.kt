package com.swent.mapin.ui.map

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
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DirectionViewModelTest {

  private lateinit var viewModel: DirectionViewModel
  private lateinit var mockDirectionsService: MapboxDirectionsService
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var testOrigin: Point
  private lateinit var testDestination: Point
  private lateinit var mockRoutePoints: List<Point>

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockDirectionsService = mock(MapboxDirectionsService::class.java)

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
    viewModel = DirectionViewModel(mockDirectionsService)
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections transitions to Displayed state on success`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService)
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
    viewModel = DirectionViewModel(mockDirectionsService)
    `when`(mockDirectionsService.getDirections(testOrigin, testDestination))
        .thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockDirectionsService).getDirections(testOrigin, testDestination)
  }

  @Test
  fun `requestDirections sets Cleared state when service returns null`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(null)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections sets Cleared state when service returns empty list`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(emptyList())

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `clearDirection sets state to Cleared from Displayed`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(mockRoutePoints)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.directionState is DirectionState.Displayed)

    viewModel.clearDirection()
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `clearDirection from Cleared state remains Cleared`() {
    viewModel = DirectionViewModel(mockDirectionsService)
    assertTrue(viewModel.directionState is DirectionState.Cleared)

    viewModel.clearDirection()
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `multiple consecutive requestDirections updates state correctly`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService)
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
    viewModel = DirectionViewModel(mockDirectionsService)
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
    viewModel = DirectionViewModel(null)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections with null service does not crash`() = runTest {
    viewModel = DirectionViewModel(null)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections handles single point route`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService)
    val singlePointRoute = listOf(testOrigin)
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(singlePointRoute)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState as DirectionState.Displayed
    assertEquals(1, state.routePoints.size)
  }

  @Test
  fun `requestDirections handles large route with many points`() = runTest {
    viewModel = DirectionViewModel(mockDirectionsService)
    val largeRoute = (0..100).map { i -> Point.fromLngLat(6.5668 + i * 0.001, 46.5197 + i * 0.001) }
    `when`(mockDirectionsService.getDirections(any(), any())).thenReturn(largeRoute)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.directionState as DirectionState.Displayed
    assertEquals(101, state.routePoints.size)
  }
}

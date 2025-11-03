package com.swent.mapin.ui.map

import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DirectionViewModel.
 * Tests direction request, clearing, and state transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DirectionViewModelTest {

  private lateinit var viewModel: DirectionViewModel
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var testOrigin: Point
  private lateinit var testDestination: Point

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    viewModel = DirectionViewModel()
    testOrigin = Point.fromLngLat(6.5668, 46.5197)
    testDestination = Point.fromLngLat(6.5700, 46.5220)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Cleared`() {
    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `requestDirections sets Loading state initially`() = runTest {
    viewModel.requestDirections(testOrigin, testDestination)

    testDispatcher.scheduler.advanceTimeBy(100)
    assertTrue(viewModel.directionState is DirectionState.Loading)
  }

  @Test
  fun `requestDirections transitions to Displayed state after delay`() = runTest {
    viewModel.requestDirections(testOrigin, testDestination)

    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.directionState
    assertTrue(state is DirectionState.Displayed)

    if (state is DirectionState.Displayed) {
      assertEquals(testOrigin, state.origin)
      assertEquals(testDestination, state.destination)
      assertTrue(state.routePoints.isNotEmpty())
    }
  }

  @Test
  fun `requestDirections creates straight line route with multiple points`() = runTest {
    viewModel.requestDirections(testOrigin, testDestination)

    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.directionState as DirectionState.Displayed

    assertEquals(11, state.routePoints.size)

    assertEquals(testOrigin.longitude(), state.routePoints.first().longitude(), 0.0001)
    assertEquals(testOrigin.latitude(), state.routePoints.first().latitude(), 0.0001)

    assertEquals(testDestination.longitude(), state.routePoints.last().longitude(), 0.0001)
    assertEquals(testDestination.latitude(), state.routePoints.last().latitude(), 0.0001)
  }

  @Test
  fun `requestDirections route points are properly interpolated`() = runTest {
    viewModel.requestDirections(testOrigin, testDestination)

    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.directionState as DirectionState.Displayed
    val points = state.routePoints

    for (i in 0 until points.size - 1) {
      val current = points[i]
      val next = points[i + 1]

      assertTrue(next.longitude() >= current.longitude())
      assertTrue(next.latitude() >= current.latitude())
    }
  }

  @Test
  fun `clearDirection sets state to Cleared`() = runTest {
    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    assertTrue(viewModel.directionState is DirectionState.Displayed)

    viewModel.clearDirection()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `clearDirection from Cleared state remains Cleared`() {
    assertTrue(viewModel.directionState is DirectionState.Cleared)

    viewModel.clearDirection()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `clearDirection from Loading state returns to Cleared`() = runTest {
    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceTimeBy(100)

    assertTrue(viewModel.directionState is DirectionState.Loading)

    viewModel.clearDirection()

    assertTrue(viewModel.directionState is DirectionState.Cleared)
  }

  @Test
  fun `multiple consecutive requestDirections updates state correctly`() = runTest {
    val destination2 = Point.fromLngLat(6.5800, 46.5300)

    viewModel.requestDirections(testOrigin, testDestination)
    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state1 = viewModel.directionState as DirectionState.Displayed
    assertEquals(testDestination, state1.destination)

    viewModel.requestDirections(testOrigin, destination2)
    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state2 = viewModel.directionState as DirectionState.Displayed
    assertEquals(destination2, state2.destination)
  }

  @Test
  fun `requestDirections with same origin and destination creates valid route`() = runTest {
    viewModel.requestDirections(testOrigin, testOrigin)

    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.directionState as DirectionState.Displayed

    assertTrue(state.routePoints.isNotEmpty())
    assertEquals(testOrigin, state.origin)
    assertEquals(testOrigin, state.destination)
  }

  @Test
  fun `requestDirections simulates API delay correctly`() = runTest {
    viewModel.requestDirections(testOrigin, testDestination)

    testDispatcher.scheduler.advanceTimeBy(400)
    testDispatcher.scheduler.runCurrent()
    assertTrue(viewModel.directionState is DirectionState.Loading)

    testDispatcher.scheduler.advanceTimeBy(200)
    testDispatcher.scheduler.runCurrent()
    assertTrue(viewModel.directionState is DirectionState.Displayed)
  }

  @Test
  fun `requestDirections handles very close points`() = runTest {
    val closeDestination = Point.fromLngLat(6.5669, 46.5198)

    viewModel.requestDirections(testOrigin, closeDestination)
    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.directionState as DirectionState.Displayed

    assertTrue(state.routePoints.isNotEmpty())
    assertEquals(11, state.routePoints.size)
  }

  @Test
  fun `requestDirections handles very far points`() = runTest {
    val farDestination = Point.fromLngLat(7.0000, 47.0000)

    viewModel.requestDirections(testOrigin, farDestination)
    testDispatcher.scheduler.advanceTimeBy(600)
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.directionState as DirectionState.Displayed

    assertTrue(state.routePoints.isNotEmpty())
    assertEquals(11, state.routePoints.size)
  }
}


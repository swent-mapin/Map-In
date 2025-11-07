package com.swent.mapin.ui.map

import com.mapbox.geojson.Point
import com.swent.mapin.ui.map.directions.DirectionInfo
import com.swent.mapin.ui.map.directions.DirectionState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DirectionState sealed class and DirectionInfo data class. Tests all possible
 * states and data structures for direction display.
 */
class DirectionStateTest {

  private lateinit var testOrigin: Point
  private lateinit var testDestination: Point
  private lateinit var testRoutePoints: List<Point>

  @Before
  fun setUp() {
    testOrigin = Point.fromLngLat(6.5668, 46.5197)
    testDestination = Point.fromLngLat(6.5700, 46.5220)
    testRoutePoints =
        listOf(
            Point.fromLngLat(6.5668, 46.5197),
            Point.fromLngLat(6.5680, 46.5205),
            Point.fromLngLat(6.5690, 46.5212),
            Point.fromLngLat(6.5700, 46.5220))
  }

  @Test
  fun `DirectionState Cleared is properly instantiated`() {
    val state = DirectionState.Cleared
    assertNotNull(state)
    assertTrue(state is DirectionState.Cleared)
  }

  @Test
  fun `DirectionState Loading is properly instantiated`() {
    val state = DirectionState.Loading
    assertNotNull(state)
    assertTrue(state is DirectionState.Loading)
  }

  @Test
  fun `DirectionState Displayed contains correct data`() {
    val state =
        DirectionState.Displayed(
            routePoints = testRoutePoints, origin = testOrigin, destination = testDestination)

    assertNotNull(state)
    assertTrue(state is DirectionState.Displayed)
    assertEquals(4, state.routePoints.size)
    assertEquals(testOrigin, state.origin)
    assertEquals(testDestination, state.destination)
  }

  @Test
  fun `DirectionState Displayed handles empty route points`() {
    val state =
        DirectionState.Displayed(
            routePoints = emptyList(), origin = testOrigin, destination = testDestination)

    assertNotNull(state)
    assertTrue(state.routePoints.isEmpty())
    assertEquals(testOrigin, state.origin)
    assertEquals(testDestination, state.destination)
  }

  @Test
  fun `DirectionState Displayed route points are correctly ordered`() {
    val state =
        DirectionState.Displayed(
            routePoints = testRoutePoints, origin = testOrigin, destination = testDestination)

    assertEquals(testOrigin.longitude(), state.routePoints.first().longitude(), 0.0001)
    assertEquals(testOrigin.latitude(), state.routePoints.first().latitude(), 0.0001)
    assertEquals(testDestination.longitude(), state.routePoints.last().longitude(), 0.0001)
    assertEquals(testDestination.latitude(), state.routePoints.last().latitude(), 0.0001)
  }

  @Test
  fun `DirectionInfo contains correct origin and destination`() {
    val directionInfo = DirectionInfo(origin = testOrigin, destination = testDestination)

    assertNotNull(directionInfo)
    assertEquals(testOrigin, directionInfo.origin)
    assertEquals(testDestination, directionInfo.destination)
  }

  @Test
  fun `DirectionInfo equality works correctly`() {
    val info1 = DirectionInfo(testOrigin, testDestination)
    val info2 = DirectionInfo(testOrigin, testDestination)
    val info3 = DirectionInfo(testDestination, testOrigin)

    assertEquals(info1, info2)
    assertNotEquals(info1, info3)
  }

  @Test
  fun `DirectionState types are distinct`() {
    val cleared = DirectionState.Cleared
    val loading = DirectionState.Loading
    val displayed = DirectionState.Displayed(testRoutePoints, testOrigin, testDestination)

    assertNotEquals(cleared, loading)
    assertNotEquals(cleared, displayed)
    assertNotEquals(loading, displayed)
  }

  @Test
  fun `DirectionState Displayed with single point route`() {
    val singlePoint = listOf(testOrigin)
    val state =
        DirectionState.Displayed(
            routePoints = singlePoint, origin = testOrigin, destination = testOrigin)

    assertEquals(1, state.routePoints.size)
    assertEquals(state.origin, state.destination)
  }
}

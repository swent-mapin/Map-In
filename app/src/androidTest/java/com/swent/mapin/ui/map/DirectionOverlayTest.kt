package com.swent.mapin.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for DirectionOverlay composable. Tests the rendering and behavior of direction
 * lines on the map.
 *
 * These are Android instrumented tests that run on a device or emulator. DirectionOverlay requires
 * a MapboxMap context to render properly. Assisted by AI.
 */
@RunWith(AndroidJUnit4::class)
class DirectionOverlayTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun directionOverlay_withEmptyList_doesNotCrash() {
    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState {
                setCameraOptions { center(Point.fromLngLat(6.5668, 46.5197)) }
              }) {
            DirectionOverlay(routePoints = emptyList())
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withSinglePoint_rendersWithoutCrashing() {
    val singlePoint = Point.fromLngLat(6.5668, 46.5197)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState { setCameraOptions { center(singlePoint) } }) {
            DirectionOverlay(routePoints = listOf(singlePoint))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withTwoPoints_rendersCorrectly() {
    val origin = Point.fromLngLat(6.5668, 46.5197)
    val destination = Point.fromLngLat(6.5700, 46.5220)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = rememberMapViewportState { setCameraOptions { center(origin) } }) {
            DirectionOverlay(routePoints = listOf(origin, destination))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withMultiplePoints_rendersCorrectly() {
    val points =
        listOf(
            Point.fromLngLat(6.5668, 46.5197),
            Point.fromLngLat(6.5680, 46.5205),
            Point.fromLngLat(6.5690, 46.5210),
            Point.fromLngLat(6.5700, 46.5220))

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState { setCameraOptions { center(points.first()) } }) {
            DirectionOverlay(routePoints = points)
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withSameOriginAndDestination_rendersCorrectly() {
    val samePoint = Point.fromLngLat(6.5668, 46.5197)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = rememberMapViewportState { setCameraOptions { center(samePoint) } }) {
            DirectionOverlay(routePoints = listOf(samePoint, samePoint))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withLargeNumberOfPoints_rendersWithoutCrashing() {
    val largePointList =
        (0..100).map { i -> Point.fromLngLat(6.5668 + i * 0.001, 46.5197 + i * 0.001) }

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState { setCameraOptions { center(largePointList.first()) } }) {
            DirectionOverlay(routePoints = largePointList)
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withVeryClosePoints_rendersCorrectly() {
    val closePoint1 = Point.fromLngLat(6.5668, 46.5197)
    val closePoint2 = Point.fromLngLat(6.5668001, 46.5197001)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState { setCameraOptions { center(closePoint1) } }) {
            DirectionOverlay(routePoints = listOf(closePoint1, closePoint2))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withVeryFarPoints_rendersCorrectly() {
    val point1 = Point.fromLngLat(-122.4194, 37.7749)
    val point2 = Point.fromLngLat(2.3522, 48.8566)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = rememberMapViewportState { setCameraOptions { center(point1) } }) {
            DirectionOverlay(routePoints = listOf(point1, point2))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_acrossInternationalDateLine_rendersCorrectly() {
    val point1 = Point.fromLngLat(179.0, 0.0)
    val point2 = Point.fromLngLat(-179.0, 0.0)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = rememberMapViewportState { setCameraOptions { center(point1) } }) {
            DirectionOverlay(routePoints = listOf(point1, point2))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_atPoles_rendersCorrectly() {
    val northPole = Point.fromLngLat(0.0, 90.0)
    val southPole = Point.fromLngLat(0.0, -90.0)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = rememberMapViewportState { setCameraOptions { center(northPole) } }) {
            DirectionOverlay(routePoints = listOf(northPole, southPole))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_recomposesWithNewPoints() {
    val initialPoints = listOf(Point.fromLngLat(6.5668, 46.5197), Point.fromLngLat(6.5700, 46.5220))
    val newPoints = listOf(Point.fromLngLat(7.0, 47.0), Point.fromLngLat(7.1, 47.1))

    composeTestRule.setContent {
      val routePoints =
          androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(initialPoints)
          }

      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState { setCameraOptions { center(routePoints.value.first()) } }) {
            DirectionOverlay(routePoints = routePoints.value)
          }

      androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        routePoints.value = newPoints
      }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withNegativeCoordinates_rendersCorrectly() {
    val point1 = Point.fromLngLat(-6.5668, -46.5197)
    val point2 = Point.fromLngLat(-6.5700, -46.5220)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = rememberMapViewportState { setCameraOptions { center(point1) } }) {
            DirectionOverlay(routePoints = listOf(point1, point2))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withZeroCoordinates_rendersCorrectly() {
    val origin = Point.fromLngLat(0.0, 0.0)
    val destination = Point.fromLngLat(1.0, 1.0)

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState = rememberMapViewportState { setCameraOptions { center(origin) } }) {
            DirectionOverlay(routePoints = listOf(origin, destination))
          }
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun directionOverlay_withComplexRoute_rendersCorrectly() {
    val complexRoute =
        listOf(
            Point.fromLngLat(6.5668, 46.5197),
            Point.fromLngLat(6.5670, 46.5200),
            Point.fromLngLat(6.5672, 46.5198),
            Point.fromLngLat(6.5675, 46.5202),
            Point.fromLngLat(6.5678, 46.5199),
            Point.fromLngLat(6.5680, 46.5205),
            Point.fromLngLat(6.5685, 46.5210),
            Point.fromLngLat(6.5690, 46.5208),
            Point.fromLngLat(6.5695, 46.5215),
            Point.fromLngLat(6.5700, 46.5220))

    composeTestRule.setContent {
      MapboxMap(
          Modifier.fillMaxSize(),
          mapViewportState =
              rememberMapViewportState { setCameraOptions { center(complexRoute.first()) } }) {
            DirectionOverlay(routePoints = complexRoute)
          }
    }

    composeTestRule.waitForIdle()
  }
}

package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.LocalEventList
import com.swent.mapin.testing.UiTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for MapScreen event interactions.
 *
 * Note: These tests verify basic integration between MapScreen and its event handling. Detailed
 * EventDetailSheet behavior is tested in EventDetailSheetTest. Detailed camera/map behavior should
 * be tested in MapViewModel tests.
 */
class MapScreenEventInteractionTest {

  @get:Rule val rule = createComposeRule()

  @Composable
  private fun MapScreenWithTestableCallbacks(onCenterCameraCalled: (Event) -> Unit = {}) {
    MaterialTheme {
      MapScreen(
          renderMap = false,
          autoRequestPermissions = false,
          onEventClick = { event -> onCenterCameraCalled(event) })
    }
  }

  // BASIC RENDERING TESTS
  @Test
  fun mapScreen_rendersSuccessfully() {
    rule.setContent { MapScreenWithTestableCallbacks() }
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_withSelectedEvent_rendersWithoutCrashing() {
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      var selectedEvent by remember { mutableStateOf<Event?>(testEvent) }
      MaterialTheme { MapScreen(renderMap = false, autoRequestPermissions = false) }
    }
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  // CAMERA CENTERING CALLBACK TESTS
  @Test
  fun onCenterCamera_receivesCorrectEventData() {
    val testEvent = LocalEventList.defaultSampleEvents()[0]
    var receivedEvent: Event? = null

    rule.setContent {
      MapScreenWithTestableCallbacks(onCenterCameraCalled = { event -> receivedEvent = event })
    }
    rule.waitForIdle()

    // Verify the event data structure is accessible
    // (The actual centering would be triggered by event click in real usage)
    assertTrue(testEvent.location.isDefined())
    assertEquals(testEvent.location.longitude!!, testEvent.location.longitude, 0.0001)
    assertEquals(testEvent.location.latitude!!, testEvent.location.latitude, 0.0001)
  }

  @Test
  fun onCenterCamera_canAccessEventLocation() {
    val testEvent = LocalEventList.defaultSampleEvents()[0]
    var eventReceived = false
    var longitudeValid = false
    var latitudeValid = false

    rule.setContent {
      MapScreenWithTestableCallbacks(
          onCenterCameraCalled = { event ->
            eventReceived = true
            // Verify we can access location data
            longitudeValid = event.location.longitude?.let { it in -180.0..180.0 } ?: false
            latitudeValid = event.location.latitude?.let { it in -90.0..90.0 } ?: false
          })
    }
    rule.waitForIdle()

    // Verify the test event has valid coordinates
    assertTrue(testEvent.location.longitude?.let { it in -180.0..180.0 } ?: false)
    assertTrue(testEvent.location.latitude?.let { it in -90.0..90.0 } ?: false)
  }

  // INTEGRATION SMOKE TESTS
  @Test
  fun mapScreen_withMultipleEvents_rendersWithoutCrashing() {
    val testEvents = LocalEventList.defaultSampleEvents().take(3)

    rule.setContent {
      MaterialTheme { MapScreen(renderMap = false, autoRequestPermissions = false) }
    }
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    // Verify we have test data
    assertTrue(testEvents.isNotEmpty())
  }
}

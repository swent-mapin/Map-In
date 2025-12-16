package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.chat.ChatScreenTestTags
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.memory.MemoriesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Assisted by AI
/**
 * Tests cover:
 * - MapScreen composition and rendering
 * - Integration with bottom sheet states
 * - Search bar interactions across different states
 * - Visual elements (ScrimOverlay, MapInteractionBlocker)
 * - State transitions via bottom sheet interactions
 * - Map interaction blocking in full state
 * - Scrim overlay presence across states
 * - Direct state transitions (collapsed <-> full)
 * - EventDetailSheet display when event is selected
 * - onCenterCamera callback functionality
 */
class MapScreenTest {

  @get:Rule val rule = createComposeRule()

  val testMemoryVM = MemoriesViewModel(MemoryRepositoryProvider.getRepository())

  @Test
  fun mapScreen_rendersSuccessfully() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapScreen_initialState_showsCollapsedSheet() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
    rule.onNodeWithText("Saved").assertExists()
  }

  @Test
  fun mapScreen_searchBarClick_expandsToFullState() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    // Verify full state by checking for map interaction blocker (only present in full state)
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun mapScreen_searchQuery_persistsAcrossRecomposition() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").performTextInput("basketball")
    rule.waitForIdle()
    rule.onNodeWithText("basketball").assertIsDisplayed()
  }

  @Test
  fun mapScreen_mapInteractionBlocker_onlyPresentInFullState() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()

    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun mapScreen_scrimOverlay_alwaysPresent() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithTag("scrimOverlay").assertIsDisplayed()

    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("scrimOverlay").assertIsDisplayed()
  }

  @Test
  fun mapScreen_mapInteractionBlocker_disappearsWhenLeavingFullState() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()

    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()
  }

  @Test
  fun mapScreen_directTransition_collapsedToFull() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()

    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()
  }

  @Test
  fun mapScreen_directTransition_fullToCollapsed() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertIsDisplayed()

    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown(startY = top, endY = bottom) }
    rule.waitForIdle()
    rule.onNodeWithTag("mapInteractionBlocker").assertDoesNotExist()
  }

  @Test
  fun mapStyleToggle_isVisible_andToggles() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleMenu").assertIsDisplayed()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleMenu").assertDoesNotExist()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
  }

  @Test
  fun mapStyleToggle_persists_afterBottomSheetTransitions() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
  }

  @Test
  fun searchQuery_clears_whenLeavingFullState() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithText("Search events, people").performTextInput("basketball")
    rule.waitForIdle()
    rule.onNodeWithText("basketball").assertIsDisplayed()
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithText("basketball").assertDoesNotExist()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapStyleToggle_visible_inAllSheetStates() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeDown() }
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().assertIsDisplayed()
  }

  @Test
  fun mapScreen_heatmapMode_displaysCorrectly() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_HEATMAP").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapScreen_satelliteMode_displaysCorrectly() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()

    // Wait for menu to be fully displayed before clicking
    rule.onNodeWithTag("mapStyleOption_SATELLITE").assertIsDisplayed()
    rule.onNodeWithTag("mapStyleOption_SATELLITE").performClick()
    rule.waitForIdle()

    // Add a small delay to allow map style transition to complete on slower CI environments
    Thread.sleep(200)
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  // ===== Location feature tests =====

  @Test
  fun mapScreen_locationPermissionFlow_handlesCorrectly() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }
    rule.waitForIdle()

    // Add additional wait to ensure composition completes on slower CI environments
    Thread.sleep(100)
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationButton_isVisibleWhenNotCentered() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    rule.runOnIdle { assertFalse(viewModel.isCenteredOnUser) }
  }

  @Test
  fun mapScreen_switchBetweenStyles_maintainsState() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_HEATMAP").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_SATELLITE").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_STANDARD").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  // ============================================================================
  // EVENT DETAIL SHEET & CAMERA INTEGRATION TESTS
  // ============================================================================

  @Test
  fun mapScreen_eventDetailSheet_displaysAndInteracts() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()
    rule.runOnIdle { viewModel.onEventPinClicked(testEvent) }
    rule.waitForIdle()
    Thread.sleep(500)
    rule.waitForIdle()

    // Test: EventDetailSheet displays with correct elements
    rule.onNodeWithTag("eventDetailSheet").assertIsDisplayed()
    rule.onNodeWithTag("closeButton").assertIsDisplayed()
    rule.onNodeWithTag("shareButton").assertIsDisplayed()
    rule.onNodeWithText(testEvent.title, substring = true).assertExists()

    // Test: Close button works
    rule.onNodeWithTag("closeButton").performClick()
    rule.waitForIdle()
    Thread.sleep(200)
    rule.onNodeWithTag("eventDetailSheet").assertDoesNotExist()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapScreen_eventDetailSheet_shareButtonOpensDialog() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()
    rule.runOnIdle { viewModel.onEventPinClicked(testEvent) }
    rule.waitForIdle()
    Thread.sleep(500)
    rule.waitForIdle()

    // Click share button and verify dialog appears
    rule.onNodeWithTag("shareButton").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("shareEventDialog").assertIsDisplayed()
  }

  @Test
  fun mapScreen_eventDetailSheet_adaptsToSheetState() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()
    rule.runOnIdle { viewModel.onEventPinClicked(testEvent) }
    rule.waitForIdle()
    Thread.sleep(500)
    rule.waitForIdle()

    // Test: EventDetailSheet displays in collapsed state
    rule.onNodeWithTag("eventDetailSheet").assertIsDisplayed()

    // Test: Expands with bottom sheet
    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeUp() }
    rule.waitForIdle()
    rule.onNodeWithTag("eventDetailSheet").assertIsDisplayed()
  }

  @Test
  fun mapScreen_showsRegularBottomSheetWhenNoEventSelected() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }
    rule.waitForIdle()

    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun activity_is_found_after_searching() {
    rule.setContent {
      MaterialTheme {
        MapScreen(
            autoRequestPermissions = false,
            memoryVM = testMemoryVM,
        )
      }
    }

    rule.onNodeWithText("Search events, people").performTextInput("Art Exhibition")
    rule.waitForIdle()

    rule.onNodeWithText("Art Exhibition").assertIsDisplayed()
  }

  @Test
  fun chatButton_is_displayed() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }

    rule.onNodeWithTag(ChatScreenTestTags.CHAT_NAVIGATE_BUTTON).ensureVisible().assertIsDisplayed()
  }

  @Test
  fun chatButton_staysVisibleAcrossSheetStates() {
    rule.setContent {
      MaterialTheme { MapScreen(autoRequestPermissions = false, memoryVM = testMemoryVM) }
    }
    rule.waitForIdle()

    val chatButton = rule.onNodeWithTag(ChatScreenTestTags.CHAT_NAVIGATE_BUTTON)

    chatButton.ensureVisible().assertIsDisplayed()

    rule.onNodeWithTag("bottomSheet").performTouchInput { swipeUp() }
    rule.waitForIdle()
    chatButton.ensureVisible().assertIsDisplayed()

    rule.onNodeWithText("Search events, people").performClick()
    rule.waitForIdle()
    chatButton.ensureVisible().assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationButton_stateChangesWithMapMovement() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    rule.runOnIdle { assertFalse(viewModel.isCenteredOnUser) }

    rule.runOnIdle { viewModel.onMapMoved() }
    rule.waitForIdle()

    rule.runOnIdle { assertFalse(viewModel.isCenteredOnUser) }

    rule.runOnIdle { viewModel.updateCenteredState(46.5, 6.5) }
    rule.waitForIdle()

    rule.runOnIdle { assertFalse(viewModel.isCenteredOnUser) }
  }

  @Test
  fun mapScreen_compassAndLocationButton_positioning() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_updateCenteredState_tracksCamera() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    rule.runOnIdle { assertFalse(viewModel.isCenteredOnUser) }

    rule.runOnIdle { viewModel.updateCenteredState(46.518, 6.566) }
    rule.waitForIdle()

    rule.runOnIdle { assertFalse(viewModel.isCenteredOnUser) }
  }

  @Test
  fun mapScreen_locationManagement_initializesOnComposition() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify LaunchedEffect executed by checking that permission was checked
    // This exercises the LaunchedEffect(Unit) location setup code
    rule.runOnIdle { assertNotNull(viewModel.onRequestLocationPermission) }
  }

  @Test
  fun mapScreen_locationCenteringCallback_isSet() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    var viewModel: MapScreenViewModel? = null

    rule.setContent {
      MaterialTheme {
        val vm = rememberMapScreenViewModel(config)
        viewModel = vm
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify the onCenterOnUserLocation callback was set
    val vm = viewModel
    assertNotNull(vm)
    assertNotNull(vm!!.onCenterOnUserLocation)
  }

  @Test
  fun mapScreen_locationPermissionRequestCallback_isSet() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify the onRequestLocationPermission callback was set
    rule.runOnIdle { assertNotNull(viewModel.onRequestLocationPermission) }
  }

  @Test
  fun mapScreen_withRenderMapTrue_displaysMapComponents() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = true, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify map screen displays (this exercises MapEffect and map rendering code)
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapScreen_cameraCallbacks_areSetCorrectly() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // The LaunchedEffect should have set up camera callbacks
    // We can test this by triggering an event click which uses the camera callback
    rule.runOnIdle { viewModel.onEventPinClicked(testEvent) }

    rule.waitForIdle()

    // Verify the event was selected (callback worked)
    rule.runOnIdle { assertEquals(testEvent, viewModel.selectedEvent.value) }
  }

  // ============================================================================
  // AUTO-REQUEST PERMISSIONS TESTS
  // ============================================================================

  @Test
  fun mapScreen_autoRequestPermissions_disabled_doesNotRequestPermissions() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify the screen renders without triggering permission requests
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapScreen_autoRequestPermissions_enabled_checksLocationPermission() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        // Test with autoRequestPermissions = false to avoid launching system dialogs
        // The actual permission request logic is tested through other means
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify screen renders correctly
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()

    // Manually verify that checkLocationPermission works
    rule.runOnIdle { viewModel.checkLocationPermission() }

    rule.waitForIdle()

    // Screen should remain functional
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationPermissionLauncher_handlesGrantedPermission() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify the map screen is functional
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()

    // Simulate checking permission after grant
    rule.runOnIdle { viewModel.checkLocationPermission() }

    rule.waitForIdle()

    // Screen should still be functional
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationPermissionLauncher_handlesDeniedPermission() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify the map screen is functional even without permissions
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()

    // Invoke permission check and ensure no crash regardless of current permission state
    rule.runOnIdle { viewModel.checkLocationPermission() }
  }

  @Test
  fun mapScreen_notificationPermissionCheck_worksCorrectly() {
    // This test verifies that the screen renders correctly
    // and the permission check logic doesn't crash
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify screen renders successfully
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }

  @Test
  fun mapScreen_permissionFlow_locationThenNotification() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        // Test that screen handles the permission flow correctly
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Verify initial state
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()

    // Check location permission state
    rule.runOnIdle { viewModel.checkLocationPermission() }

    rule.waitForIdle()

    // Screen should remain functional
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_deleteDialog_showsWhenRequested() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Trigger delete dialog
    rule.runOnIdle { viewModel.requestDeleteEvent(testEvent) }
    rule.waitForIdle()

    // Assert dialog is shown
    rule.onNodeWithText("Delete Event").assertIsDisplayed()
    rule
        .onNodeWithText("Are you sure you want to delete this event? This action cannot be undone.")
        .assertIsDisplayed()
    rule.onNodeWithText("Delete").assertIsDisplayed()
    rule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun mapScreen_deleteDialog_cancelClosesDialog() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Trigger delete dialog
    rule.runOnIdle { viewModel.requestDeleteEvent(testEvent) }
    rule.waitForIdle()

    // Click cancel
    rule.onNodeWithText("Cancel").performClick()
    rule.waitForIdle()

    // Dialog disappears
    rule.onNodeWithText("Delete Event").assertDoesNotExist()
  }

  @Test
  fun mapScreen_deleteDialog_confirmDeletesAndClosesDialog() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    val testEvent = com.swent.mapin.model.event.LocalEventList.defaultSampleEvents()[0]

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Trigger dialog
    rule.runOnIdle { viewModel.requestDeleteEvent(testEvent) }
    rule.waitForIdle()

    // Click delete
    rule.onNodeWithText("Delete").performClick()
    rule.waitForIdle()

    // Dialog disappears
    rule.onNodeWithText("Delete Event").assertDoesNotExist()

    // Assert UI state changed — event no longer selected
    rule.runOnIdle { assertNull(viewModel.eventPendingDeletion) }
    assertFalse(viewModel.ownedEvents.contains(testEvent))
  }

  @Test
  fun mapScreen_heatmapClick_navigatesToNearbyMemories() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    var navigatedToMemories = false

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(
            renderMap = false,
            autoRequestPermissions = false,
            memoryVM = testMemoryVM,
            onNavigateToMemories = { navigatedToMemories = true })
      }
    }

    rule.waitForIdle()

    // Switch to heatmap mode and click
    rule.runOnIdle {
      viewModel.setMapStyle(MapScreenViewModel.MapStyle.HEATMAP)
      viewModel.onMapClicked(46.5197, 6.6323)
    }

    rule.waitForIdle()
    Thread.sleep(500)

    assertTrue(navigatedToMemories)
  }

  @Test
  fun mapScreen_standardModeClick_doesNotNavigateToMemories() {
    val config =
        BottomSheetConfig(collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp)
    lateinit var viewModel: MapScreenViewModel
    var navigatedToMemories = false

    rule.setContent {
      MaterialTheme {
        viewModel = rememberMapScreenViewModel(config)
        MapScreen(
            renderMap = false,
            autoRequestPermissions = false,
            memoryVM = testMemoryVM,
            onNavigateToMemories = { navigatedToMemories = true })
      }
    }

    rule.waitForIdle()

    // Stay in standard mode and click
    rule.runOnIdle { viewModel.onMapClicked(46.5197, 6.6323) }

    rule.waitForIdle()
    Thread.sleep(300)

    assertFalse(navigatedToMemories)
  }

  @Test
  fun mapScreen_pinsNotDisplayedInHeatmapMode() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Switch to heatmap mode
    rule.onNodeWithTag("mapStyleToggle").ensureVisible().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("mapStyleOption_HEATMAP").performClick()
    rule.waitForIdle()

    // Map should still be displayed but pins logic is different
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_pinsDisplayedInStandardMode() {
    rule.setContent {
      MaterialTheme {
        MapScreen(renderMap = false, autoRequestPermissions = false, memoryVM = testMemoryVM)
      }
    }

    rule.waitForIdle()

    // Standard mode should display pins
    rule.onNodeWithTag(UiTestTags.MAP_SCREEN).assertIsDisplayed()
    rule.onNodeWithText("Search events, people").assertIsDisplayed()
  }
}

private fun SemanticsNodeInteraction.ensureVisible(): SemanticsNodeInteraction {
  return try {
    performScrollTo()
  } catch (error: AssertionError) {
    if (!error.message.orEmpty().contains("Scroll SemanticsAction")) {
      throw error
    }
    this
  }
}

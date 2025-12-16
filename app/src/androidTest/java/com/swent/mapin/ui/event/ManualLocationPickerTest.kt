package com.swent.mapin.ui.event

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mapbox.geojson.Point
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.location.LocationRepository
import com.swent.mapin.model.location.LocationViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

/** Instrumentation tests for ManualLocationPickerDialog UI */
class ManualLocationPickerTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val mockLocationRepo = mockk<LocationRepository>(relaxed = true)

  @Test
  fun manualLocationPickerDialog_displays_all_components() {
    var dismissed = false
    var pickedLocation: Location? = null
    val testRecenterPoint = Point.fromLngLat(6.566, 46.519)

    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = { dismissed = true },
          onLocationPicked = { pickedLocation = it },
          searchResults = emptyList(),
          onSearchQuery = {},
          onSearchResultSelect = {},
          recenterPoint = testRecenterPoint)
    }

    // Verify all main components are displayed
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.DIALOG).assertExists()
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.SEARCH_FIELD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.MY_LOCATION_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.MAP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.CANCEL_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.USE_LOCATION_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun manualLocationPickerDialog_cancel_button_dismisses_dialog() {
    var dismissed = false

    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = { dismissed = true },
          onLocationPicked = {},
          searchResults = emptyList(),
          onSearchQuery = {},
          onSearchResultSelect = {})
    }

    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.CANCEL_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assert(dismissed) { "Dialog should be dismissed after clicking cancel" }
  }

  @Test
  fun manualLocationPickerDialog_search_field_accepts_input() {
    var searchQueryReceived = ""

    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = emptyList(),
          onSearchQuery = { searchQueryReceived = it },
          onSearchResultSelect = {})
    }

    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.SEARCH_FIELD)
        .performTextInput("Lausanne")
    composeTestRule.waitForIdle()

    assert(searchQueryReceived == "Lausanne") {
      "Search query should be passed to callback. Expected: 'Lausanne', got: '$searchQueryReceived'"
    }
  }

  @Test
  fun manualLocationPickerDialog_my_location_button_enabled_when_recenterPoint_provided() {
    val testRecenterPoint = Point.fromLngLat(6.566, 46.519)

    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = emptyList(),
          onSearchQuery = {},
          onSearchResultSelect = {},
          recenterPoint = testRecenterPoint)
    }

    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.MY_LOCATION_BUTTON).assertIsEnabled()
  }

  @Test
  fun manualLocationPickerDialog_my_location_button_disabled_when_no_recenterPoint() {
    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = emptyList(),
          onSearchQuery = {},
          onSearchResultSelect = {},
          recenterPoint = null)
    }

    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.MY_LOCATION_BUTTON)
        .assertIsNotEnabled()
  }

  @Test
  fun manualLocationPickerDialog_use_location_button_disabled_initially() {
    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = emptyList(),
          onSearchQuery = {},
          onSearchResultSelect = {})
    }

    // Initially no point is picked, so button should be disabled
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.USE_LOCATION_BUTTON)
        .assertIsNotEnabled()
  }

  @Test
  fun manualLocationPickerDialog_use_location_button_enabled_when_initialLocation_provided() {
    val testLocation = Location.from("Test", 46.519, 6.566)

    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = testLocation,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = emptyList(),
          onSearchQuery = {},
          onSearchResultSelect = {})
    }

    // When initialLocation is provided, a point is picked, so button should be enabled
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.USE_LOCATION_BUTTON)
        .assertIsEnabled()
  }

  @Test
  fun manualLocationPickerDialog_search_results_display_correctly() {
    val searchResults =
        listOf(Location.from("Lausanne", 46.5197, 6.6323), Location.from("EPFL", 46.5197, 6.5665))

    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = searchResults,
          onSearchQuery = {},
          onSearchResultSelect = {})
    }

    // Type in search field to trigger showing results
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.SEARCH_FIELD).performTextInput("L")
    composeTestRule.waitForIdle()

    // Verify search results are displayed
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.SEARCH_RESULT_PREFIX + 0)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.SEARCH_RESULT_PREFIX + 1)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Lausanne").assertIsDisplayed()
    composeTestRule.onNodeWithText("EPFL").assertIsDisplayed()
  }

  @Test
  fun manualLocationPickerDialog_clicking_search_result_triggers_callback() {
    val searchResults = listOf(Location.from("Lausanne", 46.5197, 6.6323))
    var selectedLocation: Location? = null

    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = searchResults,
          onSearchQuery = {},
          onSearchResultSelect = { selectedLocation = it })
    }

    // Type in search field to show results
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.SEARCH_FIELD).performTextInput("L")
    composeTestRule.waitForIdle()

    // Click the first search result
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.SEARCH_RESULT_PREFIX + 0)
        .performClick()
    composeTestRule.waitForIdle()

    assert(selectedLocation?.name == "Lausanne") {
      "Selected location should be Lausanne, got: ${selectedLocation?.name}"
    }
  }

  @Test
  fun manualLocationPickerDialog_shows_map() {
    composeTestRule.setContent {
      ManualLocationPickerDialog(
          initialLocation = null,
          onDismiss = {},
          onLocationPicked = {},
          searchResults = emptyList(),
          onSearchQuery = {},
          onSearchResultSelect = {})
    }

    // Map should be visible
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.MAP).assertIsDisplayed()
  }

  @Test
  fun addEventScreen_pick_location_on_map_button_opens_dialog() {
    val eventViewModel = mockk<EventViewModel>(relaxed = true)
    val locationViewModel = LocationViewModel(mockLocationRepo)

    composeTestRule.setContent {
      AddEventScreen(eventViewModel = eventViewModel, locationViewModel = locationViewModel)
    }

    // Find and click the "Pick location on map" button
    composeTestRule.onNodeWithText("Pick location on map").performClick()
    composeTestRule.waitForIdle()

    // Dialog should be displayed
    composeTestRule
        .onNodeWithTag(ManualLocationPickerTestTags.DIALOG)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun addEventScreen_dialog_dismisses_on_cancel() {
    val eventViewModel = mockk<EventViewModel>(relaxed = true)
    val locationViewModel = LocationViewModel(mockLocationRepo)

    composeTestRule.setContent {
      AddEventScreen(eventViewModel = eventViewModel, locationViewModel = locationViewModel)
    }

    // Open dialog
    composeTestRule.onNodeWithText("Pick location on map").performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is displayed
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.DIALOG).assertIsDisplayed()

    // Click cancel
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.CANCEL_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed
    composeTestRule.onNodeWithTag(ManualLocationPickerTestTags.DIALOG).assertDoesNotExist()
  }
}

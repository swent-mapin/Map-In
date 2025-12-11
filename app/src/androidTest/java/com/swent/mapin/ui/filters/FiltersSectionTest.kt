package com.swent.mapin.ui.filters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.location.LocationRepository
import com.swent.mapin.model.location.LocationViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FiltersSectionTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: FiltersSectionViewModel
  private lateinit var locationViewModel: LocationViewModel
  private lateinit var userProfile: UserProfile

  @Before
  fun setUp() {
    // Fake repository that immediately returns predictable results
    class FakeLocationRepository : LocationRepository {
      override suspend fun forwardGeocode(query: String): List<Location> {
        return listOf(
            Location.from("Lausanne", 0.0, 0.0),
            Location.from("Geneva", 0.0, 0.0),
            Location.from("Bern", 0.0, 0.0))
      }

      override suspend fun reverseGeocode(lat: Double, lon: Double): Location {
        // Not needed for this test
        return Location.UNDEFINED
      }
    }

    viewModel = FiltersSectionViewModel()
    locationViewModel = LocationViewModel(repository = FakeLocationRepository())
    userProfile = UserProfile()

    composeTestRule.setContent {
      FiltersSection()
          .Render(
              filterViewModel = viewModel,
              locationViewModel = locationViewModel,
              userProfile = userProfile)
    }
  }

  // ---------------------- TIME SECTION ----------------------
  @Test
  fun timeSection_toggleExpandsAndCollapses() {
    val toggle = composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_DATE)

    toggle.performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.DATE_PICKER_BUTTON).assertIsDisplayed()

    toggle.performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.DATE_PICKER_BUTTON).assertDoesNotExist()
  }

  @Test
  fun datePicker_showsTodayByDefault() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_DATE).performClick()

    val today =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

    composeTestRule
        .onNodeWithTag(FiltersSectionTestTags.FROM_DATE_TEXT, useUnmergedTree = true)
        .assert(hasText("From: $today"))

    composeTestRule.runOnIdle {
      assertTrue(viewModel.isWhenChecked.value)
      assertEquals(
          Calendar.getInstance().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate(),
          viewModel.filters.value.startDate)
    }
  }

  // ---------------------- PLACE SECTION ----------------------

  @Test
  fun placeSection_switchToUser_setsUserLocation() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_LOCATION).performClick()

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.AROUND_USER).performClick()

    composeTestRule.runOnIdle { assertEquals("user", viewModel.filters.value.place?.name) }
  }

  @Test
  fun radiusInput_acceptsOnlyDigits() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_LOCATION).performClick()

    val radiusField = composeTestRule.onNodeWithTag(FiltersSectionTestTags.RADIUS_INPUT)

    // Clear any existing text first
    radiusField.performTextReplacement("")

    // Enter mixed input
    radiusField.performTextInput("abc50xyz")
    radiusField.assert(hasText("50"))

    composeTestRule.runOnIdle { assertEquals(50, viewModel.filters.value.radiusKm) }
  }

  @Test
  fun searchPlaceInput_updatesQuery() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_LOCATION).performClick()

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.AROUND_SEARCH).performClick()

    val searchField = composeTestRule.onNodeWithTag(FiltersSectionTestTags.SEARCH_PLACE_INPUT)
    searchField.performTextInput("Lausanne")
    searchField.assert(hasText("Lausanne"))
  }

  @Test
  fun searchResultsDropdown_displaysResultsAndSelectsLocation() {
    // Open the Place section and switch to Search mode
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_LOCATION).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.AROUND_SEARCH).performClick()

    // Type a query — this will trigger the fake repo to return results
    val searchField = composeTestRule.onNodeWithTag(FiltersSectionTestTags.SEARCH_PLACE_INPUT)
    searchField.performTextInput("Lau")

    // Wait for debounce and recomposition
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      locationViewModel.locations.value.isNotEmpty()
    }

    // The dropdown should appear with the results and footer text
    composeTestRule.onNodeWithText("Lausanne").assertIsDisplayed()

    // Click one of the results
    composeTestRule.onNodeWithText("Lausanne").performClick()

    // The dropdown should collapse and the selected name should appear in the input
    searchField.assert(hasText("Lausanne"))
  }

  // ---------------------- PRICE SECTION ----------------------
  @Test
  fun priceSection_updatesMaxPrice_whenSliderMoves() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performClick()

    val sliderNode =
        composeTestRule.onNodeWithTag(FiltersSectionTestTags.PRICE_INPUT).fetchSemanticsNode()

    val bounds = sliderNode.boundsInRoot

    val localX = bounds.width * 0.8f
    val localY = bounds.height / 2f

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.PRICE_INPUT).performTouchInput {
      click(Offset(localX, localY))
    }

    // Read actual value from the ViewModel after slider event
    val actualValue = composeTestRule.runOnIdle { viewModel.filters.value.maxPrice }

    // Assert UI displays the right CHF value
    composeTestRule.onNodeWithText("$actualValue CHF").assertExists()

    // Sanity check: value should be in the upper range (slider was clicked ~80%)
    assert(actualValue in 70..100)
  }

  // ---------------------- FRIENDS / POPULAR ----------------------
  @Test
  fun friendsOnly_togglesCorrectly() {
    val toggle = composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_FRIENDS)
    toggle.performClick()
    composeTestRule.runOnIdle { assertTrue(viewModel.filters.value.friendsOnly) }
    toggle.performClick()
    composeTestRule.runOnIdle { assertFalse(viewModel.filters.value.friendsOnly) }
  }

  @Test
  fun popularOnly_togglesCorrectly() {
    val toggle = composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_POPULAR)
    toggle.performClick()
    composeTestRule.runOnIdle { assertTrue(viewModel.filters.value.popularOnly) }
  }

  // ---------------------- INITIAL STATE ----------------------
  @Test
  fun allSections_areInitiallyCollapsed() {
    listOf(
            FiltersSectionTestTags.DATE_PICKER_BUTTON,
            FiltersSectionTestTags.RADIUS_INPUT,
            FiltersSectionTestTags.PRICE_INPUT,
            FiltersSectionTestTags.tag("Music"))
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertDoesNotExist() }
  }

  // ---------------------- RESET ----------------------
  @Test
  fun reset_clearsAllFiltersAndResetsToDefaults() {
    // Enable some filters
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_DATE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TAGS).performClick()

    val sportTag = composeTestRule.onNodeWithTag(FiltersSectionTestTags.tag("Sport"))
    sportTag.assertIsDisplayed()
    sportTag.performClick()

    // Set radius manually
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_LOCATION).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.RADIUS_INPUT).performTextReplacement("")
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.RADIUS_INPUT).performTextInput("25")

    composeTestRule.runOnIdle {
      assertTrue(viewModel.isWhenChecked.value)
      assertTrue(viewModel.isPriceChecked.value)
      assertTrue(viewModel.isTagsChecked.value)
      assertEquals(25, viewModel.filters.value.radiusKm)
      assertTrue("Sport" in viewModel.filters.value.tags)
    }

    // Reset
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.RESET_BUTTON).performClick()

    composeTestRule.runOnIdle {
      val today = LocalDate.now()
      assertFalse(viewModel.isWhenChecked.value)
      assertFalse(viewModel.isPriceChecked.value)
      assertFalse(viewModel.isTagsChecked.value)
      assertTrue(viewModel.filters.value.tags.isEmpty())

      // Defaults
      assertEquals(today, viewModel.filters.value.startDate)
      assertNull(viewModel.filters.value.endDate)
      assertNull(viewModel.filters.value.maxPrice)
      assertEquals(10, viewModel.filters.value.radiusKm)
      assertFalse(viewModel.filters.value.friendsOnly)
      assertFalse(viewModel.filters.value.popularOnly)

      assertTrue(viewModel.filters.value.isEmpty())
    }
  }

  @Test
  fun testErrorHandlingAndEndDate() {
    // parseDate() failure with blank input
    viewModel.setStartDate("")
    assertEquals("Date input cannot be blank", viewModel.errorMessage.value)

    // Clear error
    viewModel.clearError()
    assertNull(viewModel.errorMessage.value)

    // parseDate() failure with invalid format
    viewModel.setStartDate("invalid")
    assertEquals("Invalid date format: invalid", viewModel.errorMessage.value)

    // Clear error again
    viewModel.clearError()
    assertNull(viewModel.errorMessage.value)

    // setEndDate() before start date triggers error
    viewModel.setStartDate("01/11/2025") // valid start
    viewModel.setEndDate("31/10/2025") // end < start
    assertEquals("End date must be on or after start date", viewModel.errorMessage.value)
  }
}

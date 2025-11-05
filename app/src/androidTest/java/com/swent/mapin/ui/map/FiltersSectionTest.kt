package com.swent.mapin.ui.map

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.UserProfile
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
    viewModel = FiltersSectionViewModel()
    locationViewModel = LocationViewModel()
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
    val toggle = composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TIME)
    val expand = composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_TIME)

    toggle.performClick()
    expand.assertExists()
    expand.performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.DATE_PICKER_BUTTON).assertIsDisplayed()

    expand.performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.DATE_PICKER_BUTTON).assertDoesNotExist()
  }

  @Test
  fun datePicker_showsTodayByDefault() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TIME).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_TIME).performClick()

    val today =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    composeTestRule
        .onNodeWithTag(FiltersSectionTestTags.FROM_DATE_TEXT)
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
  fun placeSection_switchToMap_showsMapPicker() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PLACE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PLACE).performClick()

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.AROUND_MAP).performClick()
    composeTestRule.onNodeWithText("Pick on map to implement").assertIsDisplayed()
  }

  @Test
  fun placeSection_switchToUser_setsUserLocation() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PLACE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PLACE).performClick()

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.AROUND_USER).performClick()

    composeTestRule.runOnIdle { assertEquals("user", viewModel.filters.value.place?.name) }
  }

  @Test
  fun radiusInput_acceptsOnlyDigits() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PLACE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PLACE).performClick()

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
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PLACE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PLACE).performClick()

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.AROUND_SEARCH).performClick()

    val searchField = composeTestRule.onNodeWithTag(FiltersSectionTestTags.SEARCH_PLACE_INPUT)
    searchField.performTextInput("Lausanne")
    searchField.assert(hasText("Lausanne"))
  }

  // ---------------------- PRICE SECTION ----------------------
  @Test
  fun priceSection_filtersDigitsOnly() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PRICE).performClick()

    val priceField = composeTestRule.onNodeWithTag(FiltersSectionTestTags.PRICE_INPUT)
    priceField.performTextReplacement("")
    priceField.performTextInput("abc999xyz")
    priceField.assert(hasText("999"))

    composeTestRule.runOnIdle { assertEquals(999, viewModel.filters.value.maxPrice) }
  }

  // ---------------------- TAGS SECTION ----------------------
  @Test
  fun tagsSection_selectsAndDeselectsTag() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TAGS).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_TAGS).performClick()

    val musicTag = composeTestRule.onNodeWithTag(FiltersSectionTestTags.tag("Music"))
    musicTag.performClick()
    composeTestRule.runOnIdle { assertTrue("Music" in viewModel.filters.value.tags) }

    musicTag.performClick()
    composeTestRule.runOnIdle { assertFalse("Music" in viewModel.filters.value.tags) }
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
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TIME).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TAGS).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_TAGS).performClick()

    val sportTag = composeTestRule.onNodeWithTag(FiltersSectionTestTags.tag("Sport"))
    sportTag.assertIsDisplayed()
    sportTag.performClick()

    // Set radius manually
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PLACE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PLACE).performClick()
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

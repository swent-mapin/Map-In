package com.swent.mapin.ui.map

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Assisted by AI tools

/**
 * UI tests for the FiltersSection composable. Uses Compose UI testing with test tags to verify:
 * - Reset functionality
 * - Toggle expand/collapse
 * - Date picker default value
 * - Location mode switching
 * - Input filtering (digits only)
 * - Tag selection
 * - Friends/Popular toggles
 * - Global reset clears all filters
 */
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

  @Test
  fun resetButton_callsReset() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.RESET_BUTTON).performClick()
    composeTestRule.runOnIdle {
      assertFalse(viewModel.isWhenChecked.value)
      assertNull(viewModel.pickedLocation.value)
    }
  }

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
  }

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

    composeTestRule.runOnIdle { assertEquals("user", viewModel.pickedLocation.value?.name) }
  }

  @Test
  fun radiusInput_acceptsOnlyDigits() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PLACE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PLACE).performClick()

    val radiusField = composeTestRule.onNodeWithTag(FiltersSectionTestTags.RADIUS_INPUT)
    radiusField.performTextInput("abc50xyz")
    radiusField.assert(hasText("50"))
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

  @Test
  fun priceSection_filtersDigitsOnly() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_PRICE).performClick()

    val priceField = composeTestRule.onNodeWithTag(FiltersSectionTestTags.PRICE_INPUT)
    priceField.performTextInput("abc999xyz")
    priceField.assert(hasText("999"))
  }

  @Test
  fun tagsSection_selectsAndDeselectsTag() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TAGS).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_TAGS).performClick()

    val musicTag = composeTestRule.onNodeWithTag(FiltersSectionTestTags.tag("Music"))
    musicTag.performClick()
    composeTestRule.runOnIdle { assertTrue("Music" in viewModel.selectedTags) }

    musicTag.performClick()
    composeTestRule.runOnIdle { assertFalse("Music" in viewModel.selectedTags) }
  }

  @Test
  fun friendsOnly_togglesCorrectly() {
    val toggle = composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_FRIENDS)
    toggle.performClick()
    composeTestRule.runOnIdle { assertTrue(viewModel.friendsOnly.value) }
    toggle.performClick()
    composeTestRule.runOnIdle { assertFalse(viewModel.friendsOnly.value) }
  }

  @Test
  fun popularOnly_togglesCorrectly() {
    val toggle = composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_POPULAR)
    toggle.performClick()
    composeTestRule.runOnIdle { assertTrue(viewModel.popularOnly.value) }
  }

  @Test
  fun allSections_areInitiallyCollapsed() {
    listOf(
            FiltersSectionTestTags.DATE_PICKER_BUTTON,
            FiltersSectionTestTags.RADIUS_INPUT,
            FiltersSectionTestTags.PRICE_INPUT,
            FiltersSectionTestTags.tag("Music"))
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertDoesNotExist() }
  }

  @Test
  fun reset_clearsAllFilters() {
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TIME).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_PRICE).performClick()

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.TOGGLE_TAGS).performClick()
    composeTestRule.onNodeWithTag(FiltersSectionTestTags.EXPAND_TAGS).performClick()

    val sportTag = composeTestRule.onNodeWithTag(FiltersSectionTestTags.tag("Sport"))
    sportTag.assertIsDisplayed()
    sportTag.performClick()

    composeTestRule.runOnIdle { assertTrue("Sport" in viewModel.selectedTags) }

    composeTestRule.onNodeWithTag(FiltersSectionTestTags.RESET_BUTTON).performClick()

    composeTestRule.runOnIdle {
      assertFalse(viewModel.isWhenChecked.value)
      assertFalse(viewModel.isPriceChecked.value)
      assertFalse(viewModel.tagsEnabled.value)
      assertTrue(viewModel.selectedTags.isEmpty())
    }
  }
}

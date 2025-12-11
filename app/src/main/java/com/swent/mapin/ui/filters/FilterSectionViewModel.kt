package com.swent.mapin.ui.filters

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.swent.mapin.model.location.Location
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for managing event filter state and UI interactions.
 *
 * This ViewModel manages the state of various event filters including:
 * - When: Date range filtering (start and end dates)
 * - Where: Location-based filtering with radius
 * - Price: Maximum price filtering
 * - Tags: Event category filtering
 * - Additional: Friends-only and popular-only filters
 *
 * **Date Format**: Accepts dates in dd/MM/yyyy format (day first). Alternative formats MM/dd/yyyy
 * and yyyy-MM-dd are also supported but may cause ambiguity. Validation errors are exposed through
 * [errorMessage].
 *
 * **Section Toggling**: Disabling a filter section automatically resets its values to defaults.
 * Enabling a section (by setting filter values) automatically toggles it on.
 *
 * **Error Propagation**: All validation errors (invalid dates, date range violations) are exposed
 * through the [errorMessage] StateFlow.
 */
class FiltersSectionViewModel : ViewModel() {
  // Single Filters state
  private var _filters = MutableStateFlow(Filters())
  /** Current filter configuration. Emits updates when any filter changes. */
  val filters: StateFlow<Filters> = _filters

  private val _errorMessage = MutableStateFlow<String?>(null)
  /** Error message from validation failures (e.g., invalid date format). */
  val errorMessage: StateFlow<String?> = _errorMessage

  // SECTION TOGGLES
  /** Whether the "When" (date) filter section is enabled. */
  var isWhenChecked = mutableStateOf(false)
    private set

  /** Whether the "Where" (location) filter section is enabled. */
  var isWhereChecked = mutableStateOf(false)
    private set

  /** Whether the price filter section is enabled. */
  var isPriceChecked = mutableStateOf(false)
    private set

  /** Whether the tags filter section is enabled. */
  var isTagsChecked = mutableStateOf(false)
    private set

  /**
   * Internal helper to update filter state and toggle visibility.
   *
   * **Side Effect**: When unchecked, resets the associated filter values to defaults.
   */
  private fun updateFiltersAndToggle(
      checked: Boolean,
      toggle: MutableState<Boolean>,
      resetBlock: Filters.() -> Filters
  ) {
    toggle.value = checked
    if (!checked) {
      _filters.value = _filters.value.resetBlock()
    }
  }

  /**
   * Toggles the "When" filter section.
   *
   * **Side Effect**: When disabled, resets start date to today and clears end date.
   */
  fun setWhenChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isWhenChecked) {
      copy(startDate = LocalDate.now(), endDate = null)
    }
  }

  /**
   * Toggles the "Where" filter section.
   *
   * **Side Effect**: When disabled, clears location and resets radius to 10 km.
   */
  fun setWhereChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isWhereChecked) {
      copy(place = Location.UNDEFINED, radiusKm = 10)
    }
  }

  /**
   * Toggles the price filter section.
   *
   * **Side Effect**: When disabled, clears the maximum price constraint.
   */
  fun setPriceChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isPriceChecked) { copy(maxPrice = null) }
  }

  /**
   * Toggles the tags filter section.
   *
   * **Side Effect**: When disabled, clears all selected tags.
   */
  fun setIsTagsChecked(enabled: Boolean) {
    updateFiltersAndToggle(enabled, isTagsChecked) { copy(tags = emptySet()) }
  }

  // TIME FILTER
  /**
   * Sets the start date for event filtering.
   *
   * **Format**: Preferred format is dd/MM/yyyy (e.g., 25/12/2024 for Dec 25, 2024). Also accepts
   * MM/dd/yyyy and yyyy-MM-dd, but these may be ambiguous.
   *
   * **Side Effect**: Auto-enables the "When" section on successful parse. Validation errors are
   * exposed via [errorMessage].
   *
   * @param date Date string in dd/MM/yyyy, MM/dd/yyyy, or yyyy-MM-dd format
   */
  fun setStartDate(date: String) {
    when (val result = parseDate(date)) {
      is ParseResult.Success -> {
        _filters.value = _filters.value.copy(startDate = result.date)
        isWhenChecked.value = true
        println(
            "FiltersSectionViewModel: Set startDate to ${result.date}, filters: ${_filters.value}")
      }
      is ParseResult.Failure -> {
        _errorMessage.value = result.message
      }
    }
  }

  /**
   * Sets the end date for event filtering.
   *
   * **Format**: Preferred format is dd/MM/yyyy (e.g., 31/12/2024 for Dec 31, 2024). Also accepts
   * MM/dd/yyyy and yyyy-MM-dd, but these may be ambiguous.
   *
   * **Side Effect**: Auto-enables the "When" section on successful parse. Validates that end date
   * is not before start date. Errors exposed via [errorMessage].
   *
   * @param date Date string in dd/MM/yyyy, MM/dd/yyyy, or yyyy-MM-dd format
   */
  fun setEndDate(date: String) {
    when (val result = parseDate(date)) {
      is ParseResult.Success -> {
        if (result.date >= _filters.value.startDate) {
          _filters.value = _filters.value.copy(endDate = result.date)
          isWhenChecked.value = true
          println(
              "FiltersSectionViewModel: Set endDate to ${result.date}, filters: ${_filters.value}")
        } else {
          _errorMessage.value = "End date must be on or after start date"
        }
      }
      is ParseResult.Failure -> {
        _errorMessage.value = result.message
      }
    }
  }

  // WHERE FILTER
  fun setLocation(location: Location) {
    _filters.value = _filters.value.copy(place = location)
    if (location.isDefined()) isWhereChecked.value = true
  }

  /**
   * Sets the search radius for location-based filtering.
   *
   * **Side Effect**: Auto-enables the "Where" section when radius is set.
   *
   * @param km Radius in kilometers (clamped to 0-999), or null to use default (10 km)
   */
  fun setRadius(km: Int?) {
    val newRadius = km?.coerceIn(0, 999) ?: 10
    _filters.value = _filters.value.copy(radiusKm = newRadius)
    isWhereChecked.value = true
  }

  // PRICE FILTER
  /**
   * Sets the maximum price filter for events.
   *
   * **Side Effect**: Auto-enables the price section when a non-null price is provided.
   *
   * @param value Maximum price in CHF (clamped to 0-999), or null to clear filter
   */
  fun setMaxPriceCHF(value: Int?) {
    val newPrice = value?.coerceIn(0, 999)
    _filters.value = _filters.value.copy(maxPrice = newPrice)
    if (newPrice != null) isPriceChecked.value = true
    println("FiltersSectionViewModel: Set maxPrice to $newPrice, filters: ${_filters.value}")
  }

  // TAGS FILTER
  /**
   * Toggles a tag in the filter selection.
   *
   * **Side Effect**: Auto-enables the tags section when tags are added. Adds the tag if not
   * present, removes it if already selected.
   *
   * @param tag The tag name to toggle
   */
  fun toggleTag(tag: String) {
    val currentTags = _filters.value.tags.toMutableSet()
    if (currentTags.contains(tag)) {
      currentTags.remove(tag)
    } else {
      currentTags.add(tag)
    }
    _filters.value = _filters.value.copy(tags = currentTags)
    if (currentTags.isNotEmpty()) isTagsChecked.value = true
  }

  // OTHER FILTERS
  /**
   * Filters to show only events with participating friends.
   *
   * @param enabled Whether to filter for friends-only events
   */
  fun setFriendsOnly(enabled: Boolean) {
    _filters.value = _filters.value.copy(friendsOnly = enabled)
  }

  /**
   * Filters to show only events with high participation.
   *
   * @param enabled Whether to filter for popular events only
   */
  fun setPopularOnly(enabled: Boolean) {
    _filters.value = _filters.value.copy(popularOnly = enabled)
  }

  // SECTION RESETS
  /** Resets and disables the "When" filter section. */
  fun resetWhen() = setWhenChecked(false)

  /** Resets and disables the "Where" filter section. */
  fun resetWhere() = setWhereChecked(false)

  /** Resets and disables the price filter section. */
  fun resetPrice() = setPriceChecked(false)

  /** Resets and disables the tags filter section. */
  fun resetTags() = setIsTagsChecked(false)

  // GLOBAL RESET
  /** Resets all filters to their default values and disables all sections. */
  fun reset() {
    resetWhen()
    resetWhere()
    resetPrice()
    resetTags()
    _filters.value = _filters.value.copy(friendsOnly = false, popularOnly = false)
  }

  /** Clears the current error message. */
  fun clearError() {
    _errorMessage.value = null
  }

  // Date parsing with error handling
  /** Sealed class representing the result of date parsing operations. */
  sealed class ParseResult {
    /** Successful parse with the resulting LocalDate. */
    data class Success(val date: LocalDate) : ParseResult()

    /** Failed parse with an error message. */
    data class Failure(val message: String) : ParseResult()
  }

  /**
   * Parses a date string using multiple format attempts.
   *
   * **Format Priority**:
   * 1. dd/MM/yyyy (preferred - day first, e.g., 25/12/2024 for Dec 25)
   * 2. MM/dd/yyyy (month first, may be ambiguous)
   * 3. yyyy-MM-dd (ISO format, unambiguous but less common in UI)
   *
   * **Note**: Dates like 01/02/2024 are ambiguous. The parser tries dd/MM/yyyy first, so this would
   * be interpreted as Feb 1, 2024. Consider using unambiguous formats like yyyy-MM-dd or providing
   * format hints in the UI.
   *
   * @param input The date string to parse
   * @return ParseResult indicating success or failure with details
   */
  private fun parseDate(input: String): ParseResult {
    if (input.isBlank()) {
      return ParseResult.Failure("Date input cannot be blank")
    }
    val formatters =
        listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    for (formatter in formatters) {
      try {
        return ParseResult.Success(LocalDate.parse(input, formatter))
      } catch (e: DateTimeParseException) {
        // Try next formatter
      }
    }
    return ParseResult.Failure("Invalid date format: $input")
  }
}

package com.swent.mapin.ui.filters

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.swent.mapin.model.Location
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
 * Each filter section can be independently enabled/disabled with toggle states.
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
   * Internal helper to update filter state and toggle visibility. When unchecked, resets the
   * associated filter values to defaults.
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

  /** Enables/disables the "When" filter section. Resets dates when disabled. */
  fun setWhenChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isWhenChecked) {
      copy(startDate = LocalDate.now(), endDate = null)
    }
  }

  /** Enables/disables the "Where" filter section. Resets location and radius when disabled. */
  fun setWhereChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isWhereChecked) { copy(place = null, radiusKm = 10) }
  }

  /** Enables/disables the price filter section. Resets max price when disabled. */
  fun setPriceChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isPriceChecked) { copy(maxPrice = null) }
  }

  /** Enables/disables the tags filter section. Clears selected tags when disabled. */
  fun setIsTagsChecked(enabled: Boolean) {
    updateFiltersAndToggle(enabled, isTagsChecked) { copy(tags = emptySet()) }
  }

  // TIME FILTER
  /**
   * Sets the start date for event filtering.
   *
   * @param date Date string in formats: dd/MM/yyyy, MM/dd/yyyy, or yyyy-MM-dd
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
   * Sets the end date for event filtering. Validates that end date is not before start date.
   *
   * @param date Date string in formats: dd/MM/yyyy, MM/dd/yyyy, or yyyy-MM-dd
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
  /**
   * Sets the location center for proximity-based filtering. Automatically enables the "Where"
   * section when a location is provided.
   *
   * @param location The geographic location to filter around, or null to clear
   */
  fun setLocation(location: Location?) {
    _filters.value = _filters.value.copy(place = location)
    if (location != null) isWhereChecked.value = true
  }

  /**
   * Sets the search radius for location-based filtering.
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
   * Toggles a tag in the filter selection. Adds the tag if not present, removes it if already
   * selected.
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
   * Enables/disables friends-only filtering. When enabled, only shows events with participating
   * friends.
   *
   * @param enabled Whether to filter for friends-only events
   */
  fun setFriendsOnly(enabled: Boolean) {
    _filters.value = _filters.value.copy(friendsOnly = enabled)
  }

  /**
   * Enables/disables popular-only filtering. When enabled, only shows events with high
   * participation.
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
   * Parses a date string using multiple format attempts. Tries formats: dd/MM/yyyy, MM/dd/yyyy, and
   * yyyy-MM-dd.
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

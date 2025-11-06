package com.swent.mapin.ui.map

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.swent.mapin.model.Location
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FiltersSectionViewModel : ViewModel() {
  // Single Filters state
  private var _filters = MutableStateFlow(Filters())
  val filters: StateFlow<Filters> = _filters

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage

  // SECTION TOGGLES
  var isWhenChecked = mutableStateOf(false)
    private set

  var isWhereChecked = mutableStateOf(false)
    private set

  var isPriceChecked = mutableStateOf(false)
    private set

  var isTagsChecked = mutableStateOf(false)
    private set

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

  fun setWhenChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isWhenChecked) {
      copy(startDate = LocalDate.now(), endDate = null)
    }
  }

  fun setWhereChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isWhereChecked) { copy(place = null, radiusKm = 10) }
  }

  fun setPriceChecked(checked: Boolean) {
    updateFiltersAndToggle(checked, isPriceChecked) { copy(maxPrice = null) }
  }

  fun setIsTagsChecked(enabled: Boolean) {
    updateFiltersAndToggle(enabled, isTagsChecked) { copy(tags = emptySet()) }
  }

  // TIME FILTER
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
  fun setLocation(location: Location?) {
    _filters.value = _filters.value.copy(place = location)
    if (location != null) isWhereChecked.value = true
  }

  fun setRadius(km: Int?) {
    val newRadius = km?.coerceIn(0, 999) ?: 10
    _filters.value = _filters.value.copy(radiusKm = newRadius)
    isWhereChecked.value = true
  }

  // PRICE FILTER
  fun setMaxPriceCHF(value: Int?) {
    val newPrice = value?.coerceIn(0, 999)
    _filters.value = _filters.value.copy(maxPrice = newPrice)
    if (newPrice != null) isPriceChecked.value = true
    println("FiltersSectionViewModel: Set maxPrice to $newPrice, filters: ${_filters.value}")
  }

  // TAGS FILTER
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
  fun setFriendsOnly(enabled: Boolean) {
    _filters.value = _filters.value.copy(friendsOnly = enabled)
  }

  fun setPopularOnly(enabled: Boolean) {
    _filters.value = _filters.value.copy(popularOnly = enabled)
  }

  // SECTION RESETS
  fun resetWhen() = setWhenChecked(false)

  fun resetWhere() = setWhereChecked(false)

  fun resetPrice() = setPriceChecked(false)

  fun resetTags() = setIsTagsChecked(false)

  // GLOBAL RESET
  fun reset() {
    resetWhen()
    resetWhere()
    resetPrice()
    resetTags()
    _filters.value = _filters.value.copy(friendsOnly = false, popularOnly = false)
  }

  fun clearError() {
    _errorMessage.value = null
  }

  // Date parsing with error handling
  sealed class ParseResult {
    data class Success(val date: LocalDate) : ParseResult()

    data class Failure(val message: String) : ParseResult()
  }

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

package com.swent.mapin.ui.map

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.swent.mapin.model.Location

class FiltersSectionViewModel : ViewModel() {

  // --- Section toggle states ---
  var isWhenChecked = mutableStateOf(false)
    private set

  var isWhereChecked = mutableStateOf(false)
    private set

  var isPriceChecked = mutableStateOf(false)
    private set

  fun setWhenChecked(checked: Boolean) {
    isWhenChecked.value = checked
  }

  fun setWhereChecked(checked: Boolean) {
    isWhereChecked.value = checked
  }

  fun setPriceChecked(checked: Boolean) {
    isPriceChecked.value = checked
  }

  // --- When section ---
  var startDate = mutableStateOf("")
    private set

  var endDate = mutableStateOf("")
    private set

  fun setStartDate(date: String) {
    startDate.value = date
  }

  fun setEndDate(date: String) {
    endDate.value = date
  }

  // --- Where section ---
  var pickedLocation = mutableStateOf<Location?>(null)
    private set

  var radiusKm = mutableStateOf<Int?>(null)
    private set

  fun setLocation(location: Location?) {
    pickedLocation.value = location
  }

  fun setRadius(km: Int?) {
    radiusKm.value = km
  }

  // --- Price section ---
  var maxPriceCHF = mutableStateOf<Int?>(null)
    private set

  fun setMaxPriceCHF(value: Int?) {
    maxPriceCHF.value = value
  }

  // --- Tags section ---
  var tagsEnabled = mutableStateOf(false)
    private set

  val selectedTags = mutableStateListOf<String>()

  fun setTagsEnabled(enabled: Boolean) {
    tagsEnabled.value = enabled
    if (!enabled) selectedTags.clear()
  }

  fun toggleTag(tag: String) {
    if (selectedTags.contains(tag)) selectedTags.remove(tag) else selectedTags.add(tag)
  }

  // --- Other filters ---
  var friendsOnly = mutableStateOf(false)
    private set

  var popularOnly = mutableStateOf(false)
    private set

  fun setFriendsOnly(enabled: Boolean) {
    friendsOnly.value = enabled
  }

  fun setPopularOnly(enabled: Boolean) {
    popularOnly.value = enabled
  }

  // --- Section Resets ---
  fun resetWhen() {
    setStartDate("")
    setEndDate("")
    setWhenChecked(false)
  }

  fun resetWhere() {
    setLocation(null)
    setRadius(null)
    setWhereChecked(false)
  }

  fun resetPrice() {
    setMaxPriceCHF(null)
    setPriceChecked(false)
  }

  fun resetTags() {
    selectedTags.clear()
    tagsEnabled.value = false
  }

  // --- GLOBAL RESET ---
  fun reset() {
    resetWhen()
    resetWhere()
    resetPrice()
    resetTags()
    setFriendsOnly(false)
    setPopularOnly(false)
  }
}

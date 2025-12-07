package com.swent.mapin.model.location

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swent.mapin.model.Location
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// Assisted by AI

/**
 * ViewModel responsible for handling location search and geocoding operations. It uses debouncing
 * and distinctUntilChanged for efficient API querying.
 *
 * @param repository The [LocationRepository] implementation provided by the Factory.
 */
@OptIn(FlowPreview::class)
class LocationViewModel(private val repository: LocationRepository) : ViewModel() {

  private val _locations = MutableStateFlow<List<Location>>(emptyList())
  /** The current list of search results returned by the forward geocoder. */
  val locations: StateFlow<List<Location>> = _locations

  private val queryFlow = MutableStateFlow("")

  init {
    viewModelScope.launch {
      queryFlow.debounce(500).distinctUntilChanged().collect { query -> performSearch(query) }
    }
  }

  /**
   * Handles the geocoding request to the repository.
   *
   * @param query The search query string.
   */
  private suspend fun performSearch(query: String) {
    if (query.isNotBlank()) {
      try {
        val results = repository.forwardGeocode(query)
        _locations.value = results
      } catch (e: Exception) {
        Log.e(null, "Search failed for query: $query", e)
        _locations.value = emptyList()
      }
    } else {
      _locations.value = emptyList()
    }
  }

  /**
   * Updates the search query flow, triggering the debounced search operation.
   *
   * @param newQuery The new text entered by the user.
   */
  fun onQueryChanged(newQuery: String) {
    queryFlow.value = newQuery
  }
}

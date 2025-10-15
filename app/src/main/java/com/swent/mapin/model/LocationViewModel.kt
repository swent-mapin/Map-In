package com.github.se.bootcamp.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swent.mapin.HttpClientProvider
import com.swent.mapin.model.Location
import com.swent.mapin.model.LocationRepository
import com.swent.mapin.model.NominatimLocationRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class LocationViewModel(
    private val repository: LocationRepository =
        NominatimLocationRepository(HttpClientProvider.client)
) : ViewModel() {

  private val _locations = MutableStateFlow<List<Location>>(emptyList())
  val locations: StateFlow<List<Location>> = _locations

  private val queryFlow = MutableStateFlow("")

  init {
    viewModelScope.launch {
      queryFlow
          .debounce(1000) // wait 1 second after typing stops
          .distinctUntilChanged()
          .collect { query ->
            if (query.isNotBlank()) {
              try {
                val results = repository.search(query)
                _locations.value = results
              } catch (e: Exception) {
                _locations.value = emptyList<Location>()
              }
            } else {
              _locations.value = emptyList()
            }
          }
    }
  }

  fun onQueryChanged(newQuery: String) {
    queryFlow.value = newQuery
  }
}

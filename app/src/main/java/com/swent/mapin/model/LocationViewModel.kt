package com.swent.mapin.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swent.mapin.HttpClientProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class LocationViewModel(
    private val repository: LocationRepository = NominatimForwardGeocoder(HttpClientProvider.client)
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
                val results = repository.forwardGeocode(query)
                _locations.value = results
              } catch (e: Exception) {
                _locations.value = emptyList<Location>()
              }
            } else {
              Log.w(null, "Query is blank")
              _locations.value = emptyList()
            }
          }
    }
  }

  fun onQueryChanged(newQuery: String) {
    queryFlow.value = newQuery
  }
}

package com.swent.mapin.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 250L

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Event> = emptyList(),
    val showNoResults: Boolean = false,
    val searchMode: Boolean = false,          // true = show dedicated search results content
    val shouldRequestFocus: Boolean = false,  // one-shot: request focus when entering search
)

class SearchViewModel(
    private val repo: EventRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    private var allEvents: List<Event> = emptyList()
    private var searchJob: Job? = null

    init {
        // Preload all events so we can filter locally (title/desc/tags/location)
        viewModelScope.launch {
            _ui.updateLoading(true)
            runCatching { repo.getAllEvents() }
                .onSuccess {
                    allEvents = it
                    _ui.updateResults(it, query = "")
                }
                .onFailure {
                    // show empty state rather than crashing; could add an error field if you want
                    _ui.updateResults(emptyList(), query = "")
                }
        }
    }

    /** User tapped the search bar → go FULL sheet & request focus. */
    fun onSearchTapped() {
        _ui.value = _ui.value.copy(
            searchMode = true,
            shouldRequestFocus = true
        )
    }

    /** UI consumed the focus request (FocusRequester.requestFocus was called). */
    fun onFocusHandled() {
        _ui.value = _ui.value.copy(shouldRequestFocus = false)
    }

    /** Query changed → debounce + dynamic results. */
    fun onQueryChange(newQuery: String) {
        _ui.value = _ui.value.copy(query = newQuery)

        // reset when cleared
        if (newQuery.isBlank()) {
            _ui.updateResults(allEvents, query = "")
            return
        }

        // debounce searches
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch(newQuery)
        }
    }

    /** Clear query → reset list, exit search mode (back to medium sheet in UI), hide keyboard. */
    fun onClearSearch() {
        searchJob?.cancel()
        _ui.value = _ui.value.copy(query = "")
        _ui.updateResults(allEvents, query = "")
        _ui.value = _ui.value.copy(searchMode = false)
    }

    // --- internals ---

    private fun performSearch(q: String) {
        val needle = norm(q)
        // local filter to support keyword-in-title/desc/tags/locationName
        val filtered = allEvents.filter { e ->
            norm(e.title).contains(needle) ||
                    norm(e.description).contains(needle) ||
                    norm(e.locationName).contains(needle) ||
                    e.tags.any { norm(it).contains(needle) }
        }
        _ui.updateResults(filtered, query = q)
    }

    private fun norm(s: String?): String = s?.trim()?.lowercase().orEmpty()

    private fun MutableStateFlow<SearchUiState>.updateLoading(v: Boolean) {
        value = value.copy(isLoading = v)
    }

    private fun MutableStateFlow<SearchUiState>.updateResults(list: List<Event>, query: String) {
        value = value.copy(
            isLoading = false,
            results = list,
            showNoResults = query.isNotBlank() && list.isEmpty()
        )
    }
}

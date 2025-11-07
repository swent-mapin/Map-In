package com.swent.mapin.ui.map.search

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.event.LocalEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Recent item shown in the search section: either a past query or a clicked event. */
sealed class RecentItem {
  data class Search(val query: String) : RecentItem()

  data class ClickedEvent(val eventId: String, val eventTitle: String) : RecentItem()
}

/**
 * Encapsulates all search-related state (query, filtering, recents and event dataset). The
 * controller owns the current event list used for search, exposes Compose-friendly state for the
 * UI, and persists recent items to SharedPreferences.
 */
class SearchStateController(
    private val applicationContext: Context,
    private val eventRepository: EventRepository,
    private val onClearFocus: () -> Unit,
    private val scope: CoroutineScope,
    private val localEventsProvider: () -> List<Event> = {
      LocalEventRepository.defaultSampleEvents()
    }
) {

  private var _searchQuery by mutableStateOf("")
  val searchQuery: String
    get() = _searchQuery

  private var _searchResults by mutableStateOf<List<Event>>(emptyList())
  val searchResults: List<Event>
    get() = _searchResults

  private var _recentItems by mutableStateOf<List<RecentItem>>(emptyList())
  val recentItems: List<RecentItem>
    get() = _recentItems

  private var _shouldFocusSearch by mutableStateOf(false)
  val shouldFocusSearch: Boolean
    get() = _shouldFocusSearch

  private var isSearchActivated by mutableStateOf(false)
  val isSearchActive: Boolean
    get() = isSearchActivated

  private var _clearSearchOnExitFull by mutableStateOf(false)
  val clearSearchOnExitFull: Boolean
    get() = _clearSearchOnExitFull

  private var allEvents: List<Event> = emptyList()

  init {
    loadRecentSearches()
  }

  fun initializeWithSamples(selectedTags: Set<String>): List<Event> {
    allEvents = localEventsProvider()
    return applyFilters(selectedTags)
  }

  fun loadRemoteEvents(selectedTags: Set<String>, onEventsUpdated: (List<Event>) -> Unit) {
    scope.launch {
      try {
        val events = eventRepository.getAllEvents()
        val filtered = setEvents(events, selectedTags)
        onEventsUpdated(filtered)
      } catch (primary: Exception) {
        Log.e(TAG, "Error loading events from repository", primary)
        try {
          val fallback = EventRepositoryProvider.createLocalRepository().getAllEvents()
          val filtered = setEvents(fallback, selectedTags)
          onEventsUpdated(filtered)
        } catch (fallbackError: Exception) {
          Log.e(TAG, "Failed to load fallback events", fallbackError)
          allEvents = emptyList()
          onEventsUpdated(applyFilters(selectedTags))
        }
      }
    }
  }

  fun refreshFilters(selectedTags: Set<String>): List<Event> = applyFilters(selectedTags)

  fun setEventsFromExternalSource(events: List<Event>, selectedTags: Set<String>): List<Event> =
      setEvents(events, selectedTags)

  fun replaceEvent(updatedEvent: Event, selectedTags: Set<String>): List<Event> {
    if (allEvents.isEmpty()) {
      allEvents = localEventsProvider()
    }
    val existingIndex = allEvents.indexOfFirst { it.uid == updatedEvent.uid }
    allEvents =
        if (existingIndex >= 0) {
          allEvents.toMutableList().also { it[existingIndex] = updatedEvent }
        } else {
          allEvents + updatedEvent
        }
    return applyFilters(selectedTags)
  }

  fun onSearchQueryChange(query: String, selectedTags: Set<String>): List<Event> {
    _searchQuery = query
    markSearchEditing()
    return applyFilters(selectedTags)
  }

  fun requestFocus() {
    _shouldFocusSearch = true
  }

  fun setFocusRequested(requested: Boolean) {
    _shouldFocusSearch = requested
  }

  fun onSearchFocusHandled() {
    _shouldFocusSearch = false
  }

  fun onSearchSubmit(selectedTags: Set<String>): List<Event>? {
    val trimmed = _searchQuery.trim()
    if (trimmed.isEmpty()) return null

    if (trimmed != _searchQuery) {
      _searchQuery = trimmed
    }

    markSearchCommitted()
    saveRecentSearch(trimmed)
    onClearFocus()
    return applyFilters(selectedTags)
  }

  fun applyRecentSearch(query: String, selectedTags: Set<String>): List<Event>? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    _searchQuery = trimmed
    markSearchCommitted()
    saveRecentSearch(trimmed)
    onClearFocus()
    return applyFilters(selectedTags)
  }

  fun resetSearchState(selectedTags: Set<String>): List<Event> {
    isSearchActivated = false
    _shouldFocusSearch = false
    onClearFocus()
    _clearSearchOnExitFull = false
    if (_searchQuery.isNotEmpty()) {
      _searchQuery = ""
    }
    return applyFilters(selectedTags)
  }

  fun saveRecentEvent(eventId: String, eventTitle: String) {
    try {
      val newItem = RecentItem.ClickedEvent(eventId, eventTitle)
      val updatedList = mutableListOf<RecentItem>(newItem)
      _recentItems.forEach { item ->
        when (item) {
          is RecentItem.Search -> updatedList.add(item)
          is RecentItem.ClickedEvent -> if (item.eventId != eventId) updatedList.add(item)
        }
      }
      _recentItems = updatedList
      saveRecentItemsToPrefs(updatedList)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save recent event", e)
    }
  }

  fun clearRecentSearches() {
    try {
      val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().remove(RECENT_ITEMS_KEY).remove(LEGACY_RECENT_KEY).apply()
      _recentItems = emptyList()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear recent items", e)
    }
  }

  fun findEventById(eventId: String): Event? = allEvents.find { it.uid == eventId }

  fun hasQuery(): Boolean = _searchQuery.isNotBlank()

  fun currentEvents(): List<Event> = allEvents

  fun loadRecentSearches() {
    try {
      val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val itemsJson = prefs.getString(RECENT_ITEMS_KEY, "[]")
      val itemsData =
          Gson().fromJson(itemsJson, Array<RecentItemData>::class.java)?.toList() ?: emptyList()

      _recentItems =
          itemsData.mapNotNull { data ->
            when (data.type) {
              "search" -> RecentItem.Search(data.value)
              "event" -> data.eventTitle?.let { RecentItem.ClickedEvent(data.value, it) }
              else -> null
            }
          }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load recent items", e)
      _recentItems = emptyList()
    }
  }

  fun markSearchEditing() {
    isSearchActivated = true
    _clearSearchOnExitFull = true
  }

  fun markSearchCommitted() {
    isSearchActivated = true
    _clearSearchOnExitFull = false
  }

  private fun setEvents(events: List<Event>, selectedTags: Set<String>): List<Event> {
    allEvents = events
    return applyFilters(selectedTags)
  }

  private fun applyFilters(selectedTags: Set<String>): List<Event> {
    val base = if (allEvents.isEmpty()) localEventsProvider() else allEvents

    val tagFiltered =
        if (selectedTags.isEmpty()) {
          base
        } else {
          base.filter { event ->
            event.tags.any { tag -> selectedTags.any { sel -> tag.equals(sel, ignoreCase = true) } }
          }
        }

    val finalList =
        if (_searchQuery.isBlank()) {
          tagFiltered
        } else {
          filterEvents(_searchQuery, tagFiltered)
        }

    _searchResults = if (_searchQuery.isBlank()) emptyList() else finalList
    return finalList
  }

  private fun filterEvents(query: String, source: List<Event>): List<Event> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return source

    val normalized = trimmed.lowercase()
    return source.filter { event ->
      val titleMatch = event.title.lowercase().contains(normalized)
      val descriptionMatch = event.description.lowercase().contains(normalized)
      val tagsMatch = event.tags.any { tag -> tag.lowercase().contains(normalized) }
      val locationMatch = event.location.name.lowercase().contains(normalized)
      titleMatch || descriptionMatch || tagsMatch || locationMatch
    }
  }

  private fun saveRecentSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return

    try {
      val newItem = RecentItem.Search(trimmed)
      val updatedList = mutableListOf<RecentItem>(newItem)
      _recentItems.forEach { item ->
        when (item) {
          is RecentItem.Search -> if (item.query != trimmed) updatedList.add(item)
          is RecentItem.ClickedEvent -> updatedList.add(item)
        }
      }
      _recentItems = updatedList
      saveRecentItemsToPrefs(updatedList)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save recent search", e)
    }
  }

  private fun saveRecentItemsToPrefs(items: List<RecentItem>) {
    try {
      val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val itemsData =
          items.map { item ->
            when (item) {
              is RecentItem.Search -> RecentItemData("search", item.query)
              is RecentItem.ClickedEvent -> RecentItemData("event", item.eventId, item.eventTitle)
            }
          }
      val itemsJson = Gson().toJson(itemsData)
      prefs.edit().putString(RECENT_ITEMS_KEY, itemsJson).apply()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save recent items to prefs", e)
    }
  }

  private data class RecentItemData(
      val type: String,
      val value: String,
      val eventTitle: String? = null
  )

  companion object {
    private const val TAG = "SearchStateController"
    private const val PREFS_NAME = "map_search_prefs"
    private const val RECENT_ITEMS_KEY = "recent_items"
    private const val LEGACY_RECENT_KEY = "recent_searches"
  }
}

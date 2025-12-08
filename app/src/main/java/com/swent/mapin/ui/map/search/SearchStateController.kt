package com.swent.mapin.ui.map.search

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.google.gson.Gson

/** Represents an item in recent searches — now only text queries. */
sealed class RecentItem {
  data class Search(val query: String) : RecentItem()

  data class ClickedEvent(val eventId: String, val eventTitle: String) : RecentItem()

  data class ClickedProfile(val userId: String, val userName: String) : RecentItem()
}

/** Handles search query text, focus state, and persistence of recent searches. */
class SearchStateController(
    private val applicationContext: Context,
    private val onClearFocus: () -> Unit,
) {

  private var _searchQuery by mutableStateOf("")
  val searchQuery: String
    get() = _searchQuery

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

  init {
    loadRecentSearches()
  }

  fun onSearchQueryChange(query: String) {
    _searchQuery = query
    markSearchEditing()
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

  /** Called when the user submits the search — just trims and logs the query. */
  fun onSearchSubmit() {
    val trimmed = _searchQuery.trim()
    if (trimmed.isEmpty()) return

    if (trimmed != _searchQuery) {
      _searchQuery = trimmed
    }

    markSearchCommitted()
    saveRecentSearch(trimmed)
    onClearFocus()
  }

  /** Called when a user taps a recent search item. */
  fun applyRecentSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    _searchQuery = trimmed
    markSearchCommitted()
    saveRecentSearch(trimmed)
    onClearFocus()
  }

  fun resetSearchState() {
    isSearchActivated = false
    _shouldFocusSearch = false
    onClearFocus()
    _clearSearchOnExitFull = false
    if (_searchQuery.isNotEmpty()) {
      _searchQuery = ""
    }
  }

  fun saveRecentEvent(eventId: String, eventTitle: String) {
    try {
      val newItem = RecentItem.ClickedEvent(eventId, eventTitle)
      val updatedList = mutableListOf<RecentItem>(newItem)
      _recentItems.forEach { item ->
        when (item) {
          is RecentItem.Search -> updatedList.add(item)
          is RecentItem.ClickedEvent -> if (item.eventId != eventId) updatedList.add(item)
          is RecentItem.ClickedProfile -> updatedList.add(item)
        }
      }
      _recentItems = updatedList
      saveRecentItemsToPrefs(updatedList)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save recent event", e)
    }
  }

  fun saveRecentProfile(userId: String, userName: String) {
    try {
      val newItem = RecentItem.ClickedProfile(userId, userName)
      val updatedList = mutableListOf<RecentItem>(newItem)
      _recentItems.forEach { item ->
        when (item) {
          is RecentItem.Search -> updatedList.add(item)
          is RecentItem.ClickedEvent -> updatedList.add(item)
          is RecentItem.ClickedProfile -> if (item.userId != userId) updatedList.add(item)
        }
      }
      _recentItems = updatedList
      saveRecentItemsToPrefs(updatedList)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save recent profile", e)
    }
  }

  fun clearRecentSearches() {
    try {
      val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit { remove(RECENT_ITEMS_KEY).remove(LEGACY_RECENT_KEY) }
      _recentItems = emptyList()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear recent items", e)
    }
  }

  fun hasQuery(): Boolean = _searchQuery.isNotBlank()

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
              "profile" -> data.eventTitle?.let { RecentItem.ClickedProfile(data.value, it) }
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
          is RecentItem.ClickedProfile -> updatedList.add(item)
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
              is RecentItem.ClickedProfile -> RecentItemData("profile", item.userId, item.userName)
            }
          }
      val itemsJson = Gson().toJson(itemsData)
      prefs.edit { putString(RECENT_ITEMS_KEY, itemsJson) }
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

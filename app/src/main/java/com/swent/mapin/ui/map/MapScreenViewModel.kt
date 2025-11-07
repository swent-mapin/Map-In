package com.swent.mapin.ui.map

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.swent.mapin.model.PreferencesRepositoryProvider
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.event.LocalEventRepository
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.map.bottomsheet.BottomSheetStateController
import com.swent.mapin.ui.map.camera.MapCameraController
import com.swent.mapin.ui.map.event.MapEventStateController
import com.swent.mapin.ui.map.memory.MemoryActionController
import com.swent.mapin.ui.map.search.RecentItem
import com.swent.mapin.ui.map.search.SearchStateController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the Map Screen, managing state for the map, bottom sheet, search, and memory form.
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    private val onClearFocus: () -> Unit,
    private val applicationContext: Context,
    private val memoryRepository: com.swent.mapin.model.memory.MemoryRepository =
        MemoryRepositoryProvider.getRepository(),
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository()
) : ViewModel() {

  private var authListener: FirebaseAuth.AuthStateListener? = null
  private val cameraController = MapCameraController(viewModelScope)
  private val searchStateController =
      SearchStateController(
          applicationContext = applicationContext,
          eventRepository = eventRepository,
          onClearFocus = onClearFocus,
          scope = viewModelScope)
  private val bottomSheetStateController =
      BottomSheetStateController(
          sheetConfig = sheetConfig,
          initialState = initialSheetState,
          isProgrammaticZoom = { cameraController.isProgrammaticZoom })
  private val eventStateController =
      MapEventStateController(
          eventRepository = eventRepository,
          auth = auth,
          scope = viewModelScope,
          replaceEventInSearch = { event ->
            searchStateController.replaceEvent(event, _selectedTags)
          },
          updateEventsState = ::applyEvents,
          getSelectedEvent = { _selectedEvent },
          setSelectedEvent = { _selectedEvent = it },
          setErrorMessage = { _errorMessage = it },
          clearErrorMessage = { _errorMessage = null })
  private val memoryActionController =
      MemoryActionController(
          applicationContext = applicationContext,
          memoryRepository = memoryRepository,
          auth = auth,
          scope = viewModelScope,
          onHideMemoryForm = { hideMemoryForm() },
          onRestoreSheetState = { restorePreviousSheetState() },
          setErrorMessage = { _errorMessage = it },
          clearErrorMessage = { _errorMessage = null })

  val bottomSheetState: BottomSheetState
    get() = bottomSheetStateController.state

  val fullEntryKey: Int
    get() = bottomSheetStateController.fullEntryKey

  var currentSheetHeight: Dp
    get() = bottomSheetStateController.currentSheetHeight
    set(value) {
      bottomSheetStateController.updateSheetHeight(value)
    }

  val searchQuery: String
    get() = searchStateController.searchQuery

  val shouldFocusSearch: Boolean
    get() = searchStateController.shouldFocusSearch

  // Visible events for map (after filtering)
  private var _events by mutableStateOf(LocalEventRepository.defaultSampleEvents())
  val events: List<Event>
    get() = _events

  val searchResults: List<Event>
    get() = searchStateController.searchResults

  fun setCenterCameraCallback(callback: (Event, Boolean) -> Unit) {
    cameraController.centerCameraCallback = callback
  }

  fun setFitCameraCallback(callback: (List<Event>) -> Unit) {
    cameraController.fitCameraCallback = callback
  }

  val isSearchMode: Boolean
    get() = searchStateController.isSearchActive && !showMemoryForm

  // Map interaction tracking
  val isZooming: Boolean
    get() = cameraController.isZooming
  // Tracks whether leaving full sheet should clear the current query (active editing state)
  // Saves the editing state before viewing an event, so we can restore it after closing
  private var wasEditingBeforeEvent by mutableStateOf(false)

  enum class MapStyle {
    STANDARD,
    SATELLITE,
    HEATMAP
  }

  private var _mapStyle by mutableStateOf(MapStyle.STANDARD)
  val mapStyle: MapStyle
    get() = _mapStyle

  val showHeatmap: Boolean
    get() = _mapStyle == MapStyle.HEATMAP

  val useSatelliteStyle: Boolean
    get() = _mapStyle == MapStyle.SATELLITE

  // State of the bottomsheet

  private var _currentBottomSheetScreen by mutableStateOf(BottomSheetScreen.MAIN_CONTENT)
  val currentBottomSheetScreen: BottomSheetScreen
    get() = _currentBottomSheetScreen

  // Memory creation form state
  private var _showMemoryForm by mutableStateOf(false)
  val showMemoryForm: Boolean
    get() = _showMemoryForm

  private var _previousSheetState: BottomSheetState? = null

  // Error and progress state
  private var _errorMessage by mutableStateOf<String?>(null)
  val errorMessage: String?
    get() = _errorMessage

  val isSavingMemory: Boolean
    get() = memoryActionController.isSavingMemory

  // Event catalog for memory linking
  val availableEvents: List<Event>
    get() = eventStateController.availableEvents

  // Joined events for bottom sheet display
  val joinedEvents: List<Event>
    get() = eventStateController.joinedEvents

  // Saved events for bottom sheet display
  val savedEvents: List<Event>
    get() = eventStateController.savedEvents

  // Saved events ids for quick lookup
  private val savedEventIds: Set<String>
    get() = eventStateController.savedEventIds

  enum class BottomSheetTab {
    SAVED_EVENTS,
    JOINED_EVENTS
  }

  private var _selectedBottomSheetTab by mutableStateOf(BottomSheetTab.SAVED_EVENTS)
  val selectedBottomSheetTab: BottomSheetTab
    get() = _selectedBottomSheetTab

  private var _selectedEvent by mutableStateOf<Event?>(null)
  val selectedEvent: Event?
    get() = _selectedEvent

  private var _organizerName by mutableStateOf("")
  val organizerName: String
    get() = _organizerName

  private var _showShareDialog by mutableStateOf(false)
  val showShareDialog: Boolean
    get() = _showShareDialog

  // Track if we came from search mode to return to it after closing event detail
  private var _cameFromSearch by mutableStateOf(false)
  // Track the sheet state before opening event to restore it correctly
  private var _sheetStateBeforeEvent by mutableStateOf(BottomSheetState.COLLAPSED)

  // Tag filtering
  private var _selectedTags by mutableStateOf<Set<String>>(emptySet())
  val selectedTags: Set<String>
    get() = _selectedTags

  private var _topTags by mutableStateOf<List<String>>(emptyList())
  val topTags: List<String>
    get() = _topTags

  val recentItems: List<RecentItem>
    get() = searchStateController.recentItems

  // User avatar URL for profile button (can be HTTP URL or preset icon ID)
  private var _avatarUrl by mutableStateOf<String?>(null)
  val avatarUrl: String?
    get() = _avatarUrl

  val directionViewModel = DirectionViewModel()

  init {
    eventStateController.updateBaseEvents(_events)
    // Load map style preference
    loadMapStylePreference()

    // Initialize with sample events quickly, then load remote data
    loadInitialSamples()
    // Preload events so the form has immediate data
    refreshEventsDataset()
    eventStateController.loadSavedEvents()
    eventStateController.loadSavedEventIds()
    _topTags = getTopTags()
    // Preload events both for searching and memory linking
    eventStateController.loadParticipantEvents()
    loadUserProfile()

    registerAuthStateListener()
  }

  /**
   * Load map style preference from DataStore synchronously.
   *
   * Uses runBlocking to ensure the preference is loaded before map initialization, preventing the
   * map from rendering with the wrong style initially. DataStore reads are fast (local storage), so
   * blocking here is acceptable and prevents visual flickering.
   */
  private fun loadMapStylePreference() {
    try {
      kotlinx.coroutines.runBlocking {
        val preferencesRepository = PreferencesRepositoryProvider.getInstance(applicationContext)
        // Use first() to get the initial value only, not continuously observe
        val style = preferencesRepository.mapStyleFlow.first()
        _mapStyle =
            when (style.lowercase()) {
              "satellite" -> MapStyle.SATELLITE
              else -> MapStyle.STANDARD
            }
      }
    } catch (e: Exception) {
      Log.e("MapScreenViewModel", "Failed to load map style preference: ${e.message}")
      _mapStyle = MapStyle.STANDARD
    }
  }

  /**
   * Reacts to Firebase auth transitions:
   * - On sign-out: clear user-scoped state (saved IDs/list, joined).
   * - On sign-in: load saved IDs/list and joined/participant events. Keeps UI consistent when users
   *   change auth state mid-session.
   *
   * Why the null-guard?
   * - This ViewModel may be re-used across configuration changes or refactors that call this
   *   registration method more than once. The guard ensures we never double-register the same
   *   listener, which would cause duplicate loads and leaks.
   * - The listener is explicitly removed in onCleared(), so the ViewModel lifecycle controls
   *   cleanup; re-registration should only ever happen after a new instance is created.
   */
  private fun registerAuthStateListener() {
    if (authListener != null) return
    authListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
          val uid = firebaseAuth.currentUser?.uid
          if (uid == null) {
            // Signed out → clear user-scoped state immediately
            eventStateController.clearUserScopedState()
            _avatarUrl = null
          } else {
            // Signed in → (re)load user-scoped data
            eventStateController.loadSavedEvents()
            eventStateController.loadSavedEventIds()
            eventStateController.updateBaseEvents(_events)
            eventStateController.loadParticipantEvents()
            loadUserProfile()
          }
        }
    auth.addAuthStateListener(authListener!!)
  }

  /** Loads initial sample events synchronously for immediate UI responsiveness. */
  private fun loadInitialSamples() {
    applyEvents(searchStateController.initializeWithSamples(_selectedTags))
  }

  /** Returns the top 5 most frequent tags across all events. */
  private fun getTopTags(count: Int = 5): List<String> {
    val events = LocalEventRepository.defaultSampleEvents()
    val tagCounts = mutableMapOf<String, Int>()

    events.forEach { event ->
      event.tags.forEach { tag -> tagCounts[tag] = tagCounts.getOrDefault(tag, 0) + 1 }
    }

    return tagCounts.entries.sortedByDescending { it.value }.take(count).map { it.key }
  }

  private fun refreshEventsDataset() {
    searchStateController.loadRemoteEvents(_selectedTags) { filtered -> applyEvents(filtered) }
  }

  private fun applyEvents(newEvents: List<Event>) {
    _events = newEvents
    eventStateController.updateBaseEvents(newEvents)
  }

  fun onZoomChange(newZoom: Float) {
    cameraController.onZoomChange(newZoom)
  }

  fun updateMediumReferenceZoom(zoom: Float) {
    bottomSheetStateController.updateMediumReferenceZoom(zoom)
  }

  fun checkZoomInteraction(currentZoom: Float): Boolean {
    return bottomSheetStateController.shouldCollapseAfterZoom(currentZoom)
  }

  fun checkTouchProximityToSheet(touchY: Float, sheetTopY: Float, densityDpi: Int): Boolean {
    return bottomSheetStateController.isTouchNearSheetTop(touchY, sheetTopY, densityDpi)
  }

  fun calculateTargetState(
      currentHeightPx: Float,
      collapsedPx: Float,
      mediumPx: Float,
      fullPx: Float
  ): BottomSheetState {
    return bottomSheetStateController.calculateTargetState(
        currentHeightPx, collapsedPx, mediumPx, fullPx)
  }

  fun getHeightForState(state: BottomSheetState): Dp {
    return bottomSheetStateController.getHeightForState(state)
  }

  fun setBottomSheetState(target: BottomSheetState, resetSearch: Boolean = true) {
    val transition = bottomSheetStateController.transitionTo(target)
    val hasCommittedSearch =
        !searchStateController.clearSearchOnExitFull && searchQuery.isNotEmpty()
    val shouldClear = transition.leftFull && !hasCommittedSearch

    if (resetSearch && shouldClear && !_showMemoryForm) {
      applyEvents(searchStateController.resetSearchState(_selectedTags))
    }
  }

  fun onSearchQueryChange(query: String) {
    if (bottomSheetState != BottomSheetState.FULL) {
      searchStateController.requestFocus()
      setBottomSheetState(BottomSheetState.FULL)
    }
    applyEvents(searchStateController.onSearchQueryChange(query, _selectedTags))
  }

  fun onSearchTap() {
    if (bottomSheetState != BottomSheetState.FULL) {
      searchStateController.requestFocus()
      setBottomSheetState(BottomSheetState.FULL)
    }
    searchStateController.markSearchEditing()
  }

  fun onSearchFocusHandled() {
    searchStateController.onSearchFocusHandled()
  }

  /** Called when user submits a search (presses enter or search button). */
  fun onSearchSubmit() {
    val filteredEvents = searchStateController.onSearchSubmit(_selectedTags) ?: return
    applyEvents(filteredEvents)
    setBottomSheetState(BottomSheetState.MEDIUM, resetSearch = false)
    focusCameraOnSearchResults()
  }

  /** Applies a recent search query from history. */
  fun applyRecentSearch(query: String) {
    val filteredEvents = searchStateController.applyRecentSearch(query, _selectedTags) ?: return
    applyEvents(filteredEvents)
    setBottomSheetState(BottomSheetState.MEDIUM, resetSearch = false)
    focusCameraOnSearchResults()
  }

  private fun focusCameraOnSearchResults() {
    if (!searchStateController.hasQuery()) return
    val results = searchResults
    if (results.isEmpty()) return
    cameraController.fitToEvents(results)
  }

  fun onClearSearch() {
    _events = searchStateController.resetSearchState(_selectedTags)
    setBottomSheetState(BottomSheetState.MEDIUM)
  }

  fun setMapStyle(style: MapStyle) {
    _mapStyle = style
    // Persist the user's choice to DataStore
    viewModelScope.launch {
      try {
        val preferencesRepository = PreferencesRepositoryProvider.getInstance(applicationContext)
        val styleString =
            when (style) {
              MapStyle.SATELLITE -> "satellite"
              MapStyle.STANDARD -> "standard"
              MapStyle.HEATMAP -> "standard" // Heatmap is a temporary overlay, save as standard
            }
        preferencesRepository.setMapStyle(styleString)
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Failed to save map style preference: ${e.message}")
      }
    }
  }

  fun clearError() {
    _errorMessage = null
  }

  /** Loads the current user's avatar URL from their profile. */
  fun loadUserProfile() {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      _avatarUrl = null
      return
    }
    viewModelScope.launch {
      try {
        val userProfile = userProfileRepository.getUserProfile(uid)
        _avatarUrl = userProfile?.avatarUrl
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Error loading user profile", e)
        _avatarUrl = null
      }
    }
  }

  /** Clears all recent items history. */
  fun clearRecentSearches() {
    searchStateController.clearRecentSearches()
  }

  fun showMemoryForm() {
    _previousSheetState = bottomSheetState
    _showMemoryForm = true
    _currentBottomSheetScreen = BottomSheetScreen.MEMORY_FORM
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideMemoryForm() {
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
  }

  fun showAddEventForm() {
    _previousSheetState = bottomSheetState
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.ADD_EVENT
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideAddEventForm() {
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
  }

  fun onMemorySave(formData: MemoryFormData) {
    memoryActionController.saveMemory(formData)
  }

  fun onMemoryCancel() {
    hideMemoryForm()
    restorePreviousSheetState()
  }

  fun onAddEventCancel() {
    hideAddEventForm()
    restorePreviousSheetState()
  }

  override fun onCleared() {
    super.onCleared()
    cameraController.clearCallbacks()

    // Remove auth listener to avoid leaks
    authListener?.let { auth.removeAuthStateListener(it) }
    authListener = null
  }

  private fun restorePreviousSheetState() {
    _previousSheetState?.let { setBottomSheetState(it) }
    _previousSheetState = null
  }

  fun setEvents(newEvents: List<Event>) {
    applyEvents(searchStateController.setEventsFromExternalSource(newEvents, _selectedTags))
  }

  fun toggleTagSelection(tag: String) {
    _selectedTags = if (_selectedTags.contains(tag)) _selectedTags - tag else _selectedTags + tag
    applyEvents(searchStateController.refreshFilters(_selectedTags))
  }

  fun setBottomSheetTab(tab: BottomSheetTab) {
    _selectedBottomSheetTab = tab
  }

  fun onEventPinClicked(event: Event, forceZoom: Boolean = false) {
    viewModelScope.launch {
      // Save current sheet state before opening event detail
      _sheetStateBeforeEvent = bottomSheetState
      _selectedEvent = event
      _organizerName = "User ${event.ownerId.take(6)}"
      setBottomSheetState(BottomSheetState.MEDIUM)

      cameraController.centerOnEvent(event, forceZoom)
    }
  }

  /**
   * Handles event clicks from search results. Focuses the pin, shows event details, and remembers
   * we came from search mode. Forces zoom to ensure the pin is visible.
   */
  fun onEventClickedFromSearch(event: Event) {
    _cameFromSearch = true
    // Save editing state before marking as committed, so we can restore it later
    wasEditingBeforeEvent = searchStateController.clearSearchOnExitFull
    searchStateController.saveRecentEvent(event.uid, event.title)
    searchStateController.markSearchCommitted()
    onEventPinClicked(event, forceZoom = true)
  }

  /** Handles event click from recent items by finding the event and showing details. */
  fun onRecentEventClicked(eventId: String) {
    val event = searchStateController.findEventById(eventId)
    if (event != null) {
      _cameFromSearch = true
      // Recent events are always committed searches (not editing)
      wasEditingBeforeEvent = false
      searchStateController.markSearchCommitted()
      onEventPinClicked(event, forceZoom = true)
    }
  }

  fun closeEventDetail() {
    _selectedEvent = null
    _organizerName = ""

    // Clear directions when closing event detail
    directionViewModel.clearDirection()

    if (_cameFromSearch) {
      // Return to search mode without clearing the current query
      _cameFromSearch = false
      searchStateController.markSearchCommitted()
      // Restore the editing state from before viewing the event
      val wasEditing = wasEditingBeforeEvent
      if (wasEditing) {
        searchStateController.markSearchEditing()
      }
      // Restore focus if user was typing (editing), otherwise just show search results
      searchStateController.setFocusRequested(wasEditing)
      wasEditingBeforeEvent = false // Reset for next time
      applyEvents(searchStateController.refreshFilters(_selectedTags))
      // Return to FULL if user was editing, otherwise restore previous sheet state
      // (FULL for recents, MEDIUM for search results)
      val targetState = if (wasEditing) BottomSheetState.FULL else _sheetStateBeforeEvent
      setBottomSheetState(targetState, resetSearch = false)
    } else {
      setBottomSheetState(BottomSheetState.COLLAPSED)
    }
  }

  /**
   * Toggle directions display for the given event. Uses a default user location (EPFL campus) for
   * demo purposes. Next week: integrate with actual user location from GPS/profile.
   */
  fun toggleDirections(event: Event) {
    val currentState = directionViewModel.directionState

    if (currentState is DirectionState.Displayed) {
      directionViewModel.clearDirection()
    } else {
      // Default user location: EPFL (for demo purposes)
      val userLocation =
          Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE)
      val eventLocation = Point.fromLngLat(event.location.longitude, event.location.latitude)

      directionViewModel.requestDirections(userLocation, eventLocation)
    }
  }

  fun showShareDialog() {
    _showShareDialog = true
  }

  fun dismissShareDialog() {
    _showShareDialog = false
  }

  fun isUserParticipating(): Boolean {
    val currentUserId = auth.currentUser?.uid ?: return false
    return _selectedEvent?.participantIds?.contains(currentUserId) ?: false
  }

  fun isUserParticipating(event: Event): Boolean {
    val currentUserId = auth.currentUser?.uid ?: return false
    return event.participantIds.contains(currentUserId)
  }

  fun isEventSaved(event: Event): Boolean {
    return savedEventIds.contains(event.uid)
  }

  fun joinEvent() {
    eventStateController.joinSelectedEvent()
  }

  fun unregisterFromEvent() {
    eventStateController.unregisterSelectedEvent()
  }

  /**
   * Saves the selected event for later by the current user. Implemented with the help of AI
   * generated code.
   */
  fun saveEventForLater() {
    eventStateController.saveSelectedEventForLater()
  }

  /**
   * Unsaves the selected event for later by the current user. Implemented with the help of AI
   * generated code.
   */
  fun unsaveEventForLater() {
    eventStateController.unsaveSelectedEvent()
  }

  /**
   * Called when the user taps an event from the "Joined events" or "Saved events" list. Reuses the
   * existing onEventPinClicked behavior to select the event and center the camera.
   */
  fun onTabEventClicked(event: Event) {
    onEventPinClicked(event)
  }
}

@Composable
fun rememberMapScreenViewModel(
    sheetConfig: BottomSheetConfig,
    initialSheetState: BottomSheetState = BottomSheetState.COLLAPSED
): MapScreenViewModel {
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val appContext = context.applicationContext

  return viewModel {
    MapScreenViewModel(
        initialSheetState = initialSheetState,
        sheetConfig = sheetConfig,
        onClearFocus = { focusManager.clearFocus(force = true) },
        applicationContext = appContext)
  }
}

fun eventsToGeoJson(events: List<Event>): String {
  val features =
      events.map { event ->
        Feature.fromGeometry(
            Point.fromLngLat(event.location.longitude, event.location.latitude),
            JsonObject().apply { addProperty("weight", event.participantIds.size) })
      }
  return FeatureCollection.fromFeatures(features).toJson()
}

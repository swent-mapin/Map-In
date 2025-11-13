package com.swent.mapin.ui.map

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.swent.mapin.model.memory.MemoryRepository
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import com.swent.mapin.ui.map.bottomsheet.BottomSheetStateController
import com.swent.mapin.ui.map.directions.DirectionState
import com.swent.mapin.ui.map.directions.DirectionViewModel
import com.swent.mapin.ui.map.eventstate.MapEventStateController
import com.swent.mapin.ui.map.location.LocationController
import com.swent.mapin.ui.map.location.LocationManager
import com.swent.mapin.ui.map.search.RecentItem
import com.swent.mapin.ui.map.search.SearchStateController
import com.swent.mapin.ui.memory.MemoryActionController
import com.swent.mapin.ui.memory.MemoryFormData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Assisted by AI tools

/**
 * ViewModel for the Map Screen, managing state for the map, bottom sheet, search, and memory form.
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    onClearFocus: () -> Unit,
    private val applicationContext: Context,
    private val memoryRepository: MemoryRepository = MemoryRepositoryProvider.getRepository(),
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(),
    private val locationManager: LocationManager = LocationManager(applicationContext),
    val filterViewModel: FiltersSectionViewModel = FiltersSectionViewModel(),
) : ViewModel() {

  private var clearFocusCallback: (() -> Unit) = onClearFocus

  private fun clearSearchFieldFocus() {
    clearFocusCallback()
  }

  private var authListener: FirebaseAuth.AuthStateListener? = null
  private val cameraController = MapCameraController(viewModelScope)
  private val searchStateController =
      SearchStateController(
          applicationContext = applicationContext, onClearFocus = ::clearSearchFieldFocus)
  private val bottomSheetStateController =
      BottomSheetStateController(
          sheetConfig = sheetConfig,
          initialState = initialSheetState,
          isProgrammaticZoom = { cameraController.isProgrammaticZoom })
  val eventStateController =
      MapEventStateController(
          eventRepository = eventRepository,
          userProfileRepository = userProfileRepository,
          auth = auth,
          scope = viewModelScope,
          filterViewModel = filterViewModel,
          getSelectedEvent = { _selectedEvent },
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
  private val locationController =
      LocationController(
          locationManager = locationManager,
          scope = viewModelScope,
          setErrorMessage = { _errorMessage = it })

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
  val events: List<Event>
    get() = eventStateController.allEvents

  val searchResults: List<Event>
    get() = eventStateController.searchResults

  fun setCenterCameraCallback(callback: (Event, Boolean) -> Unit) {
    cameraController.centerCameraCallback = callback
  }

  fun setFitCameraCallback(callback: (List<Event>) -> Unit) {
    cameraController.fitCameraCallback = callback
  }

  val isSearchMode: Boolean
    get() = searchStateController.isSearchActive && !showMemoryForm

  fun runProgrammaticCamera(block: () -> Unit) {
    cameraController.runProgrammatic(block)
  }

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
  private var _sheetStateBeforeEvent by mutableStateOf<BottomSheetState?>(null)

  val recentItems: List<RecentItem>
    get() = searchStateController.recentItems

  // Location state - delegated to LocationController
  val currentLocation: Location?
    get() = locationController.currentLocation

  val hasLocationPermission: Boolean
    get() = locationController.hasLocationPermission

  var onCenterOnUserLocation: (() -> Unit)?
    get() = locationController.onCenterOnUserLocation
    set(value) {
      locationController.onCenterOnUserLocation = value
    }

  var onRequestLocationPermission: (() -> Unit)?
    get() = locationController.onRequestLocationPermission
    set(value) {
      locationController.onRequestLocationPermission = value
    }

  val locationBearing: Float
    get() = locationController.locationBearing

  val isCenteredOnUser: Boolean
    get() = locationController.isCenteredOnUser

  // User avatar URL for profile button (can be HTTP URL or preset icon ID)
  private var _avatarUrl by mutableStateOf<String?>(null)
  val avatarUrl: String?
    get() = _avatarUrl

  val directionViewModel: DirectionViewModel by lazy {
    val accessToken =
        com.swent.mapin.ui.map.directions.ApiKeyProvider.getMapboxAccessToken(applicationContext)
    val directionsService =
        if (accessToken.isNotEmpty()) {
          com.swent.mapin.ui.map.directions.MapboxDirectionsService(accessToken)
        } else null
    DirectionViewModel(directionsService)
  }

  init {
    eventStateController.observeFilters()
    // Load map style preference
    loadMapStylePreference()

    // Load events
    eventStateController.refreshEventsList()
    eventStateController.loadSavedEvents()
    eventStateController.loadJoinedEvents()

    // Load user profile
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
            eventStateController.refreshEventsList()
            eventStateController.loadSavedEvents()
            eventStateController.loadJoinedEvents()
            loadUserProfile()
          }
        }
    auth.addAuthStateListener(authListener!!)
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
      searchStateController.resetSearchState()
      eventStateController.clearSearchResults()
    }
  }

  fun onSearchQueryChange(query: String) {
    if (bottomSheetState != BottomSheetState.FULL) {
      searchStateController.requestFocus()
      setBottomSheetState(BottomSheetState.FULL)
    }
    searchStateController.onSearchQueryChange(query)
    eventStateController.searchEvents(query)
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
    searchStateController.onSearchSubmit()
    setBottomSheetState(BottomSheetState.MEDIUM, resetSearch = false)
    focusCameraOnSearchResults()
  }

  /** Applies a recent search query from history. */
  fun applyRecentSearch(query: String) {
    searchStateController.applyRecentSearch(query)
    eventStateController.searchEvents(query)
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
    searchStateController.resetSearchState()
    eventStateController.clearSearchResults()
    setBottomSheetState(BottomSheetState.MEDIUM)
  }

  fun updateFocusClearer(onClearFocus: () -> Unit) {
    clearFocusCallback = onClearFocus
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
    val event = eventStateController.allEvents.find { it.uid == eventId }
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

    val previousSheetState = _sheetStateBeforeEvent
    _sheetStateBeforeEvent = null

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

      // Return to FULL if user was editing, otherwise restore previous sheet state
      // (FULL for recents, MEDIUM for search results)
      val targetState =
          if (wasEditing) BottomSheetState.FULL
          else previousSheetState ?: BottomSheetState.COLLAPSED
      setBottomSheetState(targetState, resetSearch = false)
    } else {
      val targetState = previousSheetState ?: BottomSheetState.COLLAPSED
      setBottomSheetState(targetState)
    }
  }

  /**
   * Toggle directions display for the given event.
   *
   * Uses the user's current location if available, otherwise falls back to a default location.
   */
  fun toggleDirections(event: Event) {
    val currentState = directionViewModel.directionState

    if (currentState is DirectionState.Displayed) {
      directionViewModel.clearDirection()
    } else {
      val userLoc = currentLocation
      val userLocation =
          if (userLoc != null) {
            Point.fromLngLat(userLoc.longitude, userLoc.latitude)
          } else {
            Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE)
          }
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

  fun isEventJoined(event: Event? = _selectedEvent): Boolean {
    return if (event == null) {
      false
    } else {
      joinedEvents.any { it.uid == event.uid }
    }
  }

  fun isEventSaved(event: Event? = _selectedEvent): Boolean {
    return if (event == null) {
      false
    } else {
      savedEvents.any { it.uid == event.uid }
    }
  }

  /** Add the selected event to the current user's joined events. */
  fun joinEvent() {
    val currentEvent = _selectedEvent ?: return

    viewModelScope.launch {
      eventStateController.joinSelectedEvent()
      _selectedEvent = eventStateController.refreshSelectedEvent(currentEvent.uid)
    }
  }

  /** Remove the selected event from the current user's joined events. */
  fun unregisterFromEvent() {
    val currentEvent = _selectedEvent ?: return

    viewModelScope.launch {
      eventStateController.leaveSelectedEvent()
      _selectedEvent = eventStateController.refreshSelectedEvent(currentEvent.uid)
    }
  }

  /** Saves the selected event for later by the current user. */
  fun saveEventForLater() {
    val currentEvent = _selectedEvent ?: return

    viewModelScope.launch {
      eventStateController.saveSelectedEvent()
      _selectedEvent = eventStateController.refreshSelectedEvent(currentEvent.uid)
    }
  }

  /** Unsaves the selected event for later by the current user. */
  fun unsaveEventForLater() {
    val currentEvent = _selectedEvent ?: return

    viewModelScope.launch {
      eventStateController.unsaveSelectedEvent()
      _selectedEvent = eventStateController.refreshSelectedEvent(currentEvent.uid)
    }
  }

  /**
   * Called when the user taps an event from the "Joined events" or "Saved events" list. Reuses the
   * existing onEventPinClicked behavior to select the event and center the camera.
   */
  fun onTabEventClicked(event: Event) {
    onEventPinClicked(event)
  }

  // Location management methods - delegated to LocationController

  /** Checks and updates the location permission status. */
  fun checkLocationPermission() = locationController.checkLocationPermission()

  /** Starts listening to location updates if permission is granted. */
  fun startLocationUpdates() = locationController.startLocationUpdates()

  /** Gets the last known location and optionally centers the camera on it. */
  fun getLastKnownLocation(centerCamera: Boolean = false) =
      locationController.getLastKnownLocation(centerCamera)

  /**
   * Handles the location button click. If permission is granted, centers on user location.
   * Otherwise, requests permission.
   */
  fun onLocationButtonClick() = locationController.onLocationButtonClick()

  /**
   * Updates the centered state based on camera position. Call this when the camera moves to check
   * if still centered on user.
   */
  fun updateCenteredState(cameraLat: Double, cameraLon: Double) =
      locationController.updateCenteredState(cameraLat, cameraLon)

  /**
   * Manually marks that the camera is no longer centered on the user. Call this when user manually
   * moves the map.
   */
  fun onMapMoved() = locationController.onMapMoved()
}

@Composable
fun rememberMapScreenViewModel(
    sheetConfig: BottomSheetConfig,
    initialSheetState: BottomSheetState = BottomSheetState.COLLAPSED
): MapScreenViewModel {
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val appContext = context.applicationContext

  val mapScreenViewModel: MapScreenViewModel = viewModel {
    MapScreenViewModel(
        initialSheetState = initialSheetState,
        sheetConfig = sheetConfig,
        onClearFocus = { focusManager.clearFocus(force = true) },
        applicationContext = appContext)
  }

  DisposableEffect(focusManager, mapScreenViewModel) {
    mapScreenViewModel.updateFocusClearer { focusManager.clearFocus(force = true) }
    onDispose { mapScreenViewModel.updateFocusClearer {} }
  }

  return mapScreenViewModel
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

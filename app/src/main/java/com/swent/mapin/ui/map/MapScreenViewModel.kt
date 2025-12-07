package com.swent.mapin.ui.map

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.swent.mapin.model.network.ConnectivityService
import com.swent.mapin.model.network.ConnectivityServiceProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import com.swent.mapin.ui.map.bottomsheet.BottomSheetStateController
import com.swent.mapin.ui.map.directions.DirectionState
import com.swent.mapin.ui.map.directions.DirectionViewModel
import com.swent.mapin.ui.map.eventstate.MapEventStateController
import com.swent.mapin.ui.map.location.LocationController
import com.swent.mapin.ui.map.location.LocationManager
import com.swent.mapin.ui.map.offline.EventBasedOfflineRegionManager
import com.swent.mapin.ui.map.offline.OfflineRegionManager
import com.swent.mapin.ui.map.offline.TileStoreManagerProvider
import com.swent.mapin.ui.map.search.RecentItem
import com.swent.mapin.ui.map.search.SearchStateController
import com.swent.mapin.ui.memory.MemoryActionController
import com.swent.mapin.ui.memory.MemoryFormData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Assisted by AI tools

/** Sealed class to handle the state of the name of the owner of the current event */
sealed class OrganizerState {
  object Loading : OrganizerState()

  data class Loaded(val userId: String, val name: String) : OrganizerState()

  object Error : OrganizerState()
}

/**
 * ViewModel for the Map Screen, managing state for the map, bottom sheet, search, and memory form.
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    onClearFocus: () -> Unit,
    private val applicationContext: Context,
    private val connectivityService: ConnectivityService,
    private val memoryRepository: MemoryRepository = MemoryRepositoryProvider.getRepository(),
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(),
    private val locationManager: LocationManager = LocationManager(applicationContext),
    val filterViewModel: FiltersSectionViewModel = FiltersSectionViewModel(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val enableEventBasedDownloads: Boolean = true
) : ViewModel() {

  private var clearFocusCallback: (() -> Unit) = onClearFocus

  private fun clearSearchFieldFocus() {
    clearFocusCallback()
  }

  private var authListener: FirebaseAuth.AuthStateListener? = null
  private var downloadCompleteDismissJob: Job? = null
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
          connectivityService = connectivityService,
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

  private val offlineRegionManager: OfflineRegionManager by lazy {
    val tileStore = TileStoreManagerProvider.getInstance().getTileStore()
    val connectivityFlow = {
      ConnectivityServiceProvider.getInstance(applicationContext).connectivityState.map {
        it.isConnected
      }
    }
    OfflineRegionManager(tileStore, connectivityFlow)
  }

  private val eventBasedOfflineRegionManager: EventBasedOfflineRegionManager? by lazy {
    if (!enableEventBasedDownloads) {
      Log.w("MapScreenViewModel", "Event-based downloads disabled")
      return@lazy null
    }
    try {
      EventBasedOfflineRegionManager(
          offlineRegionManager = offlineRegionManager,
          connectivityService = ConnectivityServiceProvider.getInstance(applicationContext),
          scope = viewModelScope,
          context = applicationContext,
          onDownloadStart = { event ->
            _downloadingEvent = event
            _downloadProgress = 0f
          },
          onDownloadProgress = { _, progress -> _downloadProgress = progress },
          onDownloadComplete = { _, result ->
            _downloadingEvent = null
            _downloadProgress = 0f
            result.onSuccess {
              _showDownloadComplete = true
              // Auto-clear after 3 seconds
              downloadCompleteDismissJob?.cancel()
              downloadCompleteDismissJob =
                  viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _showDownloadComplete = false
                  }
            }
          })
    } catch (e: Exception) {
      Log.w("MapScreenViewModel", "EventBasedOfflineRegionManager not available", e)
      null
    }
  }

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

  val userSearchResults: List<com.swent.mapin.model.UserProfile>
    get() = eventStateController.userSearchResults

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

  // Download progress state
  private var _downloadingEvent by mutableStateOf<Event?>(null)
  val downloadingEvent: Event?
    get() = _downloadingEvent

  private var _downloadProgress by mutableFloatStateOf(0f)
  val downloadProgress: Float
    get() = _downloadProgress

  private var _showDownloadComplete by mutableStateOf(false)
  val showDownloadComplete: Boolean
    get() = _showDownloadComplete

  // Event catalog for memory linking
  val availableEvents: List<Event>
    get() = eventStateController.availableEvents

  // Joined events for bottom sheet display
  val joinedEvents: List<Event>
    get() = eventStateController.joinedEvents

  // New: Attended events for bottom sheet display
  val attendedEvents: List<Event>
    get() = eventStateController.attendedEvents

  // Saved events for bottom sheet display
  val savedEvents: List<Event>
    get() = eventStateController.savedEvents

  // Owned events for bottom sheet display
  val ownedEvents: List<Event>
    get() = eventStateController.ownedEvents

  val ownedEventsLoading: Boolean
    get() = eventStateController.ownedLoading

  val ownedEventsError: String?
    get() = eventStateController.ownedError

  enum class BottomSheetTab {
    SAVED,
    UPCOMING,
    PAST,
    OWNED
  }

  private var _selectedBottomSheetTab by mutableStateOf(BottomSheetTab.SAVED)
  val selectedBottomSheetTab: BottomSheetTab
    get() = _selectedBottomSheetTab

  private var _selectedEvent by mutableStateOf<Event?>(null)
  val selectedEvent: Event?
    get() = _selectedEvent
  // Initial event to prefill the memory form when opening it via the '+' button
  private var _memoryFormInitialEvent by mutableStateOf<Event?>(null)
  val memoryFormInitialEvent: Event?
    get() = _memoryFormInitialEvent

  private var _organizerState by mutableStateOf<OrganizerState>(OrganizerState.Loading)
  val organizerState: OrganizerState
    get() = _organizerState

  private var _showShareDialog by mutableStateOf(false)
  val showShareDialog: Boolean
    get() = _showShareDialog

  // Track if we came from search mode to return to it after closing event detail
  private var _cameFromSearch by mutableStateOf(false)
  // Track the sheet state before opening event to restore it correctly
  private var _sheetStateBeforeEvent by mutableStateOf<BottomSheetState?>(null)
  // Store pending deep link until events are loaded
  private var pendingDeepLinkEventId: String? = null
  private var deepLinkFetchAttempted = false
  // Expose resolved deep link event for UI to handle
  private var _resolvedDeepLinkEvent by mutableStateOf<Event?>(null)
  val resolvedDeepLinkEvent: Event?
    get() = _resolvedDeepLinkEvent

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
    // Load map style preference
    loadMapStylePreference()

    // Observe filters and connectivity changes
    eventStateController.observeFilters()
    eventStateController.observeConnectivity()
    // Load events and start listeners
    eventStateController.refreshEventsList()
    eventStateController.startListeners()

    // Load user profile
    loadUserProfile()

    registerAuthStateListener()

    locationController.onLocationUpdate = { location ->
      directionViewModel.onLocationUpdate(location)
    }

    // Start observing saved/joined events for offline downloads
    if (enableEventBasedDownloads) {
      startEventBasedOfflineDownloads()
    }
  }

  /**
   * Starts observing saved and joined events for offline region downloads and deletions.
   *
   * This is called during ViewModel initialization if event-based downloads are enabled. The
   * manager will reactively download 2km radius regions around saved/joined events when online, and
   * delete regions when events are unsaved or left.
   */
  private fun startEventBasedOfflineDownloads() {
    viewModelScope.launch {
      try {
        val manager = eventBasedOfflineRegionManager ?: return@launch
        val userId = auth.currentUser?.uid ?: return@launch

        // Start observing for downloads
        manager.observeEvents(
            onSavedEventsFlow = eventStateController.savedEventsFlow,
            onJoinedEventsFlow = eventStateController.joinedEventsFlow)

        // Start observing for deletions
        manager.observeEventsForDeletion(
            onSavedEventsFlow = eventStateController.savedEventsFlow,
            onJoinedEventsFlow = eventStateController.joinedEventsFlow)

        Log.w(
            "MapScreenViewModel",
            "Event-based offline downloads and deletions started for user: $userId")
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Failed to start event-based offline downloads", e)
      }
    }
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
            eventStateController.loadOwnedEvents()
            eventStateController.startListeners()
            loadUserProfile()
          }
        }
    auth.addAuthStateListener(authListener!!)
  }

  // ...existing code...
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
    eventStateController.clearUserSearchResults()
    setBottomSheetState(BottomSheetState.MEDIUM)
  }

  /** Called when a user is clicked in search results. Opens their profile sheet. */
  fun onSearchUserClick(userId: String) {
    showProfileSheet(userId)
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

  /** Clears the download completion message. */
  fun clearDownloadComplete() {
    _showDownloadComplete = false
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

  /** Initializes the TileStore for offline map caching. */
  fun initializeTileStore() {
    viewModelScope.launch(ioDispatcher) {
      try {
        TileStoreManagerProvider.getInstance()
        // TileStore is initialized in the provider's getInstance()
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Failed to initialize TileStore", e)
        withContext(mainDispatcher) { _errorMessage = "Failed to initialize offline map storage" }
      }
    }
  }

  /** Clears all recent items history. */
  fun clearRecentSearches() {
    searchStateController.clearRecentSearches()
  }

  fun showMemoryForm(event: Event) {
    _memoryFormInitialEvent = event
    _previousSheetState = bottomSheetState
    _showMemoryForm = true
    _currentBottomSheetScreen = BottomSheetScreen.MEMORY_FORM
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideMemoryForm() {
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
    // clear any prefilled event selection
    _memoryFormInitialEvent = null
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

  fun showEditEventForm() {
    _previousSheetState = bottomSheetState
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.EDIT_EVENT
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideEditEventForm() {
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

  fun onEditEventCancel() {
    hideEditEventForm()
    restorePreviousSheetState()
  }

  override fun onCleared() {
    super.onCleared()
    cameraController.clearCallbacks()

    // Stop event listeners and observers to prevent leaks
    eventStateController.stopListeners()
    eventStateController.stopObserving()

    // Cancel any active offline downloads to prevent resource leaks
    try {
      offlineRegionManager.cancelActiveDownload()
      eventBasedOfflineRegionManager?.stopObserving()
      downloadCompleteDismissJob?.cancel()
    } catch (e: Exception) {
      Log.e("MapScreenViewModel", "Failed to cancel offline download", e)
    }

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
    // Save current sheet state before opening event detail
    _sheetStateBeforeEvent = bottomSheetState
    _selectedEvent = event
    _organizerState = OrganizerState.Loading

    // Try to fetch the name of the owner
    viewModelScope.launch {
      try {
        val ownerProfile = userProfileRepository.getUserProfile(event.ownerId)
        _organizerState =
            if (ownerProfile?.name != null) {
              OrganizerState.Loaded(userId = event.ownerId, name = ownerProfile.name)
            } else {
              OrganizerState.Error
            }
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Error loading organizer profile", e)
        _organizerState = OrganizerState.Error
      }
    }

    setBottomSheetState(BottomSheetState.MEDIUM)

    viewModelScope.launch { cameraController.centerOnEvent(event, forceZoom) }
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
    // Clear focus to hide keyboard before showing event detail
    clearSearchFieldFocus()
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

  /**
   * Handles deep link navigation to a specific event.
   *
   * Finds the event by ID and displays its details with the bottom sheet.
   *
   * @param eventId The UID of the event to display
   */
  fun onDeepLinkEvent(eventId: String) {
    pendingDeepLinkEventId = eventId
    deepLinkFetchAttempted = false
    viewModelScope.launch { resolvePendingDeepLinkEvent() }
  }

  /** Retry deep link handling after events refresh. */
  fun onEventsUpdated() {
    if (pendingDeepLinkEventId == null) return
    viewModelScope.launch { resolvePendingDeepLinkEvent() }
  }

  private suspend fun resolvePendingDeepLinkEvent() {
    val targetId = pendingDeepLinkEventId ?: return
    val event =
        eventStateController.allEvents.find { it.uid == targetId } ?: fetchDeepLinkEvent(targetId)
    if (event != null) {
      pendingDeepLinkEventId = null
      deepLinkFetchAttempted = false
      // Expose event via state for UI to handle navigation
      _resolvedDeepLinkEvent = event
    }
  }

  /** Clears the resolved deep link event after UI consumes it. */
  fun clearResolvedDeepLinkEvent() {
    _resolvedDeepLinkEvent = null
  }

  private suspend fun fetchDeepLinkEvent(eventId: String): Event? {
    if (deepLinkFetchAttempted) return null
    deepLinkFetchAttempted = true
    return withContext(ioDispatcher) {
      runCatching { eventRepository.getEvent(eventId) }
          .onFailure { Log.i("MapScreenViewModel", "Deep link event not found: $eventId", it) }
          .getOrNull()
    }
  }

  fun closeEventDetail() {
    _selectedEvent = null
    _organizerState = OrganizerState.Loaded(userId = "", name = "")

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
   * Requires location permission. If permission is not granted, sets an error message instead of
   * using default location.
   */
  fun toggleDirections(event: Event) {
    val currentState = directionViewModel.directionState

    if (currentState is DirectionState.Displayed) {
      directionViewModel.clearDirection()
    } else {
      // Check if location permission is granted
      if (!hasLocationPermission) {
        _errorMessage = "Location permission is required to get directions"
        return
      }

      val userLoc = currentLocation
      val userLocation =
          if (userLoc != null) {
            Point.fromLngLat(userLoc.longitude, userLoc.latitude)
          } else {
            // If permission is granted but location not yet available, show message
            _errorMessage = "Waiting for location... Please try again in a moment"
            return
          }
      val eventLocation = Point.fromLngLat(event.location.longitude, event.location.latitude)

      directionViewModel.requestDirections(userLocation, eventLocation, userLoc)
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

  fun loadOwnedEvents() {
    eventStateController.loadOwnedEvents()
  }

  var eventPendingDeletion by mutableStateOf<Event?>(null)
  var showDeleteDialog by mutableStateOf(false)

  // Profile sheet state
  private var _profileSheetUserId by mutableStateOf<String?>(null)
  val profileSheetUserId: String?
    get() = _profileSheetUserId

  fun requestDeleteEvent(event: Event) {
    eventPendingDeletion = event
    showDeleteDialog = true
  }

  fun cancelDelete() {
    eventPendingDeletion = null
    showDeleteDialog = false
  }

  fun showProfileSheet(userId: String) {
    // Clear selected event to switch UI from EventDetailSheet to BottomSheetContent
    _selectedEvent = null

    // Clear any navigation state
    _sheetStateBeforeEvent = null
    _cameFromSearch = false
    wasEditingBeforeEvent = false

    _profileSheetUserId = userId
    _previousSheetState = bottomSheetState
    _currentBottomSheetScreen = BottomSheetScreen.PROFILE_SHEET
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideProfileSheet() {
    _profileSheetUserId = null
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
    restorePreviousSheetState()
  }

  fun onProfileSheetEventClick(event: Event) {
    _profileSheetUserId = null
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT

    onEventPinClicked(event, forceZoom = true)
  }

  /** Called when closing event detail */
  fun closeEventDetailWithNavigation() {
    closeEventDetail()
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
  val connectivityService = ConnectivityServiceProvider.getInstance(appContext)

  val mapScreenViewModel: MapScreenViewModel = viewModel {
    MapScreenViewModel(
        initialSheetState = initialSheetState,
        sheetConfig = sheetConfig,
        connectivityService = connectivityService,
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

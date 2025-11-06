package com.swent.mapin.ui.map

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.swent.mapin.model.PreferencesRepositoryProvider
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.LocalEventRepository
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.event.rememberEventViewModel
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Assisted by AI

/**
 * ViewModel for the map screen, managing map interactions, bottom sheet state, event data, and
 * memory creation. Integrates with Firebase for authentication and data persistence, and uses
 * Compose for reactive state management.
 *
 * @param initialSheetState The initial state of the bottom sheet (e.g., COLLAPSED, MEDIUM, FULL).
 * @param sheetConfig Configuration for bottom sheet heights and behavior.
 * @param onClearFocus Callback to clear focus from the search bar.
 * @param applicationContext The application context for accessing preferences and resources.
 * @param eventViewModel ViewModel for managing event-related data and operations.
 * @param memoryRepository Repository for memory-related operations (defaults to
 *   [MemoryRepositoryProvider.getRepository]).
 * @param filterViewModel ViewModel for managing event filters.
 * @param auth Firebase authentication instance (defaults to [FirebaseAuth.getInstance]).
 * @param userProfileRepository Repository for user profile data (defaults to
 *   [UserProfileRepository]).
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    private val onClearFocus: () -> Unit,
    private val applicationContext: Context,
    private val eventViewModel: EventViewModel,
    private val memoryRepository: com.swent.mapin.model.memory.MemoryRepository =
        MemoryRepositoryProvider.getRepository(),
    private val filterViewModel: FiltersSectionViewModel,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository()
) : ViewModel() {

  private var authListener: FirebaseAuth.AuthStateListener? = null

  // Bottom sheet state
  private var _bottomSheetState by mutableStateOf(initialSheetState)
  val bottomSheetState: BottomSheetState
    get() = _bottomSheetState

  private var _fullEntryKey by mutableIntStateOf(0)
  val fullEntryKey: Int
    get() = _fullEntryKey

  var currentSheetHeight by mutableStateOf(sheetConfig.collapsedHeight)

  // Search input state
  private var _searchQuery by mutableStateOf("")
  val searchQuery: String
    get() = _searchQuery

  private var _shouldFocusSearch by mutableStateOf(false)
  val shouldFocusSearch: Boolean
    get() = _shouldFocusSearch

  private var isSearchActivated by mutableStateOf(false)

  // Raw dataset loaded from repository (kept in EventViewModel)
  private var _allEvents by mutableStateOf<List<Event>>(emptyList())

  // Visible events (subscribed from EventViewModel)
  private var _events by mutableStateOf(LocalEventRepository.defaultSampleEvents())
  val events: List<Event>
    get() = _events

  // Search results for bottom sheet list (subscribed)
  private var _searchResults by mutableStateOf<List<Event>>(emptyList())
  val searchResults: List<Event>
    get() = _searchResults

  val isSearchMode: Boolean
    get() = isSearchActivated && _bottomSheetState == BottomSheetState.FULL && !showMemoryForm

  // Map interaction tracking
  private var _mediumReferenceZoom by mutableFloatStateOf(0f)
  private var isInMediumMode = false

  private var _isZooming by mutableStateOf(false)
  val isZooming: Boolean
    get() = _isZooming

  private var _lastZoom by mutableFloatStateOf(0f)
  private var hideScaleBarJob: kotlinx.coroutines.Job? = null

  // Track programmatic zooms to prevent sheet collapse during camera animations
  private var isProgrammaticZoom by mutableStateOf(false)
  private var programmaticZoomJob: kotlinx.coroutines.Job? = null

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

  // State of the bottom sheet
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

  private var _isSavingMemory by mutableStateOf(false)
  val isSavingMemory: Boolean
    get() = _isSavingMemory

  // Event catalog for memory linking (subscribed)
  private var _availableEvents by mutableStateOf<List<Event>>(emptyList())
  val availableEvents: List<Event>
    get() = _availableEvents

  // Joined events for bottom sheet display (subscribed)
  private var _joinedEvents by mutableStateOf<List<Event>>(emptyList())
  val joinedEvents: List<Event>
    get() = _joinedEvents

  // Saved events for bottom sheet display (subscribed)
  private var _savedEvents by mutableStateOf<List<Event>>(emptyList())
  val savedEvents: List<Event>
    get() = _savedEvents

  // Saved events ids for quick lookup (subscribed)
  private var _savedEventIds by mutableStateOf<Set<String>>(emptySet())
  private val savedEventIds: Set<String>
    get() = _savedEventIds

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

  var onCenterCamera: ((Event, Boolean) -> Unit)? = null

  // Track if we came from search mode to return to it after closing event detail
  private var _cameFromSearch by mutableStateOf(false)

  // User avatar URL for profile button (can be HTTP URL or preset icon ID)
  private var _avatarUrl by mutableStateOf<String?>(null)
  val avatarUrl: String?
    get() = _avatarUrl

  val directionViewModel = DirectionViewModel()

  init {
    loadMapStylePreference()
    eventViewModel.loadInitialSamples()
    subscribeToEventFlows()
    loadAllEvents()
    observeFilters()
    registerAuthStateListener()
    loadUserProfile()
  }

  /**
   * Observes changes to the filter state and updates the displayed events accordingly. Applies
   * filters from [filterViewModel] and updates [_events] and [_searchResults].
   */
  private fun observeFilters() {
    viewModelScope.launch {
      filterViewModel.filters.collect { filters ->
        try {
          eventViewModel.getFilteredEvents(filters)
        } catch (e: Exception) {
          Log.e("MapScreenViewModel", "Error applying filters", e)
          _errorMessage = "Failed to apply filters: ${e.message}"
          val localFilteredEvents = LocalEventRepository().getFilteredEvents(filters)
          _events = localFilteredEvents
          _searchResults = localFilteredEvents
        }
      }
    }
  }

  /**
   * Registers a Firebase authentication state listener to handle user login/logout events. Updates
   * user-specific data (e.g., saved events, joined events, avatar) based on authentication state.
   */
  private fun registerAuthStateListener() {
    if (authListener != null) return
    authListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
          val uid = firebaseAuth.currentUser?.uid
          if (uid == null) {
            // clear user-scoped data
            _savedEvents = emptyList()
            _savedEventIds = emptySet()
            _joinedEvents = emptyList()
            _availableEvents = emptyList()
            _avatarUrl = null
          } else {
            // delegate loading to EventViewModel
            eventViewModel.getSavedEventIds(uid)
            eventViewModel.getSavedEvents(uid)
            // joined events may be computed by repository; reuse available API if exists
          }
        }
    auth.addAuthStateListener(authListener!!)
  }

  /**
   * Subscribes to event data flows from [eventViewModel] to keep UI state updated. Collects events,
   * search results, saved events, joined events, and saved event IDs.
   */
  private fun subscribeToEventFlows() {
    viewModelScope.launch {
      eventViewModel.events.collect { list ->
        _events = list
        _searchResults = list
        _allEvents = list
      }
    }
    viewModelScope.launch {
      eventViewModel.availableEvents.collect { list -> _availableEvents = list }
    }
    viewModelScope.launch { eventViewModel.searchResults.collect { list -> _searchResults = list } }
    viewModelScope.launch { eventViewModel.savedEvents.collect { list -> _savedEvents = list } }
    viewModelScope.launch { eventViewModel.joinedEvents.collect { list -> _joinedEvents = list } }
    viewModelScope.launch { eventViewModel.savedEventIds.collect { ids -> _savedEventIds = ids } }
  }

  /**
   * Loads the user profile, including the avatar URL, for the currently authenticated user. Sets
   * [_avatarUrl] to null if no user is authenticated or if loading fails.
   */
  fun loadUserProfile() {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      _avatarUrl = null
      return
    }
    viewModelScope.launch {
      try {
        val profile = userProfileRepository.getUserProfile(uid)
        _avatarUrl = profile?.avatarUrl
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Failed loading profile", e)
      }
    }
  }

  /**
   * Allows the current user to join the selected event. Updates [_selectedEvent] and
   * [_errorMessage] based on the operation's success or failure.
   */
  fun joinEvent() {
    viewModelScope.launch {
      val event = _selectedEvent ?: return@launch
      val currentUserId = auth.currentUser?.uid
      if (currentUserId == null) {
        _errorMessage = "You must be signed in to join events"
        return@launch
      }
      _errorMessage = null
      val updated = eventViewModel.joinEventForUser(currentUserId, event.uid)
      if (updated != null) {
        _selectedEvent = updated
      } else {
        _errorMessage = "Failed to join event"
      }
    }
  }

  /**
   * Removes the current user from the selected event. Updates [_selectedEvent] and [_errorMessage]
   * based on the operation's success or failure.
   */
  fun unregisterFromEvent() {
    val event = _selectedEvent ?: return
    val currentUserId = auth.currentUser?.uid ?: return
    viewModelScope.launch {
      _errorMessage = null
      val updated = eventViewModel.unregisterUserFromEvent(currentUserId, event.uid)
      if (updated != null) {
        _selectedEvent = updated
      } else {
        _errorMessage = "Failed to unregister"
      }
    }
  }

  /**
   * Saves the selected event for the current user to view later. Updates [_errorMessage] if the
   * user is not authenticated or the operation fails.
   */
  fun saveEventForLater() {
    viewModelScope.launch {
      val eventUid = _selectedEvent?.uid ?: return@launch
      val currentUserId = auth.currentUser?.uid
      if (currentUserId == null) {
        _errorMessage = "You must be signed in to save events"
        return@launch
      }
      _errorMessage = null
      // delegate to EventViewModel (it updates saved flows)
      eventViewModel.saveEventForUser(currentUserId, eventUid)
    }
  }

  /**
   * Removes the selected event from the current user's saved events. Updates [_errorMessage] if the
   * user is not authenticated or the operation fails.
   */
  fun unsaveEventForLater() {
    val eventUid = _selectedEvent?.uid ?: return
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _errorMessage = "You must be signed in to unsave events"
      return
    }
    _errorMessage = null
    eventViewModel.unsaveEventForUser(currentUserId, eventUid)
  }

  /**
   * Loads the user's preferred map style (e.g., STANDARD, SATELLITE) from preferences. Falls back
   * to [MapStyle.STANDARD] if loading fails.
   */
  private fun loadMapStylePreference() {
    try {
      kotlinx.coroutines.runBlocking {
        val preferencesRepository = PreferencesRepositoryProvider.getInstance(applicationContext)
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
   * Loads all events from the [eventViewModel] to initialize the event list. Logs an error if the
   * fetch fails but does not update UI state directly.
   */
  private fun loadAllEvents() {
    viewModelScope.launch {
      try {
        eventViewModel.getAllEvents()
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Failed initial events fetch: ${e.message}")
      }
    }
  }

  /**
   * Applies filters or search queries to update the displayed events. Fetches filtered or searched
   * events from [eventViewModel] and updates [_events] and [_searchResults].
   */
  private fun applyFilters() {
    viewModelScope.launch {
      try {
        val finalList =
            if (_searchQuery.isBlank()) {
              println("MapScreenViewModel: Applying filters: ${filterViewModel.filters.value}")
              eventViewModel.fetchFilteredEvents(filterViewModel.filters.value)
            } else {
              eventViewModel.fetchSearchedEvents(_searchQuery)
            }
        _searchResults = finalList
        _events = finalList
        println("MapScreenViewModel: Applied filters, got ${finalList.size} events")
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Error applying search query or filters", e)
        _errorMessage = "Failed to apply filters: ${e.message}"
        println("MapScreenViewModel: Applying filters: ${filterViewModel.filters.value}")
        val localFiltered = LocalEventRepository().getFilteredEvents(filterViewModel.filters.value)
        _events = localFiltered
        _searchResults = localFiltered
      }
    }
  }

  /**
   * Updates the zoom state and manages the visibility of the scale bar. Temporarily sets
   * [_isZooming] to true and hides the scale bar after a delay.
   *
   * @param newZoom The new zoom level of the map.
   */
  fun onZoomChange(newZoom: Float) {
    if (abs(newZoom - _lastZoom) > 0.01f) {
      _isZooming = true
      _lastZoom = newZoom

      hideScaleBarJob?.cancel()
      hideScaleBarJob =
          viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _isZooming = false
          }
    }
  }

  /**
   * Updates the reference zoom level when in medium bottom sheet mode.
   *
   * @param zoom The current zoom level to set as the reference.
   */
  fun updateMediumReferenceZoom(zoom: Float) {
    if (isInMediumMode) {
      _mediumReferenceZoom = zoom
    }
  }

  /**
   * Checks if the map zoom interaction exceeds the threshold for collapsing the bottom sheet.
   *
   * @param currentZoom The current zoom level of the map.
   * @return True if the zoom change exceeds the threshold and is not programmatic, false otherwise.
   */
  fun checkZoomInteraction(currentZoom: Float): Boolean {
    if (!isInMediumMode) return false
    if (isProgrammaticZoom) return false

    val zoomDelta = abs(currentZoom - _mediumReferenceZoom)
    return zoomDelta >= MapConstants.ZOOM_CHANGE_THRESHOLD
  }

  /**
   * Checks if a touch event is close to the bottom sheet, affecting its behavior.
   *
   * @param touchY The Y-coordinate of the touch event in pixels.
   * @param sheetTopY The Y-coordinate of the top of the bottom sheet in pixels.
   * @param densityDpi The device's DPI for converting DP to pixels.
   * @return True if the touch is within the proximity threshold of the bottom sheet, false
   *   otherwise.
   */
  fun checkTouchProximityToSheet(touchY: Float, sheetTopY: Float, densityDpi: Int): Boolean {
    if (!isInMediumMode) return false

    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f
    val distance = abs(touchY - sheetTopY)

    return distance <= thresholdPx
  }

  /**
   * Calculates the target bottom sheet state based on its current height.
   *
   * @param currentHeightPx The current height of the bottom sheet in pixels.
   * @param collapsedPx The height of the collapsed state in pixels.
   * @param mediumPx The height of the medium state in pixels.
   * @param fullPx The height of the full state in pixels.
   * @return The target [BottomSheetState] based on the current height.
   */
  fun calculateTargetState(
      currentHeightPx: Float,
      collapsedPx: Float,
      mediumPx: Float,
      fullPx: Float
  ): BottomSheetState {
    return when {
      currentHeightPx < (collapsedPx + mediumPx) / 2f -> BottomSheetState.COLLAPSED
      currentHeightPx < (mediumPx + fullPx) / 2f -> BottomSheetState.MEDIUM
      else -> BottomSheetState.FULL
    }
  }

  /**
   * Returns the height for a given bottom sheet state.
   *
   * @param state The [BottomSheetState] to get the height for.
   * @return The height in [Dp] for the specified state.
   */
  fun getHeightForState(state: BottomSheetState): Dp {
    return when (state) {
      BottomSheetState.COLLAPSED -> sheetConfig.collapsedHeight
      BottomSheetState.MEDIUM -> sheetConfig.mediumHeight
      BottomSheetState.FULL -> sheetConfig.fullHeight
    }
  }

  /**
   * Sets the bottom sheet state and updates related UI states.
   *
   * @param target The target [BottomSheetState] to set.
   */
  fun setBottomSheetState(target: BottomSheetState) {
    if (target == BottomSheetState.FULL && _bottomSheetState != BottomSheetState.FULL) {
      _fullEntryKey += 1
    }

    isInMediumMode = target == BottomSheetState.MEDIUM
    if (target != BottomSheetState.FULL &&
        _bottomSheetState == BottomSheetState.FULL &&
        !_showMemoryForm) {
      resetSearchState()
    }

    _bottomSheetState = target
  }

  /**
   * Updates the search query and applies filters to refresh the event list. Automatically expands
   * the bottom sheet to FULL if not already expanded.
   *
   * @param query The new search query string.
   */
  fun onSearchQueryChange(query: String) {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
    isSearchActivated = true
    _searchQuery = query
    applyFilters()
  }

  /** Handles a tap on the search bar, activating search mode and expanding the bottom sheet. */
  fun onSearchTap() {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
    isSearchActivated = true
  }

  /** Marks the search bar focus as handled, resetting the focus request state. */
  fun onSearchFocusHandled() {
    _shouldFocusSearch = false
  }

  /** Clears the search state, resetting the query and deactivating search mode. */
  private fun resetSearchState() {
    isSearchActivated = false
    _shouldFocusSearch = false
    onClearFocus()
    if (_searchQuery.isNotEmpty()) _searchQuery = ""
    applyFilters()
  }

  /** Clears the current search query and collapses the bottom sheet to medium state. */
  fun onClearSearch() {
    resetSearchState()
    setBottomSheetState(BottomSheetState.MEDIUM)
  }

  /**
   * Sets the map style and saves it to preferences.
   *
   * @param style The [MapStyle] to set (e.g., STANDARD, SATELLITE, HEATMAP).
   */
  fun setMapStyle(style: MapStyle) {
    _mapStyle = style
    viewModelScope.launch {
      try {
        val preferencesRepository = PreferencesRepositoryProvider.getInstance(applicationContext)
        val styleString =
            when (style) {
              MapStyle.SATELLITE -> "satellite"
              MapStyle.STANDARD -> "standard"
              MapStyle.HEATMAP -> "standard"
            }
        preferencesRepository.setMapStyle(styleString)
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Failed to save map style preference: ${e.message}")
      }
    }
  }

  /** Clears the current error message. */
  fun clearError() {
    _errorMessage = null
  }

  /** Shows the memory creation form and sets the bottom sheet to FULL. */
  fun showMemoryForm() {
    _previousSheetState = _bottomSheetState
    _showMemoryForm = true
    _currentBottomSheetScreen = BottomSheetScreen.MEMORY_FORM
    setBottomSheetState(BottomSheetState.FULL)
  }

  /** Hides the memory creation form and restores the previous bottom sheet state. */
  fun hideMemoryForm() {
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
  }

  /** Shows the add event form and sets the bottom sheet to FULL. */
  fun showAddEventForm() {
    _previousSheetState = _bottomSheetState
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.ADD_EVENT
    setBottomSheetState(BottomSheetState.FULL)
  }

  /** Hides the add event form and restores the previous bottom sheet state. */
  fun hideAddEventForm() {
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
  }

  /**
   * Saves a memory with the provided form data, including media uploads. Requires user
   * authentication and updates the UI state on success or failure.
   *
   * @param formData The memory form data containing title, description, event ID, media URIs, and
   *   visibility settings.
   */
  fun onMemorySave(formData: MemoryFormData) {
    viewModelScope.launch {
      _isSavingMemory = true
      _errorMessage = null
      try {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
          _errorMessage = "You must be signed in to create a memory"
          Log.e("MapScreenViewModel", "Cannot save memory: User not authenticated")
          return@launch
        }

        val mediaUrls = uploadMediaFiles(formData.mediaUris, currentUserId)
        val memory =
            Memory(
                uid = memoryRepository.getNewUid(),
                title = formData.title,
                description = formData.description,
                eventId = formData.eventId,
                ownerId = currentUserId,
                isPublic = formData.isPublic,
                createdAt = Timestamp.now(),
                mediaUrls = mediaUrls,
                taggedUserIds = formData.taggedUserIds)
        memoryRepository.addMemory(memory)
        Log.d("MapScreenViewModel", "Memory saved successfully")
        hideMemoryForm()
        restorePreviousSheetState()
      } catch (e: Exception) {
        _errorMessage = "Failed to save memory: ${e.message ?: "Unknown error"}"
        Log.e("MapScreenViewModel", "Error saving memory", e)
      } finally {
        _isSavingMemory = false
      }
    }
  }

  /**
   * Uploads media files to Firebase Storage and returns their download URLs.
   *
   * @param uris The list of media URIs to upload.
   * @param userId The ID of the user uploading the media.
   * @return A list of download URLs for the uploaded media files.
   */
  private suspend fun uploadMediaFiles(uris: List<Uri>, userId: String): List<String> {
    if (uris.isEmpty()) return emptyList()
    val downloadUrls = mutableListOf<String>()
    for (uri in uris) {
      try {
        val extension =
            applicationContext.contentResolver.getType(uri)?.split("/")?.lastOrNull() ?: "jpg"
        val filename =
            "memories/$userId/${UUID.randomUUID()}_${System.currentTimeMillis()}.$extension"
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child(filename)
        fileRef.putFile(uri).await()
        val downloadUrl = fileRef.downloadUrl.await().toString()
        downloadUrls.add(downloadUrl)
        Log.d("MapScreenViewModel", "Uploaded media file successfully")
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Failed to upload media file", e)
      }
    }
    return downloadUrls
  }

  /** Cancels the memory creation form and restores the previous bottom sheet state. */
  fun onMemoryCancel() {
    hideMemoryForm()
    restorePreviousSheetState()
  }

  /** Cancels the add event form and restores the previous bottom sheet state. */
  fun onAddEventCancel() {
    hideAddEventForm()
    restorePreviousSheetState()
  }

  /**
   * Cleans up resources when the ViewModel is cleared, canceling coroutines and removing the auth
   * listener.
   */
  override fun onCleared() {
    super.onCleared()
    hideScaleBarJob?.cancel()
    programmaticZoomJob?.cancel()
    authListener?.let { auth.removeAuthStateListener(it) }
    authListener = null
  }

  /** Restores the previous bottom sheet state, if available. */
  private fun restorePreviousSheetState() {
    _previousSheetState?.let { setBottomSheetState(it) }
    _previousSheetState = null
  }

  /**
   * Sets the list of events and applies filters to update the displayed events.
   *
   * @param newEvents The new list of events to set.
   */
  fun setEvents(newEvents: List<Event>) {
    _allEvents = newEvents
    applyFilters()
  }

  /**
   * Sets the selected bottom sheet tab (e.g., SAVED_EVENTS, JOINED_EVENTS).
   *
   * @param tab The [BottomSheetTab] to select.
   */
  fun setBottomSheetTab(tab: BottomSheetTab) {
    _selectedBottomSheetTab = tab
  }

  /**
   * Handles a click on an event pin, centering the map on the event and updating the bottom sheet.
   *
   * @param event The [Event] associated with the clicked pin.
   * @param forceZoom Whether to force a zoom animation on the map (defaults to false).
   */
  fun onEventPinClicked(event: Event, forceZoom: Boolean = false) {
    viewModelScope.launch {
      _selectedEvent = event
      _organizerName = "User ${event.ownerId.take(6)}"
      setBottomSheetState(BottomSheetState.MEDIUM)

      isProgrammaticZoom = true
      onCenterCamera?.invoke(event, forceZoom)

      programmaticZoomJob?.cancel()
      programmaticZoomJob =
          viewModelScope.launch {
            kotlinx.coroutines.delay(1100)
            isProgrammaticZoom = false
          }
    }
  }

  /**
   * Handles a click on an event from the search results, marking the origin as search mode.
   *
   * @param event The [Event] clicked from the search results.
   */
  fun onEventClickedFromSearch(event: Event) {
    _cameFromSearch = true
    onEventPinClicked(event, forceZoom = true)
  }

  /**
   * Closes the event detail view and restores the appropriate bottom sheet state. Returns to search
   * mode if the event was clicked from search results.
   */
  fun closeEventDetail() {
    _selectedEvent = null
    _organizerName = ""
    directionViewModel.clearDirection()

    if (_cameFromSearch) {
      _cameFromSearch = false
      _searchQuery = ""
      isSearchActivated = true
      _shouldFocusSearch = false
      setBottomSheetState(BottomSheetState.FULL)
      applyFilters()
    } else {
      setBottomSheetState(BottomSheetState.COLLAPSED)
    }
  }

  /**
   * Toggles the display of directions to the selected event. Either requests new directions or
   * clears existing ones based on the current state.
   *
   * @param event The [Event] for which to toggle directions.
   */
  fun toggleDirections(event: Event) {
    val currentState = directionViewModel.directionState

    if (currentState is DirectionState.Displayed) {
      directionViewModel.clearDirection()
    } else {
      val userLocation =
          Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE)
      val eventLocation = Point.fromLngLat(event.location.longitude, event.location.latitude)
      directionViewModel.requestDirections(userLocation, eventLocation)
    }
  }

  /** Shows the share dialog for the selected event. */
  fun showShareDialog() {
    _showShareDialog = true
  }

  /** Dismisses the share dialog. */
  fun dismissShareDialog() {
    _showShareDialog = false
  }

  /**
   * Checks if the current user is participating in the selected event.
   *
   * @return True if the user is a participant, false otherwise.
   */
  fun isUserParticipating(): Boolean {
    val currentUserId = auth.currentUser?.uid ?: return false
    return _selectedEvent?.participantIds?.contains(currentUserId) ?: false
  }

  /**
   * Checks if the current user is participating in a specific event.
   *
   * @param event The [Event] to check participation for.
   * @return True if the user is a participant, false otherwise.
   */
  fun isUserParticipating(event: Event): Boolean {
    val currentUserId = auth.currentUser?.uid ?: return false
    return event.participantIds.contains(currentUserId)
  }

  /**
   * Checks if a specific event is saved by the current user.
   *
   * @param event The [Event] to check.
   * @return True if the event is saved, false otherwise.
   */
  fun isEventSaved(event: Event): Boolean {
    return savedEventIds.contains(event.uid)
  }

  /**
   * Handles a click on an event from the bottom sheet tabs (e.g., saved or joined events).
   *
   * @param event The [Event] clicked from the tab.
   */
  fun onTabEventClicked(event: Event) {
    onEventPinClicked(event)
  }
}

/**
 * Composable to create and remember a [MapScreenViewModel] instance.
 *
 * @param sheetConfig Configuration for bottom sheet heights and behavior.
 * @param initialSheetState The initial state of the bottom sheet (defaults to
 *   [BottomSheetState.COLLAPSED]).
 * @param viewModelStoreOwner The ViewModel store owner (defaults to
 *   [LocalViewModelStoreOwner.current]).
 * @return A [MapScreenViewModel] instance.
 */
@Composable
fun rememberMapScreenViewModel(
    sheetConfig: BottomSheetConfig,
    initialSheetState: BottomSheetState = BottomSheetState.COLLAPSED,
    viewModelStoreOwner: ViewModelStoreOwner =
        LocalViewModelStoreOwner.current ?: error("No ViewModelStoreOwner provided")
): MapScreenViewModel {
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val appContext = context.applicationContext

  val filterViewModel: FiltersSectionViewModel =
      viewModel(viewModelStoreOwner = viewModelStoreOwner, key = "FiltersSectionViewModel")

  // Reuse the shared EventViewModel instance
  val eventViewModel: EventViewModel = rememberEventViewModel(viewModelStoreOwner)

  return viewModel(
      viewModelStoreOwner = viewModelStoreOwner,
      key = "MapScreenViewModel",
      factory =
          object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
              return MapScreenViewModel(
                  initialSheetState = initialSheetState,
                  sheetConfig = sheetConfig,
                  onClearFocus = { focusManager.clearFocus(force = true) },
                  applicationContext = appContext,
                  eventViewModel = eventViewModel,
                  filterViewModel = filterViewModel)
                  as T
            }
          })
}

/**
 * Converts a list of events to a GeoJSON string for map rendering.
 *
 * @param events The list of events to convert.
 * @return A GeoJSON string representing the events as point features with participant count
 *   weights.
 */
fun eventsToGeoJson(events: List<Event>): String {
  val features =
      events.map { event ->
        Feature.fromGeometry(
            Point.fromLngLat(event.location.longitude, event.location.latitude),
            JsonObject().apply { addProperty("weight", event.participantIds.size) })
      }
  return FeatureCollection.fromFeatures(features).toJson()
}

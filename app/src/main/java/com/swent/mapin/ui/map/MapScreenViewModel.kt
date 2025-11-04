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
import androidx.lifecycle.viewModelScope
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
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.event.LocalEventRepository
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

  // Raw dataset loaded from repository (or empty until loaded)
  private var _allEvents by mutableStateOf<List<Event>>(emptyList())

  // Visible events for map (after filtering)
  private var _events by mutableStateOf(LocalEventRepository.defaultSampleEvents())
  val events: List<Event>
    get() = _events

  // Search results for bottom sheet list
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

  private var _isSavingMemory by mutableStateOf(false)
  val isSavingMemory: Boolean
    get() = _isSavingMemory

  // Event catalog for memory linking
  private var _availableEvents by mutableStateOf<List<Event>>(emptyList())
  val availableEvents: List<Event>
    get() = _availableEvents

  // Joined events for bottom sheet display
  private var _joinedEvents by mutableStateOf<List<Event>>(emptyList())
  val joinedEvents: List<Event>
    get() = _joinedEvents

  // Saved events for bottom sheet display
  private var _savedEvents by mutableStateOf<List<Event>>(emptyList())
  val savedEvents: List<Event>
    get() = _savedEvents

  // Saved events ids for quick lookup
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

  // Tag filtering
  private var _selectedTags by mutableStateOf<Set<String>>(emptySet())
  val selectedTags: Set<String>
    get() = _selectedTags

  private var _topTags by mutableStateOf<List<String>>(emptyList())
  val topTags: List<String>
    get() = _topTags

  // User avatar URL for profile button (can be HTTP URL or preset icon ID)
  private var _avatarUrl by mutableStateOf<String?>(null)
  val avatarUrl: String?
    get() = _avatarUrl

  val directionViewModel = DirectionViewModel()

  init {
    // Load map style preference
    loadMapStylePreference()

    // Initialize with sample events quickly, then load remote data
    loadInitialSamples()
    // Preload events so the form has immediate data
    loadEvents()
    loadJoinedEvents()
    loadSavedEvents()
    loadSavedEventIds()
    _topTags = getTopTags()
    // Preload events both for searching and memory linking
    loadAllEvents()
    loadParticipantEvents()
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
            _savedEvents = emptyList()
            _savedEventIds = emptySet()
            _joinedEvents = emptyList()
            _avatarUrl = null
          } else {
            // Signed in → (re)load user-scoped data
            loadSavedEvents()
            loadSavedEventIds()
            loadJoinedEvents()
            loadParticipantEvents()
            loadUserProfile()
          }
        }
    auth.addAuthStateListener(authListener!!)
  }

  /** Loads initial sample events synchronously for immediate UI responsiveness. */
  private fun loadInitialSamples() {
    _events = LocalEventRepository.defaultSampleEvents()
    _searchResults = _events
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

  /** Loads primary events list for immediate UI usage with fallback to local samples. */
  private fun loadEvents() {
    viewModelScope.launch {
      try {
        val events = eventRepository.getAllEvents()
        _events = events
        _searchResults = events
      } catch (e: Exception) {
        Log.w("MapScreenViewModel", "Failed to load events from repository, using samples", e)
        _events = LocalEventRepository.defaultSampleEvents()
        _searchResults = _events
      }
    }
  }

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

  fun updateMediumReferenceZoom(zoom: Float) {
    if (isInMediumMode) {
      _mediumReferenceZoom = zoom
    }
  }

  fun checkZoomInteraction(currentZoom: Float): Boolean {
    if (!isInMediumMode) return false
    // Don't collapse during programmatic zooms (e.g., when centering on an event)
    if (isProgrammaticZoom) return false

    val zoomDelta = abs(currentZoom - _mediumReferenceZoom)
    return zoomDelta >= MapConstants.ZOOM_CHANGE_THRESHOLD
  }

  fun checkTouchProximityToSheet(touchY: Float, sheetTopY: Float, densityDpi: Int): Boolean {
    if (!isInMediumMode) return false

    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f
    val distance = abs(touchY - sheetTopY)

    return distance <= thresholdPx
  }

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

  fun getHeightForState(state: BottomSheetState): Dp {
    return when (state) {
      BottomSheetState.COLLAPSED -> sheetConfig.collapsedHeight
      BottomSheetState.MEDIUM -> sheetConfig.mediumHeight
      BottomSheetState.FULL -> sheetConfig.fullHeight
    }
  }

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

  fun onSearchQueryChange(query: String) {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
    isSearchActivated = true
    _searchQuery = query
    applyFilters()
  }

  fun onSearchTap() {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
    isSearchActivated = true
  }

  fun onSearchFocusHandled() {
    _shouldFocusSearch = false
  }

  private fun resetSearchState() {
    isSearchActivated = false
    _shouldFocusSearch = false
    onClearFocus()
    if (_searchQuery.isNotEmpty()) _searchQuery = ""
    applyFilters()
  }

  fun onClearSearch() {
    resetSearchState()
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

  /** Loads all events for search and map display, falling back to local data on error. */
  private fun loadAllEvents() {
    viewModelScope.launch {
      try {
        val events = eventRepository.getAllEvents()
        applyEventsDataset(events)
      } catch (primary: Exception) {
        Log.e("MapScreenViewModel", "Error loading events from repository", primary)
        try {
          val localEvents = EventRepositoryProvider.createLocalRepository().getAllEvents()
          applyEventsDataset(localEvents)
        } catch (fallback: Exception) {
          Log.e("MapScreenViewModel", "Failed to load local events fallback", fallback)
          _allEvents = emptyList()
          applyFilters()
        }
      }
    }
  }

  private fun applyEventsDataset(events: List<Event>) {
    _allEvents = events
    loadJoinedEvents()
    applyFilters()
  }

  private fun applyFilters() {
    val base = _allEvents.ifEmpty { LocalEventRepository.defaultSampleEvents() }

    val tagFiltered =
        if (_selectedTags.isEmpty()) {
          base
        } else {
          base.filter { event ->
            event.tags.any { tag ->
              _selectedTags.any { sel -> tag.equals(sel, ignoreCase = true) }
            }
          }
        }

    val finalList =
        if (_searchQuery.isBlank()) {
          tagFiltered
        } else {
          filterEvents(_searchQuery, tagFiltered)
        }

    _searchResults = if (_searchQuery.isBlank()) tagFiltered else finalList
    _events = finalList
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

  private fun loadParticipantEvents() {
    viewModelScope.launch {
      try {
        val currentUserId = auth.currentUser?.uid
        _availableEvents =
            if (currentUserId != null) eventRepository.getEventsByParticipant(currentUserId)
            else emptyList()
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Error loading events", e)
        _availableEvents = emptyList()
      }
    }
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

  private fun loadJoinedEvents() {
    val uid =
        auth.currentUser?.uid
            ?: run {
              _joinedEvents = emptyList()
              return
            }
    val base = _allEvents.ifEmpty { _events }
    _joinedEvents = base.filter { uid in it.participantIds }
  }

  private fun loadSavedEvents() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _savedEvents = emptyList()
      return
    }
    viewModelScope.launch { _savedEvents = eventRepository.getSavedEvents(currentUserId) }
  }

  private fun loadSavedEventIds() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _savedEventIds = emptySet()
      return
    }
    viewModelScope.launch { _savedEventIds = eventRepository.getSavedEventIds(currentUserId) }
  }

  fun showMemoryForm() {
    _previousSheetState = _bottomSheetState
    _showMemoryForm = true
    _currentBottomSheetScreen = BottomSheetScreen.MEMORY_FORM
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideMemoryForm() {
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
  }

  fun showAddEventForm() {
    _previousSheetState = _bottomSheetState
    _showMemoryForm = false
    _currentBottomSheetScreen = BottomSheetScreen.ADD_EVENT
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideAddEventForm() {
    _currentBottomSheetScreen = BottomSheetScreen.MAIN_CONTENT
  }

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
    hideScaleBarJob?.cancel()
    programmaticZoomJob?.cancel()

    // Remove auth listener to avoid leaks
    authListener?.let { auth.removeAuthStateListener(it) }
    authListener = null
  }

  private fun restorePreviousSheetState() {
    _previousSheetState?.let { setBottomSheetState(it) }
    _previousSheetState = null
  }

  fun setEvents(newEvents: List<Event>) {
    _allEvents = newEvents
    applyFilters()
  }

  fun toggleTagSelection(tag: String) {
    _selectedTags = if (_selectedTags.contains(tag)) _selectedTags - tag else _selectedTags + tag
    applyFilters()
  }

  fun setBottomSheetTab(tab: BottomSheetTab) {
    _selectedBottomSheetTab = tab
  }

  fun onEventPinClicked(event: Event, forceZoom: Boolean = false) {
    viewModelScope.launch {
      _selectedEvent = event
      _organizerName = "User ${event.ownerId.take(6)}"
      setBottomSheetState(BottomSheetState.MEDIUM)

      // Mark as programmatic zoom to prevent sheet collapse during camera animation
      isProgrammaticZoom = true
      onCenterCamera?.invoke(event, forceZoom)

      // Clear the flag after animation completes (500ms animation + 600ms buffer)
      programmaticZoomJob?.cancel()
      programmaticZoomJob =
          viewModelScope.launch {
            kotlinx.coroutines.delay(1100)
            isProgrammaticZoom = false
          }
    }
  }

  /**
   * Handles event clicks from search results. Focuses the pin, shows event details, and remembers
   * we came from search mode. Forces zoom to ensure the pin is visible.
   */
  fun onEventClickedFromSearch(event: Event) {
    _cameFromSearch = true
    onEventPinClicked(event, forceZoom = true)
  }

  fun closeEventDetail() {
    _selectedEvent = null
    _organizerName = ""

    // Clear directions when closing event detail
    directionViewModel.clearDirection()

    if (_cameFromSearch) {
      // Return to search mode: full sheet with cleared search, no keyboard
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
    viewModelScope.launch {
      val event = _selectedEvent ?: return@launch
      val currentUserId = auth.currentUser?.uid
      if (currentUserId == null) {
        _errorMessage = "You must be signed in to join events"
        return@launch
      }
      _errorMessage = null
      try {
        val capacity = event.capacity
        val currentAttendees = event.participantIds.size
        if (capacity != null && currentAttendees >= capacity) {
          _errorMessage = "Event is at full capacity"
          return@launch
        }
        val updatedParticipantIds =
            event.participantIds.toMutableList().apply {
              if (!contains(currentUserId)) add(currentUserId)
            }
        val updatedEvent = event.copy(participantIds = updatedParticipantIds)
        eventRepository.editEvent(event.uid, updatedEvent)
        _selectedEvent = updatedEvent
        _events = _events.map { if (it.uid == event.uid) updatedEvent else it }
        _allEvents = _allEvents.map { if (it.uid == event.uid) updatedEvent else it }
        loadJoinedEvents()
      } catch (e: Exception) {
        _errorMessage = "Failed to join event: ${e.message}"
      }
    }
  }

  fun unregisterFromEvent() {
    val event = _selectedEvent ?: return
    val currentUserId = auth.currentUser?.uid ?: return
    viewModelScope.launch {
      _errorMessage = null
      try {
        val updatedParticipantIds =
            event.participantIds.toMutableList().apply { remove(currentUserId) }
        val updatedEvent = event.copy(participantIds = updatedParticipantIds)
        eventRepository.editEvent(event.uid, updatedEvent)
        _selectedEvent = updatedEvent
        _events = _events.map { if (it.uid == event.uid) updatedEvent else it }
        loadJoinedEvents()
      } catch (e: Exception) {
        _errorMessage = "Failed to unregister: ${e.message}"
      }
    }
  }

  /** Saves the selected event for later by the current user. */
  fun saveEventForLater() {
    viewModelScope.launch {
      val eventUid = _selectedEvent?.uid ?: return@launch
      val currentUserId = auth.currentUser?.uid
      if (currentUserId == null) {
        _errorMessage = "You must be signed in to save events"
        return@launch
      }
      _errorMessage = null
      try {
        val success = eventRepository.saveEventForUser(currentUserId, eventUid)
        if (success) {
          _savedEventIds = _savedEventIds + eventUid
          loadSavedEvents()
        } else {
          _errorMessage = "Event is already saved"
        }
      } catch (e: Exception) {
        _errorMessage = "Failed to save event: ${e.message}"
      }
    }
  }

  /** Unsaves the selected event for later by the current user. */
  fun unsaveEventForLater() {
    val eventUid = _selectedEvent?.uid ?: return
    viewModelScope.launch {
      val currentUserId = auth.currentUser?.uid
      if (currentUserId == null) {
        _errorMessage = "You must be signed in to unsave events"
        return@launch
      }
      _errorMessage = null
      try {
        val success = eventRepository.unsaveEventForUser(currentUserId, eventUid)
        if (success) {
          _savedEventIds = _savedEventIds - eventUid
          loadSavedEvents()
        } else {
          _errorMessage = "Event was not saved"
        }
      } catch (e: Exception) {
        _errorMessage = "Failed to unsave: ${e.message}"
      }
    }
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

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

  // User avatar URL for profile button (can be HTTP URL or preset icon ID)
  private var _avatarUrl by mutableStateOf<String?>(null)
  val avatarUrl: String?
    get() = _avatarUrl

  init {
    // Initialize with sample events quickly, then load remote data
    loadInitialSamples()
    // Preload events for searching and memory linking
    loadAllEvents()
    loadJoinedEvents()
    loadSavedEvents()
    loadSavedEventIds()
    loadUserProfile()
    // Collect filter updates for real-time pin updates
    observeFilters()
    registerAuthStateListener()
  }

  /** Collects filter updates from FiltersSectionViewModel to update map pins in real-time. */
  private fun observeFilters() {
    viewModelScope.launch {
      filterViewModel.filters.collect { filters ->
        try {
          println("MapScreenViewModel: Applying filters: $filters")
          val filteredEvents = eventRepository.getFilteredEvents(filters)
          _events = filteredEvents
          _searchResults = filteredEvents
          println("MapScreenViewModel: Fetched ${filteredEvents.size} filtered events for $filters")
        } catch (e: Exception) {
          Log.e("MapScreenViewModel", "Error applying filters", e)
          _errorMessage = "Failed to apply filters: ${e.message}"
          println("MapScreenViewModel: Applying filters: $filters")
          val localFilteredEvents = LocalEventRepository().getFilteredEvents(filters)
          _events = localFilteredEvents
          _searchResults = localFilteredEvents
          println(
              "MapScreenViewModel: Loaded ${localFilteredEvents.size} local filtered events as fallback")
        }
      }
    }
  }

  /**
   * Reacts to Firebase auth transitions:
   * - On sign-out: clear user-scoped state (saved IDs/list, joined).
   * - On sign-in: load saved IDs/list and joined events.
   */
  private fun registerAuthStateListener() {
    if (authListener != null) return
    authListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
          val uid = firebaseAuth.currentUser?.uid
          if (uid == null) {
            _savedEvents = emptyList()
            _savedEventIds = emptySet()
            _joinedEvents = emptyList()
            _availableEvents = emptyList()
            _avatarUrl = null
          } else {
            loadSavedEvents()
            loadSavedEventIds()
            loadJoinedEvents()
            loadUserProfile()
          }
        }
    auth.addAuthStateListener(authListener!!)
  }

  /** Loads initial sample events synchronously for immediate UI responsiveness. */
  private fun loadInitialSamples() {
    _events = LocalEventRepository.defaultSampleEvents()
    _searchResults = _events
    _availableEvents = _events
  }

  /** Loads all events for search and map display, falling back to local data on error. */
  private fun loadAllEvents() {
    viewModelScope.launch {
      try {
        val events = eventRepository.getAllEvents()
        applyEventsDataset(events)
        println("MapScreenViewModel: Successfully loaded ${events.size} events from repository")
      } catch (primary: Exception) {
        Log.e("MapScreenViewModel", "Error loading events from repository", primary)
        try {
          val localEvents = EventRepositoryProvider.createLocalRepository().getAllEvents()
          applyEventsDataset(localEvents)
          println("MapScreenViewModel: Loaded ${localEvents.size} local events as fallback")
        } catch (fallback: Exception) {
          Log.e("MapScreenViewModel", "Failed to load local events fallback", fallback)
          _allEvents = emptyList()
          _events = LocalEventRepository().getAllEvents()
          _searchResults = _events
        }
      }
    }
  }

  private fun applyEventsDataset(events: List<Event>) {
    _allEvents = events
    _availableEvents = events
    loadJoinedEvents()
    // Do not call applyFilters to avoid overriding filtered events
  }

  private fun applyFilters() {
    viewModelScope.launch {
      try {
        val finalList =
            if (_searchQuery.isBlank()) {
              println("MapScreenViewModel: Applying filters: ${filterViewModel.filters.value}")
              eventRepository.getFilteredEvents(filterViewModel.filters.value)
            } else {
              eventRepository.getSearchedEvents(_searchQuery)
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
  }

  fun clearError() {
    _errorMessage = null
  }

  /** Loads joined events by filtering events where the current user is a participant. */
  private fun loadJoinedEvents() {
    val uid =
        auth.currentUser?.uid
            ?: run {
              _joinedEvents = emptyList()
              return
            }
    viewModelScope.launch {
      try {
        val base = _allEvents.ifEmpty { eventRepository.getSearchedEvents("") }
        _joinedEvents = base.filter { uid in it.participantIds }
        println("MapScreenViewModel: Loaded ${_joinedEvents.size} joined events")
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Error loading joined events", e)
        _joinedEvents = emptyList()
      }
    }
  }

  private fun loadSavedEvents() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _savedEvents = emptyList()
      return
    }
    viewModelScope.launch {
      _savedEvents = eventRepository.getSavedEvents(currentUserId)
      println("MapScreenViewModel: Loaded ${_savedEvents.size} saved events")
    }
  }

  private fun loadSavedEventIds() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _savedEventIds = emptySet()
      return
    }
    viewModelScope.launch {
      _savedEventIds = eventRepository.getSavedEventIds(currentUserId)
      println("MapScreenViewModel: Loaded ${_savedEventIds.size} saved event IDs")
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
        println("MapScreenViewModel: Loaded user profile with avatar URL: $_avatarUrl")
      } catch (e: Exception) {
        Log.e("MapScreenViewModel", "Error loading user profile", e)
        _avatarUrl = null
      }
    }
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

  fun onEventClickedFromSearch(event: Event) {
    _cameFromSearch = true
    onEventPinClicked(event, forceZoom = true)
  }

  fun closeEventDetail() {
    _selectedEvent = null
    _organizerName = ""

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

  fun onTabEventClicked(event: Event) {
    onEventPinClicked(event)
  }
}

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

  // Create FiltersSectionViewModel with the same ViewModelStoreOwner
  val filterViewModel: FiltersSectionViewModel =
      viewModel(viewModelStoreOwner = viewModelStoreOwner, key = "FiltersSectionViewModel")

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
                  filterViewModel = filterViewModel)
                  as T
            }
          })
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

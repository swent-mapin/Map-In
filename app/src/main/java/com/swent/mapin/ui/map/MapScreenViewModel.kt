package com.swent.mapin.ui.map

import android.content.Context
import android.location.Location
import android.net.Uri
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
import com.swent.mapin.model.SampleEventRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel central pour MapScreen — gère la feuille, la recherche, les événements, et la mémoire.
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    private val onClearFocus: () -> Unit,
    private val applicationContext: Context,
    private val memoryRepository: com.swent.mapin.model.memory.MemoryRepository =
        MemoryRepositoryProvider.getRepository(),
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

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
  private var _events by mutableStateOf<List<Event>>(SampleEventRepository.getSampleEvents())
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

  enum class BottomSheetTab {
    RECENT_ACTIVITIES,
    JOINED_EVENTS
  }

  private var _selectedBottomSheetTab by mutableStateOf(BottomSheetTab.RECENT_ACTIVITIES)
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

  var onCenterCamera: ((Event) -> Unit)? = null

  // Tag filtering
  private var _selectedTags by mutableStateOf<Set<String>>(emptySet())
  val selectedTags: Set<String>
    get() = _selectedTags

  private var _topTags by mutableStateOf<List<String>>(emptyList())
  val topTags: List<String>
    get() = _topTags

  // Location state
  private val locationManager = LocationManager(applicationContext)

  private var _currentLocation by mutableStateOf<Location?>(null)
  val currentLocation: Location?
    get() = _currentLocation

  private var _hasLocationPermission by mutableStateOf(false)
  val hasLocationPermission: Boolean
    get() = _hasLocationPermission

  var onCenterOnUserLocation: (() -> Unit)? = null
  var onRequestLocationPermission: (() -> Unit)? = null

  private var _locationBearing by mutableFloatStateOf(0f)
  val locationBearing: Float
    get() = _locationBearing

  private var _isCenteredOnUser by mutableStateOf(false)
  val isCenteredOnUser: Boolean
    get() = _isCenteredOnUser

  init {
    // Initialize with sample events quickly, then load remote data
    loadInitialSamples()
    // Preload events so the form has immediate data
    loadEvents()
    loadJoinedEvents()
    _topTags = SampleEventRepository.getTopTags()
    // Preload events both for searching and memory linking
    loadAllEvents()
    loadParticipantEvents()
  }

  /** Loads initial sample events synchronously for immediate UI responsiveness. */
  private fun loadInitialSamples() {
    _events = SampleEventRepository.getSampleEvents()
    _searchResults = _events
  }

  /** Loads primary events list for immediate UI usage with fallback to local samples. */
  private fun loadEvents() {
    viewModelScope.launch {
      try {
        val events = eventRepository.getAllEvents()
        _events = events
        _searchResults = events
      } catch (e: Exception) {
        android.util.Log.w(
            "MapScreenViewModel", "Failed to load events from repository, using samples", e)
        _events = SampleEventRepository.getSampleEvents()
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

  /** Loads all events for search and map display, falling back to local data on error. */
  private fun loadAllEvents() {
    viewModelScope.launch {
      try {
        val events = eventRepository.getAllEvents()
        applyEventsDataset(events)
      } catch (primary: Exception) {
        android.util.Log.e("MapScreenViewModel", "Error loading events from repository", primary)
        try {
          val localEvents = EventRepositoryProvider.createLocalRepository().getAllEvents()
          applyEventsDataset(localEvents)
        } catch (fallback: Exception) {
          android.util.Log.e("MapScreenViewModel", "Failed to load local events fallback", fallback)
          _allEvents = emptyList()
          applyFilters()
        }
      }
    }
  }

  private fun applyEventsDataset(events: List<Event>) {
    _allEvents = events
    applyFilters()
  }

  private fun applyFilters() {
    val base = if (_allEvents.isNotEmpty()) _allEvents else SampleEventRepository.getSampleEvents()

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
        android.util.Log.e("MapScreenViewModel", "Error loading events", e)
        _availableEvents = emptyList()
      }
    }
  }

  private fun loadJoinedEvents() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _joinedEvents = emptyList()
      return
    }
    _joinedEvents = _events.filter { event -> event.participantIds.contains(currentUserId) }
  }

  fun showMemoryForm() {
    _previousSheetState = _bottomSheetState
    _showMemoryForm = true
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideMemoryForm() {
    _showMemoryForm = false
  }

  fun onMemorySave(formData: MemoryFormData) {
    viewModelScope.launch {
      _isSavingMemory = true
      _errorMessage = null
      try {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
          _errorMessage = "You must be signed in to create a memory"
          android.util.Log.e("MapScreenViewModel", "Cannot save memory: User not authenticated")
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
        android.util.Log.d("MapScreenViewModel", "Memory saved successfully: ${memory.uid}")
        hideMemoryForm()
        restorePreviousSheetState()
      } catch (e: Exception) {
        _errorMessage = "Failed to save memory: ${e.message ?: "Unknown error"}"
        android.util.Log.e("MapScreenViewModel", "Error saving memory", e)
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
        android.util.Log.d("MapScreenViewModel", "Uploaded media: $downloadUrl")
      } catch (e: Exception) {
        android.util.Log.e("MapScreenViewModel", "Failed to upload media file: $uri", e)
      }
    }
    return downloadUrls
  }

  fun onMemoryCancel() {
    hideMemoryForm()
    restorePreviousSheetState()
  }

  override fun onCleared() {
    super.onCleared()
    hideScaleBarJob?.cancel()
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

  fun onEventPinClicked(event: Event) {
    viewModelScope.launch {
      _selectedEvent = event
      _organizerName = "User ${event.ownerId.take(6)}"
      setBottomSheetState(BottomSheetState.MEDIUM)
      onCenterCamera?.invoke(event)
    }
  }

  fun closeEventDetail() {
    _selectedEvent = null
    _organizerName = ""
    setBottomSheetState(BottomSheetState.COLLAPSED)
  }

  fun showShareDialog() {
    _showShareDialog = true
  }

  fun dismissShareDialog() {
    _showShareDialog = false
  }

  // NOTE: recentActivities and recordRecentActivity removed — UI no longer tracks recent
  // activities.

  fun isUserParticipating(): Boolean {
    val currentUserId = auth.currentUser?.uid ?: return false
    return _selectedEvent?.participantIds?.contains(currentUserId) ?: false
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
        val currentAttendees = event.attendeeCount ?: 0
        if (capacity != null && currentAttendees >= capacity) {
          _errorMessage = "Event is at full capacity"
          return@launch
        }
        val updatedParticipantIds =
            event.participantIds.toMutableList().apply {
              if (!contains(currentUserId)) add(currentUserId)
            }
        val newAttendeeCount = currentAttendees + 1
        val updatedEvent =
            event.copy(participantIds = updatedParticipantIds, attendeeCount = newAttendeeCount)
        eventRepository.editEvent(event.uid, updatedEvent)
        _selectedEvent = updatedEvent
        _events = _events.map { if (it.uid == event.uid) updatedEvent else it }
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
        val currentAttendees = event.attendeeCount ?: 0
        val newAttendeeCount = (currentAttendees - 1).coerceAtLeast(0)
        val updatedEvent =
            event.copy(participantIds = updatedParticipantIds, attendeeCount = newAttendeeCount)
        eventRepository.editEvent(event.uid, updatedEvent)
        _selectedEvent = updatedEvent
        _events = _events.map { if (it.uid == event.uid) updatedEvent else it }
        loadJoinedEvents()
      } catch (e: Exception) {
        _errorMessage = "Failed to unregister: ${e.message}"
      }
    }
  }

  /** Placeholder action for "save for later" used by the UI/tests. */
  fun saveEventForLater() {
    _errorMessage = "Save for later - Coming soon!"
  }

  /**
   * Called when the user taps an event from the "Joined events" list. Reuses the existing
   * onEventPinClicked behavior to select the event and center the camera.
   */
  fun onJoinedEventClicked(event: Event) {
    onEventPinClicked(event)
  }

  // Location management methods

  /** Checks and updates the location permission status. */
  fun checkLocationPermission() {
    _hasLocationPermission = locationManager.hasLocationPermission()
  }

  /** Starts listening to location updates if permission is granted. */
  fun startLocationUpdates() {
    if (!locationManager.hasLocationPermission()) {
      _hasLocationPermission = false
      return
    }

    _hasLocationPermission = true

    viewModelScope.launch {
      locationManager
          .getLocationUpdates()
          .catch { e ->
            android.util.Log.e("MapScreenViewModel", "Error getting location updates", e)
            _errorMessage = "Failed to get location updates"
          }
          .collect { location ->
            _currentLocation = location
            if (location.hasBearing()) {
              _locationBearing = location.bearing
            }
          }
    }
  }

  /** Gets the last known location and optionally centers the camera on it. */
  fun getLastKnownLocation(centerCamera: Boolean = false) {
    locationManager.getLastKnownLocation(
        onSuccess = { location ->
          _currentLocation = location
          if (location.hasBearing()) {
            _locationBearing = location.bearing
          }
          if (centerCamera) {
            onCenterOnUserLocation?.invoke()
          }
        },
        onError = { android.util.Log.w("MapScreenViewModel", "Could not get last known location") })
  }

  /**
   * Handles the location button click. If permission is granted, centers on user location.
   * Otherwise, requests permission.
   */
  fun onLocationButtonClick() {
    if (locationManager.hasLocationPermission()) {
      _isCenteredOnUser = true
      onCenterOnUserLocation?.invoke()
    } else {
      onRequestLocationPermission?.invoke()
    }
  }

  /**
   * Updates the centered state based on camera position. Call this when the camera moves to check
   * if still centered on user.
   */
  fun updateCenteredState(cameraLat: Double, cameraLon: Double) {
    val userLoc = _currentLocation
    if (userLoc == null) {
      _isCenteredOnUser = false
      return
    }

    // Check if camera is close enough to user location (within ~50 meters)
    val latDiff = abs(cameraLat - userLoc.latitude)
    val lonDiff = abs(cameraLon - userLoc.longitude)
    val threshold = 0.0005

    _isCenteredOnUser = latDiff < threshold && lonDiff < threshold
  }

  /**
   * Manually marks that the camera is no longer centered on the user. Call this when user manually
   * moves the map.
   */
  fun onMapMoved() {
    _isCenteredOnUser = false
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
            JsonObject().apply { addProperty("weight", event.attendeeCount) })
      }
  return FeatureCollection.fromFeatures(features).toJson()
}

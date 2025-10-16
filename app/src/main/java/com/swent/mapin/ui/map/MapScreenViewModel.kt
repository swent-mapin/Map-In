package com.swent.mapin.ui.map

import android.content.Context
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
import com.swent.mapin.model.event.EventRepositoryFirestore
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Assisted by AI

/**
 * Coordinates MapScreen state: sheet transitions, search focus, zoom behavior, and memory creation.
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    private val onClearFocus: () -> Unit,
    private val applicationContext: Context,
    private val memoryRepository: com.swent.mapin.model.memory.MemoryRepository =
        MemoryRepositoryProvider.getRepository(),
    private val eventRepository: EventRepository =
        EventRepositoryFirestore(com.google.firebase.firestore.FirebaseFirestore.getInstance()),
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

  /** Event data for display on the map */
  // Tag filtering
  private var _selectedTags by mutableStateOf<Set<String>>(emptySet())
  val selectedTags: Set<String>
    get() = _selectedTags

  private var _topTags by mutableStateOf<List<String>>(emptyList())
  val topTags: List<String>
    get() = _topTags

  // Event data for display on the map
  private var _events by mutableStateOf(SampleEventRepository.getSampleEvents())
  val events: List<Event>
    get() = _events

  init {
    // Preload events so the form has immediate data
    loadEvents()
    loadJoinedEvents()
    _topTags = SampleEventRepository.getTopTags()
  }

  fun onZoomChange(newZoom: Float) {
    // Ignore micro-changes to avoid flickering the scale bar
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

    // Convert dp threshold to pixels before comparing the gesture location
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

    if (target != BottomSheetState.FULL && _bottomSheetState == BottomSheetState.FULL) {
      clearSearch()
    }

    _bottomSheetState = target
  }

  fun onSearchQueryChange(query: String) {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
    _searchQuery = query
  }

  fun onSearchTap() {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
  }

  fun onSearchFocusHandled() {
    _shouldFocusSearch = false
  }

  private fun clearSearch() {
    _shouldFocusSearch = false
    onClearFocus()
    if (_searchQuery.isNotEmpty()) {
      _searchQuery = ""
    }
  }

  fun setMapStyle(style: MapStyle) {
    _mapStyle = style
  }

  fun clearError() {
    _errorMessage = null
  }

  /** Loads events the signed-in user can attach to memories. */
  private fun loadEvents() {
    viewModelScope.launch {
      try {
        val currentUserId = auth.currentUser?.uid
        _availableEvents =
            if (currentUserId != null) {
              eventRepository.getEventsByParticipant(currentUserId)
            } else {
              emptyList()
            }
      } catch (e: Exception) {
        android.util.Log.e("MapScreenViewModel", "Error loading events", e)
        _availableEvents = emptyList()
      }
    }
  }

  /** Loads events the user has joined from the sample events. */
  private fun loadJoinedEvents() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _joinedEvents = emptyList()
      return
    }

    // Filter sample events where the current user is a participant
    _joinedEvents = _events.filter { event -> event.participantIds.contains(currentUserId) }
  }

  /** Opens the creation form while preserving the previous sheet state. */
  fun showMemoryForm() {
    _previousSheetState = _bottomSheetState
    _showMemoryForm = true
    setBottomSheetState(BottomSheetState.FULL)
  }

  fun hideMemoryForm() {
    _showMemoryForm = false
  }

  /** Saves a memory from the form and restores the sheet on success. */
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

  /** Uploads media to Firebase Storage and returns download URLs. */
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
    _events = newEvents
  /** Toggle tag selection and filter events accordingly */
  fun toggleTagSelection(tag: String) {
    _selectedTags =
        if (_selectedTags.contains(tag)) {
          _selectedTags - tag
        } else {
          _selectedTags + tag
        }
    filterEvents()
  }

  /** Filter events based on selected tags */
  private fun filterEvents() {
    val allEvents = SampleEventRepository.getSampleEvents()
    _events =
        if (_selectedTags.isEmpty()) {
          allEvents
        } else {
          allEvents.filter { event -> event.tags.any { tag -> _selectedTags.contains(tag) } }
        }
  }

  /** Displays event details when a pin is clicked */
  fun onEventPinClicked(event: Event) {
    viewModelScope.launch {
      _selectedEvent = event
      _organizerName = "User ${event.ownerId.take(6)}"
      setBottomSheetState(BottomSheetState.MEDIUM)

      onCenterCamera?.invoke(event)
    }
  }

  /** Closes event detail and returns to menu */
  fun closeEventDetail() {
    _selectedEvent = null
    _organizerName = ""
    setBottomSheetState(BottomSheetState.COLLAPSED)
  }

  /** Shows share dialog */
  fun showShareDialog() {
    _showShareDialog = true
  }

  /** Dismisses share dialog */
  fun dismissShareDialog() {
    _showShareDialog = false
  }

  /** Checks if current user is participating */
  fun isUserParticipating(): Boolean {
    val currentUserId = auth.currentUser?.uid ?: return false
    return _selectedEvent?.participantIds?.contains(currentUserId) ?: false
  }

  /** Joins the selected event */
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

  /** Unregisters from the selected event */
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

  /** Save event for later (placeholder) */
  fun saveEventForLater() {
    _errorMessage = "Save for later - Coming soon!"
  }

  /** Changes the selected tab in the bottom sheet */
  fun setBottomSheetTab(tab: BottomSheetTab) {
    _selectedBottomSheetTab = tab
  }

  /** Called when a joined event is clicked to show its details */
  fun onJoinedEventClicked(event: Event) {
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
  // Avoid leaking the composition by promoting to application context
  val appContext = context.applicationContext

  return viewModel {
    MapScreenViewModel(
        initialSheetState = initialSheetState,
        sheetConfig = sheetConfig,
        onClearFocus = { focusManager.clearFocus(force = true) },
        applicationContext = appContext)
  }
}

/** Converts events into a GeoJSON payload consumable by Mapbox heatmaps. */
fun eventsToGeoJson(events: List<Event>): String {
  val features =
      events.map { event ->
        Feature.fromGeometry(
            Point.fromLngLat(event.location.longitude, event.location.latitude),
            JsonObject().apply { addProperty("weight", event.attendeeCount) })
      }
  return FeatureCollection.fromFeatures(features).toJson()
}

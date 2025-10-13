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
import com.swent.mapin.model.Location
import com.swent.mapin.model.SampleLocationRepository
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

  // Locations backing the map
  private var _locations by mutableStateOf(SampleLocationRepository.getSampleLocations())
  val locations: List<Location>
    get() = _locations

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

  init {
    // Preload events so the form has immediate data
    loadEvents()
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

  fun setLocations(newLocations: List<Location>) {
    _locations = newLocations
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

/** Converts locations into a GeoJSON payload consumable by Mapbox heatmaps. */
fun locationsToGeoJson(locations: List<Location>): String {
  val features =
      locations.map { location ->
        Feature.fromGeometry(
            Point.fromLngLat(location.longitude, location.latitude),
            JsonObject().apply { addProperty("weight", location.attendees) })
      }
  return FeatureCollection.fromFeatures(features).toJson()
}

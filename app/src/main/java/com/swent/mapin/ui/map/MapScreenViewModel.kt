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
import com.swent.mapin.model.SampleEventRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryFirestore
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.abs

// Assisted by AI
/**
 * ViewModel for MapScreen that encapsulates all state management and business logic.
 * - Manages bottom sheet state transitions
 * - Handles search interactions
 * - Detects zoom-based map interactions
 * - Coordinates scroll resets and focus management
 * - Exposes optional pins and heatmap state for the map layer
 */
class MapScreenViewModel(
    initialSheetState: BottomSheetState,
    private val sheetConfig: BottomSheetConfig,
    private val onClearFocus: () -> Unit,
    private val context: Context,
    private val memoryRepository: com.swent.mapin.model.memory.MemoryRepository =
        MemoryRepositoryProvider.getRepository(),
    private val eventRepository: EventRepository =
        EventRepositoryFirestore(com.google.firebase.firestore.FirebaseFirestore.getInstance()),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  // ---------------- Existing state (unchanged) ----------------

  private var _bottomSheetState by mutableStateOf(initialSheetState)
  val bottomSheetState: BottomSheetState
    get() = _bottomSheetState

  private var _fullEntryKey by mutableIntStateOf(0)
  val fullEntryKey: Int
    get() = _fullEntryKey

  var currentSheetHeight by mutableStateOf(sheetConfig.collapsedHeight)

  private var _searchQuery by mutableStateOf("")
  val searchQuery: String
    get() = _searchQuery

  private var _shouldFocusSearch by mutableStateOf(false)
  val shouldFocusSearch: Boolean
    get() = _shouldFocusSearch

  private var _mediumReferenceZoom by mutableFloatStateOf(0f)

  private var isInMediumMode = false

  /** Update bottom sheet state and handle side effects */
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

  /** Handle search query changes - automatically expands to full mode */
  fun onSearchQueryChange(query: String) {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
    _searchQuery = query
  }

  /** Handle search bar tap - expands to full mode */
  fun onSearchTap() {
    if (_bottomSheetState != BottomSheetState.FULL) {
      _shouldFocusSearch = true
      setBottomSheetState(BottomSheetState.FULL)
    }
  }

  /** Clear focus flag after search bar has been focused */
  fun onSearchFocusHandled() {
    _shouldFocusSearch = false
  }

  /** Clear search and dismiss keyboard */
  private fun clearSearch() {
    _shouldFocusSearch = false
    onClearFocus()
    if (_searchQuery.isNotEmpty()) {
      _searchQuery = ""
    }
  }

  /** Set reference zoom when entering medium mode */
  fun updateMediumReferenceZoom(zoom: Float) {
    if (isInMediumMode) {
      _mediumReferenceZoom = zoom
    }
  }

  /** Check if zoom change should trigger collapse */
  fun checkZoomInteraction(currentZoom: Float): Boolean {
    if (!isInMediumMode) return false

    val zoomDelta = abs(currentZoom - _mediumReferenceZoom)
    return zoomDelta >= MapConstants.ZOOM_CHANGE_THRESHOLD
  }

  /**
   * Check if touch position is within proximity of sheet top edge. Returns true if the touch is
   * close enough to trigger collapse.
   *
   * @param touchY Y coordinate of touch position in pixels
   * @param sheetTopY Y coordinate of sheet top edge in pixels
   * @param densityDpi Screen density in DPI for dp-to-px conversion
   */
  fun checkTouchProximityToSheet(touchY: Float, sheetTopY: Float, densityDpi: Int): Boolean {
    if (!isInMediumMode) return false

    val thresholdPx = MapConstants.SHEET_PROXIMITY_THRESHOLD_DP * densityDpi / 160f
    val distance = abs(touchY - sheetTopY)

    return distance <= thresholdPx
  }

  /** Calculate target state after drag based on current height */
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

  /** Map state to height */
  fun getHeightForState(state: BottomSheetState): Dp {
    return when (state) {
      BottomSheetState.COLLAPSED -> sheetConfig.collapsedHeight
      BottomSheetState.MEDIUM -> sheetConfig.mediumHeight
      BottomSheetState.FULL -> sheetConfig.fullHeight
    }
  }

  /** Cleanup when ViewModel is destroyed. Later: add cleanup for coroutines/resources. */
  override fun onCleared() {
    super.onCleared()
  }

  // ---------------- Events & Heatmap ----------------

  /** Event data for display on the map */
  private var _events by mutableStateOf(SampleEventRepository.getSampleEvents())
  val events: List<Event>
    get() = _events

  fun setEvents(newEvents: List<Event>) {
    _events = newEvents
  }

  // Heatmap visibility toggle
  private var _showHeatmap by mutableStateOf(false)
  val showHeatmap: Boolean
    get() = _showHeatmap

  fun toggleHeatmap() {
    _showHeatmap = !_showHeatmap
  }

  // ---------------- Memory Creation Form ----------------

  /** Whether the memory creation form is currently displayed */
  private var _showMemoryForm by mutableStateOf(false)
  val showMemoryForm: Boolean
    get() = _showMemoryForm

  /** State of the bottom sheet before showing the memory form */
  private var _previousSheetState: BottomSheetState? = null

  /** Error message to display to user */
  private var _errorMessage by mutableStateOf<String?>(null)
  val errorMessage: String?
    get() = _errorMessage

  /** Loading state for memory saving */
  private var _isSavingMemory by mutableStateOf(false)
  val isSavingMemory: Boolean
    get() = _isSavingMemory

  /** Clear error message */
  fun clearError() {
    _errorMessage = null
  }

  /** Available events for linking to memories */
  private var _availableEvents by mutableStateOf<List<Event>>(emptyList())
  val availableEvents: List<Event>
    get() = _availableEvents

  init {
    // Load events from repository
    loadEvents()
  }

  /** Load events from the repository */
  private fun loadEvents() {
    viewModelScope.launch {
      try {
        _availableEvents = eventRepository.getAllEvents()
      } catch (e: Exception) {
        // Handle error, for now just log and keep empty list
        android.util.Log.e("MapScreenViewModel", "Error loading events", e)
        _availableEvents = emptyList()
      }
    }
  }

  /** Show the memory creation form and expand to full mode */
  fun showMemoryForm() {
    _previousSheetState = _bottomSheetState
    _showMemoryForm = true
    setBottomSheetState(BottomSheetState.FULL)
  }

  /** Hide the memory creation form */
  fun hideMemoryForm() {
    _showMemoryForm = false
  }

  /**
   * Handle memory form save action
   *
   * @param formData Form data containing title, description, eventId, visibility, media, and tags
   */
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

        // Upload media files to Firebase Storage
        val mediaUrls = uploadMediaFiles(formData.mediaUris, currentUserId)

        // Create and save memory
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

        // Success, close form and restore previous sheet state
        hideMemoryForm()
        _previousSheetState?.let { setBottomSheetState(it) }
        _previousSheetState = null
      } catch (e: Exception) {
        _errorMessage = "Failed to save memory: ${e.message ?: "Unknown error"}"
        android.util.Log.e("MapScreenViewModel", "Error saving memory", e)
      } finally {
        _isSavingMemory = false
      }
    }
  }

  /**
   * Upload media files to Firebase Storage
   *
   * @param uris List of media URIs to upload
   * @param userId User ID to organize uploads
   * @return List of download URLs for uploaded media
   */
  private suspend fun uploadMediaFiles(uris: List<Uri>, userId: String): List<String> {
    if (uris.isEmpty()) return emptyList()

    val downloadUrls = mutableListOf<String>()

    for (uri in uris) {
      try {
        val extension = context.contentResolver.getType(uri)?.split("/")?.lastOrNull() ?: "jpg"
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

  /** Handle memory form cancel action */
  fun onMemoryCancel() {
    hideMemoryForm()
    _previousSheetState?.let { setBottomSheetState(it) }
    _previousSheetState = null
  }
}

/**
 * Creates MapScreenViewModel with configuration.
 *
 * @param sheetConfig Configuration for bottom sheet heights
 * @param context Application context for media operations
 * @param initialSheetState Initial state of the bottom sheet
 */
@Composable
fun rememberMapScreenViewModel(
    sheetConfig: BottomSheetConfig,
    initialSheetState: BottomSheetState = BottomSheetState.COLLAPSED
): MapScreenViewModel {
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current

  return viewModel {
    MapScreenViewModel(
        initialSheetState = initialSheetState,
        sheetConfig = sheetConfig,
        onClearFocus = { focusManager.clearFocus(force = true) },
        context = context)
  }
}

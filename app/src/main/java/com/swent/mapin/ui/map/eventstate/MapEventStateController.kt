package com.swent.mapin.ui.map.eventstate

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.network.ConnectivityService
import com.swent.mapin.ui.filters.Filters
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Represents an action that can be queued for offline execution. */
sealed class OfflineAction {
  data class LeaveEvent(val eventId: String, val userId: String) : OfflineAction()

  data class SaveEvent(val eventId: String, val userId: String) : OfflineAction()

  data class UnsaveEvent(val eventId: String, val userId: String) : OfflineAction()
}

/**
 * Encapsulates all event-related state (filtered, search results, joined, saved) and repository
 * interactions (join, save, etc.) so the ViewModel only coordinates UI state.
 *
 * Now includes offline mode support with action queuing and connectivity monitoring.
 */
class MapEventStateController(
    private val eventRepository: EventRepository,
    private val userProfileRepository: UserProfileRepository,
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope,
    private val filterViewModel: FiltersSectionViewModel,
    private val connectivityService: ConnectivityService,
    private val getSelectedEvent: () -> Event?,
    private val setErrorMessage: (String) -> Unit,
    private val clearErrorMessage: () -> Unit,
    // Indicates whether the code is not running inside a unit test environment.
    // Used to disable features that rely on infinite or long-running coroutines (e.g., periodic
    // auto-refresh loops) which would otherwise block or hang the test runner.
    private val autoRefreshEnabled: Boolean = true
) {

  private var _allEvents by mutableStateOf<List<Event>>(emptyList())
  val allEvents: List<Event>
    get() = _allEvents

  private var _searchResults by mutableStateOf<List<Event>>(emptyList())
  val searchResults: List<Event>
    get() = _searchResults

  private var _availableEvents by mutableStateOf<List<Event>>(emptyList())
  val availableEvents: List<Event>
    get() = _availableEvents

  private var _joinedEvents by mutableStateOf<List<Event>>(emptyList())
  val joinedEvents: List<Event>
    get() = _joinedEvents

  // New: events the user has attended (joined + finished)
  private var _attendedEvents by mutableStateOf<List<Event>>(emptyList())
  val attendedEvents: List<Event>
    get() = _attendedEvents

  private var _savedEvents by mutableStateOf<List<Event>>(emptyList())
  val savedEvents: List<Event>
    get() = _savedEvents

  // Owned events (events created by current authenticated user)
  private var _ownedEvents by mutableStateOf<List<Event>>(emptyList())
  val ownedEvents: List<Event>
    get() = _ownedEvents

  private var _ownedLoading by mutableStateOf(false)
  val ownedLoading: Boolean
    get() = _ownedLoading

  private var _ownedError by mutableStateOf<String?>(null)
  val ownedError: String?
    get() = _ownedError

  // StateFlow for joined events (for offline region downloads)
  private val _joinedEventsFlow = MutableStateFlow<List<Event>>(emptyList())
  val joinedEventsFlow: StateFlow<List<Event>> = _joinedEventsFlow.asStateFlow()

  // StateFlow for saved events (for offline region downloads)
  private val _savedEventsFlow = MutableStateFlow<List<Event>>(emptyList())
  val savedEventsFlow: StateFlow<List<Event>> = _savedEventsFlow.asStateFlow()

  // Offline mode support
  private val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
  private val offlineActionQueue = ConcurrentLinkedQueue<OfflineAction>()
  private var isProcessingQueue = false

  // Listeners for real-time updates
  private var joinedEventsListener: ListenerRegistration? = null
  private var savedEventsListener: ListenerRegistration? = null
  private var filteredEventsListener: ListenerRegistration? = null

  // Observer jobs
  private var filterObserverJob: Job? = null
  private var connectivityObserverJob: Job? = null

  /**
   * Observes filter changes from [FiltersSectionViewModel] and applies them to update [allEvents]
   * accordingly.
   */
  @OptIn(FlowPreview::class)
  fun observeFilters() {
    filterObserverJob =
        scope.launch {
          filterViewModel.filters.debounce(300).distinctUntilChanged().collect { filters ->
            getFilteredEvents(filters)
          }
        }
  }

  /** Observes connectivity changes and processes offline queue when connection is restored. */
  fun observeConnectivity() {
    connectivityObserverJob =
        scope.launch {
          connectivityService.connectivityState.collect { state ->
            val wasOffline = !_isOnline.value
            _isOnline.value = state.isConnected

            if (state.isConnected && wasOffline && offlineActionQueue.isNotEmpty()) {
              processOfflineQueue()
            }
          }
        }
  }

  /** Stops observing filters and connectivity changes. */
  fun stopObserving() {
    filterObserverJob?.cancel()
    connectivityObserverJob?.cancel()
  }

  /**
   * Processes all actions in the offline queue. Each action is retried up to 5 times before
   * reverting the optimistic update.
   */
  private suspend fun processOfflineQueue() {
    if (isProcessingQueue) return

    isProcessingQueue = true
    val failedActions = mutableListOf<OfflineAction>()

    try {
      while (offlineActionQueue.isNotEmpty()) {
        val action = offlineActionQueue.first()
        var success = false
        var attempts = 0
        val maxAttempts = 5

        while (attempts < maxAttempts && !success) {
          attempts++
          try {
            when (action) {
              is OfflineAction.LeaveEvent -> {
                try {
                  eventRepository.editEventAsUser(action.eventId, action.userId, join = false)
                  success = true
                } catch (e: Exception) {
                  if (e.message?.contains("Event not found") == true) {
                    success = true
                  } else {
                    throw e
                  }
                }
              }
              is OfflineAction.SaveEvent -> {
                val userProfile =
                    userProfileRepository.getUserProfile(action.userId)
                        ?: throw Exception("User profile not found")
                val updatedSavedEventIds = userProfile.savedEventIds + action.eventId
                val updatedProfile = userProfile.copy(savedEventIds = updatedSavedEventIds)
                userProfileRepository.saveUserProfile(updatedProfile)
                success = true
              }
              is OfflineAction.UnsaveEvent -> {
                val userProfile =
                    userProfileRepository.getUserProfile(action.userId)
                        ?: throw Exception("User profile not found")
                val updatedSavedEventIds = userProfile.savedEventIds - action.eventId
                val updatedProfile = userProfile.copy(savedEventIds = updatedSavedEventIds)
                userProfileRepository.saveUserProfile(updatedProfile)
                success = true
              }
            }
          } catch (_: Exception) {
            if (attempts >= maxAttempts) {
              // Max attempts reached - revert optimistic update
              revertOfflineAction(action)
              failedActions.add(action)
            }
          }
        }

        // Remove action from queue if successful or max attempts reached
        offlineActionQueue.poll()
      }

      // Refresh lists after processing queue
      if (failedActions.isEmpty()) {
        refreshEventsList()
      } else {
        setErrorMessage("Some offline actions could not be synced")
      }
    } finally {
      isProcessingQueue = false
    }
  }

  /** Reverts an optimistic update that failed after max retry attempts. */
  private fun revertOfflineAction(action: OfflineAction) {
    when (action) {
      is OfflineAction.LeaveEvent -> {
        // Find the event and re-add it to joined events
        val event = _allEvents.find { it.uid == action.eventId }
        if (event != null) {
          _joinedEvents = (_joinedEvents + event).sortedByDescending { it.date }
          _joinedEventsFlow.value = _joinedEvents
          loadAttendedEvents()
        }
      }
      is OfflineAction.SaveEvent -> {
        // Remove event from saved events
        _savedEvents = _savedEvents.filter { it.uid != action.eventId }
        _savedEventsFlow.value = _savedEvents
      }
      is OfflineAction.UnsaveEvent -> {
        // Find the event and re-add it to saved events
        val event = _allEvents.find { it.uid == action.eventId }
        if (event != null) {
          _savedEvents = (_savedEvents + event).sortedByDescending { it.date }
          _savedEventsFlow.value = _savedEvents
        }
      }
    }
  }

  /**
   * Starts real-time listeners for joined and saved events. Should be called after user
   * authentication.
   */
  fun startListeners() {
    try {
      val currentUserId = getUserId()

      // Listen to joined events
      joinedEventsListener =
          eventRepository.listenToJoinedEvents(
              userId = currentUserId,
              onUpdate = { added, modified, removed ->
                handleJoinedEventsUpdate(added, modified, removed)
              })

      // Listen to saved events
      savedEventsListener =
          eventRepository.listenToSavedEvents(
              userId = currentUserId,
              onUpdate = { added, modified, removed ->
                handleSavedEventsUpdate(added, modified, removed)
              })
    } catch (e: Exception) {
      setErrorMessage(e.message ?: "Failed to start event listeners")
    }
  }

  /** Stops all active listeners. Should be called when user logs out or controller is disposed. */
  fun stopListeners() {
    joinedEventsListener?.remove()
    joinedEventsListener = null

    savedEventsListener?.remove()
    savedEventsListener = null

    filteredEventsListener?.remove()
    filteredEventsListener = null
  }

  /** Handles updates to joined events from Firestore listener. */
  private fun handleJoinedEventsUpdate(
      added: List<Event>,
      modified: List<Event>,
      removedIds: List<String>
  ) {
    val currentMap = _joinedEvents.associateBy { it.uid }.toMutableMap()

    // Add new events
    added.forEach { event -> currentMap[event.uid] = event }

    // Update modified events
    modified.forEach { event -> currentMap[event.uid] = event }

    // Remove deleted events
    removedIds.forEach { id -> currentMap.remove(id) }

    // Update state
    _joinedEvents = currentMap.values.sortedByDescending { it.date }
    _joinedEventsFlow.value = _joinedEvents

    // Recalculate attended events
    loadAttendedEvents()
  }

  /** Handles updates to saved events from Firestore listener. */
  private fun handleSavedEventsUpdate(
      added: List<Event>,
      modified: List<Event>,
      removedIds: List<String>
  ) {
    val currentMap = _savedEvents.associateBy { it.uid }.toMutableMap()

    // Add new events
    added.forEach { event -> currentMap[event.uid] = event }

    // Update modified events
    modified.forEach { event -> currentMap[event.uid] = event }

    // Remove deleted events
    removedIds.forEach { id -> currentMap.remove(id) }

    // Update state
    _savedEvents = currentMap.values.sortedByDescending { it.date }
    _savedEventsFlow.value = _savedEvents
  }

  /** Refreshes [allEvents] using the current filters. */
  fun refreshEventsList() {
    getFilteredEvents(filterViewModel.filters.value)
    loadJoinedEvents()
    loadSavedEvents()
    loadAttendedEvents()
    loadOwnedEvents()
  }

  /**
   * Refreshes and returns the latest data for the event with the given [eventId] from [allEvents].
   *
   * @param eventId The ID of the event to refresh.
   * @return The refreshed [Event] if found, otherwise null.
   */
  fun refreshSelectedEvent(eventId: String): Event? {
    return allEvents.find { it.uid == eventId }
  }

  /**
   * Applies the given [filters] to fetch events from the repository and update [allEvents]. Works
   * offline using Firestore cache.
   *
   * @param filters The filters to apply to the event list.
   */
  fun getFilteredEvents(filters: Filters) {
    scope.launch {
      try {
        val userId = getUserId()
        // This will use Firestore cache if offline
        _allEvents = eventRepository.getFilteredEvents(filters, userId)
      } catch (e: Exception) {
        setErrorMessage(e.message ?: "Unknown error occurred while fetching events")
      }
    }
  }

  /**
   * Performs a local search on [allEvents] for events containing the [query] in their title.
   *
   * @param query The search query string.
   */
  fun searchEvents(query: String) {
    val lowerQuery = query.trim().lowercase()
    _searchResults =
        if (lowerQuery.isEmpty()) {
          emptyList()
        } else {
          allEvents.filter { event ->
            listOfNotNull(event.title, event.description, event.location.name).any { field ->
              field.trim().lowercase().contains(lowerQuery)
            } || event.tags.any { tag -> tag.lowercase().contains(lowerQuery) }
          }
        }
  }

  /**
   * Loads the list of events that the current user has joined. Works offline using Firestore cache.
   *
   * Populates [joinedEvents] by fetching events from the repository.
   *
   * Errors encountered during loading are transmitted to caller.
   */
  fun loadJoinedEvents() {
    scope.launch {
      try {
        val currentUserId = getUserId()
        _joinedEvents = eventRepository.getJoinedEvents(currentUserId)
        _joinedEventsFlow.value = _joinedEvents
        loadAttendedEvents()
      } catch (e: Exception) {
        setErrorMessage(e.message ?: "Unknown error occurred while fetching joined events")
      }
    }
  }

  /**
   * Loads the list of events that the current user has saved. Works offline using Firestore cache.
   *
   * Populates [savedEvents] by fetching events from the repository.
   *
   * Errors encountered during loading are transmitted to caller.
   */
  fun loadSavedEvents() {
    scope.launch {
      try {
        val currentUserId = getUserId()
        _savedEvents = eventRepository.getSavedEvents(currentUserId)
        _savedEventsFlow.value = _savedEvents
      } catch (e: Exception) {
        setErrorMessage(e.message ?: "Unknown error occurred while fetching saved events")
      }
    }
  }

  /**
   * Loads events owned by the current authenticated user. Uses direct repository query
   * (getEventsByOwner) to avoid fetching all events and filtering client-side, which improves
   * performance and reduces data transfer. Works offline using Firestore cache. Errors are surfaced
   * via setErrorMessage.
   */
  fun loadOwnedEvents() {
    scope.launch {
      _ownedLoading = true
      _ownedError = null
      try {
        val currentUserId = getUserId()
        _ownedEvents = eventRepository.getOwnedEvents(currentUserId)
      } catch (e: Exception) {
        val msg = e.message ?: "Unknown error occurred while fetching owned events"
        _ownedError = msg
        setErrorMessage(msg)
      } finally {
        _ownedLoading = false
      }
    }
  }

  /**
   * Loads the list of events that the current user has attended. Populates [attendedEvents] by
   * filtering [joinedEvents] for events that have already ended.
   */
  private fun loadAttendedEvents() {
    _attendedEvents = computeAttendedEvents(_joinedEvents)
  }

  companion object {
    /**
     * Visible helper used by tests to compute which of the provided joined events are "attended"
     * (i.e. their endDate is in the past) and return them sorted by most recent end date first.
     */
    @VisibleForTesting
    fun computeAttendedEvents(joinedEvents: List<Event>): List<Event> {
      val now = System.currentTimeMillis()
      return joinedEvents
          .filter { ev -> ev.endDate?.toDate()?.time?.let { it <= now } ?: false }
          .sortedByDescending { it.endDate?.toDate()?.time ?: 0L }
    }
  }

  /** Automatically refreshes [attendedEvents] every 10 seconds. */
  init {
    if (autoRefreshEnabled && !isRunningUnderUnitTest()) {
      startAttendedAutoRefresh()
    }
  }

  private fun startAttendedAutoRefresh() {
    scope.launch {
      while (isActive) {
        loadAttendedEvents()
        delay(10_000) // every 10 seconds
      }
    }
  }

  fun isRunningUnderUnitTest(): Boolean {
    return Thread.currentThread().stackTrace.any {
      it.className.startsWith("org.junit") || it.className.contains("GradleTest")
    }
  }

  /**
   * Adds the current user as a participant to the selected event. Uses optimistic update: UI is
   * updated immediately, then synced with server.
   *
   * REQUIRES INTERNET: This action affects other users (capacity) so cannot be queued offline.
   *
   * Updates the event in the repository and refreshes [allEvents] and [joinedEvents].
   *
   * Errors encountered are transmitted to caller and state is rolled back.
   */
  suspend fun joinSelectedEvent() {
    // Check connectivity - joining affects other users (capacity)
    if (!_isOnline.value) {
      setErrorMessage("Joining events requires an internet connection")
      return
    }

    val event = getSelectedEvent() ?: return
    val currentUserId = getUserId()

    // Check if already joined
    if (event.participantIds.contains(currentUserId)) {
      return
    }

    // Check capacity before optimistic update
    if (event.capacity != null && event.participantIds.size >= event.capacity) {
      setErrorMessage(
          "Event is at full capacity: ${event.participantIds.size} out of ${event.capacity}")
      return
    }

    // Optimistic update: add event to joined list immediately
    val previousJoinedEvents = _joinedEvents
    _joinedEvents = (_joinedEvents + event).sortedByDescending { it.date }
    _joinedEventsFlow.value = _joinedEvents
    loadAttendedEvents()

    // Try to sync with server
    try {
      eventRepository.editEventAsUser(event.uid, currentUserId, join = true)
      // Success - listener will confirm the state
      refreshEventsList()
    } catch (e: Exception) {
      // Rollback optimistic update on error
      _joinedEvents = previousJoinedEvents
      _joinedEventsFlow.value = _joinedEvents
      loadAttendedEvents()
      setErrorMessage(e.message ?: "Unknown error occurred while joining event")
    }
  }

  /**
   * Removes the current user from the participant list of the selected event. Uses optimistic
   * update: UI is updated immediately, then synced with server.
   *
   * WORKS OFFLINE: Action is queued if offline and processed when connection is restored.
   *
   * Updates the event in the repository and refreshes [allEvents] and [joinedEvents].
   */
  suspend fun leaveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    val currentUserId = getUserId()

    // Check if event still exists in joined events
    if (!_joinedEvents.any { it.uid == event.uid }) {
      // Event was already removed (probably deleted)
      return
    }

    // Check if actually joined
    if (!event.participantIds.contains(currentUserId)) {
      return
    }

    // Optimistic update: remove event from joined list immediately
    val previousJoinedEvents = _joinedEvents
    _joinedEvents = _joinedEvents.filter { it.uid != event.uid }
    _joinedEventsFlow.value = _joinedEvents
    loadAttendedEvents()

    // Try to sync with server (or queue if offline)
    if (!_isOnline.value) {
      // Queue action for later
      offlineActionQueue.add(OfflineAction.LeaveEvent(event.uid, currentUserId))
      return
    }

    try {
      eventRepository.editEventAsUser(event.uid, currentUserId, join = false)
      // Success - listener will confirm the state
      refreshEventsList()
    } catch (e: Exception) {
      if (e.message?.contains("Event not found") == true) {
        // Event was deleted while user was leaving - treat as success
        return
      }
      // Rollback optimistic update on error
      _joinedEvents = previousJoinedEvents
      _joinedEventsFlow.value = _joinedEvents
      loadAttendedEvents()
      setErrorMessage(e.message ?: "Unknown error occurred while leaving event")
    }
  }

  /**
   * Saves the selected event for the current user by adding it to their savedEventIds. Uses
   * optimistic update: UI is updated immediately, then synced with server.
   *
   * WORKS OFFLINE: Action is queued if offline and processed when connection is restored.
   *
   * Updates the user's profile in Firestore and refreshes [savedEvents].
   */
  suspend fun saveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    val currentUserId = getUserId()

    // Check if already saved
    if (_savedEvents.any { it.uid == event.uid }) {
      return
    }

    // Optimistic update: add event to saved list immediately
    val previousSavedEvents = _savedEvents
    _savedEvents = (_savedEvents + event).sortedByDescending { it.date }
    _savedEventsFlow.value = _savedEvents

    // Try to sync with server (or queue if offline)
    if (!_isOnline.value) {
      // Queue action for later
      offlineActionQueue.add(OfflineAction.SaveEvent(event.uid, currentUserId))
      return
    }

    try {
      val userProfile =
          userProfileRepository.getUserProfile(currentUserId)
              ?: throw Exception("User profile not found")
      val updatedSavedEventIds = userProfile.savedEventIds + event.uid
      val updatedProfile = userProfile.copy(savedEventIds = updatedSavedEventIds)
      userProfileRepository.saveUserProfile(updatedProfile)
      // Success - listener will confirm the state
    } catch (e: Exception) {
      // Rollback optimistic update on error
      _savedEvents = previousSavedEvents
      _savedEventsFlow.value = _savedEvents
      setErrorMessage(e.message ?: "Unknown error occurred while saving event")
    }
  }

  /**
   * Unsaves the selected event for the current user by removing it from their savedEventIds. Uses
   * optimistic update: UI is updated immediately, then synced with server.
   *
   * WORKS OFFLINE: Action is queued if offline and processed when connection is restored.
   *
   * Updates the user's profile in Firestore and refreshes [savedEvents].
   */
  suspend fun unsaveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    val currentUserId = getUserId()

    // Check if actually saved
    if (!_savedEvents.any { it.uid == event.uid }) {
      return
    }

    // Optimistic update: remove event from saved list immediately
    val previousSavedEvents = _savedEvents
    _savedEvents = _savedEvents.filter { it.uid != event.uid }
    _savedEventsFlow.value = _savedEvents

    // Try to sync with server (or queue if offline)
    if (!_isOnline.value) {
      // Queue action for later
      offlineActionQueue.add(OfflineAction.UnsaveEvent(event.uid, currentUserId))
      return
    }

    try {
      val userProfile =
          userProfileRepository.getUserProfile(currentUserId)
              ?: throw Exception("User profile not found")
      val updatedSavedEventIds = userProfile.savedEventIds - event.uid
      val updatedProfile = userProfile.copy(savedEventIds = updatedSavedEventIds)
      userProfileRepository.saveUserProfile(updatedProfile)
      // Success - listener will confirm the state
    } catch (e: Exception) {
      // Rollback optimistic update on error
      _savedEvents = previousSavedEvents
      _savedEventsFlow.value = _savedEvents
      setErrorMessage(e.message ?: "Unknown error occurred while unsaving event")
    }
  }

  /**
   * Retrieves the current authenticated user's ID.
   *
   * @return The user ID of the currently authenticated user.
   * @throws Exception if no user is authenticated.
   */
  fun getUserId(): String {
    return auth.currentUser?.uid ?: throw Exception("User not authenticated")
  }

  /** Clears all user-scoped state (joined, saved, etc.) and stops listeners. */
  fun clearUserScopedState() {
    stopListeners()
    _allEvents = emptyList()
    _searchResults = emptyList()
    _availableEvents = emptyList()
    _joinedEvents = emptyList()
    _attendedEvents = emptyList()
    _savedEvents = emptyList()
    offlineActionQueue.clear()
  }

  /** Clears the current search results. */
  fun clearSearchResults() {
    _searchResults = emptyList()
  }

  /** Clears the current error message. */
  fun clearError() {
    clearErrorMessage()
  }

  /** Clears the offline action queue (for testing). */
  @VisibleForTesting
  fun clearOfflineQueue() {
    offlineActionQueue.clear()
  }

  /** Gets the current size of the offline queue (for testing). */
  @VisibleForTesting fun getOfflineQueueSize(): Int = offlineActionQueue.size

  @VisibleForTesting
  fun setAllEventsForTest(events: List<Event>) {
    _allEvents = events
  }

  @VisibleForTesting
  fun setSearchResultForTest(events: List<Event>) {
    _searchResults = events
  }

  @VisibleForTesting
  fun setAvailableEventsForTest(events: List<Event>) {
    _availableEvents = events
  }

  @VisibleForTesting
  fun setJoinedEventsForTest(events: List<Event>) {
    _joinedEvents = events
    // Ensure attendedEvents is recomputed when tests set joined events directly.
    loadAttendedEvents()
  }

  @VisibleForTesting
  fun setSavedEventsForTest(events: List<Event>) {
    _savedEvents = events
  }

  @VisibleForTesting
  fun setOwnedEventsForTest(events: List<Event>) {
    _ownedEvents = events
  }
}

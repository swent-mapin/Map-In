package com.swent.mapin.ui.map.eventstate

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.filters.Filters
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Encapsulates all event-related state (filtered, search results, joined, saved) and repository
 * interactions (join, save, etc.) so the ViewModel only coordinates UI state.
 */
class MapEventStateController(
    private val eventRepository: EventRepository,
    private val userProfileRepository: UserProfileRepository,
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope,
    private val filterViewModel: FiltersSectionViewModel,
    private val getSelectedEvent: () -> Event?,
    private val setErrorMessage: (String) -> Unit,
    private val clearErrorMessage: () -> Unit
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

  /**
   * Observes filter changes from [FiltersSectionViewModel] and applies them to update [allEvents]
   * accordingly.
   */
  @OptIn(FlowPreview::class)
  fun observeFilters() {
    scope.launch {
      filterViewModel.filters.debounce(300).distinctUntilChanged().collect { filters ->
        getFilteredEvents(filters)
      }
    }
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
   * Applies the given [filters] to fetch events from the repository and update [allEvents].
   *
   * @param filters The filters to apply to the event list.
   */
  fun getFilteredEvents(filters: Filters) {
    scope.launch {
      try {
        _allEvents = eventRepository.getFilteredEvents(filters)
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
   * Loads the list of events that the current user has joined.
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
   * Loads the list of events that the current user has saved.
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
   * performance and reduces data transfer. Errors are surfaced via setErrorMessage.
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
    startAttendedAutoRefresh()
  }

  private fun startAttendedAutoRefresh() {
    scope.launch {
      while (true) {
        loadAttendedEvents()
        delay(10_000) // every 10 seconds
      }
    }
  }

  /**
   * Adds the current user as a participant to the selected event.
   *
   * Updates the event in the repository and refreshes [allEvents] and [joinedEvents].
   *
   * Errors encountered are transmitted to caller.
   */
  suspend fun joinSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      if (!event.participantIds.contains(currentUserId)) {
        require(event.capacity == null || event.participantIds.size < event.capacity) {
          "Event is at full capacity: ${event.participantIds.size} out of ${event.capacity}"
        }
        eventRepository.editEventAsUser(event.uid, currentUserId, join = true)
        refreshEventsList()
      }
    } catch (e: Exception) {
      setErrorMessage(e.message ?: "Unknown error occurred while joining event")
    }
  }

  /**
   * Removes the current user from the participant list of the selected event.
   *
   * Updates the event in the repository and refreshes [allEvents] and [joinedEvents].
   */
  suspend fun leaveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      if (event.participantIds.contains(currentUserId)) {
        eventRepository.editEventAsUser(event.uid, currentUserId, join = false)
        refreshEventsList()
      }
    } catch (e: Exception) {
      setErrorMessage(e.message ?: "Unknown error occurred while leaving event")
    }
  }

  /**
   * Saves the selected event for the current user by adding it to their savedEventIds.
   *
   * Updates the user's profile in Firestore and refreshes [savedEvents].
   */
  suspend fun saveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      if (!_savedEvents.any { it.uid == event.uid }) {
        val userProfile =
            userProfileRepository.getUserProfile(currentUserId)
                ?: throw Exception("User profile not found")
        val updatedSavedEventIds = userProfile.savedEventIds + event.uid
        val updatedProfile = userProfile.copy(savedEventIds = updatedSavedEventIds)
        userProfileRepository.saveUserProfile(updatedProfile)
        loadSavedEvents()
      }
    } catch (e: Exception) {
      setErrorMessage(e.message ?: "Unknown error occurred while saving event")
    }
  }

  /**
   * Unsaves the selected event for the current user by removing it from their savedEventIds.
   *
   * Updates the user's profile in Firestore and refreshes [savedEvents].
   */
  suspend fun unsaveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      if (_savedEvents.any { it.uid == event.uid }) {
        val userProfile =
            userProfileRepository.getUserProfile(currentUserId)
                ?: throw Exception("User profile not found")
        val updatedSavedEventIds = userProfile.savedEventIds - event.uid
        val updatedProfile = userProfile.copy(savedEventIds = updatedSavedEventIds)
        userProfileRepository.saveUserProfile(updatedProfile)
        loadSavedEvents()
      }
    } catch (e: Exception) {
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

  /** Clears all user-scoped state (joined, saved, etc.). */
  fun clearUserScopedState() {
    _allEvents = emptyList()
    _searchResults = emptyList()
    _availableEvents = emptyList()
    _joinedEvents = emptyList()
    _attendedEvents = emptyList()
    _savedEvents = emptyList()
  }

  /** Clears the current search results. */
  fun clearSearchResults() {
    _searchResults = emptyList()
  }

  /** Clears the current error message. */
  fun clearError() {
    clearErrorMessage()
  }

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

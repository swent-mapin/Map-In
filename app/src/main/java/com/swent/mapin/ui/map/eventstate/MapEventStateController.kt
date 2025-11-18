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
   * Populates [joinedEvents] by fetching each event from the repository based on the participating
   * event IDs in the user's profile. This reflects the events where the current user is a
   * participant.
   *
   * Errors encountered during loading are transmitted to caller.
   */
  fun loadJoinedEvents() {
    scope.launch {
      try {
        val currentUserId = getUserId()
        val user = userProfileRepository.getUserProfile(currentUserId)
        requireNotNull(user) { "User profile not found" }

        val joinedEventsList = mutableListOf<Event>()
        for (eventId in user.participatingEventIds) {
          val event = eventRepository.getEvent(eventId)
          if (event.participantIds.contains(currentUserId)) {
            joinedEventsList.add(event)
          } else {
            throw Exception(
                "Inconsistent data: User not in participant list for event $eventId but event ID is in user's participatingEventIds")
          }
        }
        _joinedEvents = joinedEventsList
      } catch (e: Exception) {
        setErrorMessage(e.message ?: "Unknown error occurred while fetching joined events")
      }
    }
  }

  /**
   * Loads the list of events that the current user has saved for later reference.
   *
   * Populates [savedEvents] by fetching the saved events from the repository. Errors encountered
   * during loading are transmitted to caller.
   */
  fun loadSavedEvents() {
    scope.launch {
      try {
        val currentUserId = getUserId()
        _savedEvents = eventRepository.getSavedEvents(currentUserId)
      } catch (e: Exception) {
        setErrorMessage(e.message ?: "Unknown error occurred while fetching saved events")
      }
    }
  }

  /**
   * Loads events owned by the current authenticated user. Repository doesn't provide a direct query
   * so we fetch all events and filter by ownerId. Errors are surfaced via setErrorMessage.
   */
  fun loadOwnedEvents() {
    scope.launch {
      _ownedLoading = true
      _ownedError = null
      try {
        val currentUserId = getUserId()
        val all = eventRepository.getAllEvents()
        _ownedEvents = all.filter { it.ownerId == currentUserId }
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
   * Adds the current user as a participant to the selected event.
   *
   * Updates the event in the repository and refreshes [allEvents] and [joinedEvents]. Any
   * exceptions encountered are transmitted to caller.
   */
  suspend fun joinSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      if (!event.participantIds.contains(currentUserId)) {
        require(event.capacity == null || event.capacity > event.participantIds.size) {
          "Event is at full capacity: ${event.participantIds.size} out of ${event.capacity}"
        }

        // Update event's participant list
        val updatedParticipantIds =
            event.participantIds.toMutableList().apply { add(currentUserId) }
        val updatedEvent = event.copy(participantIds = updatedParticipantIds)

        // Update locally the event list immediately
        _allEvents = allEvents.map { if (it.uid == event.uid) updatedEvent else it }

        try {
          eventRepository.editEvent(event.uid, updatedEvent)

          // Update user's participatingEventIds
          val userProfile =
              userProfileRepository.getUserProfile(currentUserId)
                  ?: throw Exception("User profile not found")
          if (!userProfile.participatingEventIds.contains(event.uid)) {
            val updatedParticipatingEventIds =
                userProfile.participatingEventIds.toMutableList().apply { add(event.uid) }
            val updatedUserProfile =
                userProfile.copy(participatingEventIds = updatedParticipatingEventIds)
            userProfileRepository.saveUserProfile(updatedUserProfile)
          }
        } catch (e: Exception) {
          // Revert local change if remote update fails
          _allEvents = allEvents.map { if (it.uid == event.uid) event else it }
          throw e
        }
      }

      refreshEventsList()
    } catch (e: Exception) {
      setErrorMessage(e.message ?: "Unknown error occurred while joining event")
    }
  }

  /**
   * Removes the current user from the participant list of the selected event.
   *
   * Updates the event in the repository and refreshes [allEvents] and [joinedEvents]. Any
   * exceptions encountered are transmitted to caller.
   */
  suspend fun leaveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      // Update event's participant list
      val updatedParticipantIds =
          event.participantIds.toMutableList().apply { remove(currentUserId) }
      val updatedEvent = event.copy(participantIds = updatedParticipantIds)
      // Update locally the event list immediately
      _allEvents = allEvents.map { if (it.uid == event.uid) updatedEvent else it }

      try {
        eventRepository.editEvent(event.uid, updatedEvent)

        val userProfile =
            userProfileRepository.getUserProfile(currentUserId)
                ?: throw Exception("User profile not found")
        val updatedParticipatingEventIds =
            userProfile.participatingEventIds.toMutableList().apply { remove(event.uid) }
        val updatedUserProfile =
            userProfile.copy(participatingEventIds = updatedParticipatingEventIds)
        userProfileRepository.saveUserProfile(updatedUserProfile)
      } catch (e: Exception) {
        // Revert local change if remote update fails
        _allEvents = allEvents.map { if (it.uid == event.uid) event else it }
        throw e
      }

      refreshEventsList()
    } catch (e: Exception) {
      setErrorMessage(e.message ?: "Unknown error occurred while unregistering from event")
    }
  }

  /**
   * Saves the selected event for the current user.
   *
   * Updates the repository and refreshes [savedEvents] to include the newly saved event. Any
   * exceptions encountered are transmitted to caller.
   */
  suspend fun saveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      eventRepository.saveEventForUser(currentUserId, event.uid)
      loadSavedEvents()
    } catch (e: Exception) {
      setErrorMessage(e.message ?: "Unknown error occurred while saving event")
    }
  }

  /**
   * Unsaves the selected event for the current user.
   *
   * Updates the repository and refreshes [savedEvents] to remove the event. Any exceptions
   * encountered are transmitted to caller.
   */
  suspend fun unsaveSelectedEvent() {
    val event = getSelectedEvent() ?: return
    try {
      val currentUserId = getUserId()
      eventRepository.unsaveEventForUser(currentUserId, event.uid)
      loadSavedEvents()
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

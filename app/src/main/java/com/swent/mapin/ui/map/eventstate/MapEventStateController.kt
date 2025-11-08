package com.swent.mapin.ui.map.eventstate

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Encapsulates saved/joined/available event state and all repository interactions (join, save,
 * etc.) so the ViewModel only coordinates UI state.
 */
class MapEventStateController(
    private val eventRepository: EventRepository,
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope,
    private val replaceEventInSearch: (Event) -> List<Event>,
    private val updateEventsState: (List<Event>) -> Unit,
    private val getSelectedEvent: () -> Event?,
    private val setSelectedEvent: (Event?) -> Unit,
    private val setErrorMessage: (String) -> Unit,
    private val clearErrorMessage: () -> Unit
) {

  private var baseEvents: List<Event> = emptyList()

  private var _availableEvents by mutableStateOf<List<Event>>(emptyList())
  val availableEvents: List<Event>
    get() = _availableEvents

  private var _joinedEvents by mutableStateOf<List<Event>>(emptyList())
  val joinedEvents: List<Event>
    get() = _joinedEvents

  private var _savedEvents by mutableStateOf<List<Event>>(emptyList())
  val savedEvents: List<Event>
    get() = _savedEvents

  private var _savedEventIds by mutableStateOf<Set<String>>(emptySet())
  val savedEventIds: Set<String>
    get() = _savedEventIds

  fun updateBaseEvents(events: List<Event>) {
    baseEvents = events
    refreshJoinedEvents()
  }

  fun loadParticipantEvents() {
    scope.launch {
      try {
        val currentUserId = auth.currentUser?.uid
        _availableEvents =
            if (currentUserId != null) eventRepository.getEventsByParticipant(currentUserId)
            else emptyList()
      } catch (e: Exception) {
        Log.e(TAG, "Error loading participant events", e)
        _availableEvents = emptyList()
      }
    }
  }

  fun loadSavedEvents() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _savedEvents = emptyList()
      return
    }
    scope.launch {
      try {
        _savedEvents = eventRepository.getSavedEvents(currentUserId)
      } catch (e: Exception) {
        Log.e(TAG, "Error loading saved events", e)
        _savedEvents = emptyList()
      }
    }
  }

  fun loadSavedEventIds() {
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      _savedEventIds = emptySet()
      return
    }
    scope.launch {
      try {
        _savedEventIds = eventRepository.getSavedEventIds(currentUserId)
      } catch (e: Exception) {
        Log.e(TAG, "Error loading saved event ids", e)
        _savedEventIds = emptySet()
      }
    }
  }

  fun joinSelectedEvent() {
    val event = getSelectedEvent() ?: return
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      setErrorMessage("You must be signed in to join events")
      return
    }

    scope.launch {
      clearErrorMessage()
      try {
        val capacity = event.capacity
        if (capacity != null && event.participantIds.size >= capacity) {
          setErrorMessage("Event is at full capacity")
          return@launch
        }
        val updatedParticipantIds =
            event.participantIds.toMutableList().apply {
              if (!contains(currentUserId)) add(currentUserId)
            }
        val updatedEvent = event.copy(participantIds = updatedParticipantIds)
        eventRepository.editEvent(event.uid, updatedEvent)
        setSelectedEvent(updatedEvent)
        val updatedList = replaceEventInSearch(updatedEvent)
        updateEventsState(updatedList)
        updateBaseEvents(updatedList)
      } catch (e: Exception) {
        setErrorMessage("Failed to join event: ${e.message}")
      }
    }
  }

  fun unregisterSelectedEvent() {
    val event = getSelectedEvent() ?: return
    val currentUserId = auth.currentUser?.uid ?: return
    scope.launch {
      clearErrorMessage()
      try {
        val updatedParticipantIds =
            event.participantIds.toMutableList().apply { remove(currentUserId) }
        val updatedEvent = event.copy(participantIds = updatedParticipantIds)
        eventRepository.editEvent(event.uid, updatedEvent)
        setSelectedEvent(updatedEvent)
        val updatedList = replaceEventInSearch(updatedEvent)
        updateEventsState(updatedList)
        updateBaseEvents(updatedList)
      } catch (e: Exception) {
        setErrorMessage("Failed to unregister: ${e.message}")
      }
    }
  }

  fun saveSelectedEventForLater() {
    val eventUid = getSelectedEvent()?.uid ?: return
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      setErrorMessage("You must be signed in to save events")
      return
    }

    scope.launch {
      clearErrorMessage()
      val previousSavedIds = _savedEventIds
      val previousSaved = _savedEvents

      _savedEventIds = _savedEventIds + eventUid
      val eventToAdd = baseEvents.find { it.uid == eventUid } ?: getSelectedEvent()
      if (eventToAdd != null && _savedEvents.none { it.uid == eventUid }) {
        _savedEvents = listOf(eventToAdd) + _savedEvents
      }
      setSelectedEvent(getSelectedEvent()?.copy())

      try {
        val success = eventRepository.saveEventForUser(currentUserId, eventUid)
        if (success) {
          loadSavedEvents()
        } else {
          _savedEventIds = previousSavedIds
          _savedEvents = previousSaved
          setErrorMessage("Event is already saved")
        }
      } catch (e: Exception) {
        _savedEventIds = previousSavedIds
        _savedEvents = previousSaved
        setErrorMessage("Failed to save event: ${e.message}")
      }
    }
  }

  fun unsaveSelectedEvent() {
    val eventUid = getSelectedEvent()?.uid ?: return
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      setErrorMessage("You must be signed in to unsave events")
      return
    }

    _savedEventIds = _savedEventIds - eventUid
    _savedEvents = _savedEvents.filter { it.uid != eventUid }
    setSelectedEvent(getSelectedEvent()?.copy())

    scope.launch {
      clearErrorMessage()
      try {
        val success = eventRepository.unsaveEventForUser(currentUserId, eventUid)
        if (!success) {
          setErrorMessage("Could not remove saved status on server; action recorded locally.")
          loadSavedEvents()
        } else {
          loadSavedEvents()
        }
      } catch (_: Exception) {
        setErrorMessage("Failed to unsave (offline). Change will sync when online.")
        loadSavedEvents()
      }
    }
  }

  private fun refreshJoinedEvents() {
    val uid = auth.currentUser?.uid
    _joinedEvents = if (uid == null) emptyList() else baseEvents.filter { uid in it.participantIds }
  }

  fun clearUserScopedState() {
    _savedEvents = emptyList()
    _savedEventIds = emptySet()
    _joinedEvents = emptyList()
  }

  companion object {
    private const val TAG = "MapEventStateController"
  }
}

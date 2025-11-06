package com.swent.mapin.ui.event

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.event.LocalEventRepository
import com.swent.mapin.ui.map.Filters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Assisted by AI

class EventViewModel(
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository()
) : ViewModel() {

  private val _events = MutableStateFlow<List<Event>>(emptyList())
  val events: StateFlow<List<Event>> = _events.asStateFlow()

  private val _availableEvents = MutableStateFlow<List<Event>>(emptyList())
  val availableEvents: StateFlow<List<Event>> = _availableEvents.asStateFlow()

  private val _searchResults = MutableStateFlow<List<Event>>(emptyList())
  val searchResults: StateFlow<List<Event>> = _searchResults.asStateFlow()

  private val _gotEvent = MutableStateFlow<Event?>(null)
  val gotEvent: StateFlow<Event?> = _gotEvent.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _savedEvents = MutableStateFlow<List<Event>>(emptyList())
  val savedEvents: StateFlow<List<Event>> = _savedEvents.asStateFlow()

  private val _savedEventIds = MutableStateFlow<Set<String>>(emptySet())
  val savedEventIds: StateFlow<Set<String>> = _savedEventIds.asStateFlow()

  private val _joinedEvents = MutableStateFlow<List<Event>>(emptyList())
  val joinedEvents: StateFlow<List<Event>> = _joinedEvents.asStateFlow()

  fun getNewUid(): String {
    return eventRepository.getNewUid()
  }

  fun loadInitialSamples() {
    val samples = LocalEventRepository.defaultSampleEvents()
    _events.value = samples
    _availableEvents.value = samples
    _searchResults.value = samples
  }

  fun getAllEvents() {
    viewModelScope.launch {
      try {
        val all = eventRepository.getAllEvents()
        _events.value = all
        _availableEvents.value = all
        _searchResults.value = all
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getEvent(eventId: String) {
    viewModelScope.launch {
      try {
        _gotEvent.value = eventRepository.getEvent(eventId)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getSearchedEvents(searchQuery: String) {
    viewModelScope.launch {
      try {
        val results = eventRepository.getSearchedEvents(searchQuery)
        _events.value = results
        _searchResults.value = results
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getFilteredEvents(filters: Filters) {
    viewModelScope.launch {
      try {
        val filtered = eventRepository.getFilteredEvents(filters)
        _events.value = filtered
        _searchResults.value = filtered
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getSavedEvents(userId: String) {
    viewModelScope.launch {
      try {
        _savedEvents.value = eventRepository.getSavedEvents(userId)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getSavedEventIds(userId: String) {
    viewModelScope.launch {
      try {
        _savedEventIds.value = eventRepository.getSavedEventIds(userId)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  suspend fun fetchAllEvents(): List<Event> = eventRepository.getAllEvents()

  suspend fun fetchFilteredEvents(filters: Filters): List<Event> =
      eventRepository.getFilteredEvents(filters)

  suspend fun fetchSearchedEvents(searchQuery: String): List<Event> =
      eventRepository.getSearchedEvents(searchQuery)

  suspend fun fetchSavedEvents(userId: String): List<Event> = eventRepository.getSavedEvents(userId)

  suspend fun fetchSavedEventIds(userId: String): Set<String> =
      eventRepository.getSavedEventIds(userId)

  suspend fun joinEventForUser(userId: String, eventId: String): Event? {
    return try {
      val event = eventRepository.getEvent(eventId)
      val updatedParticipantIds =
          event.participantIds.toMutableList().apply { if (!contains(userId)) add(userId) }
      val updatedEvent = event.copy(participantIds = updatedParticipantIds)
      eventRepository.editEvent(eventId, updatedEvent)
      _events.value = _events.value.map { if (it.uid == eventId) updatedEvent else it }
      _joinedEvents.value = (_joinedEvents.value + updatedEvent).distinctBy { it.uid }
      updatedEvent
    } catch (e: Exception) {
      _error.value = e.message
      null
    }
  }

  suspend fun unregisterUserFromEvent(userId: String, eventId: String): Event? {
    return try {
      val event = eventRepository.getEvent(eventId)
      val updatedParticipantIds = event.participantIds.toMutableList().apply { remove(userId) }
      val updatedEvent = event.copy(participantIds = updatedParticipantIds)
      eventRepository.editEvent(eventId, updatedEvent)
      _events.value = _events.value.map { if (it.uid == eventId) updatedEvent else it }
      _joinedEvents.value = _joinedEvents.value.filter { it.uid != eventId }
      updatedEvent
    } catch (e: Exception) {
      _error.value = e.message
      null
    }
  }

  fun saveEventForUser(userId: String, eventId: String) {
    viewModelScope.launch {
      try {
        val success = eventRepository.saveEventForUser(userId, eventId)
        if (success) {
          _savedEventIds.value = _savedEventIds.value + eventId
          getSavedEvents(userId)
        } else {
          _error.value = "Event is already saved"
        }
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun unsaveEventForUser(userId: String, eventId: String) {
    viewModelScope.launch {
      try {
        val success = eventRepository.unsaveEventForUser(userId, eventId)
        if (success) {
          _savedEventIds.value = _savedEventIds.value - eventId
          getSavedEvents(userId)
        } else {
          _error.value = "Event was not saved"
        }
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun addEvent(event: Event) {
    viewModelScope.launch {
      try {
        eventRepository.addEvent(event)
        getAllEvents()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun editEvent(eventId: String, event: Event) {
    viewModelScope.launch {
      try {
        eventRepository.editEvent(eventId, event)
        getAllEvents()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun deleteEvent(eventId: String) {
    viewModelScope.launch {
      try {
        eventRepository.deleteEvent(eventId)
        getAllEvents()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun clearError() {
    _error.value = null
  }
}

@Composable
fun rememberEventViewModel(
    viewModelStoreOwner: ViewModelStoreOwner =
        LocalViewModelStoreOwner.current ?: error("No ViewModelStoreOwner provided")
): EventViewModel {
  return viewModel(
      viewModelStoreOwner = viewModelStoreOwner,
      key = "EventViewModel",
      factory =
          object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
              return EventViewModel() as T
            }
          })
}

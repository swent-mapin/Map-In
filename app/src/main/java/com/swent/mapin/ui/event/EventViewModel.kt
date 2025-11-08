package com.swent.mapin.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel(
    private val eventRepository: EventRepository = EventRepositoryFirestore(db = Firebase.firestore)
) : ViewModel() {

  private val _events = MutableStateFlow<List<Event>>(emptyList())
  val events: StateFlow<List<Event>> = _events.asStateFlow()

  private val _gotEvent = MutableStateFlow<Event?>(null)
  val gotEvent: StateFlow<Event?> = _gotEvent.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun getNewUid(): String {
    return eventRepository.getNewUid()
  }

  fun getAllEvents() {
    viewModelScope.launch {
      try {
        _events.value = eventRepository.getAllEvents()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getEvent(eventID: String) {
    viewModelScope.launch {
      try {
        _gotEvent.value = eventRepository.getEvent(eventID)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getFilteredEvents(filters: com.swent.mapin.ui.filters.Filters) {
    viewModelScope.launch {
      try {
        _events.value = eventRepository.getFilteredEvents(filters)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun getSearchedEvents(title: String, filters: com.swent.mapin.ui.filters.Filters) {
    viewModelScope.launch {
      try {
        _events.value = eventRepository.getSearchedEvents(title, filters)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun addEvent(event: Event) {
    viewModelScope.launch {
      try {
        eventRepository.addEvent(event)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun editEvent(eventID: String, event: Event) {
    viewModelScope.launch {
      try {
        eventRepository.editEvent(eventID, event)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun deleteEvent(eventID: String) {
    viewModelScope.launch {
      try {
        eventRepository.deleteEvent(eventID)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun clearError() {
    _error.value = null
  }
}

package com.swent.mapin.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.map.eventstate.MapEventStateController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel(
    private val eventRepository: EventRepository,
    private val stateController: MapEventStateController
) : ViewModel() {
  private val _error = MutableStateFlow<String?>(null)
  val error = _error.asStateFlow()

  fun getNewUid(): String {
    return eventRepository.getNewUid()
  }

  fun addEvent(event: Event) {
    viewModelScope.launch {
      try {
        eventRepository.addEvent(event)
        stateController.refreshEventsList()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun editEvent(eventID: String, event: Event) {
    viewModelScope.launch {
      try {
        eventRepository.editEvent(eventID, event)
        stateController.refreshEventsList()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun deleteEvent(eventID: String) {
    viewModelScope.launch {
      try {
        eventRepository.deleteEvent(eventID)
        stateController.refreshEventsList()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  /** Clears the current error message. */
  fun clearError() {
    _error.value = null
  }
}

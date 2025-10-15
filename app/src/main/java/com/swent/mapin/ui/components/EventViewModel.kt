package com.swent.mapin.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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

  fun getNewUid(): String {
    return eventRepository.getNewUid()
  }

  fun getAllEvents() {
    viewModelScope.launch { _events.value = eventRepository.getAllEvents() }
  }

  fun getEvent(eventID: String) {
    viewModelScope.launch { _gotEvent.value = eventRepository.getEvent(eventID) }
  }

  fun getEventByTags(tags: List<String>) {
    viewModelScope.launch { _events.value = eventRepository.getEventsByTags(tags) }
  }

  fun getEventsOnDay(dayStart: Timestamp, dayEnd: Timestamp) {
    viewModelScope.launch { _events.value = eventRepository.getEventsOnDay(dayStart, dayEnd) }
  }

  fun getEventsByOwner(ownerId: String) {
    viewModelScope.launch { _events.value = eventRepository.getEventsByOwner(ownerId) }
  }

  fun addEvent(event: Event) {
    viewModelScope.launch { eventRepository.addEvent(event) }
  }

  fun editEvent(eventID: String, event: Event) {
    viewModelScope.launch { eventRepository.editEvent(eventID, event) }
  }

  fun deleteEvent(eventID: String) {
    viewModelScope.launch { eventRepository.deleteEvent(eventID) }
  }
}

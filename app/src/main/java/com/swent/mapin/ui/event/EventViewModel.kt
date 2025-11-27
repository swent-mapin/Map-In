package com.swent.mapin.ui.event

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.map.eventstate.MapEventStateController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel(
    private val eventRepository: EventRepository,
    private val stateController: MapEventStateController,
    private val firebaseAuth: FirebaseAuth = Firebase.auth
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

  private val _eventToEdit = MutableStateFlow<Event?>(null)
  val eventToEdit = _eventToEdit.asStateFlow()

  fun selectEventToEdit(event: Event) {
    _eventToEdit.value = event
  }

  fun clearEventToEdit() {
    _eventToEdit.value = null
  }

  fun editEvent(eventID: String, event: Event) {
    viewModelScope.launch {
      try {
        eventRepository.editEventAsOwner(eventID, event)
        stateController.refreshEventsList()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun saveEditedEvent(
      originalEvent: Event,
      title: String,
      description: String,
      location: Location,
      startTs: Timestamp,
      endTs: Timestamp,
      tagsString: String,
      onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      try {
        val editedEvent =
            originalEvent.copy(
                title = title,
                description = description,
                location = location,
                date = startTs,
                endDate = endTs,
                tags = extractTags(tagsString))

        val currentUser = firebaseAuth.currentUser
        if (currentUser?.uid == originalEvent.ownerId) {
          editEvent(originalEvent.uid, editedEvent)
          onSuccess()
        } else {
          Log.e("InvalidEditEvent", "You are not the owner of the event!")
        }
      } catch (e: Exception) {
        Log.e("InvalidEditEvent", "Unknown error $e")
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

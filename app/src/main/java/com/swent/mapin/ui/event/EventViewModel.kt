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

/**
 * ViewModel for managing event operations and state.
 *
 * Coordinates event creation, editing, and deletion operations while maintaining UI state. Works
 * closely with MapEventStateController to refresh event lists after modifications.
 *
 * @property eventRepository Repository for event data operations
 * @property stateController Controller for managing map event state
 * @property firebaseAuth Firebase authentication instance
 */
class EventViewModel(
    private val eventRepository: EventRepository,
    private val stateController: MapEventStateController,
    private val firebaseAuth: FirebaseAuth = Firebase.auth
) : ViewModel() {
  private val _error = MutableStateFlow<String?>(null)
  /** Error message from failed operations */
  val error = _error.asStateFlow()

  /**
   * Generates a new unique identifier for an event.
   *
   * @return A unique event ID string
   */
  fun getNewUid(): String {
    return eventRepository.getNewUid()
  }

  /**
   * Adds a new event to the repository. Refreshes the event list on success.
   *
   * @param event The event to add
   */
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
  /** Currently selected event for editing */
  val eventToEdit = _eventToEdit.asStateFlow()

  /**
   * Selects an event for editing.
   *
   * @param event The event to edit
   */
  fun selectEventToEdit(event: Event) {
    _eventToEdit.value = event
  }

  /** Clears the currently selected event for editing */
  fun clearEventToEdit() {
    _eventToEdit.value = null
  }

  /**
   * Edits an existing event as the owner. Refreshes the event list on success.
   *
   * @param eventID The ID of the event to edit
   * @param event The updated event data
   */
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

  /**
   * Saves an edited event after validating ownership.
   *
   * @param originalEvent The original event before editing
   * @param title Updated title
   * @param description Updated description
   * @param location Updated location
   * @param startTs Updated start timestamp
   * @param endTs Updated end timestamp
   * @param tagsString Updated tags as comma-separated string
   * @param onSuccess Callback invoked on successful save
   */
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

  /**
   * Deletes an event from the repository. Refreshes the event list on success.
   *
   * @param eventID The ID of the event to delete
   */
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

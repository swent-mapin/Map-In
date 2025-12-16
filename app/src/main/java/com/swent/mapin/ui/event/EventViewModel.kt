package com.swent.mapin.ui.event

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.location.Location
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
 * **Error Handling:** Errors from repository operations are exposed through the [error] StateFlow.
 * The UI should observe this flow and display error messages to users (e.g., in Snackbars or
 * AlertDialogs). Call [clearError] after displaying the error to reset the state.
 *
 * **State Communication:**
 * - [error]: Emits error messages from failed operations (addEvent, editEvent, deleteEvent)
 * - [eventToEdit]: Emits the currently selected event for editing, or null when cleared
 * - UI composables should collect these StateFlows to react to state changes
 * - Map event list updates are handled automatically via MapEventStateController
 *
 * **Usage Example:**
 *
 * ```
 * @Composable
 * fun EventScreen(viewModel: EventViewModel) {
 *     val error by viewModel.error.collectAsState()
 *     val eventToEdit by viewModel.eventToEdit.collectAsState()
 *
 *     error?.let { errorMsg ->
 *         Snackbar(message = errorMsg, onDismiss = { viewModel.clearError() })
 *     }
 * }
 * ```
 *
 * @property eventRepository Repository for event data operations
 * @property stateController Controller for managing map event state and triggering UI refreshes
 * @property firebaseAuth Firebase authentication instance for ownership validation
 */
class EventViewModel(
    private val eventRepository: EventRepository,
    private val stateController: MapEventStateController,
    private val firebaseAuth: FirebaseAuth = Firebase.auth
) : ViewModel() {
  private val _error = MutableStateFlow<String?>(null)
  /**
   * Error message from failed operations.
   *
   * **UI Communication:** Observe this StateFlow in UI composables to display error messages to
   * users. Set to null after the error is displayed by calling [clearError].
   *
   * **When set:**
   * - Repository operations fail (network errors, permission issues, etc.)
   * - Set to the exception message for user-facing error display
   */
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
   * Adds a new event to the repository.
   *
   * **Side Effects:**
   * - On success: Triggers [stateController.refreshEventsList] to update map UI
   * - On failure: Sets [error] StateFlow with exception message for UI display
   *
   * **UI Impact:** The map screen will automatically refresh to show the new event. Any errors
   * should be displayed to the user via the [error] StateFlow.
   *
   * @param event The event to add to the repository
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
  /**
   * Currently selected event for editing.
   *
   * **UI Communication:** Observe this StateFlow to navigate to edit screens or populate edit
   * forms. Set to null when editing is complete or cancelled.
   *
   * **State Changes:**
   * - Non-null: An event is selected for editing (set via [selectEventToEdit])
   * - Null: No event selected (cleared via [clearEventToEdit])
   */
  val eventToEdit = _eventToEdit.asStateFlow()

  /**
   * Selects an event for editing.
   *
   * **State Changes:** Sets [eventToEdit] to the provided event. UI should observe this flow and
   * navigate to or display the edit screen when a non-null event is emitted.
   *
   * @param event The event to edit
   */
  fun selectEventToEdit(event: Event) {
    _eventToEdit.value = event
  }

  /**
   * Clears the currently selected event for editing.
   *
   * **State Changes:** Sets [eventToEdit] to null. UI should observe this and dismiss edit screens
   * or forms.
   */
  fun clearEventToEdit() {
    _eventToEdit.value = null
  }

  /**
   * Edits an existing event as the owner.
   *
   * **Side Effects:**
   * - On success: Triggers [stateController.refreshEventsList] to update map UI
   * - On failure: Sets [error] StateFlow with exception message for UI display
   *
   * **UI Impact:** The map and event detail screens will automatically refresh to show updated
   * event data.
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
   * **Validation:** Checks that the current authenticated user is the owner of the event. If
   * validation fails, logs an error but does NOT surface it to [error] StateFlow (ownership
   * failures are logged only).
   *
   * **Side Effects:**
   * - On success: Calls [editEvent] which triggers map refresh, then invokes [onSuccess] callback
   * - On validation failure: Logs error to Logcat (not exposed to UI)
   * - On exception: Logs error to Logcat (not exposed to UI)
   *
   * **Parameters:**
   *
   * @param originalEvent The original event before editing (used for ownership validation)
   * @param title Updated event title
   * @param description Updated event description
   * @param location Updated event location
   * @param startTs Updated start timestamp
   * @param endTs Updated end timestamp
   * @param tagsString Updated tags as comma-separated string (will be parsed)
   * @param onSuccess Callback invoked when edit succeeds (use for navigation or UI updates)
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
      mediaUri: Uri?,
  ) {
    viewModelScope.launch {
      try {
        val mediaUrl = mediaUri?.let { uploadEventMedia(it, originalEvent.ownerId) }

        val editedEvent =
            originalEvent.copy(
                title = title,
                description = description,
                location = location,
                date = startTs,
                endDate = endTs,
                tags = extractTags(tagsString),
                imageUrl = mediaUrl)

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
   * Deletes an event from the repository.
   *
   * **Side Effects:**
   * - On success: Triggers [stateController.refreshEventsList] to update map UI
   * - On failure: Sets [error] StateFlow with exception message for UI display
   *
   * **UI Impact:** The map screen will automatically refresh to remove the deleted event. The UI
   * should observe [error] to display any deletion failures.
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

  /**
   * Clears the current error message.
   *
   * **State Changes:** Sets [error] to null. Call this after displaying an error message to users
   * to reset the error state and prevent showing the same error multiple times.
   *
   * **Usage:** Typically called from the UI after the user dismisses a Snackbar or AlertDialog
   * showing the error.
   */
  fun clearError() {
    _error.value = null
  }
}

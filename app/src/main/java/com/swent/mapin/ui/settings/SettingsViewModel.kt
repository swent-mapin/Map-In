package com.swent.mapin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Data class representing map preferences */
data class MapPreferences(
    val showPOIs: Boolean = true,
    val showRoadNumbers: Boolean = true,
    val showStreetNames: Boolean = true,
    val enable3DView: Boolean = false
)

/**
 * ViewModel for managing settings state and operations.
 *
 * Responsibilities:
 * - Manage map preferences/visibility toggles
 * - Handle logout
 * - Handle account deletion
 * - Persist settings to Firestore
 */
class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

  // Map preferences state
  private val _mapPreferences = MutableStateFlow(MapPreferences())
  val mapPreferences: StateFlow<MapPreferences> = _mapPreferences.asStateFlow()

  // Loading state for async operations
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Error state for displaying error messages
  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  init {
    // Load saved preferences from Firestore
    loadMapPreferences()
  }

  /** Load map preferences from Firestore */
  private fun loadMapPreferences() {
    viewModelScope.launch {
      try {
        val userId = auth.currentUser?.uid
        if (userId != null) {
          firestore.collection("settings").document(userId).addSnapshotListener { doc, error ->
            if (error != null) {
              _errorMessage.value = "Failed to load preferences: ${error.message}"
              _mapPreferences.value = MapPreferences()
              return@addSnapshotListener
            }

            if (doc != null && doc.exists()) {
              try {
                val prefs =
                    MapPreferences(
                        showPOIs = doc.getBoolean("showPOIs") ?: true,
                        showRoadNumbers = doc.getBoolean("showRoadNumbers") ?: true,
                        showStreetNames = doc.getBoolean("showStreetNames") ?: true,
                        enable3DView = doc.getBoolean("enable3DView") ?: false)
                _mapPreferences.value = prefs
              } catch (e: Exception) {
                _errorMessage.value = "Failed to parse preferences: ${e.message}"
                _mapPreferences.value = MapPreferences()
              }
            } else {
              _mapPreferences.value = MapPreferences()
            }
          }
        } else {
          _mapPreferences.value = MapPreferences()
        }
      } catch (e: Exception) {
        _errorMessage.value = "Failed to load preferences: ${e.message}"
        _mapPreferences.value = MapPreferences()
      }
    }
  }

  /** Update POI visibility setting */
  fun updateShowPOIs(show: Boolean) {
    _mapPreferences.value = _mapPreferences.value.copy(showPOIs = show)
    saveMapPreferences()
  }

  /** Update road numbers visibility setting */
  fun updateShowRoadNumbers(show: Boolean) {
    _mapPreferences.value = _mapPreferences.value.copy(showRoadNumbers = show)
    saveMapPreferences()
  }

  /** Update street names visibility setting */
  fun updateShowStreetNames(show: Boolean) {
    _mapPreferences.value = _mapPreferences.value.copy(showStreetNames = show)
    saveMapPreferences()
  }

  /** Update 3D view setting */
  fun updateEnable3DView(enable: Boolean) {
    _mapPreferences.value = _mapPreferences.value.copy(enable3DView = enable)
    saveMapPreferences()
  }

  /** Save map preferences to Firestore */
  private fun saveMapPreferences() {
    viewModelScope.launch {
      try {
        val userId = auth.currentUser?.uid
        if (userId != null) {
          val prefsMap =
              mapOf(
                  "showPOIs" to mapPreferences.value.showPOIs,
                  "showRoadNumbers" to mapPreferences.value.showRoadNumbers,
                  "showStreetNames" to mapPreferences.value.showStreetNames,
                  "enable3DView" to mapPreferences.value.enable3DView)
          firestore.collection("settings").document(userId).set(prefsMap).addOnFailureListener { e
            ->
            _errorMessage.value = "Failed to save preferences: ${e.message}"
            e.printStackTrace()
          }
        }
      } catch (e: Exception) {
        _errorMessage.value = "Failed to save preferences: ${e.message}"
        e.printStackTrace()
      }
    }
  }

  /** Sign out the current user */
  fun signOut() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        auth.signOut()
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Delete the current user's account and all associated data from Firestore */
  fun deleteAccount() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val user = auth.currentUser
        if (user != null) {
          val userId = user.uid

          // Delete user settings from Firestore
          firestore
              .collection("settings")
              .document(userId)
              .delete()
              .addOnSuccessListener {
                // Delete user profile from Firestore
                firestore
                    .collection("users")
                    .document(userId)
                    .delete()
                    .addOnSuccessListener {
                      // Finally delete the user account from Firebase Auth
                      user
                          .delete()
                          .addOnSuccessListener { _isLoading.value = false }
                          .addOnFailureListener { e ->
                            _errorMessage.value = "Failed to delete auth account: ${e.message}"
                            _isLoading.value = false
                          }
                    }
                    .addOnFailureListener { e ->
                      _errorMessage.value = "Failed to delete user profile: ${e.message}"
                      _isLoading.value = false
                    }
              }
              .addOnFailureListener { e ->
                _errorMessage.value = "Failed to delete settings: ${e.message}"
                _isLoading.value = false
              }
        } else {
          _isLoading.value = false
        }
      } catch (e: Exception) {
        _errorMessage.value = "Failed to delete account: ${e.message}"
        e.printStackTrace()
        _isLoading.value = false
      }
    }
  }
}

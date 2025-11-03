package com.swent.mapin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.PreferencesRepository
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

/** Enum for theme mode options */
enum class ThemeMode {
  LIGHT,
  DARK,
  SYSTEM;

  companion object {
    fun fromString(value: String): ThemeMode {
      return when (value.lowercase()) {
        "light" -> LIGHT
        "dark" -> DARK
        else -> SYSTEM
      }
    }
  }

  fun toDisplayString(): String {
    return when (this) {
      LIGHT -> "Light"
      DARK -> "Dark"
      SYSTEM -> "System"
    }
  }

  fun toStorageString(): String {
    return when (this) {
      LIGHT -> "light"
      DARK -> "dark"
      SYSTEM -> "system"
    }
  }
}

/**
 * ViewModel for managing settings state and operations.
 *
 * Responsibilities:
 * - Manage theme mode (light/dark/system)
 * - Manage map preferences/visibility toggles
 * - Handle logout
 * - Handle account deletion
 * - Persist settings to DataStore
 */
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

  // Theme mode state
  private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

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
    // Load saved preferences from DataStore and Firestore
    loadPreferences()
  }

  /** Load all preferences from DataStore */
  private fun loadPreferences() {
    viewModelScope.launch {
      // Load theme mode
      preferencesRepository.themeModeFlow.collect { mode ->
        _themeMode.value = ThemeMode.fromString(mode)
      }
    }
    viewModelScope.launch {
      // Load map preferences
      preferencesRepository.showPOIsFlow.collect { show ->
        _mapPreferences.value = _mapPreferences.value.copy(showPOIs = show)
      }
    }
    viewModelScope.launch {
      preferencesRepository.showRoadNumbersFlow.collect { show ->
        _mapPreferences.value = _mapPreferences.value.copy(showRoadNumbers = show)
      }
    }
    viewModelScope.launch {
      preferencesRepository.showStreetNamesFlow.collect { show ->
        _mapPreferences.value = _mapPreferences.value.copy(showStreetNames = show)
      }
    }
    viewModelScope.launch {
      preferencesRepository.enable3DViewFlow.collect { enable ->
        _mapPreferences.value = _mapPreferences.value.copy(enable3DView = enable)
      }
    }
  }

  /** Update theme mode */
  fun updateThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      try {
        _themeMode.value = mode
        preferencesRepository.setThemeMode(mode.toStorageString())
      } catch (e: Exception) {
        _errorMessage.value = "Failed to update theme: ${e.message}"
      }
    }
  }

  /** Update POI visibility setting */
  fun updateShowPOIs(show: Boolean) {
    viewModelScope.launch {
      try {
        _mapPreferences.value = _mapPreferences.value.copy(showPOIs = show)
        preferencesRepository.setShowPOIs(show)
      } catch (e: Exception) {
        _errorMessage.value = "Failed to update POI setting: ${e.message}"
      }
    }
  }

  /** Update road numbers visibility setting */
  fun updateShowRoadNumbers(show: Boolean) {
    viewModelScope.launch {
      try {
        _mapPreferences.value = _mapPreferences.value.copy(showRoadNumbers = show)
        preferencesRepository.setShowRoadNumbers(show)
      } catch (e: Exception) {
        _errorMessage.value = "Failed to update road numbers setting: ${e.message}"
      }
    }
  }

  /** Update street names visibility setting */
  fun updateShowStreetNames(show: Boolean) {
    viewModelScope.launch {
      try {
        _mapPreferences.value = _mapPreferences.value.copy(showStreetNames = show)
        preferencesRepository.setShowStreetNames(show)
      } catch (e: Exception) {
        _errorMessage.value = "Failed to update street names setting: ${e.message}"
      }
    }
  }

  /** Update 3D view setting */
  fun updateEnable3DView(enable: Boolean) {
    viewModelScope.launch {
      try {
        _mapPreferences.value = _mapPreferences.value.copy(enable3DView = enable)
        preferencesRepository.setEnable3DView(enable)
      } catch (e: Exception) {
        _errorMessage.value = "Failed to update 3D view setting: ${e.message}"
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

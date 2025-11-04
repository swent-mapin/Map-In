package com.swent.mapin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

  // Theme mode state - cached from DataStore using stateIn() to prevent repeated reads
  val themeMode: StateFlow<ThemeMode> =
      preferencesRepository.themeModeFlow
          .map { ThemeMode.fromString(it) }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = ThemeMode.SYSTEM)

  // Map preferences state - combined from multiple DataStore flows and cached
  val mapPreferences: StateFlow<MapPreferences> =
      combine(
              preferencesRepository.showPOIsFlow,
              preferencesRepository.showRoadNumbersFlow,
              preferencesRepository.showStreetNamesFlow,
              preferencesRepository.enable3DViewFlow) {
                  showPOIs,
                  showRoadNumbers,
                  showStreetNames,
                  enable3DView ->
                MapPreferences(
                    showPOIs = showPOIs,
                    showRoadNumbers = showRoadNumbers,
                    showStreetNames = showStreetNames,
                    enable3DView = enable3DView)
              }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = MapPreferences())

  // Loading state for async operations
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Error state for displaying error messages
  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  /** Clear error message after it has been displayed to the user */
  fun clearErrorMessage() {
    _errorMessage.value = null
  }

  /** Update theme mode */
  fun updateThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      try {
        // Just update DataStore - stateIn() will automatically emit the new value
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
        // Just update DataStore - stateIn() will automatically emit the new value
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
        // Just update DataStore - stateIn() will automatically emit the new value
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
        // Just update DataStore - stateIn() will automatically emit the new value
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
        // Just update DataStore - stateIn() will automatically emit the new value
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

  companion object {
    /**
     * Factory for creating SettingsViewModel with proper dependency injection.
     *
     * Usage in Composable:
     * ```
     * val viewModel: SettingsViewModel = viewModel(
     *   factory = SettingsViewModel.Factory(preferencesRepository)
     * )
     * ```
     */
    fun Factory(
        preferencesRepository: PreferencesRepository,
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
              return SettingsViewModel(preferencesRepository, auth, firestore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
          }
        }
  }
}

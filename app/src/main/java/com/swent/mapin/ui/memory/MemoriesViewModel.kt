package com.swent.mapin.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MemoryDisplayMode {
  OWNER_MEMORIES,
  NEARBY_MEMORIES
}

const val DEFAULT_MEMORY_RADIUS = 2.0

class MemoriesViewModel(
    private val memoryRepository: MemoryRepository,
    private val userRepository: UserProfileRepository = UserProfileRepository(),
    private val firebaseAuth: FirebaseAuth = Firebase.auth
) : ViewModel() {

  private val _error = MutableStateFlow<String?>(null)
  val error = _error.asStateFlow()

  private val _memories = MutableStateFlow<List<Memory>>(emptyList())
  val memories = _memories.asStateFlow()

  private val _selectedMemory = MutableStateFlow<Memory?>(null)
  val selectedMemory = _selectedMemory.asStateFlow()

  private val _ownerName = MutableStateFlow("")
  val ownerName = _ownerName.asStateFlow()

  private val _taggedNames = MutableStateFlow<List<String>>(emptyList())
  val taggedNames = _taggedNames.asStateFlow()

  private val _displayMode = MutableStateFlow(MemoryDisplayMode.OWNER_MEMORIES)
  val displayMode = _displayMode.asStateFlow()

  private val _nearbyLocation = MutableStateFlow(Location.UNDEFINED)
  private val _nearbyRadius = MutableStateFlow(DEFAULT_MEMORY_RADIUS)

  init {
    loadMemoriesOfOwner()
  }

  private fun requireUserUid(): String {
    return firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
  }

  fun loadMemoriesOfOwner() {
    viewModelScope.launch {
      try {
        val uid = requireUserUid()
        _memories.value = memoryRepository.getMemoriesByOwner(uid)
      } catch (e: IllegalStateException) {
        _error.value = e.message
      } catch (_: Exception) {
        _error.value = "Failed to load memories"
      }
    }
  }

  fun refresh() {
    when (_displayMode.value) {
      MemoryDisplayMode.OWNER_MEMORIES -> loadMemoriesOfOwner()
      MemoryDisplayMode.NEARBY_MEMORIES -> {
        _nearbyLocation.value.let { loc ->
          viewModelScope.launch { loadMemoriesNearLocation(loc, _nearbyRadius.value) }
        }
      }
    }
  }

  fun selectMemoryToView(memoryId: String) {
    viewModelScope.launch {
      try {
        _error.value = null
        _ownerName.value = ""
        _taggedNames.value = emptyList()

        val mem =
            _memories.value.find { it.uid == memoryId } ?: memoryRepository.getMemory(memoryId)

        _selectedMemory.value = mem

        // Owner name
        val ownerProfile = userRepository.getUserProfile(mem.ownerId)
        _ownerName.value = ownerProfile?.name ?: ""

        // Tagged names
        _taggedNames.value = mem.taggedUserIds
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun loadMemoriesNearLocation(location: Location, radius: Double) {
    viewModelScope.launch {
      try {
        _error.value = null
        _displayMode.value = MemoryDisplayMode.NEARBY_MEMORIES
        _nearbyLocation.value = location
        _nearbyRadius.value = radius

        _memories.value = memoryRepository.getMemoriesByLocation(location, radius)
      } catch (e: Exception) {
        _error.value = "Failed to load nearby memories: ${e.message}"
      }
    }
  }

  fun returnToOwnerMemories() {
    _displayMode.value = MemoryDisplayMode.OWNER_MEMORIES
    _nearbyLocation.value = Location.UNDEFINED
    loadMemoriesOfOwner()
  }

  fun clearSelectedMemory() {
    _selectedMemory.value = null
    _ownerName.value = ""
    _taggedNames.value = emptyList()
  }

  fun clearError() {
    _error.value = null
  }

  companion object {
    /** Factory for creating MemoriesViewModel with proper dependency injection. */
    fun Factory(
        memoryRepository: MemoryRepository,
        userRepository: UserProfileRepository = UserProfileRepository(),
        firebaseAuth: FirebaseAuth = Firebase.auth
    ): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MemoriesViewModel::class.java)) {
              return MemoriesViewModel(memoryRepository, userRepository, firebaseAuth) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
          }
        }
  }
}

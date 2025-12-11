package com.swent.mapin.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemoriesViewModel(
    private val memoryRepository: MemoryRepository,
    private val userRepository: UserProfileRepository = UserProfileRepository(),
    private val firebaseAuth: FirebaseAuth = Firebase.auth
) : ViewModel() {

  private val _error = MutableStateFlow<String?>(null)
  val error = _error.asStateFlow()

  private val _memories = MutableStateFlow<List<Memory>>(emptyList())
  val memories = _memories.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  private val _selectedMemory = MutableStateFlow<Memory?>(null)
  val selectedMemory = _selectedMemory.asStateFlow()

  private val _ownerName = MutableStateFlow("")
  val ownerName = _ownerName.asStateFlow()

  private val _taggedNames = MutableStateFlow<List<String>>(emptyList())
  val taggedNames = _taggedNames.asStateFlow()

  init {
    loadMemoriesOfOwner()
  }

  fun loadMemoriesOfOwner() {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        _memories.value = memoryRepository.getMemoriesByOwner(firebaseAuth.currentUser!!.uid)
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun refresh() = loadMemoriesOfOwner()

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

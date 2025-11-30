package com.swent.mapin.ui.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.ImageUploadHelper
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user profile state and operations.
 *
 * Responsibilities:
 * - Load user profile data from Firestore
 * - Handle profile editing mode
 * - Save profile changes to Firestore
 * - Manage temporary edit state
 * - Validate user inputs
 * - Upload images to Firebase Storage
 */
class ProfileViewModel(
    private val repository: UserProfileRepository = UserProfileRepository(),
    private val imageUploadHelper: ImageUploadHelper = ImageUploadHelper()
) : ViewModel() {

  // Current user profile from Firestore
  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // Loading state
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Whether the profile is in edit mode
  var isEditMode by mutableStateOf(false)
    private set

  // Whether the avatar selector dialog is shown
  var showAvatarSelector by mutableStateOf(false)
    private set

  // Whether the banner selector dialog is shown
  var showBannerSelector by mutableStateOf(false)
    private set

  // Whether uploading an image
  var isUploadingImage by mutableStateOf(false)
    private set

  // Temporary fields for editing (before saving)
  var editName by mutableStateOf("")
    private set

  var editBio by mutableStateOf("")
    private set

  var editLocation by mutableStateOf("")
    private set

  var editHobbies by mutableStateOf("")
    private set

  var selectedAvatar by mutableStateOf("")
    private set

  var selectedBanner by mutableStateOf("")
    private set

  // Add a counter to force recomposition
  private var _avatarUpdateCounter by mutableStateOf(0)
  private var _bannerUpdateCounter by mutableStateOf(0)

  // Visibility toggles
  var hobbiesVisible by mutableStateOf(true)
    private set

  // Validation error messages
  var nameError by mutableStateOf<String?>(null)
    private set

  var bioError by mutableStateOf<String?>(null)
    private set

  var locationError by mutableStateOf<String?>(null)
    private set

  var hobbiesError by mutableStateOf<String?>(null)
    private set

  init {
    loadUserProfile()
  }

  /** Load user profile from Firestore. If profile doesn't exist, creates a default one. */
  fun loadUserProfile() {
    viewModelScope.launch {
      _isLoading.value = true
      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        // Try to load existing profile from Firestore
        val existingProfile = repository.getUserProfile(currentUser.uid)

        if (existingProfile != null) {
          // Profile exists in Firestore, use it
          _userProfile.value = existingProfile
        } else {
          // No profile in Firestore, create default one
          val defaultProfile =
              repository.createDefaultProfile(
                  userId = currentUser.uid,
                  name = currentUser.displayName ?: "Anonymous User",
                  profilePictureUrl = currentUser.photoUrl?.toString())
          _userProfile.value = defaultProfile
        }
      }
      _isLoading.value = false
    }
  }

  /** Enable edit mode and populate edit fields with current values */
  fun startEditing() {
    isEditMode = true
    val profile = _userProfile.value
    editName = profile.name

    // Clear default placeholder values when editing
    editBio = if (profile.bio == "Tell us about yourself...") "" else profile.bio
    editLocation = if (profile.location == "Unknown") "" else profile.location

    editHobbies = profile.hobbies.joinToString(", ")
    selectedAvatar = profile.avatarUrl ?: ""
    selectedBanner = profile.bannerUrl ?: ""
    hobbiesVisible = profile.hobbiesVisible
    clearErrors()
  }

  /** Cancel editing and discard changes */
  fun cancelEditing() {
    isEditMode = false
    editName = ""
    editBio = ""
    editLocation = ""
    editHobbies = ""
    selectedAvatar = ""
    selectedBanner = ""
    clearErrors()
  }

  /** Save profile changes to Firestore and exit edit mode */
  fun saveProfile() {
    // Validate all fields before saving
    if (!validateAll()) {
      println("ProfileViewModel - Validation failed")
      // Don't exit edit mode if validation fails
      return
    }

    viewModelScope.launch {
      _isLoading.value = true

      val hobbiesList = editHobbies.split(",").map { it.trim() }.filter { it.isNotEmpty() }

      val updatedProfile =
          _userProfile.value.copy(
              name = editName,
              bio = editBio,
              location = editLocation,
              hobbies = hobbiesList,
              avatarUrl = selectedAvatar.ifEmpty { null },
              bannerUrl = selectedBanner.ifEmpty { null },
              hobbiesVisible = hobbiesVisible)

      // Log pour debug
      println("ProfileViewModel - Saving profile: $updatedProfile")
      println("ProfileViewModel - Avatar URL: ${updatedProfile.avatarUrl}")
      println("ProfileViewModel - Banner URL: ${updatedProfile.bannerUrl}")

      // Save to Firestore
      val success = repository.saveUserProfile(updatedProfile)

      println("ProfileViewModel - Save success: $success")

      // Exit edit mode regardless of success/failure
      if (success) {
        // Update local state only if Firestore save succeeded
        _userProfile.value = updatedProfile
        println("ProfileViewModel - Profile updated successfully")
      } else {
        // Handle error
        println("ProfileViewModel - Failed to save profile")
      }

      isEditMode = false
      clearErrors()

      _isLoading.value = false
    }
  }

  /** Update name field during editing */
  fun updateEditName(newName: String) {
    editName = newName
    validateName()
  }

  /** Update bio field during editing */
  fun updateEditBio(newBio: String) {
    editBio = newBio
    validateBio()
  }

  /** Update location field during editing */
  fun updateEditLocation(newLocation: String) {
    editLocation = newLocation
    validateLocation()
  }

  /** Update hobbies field during editing */
  fun updateEditHobbies(newHobbies: String) {
    editHobbies = newHobbies
    validateHobbies()
  }

  /** Validate name field */
  private fun validateName() {
    nameError =
        when {
          editName.isEmpty() -> "Name cannot be empty"
          editName.length < 2 -> "Name must be at least 2 characters"
          editName.length > 50 -> "Name must be less than 50 characters"
          else -> null
        }
  }

  /** Validate bio field */
  private fun validateBio() {
    bioError =
        when {
          editBio.length > 500 -> "Bio must be less than 500 characters"
          else -> null
        }
  }

  /** Validate location field */
  private fun validateLocation() {
    locationError =
        when {
          editLocation.isEmpty() -> "Location cannot be empty"
          editLocation.length > 100 -> "Location must be less than 100 characters"
          else -> null
        }
  }

  /** Validate hobbies field */
  private fun validateHobbies() {
    hobbiesError =
        when {
          editHobbies.length > 200 -> "Hobbies must be less than 200 characters"
          else -> null
        }
  }

  /** Validate all fields */
  private fun validateAll(): Boolean {
    validateName()
    validateBio()
    validateLocation()
    validateHobbies()

    return nameError == null && bioError == null && locationError == null && hobbiesError == null
  }

  /** Clear all error messages */
  private fun clearErrors() {
    nameError = null
    bioError = null
    locationError = null
    hobbiesError = null
  }

  /** Update avatar selection */
  fun updateAvatarSelection(avatarUrl: String) {
    selectedAvatar = avatarUrl
    _avatarUpdateCounter++
  }

  /** Update banner selection */
  fun updateBannerSelection(bannerUrl: String) {
    selectedBanner = bannerUrl
    _bannerUpdateCounter++
  }

  /** Toggle avatar selector dialog */
  fun toggleAvatarSelector() {
    showAvatarSelector = !showAvatarSelector
  }

  /** Toggle banner selector dialog */
  fun toggleBannerSelector() {
    showBannerSelector = !showBannerSelector
  }

  /** Toggle hobbies visibility */
  fun toggleHobbiesVisibility() {
    hobbiesVisible = !hobbiesVisible
  }

  /**
   * Signs out the current user from Firebase Auth and resets the profile state.
   *
   * This method performs the following actions:
   * - Signs out the user from Firebase Authentication.
   * - Resets the user profile to a default, empty state.
   * - Exits edit mode.
   * - Clears any validation or input errors.
   *
   * Call this method when the user chooses to sign out, to ensure all profile-related state in the
   * ViewModel is cleared and ready for a new session.
   */
  fun signOut() {
    FirebaseAuth.getInstance().signOut()
    // Reset the profile state
    _userProfile.value = UserProfile()
    isEditMode = false
    clearErrors()
  }

  /** Upload avatar image from gallery */
  fun uploadAvatarImage(uri: Uri) {
    viewModelScope.launch {
      isUploadingImage = true
      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        val downloadUrl = imageUploadHelper.uploadImage(uri, currentUser.uid, "avatar")
        if (downloadUrl != null) {
          // Force recomposition by reassigning the entire value
          selectedAvatar = ""
          selectedAvatar = downloadUrl
          println("ProfileViewModel - Avatar uploaded: $downloadUrl")
        } else {
          println("ProfileViewModel - Failed to upload avatar")
        }
      }

      isUploadingImage = false
    }
  }

  /** Upload banner image from gallery */
  fun uploadBannerImage(uri: Uri) {
    viewModelScope.launch {
      isUploadingImage = true
      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        val downloadUrl = imageUploadHelper.uploadImage(uri, currentUser.uid, "banner")
        if (downloadUrl != null) {
          // Force recomposition by reassigning the entire value
          selectedBanner = ""
          selectedBanner = downloadUrl
          println("ProfileViewModel - Banner uploaded: $downloadUrl")
        } else {
          println("ProfileViewModel - Failed to upload banner")
        }
      }

      isUploadingImage = false
    }
  }
}

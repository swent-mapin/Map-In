// assisted by AI
package com.swent.mapin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swent.mapin.model.changepassword.ChangePasswordRepository
import com.swent.mapin.model.changepassword.PasswordChangeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Data class representing the UI state for password change */
data class ChangePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val validationErrors: ValidationErrors = ValidationErrors()
)

/** Data class for field-specific validation errors */
data class ValidationErrors(
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null
)

/**
 * ViewModel for managing password change operations.
 *
 * Responsibilities:
 * - Manage password change form state
 * - Validate password inputs
 * - Handle password change through repository
 * - Provide feedback to UI
 */
class ChangePasswordViewModel(private val repository: ChangePasswordRepository) : ViewModel() {

  private val _state = MutableStateFlow(ChangePasswordState())
  val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

  /**
   * Updates the current password field.
   *
   * @param password The new value for current password
   */
  fun updateCurrentPassword(password: String) {
    _state.value =
        _state.value.copy(
            currentPassword = password,
            validationErrors = _state.value.validationErrors.copy(currentPasswordError = null),
            errorMessage = null)
  }

  /**
   * Updates the new password field.
   *
   * @param password The new value for new password
   */
  fun updateNewPassword(password: String) {
    _state.value =
        _state.value.copy(
            newPassword = password,
            validationErrors = _state.value.validationErrors.copy(newPasswordError = null),
            errorMessage = null)
  }

  /**
   * Updates the confirm password field.
   *
   * @param password The new value for confirm password
   */
  fun updateConfirmPassword(password: String) {
    _state.value =
        _state.value.copy(
            confirmPassword = password,
            validationErrors = _state.value.validationErrors.copy(confirmPasswordError = null),
            errorMessage = null)
  }

  /**
   * Validates password inputs according to requirements.
   *
   * @return true if all validations pass, false otherwise
   */
  private fun validateInputs(): Boolean {
    val currentPassword = _state.value.currentPassword
    val newPassword = _state.value.newPassword
    val confirmPassword = _state.value.confirmPassword

    var hasErrors = false
    var validationErrors = ValidationErrors()

    // Validate current password is not empty
    if (currentPassword.isBlank()) {
      validationErrors =
          validationErrors.copy(currentPasswordError = "Current password is required")
      hasErrors = true
    }

    // Validate new password is different from current password
    if (currentPassword.isNotBlank() &&
        newPassword.isNotBlank() &&
        currentPassword == newPassword) {
      validationErrors =
          validationErrors.copy(
              newPasswordError = "New password must be different from current password")
      hasErrors = true
    }

    // Validate new password requirements
    if (newPassword.isBlank()) {
      validationErrors = validationErrors.copy(newPasswordError = "New password is required")
      hasErrors = true
    } else if (newPassword.length < 8) {
      validationErrors =
          validationErrors.copy(newPasswordError = "Password must be at least 8 characters long")
      hasErrors = true
    } else if (!newPassword.any { it.isUpperCase() }) {
      validationErrors =
          validationErrors.copy(
              newPasswordError = "Password must contain at least one uppercase letter")
      hasErrors = true
    } else if (!newPassword.any { it.isLowerCase() }) {
      validationErrors =
          validationErrors.copy(
              newPasswordError = "Password must contain at least one lowercase letter")
      hasErrors = true
    } else if (!newPassword.any { it.isDigit() }) {
      validationErrors =
          validationErrors.copy(newPasswordError = "Password must contain at least one number")
      hasErrors = true
    } else if (!newPassword.any { !it.isLetterOrDigit() }) {
      validationErrors =
          validationErrors.copy(
              newPasswordError = "Password must contain at least one special character")
      hasErrors = true
    }

    // Validate passwords match
    if (confirmPassword.isBlank()) {
      validationErrors =
          validationErrors.copy(confirmPasswordError = "Please confirm your password")
      hasErrors = true
    } else if (newPassword != confirmPassword) {
      validationErrors = validationErrors.copy(confirmPasswordError = "Passwords do not match")
      hasErrors = true
    }

    _state.value = _state.value.copy(validationErrors = validationErrors)
    return !hasErrors
  }

  /**
   * Attempts to change the user's password.
   *
   * Validates inputs, calls repository to perform the change, and updates state based on result.
   *
   * @param onSuccess Callback invoked when password change succeeds
   */
  fun changePassword(onSuccess: () -> Unit) {
    // Validate inputs first
    if (!validateInputs()) {
      return
    }

    _state.value =
        _state.value.copy(
            isLoading = true, errorMessage = null, validationErrors = ValidationErrors())

    viewModelScope.launch {
      val result =
          repository.changePassword(
              currentPassword = _state.value.currentPassword,
              newPassword = _state.value.newPassword)

      _state.value =
          when (result) {
            is PasswordChangeResult.Success -> {
              onSuccess()
              _state.value.copy(isLoading = false, errorMessage = null)
            }
            is PasswordChangeResult.InvalidCurrentPassword -> {
              _state.value.copy(
                  isLoading = false,
                  validationErrors =
                      _state.value.validationErrors.copy(
                          currentPasswordError = "Current password is incorrect"))
            }
            is PasswordChangeResult.UserNotFound -> {
              _state.value.copy(
                  isLoading = false,
                  errorMessage = "User account not found. Please sign in again and try again.")
            }
            is PasswordChangeResult.UserNotAuthenticated -> {
              _state.value.copy(
                  isLoading = false,
                  errorMessage =
                      "You must be signed in to change your password. Please sign in and try again.")
            }
            is PasswordChangeResult.Error -> {
              _state.value.copy(isLoading = false, errorMessage = result.message)
            }
          }
    }
  }

  /** Clears the error message from state. */
  fun clearError() {
    _state.value = _state.value.copy(errorMessage = null)
  }

  /** Resets all form fields and state. */
  fun resetState() {
    _state.value = ChangePasswordState()
  }

  /**
   * Checks if the current user is authenticated with email/password.
   *
   * @return true if user uses email/password authentication, false otherwise
   */
  fun isEmailPasswordUser(): Boolean {
    return repository.isEmailPasswordUser()
  }

  /** Factory for creating ChangePasswordViewModel instances. */
  class Factory(private val repository: ChangePasswordRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(ChangePasswordViewModel::class.java)) {
        return ChangePasswordViewModel(repository) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}

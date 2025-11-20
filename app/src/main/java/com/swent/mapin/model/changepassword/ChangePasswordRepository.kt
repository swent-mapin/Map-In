// Assisted by AI
package com.swent.mapin.model.changepassword

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Result type for password change operations.
 *
 * Sealed class representing the outcome of attempting to change a user's password.
 */
sealed class PasswordChangeResult {
  /** Password was successfully changed. */
  object Success : PasswordChangeResult()

  /** Current password provided by the user is incorrect. */
  object InvalidCurrentPassword : PasswordChangeResult()

  /** User must be authenticated to change password. */
  object UserNotAuthenticated : PasswordChangeResult()

  /** User account no longer exists or is disabled. */
  object UserNotFound : PasswordChangeResult()

  /**
   * Generic error occurred during password change.
   *
   * @property message Human-readable error message describing what went wrong.
   */
  data class Error(val message: String) : PasswordChangeResult()
}

/**
 * Repository for managing password change operations with Firebase Auth.
 *
 * Provides methods to:
 * - Change user password with re-authentication
 * - Check authentication provider type
 * - Get current user information
 *
 * @property auth Firebase Authentication instance used for auth operations.
 */
interface ChangePasswordRepository {
  /**
   * Changes the password for the currently authenticated user.
   *
   * This operation requires re-authentication with the current password before allowing the
   * password change. This is a Firebase security requirement for sensitive operations.
   *
   * @param currentPassword The user's current password for re-authentication.
   * @param newPassword The new password to set.
   * @return [PasswordChangeResult] indicating success or specific failure reason.
   */
  suspend fun changePassword(currentPassword: String, newPassword: String): PasswordChangeResult

  /**
   * Checks if the current user is authenticated with email/password provider.
   *
   * Returns true only if the user signed in with email/password authentication. Returns false for
   * OAuth providers (Google, Microsoft, etc.) or if no user is authenticated.
   *
   * @return true if user is authenticated with email/password, false otherwise.
   */
  fun isEmailPasswordUser(): Boolean

  /**
   * Gets the currently authenticated Firebase user.
   *
   * @return [FirebaseUser] if authenticated, null otherwise.
   */
  fun getCurrentUser(): FirebaseUser?
}

/**
 * Firebase implementation of [ChangePasswordRepository].
 *
 * Handles password changes through Firebase Authentication API with proper error handling and
 * re-authentication flow.
 *
 * @property auth Firebase Authentication instance, defaults to [FirebaseAuth.getInstance].
 */
class ChangePasswordRepositoryFirebase(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ChangePasswordRepository {

  companion object {
    /** Provider ID for email/password authentication method. */
    private const val EMAIL_PROVIDER_ID = "password"
  }

  override suspend fun changePassword(
      currentPassword: String,
      newPassword: String
  ): PasswordChangeResult {
    return try {
      // Get current user
      val user = auth.currentUser ?: return PasswordChangeResult.UserNotAuthenticated

      // Get user's email
      val email = user.email ?: return PasswordChangeResult.Error("User email not found")

      // Re-authenticate user with current password (required by Firebase for sensitive operations)
      val credential = EmailAuthProvider.getCredential(email, currentPassword)

      try {
        user.reauthenticate(credential).await()
      } catch (e: FirebaseAuthInvalidCredentialsException) {
        // Current password is wrong
        return PasswordChangeResult.InvalidCurrentPassword
      } catch (e: FirebaseAuthInvalidUserException) {
        // User account no longer exists
        return PasswordChangeResult.UserNotFound
      } catch (e: Exception) {
        // Catch any other unexpected errors during re-authentication
        return PasswordChangeResult.Error(e.message ?: "Re-authentication failed")
      }

      // If re-authentication succeeded, update password
      user.updatePassword(newPassword).await()

      PasswordChangeResult.Success
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      PasswordChangeResult.InvalidCurrentPassword
    } catch (e: FirebaseAuthInvalidUserException) {
      PasswordChangeResult.UserNotFound
    } catch (e: Exception) {
      // Generic error
      PasswordChangeResult.Error(e.message ?: "Failed to change password")
    }
  }

  override fun isEmailPasswordUser(): Boolean {
    val user = auth.currentUser ?: return false

    // Check if user has email/password provider
    return user.providerData.any { it.providerId == EMAIL_PROVIDER_ID }
  }

  override fun getCurrentUser(): FirebaseUser? {
    return auth.currentUser
  }
}

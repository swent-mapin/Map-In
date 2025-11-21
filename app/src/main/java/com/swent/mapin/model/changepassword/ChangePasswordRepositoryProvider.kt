// Assisted by AI
package com.swent.mapin.model.changepassword

import androidx.annotation.VisibleForTesting
import com.google.firebase.auth.FirebaseAuth

/**
 * Singleton provider for ChangePasswordRepository.
 *
 * Ensures a single instance is used throughout the app while allowing test injection for unit
 * testing.
 */
object ChangePasswordRepositoryProvider {
  private var instance: ChangePasswordRepository? = null

  /**
   * Gets the singleton ChangePasswordRepository instance.
   *
   * Creates a new [ChangePasswordRepositoryFirebase] instance if one doesn't exist, otherwise
   * returns the existing instance.
   *
   * @return The singleton [ChangePasswordRepository] instance.
   */
  fun getRepository(): ChangePasswordRepository {
    return instance
        ?: synchronized(this) {
          instance
              ?: ChangePasswordRepositoryFirebase(FirebaseAuth.getInstance()).also { instance = it }
        }
  }

  /**
   * Sets a custom ChangePasswordRepository instance.
   *
   * This method is intended for testing purposes only, allowing tests to inject mock or fake
   * implementations of ChangePasswordRepository.
   *
   * @param repository The ChangePasswordRepository instance to use.
   */
  @VisibleForTesting
  fun setRepository(repository: ChangePasswordRepository) {
    instance = repository
  }

  /**
   * Clears the current ChangePasswordRepository instance.
   *
   * This method is intended for testing purposes only, allowing tests to reset the singleton state
   * between test cases to ensure test isolation.
   */
  @VisibleForTesting
  fun clearInstance() {
    instance = null
  }
}

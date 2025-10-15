package com.swent.mapin.model.memory

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Provider for MemoryRepository instances. Allows switching between different repository
 * implementations (Local, Firestore) for testing and production use.
 */
object MemoryRepositoryProvider {
  private var repository: MemoryRepository? = null

  /**
   * Gets the current MemoryRepository instance. If none is set, creates a Firestore-backed
   * repository by default.
   */
  fun getRepository(): MemoryRepository {
    return repository ?: createFirestoreRepository().also { repository = it }
  }

  /**
   * Sets a custom MemoryRepository implementation. Useful for testing with LocalMemoryRepository or
   * injecting mock repositories.
   *
   * @param repo The repository implementation to use
   */
  fun setRepository(repo: MemoryRepository) {
    repository = repo
  }

  /**
   * Resets the repository to null, forcing a new instance to be created on next getRepository()
   * call.
   */
  fun resetRepository() {
    repository = null
  }

  /** Creates a new Firestore-backed repository instance. */
  private fun createFirestoreRepository(): MemoryRepository {
    return MemoryRepositoryFirestore(FirebaseFirestore.getInstance())
  }

  /** Creates a new local in-memory repository instance for testing. */
  fun createLocalRepository(): MemoryRepository {
    return LocalMemoryRepository()
  }
}

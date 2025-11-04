package com.swent.mapin.model.event

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.event.EventRepositoryProvider.getRepository

/** Provider for [EventRepository] implementations to allow easy swapping between data sources. */
object EventRepositoryProvider {
  private var repository: EventRepository? = null
  private var appContext: Context? = null

  /** Initialize the provider with an Application context (optional). Call from Application.onCreate(). */
  fun init(context: Context) {
    appContext = context.applicationContext
  }

  /** Returns the configured [EventRepository], defaulting to a Firestore-backed implementation. */
  fun getRepository(): EventRepository {
    return repository ?: createFirestoreRepository().also { repository = it }
  }

  /** Sets a custom [EventRepository] (e.g., local or mocked) for use throughout the app. */
  fun setRepository(repo: EventRepository) {
    repository = repo
  }

  /** Clears the cached repository so the next call to [getRepository] recreates it. */
  fun resetRepository() {
    repository = null
  }

  /** Instantiates the Firestore-backed repository implementation. */
  private fun createFirestoreRepository(): EventRepository {
    val firestore = FirebaseFirestore.getInstance()
    val localCache = appContext?.let { EventLocalCache.forContext(it) }
    return EventRepositoryFirestore(firestore, localCache)
  }

  /** Convenience helper to create a local in-memory repository populated with sample data. */
  fun createLocalRepository(): EventRepository = LocalEventRepository()
}

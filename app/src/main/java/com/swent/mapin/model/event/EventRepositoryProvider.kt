package com.swent.mapin.model.event

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.friends.FriendRequestRepository
import com.swent.mapin.model.notification.NotificationService
import com.swent.mapin.model.userprofile.UserProfileRepository
import com.swent.mapin.model.badge.BadgeRepositoryFirestore
import com.swent.mapin.model.event.EventRepositoryProvider.getRepository

/** Provider for [EventRepository] implementations to allow easy swapping between data sources. */
object EventRepositoryProvider {
  private var repository: EventRepository? = null
  private var appContext: Context? = null

  /**
   * Initialize the provider with an Application context (optional). Call from
   * Application.onCreate(). If not called, Firestore repository will be created without local cache
   * support. Should only be called once.
   */
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
    val notificationService = NotificationService()
    val friendRepository = FriendRequestRepository(notificationService = notificationService)
    val userProfileRepository = UserProfileRepository()
    return EventRepositoryFirestore(
        firestore,
        friendRepository,
        notificationService,
        badgeRepository = BadgeRepositoryFirestore(firestore, userProfileRepository),
        userProfileRepository = userProfileRepository)
  }
}

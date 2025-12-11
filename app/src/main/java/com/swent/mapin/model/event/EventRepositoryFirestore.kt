package com.swent.mapin.model.event

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.Location
import com.swent.mapin.model.NotificationService
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.badge.BadgeContext
import com.swent.mapin.model.badge.BadgeRepository
import com.swent.mapin.model.event.FirestoreSchema.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.USERS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.UserFields.JOINED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.OWNED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.SAVED_EVENT_IDS
import com.swent.mapin.ui.filters.Filters
import com.swent.mapin.util.EventUtils.calculateHaversineDistance
import com.swent.mapin.util.TimeUtils
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Calendar

object FirestoreSchema {
  const val EVENTS_COLLECTION_PATH = "events"
  const val USERS_COLLECTION_PATH = "users"

  object UserFields {
    const val SAVED_EVENT_IDS = "savedEventIds"
    const val JOINED_EVENT_IDS = "joinedEventIds"
    const val OWNED_EVENT_IDS = "ownedEventIds"
  }
}

const val FIRESTORE_QUERY_LIMIT: Int = 10
const val FIRESTORE_MAX_ARRAY: Int = 10
const val FRIEND_QUERY_CHUNK_SIZE: Int = 10
const val POPULAR_EVENT_PARTICIPANT_THRESHOLD: Int = 30

/**
 * Enum to specify the source of user events (saved, joined, owned). Defines the field name in the
 * user document where event IDs are stored as arrays.
 */
enum class UserEventSource(val fieldName: String) {
  SAVED("savedEventIds"),
  JOINED("joinedEventIds"),
  OWNED("ownedEventIds")
}

/**
 * Firestore implementation of [EventRepository] with optimized queries.
 *
 * @param db Firestore instance to use.
 * @param friendRequestRepository Repository for friend-related queries.
 * @param notificationService Service for sending push notifications.
 * @param userProfileRepository Repository for accessing user profile data.
 */
class EventRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val friendRequestRepository: FriendRequestRepository,
    private val notificationService: NotificationService,
    private val userProfileRepository: UserProfileRepository,
    private val badgeRepository: BadgeRepository
) : EventRepository {

  /**
   * Generates and returns a new unique identifier for an Event item.
   *
   * @return A unique string identifier.
   */
  override fun getNewUid(): String = db.collection(EVENTS_COLLECTION_PATH).document().id

  /**
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  override suspend fun addEvent(event: Event) {
    require(event.isValidEvent()) { "Failed to add event: Invalid event data" }
    require(event.participantIds.isEmpty()) {
      "Failed to add event: participantIds should be empty"
    }
    try {
      val id = event.uid.ifBlank { getNewUid() }
      val eventToSave = event.copy(uid = id)

      db.runBatch { batch ->
            // Write the event document
            batch.set(db.collection(EVENTS_COLLECTION_PATH).document(id), eventToSave)
            // Update the owner's ownedEventIds
            batch.update(
                db.collection(USERS_COLLECTION_PATH).document(eventToSave.ownerId),
                OWNED_EVENT_IDS,
                FieldValue.arrayUnion(id))
          }
          .await()

      // Increment the user's number of events created
      val userId = Firebase.auth.currentUser?.uid
      val ctx = if (userId == null) {
          BadgeContext()
      } else {
          badgeRepository.getBadgeContext(userId)
      }
      var newCtx = ctx.copy(createdEvents = ctx.createdEvents + 1)
      val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
      if (hour in 5..8) {
          newCtx = newCtx.copy(earlyCreate = newCtx.earlyCreate + 1)
      } else if (hour in 0..2) {
          newCtx = newCtx.copy(lateCreate = newCtx.lateCreate + 1)
      }
      if (userId != null) {
          badgeRepository.saveBadgeContext(userId, newCtx)
          badgeRepository.updateBadgesAfterContextChange(userId)
      }

      // Send notifications to followers of the event creator
      notifyFollowersOfNewEvent(eventToSave)
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to add event (id=${event.uid}): ${e.message}", e)
      throw Exception("Failed to add event: ${e.message}", e)
    }
  }

  /**
   * Sends notifications to all followers of the event creator about the new event.
   *
   * @param event The newly created event.
   */
  private suspend fun notifyFollowersOfNewEvent(event: Event) {
    try {
      val ownerProfile = userProfileRepository.getUserProfile(event.ownerId)
      if (ownerProfile == null) {
        Log.w("EventRepositoryFirestore", "Owner profile not found for event notification")
        return
      }

      val followerIds = ownerProfile.followerIds
      if (followerIds.isEmpty()) {
        Log.w("EventRepositoryFirestore", "No followers to notify for new event")
        return
      }

      val organizerName = ownerProfile.name.ifBlank { "Someone you follow" }

      Log.w(
          "EventRepositoryFirestore",
          "Notifying ${followerIds.size} followers about new event: ${event.title}")

      followerIds.forEach { followerId ->
        try {
          notificationService.sendNewEventFromFollowedUserNotification(
              recipientId = followerId,
              organizerId = event.ownerId,
              organizerName = organizerName,
              eventId = event.uid,
              eventTitle = event.title)
        } catch (e: Exception) {
          Log.e(
              "EventRepositoryFirestore", "Failed to notify follower $followerId: ${e.message}", e)
        }
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to notify followers: ${e.message}", e)
      // Don't throw - event creation succeeded, notification failure is non-critical
    }
  }

  /**
   * Edits an existing event as the owner.
   *
   * Owner cannot:
   * - change participants
   * - change public value
   * - reduce capacity below current number of participants
   */
  override suspend fun editEventAsOwner(eventId: String, newValue: Event) {
    require(newValue.isValidEvent()) { "Failed to edit event: Invalid event data" }
    try {
      db.runTransaction { transaction ->
            val docRef = db.collection(EVENTS_COLLECTION_PATH).document(eventId)
            val snapshot = transaction.get(docRef)

            if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")

            val oldEvent = snapshot.toEvent()!!
            if (oldEvent.ownerId != newValue.ownerId)
                throw IllegalArgumentException("Only the owner can call editEventAsOwner")

            // Owner cannot change participants
            if (newValue.participantIds != oldEvent.participantIds)
                throw IllegalArgumentException("Owner cannot change participants list")

            // Owner cannot change 'public'
            if (newValue.public != oldEvent.public && !newValue.public)
                throw IllegalArgumentException("Owner cannot change from public to private")

            // Capacity cannot go below existing participant count
            val currentParticipants = oldEvent.participantIds.size
            if (newValue.capacity != null && newValue.capacity < currentParticipants)
                throw IllegalArgumentException(
                    "Capacity cannot be lower than current participants ($currentParticipants)")

            // Save validated event
            val eventToSave = newValue.copy(uid = eventId)
            transaction.set(docRef, eventToSave)
          }
          .await()
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed editEventAsOwner(id=$eventId): ${e.message}", e)
      throw Exception("Failed to edit event (id=$eventId) as owner: ${e.message}", e)
    }
  }

  /** Allows a non-owner user to join or leave an event. Users can ONLY change participantIds. */
  override suspend fun editEventAsUser(eventId: String, userId: String, join: Boolean) {
    try {
      db.runTransaction { transaction ->
            val docRef = db.collection(EVENTS_COLLECTION_PATH).document(eventId)
            val snapshot = transaction.get(docRef)

            if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
            val oldEvent = snapshot.toEvent()!!

            val isParticipant = userId in oldEvent.participantIds

            val updatedParticipants =
                if (join) {
                  if (isParticipant) return@runTransaction // no changes
                  if (oldEvent.capacity != null &&
                      oldEvent.participantIds.size >= oldEvent.capacity)
                      throw IllegalStateException("Event is full")
                  oldEvent.participantIds + userId
                } else {
                  if (!isParticipant) return@runTransaction // no changes
                  oldEvent.participantIds - userId
                }

            // Update event
            transaction.update(docRef, "participantIds", updatedParticipants)

            // Update user joinedEventIds
            val userRef = db.collection(USERS_COLLECTION_PATH).document(userId)
            transaction.update(
                userRef,
                JOINED_EVENT_IDS,
                if (join) FieldValue.arrayUnion(eventId) else FieldValue.arrayRemove(eventId))
          }
          .await()

        if (join) {
            val ctx = badgeRepository.getBadgeContext(userId)
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            var newCtx = ctx.copy(joinedEvents = ctx.joinedEvents + 1)
            if (hour in 5..8) {
                newCtx = newCtx.copy(earlyJoin = newCtx.earlyJoin + 1)
            } else if (hour in 0..2) {
                newCtx = newCtx.copy(lateJoin = newCtx.lateJoin + 1)
            }
            badgeRepository.saveBadgeContext(userId, newCtx)
            badgeRepository.updateBadgesAfterContextChange(userId)
        }

    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed editEventAsUser(id=$eventId): ${e.message}", e)
      throw Exception("Failed to edit event (id=$eventId) as user: ${e.message}", e)
    }
  }

  /**
   * Deletes an Event item from the repository.
   *
   * @param eventId The unique identifier of the Event item to delete.
   * @throws Exception if the Event item is not found.
   */
  override suspend fun deleteEvent(eventId: String) {
    try {
      // Read the event to get participantIds and ownerId
      val snapshot = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      if (!snapshot.exists()) throw NoSuchElementException("Event not found (id=$eventId)")
      val event = snapshot.toEvent()!!

      // Chunk participant updates, with the first chunk included in the initial batch if small
      // enough
      val participantChunks = event.participantIds.chunked(450)
      val firstChunk = participantChunks.firstOrNull() ?: emptyList()
      val remainingChunks = participantChunks.drop(1)

      // First batch: Delete event, update owner's ownedEventIds, and handle first chunk of
      // participants
      db.runBatch { batch ->
            // Delete the event
            batch.delete(db.collection(EVENTS_COLLECTION_PATH).document(eventId))

            // Update owner's ownedEventIds
            if (event.ownerId.isNotBlank()) {
              batch.update(
                  db.collection(USERS_COLLECTION_PATH).document(event.ownerId),
                  OWNED_EVENT_IDS,
                  FieldValue.arrayRemove(eventId))
            }

            // Update joinedEventIds for the first chunk of participants
            for (userId in firstChunk) {
              batch.update(
                  db.collection(USERS_COLLECTION_PATH).document(userId),
                  JOINED_EVENT_IDS,
                  FieldValue.arrayRemove(eventId))
            }
          }
          .await()

      // Handle remaining participant chunks in separate batches
      for (chunk in remainingChunks) {
        db.runBatch { batch ->
              for (userId in chunk) {
                batch.update(
                    db.collection(USERS_COLLECTION_PATH).document(userId),
                    JOINED_EVENT_IDS,
                    FieldValue.arrayRemove(eventId))
              }
            }
            .await()
      }

      // Handle savedEventIds separately
      val usersSnapSaved =
          db.collection(USERS_COLLECTION_PATH)
              .whereArrayContains(SAVED_EVENT_IDS, eventId)
              .get()
              .await()

      if (usersSnapSaved.documents.isNotEmpty()) {
        db.runBatch { batch ->
              for (userDoc in usersSnapSaved.documents) {
                batch.update(
                    db.collection(USERS_COLLECTION_PATH).document(userDoc.id),
                    SAVED_EVENT_IDS,
                    FieldValue.arrayRemove(eventId))
              }
            }
            .await()
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to delete event (id=$eventId): ${e.message}", e)
      throw e as? NoSuchElementException
          ?: Exception("Failed to delete event (id=$eventId): ${e.message}", e)
    }
  }

  /**
   * Retrieves a specific Event item by its unique identifier.
   *
   * @param eventId The unique identifier of the Event item to retrieve.
   * @return The Event item with the specified identifier.
   * @throws Exception if the Event item is not found.
   */
  override suspend fun getEvent(eventId: String): Event {
    return try {
      val doc = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
      doc.toEvent() ?: throw NoSuchElementException("Event not found (id=$eventId)")
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch event $eventId", e)
      throw Exception("Failed to fetch event $eventId: ${e.message}", e)
    }
  }

  /**
   * Retrieves Event items based on the specified filters with optimized queries.
   *
   * Strategy:
   * 1. Use Firestore queries for server-side filtering when possible (dates, price, tags/friends)
   * 2. Apply client-side filters for complex operations (geo-distance, popularOnly)
   * 3. Prioritize friendsOnly filter in query to reduce data transfer
   *
   * @param filters The filtering criteria (e.g., tags, date range, location, etc.).
   * @return A list of Event items matching the filters.
   */
  override suspend fun getFilteredEvents(filters: Filters, userId: String): List<Event> {
    return try {
      // Determine query strategy based on filters
      val queryStrategy = determineQueryStrategy(filters)

      val events =
          when (queryStrategy) {
            QueryStrategy.FRIENDS -> getEventsByFriendsQuery(filters, userId)
            QueryStrategy.TAGS -> getEventsByTagsQuery(filters)
            QueryStrategy.BASIC -> getEventsByBasicQuery(filters)
          }

      // Apply client-side filters that can't be done server-side
      applyClientSideFilters(events, filters)
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch filtered events", e)
      throw Exception("Failed to fetch filtered events: ${e.message}", e)
    }
  }

  /** Determines the optimal query strategy based on filter combinations. */
  private enum class QueryStrategy {
    FRIENDS, // Friends filter in query (tags client-side if present)
    TAGS, // Tags in query (friends not active)
    BASIC // Date and price only (all other filters client-side)
  }

  /**
   * Analyzes filters and returns the best query strategy. Priority: friendsOnly > tags Price is
   * automatically added when present (compatible with all strategies).
   */
  private fun determineQueryStrategy(filters: Filters): QueryStrategy {
    return when {
      // Friends filter (most selective)
      filters.friendsOnly -> QueryStrategy.FRIENDS

      // Tags filter (no friends)
      filters.tags.isNotEmpty() -> QueryStrategy.TAGS

      // Basic query (dates + price, everything else client-side)
      else -> QueryStrategy.BASIC
    }
  }

  /**
   * Query events by friends' IDs. Price is added to Firestore query if present. Tags are filtered
   * client-side (Firestore constraint: can't combine whereIn + whereArrayContainsAny).
   */
  private suspend fun getEventsByFriendsQuery(filters: Filters, userId: String): List<Event> =
      coroutineScope {
        val friendsDeferred = async {
          try {
            friendRequestRepository.getFriends(userId).map { it.userProfile.userId }
          } catch (e: Exception) {
            Log.e("EventRepositoryFirestore", "Failed to fetch friends", e)
            emptyList()
          }
        }

        val friendIds = friendsDeferred.await()
        if (friendIds.isEmpty()) return@coroutineScope emptyList()

        // Build query with dates + price (if present)
        val query = buildBaseQuery(filters)

        // Fetch events by friend chunks
        val friendChunks = friendIds.chunked(FRIEND_QUERY_CHUNK_SIZE)
        val allEvents = mutableListOf<Event>()

        for (chunk in friendChunks) {
          val snap = query.whereIn("ownerId", chunk).get().await()
          allEvents += snap.documents.mapNotNull { it.toEvent() }
        }

        // Apply tags filter CLIENT-SIDE (Firestore constraint)
        return@coroutineScope if (filters.tags.isNotEmpty()) {
          allEvents.filter { event -> event.tags.any { tag -> filters.tags.contains(tag) } }
        } else {
          allEvents
        }
      }

  /**
   * Query events by tags. Price is added to Firestore query if present (fully server-side
   * filtering).
   */
  private suspend fun getEventsByTagsQuery(filters: Filters): List<Event> {
    var query = buildBaseQuery(filters)

    // Add tags filter
    val limitedTags = filters.tags.take(FIRESTORE_QUERY_LIMIT).toList()
    query = query.whereArrayContainsAny("tags", limitedTags)

    val snap = query.orderBy("date", Query.Direction.DESCENDING).get().await()
    return snap.documents.mapNotNull { it.toEvent() }
  }

  /**
   * Basic query with dates and optional price. All other filters (tags, friends) applied
   * client-side.
   */
  private suspend fun getEventsByBasicQuery(filters: Filters): List<Event> {
    val query = buildBaseQuery(filters)

    val snap = query.orderBy("date", Query.Direction.DESCENDING).get().await()
    val events = snap.documents.mapNotNull { it.toEvent() }

    return events
  }

  /** Builds the base query with date filters. */
  private fun buildBaseQuery(filters: Filters): Query {
    var query: Query = db.collection(EVENTS_COLLECTION_PATH)

    // Apply startDate filter
    val (startTimestamp, _) = TimeUtils.dayBounds(filters.startDate, zone = ZoneOffset.UTC)
    query = query.whereGreaterThanOrEqualTo("date", startTimestamp)

    // Apply endDate filter if provided
    filters.endDate?.let { endDate ->
      val (_, endOfDayExclusive) = TimeUtils.dayBounds(endDate, zone = ZoneOffset.UTC)
      query = query.whereLessThan("date", endOfDayExclusive)
    }

    // Add price to query if present
    if (filters.maxPrice != null) {
      query = query.whereLessThanOrEqualTo("price", filters.maxPrice.toDouble())
    }

    return query
  }

  /** Applies client-side filters that can't be efficiently done server-side. */
  private fun applyClientSideFilters(events: List<Event>, filters: Filters): List<Event> {
    var filtered = events

    // Apply popularOnly filter (requires counting participants)
    if (filters.popularOnly) {
      filtered = filtered.filter { it.participantIds.size > POPULAR_EVENT_PARTICIPANT_THRESHOLD }
    }

    // Apply location/radius filter (requires haversine distance calculation)
    if (filters.place != null) {
      val placeGeoPoint = parsePlaceToGeoPoint(filters.place) ?: return emptyList()
      filtered =
          filtered.filter { event ->
            val eventGeoPoint = GeoPoint(event.location.latitude, event.location.longitude)
            val distance = calculateHaversineDistance(eventGeoPoint, placeGeoPoint)
            distance <= filters.radiusKm
          }
    }

    return filtered.sortedByDescending { it.date }
  }

  /**
   * Get the saved events for a user, sorted by event date.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items saved by the user.
   */
  override suspend fun getSavedEvents(userId: String): List<Event> {
    return getUserEvents(userId, UserEventSource.SAVED)
  }

  /**
   * Get the joined events for a user, sorted by event date.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items the user has joined.
   */
  override suspend fun getJoinedEvents(userId: String): List<Event> {
    return getUserEvents(userId, UserEventSource.JOINED)
  }

  /**
   * Get the owned events for a user, sorted by event date.
   *
   * @param userId The unique identifier of the user.
   * @return A list of Event items owned by the user.
   */
  override suspend fun getOwnedEvents(userId: String): List<Event> {
    return getUserEvents(userId, UserEventSource.OWNED)
  }

  /**
   * Listen to changes in the events collection with filters.
   *
   * @param filters The filtering criteria.
   * @param onUpdate Callback with added, modified, and removed events.
   * @return ListenerRegistration to manage the listener.
   */
  override fun listenToFilteredEvents(
      filters: Filters,
      onUpdate: (added: List<Event>, modified: List<Event>, removed: List<Event>) -> Unit
  ): ListenerRegistration {
    var query = buildBaseQuery(filters)

    // Add tags filter if available and not using friendsOnly
    if (filters.tags.isNotEmpty() && !filters.friendsOnly) {
      val limitedTags = filters.tags.take(FIRESTORE_QUERY_LIMIT).toList()
      query = query.whereArrayContainsAny("tags", limitedTags)
    }

    return query.orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error
      ->
      if (error != null || snapshot == null) {
        Log.e("EventRepositoryFirestore", "Filtered events listener error", error)
        onUpdate(emptyList(), emptyList(), emptyList())
        return@addSnapshotListener
      }

      val addedEvents = mutableListOf<Event>()
      val modifiedEvents = mutableListOf<Event>()
      val removedEvents = mutableListOf<Event>()

      for (change in snapshot.documentChanges) {
        val event = change.document.toEvent() ?: continue
        when (change.type) {
          DocumentChange.Type.ADDED -> addedEvents.add(event)
          DocumentChange.Type.MODIFIED -> modifiedEvents.add(event)
          DocumentChange.Type.REMOVED -> removedEvents.add(event)
        }
      }

      // Apply client-side filters
      val filteredAdded = applyClientSideFilters(addedEvents, filters)
      val filteredModified = applyClientSideFilters(modifiedEvents, filters)
      val filteredRemoved = applyClientSideFilters(removedEvents, filters)

      onUpdate(filteredAdded, filteredModified, filteredRemoved)
    }
  }

  /**
   * Listen to changes in saved events for a user. Monitors the user's savedEventIds array and
   * fetches corresponding events.
   *
   * @param userId The unique identifier of the user.
   * @param onUpdate Callback with added, modified, and removed events.
   * @return ListenerRegistration to manage the listener.
   */
  override fun listenToSavedEvents(
      userId: String,
      onUpdate: (added: List<Event>, modified: List<Event>, removed: List<String>) -> Unit
  ): ListenerRegistration {
    return listenToUserEvents(userId, UserEventSource.SAVED, onUpdate)
  }

  /**
   * Listen to changes in saved events for a user. Monitors the user's savedEventIds array and
   * fetches corresponding events.
   *
   * @param userId The unique identifier of the user.
   * @param onUpdate Callback with added, modified, and removed events.
   * @return ListenerRegistration to manage the listener.
   */
  override fun listenToOwnedEvents(
      userId: String,
      onUpdate: (added: List<Event>, modified: List<Event>, removed: List<String>) -> Unit
  ): ListenerRegistration {
    return listenToUserEvents(userId, UserEventSource.OWNED, onUpdate)
  }

  /**
   * Listen to changes in joined events for a user. Monitors the user's joinedEventIds array and
   * fetches corresponding events.
   *
   * @param userId The unique identifier of the user.
   * @param onUpdate Callback with added, modified, and removed events.
   * @return ListenerRegistration to manage the listener.
   */
  override fun listenToJoinedEvents(
      userId: String,
      onUpdate: (added: List<Event>, modified: List<Event>, removed: List<String>) -> Unit
  ): ListenerRegistration {
    return listenToUserEvents(userId, UserEventSource.JOINED, onUpdate)
  }

  /**
   * Generic method to listen to changes in user events (saved, joined, owned) in real-time.
   *
   * Note: This creates multiple listeners (one for the User document + one per Event document).
   *
   * @param userId The user ID.
   * @param source The source type (SAVED, JOINED, OWNED) field containing the list of IDs.
   * @param onUpdate Callback to dispatch real-time changes: newly added, modified, or removed
   *   events/IDs.
   * @return ListenerRegistration composite object to unregister all attached listeners (user
   *   listener and all event listeners).
   */
  private fun listenToUserEvents(
      userId: String,
      source: UserEventSource,
      onUpdate: (added: List<Event>, modified: List<Event>, removed: List<String>) -> Unit
  ): ListenerRegistration {
    // Composite listener to manage multiple sub-listeners
    val eventListeners = mutableMapOf<String, ListenerRegistration>()
    var previousEventIds = emptySet<String>()

    // Main listener on user document to know which events to monitor
    val userListener =
        db.collection(USERS_COLLECTION_PATH).document(userId).addSnapshotListener { snapshot, error
          ->
          if (error != null) {
            Log.e(
                "EventRepository",
                "Error listening to user $userId ${source.fieldName}: ${error.message}",
                error)
            onUpdate(emptyList(), emptyList(), emptyList())
            return@addSnapshotListener
          }

          if (snapshot == null || !snapshot.exists()) {
            eventListeners.values.forEach { it.remove() }
            eventListeners.clear()
            onUpdate(emptyList(), emptyList(), previousEventIds.toList())
            previousEventIds = emptySet() // Necessary here, do not remove
            return@addSnapshotListener
          }

          // Get the current event IDs from the user document
          val currentEventIds =
              try {
                @Suppress("UNCHECKED_CAST")
                (snapshot.get(source.fieldName) as? List<*>)?.filterIsInstance<String>()?.toSet()
                    ?: emptySet()
              } catch (e: Exception) {
                Log.e("EventRepository", "Error parsing event IDs", e)
                emptySet()
              }

          // Determine added and removed events
          val addedIds = currentEventIds - previousEventIds
          val removedIds = previousEventIds - currentEventIds

          // Remove listeners from outdated events
          if (removedIds.isNotEmpty()) {
            removedIds.forEach { eventId -> eventListeners.remove(eventId)?.remove() }
            onUpdate(emptyList(), emptyList(), removedIds.toList())
          }

          // Add listeners for new events
          addedIds.forEach { eventId -> setupEventListener(eventId, eventListeners, onUpdate) }

          // Necessary here: previousEventIds value is transferred from one call of the listener to
          // the next one
          previousEventIds = currentEventIds
        }

    return ListenerRegistration {
      userListener.remove()
      eventListeners.values.forEach { it.remove() }
      eventListeners.clear()
    }
  }

  /**
   * Helper function to set up and manage a dedicated SnapshotListener for a single Event document.
   * This listener handles both the initial data fetch (treated as 'added') and subsequent
   * modifications ('modified'). It also notifies the system if the underlying document is deleted.
   *
   * @param eventId The ID of the specific event document to listen to.
   * @param eventListeners A mutable map used to store the ListenerRegistration for tracking and
   *   clean-up.
   * @param onUpdate The main callback used to dispatch the event (as added or modified).
   */
  private fun setupEventListener(
      eventId: String,
      eventListeners: MutableMap<String, ListenerRegistration>,
      onUpdate: (added: List<Event>, modified: List<Event>, removed: List<String>) -> Unit
  ) {
    // Prevent duplicate listeners
    if (eventListeners.containsKey(eventId)) return

    var isFirstLoad = true

    val listener =
        db.collection(EVENTS_COLLECTION_PATH).document(eventId).addSnapshotListener {
            eventSnapshot,
            eventError ->
          if (eventError != null) {
            Log.e(
                "EventRepository",
                "Error listening to event $eventId: ${eventError.message}",
                eventError)
            onUpdate(emptyList(), emptyList(), emptyList())
            return@addSnapshotListener
          }

          if (eventSnapshot == null || !eventSnapshot.exists()) {
            onUpdate(emptyList(), emptyList(), listOf(eventId))
            return@addSnapshotListener
          }

          val event = eventSnapshot.toEvent() ?: return@addSnapshotListener

          if (isFirstLoad) {
            // First load: 'added'
            isFirstLoad = false // Necessary here, do not remove
            onUpdate(listOf(event), emptyList(), emptyList())
          } else {
            // Any other load: 'modified'
            onUpdate(emptyList(), listOf(event), emptyList())
          }
        }

    eventListeners[eventId] = listener
  }

  /**
   * Generic method to retrieve event IDs from a user's array field.
   *
   * @param userId The user ID.
   * @param source The source type (SAVED, JOINED, OWNED).
   * @return A set of event IDs.
   */
  private suspend fun getUserEventIds(userId: String, source: UserEventSource): List<String> {
    return try {
      val snap = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
      if (!snap.exists()) {
        Log.w("EventRepositoryFirestore", "User $userId not found")
        throw Exception("User $userId not found")
      } else {
        @Suppress("UNCHECKED_CAST")
        (snap.get(source.fieldName) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
      }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch event IDs for $source", e)
      throw e
    }
  }

  /**
   * Generic method to retrieve full events for a user.
   *
   * @param userId The user ID.
   * @param source The source type (SAVED, JOINED, OWNED).
   * @return A list of events, sorted by date.
   */
  private suspend fun getUserEvents(userId: String, source: UserEventSource): List<Event> {
    val ids = getUserEventIds(userId, source)
    return fetchEventsByIds(ids)
  }

  /**
   * Fetch events by IDs, handling Firestore query limits with parallel execution.
   *
   * @param ids List of event IDs.
   * @return List of events, sorted by date.
   */
  private suspend fun fetchEventsByIds(ids: List<String>): List<Event> = coroutineScope {
    if (ids.isEmpty()) return@coroutineScope emptyList()

    try {
      val chunks = ids.chunked(FIRESTORE_MAX_ARRAY)

      // Execute all chunks in parallel for better performance
      val deferredResults =
          chunks.map { chunk ->
            async {
              try {
                val snap =
                    db.collection(EVENTS_COLLECTION_PATH)
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                snap.documents.mapNotNull { it.toEvent() }
              } catch (e: Exception) {
                Log.e("EventRepositoryFirestore", "Failed to fetch chunk: ${e.message}")
                emptyList()
              }
            }
          }

      // Collect all results and sort
      deferredResults.flatMap { it.await() }.sortedByDescending { it.date }
    } catch (e: Exception) {
      Log.e("EventRepositoryFirestore", "Failed to fetch events by IDs", e)
      emptyList()
    }
  }

  /**
   * Convert DocumentSnapshot to Event.
   *
   * @return The Event object or null if conversion fails.
   */
  private fun DocumentSnapshot.toEvent(): Event? =
      try {
        this.toObject(Event::class.java)?.copy(uid = this.id)
      } catch (e: Exception) {
        Log.e("EventRepositoryFirestore", "Error converting document to Event (id=${this.id})", e)
        throw e
      }

  /**
   * Parse Location to GeoPoint.
   *
   * @param place The location.
   * @return GeoPoint or null if invalid.
   */
  private fun parsePlaceToGeoPoint(place: Location?): GeoPoint? {
    return try {
      if (place == null) null else GeoPoint(place.latitude, place.longitude)
    } catch (e: Exception) {
      Log.w("EventRepositoryFirestore", "Invalid location coordinates: $place", e)
      null
    }
  }
}

package com.swent.mapin.model

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.badge.BadgeRepository
import com.swent.mapin.model.badge.BadgeRepositoryFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing friend requests and friendships in Firestore.
 *
 * Handles all operations related to friend requests including sending, accepting, rejecting, and
 * removing friendships. Supports bidirectional relationship queries.
 *
 * @property firestore Firestore instance for database operations.
 * @property userProfileRepository Repository for fetching user profile information.
 * @property notificationService Service for sending push notifications.
 */
class FriendRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(firestore),
    private val badgeRepository: BadgeRepository = BadgeRepositoryFirestore(firestore),
    private val notificationService: NotificationService
) {
  companion object {
    private const val COLLECTION = "friendRequests"
    private const val TAG = "FriendRequestRepository"
  }

  /**
   * Sends a friend request from one user to another.
   *
   * @param fromUserId User ID of the sender.
   * @param toUserId User ID of the recipient.
   * @return True if the request was sent successfully, false if it already exists or on error.
   */
  suspend fun sendFriendRequest(fromUserId: String, toUserId: String): Boolean {
    return try {
      val existingRequest = getExistingRequest(fromUserId, toUserId)

      // If there's an existing request
      if (existingRequest != null) {
        // If it's rejected, update it back to PENDING (allow re-requesting)
        if (existingRequest.status == FriendshipStatus.REJECTED) {
          firestore
              .collection(COLLECTION)
              .document(existingRequest.requestId)
              .update(
                  mapOf(
                      "status" to FriendshipStatus.PENDING.name,
                      "timestamp" to com.google.firebase.Timestamp.now()))
              .await()

          // Send notification for re-requested friend request
          sendFriendRequestNotificationSafely(fromUserId, toUserId, existingRequest.requestId)
          return true
        }
        // If it's already pending or accepted, don't create a new one
        return false
      }

      // No existing request, create a new one
      val id = firestore.collection(COLLECTION).document().id
      firestore
          .collection(COLLECTION)
          .document(id)
          .set(FriendRequest(id, fromUserId, toUserId, FriendshipStatus.PENDING))
          .await()

      // Send notification to the recipient
      sendFriendRequestNotificationSafely(fromUserId, toUserId, id)

      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to send friend request", e)
      false
    }
  }

  /**
   * Accepts a friend request by updating its status to ACCEPTED.
   *
   * When a friend request is accepted, both users automatically follow each other (bidirectional
   * follow). This ensures friends are always mutually following.
   *
   * @param requestId The ID of the request to accept.
   * @return True if the request was accepted successfully, false on error.
   */
  suspend fun acceptFriendRequest(requestId: String): Boolean {
    return try {
      // Get the request details before updating
      val requestDoc = firestore.collection(COLLECTION).document(requestId).get().await()
      val request = requestDoc.toObject(FriendRequest::class.java)

      if (request == null) {
        Log.e(TAG, "Friend request $requestId not found")
        return false
      }

      // Update status to ACCEPTED
      val success = updateStatus(requestId, FriendshipStatus.ACCEPTED)

      if (success) {
        // When becoming friends, establish bidirectional follow
        // fromUserId follows toUserId and toUserId follows fromUserId
        userProfileRepository.followUser(request.fromUserId, request.toUserId)
        userProfileRepository.followUser(request.toUserId, request.fromUserId)

        // Send notification to the original sender that their request was accepted
        sendFriendRequestAcceptedNotificationSafely(request.toUserId, request.fromUserId)
        val ctxSender = badgeRepository.getBadgeContext(request.fromUserId)
        val updatedSender = ctxSender.copy(friendsCount = ctxSender.friendsCount + 1)
        badgeRepository.saveBadgeContext(request.fromUserId, updatedSender)

        val ctxReceiver = badgeRepository.getBadgeContext(request.toUserId)
        val updatedReceiver = ctxReceiver.copy(friendsCount = ctxReceiver.friendsCount + 1)
        badgeRepository.saveBadgeContext(request.toUserId, updatedReceiver)

        badgeRepository.updateBadgesAfterContextChange(request.fromUserId)
        badgeRepository.updateBadgesAfterContextChange(request.toUserId)
      }

      success
    } catch (e: Exception) {
      Log.e(TAG, "Failed to accept friend request", e)
      false
    }
  }

  /**
   * Rejects a friend request by updating its status to REJECTED.
   *
   * @param requestId The ID of the request to reject.
   * @return True if the request was rejected successfully, false on error.
   */
  suspend fun rejectFriendRequest(requestId: String): Boolean =
      updateStatus(requestId, FriendshipStatus.REJECTED)

  /**
   * Removes a friendship by deleting the accepted request from Firestore. The operation is
   * bidirectional - checks both directions of the friendship.
   *
   * @param userId User ID of the first user.
   * @param friendId User ID of the second user.
   * @return True if the friendship was removed successfully, false if not found or on error.
   */
  suspend fun removeFriendship(userId: String, friendId: String): Boolean {
    return try {
      getExistingRequest(userId, friendId)?.let {
        firestore.collection(COLLECTION).document(it.requestId).delete().await()
        true
      } ?: false
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Retrieves all friends for a user (ACCEPTED requests only). Queries both sent and received
   * requests to support bidirectional friendships.
   *
   * @param userId The user ID to get friends for.
   * @return List of friends with their profile information.
   */
  suspend fun getFriends(userId: String): List<FriendWithProfile> {
    return try {
      val sent = getByStatus(userId, true, FriendshipStatus.ACCEPTED)
      val received = getByStatus(userId, false, FriendshipStatus.ACCEPTED)
      (sent.mapNotNull { req ->
        userProfileRepository.getUserProfile(req.toUserId)?.let {
          FriendWithProfile(it, FriendshipStatus.ACCEPTED, req.requestId)
        }
      } +
          received.mapNotNull { req ->
            userProfileRepository.getUserProfile(req.fromUserId)?.let {
              FriendWithProfile(it, FriendshipStatus.ACCEPTED, req.requestId)
            }
          })
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Retrieves all pending friend requests received by a user.
   *
   * @param userId The user ID to get pending requests for.
   * @return List of pending requests with sender profile information.
   */
  suspend fun getPendingRequests(userId: String): List<FriendWithProfile> {
    return try {
      getByStatus(userId, false, FriendshipStatus.PENDING).mapNotNull { req ->
        userProfileRepository.getUserProfile(req.fromUserId)?.let {
          FriendWithProfile(it, FriendshipStatus.PENDING, req.requestId)
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Searches for users by name with status information about pending requests. Excludes the current
   * user and users with existing relationships (friends or blocked).
   *
   * @param query Search query (filters by name, case-insensitive).
   * @param userId Current user ID (excluded from results).
   * @return List of search results with pending request status information.
   */
  suspend fun searchUsersWithStatus(query: String, userId: String): List<SearchResultWithStatus> {
    return try {
      if (query.isBlank()) return emptyList()
      val users =
          firestore
              .collection("users")
              .get()
              .await()
              .documents
              .mapNotNull { it.toObject(UserProfile::class.java) }
              .filter { it.name.contains(query, true) && it.userId != userId }
      val pending = getByStatus(userId, true, FriendshipStatus.PENDING).map { it.toUserId }.toSet()
      val excluded =
          (getByStatus(userId, true, FriendshipStatus.ACCEPTED) +
                  getByStatus(userId, false, FriendshipStatus.ACCEPTED) +
                  getByStatus(userId, true, FriendshipStatus.BLOCKED) +
                  getByStatus(userId, false, FriendshipStatus.BLOCKED))
              .map { if (it.fromUserId == userId) it.toUserId else it.fromUserId }
              .toSet()
      users
          .filter { it.userId !in excluded }
          .map { SearchResultWithStatus(it, it.userId in pending) }
    } catch (_: Exception) {
      emptyList()
    }
  }

  /** Returns the friendship status between two users if a request exists, null otherwise. */
  suspend fun getFriendshipStatus(user1: String, user2: String): FriendshipStatus? {
    return getExistingRequest(user1, user2)?.status
  }

  /**
   * Updates the status of a friend request.
   *
   * @param id Request ID to update.
   * @param status New status to set.
   * @return True if updated successfully, false on error.
   */
  private suspend fun updateStatus(id: String, status: FriendshipStatus): Boolean {
    return try {
      firestore.collection(COLLECTION).document(id).update("status", status.name).await()
      true
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Retrieves friend requests by status for a user.
   *
   * @param userId User ID to query.
   * @param sent If true, queries sent requests; if false, queries received requests.
   * @param status Status to filter by.
   * @return List of friend requests matching the criteria.
   */
  private suspend fun getByStatus(userId: String, sent: Boolean, status: FriendshipStatus) =
      firestore
          .collection(COLLECTION)
          .whereEqualTo(if (sent) "fromUserId" else "toUserId", userId)
          .whereEqualTo("status", status.name)
          .get()
          .await()
          .mapNotNull { it.toObject(FriendRequest::class.java) }

  /**
   * Finds an existing request between two users (checks both directions).
   *
   * @param user1 First user ID.
   * @param user2 Second user ID.
   * @return The existing request if found, null otherwise.
   */
  private suspend fun getExistingRequest(user1: String, user2: String): FriendRequest? {
    return try {
      val sent =
          firestore
              .collection(COLLECTION)
              .whereEqualTo("fromUserId", user1)
              .whereEqualTo("toUserId", user2)
              .get()
              .await()
      if (!sent.isEmpty) return sent.documents.first().toObject(FriendRequest::class.java)
      val received =
          firestore
              .collection(COLLECTION)
              .whereEqualTo("fromUserId", user2)
              .whereEqualTo("toUserId", user1)
              .get()
              .await()
      if (!received.isEmpty) received.documents.first().toObject(FriendRequest::class.java)
      else null
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Observes friends list in real-time using Firestore listeners. Automatically updates when
   * friendships are added, removed, or modified.
   *
   * @param userId The user ID to observe friends for.
   * @return Flow of friend lists that updates in real-time.
   */
  fun observeFriends(userId: String): Flow<List<FriendWithProfile>> = callbackFlow {
    val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    // Helper function to fetch and send updated data
    suspend fun refreshAndSend() {
      try {
        val friends = getFriends(userId)
        trySend(friends)
      } catch (e: Exception) {
        close(e)
      }
    }

    // Listen to sent friend requests (where user is fromUserId)
    val sentListener =
        firestore
            .collection(COLLECTION)
            .whereEqualTo("fromUserId", userId)
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
            .addSnapshotListener { _, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }
              // Trigger update when sent requests change
              launch { refreshAndSend() }
            }

    // Listen to received friend requests (where user is toUserId)
    val receivedListener =
        firestore
            .collection(COLLECTION)
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
            .addSnapshotListener { _, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }
              // Trigger update when received requests change
              launch { refreshAndSend() }
            }

    listeners.add(sentListener)
    listeners.add(receivedListener)

    // Send initial data
    refreshAndSend()

    awaitClose { listeners.forEach { it.remove() } }
  }

  /**
   * Observes pending friend requests in real-time using Firestore listeners. Automatically updates
   * when requests are sent, accepted, or rejected.
   *
   * @param userId The user ID to observe pending requests for.
   * @return Flow of pending request lists that updates in real-time.
   */
  fun observePendingRequests(userId: String): Flow<List<FriendWithProfile>> = callbackFlow {
    val listener =
        firestore
            .collection(COLLECTION)
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .addSnapshotListener { _, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }

              // Fetch updated data and send
              launch {
                try {
                  val pendingRequests = getPendingRequests(userId)
                  trySend(pendingRequests)
                } catch (e: Exception) {
                  close(e)
                }
              }
            }

    // Send initial data
    launch {
      try {
        val pendingRequests = getPendingRequests(userId)
        trySend(pendingRequests)
      } catch (e: Exception) {
        close(e)
      }
    }

    awaitClose { listener.remove() }
  }

  /**
   * Safely sends a friend request notification. Logs errors but doesn't fail the request.
   *
   * @param fromUserId User ID of the sender.
   * @param toUserId User ID of the recipient.
   * @param requestId ID of the friend request.
   */
  private suspend fun sendFriendRequestNotificationSafely(
      fromUserId: String,
      toUserId: String,
      requestId: String
  ) {
    try {
      Log.i(TAG, "=== NOTIFICATION DEBUG: Starting ===")
      Log.i(TAG, "Sending notification - From: $fromUserId, To: $toUserId, Request: $requestId")

      // Get sender profile to include name in notification
      val senderProfile = userProfileRepository.getUserProfile(fromUserId)
      val senderName = senderProfile?.name ?: "Someone"
      Log.i(TAG, "Sender profile retrieved: name=$senderName")

      Log.i(TAG, "Calling NotificationService.sendFriendRequestNotification()...")
      val result =
          notificationService.sendFriendRequestNotification(
              recipientId = toUserId,
              senderId = fromUserId,
              senderName = senderName,
              requestId = requestId)

      when (result) {
        is NotificationResult.Success -> {
          Log.i(TAG, "✅ Notification created successfully: ${result.notification.notificationId}")
          Log.i(
              TAG,
              "Notification details - Title: ${result.notification.title}, Recipient: ${result.notification.recipientId}")
        }
        is NotificationResult.Error -> {
          Log.e(TAG, "❌ Notification creation failed: ${result.message}", result.exception)
        }
      }

      Log.i(TAG, "Friend request notification process completed for $toUserId")
    } catch (e: Exception) {
      // Don't fail the request if notification fails
      Log.e(TAG, "Failed to send friend request notification", e)
    }
  }

  /**
   * Safely sends a notification when a friend request is accepted. Notifies the original sender
   * that their request was accepted.
   *
   * @param accepterId User ID of the person who accepted the request.
   * @param originalSenderId User ID of the person who originally sent the request.
   */
  private suspend fun sendFriendRequestAcceptedNotificationSafely(
      accepterId: String,
      originalSenderId: String
  ) {
    try {
      Log.i(TAG, "=== ACCEPTANCE NOTIFICATION DEBUG: Starting ===")
      Log.i(TAG, "Accepter: $accepterId, Original Sender: $originalSenderId")

      // Get accepter profile to include name in notification
      val accepterProfile = userProfileRepository.getUserProfile(accepterId)
      val accepterName = accepterProfile?.name ?: "Someone"
      Log.i(TAG, "Accepter profile retrieved: name=$accepterName")

      // Send an info notification about the accepted request
      Log.d(TAG, "Calling NotificationService.sendInfoNotification()...")
      val actionUrl = "mapin://friendAccepted"
      Log.d(TAG, "🔗 ACTION URL BEING SENT: $actionUrl")
      val result =
          notificationService.sendInfoNotification(
              recipientId = originalSenderId,
              title = "Friend Request Accepted",
              message = "$accepterName accepted your friend request",
              metadata = mapOf("userId" to accepterId, "accepterName" to accepterName),
              actionUrl = actionUrl)

      when (result) {
        is NotificationResult.Success -> {
          Log.i(TAG, "✅ Acceptance notification created: ${result.notification.notificationId}")
        }
        is NotificationResult.Error -> {
          Log.e(TAG, "❌ Acceptance notification failed: ${result.message}", result.exception)
        }
      }

      Log.i(TAG, "Friend request accepted notification process completed for $originalSenderId")
    } catch (e: Exception) {
      // Don't fail the acceptance if notification fails
      Log.e(TAG, "Failed to send friend request accepted notification", e)
    }
  }
}

package com.swent.mapin.model

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing friend requests and friendships in Firestore.
 *
 * Handles all operations related to friend requests including sending, accepting, rejecting, and
 * removing friendships. Supports bidirectional relationship queries.
 *
 * @property firestore Firestore instance for database operations.
 * @property userProfileRepository Repository for fetching user profile information.
 */
class FriendRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(firestore)
) {
  companion object {
    private const val COLLECTION = "friendRequests"
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
      true
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Accepts a friend request by updating its status to ACCEPTED.
   *
   * @param requestId The ID of the request to accept.
   * @return True if the request was accepted successfully, false on error.
   */
  suspend fun acceptFriendRequest(requestId: String): Boolean =
      updateStatus(requestId, FriendshipStatus.ACCEPTED)

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
}

package com.swent.mapin.model

import com.google.firebase.Timestamp
import com.swent.mapin.model.friends.FriendRequest
import com.swent.mapin.model.friends.FriendWithProfile
import com.swent.mapin.model.friends.FriendshipStatus
import com.swent.mapin.model.userprofile.UserProfile
import org.junit.Assert.*
import org.junit.Test

// Assisted by AI

/**
 * Unit tests for FriendRequest data models.
 *
 * Tests cover:
 * - FriendshipStatus enum values
 * - FriendRequest data class creation and properties
 * - FriendWithProfile data class creation and properties
 * - Default values
 * - Data class equality and copy methods
 */
class FriendRequestTest {

  // ==================== FriendshipStatus Tests ====================

  @Test
  fun friendshipStatus_hasAllExpectedValues() {
    val values = FriendshipStatus.entries
    assertEquals(4, values.size)
    assertTrue(values.contains(FriendshipStatus.PENDING))
    assertTrue(values.contains(FriendshipStatus.ACCEPTED))
    assertTrue(values.contains(FriendshipStatus.REJECTED))
    assertTrue(values.contains(FriendshipStatus.BLOCKED))
  }

  @Test
  fun friendshipStatus_valueOf_returnsCorrectEnum() {
    assertEquals(FriendshipStatus.PENDING, FriendshipStatus.valueOf("PENDING"))
    assertEquals(FriendshipStatus.ACCEPTED, FriendshipStatus.valueOf("ACCEPTED"))
    assertEquals(FriendshipStatus.REJECTED, FriendshipStatus.valueOf("REJECTED"))
    assertEquals(FriendshipStatus.BLOCKED, FriendshipStatus.valueOf("BLOCKED"))
  }

  @Test
  fun friendshipStatus_ordinal_isCorrect() {
    assertEquals(0, FriendshipStatus.PENDING.ordinal)
    assertEquals(1, FriendshipStatus.ACCEPTED.ordinal)
    assertEquals(2, FriendshipStatus.REJECTED.ordinal)
    assertEquals(3, FriendshipStatus.BLOCKED.ordinal)
  }

  // ==================== FriendRequest Tests ====================

  @Test
  fun friendRequest_defaultConstructor_hasEmptyValues() {
    val request = FriendRequest()

    assertEquals("", request.requestId)
    assertEquals("", request.fromUserId)
    assertEquals("", request.toUserId)
    assertEquals(FriendshipStatus.PENDING, request.status)
    assertNotNull(request.timestamp)
  }

  @Test
  fun friendRequest_withParameters_setsCorrectValues() {
    val timestamp = Timestamp.now()
    val request =
        FriendRequest(
            requestId = "req123",
            fromUserId = "user1",
            toUserId = "user2",
            status = FriendshipStatus.ACCEPTED,
            timestamp = timestamp)

    assertEquals("req123", request.requestId)
    assertEquals("user1", request.fromUserId)
    assertEquals("user2", request.toUserId)
    assertEquals(FriendshipStatus.ACCEPTED, request.status)
    assertEquals(timestamp, request.timestamp)
  }

  @Test
  fun friendRequest_copy_createsNewInstance() {
    val original =
        FriendRequest(
            requestId = "req123",
            fromUserId = "user1",
            toUserId = "user2",
            status = FriendshipStatus.PENDING)

    val copy = original.copy(status = FriendshipStatus.ACCEPTED)

    assertEquals("req123", copy.requestId)
    assertEquals("user1", copy.fromUserId)
    assertEquals("user2", copy.toUserId)
    assertEquals(FriendshipStatus.ACCEPTED, copy.status)
    assertNotEquals(original.status, copy.status)
  }

  @Test
  fun friendRequest_equality_worksCorrectly() {
    val timestamp = Timestamp.now()
    val request1 =
        FriendRequest(
            requestId = "req123",
            fromUserId = "user1",
            toUserId = "user2",
            status = FriendshipStatus.PENDING,
            timestamp = timestamp)

    val request2 =
        FriendRequest(
            requestId = "req123",
            fromUserId = "user1",
            toUserId = "user2",
            status = FriendshipStatus.PENDING,
            timestamp = timestamp)

    assertEquals(request1, request2)
    assertEquals(request1.hashCode(), request2.hashCode())
  }

  @Test
  fun friendRequest_inequality_worksCorrectly() {
    val request1 = FriendRequest(requestId = "req123", fromUserId = "user1", toUserId = "user2")

    val request2 = FriendRequest(requestId = "req456", fromUserId = "user1", toUserId = "user2")

    assertNotEquals(request1, request2)
  }

  @Test
  fun friendRequest_withDifferentStatuses() {
    val pending = FriendRequest(requestId = "1", status = FriendshipStatus.PENDING)
    val accepted = FriendRequest(requestId = "2", status = FriendshipStatus.ACCEPTED)
    val rejected = FriendRequest(requestId = "3", status = FriendshipStatus.REJECTED)
    val blocked = FriendRequest(requestId = "4", status = FriendshipStatus.BLOCKED)

    assertEquals(FriendshipStatus.PENDING, pending.status)
    assertEquals(FriendshipStatus.ACCEPTED, accepted.status)
    assertEquals(FriendshipStatus.REJECTED, rejected.status)
    assertEquals(FriendshipStatus.BLOCKED, blocked.status)
  }

  // ==================== FriendWithProfile Tests ====================

  @Test
  fun friendWithProfile_defaultConstructor_hasDefaultValues() {
    val userProfile = UserProfile(userId = "user1", name = "Alice")
    val friendWithProfile = FriendWithProfile(userProfile = userProfile)

    assertEquals(userProfile, friendWithProfile.userProfile)
    assertEquals(FriendshipStatus.PENDING, friendWithProfile.friendshipStatus)
    assertEquals("", friendWithProfile.requestId)
  }

  @Test
  fun friendWithProfile_withParameters_setsCorrectValues() {
    val userProfile =
        UserProfile(userId = "user1", name = "Alice", bio = "Hello", location = "Paris")

    val friendWithProfile =
        FriendWithProfile(
            userProfile = userProfile,
            friendshipStatus = FriendshipStatus.ACCEPTED,
            requestId = "req123")

    assertEquals(userProfile, friendWithProfile.userProfile)
    assertEquals(FriendshipStatus.ACCEPTED, friendWithProfile.friendshipStatus)
    assertEquals("req123", friendWithProfile.requestId)
    assertEquals("Alice", friendWithProfile.userProfile.name)
    assertEquals("Paris", friendWithProfile.userProfile.location)
  }

  @Test
  fun friendWithProfile_copy_createsNewInstance() {
    val userProfile = UserProfile(userId = "user1", name = "Alice")
    val original =
        FriendWithProfile(
            userProfile = userProfile,
            friendshipStatus = FriendshipStatus.PENDING,
            requestId = "req123")

    val copy = original.copy(friendshipStatus = FriendshipStatus.ACCEPTED)

    assertEquals(userProfile, copy.userProfile)
    assertEquals(FriendshipStatus.ACCEPTED, copy.friendshipStatus)
    assertEquals("req123", copy.requestId)
    assertNotEquals(original.friendshipStatus, copy.friendshipStatus)
  }

  @Test
  fun friendWithProfile_equality_worksCorrectly() {
    val userProfile = UserProfile(userId = "user1", name = "Alice")
    val friend1 =
        FriendWithProfile(
            userProfile = userProfile,
            friendshipStatus = FriendshipStatus.ACCEPTED,
            requestId = "req123")

    val friend2 =
        FriendWithProfile(
            userProfile = userProfile,
            friendshipStatus = FriendshipStatus.ACCEPTED,
            requestId = "req123")

    assertEquals(friend1, friend2)
    assertEquals(friend1.hashCode(), friend2.hashCode())
  }

  @Test
  fun friendWithProfile_inequality_worksCorrectly() {
    val userProfile1 = UserProfile(userId = "user1", name = "Alice")
    val userProfile2 = UserProfile(userId = "user2", name = "Bob")

    val friend1 = FriendWithProfile(userProfile = userProfile1, requestId = "req123")
    val friend2 = FriendWithProfile(userProfile = userProfile2, requestId = "req456")

    assertNotEquals(friend1, friend2)
  }

  @Test
  fun friendWithProfile_withDifferentStatuses() {
    val userProfile = UserProfile(userId = "user1", name = "Alice")

    val pending = FriendWithProfile(userProfile, FriendshipStatus.PENDING, "1")
    val accepted = FriendWithProfile(userProfile, FriendshipStatus.ACCEPTED, "2")
    val rejected = FriendWithProfile(userProfile, FriendshipStatus.REJECTED, "3")
    val blocked = FriendWithProfile(userProfile, FriendshipStatus.BLOCKED, "4")

    assertEquals(FriendshipStatus.PENDING, pending.friendshipStatus)
    assertEquals(FriendshipStatus.ACCEPTED, accepted.friendshipStatus)
    assertEquals(FriendshipStatus.REJECTED, rejected.friendshipStatus)
    assertEquals(FriendshipStatus.BLOCKED, blocked.friendshipStatus)
  }

  @Test
  fun friendWithProfile_userProfileIntegration() {
    val userProfile =
        UserProfile(
            userId = "user1",
            name = "Alice",
            bio = "Developer",
            location = "Paris",
            hobbies = listOf("Reading", "Coding"),
            hobbiesVisible = true)

    val friendWithProfile =
        FriendWithProfile(
            userProfile = userProfile,
            friendshipStatus = FriendshipStatus.ACCEPTED,
            requestId = "req123")

    assertEquals("user1", friendWithProfile.userProfile.userId)
    assertEquals("Alice", friendWithProfile.userProfile.name)
    assertEquals("Developer", friendWithProfile.userProfile.bio)
    assertEquals("Paris", friendWithProfile.userProfile.location)
    assertEquals(2, friendWithProfile.userProfile.hobbies.size)
    assertTrue(friendWithProfile.userProfile.hobbiesVisible)
  }

  @Test
  fun friendWithProfile_toString_containsAllFields() {
    val userProfile = UserProfile(userId = "user1", name = "Alice")
    val friendWithProfile =
        FriendWithProfile(
            userProfile = userProfile,
            friendshipStatus = FriendshipStatus.ACCEPTED,
            requestId = "req123")

    val string = friendWithProfile.toString()
    assertTrue(string.contains("userProfile"))
    assertTrue(string.contains("friendshipStatus"))
    assertTrue(string.contains("requestId"))
  }

  // ==================== Integration Tests ====================

  @Test
  fun friendRequest_canBeUsedWithFriendWithProfile() {
    // Simulate a friend request flow
    val fromUser = UserProfile(userId = "user1", name = "Alice")
    val toUser = UserProfile(userId = "user2", name = "Bob")

    // Create initial friend request
    val request =
        FriendRequest(
            requestId = "req123",
            fromUserId = fromUser.userId,
            toUserId = toUser.userId,
            status = FriendshipStatus.PENDING)

    // Create FriendWithProfile for display
    val friendWithProfile =
        FriendWithProfile(
            userProfile = fromUser,
            friendshipStatus = request.status,
            requestId = request.requestId)

    assertEquals(request.requestId, friendWithProfile.requestId)
    assertEquals(request.status, friendWithProfile.friendshipStatus)
    assertEquals(request.fromUserId, friendWithProfile.userProfile.userId)
  }

  @Test
  fun friendRequest_statusTransitions() {
    val request =
        FriendRequest(
            requestId = "req123",
            fromUserId = "user1",
            toUserId = "user2",
            status = FriendshipStatus.PENDING)

    // Accept the request
    val accepted = request.copy(status = FriendshipStatus.ACCEPTED)
    assertEquals(FriendshipStatus.ACCEPTED, accepted.status)
    assertEquals(request.requestId, accepted.requestId)

    // Alternatively reject
    val rejected = request.copy(status = FriendshipStatus.REJECTED)
    assertEquals(FriendshipStatus.REJECTED, rejected.status)

    // Or block
    val blocked = request.copy(status = FriendshipStatus.BLOCKED)
    assertEquals(FriendshipStatus.BLOCKED, blocked.status)
  }

  @Test
  fun friendWithProfile_emptyRequestId() {
    val userProfile = UserProfile(userId = "user1", name = "Alice")
    val friendWithProfile =
        FriendWithProfile(
            userProfile = userProfile, friendshipStatus = FriendshipStatus.ACCEPTED, requestId = "")

    assertEquals("", friendWithProfile.requestId)
    assertEquals(FriendshipStatus.ACCEPTED, friendWithProfile.friendshipStatus)
  }

  @Test
  fun friendRequest_emptyUserIds() {
    val request =
        FriendRequest(
            requestId = "req123", fromUserId = "", toUserId = "", status = FriendshipStatus.PENDING)

    assertEquals("", request.fromUserId)
    assertEquals("", request.toUserId)
    assertEquals("req123", request.requestId)
  }
}

package com.swent.mapin.model.badge

import com.swent.mapin.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

// Assisted by AI

/** Tests for BadgeManager to ensure badge unlock logic works correctly. */
class BadgeManagerTest {

  @Test
  fun `friendly badge is locked when user has no friends`() {
    val profile = UserProfile(userId = "test_user", name = "Test User")
    val friendsCount = 0

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val friendlyBadge = badges.find { it.id == "first_friend" }

    assertNotNull(friendlyBadge)
    assertFalse(friendlyBadge!!.isUnlocked)
    assertEquals(0f, friendlyBadge.progress, 0.01f)
  }

  @Test
  fun `friendly badge is unlocked when user has one friend`() {
    val profile = UserProfile(userId = "test_user", name = "Test User")
    val friendsCount = 1

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val friendlyBadge = badges.find { it.id == "first_friend" }

    assertNotNull(friendlyBadge)
    assertTrue(friendlyBadge!!.isUnlocked)
    assertEquals(1f, friendlyBadge.progress, 0.01f)
  }

  @Test
  fun `friendly badge is unlocked when user has multiple friends`() {
    val profile = UserProfile(userId = "test_user", name = "Test User")
    val friendsCount = 5

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val friendlyBadge = badges.find { it.id == "first_friend" }

    assertNotNull(friendlyBadge)
    assertTrue(friendlyBadge!!.isUnlocked)
    assertEquals(1f, friendlyBadge.progress, 0.01f)
  }

  @Test
  fun `profile pro badge is locked when profile is incomplete`() {
    val profile =
        UserProfile(
            userId = "test_user",
            name = "Test User",
            bio = "",
            location = "Unknown",
            hobbies = emptyList(),
            avatarUrl = null)
    val friendsCount = 0

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val profileProBadge = badges.find { it.id == "profile_complete" }

    assertNotNull(profileProBadge)
    assertFalse(profileProBadge!!.isUnlocked)
    assertTrue(profileProBadge.progress < 1f)
  }

  @Test
  fun `profile pro badge has partial progress when some fields are filled`() {
    val profile =
        UserProfile(
            userId = "test_user",
            name = "Test User",
            bio = "Hello World",
            location = "Paris",
            hobbies = emptyList(),
            avatarUrl = null)
    val friendsCount = 0

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val profileProBadge = badges.find { it.id == "profile_complete" }

    assertNotNull(profileProBadge)
    assertFalse(profileProBadge!!.isUnlocked)
    assertEquals(0.6f, profileProBadge.progress, 0.01f) // 3 out of 5 fields
  }

  @Test
  fun `profile pro badge is unlocked when all fields are filled`() {
    val profile =
        UserProfile(
            userId = "test_user",
            name = "Test User",
            bio = "Hello World",
            location = "Paris",
            hobbies = listOf("Reading", "Gaming"),
            avatarUrl = "https://example.com/avatar.png")
    val friendsCount = 0

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val profileProBadge = badges.find { it.id == "profile_complete" }

    assertNotNull(profileProBadge)
    assertTrue(profileProBadge!!.isUnlocked)
    assertEquals(1f, profileProBadge.progress, 0.01f)
  }

  @Test
  fun `profile pro badge ignores default placeholder values`() {
    val profile =
        UserProfile(
            userId = "test_user",
            name = "Test User",
            bio = "Tell us about yourself...",
            location = "Unknown",
            hobbies = listOf("Reading"),
            avatarUrl = "https://example.com/avatar.png")
    val friendsCount = 0

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val profileProBadge = badges.find { it.id == "profile_complete" }

    assertNotNull(profileProBadge)
    assertFalse(profileProBadge!!.isUnlocked)
    // Only name, hobbies, and avatar are filled (3/5 = 0.6)
    assertEquals(0.6f, profileProBadge.progress, 0.01f)
  }

  @Test
  fun `profile pro badge works with profilePictureUrl instead of avatarUrl`() {
    val profile =
        UserProfile(
            userId = "test_user",
            name = "Test User",
            bio = "Hello World",
            location = "Paris",
            hobbies = listOf("Reading"),
            avatarUrl = null,
            profilePictureUrl = "https://example.com/profile.png")
    val friendsCount = 0

    val badges = BadgeManager.calculateBadges(profile, friendsCount)
    val profileProBadge = badges.find { it.id == "profile_complete" }

    assertNotNull(profileProBadge)
    assertTrue(profileProBadge!!.isUnlocked)
    assertEquals(1f, profileProBadge.progress, 0.01f)
  }

  @Test
  fun `both badges can be unlocked simultaneously`() {
    val profile =
        UserProfile(
            userId = "test_user",
            name = "Test User",
            bio = "Hello World",
            location = "Paris",
            hobbies = listOf("Reading"),
            avatarUrl = "https://example.com/avatar.png")
    val friendsCount = 1

    val badges = BadgeManager.calculateBadges(profile, friendsCount)

    assertEquals(2, badges.size)
    assertTrue(badges.all { it.isUnlocked })
  }
}

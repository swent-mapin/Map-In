package com.swent.mapin.model.badge

import com.swent.mapin.model.userprofile.UserProfile
import org.junit.Assert.*
import org.junit.Test

class BadgeManagerTest {

  // ==================== Friendly Badge Tests ====================

  @Test
  fun `calculateBadges returns Friendly badge unlocked when user has one friend`() {
    val profile = UserProfile(userId = "user1", name = "Test User")
    val badgeContext = BadgeContext(friendsCount = 1) // updated

    val badges = BadgeManager.calculateBadges(profile, badgeContext)

    val friendlyBadge = badges.find { it.id == "friendly" }
    assertNotNull("Friendly badge should exist", friendlyBadge)
    assertTrue("Friendly badge should be unlocked", friendlyBadge!!.isUnlocked)
    assertEquals("Friendly badge progress should be 100%", 1f, friendlyBadge.progress, 0.001f)
    assertEquals("Friendly badge title", "Friendly", friendlyBadge.title)
    assertEquals("Friendly badge rarity", BadgeRarity.COMMON, friendlyBadge.rarity)
  }

  @Test
  fun `calculateBadges returns Friendly badge locked when user has no friends`() {
    val profile = UserProfile(userId = "user1", name = "Test User")
    val badgeContext = BadgeContext(friendsCount = 0) // updated

    val badges = BadgeManager.calculateBadges(profile, badgeContext)

    val friendlyBadge = badges.find { it.id == "friendly" }
    assertNotNull("Friendly badge should exist", friendlyBadge)
    assertFalse("Friendly badge should be locked", friendlyBadge!!.isUnlocked)
    assertEquals("Friendly badge progress should be 0%", 0f, friendlyBadge.progress, 0.001f)
  }

  // ==================== Profile Pro Badge Tests ====================

  @Test
  fun `calculateBadges returns Profile Pro badge unlocked when all fields completed`() {
    val profile =
        UserProfile(
            userId = "user1",
            name = "Test User",
            bio = "I love coding",
            location = "Paris",
            hobbies = listOf("Reading", "Gaming"),
            avatarUrl = "https://example.com/avatar.jpg")
    val badgeContext = BadgeContext() // friendsCount not relevant here

    val badges = BadgeManager.calculateBadges(profile, badgeContext)

    val profileProBadge = badges.find { it.id == "profile_pro" }
    assertNotNull("Profile Pro badge should exist", profileProBadge)
    assertTrue("Profile Pro badge should be unlocked", profileProBadge!!.isUnlocked)
    assertEquals("Profile Pro badge progress should be 100%", 1f, profileProBadge.progress, 0.001f)
  }

  @Test
  fun `calculateBadges returns Profile Pro badge with partial progress`() {
    val profile =
        UserProfile(
            userId = "user1",
            name = "Test User",
            bio = "I love coding",
            location = "Paris",
            hobbies = emptyList(),
            avatarUrl = null)
    val badgeContext = BadgeContext()

    val badges = BadgeManager.calculateBadges(profile, badgeContext)

    val profileProBadge = badges.find { it.id == "profile_pro" }
    assertFalse("Profile Pro badge should be locked", profileProBadge!!.isUnlocked)
    assertEquals("Profile Pro badge progress should be 60%", 0.6f, profileProBadge.progress, 0.001f)
  }

  @Test
  fun `calculateBadges ignores default placeholder values`() {
    val profile =
        UserProfile(
            userId = "user1",
            name = "Test User",
            bio = "Tell us about yourself...",
            location = "Unknown",
            hobbies = listOf("Reading"),
            avatarUrl = "person")
    val badgeContext = BadgeContext()

    val badges = BadgeManager.calculateBadges(profile, badgeContext)

    val profileProBadge = badges.find { it.id == "profile_pro" }
    assertFalse(
        "Profile Pro badge should be locked with default values", profileProBadge!!.isUnlocked)
    assertEquals(
        "Progress should be 40% (only name and hobbies counted)",
        0.4f,
        profileProBadge.progress,
        0.001f)
  }

  @Test
  fun `calculateBadges accepts profilePictureUrl as valid avatar`() {
    val profile =
        UserProfile(
            userId = "user1",
            name = "Test User",
            bio = "I love coding",
            location = "Paris",
            hobbies = listOf("Reading"),
            avatarUrl = null,
            profilePictureUrl = "https://example.com/profile.jpg")
    val badgeContext = BadgeContext()

    val badges = BadgeManager.calculateBadges(profile, badgeContext)

    val profileProBadge = badges.find { it.id == "profile_pro" }
    assertTrue(
        "Profile Pro badge should be unlocked with profilePictureUrl", profileProBadge!!.isUnlocked)
    assertEquals("Profile Pro badge progress should be 100%", 1f, profileProBadge.progress, 0.001f)
  }
}

package com.swent.mapin.model.badge

import androidx.compose.ui.graphics.vector.ImageVector
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class BadgeTest {

  @Test
  fun `defaults and equality`() {
    val icon = mockk<ImageVector>()
    val b1 =
        Badge(
            id = "badge1",
            title = "Title",
            description = "Desc",
            icon = icon,
            rarity = BadgeRarity.RARE)

    // defaults
    assertFalse(b1.isUnlocked)
    assertEquals(0f, b1.progress, 0.0f)

    // copy and equality
    val b2 = b1.copy()
    assertEquals(b1, b2)
    assertEquals(b1.hashCode(), b2.hashCode())
  }

  @Test
  fun `copy modifies fields`() {
    val icon = mockk<ImageVector>()
    val original =
        Badge(
            id = "id",
            title = "Title",
            description = "Desc",
            icon = icon,
            rarity = BadgeRarity.EPIC,
            isUnlocked = true,
            progress = 1f)

    val modified = original.copy(isUnlocked = false, progress = 0.3f, title = "NewTitle")

    assertFalse(modified.isUnlocked)
    assertEquals(0.3f, modified.progress, 0.0f)
    assertEquals("NewTitle", modified.title)
    // unchanged fields
    assertEquals(original.id, modified.id)
    assertEquals(original.description, modified.description)
  }

  @Test
  fun `toString contains fields`() {
    val icon = mockk<ImageVector>()
    val b =
        Badge(
            id = "idToStr",
            title = "MyTitle",
            description = "MyDesc",
            icon = icon,
            rarity = BadgeRarity.COMMON)
    val s = b.toString()

    assertTrue(s.contains("idToStr"))
    assertTrue(s.contains("MyTitle"))
    assertTrue(s.contains("MyDesc"))
  }

  @Test
  fun `badge rarity values and names`() {
    val values = BadgeRarity.entries
    assertEquals(4, values.size)
    val expected = listOf("COMMON", "RARE", "EPIC", "LEGENDARY")
    assertEquals(expected, values.map { it.name })
  }

  @Test
  fun `progress boundaries`() {
    val icon = mockk<ImageVector>()
    val zero =
        Badge(
            id = "z",
            title = "t",
            description = "d",
            icon = icon,
            rarity = BadgeRarity.COMMON,
            progress = 0f)
    val one =
        Badge(
            id = "o",
            title = "t",
            description = "d",
            icon = icon,
            rarity = BadgeRarity.COMMON,
            progress = 1f)

    assertEquals(0f, zero.progress, 0.0f)
    assertEquals(1f, one.progress, 0.0f)
  }

  @Test
  fun `getSampleBadges returns expected sample badges`() {
    val badges = SampleBadges.getSampleBadges()

    // basic expectations
    assertEquals(3, badges.size)

    val first = badges.find { it.id == "first_event" }
    assertNotNull(first)
    assertEquals("First Step", first!!.title)
    assertEquals(1f, first.progress, 0.0f)
    assertTrue(first.isUnlocked)

    val friend = badges.find { it.id == "first_friend" }
    assertNotNull(friend)
    assertEquals("Friendly", friend!!.title)
    assertEquals(0.5f, friend.progress, 0.0f)
    assertFalse(friend.isUnlocked)

    val profile = badges.find { it.id == "profile_complete" }
    assertNotNull(profile)
    assertEquals("Profile Pro", profile!!.title)
    assertEquals(0.2f, profile.progress, 0.0f)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `progress below zero throws exception`() {
    val icon = mockk<ImageVector>()
    Badge(
        id = "invalid",
        title = "Invalid",
        description = "Should fail",
        icon = icon,
        rarity = BadgeRarity.COMMON,
        progress = -0.1f)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `progress above one throws exception`() {
    val icon = mockk<ImageVector>()
    Badge(
        id = "invalid",
        title = "Invalid",
        description = "Should fail",
        icon = icon,
        rarity = BadgeRarity.COMMON,
        progress = 1.1f)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `progress far below zero throws exception`() {
    val icon = mockk<ImageVector>()
    Badge(
        id = "invalid",
        title = "Invalid",
        description = "Should fail",
        icon = icon,
        rarity = BadgeRarity.COMMON,
        progress = -100f)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `progress far above one throws exception`() {
    val icon = mockk<ImageVector>()
    Badge(
        id = "invalid",
        title = "Invalid",
        description = "Should fail",
        icon = icon,
        rarity = BadgeRarity.COMMON,
        progress = 100f)
  }

  @Test
  fun `progress exactly at boundaries is valid`() {
    val icon = mockk<ImageVector>()

    // 0f should be valid
    val atZero =
        Badge(
            id = "zero",
            title = "Zero",
            description = "At zero",
            icon = icon,
            rarity = BadgeRarity.COMMON,
            progress = 0f)
    assertEquals(0f, atZero.progress, 0.0f)

    // 1f should be valid
    val atOne =
        Badge(
            id = "one",
            title = "One",
            description = "At one",
            icon = icon,
            rarity = BadgeRarity.COMMON,
            progress = 1f)
    assertEquals(1f, atOne.progress, 0.0f)
  }
}

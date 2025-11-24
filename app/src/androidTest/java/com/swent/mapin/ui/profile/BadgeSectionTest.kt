package com.swent.mapin.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.badge.Badge
import com.swent.mapin.model.badge.BadgeRarity
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests for BadgeSection composables.
 *
 * Tests cover:
 * - BadgesSection rendering with different badge states
 * - BadgeItem display for locked and unlocked badges
 * - BadgeDetailDialog interactions and content
 * - BadgeRarityLegend display
 * - Badge click interactions
 * - Progress indicators
 * - Rarity color schemes
 */
class BadgeSectionTest {

  @get:Rule val composeTestRule = createComposeRule()

  // ==================== Test Data ====================

  private fun createUnlockedBadge(
      id: String = "test_badge",
      title: String = "Test Badge",
      rarity: BadgeRarity = BadgeRarity.COMMON
  ): Badge {
    return Badge(
        id = id,
        title = title,
        description = "Test description",
        icon = Icons.Default.Star,
        rarity = rarity,
        isUnlocked = true,
        progress = 1f)
  }

  private fun createLockedBadge(
      id: String = "locked_badge",
      title: String = "Locked Badge",
      progress: Float = 0f,
      rarity: BadgeRarity = BadgeRarity.COMMON
  ): Badge {
    return Badge(
        id = id,
        title = title,
        description = "Locked description",
        icon = Icons.Default.EmojiEvents,
        rarity = rarity,
        isUnlocked = false,
        progress = progress)
  }

  // ==================== BadgesSection Tests ====================

  @Test
  fun badgesSection_displaysCorrectly() {
    val badges = listOf(createUnlockedBadge(), createLockedBadge())

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = badges) } }

    composeTestRule.onNodeWithTag("badgesSection").assertIsDisplayed()
    composeTestRule.onNodeWithText("Achievements").assertIsDisplayed()
  }

  @Test
  fun badgesSection_displaysBadgeCount() {
    val badges =
        listOf(
            createUnlockedBadge(id = "badge1"),
            createUnlockedBadge(id = "badge2"),
            createLockedBadge(id = "badge3"),
            createLockedBadge(id = "badge4"))

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = badges) } }

    composeTestRule.onNodeWithTag("badgeCount").assertIsDisplayed()
    composeTestRule.onNodeWithText("2/4").assertIsDisplayed()
  }

  @Test
  fun badgesSection_displaysAllBadges() {
    val badges =
        listOf(
            createUnlockedBadge(id = "badge1", title = "First Badge"),
            createUnlockedBadge(id = "badge2", title = "Second Badge"),
            createLockedBadge(id = "badge3", title = "Third Badge"))

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = badges) } }

    composeTestRule.onNodeWithTag("badgeItem_badge1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("badgeItem_badge2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("badgeItem_badge3").assertIsDisplayed()

    composeTestRule.onNodeWithText("First Badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second Badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("Third Badge").assertIsDisplayed()
  }

  @Test
  fun badgesSection_displaysEmptyList() {
    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = emptyList()) } }

    composeTestRule.onNodeWithTag("badgesSection").assertIsDisplayed()
    composeTestRule.onNodeWithText("0/0").assertIsDisplayed()
  }

  @Test
  fun badgesSection_displaysRarityLegend() {
    val badges = listOf(createUnlockedBadge())

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = badges) } }

    composeTestRule.onNodeWithText("Rarity Levels").assertIsDisplayed()
    composeTestRule.onNodeWithText("Common").assertIsDisplayed()
    composeTestRule.onNodeWithText("Rare").assertIsDisplayed()
    composeTestRule.onNodeWithText("Epic").assertIsDisplayed()
    composeTestRule.onNodeWithText("Legendary").assertIsDisplayed()
  }

  // ==================== BadgeDetailDialog Tests ====================

  @Test
  fun badgeDetailDialog_displaysProgressForLockedBadge() {
    val badge = createLockedBadge(progress = 0.75f)

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = listOf(badge)) } }

    composeTestRule.onNodeWithTag("badgeItem_locked_badge").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Progress").assertIsDisplayed()
    composeTestRule.onNodeWithText("75%").assertIsDisplayed()
  }

  @Test
  fun badgeDetailDialog_closesOnButtonClick() {
    val badge = createUnlockedBadge()

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = listOf(badge)) } }

    composeTestRule.onNodeWithTag("badgeItem_test_badge").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Close").assertIsDisplayed()
    composeTestRule.onNodeWithText("Close").performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed
    composeTestRule.onNodeWithText("Close").assertDoesNotExist()
  }

  // ==================== Multiple Badges Tests ====================

  @Test
  fun badgesSection_displaysMultipleBadgesWithDifferentRarities() {
    val badges =
        listOf(
            createUnlockedBadge(id = "common", title = "Common Badge", rarity = BadgeRarity.COMMON),
            createUnlockedBadge(id = "rare", title = "Rare Badge", rarity = BadgeRarity.RARE),
            createUnlockedBadge(id = "epic", title = "Epic Badge", rarity = BadgeRarity.EPIC),
            createUnlockedBadge(
                id = "legendary", title = "Legendary Badge", rarity = BadgeRarity.LEGENDARY))

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = badges) } }

    composeTestRule.onNodeWithText("Common Badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("Rare Badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("Epic Badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("Legendary Badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("4/4").assertIsDisplayed()
  }

  @Test
  fun badgesSection_displaysMultipleBadgesWithDifferentProgress() {
    val badges =
        listOf(
            createLockedBadge(id = "progress0", title = "No Progress", progress = 0f),
            createLockedBadge(id = "progress25", title = "Quarter Way", progress = 0.25f),
            createLockedBadge(id = "progress50", title = "Halfway", progress = 0.5f),
            createLockedBadge(id = "progress75", title = "Almost There", progress = 0.75f))

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = badges) } }

    // Verify all badges are displayed
    composeTestRule.onNodeWithText("No Progress").assertIsDisplayed()
    composeTestRule.onNodeWithText("Quarter Way").assertIsDisplayed()
    composeTestRule.onNodeWithText("Halfway").assertIsDisplayed()
    composeTestRule.onNodeWithText("Almost There").assertIsDisplayed()

    // Check one badge's progress in detail dialog
    composeTestRule.onNodeWithTag("badgeItem_progress50").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("50%").assertIsDisplayed()
  }

  @Test
  fun badgesSection_handlesMixedLockedAndUnlockedBadges() {
    val badges =
        listOf(
            createUnlockedBadge(id = "unlocked1", title = "Unlocked 1"),
            createLockedBadge(id = "locked1", title = "Locked 1", progress = 0.3f),
            createUnlockedBadge(id = "unlocked2", title = "Unlocked 2"),
            createLockedBadge(id = "locked2", title = "Locked 2", progress = 0.8f))

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = badges) } }

    composeTestRule.onNodeWithText("2/4").assertIsDisplayed()
    composeTestRule.onNodeWithText("Unlocked 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Locked 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Unlocked 2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Locked 2").assertIsDisplayed()
  }

  // ==================== Edge Cases ====================

  @Test
  fun badgesSection_handlesZeroProgress() {
    val badge = createLockedBadge(progress = 0f)

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = listOf(badge)) } }

    composeTestRule.onNodeWithTag("badgeItem_locked_badge").performClick()
    composeTestRule.waitForIdle()

    // Should show locked without progress bar
    composeTestRule.onNodeWithText("🔒 Locked").assertIsDisplayed()
    composeTestRule.onNodeWithText("Progress").assertDoesNotExist()
  }

  @Test
  fun badgesSection_handlesFullProgress() {
    val badge = createLockedBadge(progress = 1f)

    composeTestRule.setContent { MaterialTheme { BadgesSection(badges = listOf(badge)) } }

    composeTestRule.onNodeWithTag("badgeItem_locked_badge").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("100%").assertIsDisplayed()
  }
}

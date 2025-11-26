package com.swent.mapin.model.badge

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a user achievement badge.
 *
 * @property id Unique identifier for the badge
 * @property title The name of the badge
 * @property description What the user needs to achieve to earn this badge
 * @property icon The icon representing the badge
 * @property rarity The rarity level of the badge
 * @property isUnlocked Whether the user has unlocked this badge
 * @property progress Current progress toward unlocking (0.0 to 1.0)
 */
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val rarity: BadgeRarity,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f // 0.0 to 1.0
) {
  init {
    require(progress in 0f..1f) { "Progress must be between 0.0 and 1.0, but was $progress" }
  }
}

/**
 * Badge rarity levels.
 *
 * Each rarity has a different color scheme and difficulty level.
 */
enum class BadgeRarity {
  COMMON, // Easy to obtain
  RARE, // Requires some effort
  EPIC, // Challenging to achieve
  LEGENDARY // Very difficult to obtain
}

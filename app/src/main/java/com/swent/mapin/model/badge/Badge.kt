package com.swent.mapin.model.badge

/**
 * Represents a user achievement badge.
 *
 * @property id Unique identifier for the badge
 * @property title The name of the badge
 * @property description What the user needs to achieve to earn this badge
 * @property iconName The name/identifier of the icon (stored as String for Firebase compatibility)
 * @property rarity The rarity level of the badge
 * @property isUnlocked Whether the user has unlocked this badge
 * @property progress Current progress toward unlocking (0.0 to 1.0)
 */
data class Badge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "", // String for Firebase compatibility instead of ImageVector
    val rarity: BadgeRarity = BadgeRarity.COMMON,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f // 0.0 to 1.0
)

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

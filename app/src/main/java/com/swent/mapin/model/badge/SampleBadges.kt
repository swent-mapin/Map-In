package com.swent.mapin.model.badge

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star

/** Provides a very small set of sample badges for development and testing. */
object SampleBadges {

  fun getSampleBadges(): List<Badge> {
    return listOf(
        Badge(
            id = "first_event",
            title = "First Step",
            description = "Join your first event",
            icon = Icons.Default.Star,
            rarity = BadgeRarity.COMMON,
            isUnlocked = true,
            progress = 1f),
        Badge(
            id = "first_friend",
            title = "Friendly",
            description = "Add your first friend",
            icon = Icons.Default.Face,
            rarity = BadgeRarity.COMMON,
            isUnlocked = false,
            progress = 0.5f),
        Badge(
            id = "profile_complete",
            title = "Profile Pro",
            description = "Complete your profile",
            icon = Icons.Default.Person,
            rarity = BadgeRarity.RARE,
            isUnlocked = false,
            progress = 0.2f))
  }
}

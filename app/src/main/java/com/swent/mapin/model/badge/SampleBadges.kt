package com.swent.mapin.model.badge

/** Provides a very small set of sample badges for development and testing. */
object SampleBadges {

  fun getSampleBadges(): List<Badge> {
    return listOf(
        Badge(
            id = "first_event",
            title = "First Step",
            description = "Join your first event",
            iconName = "star",
            rarity = BadgeRarity.COMMON,
            isUnlocked = true,
            progress = 1f),
        Badge(
            id = "first_friend",
            title = "Friendly",
            description = "Add your first friend",
            iconName = "face",
            rarity = BadgeRarity.COMMON,
            isUnlocked = false,
            progress = 0.5f),
        Badge(
            id = "profile_complete",
            title = "Profile Pro",
            description = "Complete your profile",
            iconName = "person",
            rarity = BadgeRarity.RARE,
            isUnlocked = false,
            progress = 0.2f))
  }
}

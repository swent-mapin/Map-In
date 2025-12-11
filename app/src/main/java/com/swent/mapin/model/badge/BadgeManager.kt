package com.swent.mapin.model.badge

import com.swent.mapin.model.UserProfile

/**
 * Singleton object responsible for calculating badge unlock status and progress.
 *
 * This manager handles all badge-related calculations in a stateless, thread-safe manner. Each
 * badge type has its own calculation logic based on user profile data and social metrics.
 *
 * Usage:
 * ```
 * val badges = BadgeManager.calculateBadges(userProfile, friendsCount)
 * ```
 */
object BadgeManager {

  /**
   * Calculate all badges for a user based on their profile and social metrics.
   *
   * This method recalculates all badge statuses and progress values from scratch. It is thread-safe
   * and can be called concurrently from multiple coroutines.
   *
   * @param userProfile The user's profile containing all profile data
   * @param data The BadgeContext which contains all related metadata
   * @return List of all badges with updated unlock status and progress
   */
  fun calculateBadges(userProfile: UserProfile, data: BadgeContext): List<Badge> {
    return listOf(
        calculateFriendlyBadge(data.friendsCount),
        calculateSocialButNotQuiteBadge(data.friendsCount),
        calculateSocialButterflyBadge(data.friendsCount),
        calculateLinkedOutBadge(data.friendsCount),
        calculateProfileProBadge(userProfile),
        calculateEventStarterBadge(data.createdEvents),
        calculateEventOrganizerBadge(data.createdEvents),
        calculateEventMasterBadge(data.createdEvents),
        calculateProbablyTooTiredBadge(data.createdEvents),
        calculateFirstStepBadge(data.joinedEvents),
        calculateEventEnjoyerBadge(data.joinedEvents),
        calculateEventEnthusiastBadge(data.joinedEvents),
        calculateBusyBadge(data.joinedEvents),
        calculateNightOwlBadge(data.lateJoin),
        calculateEarlyBirdBadge(data.earlyJoin),
        calculateProcrastinationBadge(data.lateCreate),
        calculateMorningPersonBadge(data.earlyCreate))
  }

  /**
   * Calculate the "Friendly" badge (COMMON rarity).
   *
   * Unlock criteria: User has at least 1 friend Progress: Binary (0% or 100%)
   *
   * @param friendsCount Number of friends the user has
   * @return Badge with unlock status and progress
   */
  private fun calculateFriendlyBadge(friendsCount: Int): Badge {
    val isUnlocked = friendsCount >= 1
    val progress = if (isUnlocked) 1f else 0f

    return Badge(
        id = "friendly",
        title = "Friendly",
        description = "Make your first friend",
        iconName = "face",
        rarity = BadgeRarity.COMMON,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateSocialButNotQuiteBadge(friendsCount: Int): Badge {
    val progress = (friendsCount / 10f).coerceAtMost(1f)
    val isUnlocked = friendsCount >= 10
    return Badge(
        id = "social_but_not_quite",
        title = "Social, but not quite",
        description = "Make 10 friends",
        iconName = "group-add",
        rarity = BadgeRarity.RARE,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateSocialButterflyBadge(friendsCount: Int): Badge {
    val progress = (friendsCount / 25f).coerceAtMost(1f)
    val isUnlocked = friendsCount >= 25
    return Badge(
        id = "social_butterfly",
        title = "Social Butterfly",
        description = "Make 25 friends",
        iconName = "diversity-3",
        rarity = BadgeRarity.EPIC,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateLinkedOutBadge(friendsCount: Int): Badge {
    val progress = (friendsCount / 50f).coerceAtMost(1f)
    val isUnlocked = friendsCount >= 50
    return Badge(
        id = "linked_out",
        title = "Linked...Out?",
        description = "Reach 50 friends",
        iconName = "handshake",
        rarity = BadgeRarity.LEGENDARY,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  /**
   * Calculate the "Profile Pro" badge (RARE rarity).
   *
   * Unlock criteria: All 5 profile fields must be completed:
   * 1. Name is not empty
   * 2. Bio is filled (not default placeholder)
   * 3. Location is filled (not "Unknown")
   * 4. At least one hobby added
   * 5. Profile picture exists (avatarUrl or profilePictureUrl)
   *
   * Progress: 20% per completed field (0.0 to 1.0)
   *
   * @param userProfile The user's profile data
   * @return Badge with unlock status and progress
   */
  private fun calculateProfileProBadge(userProfile: UserProfile): Badge {
    var completedFields = 0

    // Check 1: Name is not empty
    if (userProfile.name.isNotEmpty()) {
      completedFields++
    }

    // Check 2: Bio is filled (not default placeholder)
    if (userProfile.bio.isNotEmpty() && userProfile.bio != "Tell us about yourself...") {
      completedFields++
    }

    // Check 3: Location is filled (not "Unknown")
    if (userProfile.location.isNotEmpty() && userProfile.location != "Unknown") {
      completedFields++
    }

    // Check 4: At least one hobby added
    if (userProfile.hobbies.isNotEmpty()) {
      completedFields++
    }

    // Check 5: Profile picture exists (check both avatarUrl and profilePictureUrl)
    // Accept if either avatarUrl is valid (not null, not empty, not default "person")
    // OR profilePictureUrl is valid (not null, not empty)
    val hasValidAvatarUrl =
        !userProfile.avatarUrl.isNullOrEmpty() && userProfile.avatarUrl != "person"
    val hasValidProfilePictureUrl = !userProfile.profilePictureUrl.isNullOrEmpty()
    val hasProfilePicture = hasValidAvatarUrl || hasValidProfilePictureUrl

    if (hasProfilePicture) {
      completedFields++
    }

    // Calculate progress (20% per field = 0.2f per field)
    val progress = completedFields / 5f
    val isUnlocked = completedFields == 5

    return Badge(
        id = "profile_pro",
        title = "Profile Pro",
        description = "Complete all profile fields",
        iconName = "person",
        rarity = BadgeRarity.RARE,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateEventStarterBadge(createdEvents: Int): Badge {
    val isUnlocked = createdEvents >= 1
    val progress = if (isUnlocked) 1f else 0f

    return Badge(
        id = "event_starter",
        title = "Event Starter",
        description = "Create your first event",
        iconName = "calendar-plus",
        rarity = BadgeRarity.COMMON,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateEventOrganizerBadge(createdEvents: Int): Badge {
    val progress = (createdEvents / 10f).coerceAtMost(1f)
    val isUnlocked = createdEvents >= 10
    return Badge(
        id = "event_organizer",
        title = "Event Organizer",
        description = "Create 10 events",
        iconName = "calendar-check",
        rarity = BadgeRarity.RARE,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateEventMasterBadge(createdEvents: Int): Badge {
    val progress = (createdEvents / 25f).coerceAtMost(1f)
    val isUnlocked = createdEvents >= 25
    return Badge(
        id = "event_master",
        title = "Event Master",
        description = "Create 25 events",
        iconName = "workspace-premium",
        rarity = BadgeRarity.EPIC,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateProbablyTooTiredBadge(createdEvents: Int): Badge {
    val progress = (createdEvents / 50f).coerceAtMost(1f)
    val isUnlocked = createdEvents >= 50
    return Badge(
        id = "probably_too_tired",
        title = "Probably too tired",
        description = "Create 50 events",
        iconName = "military-tech",
        rarity = BadgeRarity.LEGENDARY,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateFirstStepBadge(joinedEvents: Int): Badge {
    val isUnlocked = joinedEvents >= 1
    val progress = if (isUnlocked) 1f else 0f
    return Badge(
        id = "first_step",
        title = "Event Enjoyer",
        description = "Join your first event",
        iconName = "thumb-up",
        rarity = BadgeRarity.COMMON,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateEventEnjoyerBadge(joinedEvents: Int): Badge {
    val progress = (joinedEvents / 10f).coerceAtMost(1f)
    val isUnlocked = joinedEvents >= 10
    return Badge(
        id = "event_enjoyer",
        title = "Event Enjoyer",
        description = "Join 10 events",
        iconName = "star",
        rarity = BadgeRarity.RARE,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateEventEnthusiastBadge(joinedEvents: Int): Badge {
    val progress = (joinedEvents / 25f).coerceAtMost(1f)
    val isUnlocked = joinedEvents >= 25
    return Badge(
        id = "event_enthusiast",
        title = "Event Enthusiast",
        description = "Join 25 events",
        iconName = "calendar-star",
        rarity = BadgeRarity.EPIC,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateBusyBadge(joinedEvents: Int): Badge {
    val progress = (joinedEvents / 50f).coerceAtMost(1f)
    val isUnlocked = joinedEvents >= 50
    return Badge(
        id = "\"i'm_busy\"",
        title = "\"I'm Busy\"",
        description = "Join 50 events",
        iconName = "trophy",
        rarity = BadgeRarity.LEGENDARY,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateNightOwlBadge(lateJoin: Int): Badge {
    val isUnlocked = lateJoin >= 1
    val progress = if (isUnlocked) 1f else 0f
    return Badge(
        id = "night_owl",
        title = "Night Owl",
        description = "Another sleepless night? Go to sleep",
        iconName = "dark-mode",
        rarity = BadgeRarity.LEGENDARY,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateEarlyBirdBadge(earlyJoin: Int): Badge {
    val isUnlocked = earlyJoin >= 1
    val progress = if (isUnlocked) 1f else 0f
    return Badge(
        id = "early_bird",
        title = "Early Bird",
        description = "Well someone is up early!",
        iconName = "brightness-5",
        rarity = BadgeRarity.LEGENDARY,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateProcrastinationBadge(lateCreate: Int): Badge {
    val isUnlocked = lateCreate >= 1
    val progress = if (isUnlocked) 1f else 0f
    return Badge(
        id = "procrastination",
        title = "Procrastination",
        description = "Shouldn't you have planned this out earlier?",
        iconName = "hourglass-bottom",
        rarity = BadgeRarity.LEGENDARY,
        isUnlocked = isUnlocked,
        progress = progress)
  }

  private fun calculateMorningPersonBadge(earlyCreate: Int): Badge {
    val isUnlocked = earlyCreate >= 1
    val progress = if (isUnlocked) 1f else 0f
    return Badge(
        id = "morning_person",
        title = "Morning Person",
        description = "Someone's got their life in check!",
        iconName = "wp-sunny",
        rarity = BadgeRarity.LEGENDARY,
        isUnlocked = isUnlocked,
        progress = progress)
  }
}

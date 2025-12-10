package com.swent.mapin.ui.filters

/**
 * Centralized list of all available event tags in the application.
 *
 * This class provides a single source of truth for event categorization tags used throughout the
 * app for filtering and organizing events.
 */
class Tags {
  companion object {
    /**
     * Set of all available event tags.
     *
     * These tags can be used to categorize events and allow users to filter events based on their
     * interests. The tags cover various event types including entertainment, sports, cultural
     * activities, and social events.
     */
    val allTags =
        setOf(
            "Music",
            "Party",
            "Sport",
            "Food",
            "Nature",
            "Art",
            "Tech",
            "Dance",
            "Cinema",
            "Festival",
            "Workshop",
            "Club",
            "Volunteering",
            "Travel",
            "Fitness",
            "Board Games")
  }
}

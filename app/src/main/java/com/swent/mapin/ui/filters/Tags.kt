package com.swent.mapin.ui.filters

/**
 * Centralized and exhaustive list of event tags.
 *
 * This is the single source of truth for all event categorization tags. Any tag used for event
 * filtering or categorization must be defined here. To add a new tag category, update [allTags] and
 * ensure consistency across:
 * - Event creation forms (tag selection)
 * - Filter UI components
 * - Backend event schema validation
 * - Search and recommendation algorithms
 */
class Tags {
  companion object {
    /**
     * Complete set of available event tags.
     *
     * Tags must match exactly (case-sensitive) when assigned to events or used in filters.
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

package com.swent.mapin.ui.filters

import com.swent.mapin.model.Location
import java.time.LocalDate

/**
 * Data class representing event filtering criteria.
 *
 * This class encapsulates all possible filters that users can apply when searching for events. All
 * filters are optional and can be combined.
 *
 * @property startDate The earliest date to include in results (defaults to today)
 * @property endDate The latest date to include in results (null means no end date limit)
 * @property place Geographic location to search around (null means no location filter)
 * @property radiusKm Search radius in kilometers around the place (default 10km)
 * @property maxPrice Maximum event price to include (null means no price limit)
 * @property tags Set of event tags to filter by (empty means all tags)
 * @property friendsOnly Whether to show only events where friends are participating
 * @property popularOnly Whether to show only popular events (based on participant count)
 */
data class Filters(
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val place: Location? = null,
    val radiusKm: Int = 10,
    val maxPrice: Int? = null,
    val tags: Set<String> = emptySet(),
    val friendsOnly: Boolean = false,
    val popularOnly: Boolean = false
) {
  /**
   * Checks if all filters are at their default values.
   *
   * This is useful for determining whether to apply filtering logic or simply show all events.
   *
   * @return true if no filters are active, false otherwise
   */
  fun isEmpty(): Boolean =
      startDate == LocalDate.now() &&
          endDate == null &&
          place == null &&
          radiusKm == 10 &&
          maxPrice == null &&
          tags.isEmpty() &&
          !friendsOnly &&
          !popularOnly
}

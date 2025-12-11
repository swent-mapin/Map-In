package com.swent.mapin.ui.filters

import com.swent.mapin.model.location.Location
import java.time.LocalDate

/**
 * Data class representing event filtering criteria.
 *
 * This class encapsulates all possible filters that users can apply when searching for events. All
 * filters are optional and can be combined using AND logic - an event must satisfy ALL active
 * filters to be included in results.
 *
 * **Filter Combination Behavior:**
 * - Multiple filters narrow down results (AND operation, not OR)
 * - Example: `friendsOnly=true` AND `tags=["Music"]` shows only Music events with friends
 * - `popularOnly=true` AND `maxPrice=20` shows only popular events under 20 currency units
 * - Geographic filters (`place`, `radiusKm`) are independent and combined with other criteria
 * - Date range filters are inclusive: events must fall within [startDate, endDate]
 *
 * @property startDate The earliest date to include in results (defaults to today)
 * @property endDate The latest date to include in results (null means no end date limit)
 * @property place Geographic location to search around (null means no location filter)
 * @property radiusKm Search radius in kilometers around the place (default 10km, ignored if place
 *   is null)
 * @property maxPrice Maximum event price to include (null means no price limit)
 * @property tags Set of event tags to filter by (empty means all tags, multiple tags require event
 *   to match ANY tag - OR logic within tags)
 * @property friendsOnly Whether to show only events where friends are participating
 * @property popularOnly Whether to show only popular events (based on participant count)
 */
data class Filters(
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val place: Location = Location.UNDEFINED,
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
          place == Location.UNDEFINED &&
          radiusKm == 10 &&
          maxPrice == null &&
          tags.isEmpty() &&
          !friendsOnly &&
          !popularOnly
}

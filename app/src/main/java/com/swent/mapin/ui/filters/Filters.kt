package com.swent.mapin.ui.filters

import com.swent.mapin.model.location.Location
import java.time.LocalDate

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
  // Helper to check if filters are effectively "empty" (i.e., defaults or no effect)
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

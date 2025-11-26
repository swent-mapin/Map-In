//Assisted by AI
package com.swent.mapin.ui.map.directions

import java.util.Locale

/**
 * Data class containing route information including distance and duration.
 *
 * @property distance Distance in meters
 * @property duration Duration in seconds
 */
data class RouteInfo(
    val distance: Double,
    val duration: Double
) {
  companion object {
    val ZERO = RouteInfo(distance = 0.0, duration = 0.0)
  }

  /**
   * Formats distance for display.
   * Returns "X km" for distances >= 1000m, "X m" otherwise.
   */
  fun formatDistance(): String {
    return if (distance >= 1000) {
      String.format(Locale.US, "%.1f km", distance / 1000)
    } else {
      String.format(Locale.US, "%.0f m", distance)
    }
  }

  /**
   * Formats duration for display.
   * Returns "X h Y min" for durations >= 1 hour, "X min" otherwise.
   */
  fun formatDuration(): String {
    val totalMinutes = (duration / 60).toInt()
    return if (totalMinutes >= 60) {
      val hours = totalMinutes / 60
      val minutes = totalMinutes % 60
      if (minutes > 0) {
        "$hours h $minutes min"
      } else {
        "$hours h"
      }
    } else {
      "$totalMinutes min"
    }
  }
}


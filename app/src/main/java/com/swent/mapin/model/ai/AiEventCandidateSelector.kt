package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location

/**
 * Component responsible for selecting and preparing candidate events for AI recommendations.
 *
 * This class encapsulates the filtering, sorting, and mapping logic to convert a list of all
 * available events into a focused set of candidate events suitable for sending to the AI backend.
 *
 * The selection process includes:
 * 1. Time-based filtering (if a time window is provided)
 * 2. Distance-based filtering and sorting (if user location is available)
 * 3. Limiting the number of candidates to avoid overwhelming the AI backend
 * 4. Mapping to AiEventSummary with relevant metadata
 *
 * @property distanceCalculator Optional calculator for computing distances between locations
 * @property maxCandidates Maximum number of candidate events to return (default: 30)
 * @property maxDistanceKm Maximum distance in kilometers for filtering events (default: 20.0 km)
 */
class AiEventCandidateSelector(
    private val distanceCalculator: DistanceCalculator? = null,
    private val maxCandidates: Int = DEFAULT_MAX_CANDIDATES,
    private val maxDistanceKm: Double = DEFAULT_MAX_DISTANCE_KM
) {

  /**
   * Selects candidate events based on user context (location and time window).
   *
   * The selection process:
   * - Filters by time window if provided
   * - Filters by distance if user location is available
   * - Sorts by distance (ascending) when available, otherwise by start time (ascending)
   * - Clips to maxCandidates
   * - Maps to AiEventSummary with distance and location information
   *
   * @param allEvents The complete list of events to select from
   * @param userLocation Optional user location for distance-based filtering and sorting
   * @param userQueryTimeWindow Optional time range for filtering events
   * @return List of AiEventSummary objects representing the selected candidates
   */
  fun selectCandidates(
      allEvents: List<Event>,
      userLocation: Location? = null,
      userQueryTimeWindow: ClosedRange<Timestamp>? = null
  ): List<AiEventSummary> {
    var candidates = allEvents

    val now = Timestamp.now()
    candidates =
        candidates.filter { event ->
          val eventTime = event.date ?: return@filter false
          eventTime.seconds >= now.seconds
        }

    if (userQueryTimeWindow != null) {
      candidates = candidates.filter { event -> event.matchesTimeWindow(userQueryTimeWindow) }
    }

    val eventDistances =
        if (userLocation != null && distanceCalculator != null) {
          candidates.associateWith { event ->
            distanceCalculator.distanceKm(userLocation, event.location)
          }
        } else {
          emptyMap()
        }

    if (userLocation != null && eventDistances.isNotEmpty()) {
      candidates =
          candidates.filter { event ->
            val distance = eventDistances[event]
            distance != null && distance <= maxDistanceKm
          }
    }

    candidates =
        if (eventDistances.isNotEmpty()) {
          candidates.sortedBy { event -> eventDistances[event] ?: Double.MAX_VALUE }
        } else {
          candidates.sortedBy { event -> event.date?.seconds ?: Long.MAX_VALUE }
        }

    candidates = candidates.take(maxCandidates)

    return candidates.map { event ->
      event.toAiEventSummary(
          distanceKm = eventDistances[event],
          locationDescription =
              event.location.name?.takeIf { it.isNotBlank() } ?: "Location not specified")
    }
  }

  /**
   * Checks if an event's time range intersects with the given time window.
   *
   * An event matches if:
   * - Its start time falls within the window, OR
   * - Its end time falls within the window, OR
   * - It spans the entire window (starts before and ends after)
   *
   * @param timeWindow The time range to check against
   * @return true if the event intersects with the time window
   */
  private fun Event.matchesTimeWindow(timeWindow: ClosedRange<Timestamp>): Boolean {
    val startTime = this.date ?: return false
    val endTime = this.endDate ?: startTime

    val windowStart = timeWindow.start
    val windowEnd = timeWindow.endInclusive

    return !startTime.toDate().after(windowEnd.toDate()) &&
        !endTime.toDate().before(windowStart.toDate())
  }

  companion object {
    /** Default maximum number of candidate events to send to the AI backend */
    const val DEFAULT_MAX_CANDIDATES = 30

    /** Default maximum distance in kilometers for filtering events */
    const val DEFAULT_MAX_DISTANCE_KM = 20.0
  }
}

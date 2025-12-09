package com.swent.mapin.model.ai

// Assisted by AI

import com.swent.mapin.model.event.Event

/**
 * Extension function to convert an Event to AiEventSummary.
 *
 * This mapping extracts the essential information needed for AI recommendations.
 *
 * @param distanceKm Optional distance in kilometers from the user's location
 * @param locationDescription Optional human-readable location description. If not provided, uses
 *   the event's location name.
 * @return An AiEventSummary representing this event
 */
fun Event.toAiEventSummary(
    distanceKm: Double? = null,
    locationDescription: String? = null
): AiEventSummary {
  val capacityRemaining =
      capacity?.let { cap ->
        val remaining = cap - participantIds.size
        maxOf(0, remaining)
      }

  return AiEventSummary(
      id = uid,
      title = title,
      description = description,
      startTime = date,
      endTime = endDate,
      tags = tags,
      distanceKm = distanceKm,
      locationDescription = locationDescription ?: location.name,
      capacityRemaining = capacityRemaining,
      price = price)
}

/**
 * Extension function to convert a list of Events to a list of AiEventSummary.
 *
 * @param distanceProvider Optional function that provides distance for each event based on its ID
 * @param locationDescriptionProvider Optional function that provides location description for each
 *   event
 * @return List of AiEventSummary objects
 */
fun List<Event>.toAiEventSummaries(
    distanceProvider: ((String) -> Double?)? = null,
    locationDescriptionProvider: ((String) -> String?)? = null
): List<AiEventSummary> {
  return map { event ->
    event.toAiEventSummary(
        distanceKm = distanceProvider?.invoke(event.uid),
        locationDescription = locationDescriptionProvider?.invoke(event.uid))
  }
}

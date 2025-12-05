package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp

/**
 * A compact representation of an Event that will be sent to the AI recommendation backend.
 *
 * This class serves as the stable contract between the app and the backend, so field names and
 * types should remain consistent.
 *
 * @property id Unique identifier of the event
 * @property title Event title
 * @property startTime Optional start time of the event
 * @property endTime Optional end time of the event
 * @property tags List of tags associated with the event
 * @property distanceKm Optional distance in kilometers from the user's location
 * @property locationDescription Optional human-readable location description
 * @property capacityRemaining Optional number of remaining spots (null if unlimited)
 * @property price Price of the event (0.0 for free events)
 */
data class AiEventSummary(
    val id: String,
    val title: String,
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val tags: List<String> = emptyList(),
    val distanceKm: Double? = null,
    val locationDescription: String? = null,
    val capacityRemaining: Int? = null,
    val price: Double = 0.0
)

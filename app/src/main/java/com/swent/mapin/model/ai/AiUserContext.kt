package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp

/**
 * Represents coarse user context information used for AI event recommendations.
 *
 * @property approxLocation Optional approximate location description (e.g., "Lausanne,
 *   Switzerland")
 * @property maxDistanceKm Optional maximum distance in kilometers from user location
 * @property timeWindowStart Optional start of the time window for event search
 * @property timeWindowEnd Optional end of the time window for event search
 */
data class AiUserContext(
    val approxLocation: String? = null,
    val maxDistanceKm: Double? = null,
    val timeWindowStart: Timestamp? = null,
    val timeWindowEnd: Timestamp? = null
)

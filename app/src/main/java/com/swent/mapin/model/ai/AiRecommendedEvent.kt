package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Represents a single recommended event in the AI response.
 *
 * @property id Event identifier (matches AiEventSummary.id)
 * @property reason Short textual explanation of why this event was recommended
 */
data class AiRecommendedEvent(
    val id: String,
    val reason: String
)


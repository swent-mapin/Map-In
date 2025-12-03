package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Represents the response from the AI recommendation backend.
 *
 * @property assistantMessage Main text message to display to the user
 * @property recommendedEvents List of recommended events with reasons
 * @property followupQuestions Optional list of suggested follow-up questions
 */
data class AiRecommendationResponse(
    val assistantMessage: String,
    val recommendedEvents: List<AiRecommendedEvent>,
    val followupQuestions: List<String>? = null
)


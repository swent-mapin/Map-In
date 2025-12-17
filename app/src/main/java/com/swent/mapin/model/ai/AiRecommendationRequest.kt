package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Represents a request to the AI recommendation backend.
 *
 * @property userQuery Free-form text query from the user
 * @property userContext User context information for personalization
 * @property events List of candidate events to consider for recommendations
 * @property responseLanguage Language code for the AI response (e.g., "en", "fr")
 */
data class AiRecommendationRequest(
    val userQuery: String,
    val userContext: AiUserContext,
    val events: List<AiEventSummary>,
    val responseLanguage: String = "en"
)

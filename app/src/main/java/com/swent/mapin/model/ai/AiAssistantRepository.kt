package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Repository interface for AI-powered event recommendations.
 *
 * This interface abstracts the AI recommendation service, allowing for different
 * implementations (e.g., real backend, fake for testing, etc.).
 */
interface AiAssistantRepository {
  /**
   * Requests event recommendations from the AI assistant.
   *
   * @param conversationId Optional conversation identifier for multi-turn conversations.
   *                       Can be null for new conversations.
   * @param request The recommendation request containing user query, context, and candidate events
   * @return AI response with recommended events and explanations
   */
  suspend fun recommendEvents(
      conversationId: String?,
      request: AiRecommendationRequest
  ): AiRecommendationResponse
}


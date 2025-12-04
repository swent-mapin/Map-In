package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Fake implementation of AiAssistantRepository for development and testing.
 *
 * This implementation provides deterministic responses without making network calls,
 * allowing UI development and testing to proceed independently of the backend.
 */
class FakeAiAssistantRepository : AiAssistantRepository {

  override suspend fun recommendEvents(
      conversationId: String?,
      request: AiRecommendationRequest
  ): AiRecommendationResponse {

    // If no events are available, return a helpful message
    if (request.events.isEmpty()) {
      return AiRecommendationResponse(
          assistantMessage = "I couldn't find any events matching your criteria. Try adjusting your search filters or location.",
          recommendedEvents = emptyList(),
          followupQuestions = listOf(
              "Can you expand the search radius?",
              "Would you like to see events from different categories?"
          )
      )
    }

    // Recommend the first two events (or just one if only one is available)
    val eventsToRecommend = request.events.take(2)

    val recommendedEvents = eventsToRecommend.mapIndexed { index, event ->
      AiRecommendedEvent(
          id = event.id,
          reason = when (index) {
            0 -> "This event matches your interests and is happening soon"
            else -> "Popular event in your area with available spots"
          }
      )
    }

    // Build a friendly assistant message
    val eventTitles = eventsToRecommend.joinToString(" and ") { "\"${it.title}\"" }
    val assistantMessage = when (eventsToRecommend.size) {
      1 -> "Based on your query, I recommend checking out $eventTitles. It looks like a great match for what you're looking for!"
      else -> "I found ${eventsToRecommend.size} events that might interest you: $eventTitles. Both look exciting!"
    }

    val followupQuestions = listOf(
        "Would you like more details about these events?",
        "Are you interested in similar events?",
        "Do you want to see events at different times?"
    )

    return AiRecommendationResponse(
        assistantMessage = assistantMessage,
        recommendedEvents = recommendedEvents,
        followupQuestions = followupQuestions
    )
  }
}


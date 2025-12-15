package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.event.EventRepository

/**
 * Result of an AI assistant query.
 *
 * @property assistantMessage The text response from the AI to be read aloud
 * @property recommendedEvents List of events recommended by the AI
 * @property followupQuestions Optional suggested follow-up questions
 */
data class AiAssistantResult(
    val assistantMessage: String,
    val recommendedEvents: List<AiRecommendedEvent>,
    val followupQuestions: List<String>? = null
)

/**
 * Orchestrates the complete AI assistant pipeline:
 * - Takes user text queries
 * - Selects relevant candidate events
 * - Calls the AI backend
 * - Returns structured results
 * - Handles joining recommended events
 *
 * This is a simple coordinator that ties together all AI components without complex state
 * management. It's designed for a student project, prioritizing simplicity and functionality.
 *
 * @property aiRepository Repository for AI recommendations
 * @property eventRepository Repository for event data and operations
 * @property candidateSelector Selects relevant events to send to the AI
 * @property distanceCalculator Optional calculator for distance-based filtering
 */
class AiAssistantOrchestrator(
    private val aiRepository: AiAssistantRepository,
    private val eventRepository: EventRepository,
    private val candidateSelector: AiEventCandidateSelector,
    private val distanceCalculator: DistanceCalculator? = null
) {

  // Track the last query result to enable "join by index" functionality
  private var lastResult: AiAssistantResult? = null
  private var lastConversationId: String? = null

  /**
   * Process a user query through the complete AI pipeline.
   *
   * Steps:
   * 1. Fetch all available events
   * 2. Select relevant candidate events based on user context
   * 3. Build AI request with user query and candidates
   * 4. Call AI backend
   * 5. Return structured result
   *
   * @param userQuery The text query from the user (from STT or typed)
   * @param userLocation Optional user location for proximity-based recommendations
   * @param timeWindowStart Optional start of time window for event filtering
   * @param timeWindowEnd Optional end of time window for event filtering
   * @param maxDistanceKm Optional maximum distance filter in kilometers
   * @param userId User ID for fetching personalized events (uses empty string if not provided)
   * @param conversationId Optional conversation ID for multi-turn conversations (null for new)
   * @return AiAssistantResult containing the AI response and recommended events
   */
  suspend fun processQuery(
      userQuery: String,
      userLocation: Location? = null,
      timeWindowStart: Timestamp? = null,
      timeWindowEnd: Timestamp? = null,
      maxDistanceKm: Double? = null,
      userId: String = "",
      conversationId: String? = null
  ): AiAssistantResult {
    // Use provided conversationId or continue last conversation
    val currentConversationId = conversationId ?: lastConversationId

    // Step 1: Fetch all events
    // We use a basic filter with no specific constraints to get all public events
    val allEvents =
        eventRepository.getFilteredEvents(
            filters = com.swent.mapin.ui.filters.Filters(), userId = userId)

    // Step 2: Select candidate events based on user context
    val timeWindow =
        if (timeWindowStart != null && timeWindowEnd != null) {
          timeWindowStart..timeWindowEnd
        } else {
          null
        }

    val candidates =
        candidateSelector.selectCandidates(
            allEvents = allEvents, userLocation = userLocation, userQueryTimeWindow = timeWindow)

    // Step 3: Build user context for AI
    val userContext =
        AiUserContext(
            approxLocation = userLocation?.name,
            maxDistanceKm = maxDistanceKm,
            timeWindowStart = timeWindowStart,
            timeWindowEnd = timeWindowEnd)

    // Step 4: Build AI request
    val request =
        AiRecommendationRequest(
            userQuery = userQuery, userContext = userContext, events = candidates)

    // Step 5: Call AI backend
    val response = aiRepository.recommendEvents(currentConversationId, request)

    // Step 6: Store result for potential "join by index" calls
    val result =
        AiAssistantResult(
            assistantMessage = response.assistantMessage,
            recommendedEvents = response.recommendedEvents,
            followupQuestions = response.followupQuestions)

    lastResult = result
    lastConversationId = currentConversationId

    return result
  }

  /**
   * Join one of the recommended events by its index in the last query result.
   *
   * This enables natural language commands like "join the second event" to be mapped to actual
   * event IDs and executed.
   *
   * @param index The 0-based index in the recommended events list
   * @param userId The user ID performing the join action
   * @return The event ID that was joined
   * @throws IllegalStateException if no previous query result exists
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  suspend fun joinRecommendedEventByIndex(index: Int, userId: String): String {
    val result = lastResult ?: throw IllegalStateException("No previous query result available")

    if (index < 0 || index >= result.recommendedEvents.size) {
      throw IndexOutOfBoundsException(
          "Index $index is out of bounds for ${result.recommendedEvents.size} recommended events")
    }

    val eventId = result.recommendedEvents[index].id
    eventRepository.editEventAsUser(eventId, userId, join = true)
    return eventId
  }

  /**
   * Join a recommended event by its event ID.
   *
   * @param eventId The unique identifier of the event to join
   * @param userId The user ID performing the join action
   */
  suspend fun joinRecommendedEventById(eventId: String, userId: String) {
    eventRepository.editEventAsUser(eventId, userId, join = true)
  }

  /** Reset the conversation state to start a new conversation. */
  fun resetConversation() {
    lastResult = null
    lastConversationId = null
  }

  /** Get the last query result (useful for UI or debugging). */
  fun getLastResult(): AiAssistantResult? = lastResult
}

package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

/**
 * Remote implementation of AiAssistantRepository using Firebase Callable Functions.
 *
 * This implementation communicates with the Firebase Cloud Function "recommendEvents" which uses
 * OpenRouter AI (Amazon Nova 2 Lite) to analyze events and provide recommendations.
 *
 * @property functions FirebaseFunctions instance for calling cloud functions
 * @property gson Gson instance for JSON serialization/deserialization
 */
class FirebaseAiAssistantRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val gson: Gson = Gson()
) : AiAssistantRepository {

  /**
   * Requests event recommendations from the Firebase Cloud Function.
   *
   * @param conversationId Optional conversation identifier (currently unused)
   * @param request The recommendation request containing user query and candidate events
   * @return AI response with recommended events and explanations
   * @throws Exception if the function call fails or response cannot be parsed
   */
  override suspend fun recommendEvents(
      conversationId: String?,
      request: AiRecommendationRequest
  ): AiRecommendationResponse {
    try {
      // Convert request to Map for Firebase Functions
      val requestMap =
          mapOf(
              "userQuery" to request.userQuery,
              "userContext" to
                  mapOf(
                      "approxLocation" to request.userContext.approxLocation,
                      "maxDistanceKm" to request.userContext.maxDistanceKm,
                      "timeWindowStart" to
                          request.userContext.timeWindowStart?.let {
                            mapOf("_seconds" to it.seconds, "_nanoseconds" to it.nanoseconds)
                          },
                      "timeWindowEnd" to
                          request.userContext.timeWindowEnd?.let {
                            mapOf("_seconds" to it.seconds, "_nanoseconds" to it.nanoseconds)
                          }),
              "events" to
                  request.events.map { event ->
                    mapOf(
                        "id" to event.id,
                        "title" to event.title,
                        "description" to event.description,
                        "startTime" to
                            event.startTime?.let {
                              mapOf("_seconds" to it.seconds, "_nanoseconds" to it.nanoseconds)
                            },
                        "endTime" to
                            event.endTime?.let {
                              mapOf("_seconds" to it.seconds, "_nanoseconds" to it.nanoseconds)
                            },
                        "tags" to event.tags,
                        "distanceKm" to event.distanceKm,
                        "locationDescription" to event.locationDescription,
                        "capacityRemaining" to event.capacityRemaining,
                        "price" to event.price)
                  })

      // Call Firebase Cloud Function
      val result = functions.getHttpsCallable("recommendEvents").call(requestMap).await()

      // Parse response
      val responseJson = gson.toJson(result.data)
      return gson.fromJson(responseJson, AiRecommendationResponse::class.java)
          ?: throw Exception("Failed to parse AI response")
    } catch (e: Exception) {
      throw Exception("AI recommendation failed: ${e.message}", e)
    }
  }
}

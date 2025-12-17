package com.swent.mapin.model.ai

// Assisted by AI

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Direct implementation of AiAssistantRepository that calls OpenRouter API directly.
 *
 * This implementation bypasses Firebase Cloud Functions and makes HTTP calls directly to the
 * OpenRouter API using Amazon Nova 2 Lite model.
 *
 * @property client OkHttpClient instance for making HTTP requests
 * @property gson Gson instance for JSON serialization/deserialization
 * @property ioDispatcher Coroutine dispatcher for IO operations
 * @property apiKey OpenRouter API key for authentication
 */
class DirectOpenRouterRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineContext = Dispatchers.IO,
    private val apiKey: String = ""
) : AiAssistantRepository {

  companion object {
    private const val TAG = "DirectOpenRouter"
    private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "amazon/nova-2-lite-v1:free"
  }

  /**
   * Requests event recommendations from OpenRouter AI.
   *
   * @param conversationId Optional conversation identifier (currently unused)
   * @param request The recommendation request containing user query and candidate events
   * @return AI response with recommended events and explanations
   * @throws Exception if the API call fails or response cannot be parsed
   */
  override suspend fun recommendEvents(
      conversationId: String?,
      request: AiRecommendationRequest
  ): AiRecommendationResponse =
      withContext(ioDispatcher) {
        runCatching {
              Log.d(TAG, "Processing recommendation request with ${request.events.size} events")

              val (currentDateStr, currentTimeStr) = currentDateTimeStrings()
              val eventsFormatted = formatEventsForPrompt(request)

              val systemPrompt = buildSystemPrompt(currentDateStr, currentTimeStr)
              val userPrompt = buildUserPrompt(request, eventsFormatted)
              val requestPayload = buildRequestPayload(systemPrompt, userPrompt)

              val responseBody = executeRequest(requestPayload)
              parseAiResponse(responseBody)
            }
            .getOrElse { error ->
              Log.e(TAG, "Error in recommendEvents: ${error.message}")
              fallbackTechnicalIssue()
            }
      }

  private fun currentDateTimeStrings(): Pair<String, String> {
    val now = Date()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    return dateFormat.format(now) to timeFormat.format(now)
  }

  private fun formatEventsForPrompt(request: AiRecommendationRequest): List<Map<String, String>> {
    return request.events.map { event ->
      val startDate =
          event.startTime?.let {
            val date = Date(it.seconds * 1000)
            SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.ENGLISH).format(date)
          } ?: "Date not specified"

      mapOf(
          "id" to event.id,
          "title" to event.title,
          "description" to (event.description ?: "No description"),
          "date" to startDate,
          "tags" to event.tags.joinToString(", "),
          "location" to (event.locationDescription ?: "Location not specified"),
          "distance" to
              if (event.distanceKm != null)
                  "${String.format(Locale.ENGLISH, "%.1f", event.distanceKm)} km"
              else "Distance unknown",
          "price" to if (event.price == 0.0) "Free" else "${event.price} CHF",
          "places" to
              if (event.capacityRemaining != null) "${event.capacityRemaining} places remaining"
              else "Unlimited capacity")
    }
  }

  private fun buildSystemPrompt(currentDateStr: String, currentTimeStr: String): String {
    return """
You are Mapi, an enthusiastic and friendly voice assistant helping users discover exciting events!

CRITICAL RULES FOR VOICE OUTPUT:
Keep your assistantMessage 10 to 15 seconds long because it will be read aloud by text-to-speech.
Never use markdown formatting like **bold**, *italics*, or bullet points. Write plain spoken text only.

Current date and time: $currentDateStr at $currentTimeStr

LANGUAGE RULE (MANDATORY):
You MUST respond ENTIRELY in English.
This applies to: assistantMessage, all reason fields, and all followupQuestions.
Event titles should remain as-is (don't translate them).

YOUR ROLE:
- Be warm, enthusiastic, and natural in your communication
- Understand user intent even if vaguely or informally expressed
- Sell events using their descriptions and strengths
- Be honest if you don't have many options to suggest

FLEXIBLE UNDERSTANDING (works in any language):
- "food", "eat", "restaurant", "manger", "bouffe" = cooking events, restaurants, gastronomy
- "drink", "bar", "boire", "apéro" = bars, tastings, evening events
- "sport", "exercise", "active", "bouger" = sports events, fitness, outdoor
- "culture", "museum", "art", "musée" = cultural events, exhibitions
- "party", "night out", "soirée", "fête" = concerts, bars, festivals, nighttime events
- "learn", "discover", "apprendre", "découvrir" = workshops, courses, conferences
Adapt to context and be intelligent in interpretation!

RECOMMENDATION RULES:
1. Recommend 2-3 events if possible (or fewer if not enough relevant ones)
2. ALWAYS use real event IDs (NEVER invent them)
3. Only recommend FUTURE events (after $currentDateStr $currentTimeStr)
4. Analyze EVERYTHING: title, description, tags, date, location, price, available spots
5. Highlight what makes each event special and attractive
6. If you have few options or no perfect match, say so honestly
7. Be conversational - your message will be read aloud!

YOUR TONE:
- Natural and enthusiastic (but not excessive)
- Personalized according to the request
- Honest if options are limited
- Encourage user to explore

EXAMPLES OF SELLING MESSAGES:
GOOD: "I found 2 amazing cooking events for you! First, a French pastry workshop with a starred chef - perfect to impress your friends! Second, a wine and cheese tasting in an authentic cellar. Both are highly rated!"

GOOD: "Great news: I found a perfect event for you! It's an Italian cooking class this Saturday. However, there are only 5 spots left, so don't wait if you're interested!"

GOOD with few options: "Unfortunately, I only have one cooking event available right now, but it looks really great! It's a creative brunch next Sunday. Otherwise, let me know if you want me to search for other types of events?"

AVOID: "Here are 2 events." (too dry, not selling)
AVOID: "There is a cooking workshop." (boring, not engaging)

RESPONSE FORMAT (strict JSON):
{
  "assistantMessage": "Natural and enthusiastic message (3-5 sentences) that SELLS the events using their descriptions and strengths",
  "recommendedEvents": [
    {
      "id": "EXACT_ID_1",
      "reason": "Short AND selling reason (e.g., 'Starred chef, hands-on workshop, warm atmosphere')"
    },
    {
      "id": "EXACT_ID_2",
      "reason": "Reason highlighting the event's strengths"
    }
  ],
  "followupQuestions": ["Engaging question 1?", "Engaging question 2?"]
}

⚡ COMPLETE EXAMPLE:
Request: "I want to eat something"
{
  "assistantMessage": "Great! I found 2 delicious events that will delight you! First, a Thai cooking workshop this Saturday with a chef just back from Bangkok - you'll learn to make restaurant-quality pad thai! Then on Sunday, there's a creative vegetarian brunch on an urban farm, relaxed atmosphere and ultra-fresh products. Both have limited spots!",
  "recommendedEvents": [
    {
      "id": "thai_cooking_123",
      "reason": "Authentic chef, pro techniques, pad thai and curry included"
    },
    {
      "id": "urban_brunch_456",
      "reason": "Local products, unique setting, creative vegetarian"
    }
  ],
  "followupQuestions": ["Would you like to know prices and schedules?", "Do you prefer a traditional restaurant instead?"]
}

SPECIAL CASES:
- If 0 relevant events: Say so frankly and suggest broadening the search
- If only 1 event: Really highlight it and suggest alternatives
- If no "cooking" events but request "eat": Suggest closest matches
- If all events are past: Explain and ask if looking for later dates

IMPORTANT: 
- Respond ONLY in valid JSON
- No text before or after the JSON
- No markdown (no ```)
- ALWAYS use real event IDs provided
      """
        .trimIndent()
  }

  private fun buildUserPrompt(
      request: AiRecommendationRequest,
      eventsFormatted: List<Map<String, String>>
  ): String {
    return """
Request: "${request.userQuery}"

${if (request.events.isEmpty()) "No events available." else "${request.events.size} event(s):"}
${gson.toJson(eventsFormatted)}

Recommend 2-3 relevant events with selling reasons.
      """
        .trimIndent()
  }

  private fun buildRequestPayload(systemPrompt: String, userPrompt: String): JsonObject {
    return JsonObject().apply {
      addProperty("model", MODEL)
      add(
          "messages",
          gson.toJsonTree(
              listOf(
                  mapOf("role" to "system", "content" to systemPrompt),
                  mapOf("role" to "user", "content" to userPrompt))))
      addProperty("temperature", 0.7)
      addProperty("max_tokens", 4000)
    }
  }

  private fun executeRequest(requestPayload: JsonObject): String {
    val requestBuilder =
        Request.Builder()
            .url(OPENROUTER_API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://mapin.app")
            .addHeader("X-Title", "Map-In Event Recommender")
            .post(requestPayload.toString().toRequestBody("application/json".toMediaType()))

    // Only add Authorization header if API key is provided
    if (apiKey.isNotEmpty()) {
      requestBuilder.addHeader("Authorization", "Bearer $apiKey")
    }

    val httpRequest = requestBuilder.build()

    client.newCall(httpRequest).execute().use { response ->
      if (!response.isSuccessful) {
        throw Exception("OpenRouter API call failed: ${response.code} - ${response.message}")
      }
      return response.body?.string() ?: throw Exception("Empty response from OpenRouter API")
    }
  }

  private fun parseAiResponse(responseBody: String): AiRecommendationResponse {
    Log.d(TAG, "Received API response (${responseBody.length} chars)")

    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
    val messageContent = extractMessageContent(jsonResponse)
    val cleanedResponse = cleanResponse(messageContent)

    return runCatching {
          gson.fromJson(cleanedResponse, AiRecommendationResponse::class.java)?.also {
            Log.d(TAG, "Successfully parsed ${it.recommendedEvents.size} recommended events")
          } ?: throw Exception("Parsing returned null")
        }
        .getOrElse { error ->
          Log.e(TAG, "Failed to parse AI response JSON", error)
          fallbackParsingIssue()
        }
  }

  private fun extractMessageContent(jsonResponse: JsonObject): String {
    val choicesArray =
        jsonResponse.getAsJsonArray("choices") ?: throw Exception("API returned no choices")
    if (choicesArray.size() == 0) {
      throw Exception("API returned no choices")
    }

    val messageObj = choicesArray[0]?.asJsonObject?.getAsJsonObject("message")
    if (messageObj == null) {
      throw Exception("API returned no message")
    }

    val aiResponseText = messageObj["content"]?.asString
    if (aiResponseText.isNullOrEmpty()) {
      throw Exception("API returned empty content")
    }
    return aiResponseText
  }

  private fun cleanResponse(aiResponseText: String): String {
    return aiResponseText
        .replace("```json\n", "")
        .replace("```json", "")
        .replace("```\n", "")
        .replace("```", "")
        .replace("\\n", "\n")
        .trim()
  }

  private fun fallbackParsingIssue(): AiRecommendationResponse {
    return AiRecommendationResponse(
        assistantMessage =
            "Sorry, I couldn't properly analyze the available events. Can you rephrase your request?",
        recommendedEvents = emptyList(),
        followupQuestions =
            listOf(
                "Would you like to see all available events?", "Can you specify your preferences?"))
  }

  private fun fallbackTechnicalIssue(): AiRecommendationResponse {
    return AiRecommendationResponse(
        assistantMessage = "Sorry, I encountered a technical issue. Can you try again in a moment?",
        recommendedEvents = emptyList(),
        followupQuestions =
            listOf("Would you like me to try again?", "Can you rephrase your request?"))
  }
}

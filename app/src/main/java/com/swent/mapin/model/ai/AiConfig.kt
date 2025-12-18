package com.swent.mapin.model.ai

// Assisted by AI

import android.content.Context
import com.google.gson.Gson
import com.swent.mapin.model.event.EventRepository
import okhttp3.OkHttpClient

/**
 * Centralized configuration and factory for AI Assistant components.
 *
 * This object provides:
 * - Feature flags (enable/disable AI)
 * - Repository factories
 * - Orchestrator factories
 * - Speech service factories
 */
object AiConfig {

  // ============================================================================
  // FEATURE FLAGS
  // ============================================================================

  /**
   * Controls whether the AI assistant feature is enabled.
   *
   * When true, the AI recommendation assistant UI and functionality will be available. When false,
   * the feature will be hidden from users.
   */
  const val AI_ASSISTANT_ENABLED: Boolean = true

  // ============================================================================
  // REPOSITORY FACTORY
  // ============================================================================

  /**
   * Provides the appropriate AiAssistantRepository based on feature flags.
   *
   * @param okHttpClient OkHttpClient instance for HTTP requests
   * @param gson Gson instance for JSON serialization
   * @param baseUrl Backend URL (deprecated, kept for compatibility)
   * @param apiKey OpenAI API key (optional, defaults to key from OpenAIKeyConfig)
   * @return AiAssistantRepository implementation (DirectOpenAI or Fake based on feature flag)
   */
  @Suppress("UNUSED_PARAMETER")
  fun provideRepository(
      okHttpClient: OkHttpClient,
      gson: Gson,
      baseUrl: String,
      apiKey: String = OpenAIKeyConfig.OPENAI_API_KEY
  ): AiAssistantRepository {
    return if (AI_ASSISTANT_ENABLED) {
      DirectOpenAIRepository(client = okHttpClient, gson = gson, apiKey = apiKey)
    } else {
      FakeAiAssistantRepository()
    }
  }

  // ============================================================================
  // ORCHESTRATOR FACTORY
  // ============================================================================

  /**
   * Create an AiAssistantOrchestrator with the specified components.
   *
   * @param aiRepository The AI repository (remote or fake)
   * @param eventRepository The event repository
   * @param useDistanceCalculation Whether to use distance-based filtering (default: true)
   * @return Configured AiAssistantOrchestrator
   */
  fun createOrchestrator(
      aiRepository: AiAssistantRepository,
      eventRepository: EventRepository,
      useDistanceCalculation: Boolean = true
  ): AiAssistantOrchestrator {
    val distanceCalculator =
        if (useDistanceCalculation) {
          DistanceCalculator()
        } else {
          null
        }

    val candidateSelector =
        AiEventCandidateSelector(
            distanceCalculator = distanceCalculator, maxCandidates = 30, maxDistanceKm = 20.0)

    return AiAssistantOrchestrator(
        aiRepository = aiRepository,
        eventRepository = eventRepository,
        candidateSelector = candidateSelector,
        distanceCalculator = distanceCalculator)
  }

  // ============================================================================
  // SPEECH SERVICES FACTORY
  // ============================================================================

  /**
   * Create an Android Text-to-Speech service.
   *
   * @param context Android context
   * @param onInitComplete Optional callback when TTS is initialized
   * @return AndroidTextToSpeechService instance
   */
  fun createTextToSpeechService(
      context: Context,
      onInitComplete: ((Boolean) -> Unit)? = null
  ): TextToSpeechService {
    return AndroidTextToSpeechService(context, onInitComplete)
  }

  /**
   * Create an Android Speech-to-Text service.
   *
   * @param context Android context
   * @return AndroidSpeechToTextService instance
   */
  fun createSpeechToTextService(context: Context): SpeechToTextService {
    return AndroidSpeechToTextService(context)
  }
}

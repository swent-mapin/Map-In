package com.swent.mapin.model.ai

// Assisted by AI

import android.content.Context
import com.swent.mapin.model.event.EventRepository
import io.mockk.mockk
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Unit tests for AiConfig.
 *
 * Tests the factory methods and configuration of AI components.
 */
class AiConfigTest {

  @Test
  fun `AI_ASSISTANT_ENABLED is true`() {
    // Verify that the AI assistant feature is enabled
    assertTrue(AiConfig.AI_ASSISTANT_ENABLED)
  }

  @Test
  fun `createOrchestrator returns configured orchestrator with distance calculation`() {
    val aiRepository = mockk<AiAssistantRepository>(relaxed = true)
    val eventRepository = mockk<EventRepository>(relaxed = true)

    val orchestrator =
        AiConfig.createOrchestrator(
            aiRepository = aiRepository,
            eventRepository = eventRepository,
            useDistanceCalculation = true)

    assertNotNull(orchestrator)
  }

  @Test
  fun `createOrchestrator returns configured orchestrator without distance calculation`() {
    val aiRepository = mockk<AiAssistantRepository>(relaxed = true)
    val eventRepository = mockk<EventRepository>(relaxed = true)

    val orchestrator =
        AiConfig.createOrchestrator(
            aiRepository = aiRepository,
            eventRepository = eventRepository,
            useDistanceCalculation = false)

    assertNotNull(orchestrator)
  }

  @Test
  fun `createTextToSpeechService returns AndroidTextToSpeechService`() {
    val context = mockk<Context>(relaxed = true)

    val tts = AiConfig.createTextToSpeechService(context)

    assertNotNull(tts)
    assertTrue(tts is AndroidTextToSpeechService)
  }

  @Test
  fun `createTextToSpeechService with callback returns AndroidTextToSpeechService`() {
    val context = mockk<Context>(relaxed = true)
    var callbackInvoked = false

    val tts = AiConfig.createTextToSpeechService(context) { success -> callbackInvoked = true }

    assertNotNull(tts)
    assertTrue(tts is AndroidTextToSpeechService)
  }

  @Test
  fun `createSpeechToTextService returns AndroidSpeechToTextService`() {
    val context = mockk<Context>(relaxed = true)

    val stt = AiConfig.createSpeechToTextService(context)

    assertNotNull(stt)
    assertTrue(stt is AndroidSpeechToTextService)
  }

  @Test
  fun `createOrchestrator uses default parameters correctly`() {
    val aiRepository = mockk<AiAssistantRepository>(relaxed = true)
    val eventRepository = mockk<EventRepository>(relaxed = true)

    // Should not throw exception with default parameters
    val orchestrator =
        AiConfig.createOrchestrator(aiRepository = aiRepository, eventRepository = eventRepository)

    assertNotNull(orchestrator)
  }
}

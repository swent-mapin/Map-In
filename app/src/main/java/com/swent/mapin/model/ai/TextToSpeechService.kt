package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Simple interface for Text-to-Speech functionality.
 *
 * This interface abstracts the TTS implementation to allow for different providers and make testing
 * easier.
 *
 * Thread Safety: All callback functions (onComplete) are guaranteed to be invoked on the main/UI
 * thread, making it safe to update UI components directly from these callbacks.
 *
 * Note: This is a minimal interface for the AI assistant pipeline. UI integration and lifecycle
 * management are handled separately.
 */
interface TextToSpeechService {
  /**
   * Speak the given text aloud.
   *
   * @param text The text to speak
   * @param onComplete Optional callback invoked when speech is complete (called on main thread)
   */
  fun speak(text: String, onComplete: (() -> Unit)? = null)

  /** Stop any ongoing speech. */
  fun stop()

  /**
   * Check if the service is currently speaking.
   *
   * @return true if speaking, false otherwise
   */
  fun isSpeaking(): Boolean

  /**
   * Release resources used by the service. Should be called when the service is no longer needed.
   */
  fun shutdown()
}

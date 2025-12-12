package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Simple interface for Speech-to-Text functionality.
 *
 * This interface abstracts the STT implementation to allow for different providers (Google, local,
 * etc.) and make testing easier.
 *
 * Thread Safety: All callback functions (onResult, onError) are guaranteed to be invoked on the
 * main/UI thread, making it safe to update UI components directly from these callbacks.
 *
 * Note: This is a minimal interface for the AI assistant pipeline. UI integration, permissions, and
 * lifecycle management are handled separately.
 */
interface SpeechToTextService {
  /**
   * Start listening for speech input.
   *
   * When speech is recognized, onResult will be called with the text. If an error occurs, onError
   * will be called with an error message.
   *
   * @param onResult Callback invoked with the recognized text (called on main thread)
   * @param onError Callback invoked if an error occurs during recognition (called on main thread)
   */
  fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit)

  /** Stop listening for speech input. */
  fun stopListening()

  /**
   * Check if the service is currently listening.
   *
   * @return true if listening, false otherwise
   */
  fun isListening(): Boolean

  /**
   * Release resources used by the service. Should be called when the service is no longer needed.
   */
  fun destroy()
}

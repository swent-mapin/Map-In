package com.swent.mapin.model.ai

// Assisted by AI

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Android implementation of Text-to-Speech using the built-in TextToSpeech API.
 *
 * This is a simple wrapper around Android's TextToSpeech API. It handles the basic text → voice
 * conversion needed for the AI assistant pipeline.
 *
 * @property context Android context needed for TextToSpeech
 */
class AndroidTextToSpeechService(
    private val context: Context,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) : TextToSpeechService {

  private var tts: TextToSpeech? = null
  private var isInitialized = false
  private val pendingSpeeches = mutableListOf<Pair<String, (() -> Unit)?>>()

  init {
    tts =
        TextToSpeech(context) { status ->
          if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isInitialized =
                result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

            // Make the voice friendlier: slightly higher pitch and slightly slower
            if (isInitialized) {
              tts?.setPitch(1.3f) // Slightly higher pitch (1.0 = normal)
              tts?.setSpeechRate(1f)
            }
            // Process any pending speeches
            if (isInitialized) {
              pendingSpeeches.forEach { (text, callback) -> speakInternal(text, callback) }
              pendingSpeeches.clear()
            }

            onInitComplete?.invoke(isInitialized)
          } else {
            onInitComplete?.invoke(false)
          }
        }
  }

  override fun speak(text: String, onComplete: (() -> Unit)?) {
    if (isInitialized) {
      speakInternal(text, onComplete)
    } else {
      // Queue for later if not initialized yet
      pendingSpeeches.add(text to onComplete)
    }
  }

  private fun speakInternal(text: String, onComplete: (() -> Unit)?) {
    val utteranceId = "utterance_${System.currentTimeMillis()}"

    if (onComplete != null) {
      tts?.setOnUtteranceProgressListener(
          object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
              // Speech started
            }

            override fun onDone(utteranceId: String?) {
              onComplete()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
              onComplete()
            }
          })
    }

    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
  }

  override fun stop() {
    tts?.stop()
  }

  override fun isSpeaking(): Boolean {
    return tts?.isSpeaking ?: false
  }

  override fun shutdown() {
    tts?.stop()
    tts?.shutdown()
    tts = null
    isInitialized = false
    pendingSpeeches.clear()
  }
}

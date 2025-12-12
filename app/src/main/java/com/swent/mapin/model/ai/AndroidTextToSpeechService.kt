package com.swent.mapin.model.ai

// Assisted by AI

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
  private val mainHandler = Handler(Looper.getMainLooper())

  init {
    tts =
        TextToSpeech(context) { status ->
          if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            isInitialized =
                result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

            if (!isInitialized) {
              Log.w("AndroidTTS", "Language not supported")
            }

            if (isInitialized) {
              pendingSpeeches.forEach { (text, callback) -> speakInternal(text, callback) }
              pendingSpeeches.clear()
            }

            onInitComplete?.invoke(isInitialized)
          } else {
            Log.e("AndroidTTS", "TTS initialization failed")
            onInitComplete?.invoke(false)
          }
        }
  }

  override fun speak(text: String, onComplete: (() -> Unit)?) {
    if (isInitialized) {
      speakInternal(text, onComplete)
    } else {
      pendingSpeeches.add(text to onComplete)
    }
  }

  private fun speakInternal(text: String, onComplete: (() -> Unit)?) {
    val utteranceId = "utterance_${System.currentTimeMillis()}"

    if (onComplete != null) {
      tts?.setOnUtteranceProgressListener(
          object : UtteranceProgressListener() {
            @Suppress("OVERRIDE_DEPRECATION") override fun onStart(utteranceId: String?) {}

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onDone(utteranceId: String?) {
              mainHandler.post { onComplete() }
            }

            @Suppress("OVERRIDE_DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
              Log.e("AndroidTTS", "Speech error for utterance: $utteranceId")
              mainHandler.post { onComplete() }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
              Log.e("AndroidTTS", "Speech error: $errorCode for utterance: $utteranceId")
              mainHandler.post { onComplete() }
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
    tts?.let {
      it.stop()
      it.shutdown()
    }
    tts = null
    isInitialized = false
    pendingSpeeches.clear()
  }
}

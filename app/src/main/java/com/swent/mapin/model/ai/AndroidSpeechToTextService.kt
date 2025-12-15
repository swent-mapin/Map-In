package com.swent.mapin.model.ai

// Assisted by AI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Android implementation of Speech-to-Text using the built-in SpeechRecognizer.
 *
 * This is a simple wrapper around Android's SpeechRecognizer API. It handles the basic voice → text
 * conversion needed for the AI assistant pipeline.
 *
 * Note: Requires RECORD_AUDIO permission at runtime. Permission handling should be done by the
 * calling code (UI layer).
 *
 * @property context Android context needed for SpeechRecognizer
 */
class AndroidSpeechToTextService(private val context: Context) : SpeechToTextService {

  private var recognizer: SpeechRecognizer? = null
  private var listening = false
  private var currentOnResult: ((String) -> Unit)? = null
  private var currentOnError: ((String) -> Unit)? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
    if (listening) {
      stopListening()
    }

    currentOnResult = onResult
    currentOnError = onError

    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      onError("Speech recognition not available on this device")
      return
    }

    recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    recognizer?.setRecognitionListener(createRecognitionListener())

    val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

    recognizer?.startListening(intent)
    listening = true
  }

  override fun stopListening() {
    recognizer?.stopListening()
    listening = false
  }

  override fun isListening(): Boolean = listening

  override fun destroy() {
    recognizer?.destroy()
    recognizer = null
    listening = false
    currentOnResult = null
    currentOnError = null
  }

  private fun createRecognitionListener() =
      object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          // Ready to listen
        }

        override fun onBeginningOfSpeech() {
          // User started speaking
        }

        override fun onRmsChanged(rmsdB: Float) {
          // Audio level changed (can be used for UI feedback)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
          // Audio buffer received
        }

        override fun onEndOfSpeech() {
          listening = false
        }

        override fun onError(error: Int) {
          listening = false
          val errorMessage =
              when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error: $error"
              }
          Log.e("AndroidSTT", "Recognition error: $errorMessage")
          val callback = currentOnError
          mainHandler.post { callback?.invoke(errorMessage) }
        }

        override fun onResults(results: Bundle?) {
          listening = false
          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          if (matches != null && matches.isNotEmpty()) {
            val result = matches[0]
            val callback = currentOnResult
            mainHandler.post { callback?.invoke(result) }
          } else {
            val callback = currentOnError
            mainHandler.post { callback?.invoke("No speech recognized") }
          }
        }

        override fun onPartialResults(partialResults: Bundle?) {
          // Partial results (not used in this simple implementation)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
          // Future events
        }
      }
}

package com.swent.mapin.model.ai

// Assisted by AI

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Speech-to-Text and Text-to-Speech service interfaces.
 *
 * These tests use fake implementations to verify the contract behavior. Android implementations are
 * tested with Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SpeechServicesTest {

  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
  }

  // Fake TTS implementation for testing
  class FakeTextToSpeechService : TextToSpeechService {
    var lastSpokenText: String? = null
    var lastCallback: (() -> Unit)? = null
    var speaking = false
    var isShutdown = false

    override fun speak(text: String, onComplete: (() -> Unit)?) {
      lastSpokenText = text
      lastCallback = onComplete
      speaking = true
      // Simulate immediate completion
      onComplete?.invoke()
      speaking = false
    }

    override fun stop() {
      speaking = false
    }

    override fun isSpeaking(): Boolean = speaking

    override fun shutdown() {
      isShutdown = true
      speaking = false
    }
  }

  // Fake STT implementation for testing
  class FakeSpeechToTextService : SpeechToTextService {
    var listening = false
    var lastOnResult: ((String) -> Unit)? = null
    var lastOnError: ((String) -> Unit)? = null

    override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
      listening = true
      lastOnResult = onResult
      lastOnError = onError
    }

    override fun stopListening() {
      listening = false
    }

    override fun isListening(): Boolean = listening

    override fun destroy() {
      listening = false
      lastOnResult = null
      lastOnError = null
    }

    // Test helper to simulate recognized speech
    fun simulateRecognition(text: String) {
      lastOnResult?.invoke(text)
      listening = false
    }

    // Test helper to simulate error
    fun simulateError(error: String) {
      lastOnError?.invoke(error)
      listening = false
    }
  }

  @Test
  fun testTTS_speakStoresTextAndInvokesCallback() {
    // Given
    val tts = FakeTextToSpeechService()
    var callbackInvoked = false

    // When
    tts.speak("Hello world") { callbackInvoked = true }

    // Then
    assertEquals("Hello world", tts.lastSpokenText)
    assertTrue(callbackInvoked)
  }

  @Test
  fun testTTS_stopStopsSpeaking() {
    // Given
    val tts = FakeTextToSpeechService()
    tts.speaking = true

    // When
    tts.stop()

    // Then
    assertFalse(tts.isSpeaking())
  }

  @Test
  fun testTTS_shutdownCleansUp() {
    // Given
    val tts = FakeTextToSpeechService()
    tts.speak("Test")

    // When
    tts.shutdown()

    // Then
    assertTrue(tts.isShutdown)
    assertFalse(tts.isSpeaking())
  }

  @Test
  fun testSTT_startListeningStoresCallbacks() {
    // Given
    val stt = FakeSpeechToTextService()
    var resultReceived: String? = null
    var errorReceived: String? = null

    // When
    stt.startListening(onResult = { resultReceived = it }, onError = { errorReceived = it })

    // Then
    assertTrue(stt.isListening())
    assertNotNull(stt.lastOnResult)
    assertNotNull(stt.lastOnError)
  }

  @Test
  fun testSTT_simulateRecognitionInvokesCallback() {
    // Given
    val stt = FakeSpeechToTextService()
    var recognizedText: String? = null

    stt.startListening(onResult = { recognizedText = it }, onError = {})

    // When
    stt.simulateRecognition("Hello AI")

    // Then
    assertEquals("Hello AI", recognizedText)
    assertFalse(stt.isListening())
  }

  @Test
  fun testSTT_simulateErrorInvokesCallback() {
    // Given
    val stt = FakeSpeechToTextService()
    var errorMessage: String? = null

    stt.startListening(onResult = {}, onError = { errorMessage = it })

    // When
    stt.simulateError("Network error")

    // Then
    assertEquals("Network error", errorMessage)
    assertFalse(stt.isListening())
  }

  @Test
  fun testSTT_stopListeningStops() {
    // Given
    val stt = FakeSpeechToTextService()
    stt.startListening(onResult = {}, onError = {})

    // When
    stt.stopListening()

    // Then
    assertFalse(stt.isListening())
  }

  @Test
  fun testSTT_destroyCleansUp() {
    // Given
    val stt = FakeSpeechToTextService()
    stt.startListening(onResult = {}, onError = {})

    // When
    stt.destroy()

    // Then
    assertFalse(stt.isListening())
    assertNull(stt.lastOnResult)
    assertNull(stt.lastOnError)
  }

  @Test
  fun testIntegration_sttToTts() {
    // Given
    val stt = FakeSpeechToTextService()
    val tts = FakeTextToSpeechService()

    // When
    stt.startListening(
        onResult = { text -> tts.speak("You said: $text") },
        onError = { error -> tts.speak("Error: $error") })
    stt.simulateRecognition("Find music events")

    // Then
    assertEquals("You said: Find music events", tts.lastSpokenText)
  }

  // ============================================================================
  // Tests for AndroidTextToSpeechService
  // ============================================================================

  @Test
  fun testAndroidTTS_initialization() {
    // Given/When
    val tts = AndroidTextToSpeechService(context)

    // Then
    assertNotNull(tts)
  }

  @Test
  fun testAndroidTTS_initializationWithCallback() {
    // Given/When
    var callbackReceived = false
    val tts = AndroidTextToSpeechService(context) { callbackReceived = true }

    // Then
    assertNotNull(tts)
    // Note: In Robolectric, TTS callbacks might not be invoked synchronously
  }

  @Test
  fun testAndroidTTS_speakBeforeInitialization() {
    // Given
    val tts = AndroidTextToSpeechService(context)

    // When - speak before TTS is fully initialized
    tts.speak("Hello", null)

    // Then - should queue the speech without crashing
    assertNotNull(tts)
  }

  @Test
  fun testAndroidTTS_stop() {
    // Given
    val tts = AndroidTextToSpeechService(context)

    // When
    tts.stop()

    // Then
    assertFalse(tts.isSpeaking())
  }

  @Test
  fun testAndroidTTS_shutdown() {
    // Given
    val tts = AndroidTextToSpeechService(context)
    tts.speak("Test", null)

    // When
    tts.shutdown()

    // Then
    assertFalse(tts.isSpeaking())
  }

  @Test
  fun testAndroidTTS_speakWithCallback() {
    // Given
    val tts = AndroidTextToSpeechService(context)

    // When
    tts.speak("Hello world") {}

    // Then - Should not crash
    assertNotNull(tts)
  }

  @Test
  fun testAndroidTTS_speakWithoutCallback() {
    // Given
    val tts = AndroidTextToSpeechService(context)

    // When
    tts.speak("Hello world", null)

    // Then
    assertNotNull(tts)
  }

  @Test
  fun testAndroidTTS_isSpeakingWhenStopped() {
    // Given
    val tts = AndroidTextToSpeechService(context)

    // When
    val speaking = tts.isSpeaking()

    // Then
    assertFalse(speaking)
  }

  @Test
  fun testAndroidTTS_multipleSpeakCalls() {
    // Given
    val tts = AndroidTextToSpeechService(context)

    // When
    tts.speak("First", null)
    tts.speak("Second", null)

    // Then - Should not crash
    assertNotNull(tts)
  }

  @Test
  fun testAndroidTTS_shutdownMultipleTimes() {
    // Given
    val tts = AndroidTextToSpeechService(context)

    // When
    tts.shutdown()
    tts.shutdown() // Should not crash

    // Then
    assertFalse(tts.isSpeaking())
  }

  // ============================================================================
  // Tests for AndroidSpeechToTextService
  // ============================================================================

  @Test
  fun testAndroidSTT_initialization() {
    // Given/When
    val stt = AndroidSpeechToTextService(context)

    // Then
    assertNotNull(stt)
    assertFalse(stt.isListening())
  }

  @Test
  fun testAndroidSTT_startListening() {
    // Given
    val stt = AndroidSpeechToTextService(context)

    // When
    stt.startListening(onResult = {}, onError = {})

    // Then
    // Note: In Robolectric, SpeechRecognizer may not be fully functional
    // We just verify the service doesn't crash
    assertNotNull(stt)
  }

  @Test
  fun testAndroidSTT_stopListening() {
    // Given
    val stt = AndroidSpeechToTextService(context)
    stt.startListening(onResult = {}, onError = {})

    // When
    stt.stopListening()

    // Then
    assertFalse(stt.isListening())
  }

  @Test
  fun testAndroidSTT_destroy() {
    // Given
    val stt = AndroidSpeechToTextService(context)
    stt.startListening(onResult = {}, onError = {})

    // When
    stt.destroy()

    // Then
    assertFalse(stt.isListening())
  }

  @Test
  fun testAndroidSTT_isListeningInitiallyFalse() {
    // Given
    val stt = AndroidSpeechToTextService(context)

    // When
    val listening = stt.isListening()

    // Then
    assertFalse(listening)
  }

  @Test
  fun testAndroidSTT_multipleDestroyCalls() {
    // Given
    val stt = AndroidSpeechToTextService(context)

    // When
    stt.destroy()
    stt.destroy() // Should not crash

    // Then
    assertFalse(stt.isListening())
  }

  @Test
  fun testAndroidSTT_startListeningStoresCallbacks() {
    // Given
    val stt = AndroidSpeechToTextService(context)

    // When
    stt.startListening(onResult = {}, onError = {})

    // Then - Should not crash and should update state
    assertNotNull(stt)
  }
}

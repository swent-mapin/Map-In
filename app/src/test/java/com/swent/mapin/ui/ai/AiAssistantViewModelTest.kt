package com.swent.mapin.ui.ai

import android.content.Context
import com.swent.mapin.model.ai.*
import com.swent.mapin.model.event.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

// Assisted by Claude Sonnet 4.5

/**
 * Unit tests for AiAssistantViewModel.
 *
 * These tests use mock implementations of the services to test the ViewModel logic in isolation
 * without requiring real Android services.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAssistantViewModelTest {

  private lateinit var viewModel: AiAssistantViewModel
  private lateinit var mockContext: Context
  private lateinit var mockEventRepository: EventRepository
  private lateinit var mockOrchestrator: AiAssistantOrchestrator
  private lateinit var mockSpeechToTextService: SpeechToTextService
  private lateinit var mockTextToSpeechService: TextToSpeechService

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    mockContext = mock(Context::class.java)
    mockEventRepository = mock(EventRepository::class.java)
    mockOrchestrator = mock(AiAssistantOrchestrator::class.java)
    mockSpeechToTextService = FakeSpeechToTextService()
    mockTextToSpeechService = FakeTextToSpeechService()

    viewModel =
        AiAssistantViewModel(
            context = mockContext,
            eventRepository = mockEventRepository,
            orchestrator = mockOrchestrator,
            speechToTextService = mockSpeechToTextService,
            textToSpeechService = mockTextToSpeechService)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Idle`() {
    assertEquals(AiAssistantState.Idle, viewModel.state.value)
  }

  @Test
  fun `initial conversation is empty`() {
    assertTrue(viewModel.conversationMessages.value.isEmpty())
  }

  @Test
  fun `initial recommended events is empty`() {
    assertTrue(viewModel.recommendedEvents.value.isEmpty())
  }

  @Test
  fun `initial followup questions is empty`() {
    assertTrue(viewModel.followupQuestions.value.isEmpty())
  }

  @Test
  fun `toggleListening starts listening when idle`() {
    viewModel.toggleListening()

    assertEquals(AiAssistantState.Listening, viewModel.state.value)
    assertTrue((mockSpeechToTextService as FakeSpeechToTextService).isListening())
  }

  @Test
  fun `toggleListening stops listening when already listening`() {
    // Start listening first
    viewModel.toggleListening()
    assertEquals(AiAssistantState.Listening, viewModel.state.value)

    // Toggle again to stop
    viewModel.toggleListening()
    assertEquals(AiAssistantState.Idle, viewModel.state.value)
    assertFalse((mockSpeechToTextService as FakeSpeechToTextService).isListening())
  }

  @Test
  fun `handleUserQuery with blank query sets state to Idle`() = runTest {
    viewModel.handleUserQuery("")
    advanceUntilIdle()

    assertEquals(AiAssistantState.Idle, viewModel.state.value)
    assertTrue(viewModel.conversationMessages.value.isEmpty())
  }

  @Test
  fun `handleUserQuery adds user message to conversation`() = runTest {
    val query = "Find me a concert"

    // Setup mock to return a result
    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            AiAssistantResult(
                assistantMessage = "Here are some concerts",
                recommendedEvents = emptyList(),
                followupQuestions = emptyList()))

    viewModel.handleUserQuery(query)
    advanceUntilIdle()

    val messages = viewModel.conversationMessages.value
    assertTrue(messages.isNotEmpty())
    assertEquals(query, messages.first().text)
    assertTrue(messages.first().isFromUser)
  }

  @Test
  fun `selectFollowupQuestion triggers handleUserQuery`() = runTest {
    val followupQuestion = "Tell me more about the first event"

    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            AiAssistantResult(
                assistantMessage = "Sure, here's more info",
                recommendedEvents = emptyList(),
                followupQuestions = emptyList()))

    viewModel.selectFollowupQuestion(followupQuestion)
    advanceUntilIdle()

    val messages = viewModel.conversationMessages.value
    assertTrue(messages.any { it.text == followupQuestion && it.isFromUser })
  }

  @Test
  fun `resetConversation clears all state`() = runTest {
    // First add some messages
    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            AiAssistantResult(
                assistantMessage = "Response",
                recommendedEvents = emptyList(),
                followupQuestions = listOf("Question?")))

    viewModel.handleUserQuery("Test query")
    advanceUntilIdle()

    // Verify state is not empty
    assertFalse(viewModel.conversationMessages.value.isEmpty())

    // Reset
    viewModel.resetConversation()

    // Verify all cleared
    assertTrue(viewModel.conversationMessages.value.isEmpty())
    assertTrue(viewModel.recommendedEvents.value.isEmpty())
    assertTrue(viewModel.followupQuestions.value.isEmpty())
    assertEquals(AiAssistantState.Idle, viewModel.state.value)
  }

  @Test
  fun `resetConversation stops TTS`() {
    val fakeTts = mockTextToSpeechService as FakeTextToSpeechService
    fakeTts.speak("Speaking...") {}

    viewModel.resetConversation()

    assertTrue(fakeTts.wasStopped)
  }

  @Test
  fun `resetConversation calls orchestrator reset`() {
    viewModel.resetConversation()

    verify(mockOrchestrator).resetConversation()
  }

  @Test
  fun `handleUserQuery sets state to Processing`() = runTest {
    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            AiAssistantResult(
                assistantMessage = "Response",
                recommendedEvents = emptyList(),
                followupQuestions = emptyList()))

    viewModel.handleUserQuery("Test")

    // State should be Processing before advanceUntilIdle
    assertEquals(AiAssistantState.Processing, viewModel.state.value)

    advanceUntilIdle()
  }

  @Test
  fun `speech recognition result triggers query processing`() = runTest {
    val recognizedText = "Find me a party tonight"

    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            AiAssistantResult(
                assistantMessage = "Found parties!",
                recommendedEvents = emptyList(),
                followupQuestions = emptyList()))

    // Start listening
    viewModel.toggleListening()

    // Simulate speech recognition result
    val fakeStt = mockSpeechToTextService as FakeSpeechToTextService
    fakeStt.simulateResult(recognizedText)

    advanceUntilIdle()

    // Verify the recognized text was processed
    val messages = viewModel.conversationMessages.value
    assertTrue(messages.any { it.text == recognizedText && it.isFromUser })
  }

  @Test
  fun `speech recognition error sets error state`() {
    val errorMessage = "No speech detected"

    // Start listening
    viewModel.toggleListening()

    // Simulate error
    val fakeStt = mockSpeechToTextService as FakeSpeechToTextService
    fakeStt.simulateError(errorMessage)

    val state = viewModel.state.value
    assertTrue(state is AiAssistantState.Error)
    assertEquals(errorMessage, (state as AiAssistantState.Error).message)
  }

  @Test
  fun `conversation messages have correct timestamps`() = runTest {
    val beforeTime = System.currentTimeMillis()

    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            AiAssistantResult(
                assistantMessage = "Response",
                recommendedEvents = emptyList(),
                followupQuestions = emptyList()))

    viewModel.handleUserQuery("Test")
    advanceUntilIdle()

    val afterTime = System.currentTimeMillis()

    val messages = viewModel.conversationMessages.value
    messages.forEach { message ->
      assertTrue(message.timestamp >= beforeTime)
      assertTrue(message.timestamp <= afterTime)
    }
  }

  @Test
  fun `multiple queries accumulate in conversation`() = runTest {
    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            AiAssistantResult(
                assistantMessage = "Response",
                recommendedEvents = emptyList(),
                followupQuestions = emptyList()))

    viewModel.handleUserQuery("First query")
    advanceUntilIdle()

    viewModel.handleUserQuery("Second query")
    advanceUntilIdle()

    val messages = viewModel.conversationMessages.value
    assertEquals(4, messages.size) // 2 user + 2 AI messages
  }

  @Test
  fun `error during processing shows error message`() = runTest {
    whenever(mockOrchestrator.processQuery(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(RuntimeException("Network error"))

    viewModel.handleUserQuery("Test")
    advanceUntilIdle()

    // Should have added an error message to conversation
    val messages = viewModel.conversationMessages.value
    assertTrue(messages.any { it.text.contains("error") && !it.isFromUser })
  }
}

/** Fake implementation of SpeechToTextService for testing. */
class FakeSpeechToTextService : SpeechToTextService {
  private var _isListening = false

  private var onResultCallback: ((String) -> Unit)? = null
  private var onErrorCallback: ((String) -> Unit)? = null

  override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
    _isListening = true
    onResultCallback = onResult
    onErrorCallback = onError
  }

  override fun stopListening() {
    _isListening = false
  }

  override fun isListening(): Boolean = _isListening

  override fun destroy() {
    _isListening = false
    onResultCallback = null
    onErrorCallback = null
  }

  fun simulateResult(text: String) {
    onResultCallback?.invoke(text)
  }

  fun simulateError(error: String) {
    onErrorCallback?.invoke(error)
  }
}

/** Fake implementation of TextToSpeechService for testing. */
class FakeTextToSpeechService : TextToSpeechService {
  var lastSpokenText: String? = null
    private set

  var wasStopped = false
    private set

  private var _isSpeaking = false
  private var onCompleteCallback: (() -> Unit)? = null

  override fun speak(text: String, onComplete: (() -> Unit)?) {
    lastSpokenText = text
    _isSpeaking = true
    onCompleteCallback = onComplete
    // Immediately complete for testing
    onComplete?.invoke()
    _isSpeaking = false
  }

  override fun stop() {
    _isSpeaking = false
    wasStopped = true
  }

  override fun isSpeaking(): Boolean = _isSpeaking

  override fun shutdown() {
    _isSpeaking = false
  }
}

package com.swent.mapin.ui.ai

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.ai.AiAssistantOrchestrator
import com.swent.mapin.model.ai.AiAssistantResult
import com.swent.mapin.model.ai.AiRecommendedEvent
import com.swent.mapin.model.ai.SpeechToTextService
import com.swent.mapin.model.ai.TextToSpeechService
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.location.Location
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Assisted by Claude Sonnet 4.5

@OptIn(ExperimentalCoroutinesApi::class)
class AiAssistantViewModelTest {

  private lateinit var viewModel: AiAssistantViewModel
  private lateinit var mockContext: Context
  private lateinit var mockEventRepository: EventRepository
  private lateinit var mockOrchestrator: AiAssistantOrchestrator
  private lateinit var mockSpeechToTextService: SpeechToTextService
  private lateinit var mockTextToSpeechService: TextToSpeechService
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  private val testDispatcher = UnconfinedTestDispatcher()
  private val testUserId = "test-user-123"

  private val testEvent =
      Event(
          uid = "event-1",
          title = "Test Event",
          description = "A test event",
          date = Timestamp.now(),
          location = Location.from("Test Location", 46.5, 6.6),
          tags = listOf("music"),
          public = true,
          ownerId = "owner-1")

  private val testEvent2 =
      Event(
          uid = "event-2",
          title = "Second Event",
          description = "Another test event",
          date = Timestamp.now(),
          location = Location.from("Another Location", 46.6, 6.7),
          tags = listOf("sport"),
          public = true,
          ownerId = "owner-2")

  private val testAiResult =
      AiAssistantResult(
          assistantMessage = "I found some events for you!",
          recommendedEvents =
              listOf(
                  AiRecommendedEvent(id = "event-1", reason = "Great music event"),
                  AiRecommendedEvent(id = "event-2", reason = "Fun sports activity")),
          followupQuestions = listOf("Want more music events?", "Interested in sports?"))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Mock FirebaseAuth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    // Mock dependencies
    mockContext = mockk(relaxed = true)
    mockEventRepository = mockk(relaxed = true)
    mockOrchestrator = mockk(relaxed = true)
    mockSpeechToTextService = mockk(relaxed = true)
    mockTextToSpeechService = mockk(relaxed = true)

    // Default mock behaviors
    every { mockSpeechToTextService.stopListening() } just Runs
    every { mockSpeechToTextService.destroy() } just Runs
    every { mockTextToSpeechService.stop() } just Runs
    every { mockTextToSpeechService.shutdown() } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  private fun createViewModel(): AiAssistantViewModel {
    return AiAssistantViewModel(
        context = mockContext,
        eventRepository = mockEventRepository,
        orchestrator = mockOrchestrator,
        speechToTextService = mockSpeechToTextService,
        textToSpeechService = mockTextToSpeechService)
  }

  // ==================== Initial State Tests ====================

  @Test
  fun `initial state is Idle with empty collections`() = runTest {
    viewModel = createViewModel()

    assertEquals(AiAssistantState.Idle, viewModel.state.value)
    assertTrue(viewModel.conversationMessages.value.isEmpty())
    assertTrue(viewModel.recommendedEvents.value.isEmpty())
    assertTrue(viewModel.followupQuestions.value.isEmpty())
  }

  // ==================== Toggle Listening Tests ====================

  @Test
  fun `toggleListening starts and stops listening`() = runTest {
    viewModel = createViewModel()

    viewModel.toggleListening()

    assertEquals(AiAssistantState.Listening, viewModel.state.value)

    viewModel.toggleListening()

    assertEquals(AiAssistantState.Idle, viewModel.state.value)
  }

  @Test
  fun `toggleListening from Error starts listening`() = runTest {
    viewModel = createViewModel()
    // Set error state by simulating speech recognition error
    val onErrorSlot = slot<(String) -> Unit>()
    every { mockSpeechToTextService.startListening(any(), capture(onErrorSlot)) } answers
        {
          onErrorSlot.captured.invoke("Test error")
        }

    viewModel.toggleListening()
    // Now state is Error, toggle again
    viewModel.toggleListening()

    verify(exactly = 2) { mockSpeechToTextService.startListening(any(), any()) }
  }

  @Test
  fun `toggleListening from Listening stops listening`() = runTest {
    viewModel = createViewModel()

    // Start listening
    viewModel.toggleListening()
    assertEquals(AiAssistantState.Listening, viewModel.state.value)

    // Stop listening
    viewModel.toggleListening()

    verify { mockSpeechToTextService.stopListening() }
    assertEquals(AiAssistantState.Idle, viewModel.state.value)
  }

  @Test
  fun `toggleListening from Speaking stops speech and goes to Idle`() = runTest {
    viewModel = createViewModel()

    // Simulate speaking state by processing a query
    coEvery { mockOrchestrator.processQuery(any(), any(), any()) } returns testAiResult
    coEvery { mockEventRepository.getEvent("event-1") } returns testEvent
    coEvery { mockEventRepository.getEvent("event-2") } returns testEvent2

    val onCompleteSlot = slot<(() -> Unit)?>()
    every { mockTextToSpeechService.speak(any(), captureNullable(onCompleteSlot)) } just Runs

    viewModel.handleUserQuery("Find me events")
    advanceUntilIdle()

    // Should be in Speaking state
    assertEquals(AiAssistantState.Speaking, viewModel.state.value)

    // Toggle to stop speaking
    viewModel.toggleListening()

    verify { mockTextToSpeechService.stop() }
    assertEquals(AiAssistantState.Idle, viewModel.state.value)
  }

  // ==================== Speech Recognition Tests ====================

  @Test
  fun `speech recognition success triggers query processing`() = runTest {
    viewModel = createViewModel()

    val onResultSlot = slot<(String) -> Unit>()
    every { mockSpeechToTextService.startListening(capture(onResultSlot), any()) } answers
        {
          onResultSlot.captured.invoke("Find music events")
        }

    coEvery { mockOrchestrator.processQuery(any(), any(), any()) } returns testAiResult
    coEvery { mockEventRepository.getEvent("event-1") } returns testEvent
    coEvery { mockEventRepository.getEvent("event-2") } returns testEvent2

    viewModel.toggleListening()
    advanceUntilIdle()

    assertEquals(2, viewModel.conversationMessages.value.size)
    assertEquals("Find music events", viewModel.conversationMessages.value[0].text)
    assertTrue(viewModel.conversationMessages.value[0].isFromUser)
  }

  @Test
  fun `speech recognition error sets Error state`() = runTest {
    viewModel = createViewModel()

    val onErrorSlot = slot<(String) -> Unit>()
    every { mockSpeechToTextService.startListening(any(), capture(onErrorSlot)) } answers
        {
          onErrorSlot.captured.invoke("Microphone error")
        }

    viewModel.toggleListening()

    assertTrue(viewModel.state.value is AiAssistantState.Error)
    assertEquals("Microphone error", (viewModel.state.value as AiAssistantState.Error).message)
  }

  // ==================== Handle User Query Tests ====================

  @Test
  fun `handleUserQuery with blank query returns to Idle`() = runTest {
    viewModel = createViewModel()

    viewModel.handleUserQuery("")

    assertEquals(AiAssistantState.Idle, viewModel.state.value)
    assertTrue(viewModel.conversationMessages.value.isEmpty())
  }

  // ==================== Join Event By ID Tests ====================

  @Test
  fun `joinEvent by ID works`() = runTest {
    viewModel = createViewModel()

    coEvery { mockEventRepository.editEventAsUser("event-1", testUserId, join = true) } just Runs

    viewModel.joinEvent("event-1")
    advanceUntilIdle()

    coVerify { mockEventRepository.editEventAsUser("event-1", testUserId, join = true) }
  }

  // ==================== Reset Conversation Tests ====================

  @Test
  fun `resetConversation clears all state`() = runTest {
    viewModel = createViewModel()

    coEvery { mockOrchestrator.processQuery(any(), any(), any()) } returns testAiResult
    coEvery { mockEventRepository.getEvent("event-1") } returns testEvent
    coEvery { mockEventRepository.getEvent("event-2") } returns testEvent2

    viewModel.handleUserQuery("Find events")
    advanceUntilIdle()

    viewModel.resetConversation()

    assertTrue(viewModel.conversationMessages.value.isEmpty())
    assertTrue(viewModel.recommendedEvents.value.isEmpty())
    assertEquals(AiAssistantState.Idle, viewModel.state.value)
    verify { mockOrchestrator.resetConversation() }
  }

  // ==================== Select Followup Question Tests ====================

  @Test
  fun `selectFollowupQuestion processes the question`() = runTest {
    viewModel = createViewModel()

    coEvery { mockOrchestrator.processQuery(any(), any(), any()) } returns testAiResult
    coEvery { mockEventRepository.getEvent("event-1") } returns testEvent
    coEvery { mockEventRepository.getEvent("event-2") } returns testEvent2

    viewModel.selectFollowupQuestion("Want more music events?")
    advanceUntilIdle()

    assertTrue(viewModel.conversationMessages.value.any { it.text == "Want more music events?" })
  }
}

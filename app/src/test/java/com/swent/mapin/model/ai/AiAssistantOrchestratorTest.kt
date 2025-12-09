package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.filters.Filters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AiAssistantOrchestratorTest {

  private lateinit var mockAiRepository: AiAssistantRepository
  private lateinit var mockEventRepository: EventRepository
  private lateinit var mockCandidateSelector: AiEventCandidateSelector
  private lateinit var orchestrator: AiAssistantOrchestrator

  @Before
  fun setup() {
    mockAiRepository = mockk<AiAssistantRepository>()
    mockEventRepository = mockk<EventRepository>()
    mockCandidateSelector = mockk<AiEventCandidateSelector>()

    orchestrator =
        AiAssistantOrchestrator(
            aiRepository = mockAiRepository,
            eventRepository = mockEventRepository,
            candidateSelector = mockCandidateSelector,
            distanceCalculator = null)
  }

  @Test
  fun testProcessQuery_returnsAiResponse() {
    runBlocking {
      // Given
      val userQuery = "Find me music events"
      val mockEvents =
          listOf(Event(uid = "event1", title = "Concert", location = Location("Venue", 0.0, 0.0)))
      val mockCandidates = listOf(AiEventSummary(id = "event1", title = "Concert"))
      val mockResponse =
          AiRecommendationResponse(
              assistantMessage = "I found a great concert for you!",
              recommendedEvents =
                  listOf(AiRecommendedEvent(id = "event1", reason = "Matches your interests")))

      coEvery { mockEventRepository.getFilteredEvents(any<Filters>(), any()) } returns mockEvents
      every { mockCandidateSelector.selectCandidates(any(), any(), any()) } returns mockCandidates
      coEvery { mockAiRepository.recommendEvents(any(), any()) } returns mockResponse

      // When
      val result = orchestrator.processQuery(userQuery)

      // Then
      assertEquals("I found a great concert for you!", result.assistantMessage)
      assertEquals(1, result.recommendedEvents.size)
      assertEquals("event1", result.recommendedEvents[0].id)
    }
  }

  @Test
  fun testJoinRecommendedEventByIndex_joinsCorrectEvent() {
    runBlocking {
      // Given - First make a query to populate results
      val mockEvents =
          listOf(Event(uid = "event1", title = "Test", location = Location("", 0.0, 0.0)))
      val mockCandidates = listOf(AiEventSummary(id = "event1", title = "Test"))
      val mockResponse =
          AiRecommendationResponse(
              assistantMessage = "Test",
              recommendedEvents =
                  listOf(
                      AiRecommendedEvent(id = "event1", reason = "Test"),
                      AiRecommendedEvent(id = "event2", reason = "Test")))

      coEvery { mockEventRepository.getFilteredEvents(any<Filters>(), any()) } returns mockEvents
      every { mockCandidateSelector.selectCandidates(any(), any(), any()) } returns mockCandidates
      coEvery { mockAiRepository.recommendEvents(any(), any()) } returns mockResponse
      coEvery { mockEventRepository.editEventAsUser(any(), any(), any()) } returns Unit

      orchestrator.processQuery("test")

      // When
      val eventId = orchestrator.joinRecommendedEventByIndex(1, "user123")

      // Then
      assertEquals("event2", eventId)
      coVerify { mockEventRepository.editEventAsUser("event2", "user123", true) }
    }
  }

  @Test
  fun testJoinRecommendedEventByIndex_throwsWhenNoQueryMade() {
    // When/Then
    var exceptionThrown = false
    try {
      runBlocking { orchestrator.joinRecommendedEventByIndex(0, "user123") }
    } catch (e: IllegalStateException) {
      exceptionThrown = true
    }
    assertTrue("Expected IllegalStateException", exceptionThrown)
  }

  @Test
  fun testJoinRecommendedEventByIndex_throwsWhenIndexInvalid() {
    // Given
    val mockEvents =
        listOf(Event(uid = "event1", title = "Test", location = Location("", 0.0, 0.0)))
    val mockCandidates = listOf(AiEventSummary(id = "event1", title = "Test"))
    val mockResponse =
        AiRecommendationResponse(
            assistantMessage = "Test",
            recommendedEvents = listOf(AiRecommendedEvent(id = "event1", reason = "Test")))

    coEvery { mockEventRepository.getFilteredEvents(any<Filters>(), any()) } returns mockEvents
    every { mockCandidateSelector.selectCandidates(any(), any(), any()) } returns mockCandidates
    coEvery { mockAiRepository.recommendEvents(any(), any()) } returns mockResponse

    // When/Then
    var exceptionThrown = false
    try {
      runBlocking {
        orchestrator.processQuery("test")
        orchestrator.joinRecommendedEventByIndex(5, "user123")
      }
    } catch (e: IndexOutOfBoundsException) {
      exceptionThrown = true
    }
    assertTrue("Expected IndexOutOfBoundsException", exceptionThrown)
  }

  @Test
  fun testResetConversation_clearsLastResult() {
    runBlocking {
      // Given
      val mockEvents =
          listOf(Event(uid = "event1", title = "Test", location = Location("", 0.0, 0.0)))
      val mockCandidates = listOf(AiEventSummary(id = "event1", title = "Test"))
      val mockResponse =
          AiRecommendationResponse(
              assistantMessage = "Test",
              recommendedEvents = listOf(AiRecommendedEvent(id = "event1", reason = "Test")))

      coEvery { mockEventRepository.getFilteredEvents(any<Filters>(), any()) } returns mockEvents
      every { mockCandidateSelector.selectCandidates(any(), any(), any()) } returns mockCandidates
      coEvery { mockAiRepository.recommendEvents(any(), any()) } returns mockResponse

      orchestrator.processQuery("test")
      assertNotNull(orchestrator.getLastResult())

      // When
      orchestrator.resetConversation()

      // Then
      assertNull(orchestrator.getLastResult())
    }
  }

  @Test
  fun testProcessQuery_passesUserContextCorrectly() {
    runBlocking {
      // Given
      val userLocation = Location("Lausanne", 46.5, 6.6)
      val timeStart = Timestamp.now()
      val timeEnd = Timestamp(timeStart.seconds + 86400, 0) // +1 day

      val mockEvents =
          listOf(Event(uid = "event1", title = "Test", location = Location("", 0.0, 0.0)))
      val mockCandidates = listOf(AiEventSummary(id = "event1", title = "Test"))
      val mockResponse =
          AiRecommendationResponse(assistantMessage = "Test", recommendedEvents = emptyList())

      coEvery { mockEventRepository.getFilteredEvents(any<Filters>(), any()) } returns mockEvents
      every { mockCandidateSelector.selectCandidates(any(), any(), any()) } returns mockCandidates
      coEvery { mockAiRepository.recommendEvents(any(), any()) } returns mockResponse

      // When
      orchestrator.processQuery(
          userQuery = "test",
          userLocation = userLocation,
          timeWindowStart = timeStart,
          timeWindowEnd = timeEnd,
          maxDistanceKm = 10.0)

      // Then
      coVerify {
        mockAiRepository.recommendEvents(
            any(),
            match { request ->
              request.userContext.approxLocation == "Lausanne" &&
                  request.userContext.maxDistanceKm == 10.0 &&
                  request.userContext.timeWindowStart == timeStart &&
                  request.userContext.timeWindowEnd == timeEnd
            })
      }
    }
  }
}

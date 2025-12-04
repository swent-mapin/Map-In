package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAiAssistantRepositoryTest {

  private lateinit var repository: FakeAiAssistantRepository

  @Before
  fun setup() {
    repository = FakeAiAssistantRepository()
  }

  @Test
  fun `recommendEvents returns empty recommendations when no events provided`() = runTest {
    val request =
        AiRecommendationRequest(
            userQuery = "Find me fun events",
            userContext = AiUserContext(approxLocation = "Lausanne"),
            events = emptyList())

    val response = repository.recommendEvents(conversationId = null, request = request)

    assertEquals(0, response.recommendedEvents.size)
    assertTrue(response.assistantMessage.isNotEmpty())
    assertTrue(response.assistantMessage.contains("couldn't find"))
    assertNotNull(response.followupQuestions)
    assertTrue(response.followupQuestions!!.isNotEmpty())
  }

  @Test
  fun `recommendEvents returns one recommendation when one event provided`() = runTest {
    val event =
        AiEventSummary(
            id = "event1",
            title = "Tech Meetup",
            startTime = Timestamp.now(),
            tags = listOf("tech", "networking"))

    val request =
        AiRecommendationRequest(
            userQuery = "Find tech events", userContext = AiUserContext(), events = listOf(event))

    val response = repository.recommendEvents(conversationId = null, request = request)

    assertEquals(1, response.recommendedEvents.size)
    assertEquals("event1", response.recommendedEvents[0].id)
    assertTrue(response.recommendedEvents[0].reason.isNotEmpty())
    assertTrue(response.assistantMessage.contains("Tech Meetup"))
    assertNotNull(response.followupQuestions)
  }

  @Test
  fun `recommendEvents returns two recommendations when multiple events provided`() = runTest {
    val events =
        listOf(
            AiEventSummary(id = "event1", title = "Tech Meetup", startTime = Timestamp.now()),
            AiEventSummary(id = "event2", title = "Music Festival", startTime = Timestamp.now()),
            AiEventSummary(id = "event3", title = "Art Exhibition", startTime = Timestamp.now()))

    val request =
        AiRecommendationRequest(
            userQuery = "What should I do this weekend?",
            userContext = AiUserContext(),
            events = events)

    val response = repository.recommendEvents(conversationId = null, request = request)

    // Should recommend at most 2 events
    assertEquals(2, response.recommendedEvents.size)
    assertEquals("event1", response.recommendedEvents[0].id)
    assertEquals("event2", response.recommendedEvents[1].id)
    assertTrue(response.assistantMessage.contains("Tech Meetup"))
    assertTrue(response.assistantMessage.contains("Music Festival"))
  }

  @Test
  fun `recommendEvents provides different reasons for multiple recommendations`() = runTest {
    val events =
        listOf(
            AiEventSummary(id = "event1", title = "Event One", startTime = Timestamp.now()),
            AiEventSummary(id = "event2", title = "Event Two", startTime = Timestamp.now()))

    val request =
        AiRecommendationRequest(
            userQuery = "Show me events", userContext = AiUserContext(), events = events)

    val response = repository.recommendEvents(conversationId = null, request = request)

    // Verify that reasons are provided and different
    val reason1 = response.recommendedEvents[0].reason
    val reason2 = response.recommendedEvents[1].reason

    assertTrue(reason1.isNotEmpty())
    assertTrue(reason2.isNotEmpty())
    assertTrue(reason1 != reason2) // Different reasons for variety
  }

  @Test
  fun `recommendEvents is deterministic for same input`() = runTest {
    val events =
        listOf(AiEventSummary(id = "event1", title = "Concert", startTime = Timestamp.now()))

    val request =
        AiRecommendationRequest(
            userQuery = "Find events", userContext = AiUserContext(), events = events)

    val response1 = repository.recommendEvents(conversationId = null, request = request)
    val response2 = repository.recommendEvents(conversationId = null, request = request)

    // Should return identical responses for identical inputs
    assertEquals(response1.assistantMessage, response2.assistantMessage)
    assertEquals(response1.recommendedEvents.size, response2.recommendedEvents.size)
    assertEquals(response1.recommendedEvents[0].id, response2.recommendedEvents[0].id)
  }

  @Test
  fun `recommendEvents ignores conversationId in fake implementation`() = runTest {
    val events = listOf(AiEventSummary(id = "event1", title = "Event", startTime = Timestamp.now()))

    val request =
        AiRecommendationRequest(
            userQuery = "Find events", userContext = AiUserContext(), events = events)

    val responseWithConversationId =
        repository.recommendEvents(conversationId = "conv123", request = request)
    val responseWithoutConversationId =
        repository.recommendEvents(conversationId = null, request = request)

    // Fake implementation should behave the same regardless of conversationId
    assertEquals(
        responseWithConversationId.recommendedEvents.size,
        responseWithoutConversationId.recommendedEvents.size)
  }

  @Test
  fun `recommendEvents always provides assistant message`() = runTest {
    val requestWithEvents =
        AiRecommendationRequest(
            userQuery = "Show events",
            userContext = AiUserContext(),
            events =
                listOf(AiEventSummary(id = "e1", title = "Event", startTime = Timestamp.now())))

    val requestWithoutEvents =
        AiRecommendationRequest(
            userQuery = "Show events", userContext = AiUserContext(), events = emptyList())

    val responseWithEvents = repository.recommendEvents(null, requestWithEvents)
    val responseWithoutEvents = repository.recommendEvents(null, requestWithoutEvents)

    assertTrue(responseWithEvents.assistantMessage.isNotEmpty())
    assertTrue(responseWithoutEvents.assistantMessage.isNotEmpty())
  }
}

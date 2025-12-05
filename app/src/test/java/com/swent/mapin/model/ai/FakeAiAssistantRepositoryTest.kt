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

    assertEquals(2, response.recommendedEvents.size)
    assertEquals("event1", response.recommendedEvents[0].id)
    assertEquals("event2", response.recommendedEvents[1].id)
  }

  @Test
  fun `recommendEvents with conversationId is handled`() = runTest {
    val event =
        AiEventSummary(
            id = "event1", title = "Concert", startTime = Timestamp.now(), tags = listOf("music"))

    val request =
        AiRecommendationRequest(
            userQuery = "Find music events", userContext = AiUserContext(), events = listOf(event))

    val response = repository.recommendEvents(conversationId = "conv123", request = request)

    assertEquals(1, response.recommendedEvents.size)
    assertNotNull(response.followupQuestions)
  }
}

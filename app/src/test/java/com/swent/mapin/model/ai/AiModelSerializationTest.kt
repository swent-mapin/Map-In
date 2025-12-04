package com.swent.mapin.model.ai

// Assisted by AI

import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Type
import java.util.Date

/**
 * Tests for JSON serialization/deserialization of AI model classes.
 *
 * These tests ensure that the data classes can be properly serialized to JSON
 * for communication with the backend, and deserialized back to Kotlin objects.
 */
class AiModelSerializationTest {

  private lateinit var gson: Gson

  /**
   * Custom Gson serializer for Firebase Timestamp.
   * Converts Timestamp to seconds since epoch for JSON representation.
   */
  class TimestampSerializer : JsonSerializer<Timestamp> {
    override fun serialize(
        src: Timestamp?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
      return JsonPrimitive(src?.seconds ?: 0)
    }
  }

  /**
   * Custom Gson deserializer for Firebase Timestamp.
   * Converts seconds since epoch back to Timestamp object.
   */
  class TimestampDeserializer : JsonDeserializer<Timestamp> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Timestamp {
      val seconds = json?.asLong ?: 0
      return Timestamp(Date(seconds * 1000))
    }
  }

  @Before
  fun setup() {
    // Create Gson instance with custom Timestamp serializers
    gson = GsonBuilder()
        .registerTypeAdapter(Timestamp::class.java, TimestampSerializer())
        .registerTypeAdapter(Timestamp::class.java, TimestampDeserializer())
        .create()
  }

  @Test
  fun `AiUserContext serializes and deserializes correctly`() {
    val original = AiUserContext(
        approxLocation = "Lausanne, Switzerland",
        maxDistanceKm = 10.0,
        timeWindowStart = Timestamp(Date(1700000000000)),
        timeWindowEnd = Timestamp(Date(1700100000000))
    )

    val json = gson.toJson(original)
    val deserialized = gson.fromJson(json, AiUserContext::class.java)

    assertEquals(original.approxLocation, deserialized.approxLocation)
    assertEquals(original.maxDistanceKm, deserialized.maxDistanceKm)
    assertNotNull(deserialized.timeWindowStart)
    assertNotNull(deserialized.timeWindowEnd)
  }

  @Test
  fun `AiUserContext with null fields serializes correctly`() {
    val original = AiUserContext(
        approxLocation = null,
        maxDistanceKm = null,
        timeWindowStart = null,
        timeWindowEnd = null
    )

    val json = gson.toJson(original)
    val deserialized = gson.fromJson(json, AiUserContext::class.java)

    assertEquals(original.approxLocation, deserialized.approxLocation)
    assertEquals(original.maxDistanceKm, deserialized.maxDistanceKm)
    assertEquals(original.timeWindowStart, deserialized.timeWindowStart)
    assertEquals(original.timeWindowEnd, deserialized.timeWindowEnd)
  }

  @Test
  fun `AiEventSummary serializes and deserializes correctly`() {
    val original = AiEventSummary(
        id = "event123",
        title = "Tech Conference",
        startTime = Timestamp(Date(1700000000000)),
        endTime = Timestamp(Date(1700100000000)),
        tags = listOf("tech", "networking"),
        distanceKm = 5.5,
        locationDescription = "EPFL Campus",
        capacityRemaining = 50,
        price = 25.0
    )

    val json = gson.toJson(original)
    val deserialized = gson.fromJson(json, AiEventSummary::class.java)

    assertEquals(original.id, deserialized.id)
    assertEquals(original.title, deserialized.title)
    assertEquals(original.tags, deserialized.tags)
    assertEquals(original.distanceKm, deserialized.distanceKm)
    assertEquals(original.locationDescription, deserialized.locationDescription)
    assertEquals(original.capacityRemaining, deserialized.capacityRemaining)
    assertEquals(original.price, deserialized.price)
  }

  @Test
  fun `AiRecommendationRequest serializes and deserializes correctly`() {
    val userContext = AiUserContext(approxLocation = "Geneva")
    val event1 = AiEventSummary(
        id = "e1",
        title = "Event One",
        tags = listOf("music")
    )
    val event2 = AiEventSummary(
        id = "e2",
        title = "Event Two",
        tags = listOf("sports")
    )

    val original = AiRecommendationRequest(
        userQuery = "Find fun events this weekend",
        userContext = userContext,
        events = listOf(event1, event2)
    )

    val json = gson.toJson(original)
    val deserialized = gson.fromJson(json, AiRecommendationRequest::class.java)

    assertEquals(original.userQuery, deserialized.userQuery)
    assertEquals(original.userContext.approxLocation, deserialized.userContext.approxLocation)
    assertEquals(original.events.size, deserialized.events.size)
    assertEquals(original.events[0].id, deserialized.events[0].id)
    assertEquals(original.events[1].title, deserialized.events[1].title)
  }

  @Test
  fun `AiRecommendedEvent serializes and deserializes correctly`() {
    val original = AiRecommendedEvent(
        id = "event456",
        reason = "Matches your interests in technology"
    )

    val json = gson.toJson(original)
    val deserialized = gson.fromJson(json, AiRecommendedEvent::class.java)

    assertEquals(original.id, deserialized.id)
    assertEquals(original.reason, deserialized.reason)
  }

  @Test
  fun `AiRecommendationResponse serializes and deserializes correctly`() {
    val recommendedEvents = listOf(
        AiRecommendedEvent(id = "e1", reason = "Great for tech enthusiasts"),
        AiRecommendedEvent(id = "e2", reason = "Popular in your area")
    )

    val original = AiRecommendationResponse(
        assistantMessage = "I found 2 events that match your interests!",
        recommendedEvents = recommendedEvents,
        followupQuestions = listOf("Want more details?", "Looking for similar events?")
    )

    val json = gson.toJson(original)
    val deserialized = gson.fromJson(json, AiRecommendationResponse::class.java)

    assertEquals(original.assistantMessage, deserialized.assistantMessage)
    assertEquals(original.recommendedEvents.size, deserialized.recommendedEvents.size)
    assertEquals(original.recommendedEvents[0].id, deserialized.recommendedEvents[0].id)
    assertEquals(original.recommendedEvents[1].reason, deserialized.recommendedEvents[1].reason)
    assertEquals(original.followupQuestions?.size, deserialized.followupQuestions?.size)
    assertEquals(original.followupQuestions?.get(0), deserialized.followupQuestions?.get(0))
  }

  @Test
  fun `AiRecommendationResponse with null followupQuestions serializes correctly`() {
    val original = AiRecommendationResponse(
        assistantMessage = "No events found",
        recommendedEvents = emptyList(),
        followupQuestions = null
    )

    val json = gson.toJson(original)
    val deserialized = gson.fromJson(json, AiRecommendationResponse::class.java)

    assertEquals(original.assistantMessage, deserialized.assistantMessage)
    assertEquals(0, deserialized.recommendedEvents.size)
    assertEquals(original.followupQuestions, deserialized.followupQuestions)
  }

  @Test
  fun `JSON field names are stable and predictable`() {
    // This test verifies that field names in JSON match expectations
    // Important for backend contract stability

    val event = AiEventSummary(
        id = "test",
        title = "Test Event",
        tags = listOf("tag1")
    )

    val json = gson.toJson(event)

    // Verify that JSON contains expected field names
    assert(json.contains("\"id\""))
    assert(json.contains("\"title\""))
    assert(json.contains("\"tags\""))
    assert(json.contains("\"price\""))
  }

  @Test
  fun `Complete round trip preserves data integrity`() {
    // Create a complete request
    val request = AiRecommendationRequest(
        userQuery = "Show me outdoor events",
        userContext = AiUserContext(
            approxLocation = "Zurich",
            maxDistanceKm = 20.0
        ),
        events = listOf(
            AiEventSummary(
                id = "hiking1",
                title = "Mountain Hiking",
                tags = listOf("outdoor", "sports"),
                price = 15.0,
                distanceKm = 8.5
            )
        )
    )

    // Create a response
    val response = AiRecommendationResponse(
        assistantMessage = "Perfect for outdoor enthusiasts!",
        recommendedEvents = listOf(
            AiRecommendedEvent(id = "hiking1", reason = "Great mountain views")
        ),
        followupQuestions = listOf("Need hiking gear recommendations?")
    )

    // Serialize both
    val requestJson = gson.toJson(request)
    val responseJson = gson.toJson(response)

    // Deserialize both
    val deserializedRequest = gson.fromJson(requestJson, AiRecommendationRequest::class.java)
    val deserializedResponse = gson.fromJson(responseJson, AiRecommendationResponse::class.java)

    // Verify request
    assertEquals(request.userQuery, deserializedRequest.userQuery)
    assertEquals(request.events[0].title, deserializedRequest.events[0].title)

    // Verify response
    assertEquals(response.assistantMessage, deserializedResponse.assistantMessage)
    assertEquals(response.recommendedEvents[0].id, deserializedResponse.recommendedEvents[0].id)
  }
}


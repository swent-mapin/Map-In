package com.swent.mapin.model

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Test writing and documentation assisted by AI tools

/**
 * Unit tests for the [NominatimLocationRepository] class.
 *
 * These tests validate the behavior of location search and parsing logic, including caching, error
 * handling, rate limiting, and JSON parsing.
 *
 * Test coverage includes:
 * - Parsing valid JSON arrays into Location objects
 * - Enforcing rate limits between API calls
 * - Handling successful and failed HTTP responses
 * - Using cached results for repeated queries
 * - Skipping invalid location entries
 * - Throwing exceptions for null or failed responses
 *
 * Mocks are used to simulate OkHttp client behavior and isolate repository logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NominatimLocationRepositoryTests {

  @Test
  fun `jsonArrayToLocations parses valid JSON correctly`() {
    val arr = JSONArray()

    val obj1 = JSONObject()
    obj1.put("display_name", "Lausanne, Switzerland")
    obj1.put("lat", "46.5191")
    obj1.put("lon", "6.6336")

    val obj2 = JSONObject()
    obj2.put("display_name", "Geneva, Switzerland")
    obj2.put("lat", "46.2044")
    obj2.put("lon", "6.1432")

    arr.put(obj1)
    arr.put(obj2)

    val repo = NominatimLocationRepository(client = OkHttpClient())
    val method = repo.javaClass.getDeclaredMethod("jsonArrayToLocations", JSONArray::class.java)
    method.isAccessible = true
    val result = method.invoke(repo, arr) as List<Location>

    assertEquals(2, result.size)
    assertEquals("Lausanne, Switzerland", result[0].name)
    assertEquals(46.5191, result[0].latitude, 0.0001)
    assertEquals(6.6336, result[0].longitude, 0.0001)
  }

  @Test
  fun `RateLimiter delays if called too soon`() = runBlocking {
    val limiter = RateLimiter(1000)
    limiter.acquire() // First call, no delay

    val elapsed = measureTimeMillis {
      limiter.acquire() // Should delay ~1000ms
    }

    assertTrue("Expected delay of ~1000ms, got $elapsed ms", elapsed >= 990)
  }

  @Test
  fun `search returns parsed locations from mocked response`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
            [
                {
                    "display_name": "Zurich, Switzerland",
                    "lat": "47.3769",
                    "lon": "8.5417"
                }
            ]
        """
            .trimIndent()

    val requestSlot = slot<Request>()

    every { mockClient.newCall(capture(requestSlot)) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockResponse.close() } returns Unit
    every { mockBody.string() } returnsMany listOf(json, json)

    val repo = NominatimLocationRepository(mockClient)
    val result = repo.search("Zurich")

    assertEquals(1, result.size)
    assertEquals("Zurich, Switzerland", result[0].name)
    assertEquals(47.3769, result[0].latitude, 0.0001)
    assertEquals(8.5417, result[0].longitude, 0.0001)
  }

  @Test
  fun `search uses cache on repeated query`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
            [
                {
                    "display_name": "Bern, Switzerland",
                    "lat": "46.9481",
                    "lon": "7.4474"
                }
            ]
        """
            .trimIndent()

    val requestSlot = slot<Request>()

    every { mockClient.newCall(capture(requestSlot)) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockResponse.close() } returns Unit
    every { mockBody.string() } returns json

    val repo = NominatimLocationRepository(mockClient)

    val result1 = repo.search("Bern")
    assertEquals(1, result1.size)
    assertEquals("Bern, Switzerland", result1[0].name)

    val result2 = repo.search("Bern")
    assertEquals(1, result2.size)
    assertEquals("Bern, Switzerland", result2[0].name)

    // Verify that newCall was only invoked once
    verify(exactly = 1) { mockClient.newCall(any()) }
  }

  @Test(expected = LocationSearchException::class)
  fun `search throws when response body is null`() =
      runTest(timeout = 20.seconds) {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val mockResponse = mockk<Response>()

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns null
        every { mockResponse.body?.string() } returns null
        every { mockResponse.close() } returns Unit

        val repo = NominatimLocationRepository(mockClient)
        repo.search("BodyIsNull")
      }

  @Test(expected = LocationSearchException::class)
  fun `search throws when response is not successful`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns false
    every { mockResponse.code } returns 500
    every { mockResponse.close() } returns Unit

    val repo = NominatimLocationRepository(mockClient)
    repo.search("InvalidQuery")
  }

  @Test(expected = LocationSearchException::class)
  fun `search throws when exception is thrown`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } throws IOException("Network failure")

    val repo = NominatimLocationRepository(mockClient)

    repo.search("ExceptionQuery")
  }

  @Test
  fun `search skips invalid location entries`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
        [
            {
                "display_name": "",
                "lat": "47.3769",
                "lon": "8.5417"
            },
            {
                "display_name": "Zurich, Switzerland",
                "lat": "47.3769",
                "lon": "8.5417"
            },
            {
                "display_name": "Missing lat",
                "lon": "8.5417"
            },
            {
                "display_name": "Missing lon",
                "lat": "47.3769"
            }
        ]
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockResponse.close() } returns Unit
    every { mockBody.string() } returns json

    val repo = NominatimLocationRepository(mockClient)
    val result = repo.search("Zurich")

    assertEquals(1, result.size)
    assertEquals("Zurich, Switzerland", result[0].name)
  }
}

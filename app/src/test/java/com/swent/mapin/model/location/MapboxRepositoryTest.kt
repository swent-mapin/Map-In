package com.swent.mapin.model.location

import com.swent.mapin.model.Location
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Test suite for [MapboxRepository].
 *
 * Uses MockK to mock HTTP requests and responses.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapboxRepositoryTest {

  @Test
  fun `forwardGeocode returns locations from successful API response`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": [
          {
            "place_name": "Paris, France",
            "center": [2.3522, 48.8566]
          },
          {
            "place_name": "Paris, Texas, USA",
            "center": [-95.5555, 33.6609]
          }
        ]
      }
    """
            .trimIndent()

    val requestSlot = slot<Request>()
    every { mockClient.newCall(capture(requestSlot)) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val results = repo.forwardGeocode("Paris")

    assertEquals(2, results.size)

    // First result - Paris, France
    assertEquals("Paris, France", results[0].name)
    assertEquals(48.8566, results[0].latitude, 0.0001)
    assertEquals(2.3522, results[0].longitude, 0.0001)
    // Second result - Paris, Texas
    assertEquals("Paris, Texas, USA", results[1].name)
    assertEquals(33.6609, results[1].latitude, 0.0001)
    assertEquals(-95.5555, results[1].longitude, 0.0001)

    // Verify URL contains correct parameters
    val request = requestSlot.captured
    assertTrue(request.url.toString().contains("access_token=test_token"))
    assertTrue(request.url.toString().contains("limit=5"))
    assertTrue(request.url.toString().contains("autocomplete=true"))
    assertTrue(request.url.toString().contains("country=ch"))
  }

  @Test
  fun `forwardGeocode returns empty list for empty features`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": []
      }
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val results = repo.forwardGeocode("NoResults")

    assertEquals(emptyList<Location>(), results)
  }

  @Test
  fun `forwardGeocode uses cache on repeated query`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": [
          {
            "place_name": "Lausanne, Switzerland",
            "center": [6.6336, 46.5197]
          }
        ]
      }
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")

    val result1 = repo.forwardGeocode("Lausanne")
    val result2 = repo.forwardGeocode("lausanne") // Different case
    val result3 = repo.forwardGeocode("  Lausanne  ") // With spaces

    // All should return cached result
    assertEquals(1, result1.size)
    assertEquals("Lausanne, Switzerland", result1[0].name)
    assertEquals(result1, result2)
    assertEquals(result1, result3)

    // Only one HTTP call should have been made
    verify(exactly = 1) { mockClient.newCall(any()) }
  }

  @Test(expected = LocationSearchException::class)
  fun `forwardGeocode throws when response body is null`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns null
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    repo.forwardGeocode("NullBody")
  }

  @Test(expected = LocationSearchException::class)
  fun `forwardGeocode throws when response is not successful`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns false
    every { mockResponse.code } returns 401
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    repo.forwardGeocode("Unauthorized")
  }

  @Test(expected = LocationSearchException::class)
  fun `forwardGeocode throws when network exception occurs`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } throws IOException("Network failure")

    val repo = MapboxRepository(mockClient, "test_token")
    repo.forwardGeocode("NetworkError")
  }

  @Test
  fun `forwardGeocode skips features with invalid or missing coordinates`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": [
          {
            "place_name": "No Center"
          },
          {
            "place_name": "Valid Location",
            "center": [6.6336, 46.5197]
          },
          {
            "place_name": "Invalid Center",
            "center": [6.6336]
          }
        ]
      }
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val results = repo.forwardGeocode("PartialData")

    // Only the valid location should be returned
    assertEquals(1, results.size)
    assertEquals("Valid Location", results[0].name)
  }

  @Test
  fun `forwardGeocode handles malformed JSON gracefully`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": null
      }
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val results = repo.forwardGeocode("NullFeatures")

    assertEquals(emptyList<Location>(), results)
  }

  @Test
  fun `reverseGeocode returns location from successful API response`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": [
          {
            "place_name": "Eiffel Tower, Paris, France",
            "center": [2.2945, 48.8584]
          }
        ]
      }
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val result = repo.reverseGeocode(48.8584, 2.2945)

    assertNotNull(result)
    assertEquals("Eiffel Tower, Paris, France", result!!.name)
    assertEquals(48.8584, result.latitude, 0.0001)
    assertEquals(2.2945, result.longitude, 0.0001)
  }

  @Test
  fun `reverseGeocode returns null when no features found`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": []
      }
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val result = repo.reverseGeocode(0.0, 0.0)

    assertNull(result)
  }

  @Test
  fun `reverseGeocode returns null when response is not successful`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns false
    every { mockResponse.code } returns 404
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val result = repo.reverseGeocode(48.8584, 2.2945)

    assertNull(result)
  }

  @Test(expected = LocationSearchException::class)
  fun `reverseGeocode throws when network exception occurs`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } throws IOException("Network failure")

    val repo = MapboxRepository(mockClient, "test_token")
    repo.reverseGeocode(48.8584, 2.2945)
  }

  @Test
  fun `reverseGeocode with extreme coordinates`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
      {
        "features": [
          {
            "place_name": "North Pole",
            "center": [0.0, 90.0]
          }
        ]
      }
    """
            .trimIndent()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockBody.string() } returns json
    every { mockResponse.close() } returns Unit

    val repo = MapboxRepository(mockClient, "test_token")
    val result = repo.reverseGeocode(90.0, 0.0)

    assertNotNull(result)
    assertEquals("North Pole", result!!.name)
    assertEquals(90.0, result.latitude, 0.0001)
  }
}

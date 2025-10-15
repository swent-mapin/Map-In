package com.swent.mapin.model

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

// Test writing and documentation assisted by AI tools

/**
 * Test suite for [NominatimReverseGeocoder].
 *
 * This class validates reverse geocoding functionality, ensuring correct parsing of JSON, proper
 * handling of HTTP responses, exception handling for invalid or missing data, and unsupported
 * operation verification for forward geocoding.
 *
 * MockK is used for mocking HTTP interactions, and kotlinx.coroutines.test.runTest is used for
 * coroutine-based testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NominatimReverseGeocoderTests {

  @Test
  fun `jsonObjectToLocation parses valid JSON correctly`() {
    val obj =
        JSONObject().apply {
          put("display_name", "Lausanne, Switzerland")
          put("lat", "46.5191")
          put("lon", "6.6336")
        }

    val repo = NominatimReverseGeocoder(client = OkHttpClient())

    val method = repo.javaClass.getDeclaredMethod("jsonObjectToLocation", JSONObject::class.java)
    method.isAccessible = true
    val result = method.invoke(repo, obj) as Location

    assertEquals("Lausanne, Switzerland", result.name)
    assertEquals(46.5191, result.latitude, 0.0001)
    assertEquals(6.6336, result.longitude, 0.0001)
  }

  @Test
  fun `reverseGeocode returns parsed location from mocked response`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()
    val mockBody = mockk<ResponseBody>()

    val json =
        """
            {
                "display_name": "Zurich, Switzerland",
                "lat": "47.3769",
                "lon": "8.5417"
            }
        """
            .trimIndent()

    val requestSlot = slot<Request>()
    every { mockClient.newCall(capture(requestSlot)) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns mockBody
    every { mockResponse.close() } returns Unit
    every { mockBody.string() } returns json

    val repo = NominatimReverseGeocoder(mockClient)
    val result = repo.reverseGeocode(47.3769, 8.5417)

    assertNotNull(result)
    assertEquals("Zurich, Switzerland", result!!.name)
    assertEquals(47.3769, result.latitude, 0.0001)
    assertEquals(8.5417, result.longitude, 0.0001)
  }

  @Test(expected = LocationSearchException::class)
  fun `reverseGeocode throws when response body is null`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body } returns null
    every { mockResponse.close() } returns Unit

    val repo = NominatimReverseGeocoder(mockClient)
    repo.reverseGeocode(46.5191, 6.6336)
  }

  @Test(expected = LocationSearchException::class)
  fun `reverseGeocode throws when response is not successful`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()
    val mockResponse = mockk<Response>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.isSuccessful } returns false
    every { mockResponse.code } returns 500
    every { mockResponse.close() } returns Unit

    val repo = NominatimReverseGeocoder(mockClient)
    repo.reverseGeocode(46.5191, 6.6336)
  }

  @Test(expected = LocationSearchException::class)
  fun `reverseGeocode throws when exception is thrown`() = runTest {
    val mockClient = mockk<OkHttpClient>()
    val mockCall = mockk<Call>()

    every { mockClient.newCall(any()) } returns mockCall
    every { mockCall.execute() } throws IOException("Network failure")

    val repo = NominatimReverseGeocoder(mockClient)
    repo.reverseGeocode(46.5191, 6.6336)
  }

  @Test
  fun `jsonObjectToLocation throws when lat is missing`() {
    val obj =
        JSONObject().apply {
          put("display_name", "Invalid Location - Missing Lat")
          put("lon", "6.6336")
          // lat intentionally missing
        }

    val repo = NominatimReverseGeocoder(client = OkHttpClient())
    val method = repo.javaClass.getDeclaredMethod("jsonObjectToLocation", JSONObject::class.java)
    method.isAccessible = true

    try {
      method.invoke(repo, obj)
      assert(false) { "Expected JSONException to be thrown" }
    } catch (e: InvocationTargetException) {
      assert(e.targetException is JSONException)
    }
  }

  @Test
  fun `jsonObjectToLocation throws when lon is missing`() {
    val obj =
        JSONObject().apply {
          put("display_name", "Invalid Location - Missing Lon")
          put("lat", "46.5191")
          // lon intentionally missing
        }

    val repo = NominatimReverseGeocoder(client = OkHttpClient())
    val method = repo.javaClass.getDeclaredMethod("jsonObjectToLocation", JSONObject::class.java)
    method.isAccessible = true

    try {
      method.invoke(repo, obj)
      assert(false) { "Expected JSONException to be thrown" }
    } catch (e: InvocationTargetException) {
      assert(e.targetException is JSONException)
    }
  }

  @Test(expected = UnsupportedOperationException::class)
  fun `forwardGeocode throws UnsupportedOperationException`() = runTest {
    val repo = NominatimReverseGeocoder(client = OkHttpClient())
    repo.forwardGeocode("Lausanne")
  }
}

package com.swent.mapin.ui.map.directions

import com.mapbox.geojson.Point
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class MapboxDirectionsServiceTest {

  private lateinit var mockClient: OkHttpClient
  private lateinit var mockCall: Call
  private lateinit var service: MapboxDirectionsService

  private val testAccessToken = "test_token_123"
  private val origin = Point.fromLngLat(6.5674, 46.5197)
  private val destination = Point.fromLngLat(6.6335, 46.5197)

  @Before
  fun setUp() {
    mockClient = mock(OkHttpClient::class.java)
    mockCall = mock(Call::class.java)
    service = MapboxDirectionsService(testAccessToken)

    val clientField = MapboxDirectionsService::class.java.getDeclaredField("client")
    clientField.isAccessible = true
    clientField.set(service, mockClient)
  }

  @Test
  fun `getDirections returns route points on successful API response`() = runTest {
    val jsonResponse =
        """
        {
          "routes": [{
            "geometry": {
              "coordinates": [
                [6.5674, 46.5197],
                [6.6000, 46.5197],
                [6.6335, 46.5197]
              ],
              "type": "LineString"
            }
          }],
          "code": "Ok"
        }
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNotNull(result)
    assertEquals(3, result?.size)
    assertEquals(6.5674, result?.get(0)?.longitude() ?: 0.0, 0.0001)
    assertEquals(46.5197, result?.get(0)?.latitude() ?: 0.0, 0.0001)
    assertEquals(6.6335, result?.get(2)?.longitude() ?: 0.0, 0.0001)
    verify(mockClient).newCall(any())
  }

  @Test
  fun `getDirections returns null on API error response`() = runTest {
    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNull(result)
  }

  @Test
  fun `getDirections returns null when no routes in response`() = runTest {
    val jsonResponse =
        """
        {
          "routes": [],
          "code": "NoRoute"
        }
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNull(result)
  }

  @Test
  fun `getDirections returns null on network exception`() = runTest {
    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenThrow(RuntimeException("Network error"))

    val result = service.getDirections(origin, destination)

    assertNull(result)
  }

  @Test
  fun `getDirections returns null when response body is null`() = runTest {
    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNull(result)
  }

  @Test
  fun `getDirections returns null on malformed JSON`() = runTest {
    val jsonResponse = "{ invalid json }"

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNull(result)
  }

  @Test
  fun `getDirections handles multiple coordinate points correctly`() = runTest {
    val jsonResponse =
        """
        {
          "routes": [{
            "geometry": {
              "coordinates": [
                [6.5674, 46.5197],
                [6.5800, 46.5200],
                [6.5900, 46.5195],
                [6.6100, 46.5198],
                [6.6335, 46.5197]
              ],
              "type": "LineString"
            }
          }]
        }
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNotNull(result)
    assertEquals(5, result?.size)
    assertEquals(6.5800, result?.get(1)?.longitude() ?: 0.0, 0.0001)
    assertEquals(46.5200, result?.get(1)?.latitude() ?: 0.0, 0.0001)
  }

  @Test
  fun `getDirections constructs correct API URL`() = runTest {
    val jsonResponse =
        """
        {
          "routes": [{
            "geometry": {
              "coordinates": [[6.5674, 46.5197], [6.6335, 46.5197]]
            }
          }]
        }
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    service.getDirections(origin, destination)

    verify(mockClient).newCall(any())
  }

  @Test
  fun `getDirections returns null when geometry is missing`() = runTest {
    val jsonResponse =
        """
        {
          "routes": [{
            "duration": 123.4
          }]
        }
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNull(result)
  }

  @Test
  fun `getDirections returns null when coordinates array is missing`() = runTest {
    val jsonResponse =
        """
        {
          "routes": [{
            "geometry": {
              "type": "LineString"
            }
          }]
        }
        """
            .trimIndent()

    val mockResponse =
        Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody())
            .build()

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
    `when`(mockCall.execute()).thenReturn(mockResponse)

    val result = service.getDirections(origin, destination)

    assertNull(result)
  }
}

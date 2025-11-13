package com.swent.mapin.ui.map.directions

import android.content.Context
import com.swent.mapin.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ApiKeyProviderTest {

  private lateinit var mockContext: Context

  @Before
  fun setUp() {
    mockContext = mock(Context::class.java)
  }

  @Test
  fun `getMapboxAccessToken returns token from resources`() {
    val testToken = "pk.test.mapbox.token.123456"

    `when`(mockContext.getString(R.string.mapbox_access_token)).thenReturn(testToken)

    val result = ApiKeyProvider.getMapboxAccessToken(mockContext)

    assertEquals(testToken, result)
  }

  @Test
  fun `getMapboxAccessToken returns empty string on exception`() {
    `when`(mockContext.getString(R.string.mapbox_access_token))
        .thenThrow(RuntimeException("Resource error"))

    val result = ApiKeyProvider.getMapboxAccessToken(mockContext)

    assertEquals("", result)
  }

  @Test
  fun `getMapboxAccessToken handles actual mapbox token format`() {
    val testToken = "pk.eyJ1IjoidGVzdHVzZXIiLCJhIjoiY2x6eHh4eHh4eHh4eHgifQ.xxxxxxxxxxxxxxxxxxxxxx"

    `when`(mockContext.getString(R.string.mapbox_access_token)).thenReturn(testToken)

    val result = ApiKeyProvider.getMapboxAccessToken(mockContext)

    assertEquals(testToken, result)
  }

  @Test
  fun `getMapboxAccessToken returns token with special characters`() {
    val testToken = "pk.eyJ1IjoidGVzdCIsImEiOiJjbHl6MTIzNDU2Nzg5In0.ABC-123_xyz"

    `when`(mockContext.getString(R.string.mapbox_access_token)).thenReturn(testToken)

    val result = ApiKeyProvider.getMapboxAccessToken(mockContext)

    assertEquals(testToken, result)
  }

  @Test
  fun `getMapboxAccessToken returns empty token if stored`() {
    `when`(mockContext.getString(R.string.mapbox_access_token)).thenReturn("")

    val result = ApiKeyProvider.getMapboxAccessToken(mockContext)

    assertEquals("", result)
  }

  @Test
  fun `getMapboxAccessToken handles Resources NotFoundException`() {
    `when`(mockContext.getString(R.string.mapbox_access_token))
        .thenThrow(android.content.res.Resources.NotFoundException())

    val result = ApiKeyProvider.getMapboxAccessToken(mockContext)

    assertEquals("", result)
  }

  @Test
  fun `getMapboxAccessToken called with correct resource id`() {
    val testToken = "pk.test.token"
    `when`(mockContext.getString(R.string.mapbox_access_token)).thenReturn(testToken)

    val result = ApiKeyProvider.getMapboxAccessToken(mockContext)

    assertEquals(testToken, result)
    org.mockito.kotlin.verify(mockContext).getString(R.string.mapbox_access_token)
  }
}

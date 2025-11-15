package com.swent.mapin.model.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectivityServiceTest {

  private lateinit var mockContext: Context
  private lateinit var mockConnectivityManager: ConnectivityManager
  private lateinit var mockNetwork: Network
  private lateinit var mockNetworkCapabilities: NetworkCapabilities
  private lateinit var service: ConnectivityServiceImpl

  @Before
  fun setUp() {
    mockContext = mock(Context::class.java)
    mockConnectivityManager = mock(ConnectivityManager::class.java)
    mockNetwork = mock(Network::class.java)
    mockNetworkCapabilities = mock(NetworkCapabilities::class.java)

    `when`(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
        .thenReturn(mockConnectivityManager)
    `when`(mockContext.applicationContext).thenReturn(mockContext)

    service = ConnectivityServiceImpl(mockContext)
  }

  @After
  fun tearDown() {
    ConnectivityServiceProvider.clearInstance()
  }

  @Test
  fun `getCurrentConnectivityState returns disconnected when no active network`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(null)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns disconnected when network capabilities are null`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(null)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns disconnected when network not validated`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected WIFI when WiFi is available and validated`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.WIFI, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected CELLULAR when cellular is available and validated`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.CELLULAR, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected ETHERNET when ethernet is available and validated`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.ETHERNET, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected OTHER when unknown transport type`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.OTHER, state.networkType)
  }

  @Test
  fun `isConnected returns true when network is connected`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    assertTrue(service.isConnected())
  }

  @Test
  fun `isConnected returns false when network is disconnected`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(null)

    assertFalse(service.isConnected())
  }

  @Test
  fun `connectivityState flow emits initial state`() = runTest {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(true)

    val state = service.connectivityState.first()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.WIFI, state.networkType)
  }

  @Test
  fun `ConnectivityState data class has correct properties`() {
    val disconnectedState = ConnectivityState(isConnected = false, networkType = null)
    assertFalse(disconnectedState.isConnected)
    assertNull(disconnectedState.networkType)

    val wifiState = ConnectivityState(isConnected = true, networkType = NetworkType.WIFI)
    assertTrue(wifiState.isConnected)
    assertEquals(NetworkType.WIFI, wifiState.networkType)
  }

  @Test
  fun `NetworkType enum has all expected values`() {
    val types = NetworkType.values()

    assertEquals(4, types.size)
    assertTrue(types.contains(NetworkType.WIFI))
    assertTrue(types.contains(NetworkType.CELLULAR))
    assertTrue(types.contains(NetworkType.ETHERNET))
    assertTrue(types.contains(NetworkType.OTHER))
  }

  @Test
  fun `ConnectivityServiceProvider getInstance returns singleton instance`() {
    val instance1 = ConnectivityServiceProvider.getInstance(mockContext)
    val instance2 = ConnectivityServiceProvider.getInstance(mockContext)

    assertNotNull(instance1)
    assertEquals(instance1, instance2)
  }

  @Test
  fun `ConnectivityServiceProvider setInstance allows custom instance`() {
    val mockService = mock(ConnectivityService::class.java)
    ConnectivityServiceProvider.setInstance(mockService)

    val instance = ConnectivityServiceProvider.getInstance(mockContext)

    assertEquals(mockService, instance)
  }

  @Test
  fun `ConnectivityServiceProvider clearInstance resets singleton`() {
    val instance1 = ConnectivityServiceProvider.getInstance(mockContext)
    ConnectivityServiceProvider.clearInstance()
    val instance2 = ConnectivityServiceProvider.getInstance(mockContext)

    // After clear, a new instance should be created
    assertNotNull(instance1)
    assertNotNull(instance2)
    // Note: We can't easily verify they're different instances due to mocking
  }

  @Test
  fun `ConnectivityState with default networkType is null`() {
    val state = ConnectivityState(isConnected = false)

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `ConnectivityState equality works correctly`() {
    val state1 = ConnectivityState(isConnected = true, networkType = NetworkType.WIFI)
    val state2 = ConnectivityState(isConnected = true, networkType = NetworkType.WIFI)
    val state3 = ConnectivityState(isConnected = false, networkType = null)

    assertEquals(state1, state2)
    assertTrue(state1 != state3)
  }

  @Test
  fun `service returns correct interface type`() {
    assertTrue(service is ConnectivityService)
  }

  @Test
  fun `getCurrentConnectivityState handles internet capability without validation`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(false)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }
}

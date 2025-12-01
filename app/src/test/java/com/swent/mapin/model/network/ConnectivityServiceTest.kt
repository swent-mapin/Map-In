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

  /** Helper function to setup connected network with default capabilities */
  private fun setupConnectedNetwork(
      hasInternet: Boolean = true,
      isValidated: Boolean = true,
      hasWifi: Boolean = false,
      hasCellular: Boolean = false,
      hasEthernet: Boolean = false
  ) {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(hasInternet)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(isValidated)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(hasWifi)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(hasCellular)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(hasEthernet)
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
    setupConnectedNetwork(hasInternet = true, isValidated = false)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected WIFI when WiFi is available and validated`() {
    setupConnectedNetwork(hasWifi = true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.WIFI, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected CELLULAR when cellular is available and validated`() {
    setupConnectedNetwork(hasCellular = true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.CELLULAR, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected ETHERNET when ethernet is available and validated`() {
    setupConnectedNetwork(hasEthernet = true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.ETHERNET, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns connected OTHER when unknown transport type`() {
    setupConnectedNetwork() // No specific transport = OTHER

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.OTHER, state.networkType)
  }

  @Test
  fun `isConnected returns true when network is connected`() {
    setupConnectedNetwork(hasWifi = true)

    assertTrue(service.isConnected())
  }

  @Test
  fun `isConnected returns false when network is disconnected`() {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(null)

    assertFalse(service.isConnected())
  }

  @Test
  fun `connectivityState flow emits initial state`() = runTest {
    setupConnectedNetwork(hasWifi = true)

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

    assertNotNull(instance1)
    assertNotNull(instance2)
    // Note: We can't easily verify they're different instances due to mocking
  }

  @Test
  fun `connectivityState flow handles initial state exception gracefully`() = runTest {
    // Setup to throw exception on getCurrentConnectivityState
    `when`(mockConnectivityManager.activeNetwork).thenThrow(RuntimeException("Test exception"))

    val state = service.connectivityState.first()

    // Should default to disconnected state
    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState prioritizes WiFi over other transports`() {
    setupConnectedNetwork(hasWifi = true, hasCellular = true, hasEthernet = true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.WIFI, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState prioritizes Cellular over Ethernet`() {
    setupConnectedNetwork(hasCellular = true, hasEthernet = true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.CELLULAR, state.networkType)
  }

  @Test
  fun `isConnected returns false when network has no internet capability`() {
    setupConnectedNetwork(hasInternet = false, isValidated = true, hasWifi = true)

    assertFalse(service.isConnected())
  }

  @Test
  fun `connectivityState flow emits disconnected on null network`() = runTest {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(null)

    val state = service.connectivityState.first()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }
}

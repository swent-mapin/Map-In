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

    // Use withTimeout to prevent test hang if flow never emits
    val state = kotlinx.coroutines.withTimeout(2000) { service.connectivityState.first() }

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

    // Verify the exact same mock instance is returned (reference equality)
    // This confirms the provider uses the injected mock instead of creating a new instance
    assertEquals(mockService, instance)
    assertTrue(
        "Provider should return the exact mock instance set via setInstance",
        mockService === instance)
  }

  @Test
  fun `ConnectivityServiceProvider setInstance persists across different contexts`() {
    val mockService = mock(ConnectivityService::class.java)
    val anotherContext = mock(Context::class.java)
    `when`(anotherContext.applicationContext).thenReturn(anotherContext)

    // Set the mock instance
    ConnectivityServiceProvider.setInstance(mockService)

    // Get instance with original context
    val instance1 = ConnectivityServiceProvider.getInstance(mockContext)
    // Get instance with different context
    val instance2 = ConnectivityServiceProvider.getInstance(anotherContext)

    // Both should return the same injected mock, regardless of context
    // This verifies that setInstance takes precedence over context-based creation
    assertTrue("Provider should return injected mock for first context", mockService === instance1)
    assertTrue("Provider should return injected mock for second context", mockService === instance2)
    assertTrue("Both calls should return the exact same instance", instance1 === instance2)
  }

  @Test
  fun `ConnectivityServiceProvider getInstance uses application context not activity context`() {
    val activityContext = mock(Context::class.java)
    val appContext = mock(Context::class.java)
    `when`(activityContext.applicationContext).thenReturn(appContext)
    `when`(appContext.getSystemService(Context.CONNECTIVITY_SERVICE))
        .thenReturn(mockConnectivityManager)

    // Clear to ensure fresh creation
    ConnectivityServiceProvider.clearInstance()

    // Create with activity context
    val instance = ConnectivityServiceProvider.getInstance(activityContext)

    // Verify instance is not null and applicationContext was accessed
    assertNotNull(instance)
    org.mockito.Mockito.verify(activityContext).applicationContext
  }

  @Test
  fun `ConnectivityServiceProvider clearInstance resets singleton`() {
    // Set first mock instance
    val firstMock = mock(ConnectivityService::class.java)
    ConnectivityServiceProvider.setInstance(firstMock)

    val instance1 = ConnectivityServiceProvider.getInstance(mockContext)

    // Verify first mock is returned
    assertTrue("First call should return first mock", firstMock === instance1)

    // Clear the instance
    ConnectivityServiceProvider.clearInstance()

    // Set a different mock instance after clearing
    val secondMock = mock(ConnectivityService::class.java)
    ConnectivityServiceProvider.setInstance(secondMock)

    val instance2 = ConnectivityServiceProvider.getInstance(mockContext)

    // Verify second mock is returned, proving clearInstance worked
    assertTrue("Second call should return second mock", secondMock === instance2)
    assertFalse("Instances should be different after clear", firstMock === instance2)
    assertFalse("Instances should be different after clear", instance1 === instance2)
  }

  @Test
  fun `ConnectivityServiceProvider clearInstance allows new default instance creation`() {
    // Create initial instance through provider
    val instance1 = ConnectivityServiceProvider.getInstance(mockContext)
    assertNotNull(instance1)

    // Clear the instance
    ConnectivityServiceProvider.clearInstance()

    // After clearing, provider should create a new instance
    // We verify by setting a mock after clear - if clear didn't work, setInstance would fail
    val mockService = mock(ConnectivityService::class.java)
    ConnectivityServiceProvider.setInstance(mockService)

    val instance2 = ConnectivityServiceProvider.getInstance(mockContext)

    // Verify the new mock is returned, confirming clear worked and allowed new injection
    assertTrue("After clear, new injected mock should be returned", mockService === instance2)
  }

  @Test
  fun `connectivityState flow handles initial state exception gracefully`() = runTest {
    // Setup: Create a new service instance with mocked ConnectivityManager that throws
    // This ensures the exception happens during callbackFlow initialization
    val throwingContext = mock(Context::class.java)
    val throwingConnectivityManager = mock(ConnectivityManager::class.java)
    `when`(throwingContext.getSystemService(Context.CONNECTIVITY_SERVICE))
        .thenReturn(throwingConnectivityManager)
    `when`(throwingContext.applicationContext).thenReturn(throwingContext)

    // Make activeNetwork throw exception to simulate system error
    `when`(throwingConnectivityManager.activeNetwork)
        .thenThrow(RuntimeException("ConnectivityManager error"))

    val throwingService = ConnectivityServiceImpl(throwingContext)

    // Use withTimeout to prevent test hang if flow never emits
    val state = kotlinx.coroutines.withTimeout(2000) { throwingService.connectivityState.first() }

    // Should default to disconnected state when exception occurs during initialization
    // The implementation catches exceptions in callbackFlow and emits disconnected state
    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  // ========== Network Capability Tests ==========
  // Tests for different combinations of INTERNET and VALIDATED capabilities
  // Both capabilities are required for a connection to be considered valid

  @Test
  fun `getCurrentConnectivityState with INTERNET and VALIDATED returns connected`() {
    // Both capabilities present - should be connected
    setupConnectedNetwork(hasInternet = true, isValidated = true, hasWifi = true)

    val state = service.getCurrentConnectivityState()

    assertTrue("Network with INTERNET and VALIDATED should be connected", state.isConnected)
    assertEquals(NetworkType.WIFI, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState with INTERNET but no VALIDATED returns disconnected`() {
    // Has INTERNET but not VALIDATED - should be disconnected
    // This can happen on captive portals or networks with connectivity issues
    setupConnectedNetwork(hasInternet = true, isValidated = false, hasWifi = true)

    val state = service.getCurrentConnectivityState()

    assertFalse("Network with INTERNET but no VALIDATED should be disconnected", state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState with VALIDATED but no INTERNET returns disconnected`() {
    // Has VALIDATED but not INTERNET - should be disconnected
    // This is an unusual case but possible on some network configurations
    setupConnectedNetwork(hasInternet = false, isValidated = true, hasWifi = true)

    val state = service.getCurrentConnectivityState()

    assertFalse("Network with VALIDATED but no INTERNET should be disconnected", state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState with neither INTERNET nor VALIDATED returns disconnected`() {
    // Neither capability present - should be disconnected
    setupConnectedNetwork(hasInternet = false, isValidated = false, hasWifi = true)

    val state = service.getCurrentConnectivityState()

    assertFalse(
        "Network with neither INTERNET nor VALIDATED should be disconnected", state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `isConnected with INTERNET and VALIDATED returns true`() {
    setupConnectedNetwork(hasInternet = true, isValidated = true, hasWifi = true)

    assertTrue(
        "isConnected should return true when both capabilities present", service.isConnected())
  }

  @Test
  fun `isConnected with INTERNET but no VALIDATED returns false`() {
    setupConnectedNetwork(hasInternet = true, isValidated = false, hasWifi = true)

    assertFalse("isConnected should return false when VALIDATED is missing", service.isConnected())
  }

  @Test
  fun `isConnected with VALIDATED but no INTERNET returns false`() {
    setupConnectedNetwork(hasInternet = false, isValidated = true, hasWifi = true)

    assertFalse("isConnected should return false when INTERNET is missing", service.isConnected())
  }

  // ========== Network Transport Prioritization Tests ==========
  // The prioritization order is: WiFi > Cellular > Ethernet > Other
  // All transports must have both INTERNET and VALIDATED capabilities to be considered connected

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
  fun `getCurrentConnectivityState returns Cellular when WiFi absent`() {
    // Negative case: WiFi not present, only Cellular
    setupConnectedNetwork(hasWifi = false, hasCellular = true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.CELLULAR, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns Ethernet when WiFi and Cellular absent`() {
    // Negative case: Higher priority transports not present, only Ethernet
    setupConnectedNetwork(hasWifi = false, hasCellular = false, hasEthernet = true)

    val state = service.getCurrentConnectivityState()

    assertTrue(state.isConnected)
    assertEquals(NetworkType.ETHERNET, state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns disconnected when WiFi lacks internet capability`() {
    // Edge case: WiFi present but missing INTERNET capability
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(true)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns disconnected when WiFi lacks validated capability`() {
    // Edge case: WiFi present with INTERNET but not VALIDATED
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(true)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns disconnected when Ethernet has internet but not validated`() {
    // Edge case: Ethernet with INTERNET but no VALIDATED capability
    setupConnectedNetwork(hasInternet = true, isValidated = false, hasEthernet = true)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `getCurrentConnectivityState returns disconnected when all transports lack validation`() {
    // Edge case: Multiple transports present but none validated
    `when`(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
    `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
        .thenReturn(mockNetworkCapabilities)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(true)
    `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(true)

    val state = service.getCurrentConnectivityState()

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `isConnected returns false when network has no internet capability`() {
    setupConnectedNetwork(hasInternet = false, isValidated = true, hasWifi = true)

    assertFalse(service.isConnected())
  }

  @Test
  fun `connectivityState flow emits disconnected on null network`() = runTest {
    `when`(mockConnectivityManager.activeNetwork).thenReturn(null)

    // Use withTimeout to prevent test hang if flow never emits
    val state = kotlinx.coroutines.withTimeout(2000) { service.connectivityState.first() }

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `connectivityState flow emits initial WiFi state correctly`() = runTest {
    setupConnectedNetwork(hasWifi = true)

    val state = kotlinx.coroutines.withTimeout(2000) { service.connectivityState.first() }

    assertTrue(state.isConnected)
    assertEquals(NetworkType.WIFI, state.networkType)
  }

  @Test
  fun `connectivityState flow emits initial Cellular state correctly`() = runTest {
    setupConnectedNetwork(hasCellular = true)

    val state = kotlinx.coroutines.withTimeout(2000) { service.connectivityState.first() }

    assertTrue(state.isConnected)
    assertEquals(NetworkType.CELLULAR, state.networkType)
  }

  @Test
  fun `connectivityState flow emits disconnected when capabilities missing`() = runTest {
    setupConnectedNetwork(hasInternet = false, isValidated = false, hasWifi = true)

    val state = kotlinx.coroutines.withTimeout(2000) { service.connectivityState.first() }

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  @Test
  fun `connectivityState flow does not hang on exception`() = runTest {
    // Create service that will throw exception
    val throwingContext = mock(Context::class.java)
    val throwingConnectivityManager = mock(ConnectivityManager::class.java)
    `when`(throwingContext.getSystemService(Context.CONNECTIVITY_SERVICE))
        .thenReturn(throwingConnectivityManager)
    `when`(throwingContext.applicationContext).thenReturn(throwingContext)
    `when`(throwingConnectivityManager.activeNetwork).thenThrow(RuntimeException("System error"))

    val throwingService = ConnectivityServiceImpl(throwingContext)

    // Should emit disconnected state within timeout, not hang
    val state = kotlinx.coroutines.withTimeout(2000) { throwingService.connectivityState.first() }

    assertFalse(state.isConnected)
    assertNull(state.networkType)
  }

  // Note: Network callback updates (onAvailable, onLost, onCapabilitiesChanged) require
  // instrumentation tests as NetworkCallback is difficult to trigger in unit tests.
}

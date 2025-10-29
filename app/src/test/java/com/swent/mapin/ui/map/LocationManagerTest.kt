package com.swent.mapin.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationManagerTest {

  private lateinit var mockContext: Context

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockkStatic(ContextCompat::class)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun denyLocationPermissions() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_DENIED

    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
    } returns PackageManager.PERMISSION_DENIED
  }

  private fun grantLocationPermissions() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED

    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED
  }

  private fun grantFineLocationOnly() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED

    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
    } returns PackageManager.PERMISSION_DENIED
  }

  private fun grantCoarseLocationOnly() {
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_DENIED

    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED
  }

  @Test
  fun locationManager_instantiation_createsSuccessfully() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)

    val hasPermission = manager.hasLocationPermission()
    assertFalse(hasPermission)
  }

  @Test
  fun hasLocationPermission_returnsFalse_withoutPermissions() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)

    val hasPermission = manager.hasLocationPermission()

    assertFalse(hasPermission)
  }

  @Test
  fun getLastKnownLocation_withoutPermission_invokesErrorCallback() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)
    var errorInvoked = false
    var successInvoked = false

    manager.getLastKnownLocation(
        onSuccess = { successInvoked = true }, onError = { errorInvoked = true })

    assertTrue("Error callback should be invoked", errorInvoked)
    assertFalse("Success callback should not be invoked", successInvoked)
  }

  @Test
  fun getLocationUpdates_withoutPermission_closesFlowWithException() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)

    var locationReceived = false

    try {
      kotlinx.coroutines.runBlocking {
        manager.getLocationUpdates().collect { locationReceived = true }
      }
    } catch (_: SecurityException) {} catch (_: Exception) {}

    assertFalse("Should not receive location without permission", locationReceived)
  }

  @Test
  fun hasLocationPermission_withPermissions_returnsTrue() {
    grantLocationPermissions()

    val manager = LocationManager(mockContext)

    assertTrue(manager.hasLocationPermission())
  }

  @Test
  fun hasLocationPermission_withOnlyFineLocation_returnsFalse() {
    grantFineLocationOnly()

    val manager = LocationManager(mockContext)

    assertFalse(manager.hasLocationPermission())
  }

  @Test
  fun getLastKnownLocation_withPermission_canBeCalledWithoutCrash() {
    grantLocationPermissions()

    val manager = LocationManager(mockContext)

    try {
      manager.getLastKnownLocation(onSuccess = {}, onError = {})
      assertTrue("Method should execute without throwing SecurityException", true)
    } catch (_: SecurityException) {
      assertTrue("Should not throw SecurityException with permissions granted", false)
    }
  }

  @Test
  fun hasLocationPermission_withOnlyCoarseLocation_returnsFalse() {
    grantCoarseLocationOnly()

    val manager = LocationManager(mockContext)

    assertFalse(manager.hasLocationPermission())
  }

  @Test
  fun hasLocationPermission_withBothDenied_returnsFalse() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)

    assertFalse(manager.hasLocationPermission())
  }

  @Test
  fun locationManager_multipleInstances_eachHasOwnState() {
    denyLocationPermissions()
    val manager1 = LocationManager(mockContext)
    val manager2 = LocationManager(mockContext)

    assertFalse(manager1.hasLocationPermission())
    assertFalse(manager2.hasLocationPermission())
  }
}

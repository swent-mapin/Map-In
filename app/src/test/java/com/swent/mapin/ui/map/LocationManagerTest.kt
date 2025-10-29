package com.swent.mapin.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LocationManagerTest {

  private lateinit var mockContext: Context
  private lateinit var mockFusedLocationClient: FusedLocationProviderClient
  private lateinit var mockTask: Task<Location>
  private lateinit var mockVoidTask: Task<Void>

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockFusedLocationClient = mockk(relaxed = true)
    mockTask = mockk(relaxed = true)
    mockVoidTask = mockk(relaxed = true)

    mockkStatic(ContextCompat::class)
    mockkStatic("com.google.android.gms.location.LocationServices")
    mockkStatic(Looper::class)

    every { Looper.getMainLooper() } returns mockk(relaxed = true)
    every {
      com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(mockContext)
    } returns mockFusedLocationClient
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

  // ========== hasLocationPermission Tests ==========

  @Test
  fun hasLocationPermission_withBothPermissionsGranted_returnsTrue() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)

    assertTrue(manager.hasLocationPermission())
  }

  @Test
  fun hasLocationPermission_withBothPermissionsDenied_returnsFalse() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)

    assertFalse(manager.hasLocationPermission())
  }

  @Test
  fun hasLocationPermission_withOnlyFineLocationGranted_returnsFalse() {
    grantFineLocationOnly()
    val manager = LocationManager(mockContext)

    assertFalse(manager.hasLocationPermission())
  }

  @Test
  fun hasLocationPermission_withOnlyCoarseLocationGranted_returnsFalse() {
    grantCoarseLocationOnly()
    val manager = LocationManager(mockContext)

    assertFalse(manager.hasLocationPermission())
  }

  // ========== getLastKnownLocation Tests ==========

  @Test
  fun getLastKnownLocation_withoutPermission_invokesErrorCallback() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)
    var errorInvoked = false
    var successInvoked = false

    manager.getLastKnownLocation(
        onSuccess = { successInvoked = true }, onError = { errorInvoked = true })

    assertTrue(errorInvoked)
    assertFalse(successInvoked)
  }

  @Test
  fun getLastKnownLocation_withPermissionAndLocation_invokesSuccessCallback() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)
    val mockLocation = mockk<Location>(relaxed = true)
    var successInvoked = false
    var errorInvoked = false
    var receivedLocation: Location? = null

    every { mockFusedLocationClient.lastLocation } returns mockTask
    every { mockTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<Location>>()
          listener.onSuccess(mockLocation)
          mockTask
        }
    every { mockTask.addOnFailureListener(any()) } returns mockTask

    manager.getLastKnownLocation(
        onSuccess = { location ->
          successInvoked = true
          receivedLocation = location
        },
        onError = { errorInvoked = true })

    assertTrue(successInvoked)
    assertFalse(errorInvoked)
    assertEquals(mockLocation, receivedLocation)
  }

  @Test
  fun getLastKnownLocation_withPermissionButNullLocation_invokesErrorCallback() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)
    var successInvoked = false
    var errorInvoked = false

    every { mockFusedLocationClient.lastLocation } returns mockTask
    every { mockTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<Location>>()
          listener.onSuccess(null)
          mockTask
        }
    every { mockTask.addOnFailureListener(any()) } returns mockTask

    manager.getLastKnownLocation(
        onSuccess = { successInvoked = true }, onError = { errorInvoked = true })

    assertFalse(successInvoked)
    assertTrue(errorInvoked)
  }

  @Test
  fun getLastKnownLocation_withPermissionButTaskFails_invokesErrorCallback() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)
    var successInvoked = false
    var errorInvoked = false

    every { mockFusedLocationClient.lastLocation } returns mockTask
    every { mockTask.addOnSuccessListener(any()) } returns mockTask
    every { mockTask.addOnFailureListener(any()) } answers
        {
          val listener = firstArg<OnFailureListener>()
          listener.onFailure(Exception("Test exception"))
          mockTask
        }

    manager.getLastKnownLocation(
        onSuccess = { successInvoked = true }, onError = { errorInvoked = true })

    assertFalse(successInvoked)
    assertTrue(errorInvoked)
  }

  @Test
  fun getLastKnownLocation_withPermissionAndSecurityException_invokesErrorCallback() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)
    var successInvoked = false
    var errorInvoked = false

    every { mockFusedLocationClient.lastLocation } throws SecurityException("Test exception")

    manager.getLastKnownLocation(
        onSuccess = { successInvoked = true }, onError = { errorInvoked = true })

    assertFalse(successInvoked)
    assertTrue(errorInvoked)
  }

  // ========== getLocationUpdates Tests ==========

  @Test
  fun getLocationUpdates_withoutPermission_closesFlowWithSecurityException() {
    denyLocationPermissions()
    val manager = LocationManager(mockContext)
    var exceptionThrown = false

    runBlocking {
      try {
        manager.getLocationUpdates().collect {}
      } catch (_: SecurityException) {
        exceptionThrown = true
      }
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getLocationUpdates_withPermission_requestsLocationUpdates() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)
    var requestCalled = false

    every {
      mockFusedLocationClient.requestLocationUpdates(any(), any<LocationCallback>(), any())
    } answers
        {
          requestCalled = true
          mockVoidTask
        }

    runBlocking {
      val job = launch { manager.getLocationUpdates().collect {} }

      // Give some time for the flow to start
      kotlinx.coroutines.delay(100)
      job.cancel()
    }

    assertTrue(requestCalled)
  }

  @Test
  fun getLocationUpdates_whenCancelled_removesLocationUpdates() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)
    val capturedCallback = slot<LocationCallback>()
    var removeUpdatesCalled = false

    every {
      mockFusedLocationClient.requestLocationUpdates(any(), capture(capturedCallback), any())
    } returns mockVoidTask

    every { mockFusedLocationClient.removeLocationUpdates(any<LocationCallback>()) } answers
        {
          removeUpdatesCalled = true
          mockVoidTask
        }

    runBlocking {
      val job = launch { manager.getLocationUpdates().collect {} }

      kotlinx.coroutines.delay(100)
      job.cancel()
      kotlinx.coroutines.delay(100)
    }

    assertTrue(removeUpdatesCalled)
  }

  @Test
  fun getLocationUpdates_withSecurityException_closesFlow() {
    grantLocationPermissions()
    val manager = LocationManager(mockContext)

    every {
      mockFusedLocationClient.requestLocationUpdates(any(), any<LocationCallback>(), any())
    } throws SecurityException("Test exception")

    runBlocking {
      var exceptionCaught = false
      try {
        manager.getLocationUpdates().collect {}
      } catch (_: SecurityException) {
        exceptionCaught = true
      }

      assertTrue(exceptionCaught)
    }
  }
}

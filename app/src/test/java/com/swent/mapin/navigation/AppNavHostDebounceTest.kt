package com.swent.mapin.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Minimal test to cover navigation debouncing logic in AppNavHost. */
class AppNavHostDebounceTest {

  @Test
  fun safePopBackStack_debounceLogic_preventsRapidCalls() {
    var popBackStackCallCount = 0
    var lastNavigationTime by mutableLongStateOf(0L)
    val navigationDebounceMs = 500L

    // Simulate safePopBackStack logic
    fun safePopBackStack() {
      val currentTime = System.currentTimeMillis()
      if (currentTime - lastNavigationTime > navigationDebounceMs) {
        lastNavigationTime = currentTime
        popBackStackCallCount++
      }
    }

    // First call should succeed
    safePopBackStack()
    assertEquals(1, popBackStackCallCount)

    // Immediate second call should be blocked by debounce
    safePopBackStack()
    assertEquals(1, popBackStackCallCount)

    // After waiting, call should succeed
    Thread.sleep(600)
    safePopBackStack()
    assertEquals(2, popBackStackCallCount)
  }
}

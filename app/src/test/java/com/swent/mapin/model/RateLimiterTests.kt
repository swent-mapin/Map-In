package com.swent.mapin.model

import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [RateLimiter] class.
 *
 * These tests validate that the rate limiter correctly enforces delays between successive calls.
 *
 * Test coverage includes:
 * - Ensuring that a second call to `acquire` is delayed if made too soon.
 */
class RateLimiterTests {
  @Test
  fun `RateLimiter delays if called too soon`() = runBlocking {
    val limiter = RateLimiter(1000)
    limiter.acquire()

    val elapsed = measureTimeMillis { limiter.acquire() }

    assertTrue("Expected delay of ~1000ms, got $elapsed ms", elapsed >= 990)
  }
}

package com.swent.mapin.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A thread-safe rate limiter that ensures a minimum interval between actions.
 *
 * @property intervalMs The minimum interval in milliseconds between allowed actions.
 */
class RateLimiter(private val intervalMs: Long) {
  private var lastTime = 0L
  private val mutex = Mutex()

  /**
   * Suspends the coroutine if the last action was too recent. Ensures that only one coroutine
   * updates the last time at a time.
   */
  suspend fun acquire() {
    mutex.withLock {
      val now = System.currentTimeMillis()
      val waitTime = intervalMs - (now - lastTime)
      if (waitTime > 0) delay(waitTime)
      lastTime = System.currentTimeMillis()
    }
  }
}

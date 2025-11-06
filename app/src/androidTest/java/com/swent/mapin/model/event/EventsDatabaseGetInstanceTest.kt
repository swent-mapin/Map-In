package com.swent.mapin.model.event

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests `EventsDatabase.getInstance(context)` to ensure the singleton creation and cached return
 * path are exercised (first call constructs the DB, second call returns the same instance).
 */
@RunWith(AndroidJUnit4::class)
class EventsDatabaseGetInstanceTest {
  private lateinit var context: Context
  private var instance: EventsDatabase? = null

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @After
  fun tearDown() {
    // Close the DB if created
    instance?.close()
    // Note: we intentionally do not attempt to null-out the companion INSTANCE to avoid
    // using reflection in tests. Closing the DB is sufficient for cleanup in instrumentation runs.
  }

  @Test
  fun getInstance_returnsSameInstanceOnRepeatedCalls() {
    val first = EventsDatabase.getInstance(context)
    assertNotNull(first)

    val second = EventsDatabase.getInstance(context)
    assertNotNull(second)

    // Should be the same singleton instance
    assertSame(first, second)

    // Keep reference to close in tearDown
    instance = first
  }
}

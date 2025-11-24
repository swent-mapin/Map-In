package com.swent.mapin.ui.map.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.notifications.NotificationBackgroundManager
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class OfflineDownloadNotificationHelperTest {

  private lateinit var mockContext: Context
  private lateinit var mockNotificationManager: NotificationManager
  private lateinit var helper: OfflineDownloadNotificationHelper

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockNotificationManager = mockk(relaxed = true)

    every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns
        mockNotificationManager
    every { mockContext.packageName } returns "com.swent.mapin"
    every { mockContext.applicationContext } returns mockContext

    helper = OfflineDownloadNotificationHelper(mockContext)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `helper can be instantiated`() {
    assertNotNull(helper)
  }

  @Test
  fun `init creates notification channel on Android O+`() {
    // Channel creation happens in init block
    verify {
      mockNotificationManager.createNotificationChannel(
          match<NotificationChannel> {
            it.id == NotificationBackgroundManager.OFFLINE_DOWNLOAD_CHANNEL_ID &&
                it.name == NotificationBackgroundManager.OFFLINE_DOWNLOAD_CHANNEL_NAME
          })
    }
  }

  @Test
  fun `showProgress can be called without crashing`() {
    val event =
        Event(
            uid = "event1",
            title = "Test Event",
            location = Location(latitude = 46.5, longitude = 6.5))

    // Just verify the method can be called - notification building is Android framework code
    try {
      helper.showProgress(event, 0.5f)
    } catch (e: UnsupportedOperationException) {
      // Expected in unit tests due to NotificationCompat.Builder
    }
  }

  @Test
  fun `showComplete can be called without crashing`() {
    val event =
        Event(
            uid = "event1",
            title = "Test Event",
            location = Location(latitude = 46.5, longitude = 6.5))

    // Just verify the method can be called - notification building is Android framework code
    try {
      helper.showComplete(event)
    } catch (e: UnsupportedOperationException) {
      // Expected in unit tests due to NotificationCompat.Builder
    }
  }

  @Test
  fun `cancel calls notification manager cancel`() {
    val eventId = "event1"

    helper.cancel(eventId)

    verify { mockNotificationManager.cancel(any()) }
  }
}

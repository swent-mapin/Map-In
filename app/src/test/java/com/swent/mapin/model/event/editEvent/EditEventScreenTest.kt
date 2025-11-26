package com.swent.mapin.model.event.editEvent

import com.google.firebase.Timestamp
import com.swent.mapin.ui.event.toDateString
import com.swent.mapin.ui.event.toTimeString
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals

class EditEventScreenTest {
  @Test
  fun `toDateString returns correct formatted date`() {
    // Arrange: create a Timestamp for 26 Nov 2025
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.YEAR, 2025)
          set(Calendar.MONTH, Calendar.NOVEMBER) // zero-based, 10 = Nov
          set(Calendar.DAY_OF_MONTH, 26)
          set(Calendar.HOUR_OF_DAY, 14)
          set(Calendar.MINUTE, 30)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val timestamp = Timestamp(calendar.time)

    // Act
    val dateString = timestamp.toDateString()

    // Assert
    assertEquals("26/11/2025", dateString)
  }

  @Test
  fun `toTimeString returns correct formatted time`() {
    // Arrange: create a Timestamp for 14:30
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.YEAR, 2025)
          set(Calendar.MONTH, Calendar.NOVEMBER)
          set(Calendar.DAY_OF_MONTH, 26)
          set(Calendar.HOUR_OF_DAY, 14)
          set(Calendar.MINUTE, 30)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val timestamp = Timestamp(calendar.time)

    // Act
    val timeString = timestamp.toTimeString()

    // Assert
    assertEquals("1430", timeString)
  }

  @Test
  fun `toDateString and toTimeString work with midnight`() {
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.YEAR, 2025)
          set(Calendar.MONTH, Calendar.NOVEMBER)
          set(Calendar.DAY_OF_MONTH, 26)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val timestamp = Timestamp(calendar.time)

    assertEquals("26/11/2025", timestamp.toDateString())
    assertEquals("0000", timestamp.toTimeString())
  }

  @Test
  fun `toTimeString handles single-digit hours and minutes`() {
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.YEAR, 2025)
          set(Calendar.MONTH, Calendar.NOVEMBER)
          set(Calendar.DAY_OF_MONTH, 26)
          set(Calendar.HOUR_OF_DAY, 5)
          set(Calendar.MINUTE, 7)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val timestamp = Timestamp(calendar.time)

    assertEquals("0507", timestamp.toTimeString())
  }
}

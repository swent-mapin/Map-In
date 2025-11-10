package com.swent.mapin.ui.event

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 *
 */

class EventDateFormatHelpersTest {

  private fun ts(year: Int, month0: Int, day: Int, hour: Int, minute: Int = 0): Timestamp {
    // month0: 0-based month
    val cal = GregorianCalendar.getInstance(TimeZone.getDefault())
    cal.clear()
    cal.set(year, month0, day, hour, minute)
    return Timestamp(Date(cal.timeInMillis))
  }

  @Test
  fun `medium single start returns single date time 24h`() {
    val t = ts(2026, 1, 1, 13, 0)
    val out = formatEventDateRangeMedium(t, null)
    // Expect time in 24h format
    assertEquals(true, out.contains("13:00"))
  }

  @Test
  fun `medium same day range returns compact range 24h`() {
    val start = ts(2025, 0, 1, 13, 0)
    val end = ts(2025, 0, 1, 15, 0)
    val out = formatEventDateRangeMedium(start, end)
    assertEquals(true, out.contains("13:00"))
    assertEquals(true, out.contains("15:00"))
    assertEquals(true, out.contains("-"))
  }

  @Test
  fun `medium multi day range shows both dates and times`() {
    val start = ts(2025, 0, 1, 13, 0)
    val end = ts(2025, 0, 2, 15, 30)
    val out = formatEventDateRangeMedium(start, end)
    assertEquals(true, out.contains("13:00"))
    assertEquals(true, out.contains("15:30"))
  }

  @Test
  fun `medium end_equal_start treated as single`() {
    val start = ts(2025, 0, 1, 13, 0)
    val end = ts(2025, 0, 1, 13, 0)
    val out = formatEventDateRangeMedium(start, end)
    // Should not contain a dash range
    assertEquals(false, out.contains("-"))
    assertEquals(true, out.contains("13:00"))
  }

  @Test
  fun `full single start contains weekday and 24h time`() {
    val t = ts(2025, 0, 1, 13, 5)
    val out = formatEventDateRangeFull(t, null)
    assertEquals(true, out.contains("13:05"))
    assertEquals(true, out.contains(",")) // date part
  }

  @Test
  fun `full same day range shows start and end times 24h`() {
    val start = ts(2025, 0, 1, 9, 0)
    val end = ts(2025, 0, 1, 17, 30)
    val out = formatEventDateRangeFull(start, end)
    assertEquals(true, out.contains("09:00"))
    assertEquals(true, out.contains("17:30"))
    assertEquals(true, out.contains("-"))
  }

  @Test
  fun `full multi day range shows both weekdays and times`() {
    val start = ts(2025, 0, 1, 9, 15)
    val end = ts(2025, 0, 3, 18, 45)
    val out = formatEventDateRangeFull(start, end)
    assertEquals(true, out.contains("09:15"))
    assertEquals(true, out.contains("18:45"))
    assertEquals(true, out.contains("-"))
  }
}


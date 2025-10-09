package com.swent.mapin.model

import com.google.firebase.Timestamp
import com.swent.mapin.util.TimeUtils
import java.time.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Test writing and documentation assisted by AI tools
/**
 * Tests for [com.swent.mapin.util.TimeUtils].
 *
 * Validates:
 * - Local date/time -> Firebase Timestamp conversion
 * - Day bounds [startOfDay, startOfNextDay) in a given ZoneId
 */
class TimeUtilsTest {

  @Test
  fun `toTimestamp converts local date+time to same instant`() {
    val date = LocalDate.of(2025, 10, 6)
    val time = LocalTime.of(18, 30)
    val zone = ZoneId.of("Europe/Zurich")

    val expected = ZonedDateTime.of(date, time, zone).toInstant()
    val ts: Timestamp = TimeUtils.toTimestamp(date, time, zone)

    assertEquals(expected.epochSecond, ts.seconds)
    assertEquals(expected.nano, ts.nanoseconds)
  }

  @Test
  fun `dayBounds returns start inclusive and next-day start exclusive`() {
    val date = LocalDate.of(2025, 10, 6)
    val zone = ZoneId.of("Europe/Zurich")

    val (start, end) = TimeUtils.dayBounds(date, zone)

    val startMs = start.seconds * 1000L + start.nanoseconds / 1_000_000L
    val endMs = end.seconds * 1000L + end.nanoseconds / 1_000_000L
    assertTrue(startMs < endMs)

    val expectedStart = date.atStartOfDay(zone).toInstant()
    val expectedEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

    assertEquals(expectedStart.epochSecond, start.seconds)
    assertEquals(expectedStart.nano, start.nanoseconds)
    assertEquals(expectedEnd.epochSecond, end.seconds)
    assertEquals(expectedEnd.nano, end.nanoseconds)
  }
}

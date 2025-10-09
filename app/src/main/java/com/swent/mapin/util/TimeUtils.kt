package com.swent.mapin.util

import com.google.firebase.Timestamp
import java.time.*

object TimeUtils {
  /**
   * Build a Firebase Timestamp from local date & time (uses device timezone by default).
   *
   * @param zone Time zone to use (defaults to system timezone).
   * @param date Local date.
   * @param time Local time.
   * @return Firebase Timestamp representing the same instant in time as the given
   */
  fun toTimestamp(
      date: LocalDate,
      time: LocalTime,
      zone: ZoneId = ZoneId.systemDefault()
  ): Timestamp {
    val instant = ZonedDateTime.of(date, time, zone).toInstant()
    return Timestamp(instant.epochSecond, instant.nano)
  }

  /**
   * Returns [startOfDay, startOfNextDay) as Firebase Timestamps.
   *
   * @param date Local date for which to get the bounds.
   * @param zone Time zone to use (defaults to system timezone).
   * @return Pair of Firebase Timestamps representing the start of the given day (inclusive) and the
   *   start of the next day (exclusive).
   */
  fun dayBounds(
      date: LocalDate,
      zone: ZoneId = ZoneId.systemDefault()
  ): Pair<Timestamp, Timestamp> {
    val start = date.atStartOfDay(zone).toInstant()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant()
    return Timestamp(start.epochSecond, start.nano) to Timestamp(end.epochSecond, end.nano)
  }
}

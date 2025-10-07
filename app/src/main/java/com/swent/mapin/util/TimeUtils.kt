package com.swent.mapin.util

import com.google.firebase.Timestamp
import java.time.*

object TimeUtils {
    /** Build a Firebase Timestamp from local date & time (uses device timezone by default). */
    fun toTimestamp(
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId = ZoneId.systemDefault()
    ): Timestamp {
        val instant = ZonedDateTime.of(date, time, zone).toInstant()
        return Timestamp(instant.epochSecond, instant.nano)
    }

    /** Returns [startOfDay, startOfNextDay) as Firebase Timestamps. */
    fun dayBounds(
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<Timestamp, Timestamp> {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return Timestamp(start.epochSecond, start.nano) to
                Timestamp(end.epochSecond, end.nano)
    }
}

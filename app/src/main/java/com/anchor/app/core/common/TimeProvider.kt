package com.anchor.app.core.common

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Injectable clock so domain logic never calls [System.currentTimeMillis] directly.
 *
 * This is required by the testing strategy (docs/superpowers/design/11-testing-strategy.md §5):
 * seed data and every date-dependent use case must be deterministic.
 */
interface TimeProvider {

    /** Current instant in epoch milliseconds (UTC). */
    fun nowMillis(): Long

    /** The system default time zone, used to compute local day boundaries. */
    fun zoneId(): ZoneId

    /** Current instant. */
    fun instant(): Instant = Instant.ofEpochMilli(nowMillis())

    /** Current zoned date-time in the device's local time zone. */
    fun zonedNow(): ZonedDateTime = ZonedDateTime.ofInstant(instant(), zoneId())

    /** Local date for "today". */
    fun today(): LocalDate = zonedNow().toLocalDate()

    /** Current local time-of-day. */
    fun nowLocalTime(): LocalTime = zonedNow().toLocalTime()

    /**
     * Local epoch-day (number of days since 1970-01-01) for the given instant,
     * interpreted in the device time zone. Used as the key for daily aggregates.
     */
    fun epochDayOf(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zoneId()).toLocalDate().toEpochDay()

    /** Today's local epoch-day. */
    fun todayEpochDay(): Long = today().toEpochDay()

    /** Convert a local date-time to epoch milliseconds. */
    fun toMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(zoneId()).toInstant().toEpochMilli()

    /** Start-of-day (00:00 local) for the given instant, as epoch milliseconds. */
    fun startOfDayMillis(millis: Long): Long =
        Instant.ofEpochMilli(millis)
            .atZone(zoneId())
            .toLocalDate()
            .atStartOfDay(zoneId())
            .toInstant()
            .toEpochMilli()

    /**
     * Start-of-day (00:00 local) for a local epoch-day, as epoch milliseconds. Use this (not
     * `epochDay * 86_400_000`) whenever converting a [LocalDate.toEpochDay] value back to an
     * instant — the naive multiply yields *UTC* midnight and lands on the wrong local day in
     * zones with a negative offset.
     */
    fun startOfDayMillisForEpochDay(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId()).toInstant().toEpochMilli()

    /**
     * Start of the *next* local day (00:00 tomorrow) for an epoch-day, as epoch milliseconds.
     * Deriving the end of day this way (instead of `start + 24h`) keeps day windows correct
     * across DST transitions, where a local day is 23 or 25 hours long.
     */
    fun endOfDayMillisForEpochDay(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay + 1).atStartOfDay(zoneId()).toInstant().toEpochMilli()

    /** Start of the *next* local day (00:00 tomorrow) for the instant [millis]. */
    fun endOfDayMillis(millis: Long): Long =
        endOfDayMillisForEpochDay(epochDayOf(millis))

    /** ISO week key, e.g. `2026-W41` (used as the weekly aggregate primary key). */
    fun isoWeekKey(date: LocalDate = today()): String {
        val weekFields = java.time.temporal.WeekFields.ISO
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())
        return "%d-W%02d".format(year, week)
    }

    /** First day of the current ISO week (Monday). */
    fun startOfWeek(): LocalDate = today().with(DayOfWeek.MONDAY)
}

/** Production implementation backed by the system clock and default time zone. */
class DefaultTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}

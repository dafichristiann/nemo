package com.anchor.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for [TimeProvider] date math (docs 11-testing-strategy.md §5: deterministic
 * time). These guard the day/week boundary logic used everywhere else.
 */
class TimeProviderTest {

    private val utc = ZoneOffset.UTC

    /** 2026-10-07T09:30:00Z (a Wednesday). */
    private val fixedMillis: Long =
        ZonedDateTime.of(2026, 10, 7, 9, 30, 0, 0, utc).toInstant().toEpochMilli()

    private fun provider(millis: Long = fixedMillis, zone: ZoneId = utc) =
        FakeTimeProvider(millis, zone)

    @Test
    fun nowMillis_returnsInjectedValue() {
        assertEquals(fixedMillis, provider().nowMillis())
    }

    @Test
    fun today_matchesInjectedInstant() {
        assertEquals(LocalDate.of(2026, 10, 7), provider().today())
    }

    @Test
    fun epochDayOf_usesDeviceZone() {
        val p = provider()
        val expected = LocalDate.of(2026, 10, 7).toEpochDay()
        assertEquals(expected, p.epochDayOf(fixedMillis))
    }

    @Test
    fun epochDayOf_respectsZoneBoundary() {
        // 2026-10-07T00:30:00+07:00 is still Oct 7 in Jakarta.
        val jakarta = ZoneId.of("Asia/Jakarta")
        val millis = ZonedDateTime.of(2026, 10, 7, 0, 30, 0, 0, jakarta).toInstant().toEpochMilli()
        val expected = LocalDate.of(2026, 10, 7).toEpochDay()
        assertEquals(expected, provider(millis, jakarta).epochDayOf(millis))
    }

    @Test
    fun startOfDayMillis_isMidnightLocal() {
        val p = provider()
        val startOfDay = p.startOfDayMillis(fixedMillis)
        val midnight = ZonedDateTime.of(2026, 10, 7, 0, 0, 0, 0, utc).toInstant().toEpochMilli()
        assertEquals(midnight, startOfDay)
    }

    @Test
    fun isoWeekKey_formatsCorrectly() {
        assertEquals("2026-W41", provider().isoWeekKey())
    }

    @Test
    fun startOfWeek_isMonday() {
        // Oct 7 2026 is a Wednesday; the Monday of that week is Oct 5.
        assertEquals(LocalDate.of(2026, 10, 5), provider().startOfWeek())
    }

    @Test
    fun toMillis_roundTripsLocalDateTime() {
        val p = provider(0L)
        val dt = LocalDateTime.of(2026, 10, 7, 23, 59)
        val millis = p.toMillis(dt)
        val expected = ZonedDateTime.of(dt, utc).toInstant().toEpochMilli()
        assertEquals(expected, millis)
    }

    @Test
    fun advanceBy_movesClockForward() {
        val p = provider()
        p.advanceBy(60_000L)
        assertEquals(fixedMillis + 60_000L, p.nowMillis())
    }
}

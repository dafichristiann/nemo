package com.anchor.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for [DateTimeFormatter] (docs 11-testing-strategy.md §4: date formatting is
 * deterministic and zone-aware). These are pure functions — seeded instants, no clock.
 */
class DateTimeFormatterTest {

    private val utc = ZoneOffset.UTC

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, utc).toInstant().toEpochMilli()

    private val now = at(2026, 10, 7, 9, 30)

    @Test
    fun deadlineLabel_today_showsTodayWithTime() {
        val deadline = at(2026, 10, 7, 23, 59)
        assertEquals("Today 23:59", DateTimeFormatter.deadlineLabel(deadline, now, utc))
    }

    @Test
    fun deadlineLabel_tomorrow_showsTomorrow() {
        val deadline = at(2026, 10, 8, 9, 0)
        assertEquals("Tomorrow 09:00", DateTimeFormatter.deadlineLabel(deadline, now, utc))
    }

    @Test
    fun deadlineLabel_yesterday_showsYesterday() {
        val deadline = at(2026, 10, 6, 18, 5)
        assertEquals("Yesterday 18:05", DateTimeFormatter.deadlineLabel(deadline, now, utc))
    }

    @Test
    fun deadlineLabel_farDate_showsWeekdayMonthDay() {
        val deadline = at(2026, 10, 15, 9, 0)
        assertEquals("Thu Oct 15, 09:00", DateTimeFormatter.deadlineLabel(deadline, now, utc))
    }

    @Test
    fun deadlineLabel_respectsZone() {
        // 2026-10-07 22:30 UTC is already 2026-10-08 00:30 in UTC+2 (next local day).
        val deadline = at(2026, 10, 7, 22, 30)
        val zone = java.time.ZoneId.of("+02:00")
        assertEquals("Tomorrow 00:30", DateTimeFormatter.deadlineLabel(deadline, now, zone))
    }

    @Test
    fun completedTimeLabel_formatsHourAndMinute() {
        assertEquals("Completed 10:32", DateTimeFormatter.completedTimeLabel(at(2026, 10, 7, 10, 32), utc))
    }

    @Test
    fun dateLabel_showsWeekdayMonthDay() {
        assertEquals("Wed Oct 7", DateTimeFormatter.dateLabel(now, utc))
    }

    @Test
    fun durationLabel_handlesHoursMinutesAndNulls() {
        assertEquals("2h est", DateTimeFormatter.durationLabel(120))
        assertEquals("45m est", DateTimeFormatter.durationLabel(45))
        assertEquals("1h 30m est", DateTimeFormatter.durationLabel(90))
        assertNull(DateTimeFormatter.durationLabel(null))
        assertNull(DateTimeFormatter.durationLabel(0))
        assertNull(DateTimeFormatter.durationLabel(-5))
    }
}

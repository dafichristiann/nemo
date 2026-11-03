package com.anchor.app.feature.tasks

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for the reschedule day math (FR-T12). Pure and deterministic: every input is
 * explicit, so no fake clock is needed.
 */
class RescheduleModelTest {

    private val utc = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 7) // a Wednesday

    private fun expected(dayOffset: Long, hour: Int) =
        ZonedDateTime.of(2026, 10, 7, hour, 0, 0, 0, utc)
            .plusDays(dayOffset)
            .toInstant()
            .toEpochMilli()

    @Test
    fun today_keepsSameDate() {
        assertEquals(
            expected(0, 23),
            resolveDeadlineMillis(RescheduleDay.TODAY, 23, today, utc),
        )
    }

    @Test
    fun tomorrow_addsOneDay() {
        assertEquals(
            expected(1, 9),
            resolveDeadlineMillis(RescheduleDay.TOMORROW, 9, today, utc),
        )
    }

    @Test
    fun plus3_addsThreeDays() {
        assertEquals(
            expected(3, 18),
            resolveDeadlineMillis(RescheduleDay.PLUS_3, 18, today, utc),
        )
    }

    @Test
    fun plus7_addsSevenDays() {
        assertEquals(
            expected(7, 12),
            resolveDeadlineMillis(RescheduleDay.PLUS_7, 12, today, utc),
        )
    }

    @Test
    fun crossesMonthBoundary() {
        val endOfMonth = LocalDate.of(2026, 10, 30)
        val millis = resolveDeadlineMillis(RescheduleDay.PLUS_3, 9, endOfMonth, utc)
        val expectedDate = LocalDate.of(2026, 11, 2)
        assertEquals(
            ZonedDateTime.of(expectedDate, java.time.LocalTime.of(9, 0), utc).toInstant().toEpochMilli(),
            millis,
        )
    }

    @Test
    fun hoursSet_isSortedAndDistinct() {
        assertEquals(listOf(9, 12, 18, 23), RESCHEDULE_HOURS.sorted())
    }

    @Test
    fun initialSelection_whenHourIsPastToday_defaultsToTomorrow() {
        val now = java.time.LocalDateTime.of(2026, 10, 7, 15, 0)
        val pastDeadline = expected(0, 9)
        val (day, hour) = initialRescheduleSelection(pastDeadline, today, utc, now)
        assertEquals(RescheduleDay.TOMORROW, day)
        assertEquals(9, hour)
    }

    @Test
    fun initialSelection_whenHourIsInFutureToday_defaultsToToday() {
        val now = java.time.LocalDateTime.of(2026, 10, 7, 15, 0)
        val futureDeadline = expected(0, 18)
        val (day, hour) = initialRescheduleSelection(futureDeadline, today, utc, now)
        assertEquals(RescheduleDay.TODAY, day)
        assertEquals(18, hour)
    }

    @Test
    fun initialSelection_whenNullDeadline_defaultsToToday23() {
        val now = java.time.LocalDateTime.of(2026, 10, 7, 15, 0)
        val (day, hour) = initialRescheduleSelection(null, today, utc, now)
        assertEquals(RescheduleDay.TODAY, day)
        assertEquals(23, hour)
    }

    @Test
    fun initialSelection_whenNullDeadlineAndPast23_defaultsToTomorrow23() {
        val now = java.time.LocalDateTime.of(2026, 10, 7, 23, 30)
        val (day, hour) = initialRescheduleSelection(null, today, utc, now)
        assertEquals(RescheduleDay.TOMORROW, day)
        assertEquals(23, hour)
    }
}

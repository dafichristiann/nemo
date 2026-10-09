package com.anchor.app.core.design.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [formatDurationSeconds] (duration wheel display). Pure JVM. */
class DurationFormatTest {

    @Test
    fun belowOneMinute_padsSeconds() {
        assertEquals("0:05", formatDurationSeconds(5))
        assertEquals("0:59", formatDurationSeconds(59))
    }

    @Test
    fun exactMinute_hasZeroSeconds() {
        assertEquals("1:00", formatDurationSeconds(60))
        assertEquals("10:00", formatDurationSeconds(600))
    }

    @Test
    fun mixedMinutesAndSeconds() {
        assertEquals("1:30", formatDurationSeconds(90))
        assertEquals("25:00", formatDurationSeconds(1500))
    }

    @Test
    fun zeroAndNegative_areClamped() {
        assertEquals("0:00", formatDurationSeconds(0))
        assertEquals("0:00", formatDurationSeconds(-10))
    }
}

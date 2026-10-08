package com.anchor.app.core.domain.usecase.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [isFocusVerified] (PRD v0.5 §10.1 Effort-based Completion).
 * Pure JVM — no Android/Room deps.
 */
class FocusVerificationTest {

    @Test
    fun fullDuration_isVerified() {
        assertTrue(isFocusVerified(actualDurationMs = 25 * 60_000L, plannedDurationMin = 25))
    }

    @Test
    fun longerThanPlanned_isVerified() {
        assertTrue(isFocusVerified(actualDurationMs = 40 * 60_000L, plannedDurationMin = 25))
    }

    @Test
    fun oneMillisecondShort_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 25 * 60_000L - 1, plannedDurationMin = 25))
    }

    @Test
    fun earlyFinish_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 10 * 60_000L, plannedDurationMin = 25))
    }

    @Test
    fun zeroDuration_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 0L, plannedDurationMin = 25))
    }

    @Test
    fun nullPlanned_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 60 * 60_000L, plannedDurationMin = null))
    }

    @Test
    fun zeroPlanned_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 60 * 60_000L, plannedDurationMin = 0))
    }

    @Test
    fun negativePlanned_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 60 * 60_000L, plannedDurationMin = -5))
    }
}

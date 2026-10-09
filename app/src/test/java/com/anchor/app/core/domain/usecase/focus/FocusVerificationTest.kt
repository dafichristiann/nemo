package com.anchor.app.core.domain.usecase.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [isFocusVerified] (PRD v0.5 §10.1 Effort-based Completion).
 * Pure JVM — no Android/Room deps. Durations are in **seconds**.
 */
class FocusVerificationTest {

    @Test
    fun fullDuration_isVerified() {
        assertTrue(isFocusVerified(actualDurationMs = 25 * 60_000L, plannedDurationSec = 25 * 60))
    }

    @Test
    fun longerThanPlanned_isVerified() {
        assertTrue(isFocusVerified(actualDurationMs = 40 * 60_000L, plannedDurationSec = 25 * 60))
    }

    @Test
    fun oneMillisecondShort_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 25 * 60_000L - 1, plannedDurationSec = 25 * 60))
    }

    @Test
    fun earlyFinish_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 10 * 60_000L, plannedDurationSec = 25 * 60))
    }

    @Test
    fun zeroDuration_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 0L, plannedDurationSec = 25 * 60))
    }

    @Test
    fun nullPlanned_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 60 * 60_000L, plannedDurationSec = null))
    }

    @Test
    fun zeroPlanned_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 60 * 60_000L, plannedDurationSec = 0))
    }

    @Test
    fun negativePlanned_isNotVerified() {
        assertFalse(isFocusVerified(actualDurationMs = 60 * 60_000L, plannedDurationSec = -5))
    }

    @Test
    fun shortSecondPreciseSession_isVerified() {
        // 30-second plan completed exactly -> verified (second precision, not minute-rounded).
        assertTrue(isFocusVerified(actualDurationMs = 30_000L, plannedDurationSec = 30))
    }
}

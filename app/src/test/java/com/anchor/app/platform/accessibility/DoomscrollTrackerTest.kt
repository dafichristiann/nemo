package com.anchor.app.platform.accessibility

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DoomscrollTracker] (specs 2026-11-01).
 * Pure JVM test using coroutine test scheduler virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DoomscrollTrackerTest {

    private val targetApps = setOf("com.instagram.android", "com.zhiliaoapp.musically")

    @Test
    fun test_threshold_triggers_alert() = runTest {
        var triggeredPkg: String? = null
        var triggeredMinutes: Int? = null

        val tracker = DoomscrollTracker(
            scope = this,
            clock = { testScheduler.currentTime },
            onThresholdReached = { pkg, mins ->
                triggeredPkg = pkg
                triggeredMinutes = mins
            },
        ).apply {
            updateConfiguration(
                enabled = true,
                thresholdMinutes = 20,
                blockedApps = targetApps,
            )
        }

        tracker.onPackageEvent("com.instagram.android")
        assertTrue(tracker.isTracking)
        assertEquals("com.instagram.android", tracker.currentPackage)
        assertFalse(tracker.isOverlayShowing)

        // Advance 19 minutes: should not trigger yet
        advanceTimeBy(19 * 60 * 1000L)
        runCurrent()
        assertNull(triggeredPkg)
        assertFalse(tracker.isOverlayShowing)

        // Advance 1 more minute (total 20 minutes)
        advanceTimeBy(1 * 60 * 1000L)
        runCurrent()
        assertEquals("com.instagram.android", triggeredPkg)
        assertEquals(20, triggeredMinutes)
        assertTrue(tracker.isOverlayShowing)
    }

    @Test
    fun test_debounce_grace_period_success() = runTest {
        var triggerCount = 0

        val tracker = DoomscrollTracker(
            scope = this,
            clock = { testScheduler.currentTime },
            debounceDelayMs = 10_000L,
            onThresholdReached = { _, _ -> triggerCount++ },
        ).apply {
            updateConfiguration(
                enabled = true,
                thresholdMinutes = 20,
                blockedApps = targetApps,
            )
        }

        // Active on Instagram for 15 minutes
        tracker.onPackageEvent("com.instagram.android")
        advanceTimeBy(15 * 60 * 1000L)
        runCurrent()

        // Switch to WhatsApp (non-target) for 8 seconds (< 10s grace period)
        tracker.onPackageEvent("com.whatsapp")
        assertTrue(tracker.isPaused)
        assertEquals("com.instagram.android", tracker.currentPackage)

        advanceTimeBy(8_000L)
        runCurrent()
        assertTrue(tracker.isTracking)

        // Return to Instagram
        tracker.onPackageEvent("com.instagram.android")
        assertFalse(tracker.isPaused)
        assertEquals(0, triggerCount)

        // Advance remaining 5 minutes
        advanceTimeBy(5 * 60 * 1000L)
        runCurrent()
        assertEquals(1, triggerCount)
        assertTrue(tracker.isOverlayShowing)
    }

    @Test
    fun test_exit_app_resets_session() = runTest {
        var triggerCount = 0

        val tracker = DoomscrollTracker(
            scope = this,
            clock = { testScheduler.currentTime },
            debounceDelayMs = 10_000L,
            onThresholdReached = { _, _ -> triggerCount++ },
        ).apply {
            updateConfiguration(
                enabled = true,
                thresholdMinutes = 20,
                blockedApps = targetApps,
            )
        }

        // Active on TikTok for 12 minutes
        tracker.onPackageEvent("com.zhiliaoapp.musically")
        advanceTimeBy(12 * 60 * 1000L)
        runCurrent()

        // Switch to Launcher for 15 seconds (> 10s debounce timeout)
        tracker.onPackageEvent("com.android.launcher")
        advanceTimeBy(15_000L)
        runCurrent()

        // Sesi harus sudah di-reset
        assertFalse(tracker.isTracking)
        assertNull(tracker.currentPackage)

        // Buka TikTok kembali: sesi baru harus mulai dari 0 menit
        tracker.onPackageEvent("com.zhiliaoapp.musically")
        advanceTimeBy(12 * 60 * 1000L)
        runCurrent()
        // Belum mencapai 20 menit pada sesi baru
        assertEquals(0, triggerCount)

        advanceTimeBy(8 * 60 * 1000L)
        runCurrent()
        // Sekarang mencapai 20 menit pada sesi baru
        assertEquals(1, triggerCount)
    }

    @Test
    fun test_snooze_extends_by_5_minutes() = runTest {
        var triggerCount = 0

        val tracker = DoomscrollTracker(
            scope = this,
            clock = { testScheduler.currentTime },
            snoozeDurationMs = 5 * 60 * 1000L,
            onThresholdReached = { _, _ -> triggerCount++ },
        ).apply {
            updateConfiguration(
                enabled = true,
                thresholdMinutes = 20,
                blockedApps = targetApps,
            )
        }

        tracker.onPackageEvent("com.instagram.android")
        advanceTimeBy(20 * 60 * 1000L)
        runCurrent()
        assertEquals(1, triggerCount)
        assertTrue(tracker.isOverlayShowing)

        // Snooze triggered
        tracker.snooze()
        assertFalse(tracker.isOverlayShowing)

        // Advance 4 minutes: should not re-trigger yet
        advanceTimeBy(4 * 60 * 1000L)
        runCurrent()
        assertEquals(1, triggerCount)

        // Advance 1 more minute (total 5 minutes snooze): should re-trigger
        advanceTimeBy(1 * 60 * 1000L)
        runCurrent()
        assertEquals(2, triggerCount)
        assertTrue(tracker.isOverlayShowing)
    }

    @Test
    fun test_switch_to_different_target_resets_and_starts_fresh() = runTest {
        var triggeredPkg: String? = null

        val tracker = DoomscrollTracker(
            scope = this,
            clock = { testScheduler.currentTime },
            onThresholdReached = { pkg, _ -> triggeredPkg = pkg },
        ).apply {
            updateConfiguration(
                enabled = true,
                thresholdMinutes = 20,
                blockedApps = targetApps,
            )
        }

        // 10 minutes on Instagram
        tracker.onPackageEvent("com.instagram.android")
        advanceTimeBy(10 * 60 * 1000L)
        runCurrent()

        // Switch directly to TikTok
        tracker.onPackageEvent("com.zhiliaoapp.musically")
        assertEquals("com.zhiliaoapp.musically", tracker.currentPackage)

        // 10 more minutes: Instagram had 10, TikTok now 10 (not 20)
        advanceTimeBy(10 * 60 * 1000L)
        runCurrent()
        assertNull(triggeredPkg)

        // 10 more minutes on TikTok (total 20 min on TikTok)
        advanceTimeBy(10 * 60 * 1000L)
        runCurrent()
        assertEquals("com.zhiliaoapp.musically", triggeredPkg)
    }

    @Test
    fun test_disabled_does_not_track() = runTest {
        val tracker = DoomscrollTracker(
            scope = this,
            clock = { testScheduler.currentTime },
        ).apply {
            updateConfiguration(
                enabled = false,
                thresholdMinutes = 20,
                blockedApps = targetApps,
            )
        }

        tracker.onPackageEvent("com.instagram.android")
        assertFalse(tracker.isTracking)
        assertNull(tracker.currentPackage)
    }

    @Test
    fun test_screen_off_pauses_and_resets() = runTest {
        var triggerCount = 0

        val tracker = DoomscrollTracker(
            scope = this,
            clock = { testScheduler.currentTime },
            debounceDelayMs = 10_000L,
            onThresholdReached = { _, _ -> triggerCount++ },
        ).apply {
            updateConfiguration(
                enabled = true,
                thresholdMinutes = 20,
                blockedApps = targetApps,
            )
        }

        // Active on Instagram for 15 minutes
        tracker.onPackageEvent("com.instagram.android")
        advanceTimeBy(15 * 60 * 1000L)
        runCurrent()

        // Screen off event (non-target)
        tracker.onPackageEvent("android.intent.action.SCREEN_OFF")
        assertTrue(tracker.isPaused)

        // Screen stays off for 2 minutes (> 10s debounce timeout)
        advanceTimeBy(2 * 60 * 1000L)
        runCurrent()

        // Sesi harus sudah di-reset
        assertFalse(tracker.isTracking)
        assertNull(tracker.currentPackage)

        // Layar menyala dan kembali ke Instagram: sesi baru mulai dari 0 menit
        tracker.onPackageEvent("com.instagram.android")
        advanceTimeBy(15 * 60 * 1000L)
        runCurrent()
        assertEquals(0, triggerCount)

        // Selesaikan sisa 5 menit hingga 20 menit pada sesi baru
        advanceTimeBy(5 * 60 * 1000L)
        runCurrent()
        assertEquals(1, triggerCount)
        assertTrue(tracker.isOverlayShowing)
    }
}

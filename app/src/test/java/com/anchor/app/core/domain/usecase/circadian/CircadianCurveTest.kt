package com.anchor.app.core.domain.usecase.circadian

import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.usecase.focus.RateFocusSessionUseCase
import com.anchor.app.testutil.FakeFocusRepository
import com.anchor.app.testutil.FakeSettingsRepository
import com.anchor.app.testutil.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class CircadianCurveTest {

    private val zoneId = ZoneOffset.UTC
    // Base time: 2026-11-03 16:00:00 UTC
    private val nowBase: Long =
        ZonedDateTime.of(2026, 11, 3, 16, 0, 0, 0, zoneId).toInstant().toEpochMilli()
    private val timeProvider = FakeTimeProvider(nowBase, zoneId)

    @Test
    fun tc01_detectWakeTime_normalLongestGap_findsWakeTime() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val usageRepo = FakeUsageRepository()
        val detectWakeTime = DetectWakeTimeUseCase(settingsRepo, usageRepo, timeProvider)

        // User slept 05:00 UTC, woke up 12:30 UTC (7.5 hours screen-off gap)
        val sleptAt = ZonedDateTime.of(2026, 11, 3, 5, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        val wokeAt = ZonedDateTime.of(2026, 11, 3, 12, 30, 0, 0, zoneId).toInstant().toEpochMilli()

        usageRepo.insertSessions(
            listOf(
                UsageSession(
                    id = "s1",
                    packageName = "com.app",
                    startTime = sleptAt - 30 * 60 * 1000L,
                    endTime = sleptAt,
                    durationMs = 30 * 60 * 1000L,
                    collectedAt = sleptAt,
                ),
                UsageSession(
                    id = "s2",
                    packageName = "com.app",
                    startTime = wokeAt,
                    endTime = wokeAt + 15 * 60 * 1000L,
                    durationMs = 15 * 60 * 1000L,
                    collectedAt = wokeAt + 15 * 60 * 1000L,
                ),
            )
        )

        val result = detectWakeTime(nowBase, zoneId)
        assertEquals(LocalTime.of(12, 30), result.time)
        assertFalse(result.isManual)
        assertEquals(WakeConfidence.HIGH, result.confidence)
    }

    @Test
    fun tc02_detectWakeTime_allNighter_fallsBackToMedianOfPast3Days() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val usageRepo = FakeUsageRepository()
        val detectWakeTime = DetectWakeTimeUseCase(settingsRepo, usageRepo, timeProvider)

        // Today: sessions every 1 hour, so screen off never >= 4 hours
        val todaySessions = mutableListOf<UsageSession>()
        for (h in 0..15) {
            val t = nowBase - (16 - h) * 3600 * 1000L
            todaySessions.add(
                UsageSession("today_$h", "com.app", t, t + 45 * 60 * 1000L, 45 * 60 * 1000L, t + 45 * 60 * 1000L)
            )
        }
        usageRepo.insertSessions(todaySessions)

        // Day -1: woke at 07:30
        val day1Wake = ZonedDateTime.of(2026, 11, 2, 7, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        val day1Sleep = day1Wake - 6 * 3600 * 1000L
        // Day -2: woke at 08:30
        val day2Wake = ZonedDateTime.of(2026, 11, 1, 8, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        val day2Sleep = day2Wake - 6 * 3600 * 1000L
        // Day -3: woke at 08:00
        val day3Wake = ZonedDateTime.of(2026, 10, 31, 8, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        val day3Sleep = day3Wake - 6 * 3600 * 1000L

        usageRepo.insertSessions(
            listOf(
                UsageSession("d1_1", "com.app", day1Sleep - 3600_000L, day1Sleep, 3600_000L, day1Sleep),
                UsageSession("d1_2", "com.app", day1Wake, day1Wake + 3600_000L, 3600_000L, day1Wake + 3600_000L),
                UsageSession("d2_1", "com.app", day2Sleep - 3600_000L, day2Sleep, 3600_000L, day2Sleep),
                UsageSession("d2_2", "com.app", day2Wake, day2Wake + 3600_000L, 3600_000L, day2Wake + 3600_000L),
                UsageSession("d3_1", "com.app", day3Sleep - 3600_000L, day3Sleep, 3600_000L, day3Sleep),
                UsageSession("d3_2", "com.app", day3Wake, day3Wake + 3600_000L, 3600_000L, day3Wake + 3600_000L),
            )
        )

        val result = detectWakeTime(nowBase, zoneId)
        // Median of 07:30, 08:00, 08:30 is 08:00
        assertEquals(LocalTime.of(8, 0), result.time)
        assertEquals(WakeConfidence.MEDIUM, result.confidence)
        assertFalse(result.isManual)
    }

    @Test
    fun tc03_detectWakeTime_manualOverrideTakesPrecedence() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val usageRepo = FakeUsageRepository()
        val detectWakeTime = DetectWakeTimeUseCase(settingsRepo, usageRepo, timeProvider)

        val todayEpoch = LocalDate.of(2026, 11, 3).toEpochDay()
        // Override to 10:00 (10 * 60 = 600 minutes)
        settingsRepo.setManualWakeTime(todayEpoch, 600)

        val result = detectWakeTime(nowBase, zoneId)
        assertEquals(LocalTime.of(10, 0), result.time)
        assertTrue(result.isManual)
        assertEquals(WakeConfidence.HIGH, result.confidence)
    }

    @Test
    fun tc04_ratingNormalization_linearForFewSamples_andZScoreForMany() {
        // Less than 5 samples -> linear normalization (r - 1) / 4
        assertEquals(0.0f, RateFocusSessionUseCase.normalize(1, listOf(1, 3)), 0.001f)
        assertEquals(0.5f, RateFocusSessionUseCase.normalize(3, listOf(1, 3)), 0.001f)
        assertEquals(1.0f, RateFocusSessionUseCase.normalize(5, listOf(1, 3)), 0.001f)

        // 5 or more samples -> z-score normalization
        val ratings = listOf(4, 5, 4, 5, 4)
        val norm = RateFocusSessionUseCase.normalize(5, ratings)
        assertTrue(norm in 0.0f..1.0f)
    }

    @Test
    fun tc05_rateFocusSession_updatesRatingAndLiftsBucket() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val usageRepo = FakeUsageRepository()
        val focusRepo = FakeFocusRepository()
        val detectWakeTime = DetectWakeTimeUseCase(settingsRepo, usageRepo, timeProvider)
        val computeCurve = ComputeCircadianCurveUseCase(detectWakeTime, focusRepo, timeProvider)
        val rateSession = RateFocusSessionUseCase(focusRepo)

        // Wake at 08:00
        val todayEpoch = LocalDate.of(2026, 11, 3).toEpochDay()
        settingsRepo.setManualWakeTime(todayEpoch, 8 * 60)

        // Prior curve capacity at bucket 1 (T+2..4h = 10:00..12:00) with prior 0.95
        val baseCurve = computeCurve(nowBase, zoneId)
        val baseBucket1 = baseCurve.bucketCapacities[1]
        assertEquals(0.95f, baseBucket1, 0.01f)

        // Record a 25-minute focus session at 10:30 (Bucket 1, since 10:30 - 08:00 = 2.5h)
        val sessionStart = ZonedDateTime.of(2026, 11, 3, 10, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        val session = FocusSession(
            id = "fs_1",
            taskId = "t1",
            mode = FocusMode.GENTLE,
            plannedDurationSec = 25 * 60,
            actualDurationMs = 25 * 60 * 1000L,
            startedAt = sessionStart,
            endedAt = sessionStart + 25 * 60 * 1000L,
            status = FocusStatus.FINISHED,
            blockedApps = emptyList(),
            fallbackReason = null,
            rating = null,
        )
        focusRepo.insert(session)

        // Rate the session 5
        rateSession("fs_1", 5)
        assertEquals(5, focusRepo.getById("fs_1")?.rating)

        // Recompute curve
        val updatedCurve = computeCurve(nowBase, zoneId)
        // With n=1, R_norm=1.0, prior=0.95: (1 * 1.0 + 5 * 0.95) / 6 = 5.75 / 6 ≈ 0.9583
        assertTrue(updatedCurve.bucketCapacities[1] > baseBucket1)
    }

    @Test
    fun tc06_splineBoundary_fritschCarlsonNoOvershoot() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val usageRepo = FakeUsageRepository()
        val focusRepo = FakeFocusRepository()
        val detectWakeTime = DetectWakeTimeUseCase(settingsRepo, usageRepo, timeProvider)
        val computeCurve = ComputeCircadianCurveUseCase(detectWakeTime, focusRepo, timeProvider)

        val curve = computeCurve(nowBase, zoneId)

        // Test every 6 minutes across the entire 24h day
        for (step in 0..240) {
            val hours = step * 0.1f
            val value = curve.evaluate(hours)
            assertTrue("Curve value $value at $hours h should be >= 0.0", value >= 0.0f)
            assertTrue("Curve value $value at $hours h should be <= 1.0", value <= 1.0f)
        }
    }

    @Test
    fun tc07_nowIndicator_positionMatchesHoursFromWake() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val usageRepo = FakeUsageRepository()
        val focusRepo = FakeFocusRepository()
        val detectWakeTime = DetectWakeTimeUseCase(settingsRepo, usageRepo, timeProvider)
        val computeCurve = ComputeCircadianCurveUseCase(detectWakeTime, focusRepo, timeProvider)

        // Wake time 12:30 (750 minutes)
        val todayEpoch = LocalDate.of(2026, 11, 3).toEpochDay()
        settingsRepo.setManualWakeTime(todayEpoch, 12 * 60 + 30)

        // Now is 16:00 (baseNow)
        // Difference = 16:00 - 12:30 = 3h 30m = 3.5 hours
        val curve = computeCurve(nowBase, zoneId)
        val expectedHours = 3.5f

        val evaluatedNow = curve.evaluate(expectedHours)
        assertEquals(curve.currentCapacity, evaluatedNow, 0.001f)
    }

    @Test
    fun tc08_dynamicPeakWindow_andSecondaryWindow_computedCorrectly() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val usageRepo = FakeUsageRepository()
        val focusRepo = FakeFocusRepository()
        val detectWakeTime = DetectWakeTimeUseCase(settingsRepo, usageRepo, timeProvider)
        val computeCurve = ComputeCircadianCurveUseCase(detectWakeTime, focusRepo, timeProvider)

        // Set wake time to 06:30 (390 minutes)
        val todayEpoch = LocalDate.of(2026, 11, 3).toEpochDay()
        settingsRepo.setManualWakeTime(todayEpoch, 6 * 60 + 30)

        val curve = computeCurve(nowBase, zoneId)
        // Peak Start = 06:30 + 2h = 08:30
        assertEquals(LocalTime.of(8, 30), curve.peakStart)
        // Peak End = 06:30 + 4.5h = 11:00
        assertEquals(LocalTime.of(11, 0), curve.peakEnd)
        assertEquals("08:30 - 11:00", curve.peakWindowFormatted())

        // Secondary Peak Start = 06:30 + 8h = 14:30
        assertEquals(LocalTime.of(14, 30), curve.secondaryPeakStart)
        // Secondary Peak End = 06:30 + 10.5h = 17:00
        assertEquals(LocalTime.of(17, 0), curve.secondaryPeakEnd)
        assertEquals("14:30 - 17:00", curve.secondaryPeakWindowFormatted())
    }
}

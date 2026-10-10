package com.anchor.app.core.domain.usecase.usage

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.platform.usagestats.UsageEvent
import com.anchor.app.testutil.FakeUsageRepository
import com.anchor.app.testutil.FakeUsageStatsSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for the usage pipeline: collection, aggregation and pruning
 * (docs 11-testing-strategy.md §2 Smart Todo → Phase 2; §5 deterministic time).
 *
 * Fixed instant: 2026-10-07T23:30:00Z (a Wednesday) for timezone/boundary coverage.
 */
class UsageUseCasesTest {

    private val utc = ZoneOffset.UTC
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 23, 30, 0, 0, utc).toInstant().toEpochMilli()

    private fun millis(
        y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0,
        zone: ZoneId = utc,
    ): Long = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    private fun session(pkg: String, start: Long, end: Long, collectedAt: Long = fixedNow) =
        UsageSession(
            id = "us-$pkg-$start",
            packageName = pkg,
            startTime = start,
            endTime = end,
            durationMs = end - start,
            collectedAt = collectedAt,
        )

    // --- CollectUsageUseCase ---

    @Test
    fun collect_returnsPermissionMissingWhenNotGranted() = runTest {
        val repo = FakeUsageRepository()
        val source = FakeUsageStatsSource(permissionGranted = false)
        val useCase = CollectUsageUseCase(repo, source, FakeIdProvider(), FakeTimeProvider(fixedNow, utc))

        val result = useCase()

        assertTrue(result.permissionMissing)
        assertEquals(0, result.collected)
        assertEquals(0, repo.sessionCount())
    }

    @Test
    fun collect_mapsEventsToSessionsAndStores() = runTest {
        val repo = FakeUsageRepository()
        val source = FakeUsageStatsSource(
            events = listOf(
                UsageEvent("com.tiktok", millis(2026, 10, 7, 20), millis(2026, 10, 7, 20, 30)),
            ),
        )
        val useCase = CollectUsageUseCase(repo, source, FakeIdProvider(), FakeTimeProvider(fixedNow, utc))

        val result = useCase()

        assertFalse(result.permissionMissing)
        assertEquals(1, result.collected)
        val stored = repo.sessions.value.single()
        assertEquals("com.tiktok", stored.packageName)
        assertEquals(30 * 60 * 1000L, stored.durationMs)
    }

    @Test
    fun collect_isIdempotentOnReRun() = runTest {
        val repo = FakeUsageRepository()
        val source = FakeUsageStatsSource(
            events = listOf(UsageEvent("com.a", millis(2026, 10, 7, 10), millis(2026, 10, 7, 11))),
        )
        val useCase = CollectUsageUseCase(repo, source, FakeIdProvider(), FakeTimeProvider(fixedNow, utc))

        useCase()
        useCase()

        // Deterministic session ids (pkg + start) make the second insert a no-op.
        assertEquals(1, repo.sessionCount())
    }

    @Test
    fun collect_secondRunQueriesFromMaxEndTimeNotMaxStartTime() = runTest {
        // Regression: the watermark must be MAX(endTime). If it were MAX(startTime), a long
        // session that started early but ended late (e.g. 10:00–12:00) would set the watermark to
        // 10:00 and a session that started later but ended sooner (11:30–11:45) would be re-read
        // (its start > 10:00). Using MAX(endTime) starts the next window at 12:00, so both are
        // captured exactly once and never re-queried.
        val repo = FakeUsageRepository()
        val source = FakeUsageStatsSource(
            events = listOf(
                UsageEvent("com.long", millis(2026, 10, 7, 10), millis(2026, 10, 7, 12)),
                UsageEvent("com.short", millis(2026, 10, 7, 11, 30), millis(2026, 10, 7, 11, 45)),
            ),
        )
        val useCase = CollectUsageUseCase(repo, source, FakeIdProvider(), FakeTimeProvider(fixedNow, utc))

        useCase()
        useCase()

        assertEquals(2, repo.sessionCount())
        assertEquals(millis(2026, 10, 7, 12), repo.latestSessionEnd())
    }

    // --- AggregateUsageUseCase ---

    @Test
    fun aggregateDay_computesTotalsTopAppsAndCategories() = runTest {
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        repo.insertSessions(
            listOf(
                session("com.tiktok", millis(2026, 10, 7, 20), millis(2026, 10, 7, 20, 30)),
                session("com.tiktok", millis(2026, 10, 7, 21), millis(2026, 10, 7, 21, 10)),
                session("com.study", millis(2026, 10, 7, 9), millis(2026, 10, 7, 10)),
            ),
        )
        repo.upsertAppCategories(
            listOf(
                AppCategory("com.tiktok", "SOCIAL", isDistraction = true, overrideSource = AppCategory.SOURCE_USER),
                AppCategory("com.study", "PRODUCTIVE", isDistraction = false, overrideSource = AppCategory.SOURCE_USER),
            ),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)

        val summary = useCase.aggregateDay(LocalDate.of(2026, 10, 7).toEpochDay())

        assertEquals(30 + 10 + 60, (summary.totalScreenTimeMs / 60_000).toInt())
        assertEquals("com.study", summary.topApps.first().packageName)
        assertEquals(3, summary.sessionCount)
        assertEquals(40 * 60_000L, summary.categoryBreakdown["SOCIAL"])
        assertEquals(60 * 60_000L, summary.categoryBreakdown["PRODUCTIVE"])
        assertNotNull(summary.distractionRatio)
        assertEquals(0.4f, summary.distractionRatio!!, 0.001f)
    }

    @Test
    fun aggregateDay_clipsSessionsCrossingMidnight() = runTest {
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        // 23:30 -> 00:30 next day: 30 min belongs to Oct 7, 30 min to Oct 8.
        repo.insertSessions(
            listOf(session("com.tiktok", millis(2026, 10, 7, 23, 30), millis(2026, 10, 8, 0, 30))),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)

        val oct7 = useCase.aggregateDay(LocalDate.of(2026, 10, 7).toEpochDay())
        val oct8 = useCase.aggregateDay(LocalDate.of(2026, 10, 8).toEpochDay())

        assertEquals(30 * 60_000L, oct7.totalScreenTimeMs)
        assertEquals(30 * 60_000L, oct8.totalScreenTimeMs)
    }

    @Test
    fun aggregateDay_respectsTimezoneDayBoundary() = runTest {
        val jakarta = ZoneId.of("Asia/Jakarta") // UTC+7
        // 2026-10-07T18:00Z == 2026-10-08T01:00 Jakarta → belongs to Oct 8 local.
        val now = millis(2026, 10, 8, 3, 0, jakarta)
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(now, jakarta)
        repo.insertSessions(
            listOf(session("com.a", millis(2026, 10, 7, 18, 0, utc), millis(2026, 10, 7, 18, 30, utc))),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)

        val oct8 = useCase.aggregateDay(LocalDate.of(2026, 10, 8).toEpochDay())
        val oct7 = useCase.aggregateDay(LocalDate.of(2026, 10, 7).toEpochDay())

        assertEquals(30 * 60_000L, oct8.totalScreenTimeMs)
        assertEquals(0L, oct7.totalScreenTimeMs)
    }

    @Test
    fun aggregateWeek_buildsTrendAndBaseline() = runTest {
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        // Two days in the same ISO week (Oct 5 = Mon, Oct 7 = Wed → 2026-W41).
        repo.insertSessions(
            listOf(
                session("com.tiktok", millis(2026, 10, 5, 20), millis(2026, 10, 5, 20, 30)),
                session("com.tiktok", millis(2026, 10, 7, 20), millis(2026, 10, 7, 21, 0)),
            ),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)
        // Aggregate both days first.
        repo.upsertDailySummary(useCase.aggregateDay(LocalDate.of(2026, 10, 5).toEpochDay()))
        repo.upsertDailySummary(useCase.aggregateDay(LocalDate.of(2026, 10, 7).toEpochDay()))

        val week = useCase.aggregateWeek("2026-W41")

        assertEquals(30 * 60_000L + 60 * 60_000L, week.totalScreenTimeMs)
        assertEquals(2, week.trend.size)
        val baseline = week.baseline.single { it.packageName == "com.tiktok" }
        assertEquals(2, baseline.sampleDays)
        assertEquals((30 + 60) / 2 * 60_000L, baseline.avgSessionMs)
    }

    @Test
    fun aggregateWeek_baselineCountsZeroDaysForAppsUsedOnSomeDaysOnly() = runTest {
        // Regression: an app used on only 1 of 2 recorded days must have its average computed
        // over BOTH days (the other day contributes 0), and sampleDays must be the number of
        // observed days — not the number of days the app appeared. Otherwise the `>= MIN_SAMPLE_DAYS`
        // drift gate overstates coverage and the mean is inflated (FR-D6, PRD §10.3).
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        repo.insertSessions(
            listOf(
                session("com.tiktok", millis(2026, 10, 5, 20), millis(2026, 10, 5, 21, 0)), // 60 min
                session("com.other", millis(2026, 10, 7, 20), millis(2026, 10, 7, 20, 30)), // com.tiktok absent Oct 7
            ),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)
        repo.upsertDailySummary(useCase.aggregateDay(LocalDate.of(2026, 10, 5).toEpochDay()))
        repo.upsertDailySummary(useCase.aggregateDay(LocalDate.of(2026, 10, 7).toEpochDay()))

        val week = useCase.aggregateWeek("2026-W41")

        val baseline = week.baseline.single { it.packageName == "com.tiktok" }
        assertEquals("sampleDays counts observed days, not app days", 2, baseline.sampleDays)
        assertEquals("missing day contributes 0 → (60 + 0) / 2", 30 * 60_000L, baseline.avgSessionMs)
    }

    @Test
    fun aggregate_invokeComputesSummaryForEachDayWithData() = runTest {
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        repo.insertSessions(
            listOf(
                session("com.a", millis(2026, 10, 6, 10), millis(2026, 10, 6, 11)),
                session("com.a", millis(2026, 10, 7, 10), millis(2026, 10, 7, 11)),
            ),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)

        val days = useCase()

        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 6).toEpochDay(),
                LocalDate.of(2026, 10, 7).toEpochDay(),
            ),
            days,
        )
        assertNotNull(repo.dailySummary(LocalDate.of(2026, 10, 6).toEpochDay()))
        assertNull(repo.dailySummary(LocalDate.of(2026, 10, 5).toEpochDay()))
    }

    @Test
    fun aggregate_invokePersistsWeeklySummaryForDriftBaseline() = runTest {
        // Regression: aggregation must persist the weekly summary. Drift detection reads the
        // weekly baseline; if it is never saved, interventions can never fire (PRD §10.3, FR-D1).
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        repo.insertSessions(
            listOf(
                session("com.a", millis(2026, 10, 6, 10), millis(2026, 10, 6, 11)),
                session("com.a", millis(2026, 10, 7, 10), millis(2026, 10, 7, 11)),
            ),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)

        useCase()

        val week = repo.latestWeeklySummary()
        assertNotNull("weekly summary must be persisted for the baseline", week)
        assertTrue(week!!.baseline.isNotEmpty())
    }

    @Test
    fun aggregate_invokePersistsEveryTouchedWeek() = runTest {
        // C3 regression: aggregation must persist a weekly summary for EVERY week that has daily
        // data — not just the latest one. Otherwise historical weeks have no baseline and drift
        // can never fire against them (PRD §10.3). Oct 5 is in ISO week 2026-W41, Sep 28 in W40.
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        repo.insertSessions(
            listOf(
                session("com.a", millis(2026, 9, 28, 10), millis(2026, 9, 28, 11)),
                session("com.a", millis(2026, 10, 6, 10), millis(2026, 10, 6, 11)),
            ),
        )
        val useCase = AggregateUsageUseCase(repo, CategorizeAppsUseCase(repo), time)

        useCase()

        assertNotNull("earlier week (2026-W40) must be persisted", repo.weeklySummary("2026-W40"))
        assertNotNull("later week (2026-W41) must be persisted", repo.weeklySummary("2026-W41"))
    }

    // --- PruneUsageUseCase ---

    @Test
    fun prune_deletesSessionsOlderThanRetention() = runTest {
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        repo.insertSessions(
            listOf(
                session("com.old", millis(2026, 8, 1, 10), millis(2026, 8, 1, 11)), // ~67 days old
                session("com.recent", millis(2026, 10, 6, 10), millis(2026, 10, 6, 11)),
            ),
        )
        val useCase = PruneUsageUseCase(repo, time)

        val deleted = useCase(retentionDays = 30)

        assertEquals(1, deleted)
        assertEquals("com.recent", repo.sessions.value.single().packageName)
    }

    @Test
    fun prune_clampsRetentionToLockedRange() = runTest {
        val repo = FakeUsageRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        repo.insertSessions(
            listOf(session("com.a", millis(2026, 9, 25, 10), millis(2026, 9, 25, 11))), // ~12 days old
        )
        val useCase = PruneUsageUseCase(repo, time)

        // Requesting 1 day is clamped up to the 14-day minimum → nothing deleted.
        val deleted = useCase(retentionDays = 1)

        assertEquals(0, deleted)
        assertEquals(1, repo.sessionCount())
    }
}

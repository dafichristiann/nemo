package com.anchor.app.core.domain.usecase.usage

import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.AppBaseline
import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.AppUsage
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.WeeklyUsageSummary
import com.anchor.app.testutil.FakeTaskRepository
import com.anchor.app.testutil.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for category auto-assignment (FR-S3) and drift detection (FR-D1..D6),
 * docs 04-intervention-engine.md §2, PRD §10.3. Deterministic time (11-testing-strategy §5).
 */
class DriftAndCategoryTest {

    private val utc = ZoneOffset.UTC
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 20, 0, 0, 0, utc).toInstant().toEpochMilli()
    private val todayEpoch = LocalDate.of(2026, 10, 7).toEpochDay()

    private fun usageDay(totalMs: Long, pkg: String = "com.tiktok") = DailyUsageSummary(
        dateEpochDay = todayEpoch,
        totalScreenTimeMs = totalMs,
        distractionRatio = 0.5f,
        topApps = listOf(AppUsage(pkg, totalMs, 1)),
        categoryBreakdown = emptyMap(),
        sessionCount = 1,
        computedAt = fixedNow,
    )

    private fun weekBaseline(pkg: String, avgMs: Long, stdDevMs: Long, sampleDays: Int) =
        WeeklyUsageSummary(
            isoWeek = "2026-W41",
            totalScreenTimeMs = avgMs,
            dailyAvgMs = avgMs,
            baseline = listOf(AppBaseline(pkg, avgMs, stdDevMs, sampleDays)),
            trend = emptyList(),
            computedAt = fixedNow,
        )

    private fun task(id: String, priority: TaskPriority, done: Boolean = false) = Task(
        id = id,
        title = "Task $id",
        priority = priority,
        status = if (done) com.anchor.app.core.domain.model.TaskStatus.DONE else com.anchor.app.core.domain.model.TaskStatus.TODO,
        createdAt = fixedNow,
    )

    // --- CategorizeAppsUseCase ---

    @Test
    fun categorize_assignsKnownDistractionAndProductive() = runTest {
        val repo = FakeUsageRepository()
        val useCase = CategorizeAppsUseCase(repo)

        val count = useCase(listOf("com.zhiliaoapp.musically", "com.android.chrome", "com.example.unknown"))

        assertEquals(3, count)
        val byPkg = repo.appCategories().associateBy { it.packageName }
        assertEquals(AppCategories.SOCIAL, byPkg.getValue("com.zhiliaoapp.musically").category)
        assertEquals(true, byPkg.getValue("com.zhiliaoapp.musically").isDistraction)
        assertEquals(AppCategories.PRODUCTIVE, byPkg.getValue("com.android.chrome").category)
        assertEquals(false, byPkg.getValue("com.android.chrome").isDistraction)
        assertEquals(AppCategories.OTHER, byPkg.getValue("com.example.unknown").category)
    }

    @Test
    fun categorize_neverOverwritesUserOverride() = runTest {
        val repo = FakeUsageRepository()
        repo.upsertAppCategory(
            AppCategory("com.zhiliaoapp.musically", AppCategories.PRODUCTIVE, isDistraction = false, overrideSource = AppCategory.SOURCE_USER),
        )
        val useCase = CategorizeAppsUseCase(repo)

        useCase(listOf("com.zhiliaoapp.musically"))

        val stored = repo.appCategories().single { it.packageName == "com.zhiliaoapp.musically" }
        assertEquals(AppCategories.PRODUCTIVE, stored.category)
        assertEquals(AppCategory.SOURCE_USER, stored.overrideSource)
    }

    // --- DetectDriftUseCase ---

    @Test
    fun drift_firesWhenDeviationAboveThresholdAndHighTaskPending() = runTest {
        val usage = FakeUsageRepository()
        val tasks = FakeTaskRepository()
        // Baseline: avg 20 min, stddev 5 min → today 40 min = +4σ.
        usage.upsertDailySummary(usageDay(totalMs = 40 * 60_000L))
        usage.upsertWeeklySummary(weekBaseline("com.tiktok", 20 * 60_000L, 5 * 60_000L, sampleDays = 5))
        tasks.insert(task("t1", TaskPriority.HIGH))
        val useCase = DetectDriftUseCase(usage, tasks, FakeTimeProvider(fixedNow, utc))

        val signal = useCase()

        assertNotNull(signal)
        assertEquals("com.tiktok", signal!!.packageName)
        assertEquals("t1", signal.relatedTaskId)
    }

    @Test
    fun drift_isNullWhenSampleDaysTooFew() = runTest {
        val usage = FakeUsageRepository()
        val tasks = FakeTaskRepository()
        usage.upsertDailySummary(usageDay(totalMs = 40 * 60_000L))
        usage.upsertWeeklySummary(weekBaseline("com.tiktok", 20 * 60_000L, 5 * 60_000L, sampleDays = 2))
        tasks.insert(task("t1", TaskPriority.HIGH))
        val useCase = DetectDriftUseCase(usage, tasks, FakeTimeProvider(fixedNow, utc))

        assertNull(useCase())
    }

    @Test
    fun drift_isNullWhenNoHighPriorityTaskPending() = runTest {
        val usage = FakeUsageRepository()
        val tasks = FakeTaskRepository()
        usage.upsertDailySummary(usageDay(totalMs = 40 * 60_000L))
        usage.upsertWeeklySummary(weekBaseline("com.tiktok", 20 * 60_000L, 5 * 60_000L, sampleDays = 5))
        // Only a LOW task open → no meaningful consequence.
        tasks.insert(task("t1", TaskPriority.LOW))
        val useCase = DetectDriftUseCase(usage, tasks, FakeTimeProvider(fixedNow, utc))

        assertNull(useCase())
    }

    @Test
    fun drift_isNullWhenWithinBaseline() = runTest {
        val usage = FakeUsageRepository()
        val tasks = FakeTaskRepository()
        usage.upsertDailySummary(usageDay(totalMs = 21 * 60_000L)) // well within 20±5
        usage.upsertWeeklySummary(weekBaseline("com.tiktok", 20 * 60_000L, 5 * 60_000L, sampleDays = 5))
        tasks.insert(task("t1", TaskPriority.URGENT))
        val useCase = DetectDriftUseCase(usage, tasks, FakeTimeProvider(fixedNow, utc))

        assertNull(useCase())
    }

    @Test
    fun drift_usesRatioFallbackWhenStdDevNearZero() = runTest {
        val usage = FakeUsageRepository()
        val tasks = FakeTaskRepository()
        usage.upsertDailySummary(usageDay(totalMs = 45 * 60_000L)) // 2.25× avg → drift
        usage.upsertWeeklySummary(weekBaseline("com.tiktok", 20 * 60_000L, stdDevMs = 0L, sampleDays = 5))
        tasks.insert(task("t1", TaskPriority.URGENT))
        val useCase = DetectDriftUseCase(usage, tasks, FakeTimeProvider(fixedNow, utc))

        assertNotNull(useCase())
    }
}

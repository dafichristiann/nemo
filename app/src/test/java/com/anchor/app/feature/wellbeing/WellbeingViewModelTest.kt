package com.anchor.app.feature.wellbeing

import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.AppUsage
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.usecase.usage.AggregateUsageUseCase
import com.anchor.app.core.domain.usecase.usage.CategorizeAppsUseCase
import com.anchor.app.testutil.FakeSettingsRepository
import com.anchor.app.testutil.FakeUsageRepository
import com.anchor.app.testutil.FakeUsageStatsSource
import com.anchor.app.testutil.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Tests for [WellbeingViewModel] day selection (docs 07-screens.md §2.7).
 *
 * Verifies the default selection is today, that tapping another day in the chart switches the
 * card/top-apps to that day, and that the comparison is made against the day before the selected
 * one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WellbeingViewModelTest {

    private val utc = ZoneOffset.UTC
    // A Wednesday, so there are earlier days in the same week window.
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 9, 30, 0, 0, utc).toInstant().toEpochMilli()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var usage: FakeUsageRepository
    private lateinit var settings: FakeSettingsRepository
    private lateinit var time: FakeTimeProvider
    private lateinit var usageStats: FakeUsageStatsSource

    private val today: LocalDate = LocalDate.of(2026, 10, 7)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        usage = FakeUsageRepository()
        settings = FakeSettingsRepository()
        time = FakeTimeProvider(fixedNow, utc)
        usageStats = FakeUsageStatsSource(permissionGranted = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = WellbeingViewModel(
        usageRepository = usage,
        settingsRepository = settings,
        usageStats = usageStats,
        aggregateUsage = AggregateUsageUseCase(
            usageRepository = usage,
            categorizeApps = CategorizeAppsUseCase(usage),
            timeProvider = time,
        ),
        timeProvider = time,
        dispatchers = TestDispatcherProvider(dispatcher),
    )

    private fun seedDay(epochDay: Long, totalMs: Long, topAppMs: Long = 0L, distractionRatio: Float? = null) {
        usage.daily.value = usage.daily.value + (
            epochDay to DailyUsageSummary(
                dateEpochDay = epochDay,
                totalScreenTimeMs = totalMs,
                distractionRatio = distractionRatio,
                topApps = if (topAppMs > 0) {
                    listOf(AppUsage("com.example.app", topAppMs, 1))
                } else {
                    emptyList()
                },
                categoryBreakdown = emptyMap(),
                sessionCount = 1,
                computedAt = fixedNow,
            )
        )
    }

    @Test
    fun defaultSelection_isToday() = runTest(dispatcher) {
        seedDay(today.toEpochDay(), 3_600_000)
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(WellbeingAvailability.AVAILABLE, state.availability)
        assertTrue(state.selectedIsToday)
        assertEquals(today, state.selectedDate)
        assertEquals(3_600_000, state.totalMs)
    }

    @Test
    fun onSelectDay_showsThatDayAndComparesToPreviousDay() = runTest(dispatcher) {
        val yesterday = today.minusDays(1)
        seedDay(today.toEpochDay(), 1_800_000)
        seedDay(yesterday.toEpochDay(), 3_600_000)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onSelectDay(yesterday.toEpochDay())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.selectedIsToday)
        assertEquals(yesterday, state.selectedDate)
        assertEquals(today.minusDays(2), state.previousDate)
        assertEquals(3_600_000, state.totalMs)
        assertTrue("the picked day should be marked selected", state.week.any { it.isSelected && it.epochDay == yesterday.toEpochDay() })
    }

    @Test
    fun onSelectDay_missingPermissionKeepsPermissionState() = runTest(dispatcher) {
        usageStats.permissionGranted = false
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(WellbeingAvailability.PERMISSION_MISSING, vm.uiState.value.availability)
    }

    @Test
    fun noData_whenWeekHasNoScreenTime() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(WellbeingAvailability.NO_DATA, vm.uiState.value.availability)
        // The chart still renders (7 bars) so the empty state is never a blank screen.
        assertEquals(7, vm.uiState.value.week.size)
    }

    @Test
    fun stats_compareTodayAgainstYesterdayAndAverage() = runTest(dispatcher) {
        val yesterday = today.minusDays(1)
        seedDay(today.toEpochDay(), 4_000_000)      // today
        seedDay(yesterday.toEpochDay(), 2_000_000)  // yesterday
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        // vs yesterday: +2,000,000 ms -> +100%.
        val vsYesterday = state.vsYesterday
        assertTrue("expected a vs-yesterday comparison", vsYesterday != null)
        assertEquals(TrendDirection.UP, vsYesterday!!.direction)
        assertEquals(2_000_000, vsYesterday.deltaMs)
        assertEquals(100, vsYesterday.deltaPct)

        // Average of the days with data = (4,000,000 + 2,000,000) / 2 = 3,000,000 -> +33%.
        val vsAvg = state.vsWeekAverage
        assertTrue("expected a vs-average comparison", vsAvg != null)
        assertEquals(2, state.daysWithData)
        assertEquals(TrendDirection.UP, vsAvg!!.direction)
        assertEquals(1_000_000, vsAvg.deltaMs)
        assertEquals(33, vsAvg.deltaPct)
    }

    @Test
    fun stats_flatWhenSameAsYesterday() = runTest(dispatcher) {
        val yesterday = today.minusDays(1)
        seedDay(today.toEpochDay(), 3_000_000)
        seedDay(yesterday.toEpochDay(), 3_000_000)
        val vm = viewModel()
        advanceUntilIdle()

        val cmp = vm.uiState.value.vsYesterday
        assertTrue(cmp != null)
        assertEquals(TrendDirection.FLAT, cmp!!.direction)
        assertEquals(0L, cmp.deltaMs)
        assertEquals(0, cmp.deltaPct)
    }

    @Test
    fun stats_distractionDeltaComparesToYesterday() = runTest(dispatcher) {
        val yesterday = today.minusDays(1)
        seedDay(today.toEpochDay(), 3_000_000, distractionRatio = 0.40f)
        seedDay(yesterday.toEpochDay(), 3_000_000, distractionRatio = 0.25f)
        val vm = viewModel()
        advanceUntilIdle()

        // 0.40 - 0.25 = 0.15 -> 15 percentage points.
        val delta = vm.uiState.value.distractionDeltaPctPoints
        assertTrue(delta != null)
        assertTrue(delta!! > 14f && delta < 16f)
    }
}

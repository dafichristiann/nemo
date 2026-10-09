package com.anchor.app.feature.wellbeing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.common.short
import com.anchor.app.core.domain.model.AppUsage
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.UsageRepository
import com.anchor.app.core.domain.usecase.usage.AggregateUsageUseCase
import com.anchor.app.platform.usagestats.UsageStatsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** One bar in the weekly chart. */
data class DayBar(
    val epochDay: Long,
    val label: String,
    val totalMs: Long,
    val isToday: Boolean,
    val isSelected: Boolean,
)

/** One row in the "top apps" list. */
data class TopAppRow(
    val packageName: String,
    val label: String,
    val totalMs: Long,
)

/** Availability of the Wellbeing data, driving the screen's state (§6 graceful degradation). */
enum class WellbeingAvailability { PERMISSION_MISSING, NO_DATA, AVAILABLE }

/** Direction of a change relative to a baseline (drives the ▲/▼ indicator + colour). */
enum class TrendDirection { UP, DOWN, FLAT }

/**
 * A single comparison statistic: how the selected day compares to a baseline (yesterday or the
 * 7-day average). [deltaPct] is null when no honest percentage can be computed (baseline is 0).
 */
data class StatComparison(
    val direction: TrendDirection,
    val deltaMs: Long,
    val deltaPct: Int?,
)

/** Wellbeing screen state (PRD §10.2, docs 07-screens.md §2.7). */
data class WellbeingUiState(
    val availability: WellbeingAvailability = WellbeingAvailability.PERMISSION_MISSING,
    /** Total screen time for the **selected** day (defaults to today). */
    val totalMs: Long = 0,
    /** Screen time for the day before the selected one, for the comparison line. */
    val previousTotalMs: Long = 0,
    /** The day whose details are shown. */
    val selectedDate: LocalDate = LocalDate.now(),
    /** The day the comparison is made against (the day before [selectedDate]). */
    val previousDate: LocalDate = LocalDate.now().minusDays(1),
    /** True while the selected day is today. */
    val selectedIsToday: Boolean = true,
    /** True while the comparison day is the day before today (i.e. "yesterday"). */
    val previousIsYesterday: Boolean = true,
    val topApps: List<TopAppRow> = emptyList(),
    val week: List<DayBar> = emptyList(),
    val distractionRatio: Float? = null,
    /** Selected day vs the day before it (delta + %). Null when the previous day has no data. */
    val vsYesterday: StatComparison? = null,
    /** Selected day vs the average of the days with data in the 7-day window. */
    val vsWeekAverage: StatComparison? = null,
    /** Distraction share change vs the previous day (percentage points), null when unavailable. */
    val distractionDeltaPctPoints: Float? = null,
    /** Number of days in the window that have data (used to label the average honestly). */
    val daysWithData: Int = 0,
)

/**
 * Wellbeing ViewModel (FR-W1, W6; PRD §10.2).
 *
 * Numbers come **only** from Android usage data (FR-W7) — never AI, never fabricated.
 * When the permission is missing or there is no data, the screen shows an honest empty
 * state instead of misleading zeros (AC §10.2).
 *
 * The screen-time card, top apps, and comparison all describe a **selectable** day: the 7-day
 * chart is tappable so the user can look back at any day this week ([onSelectDay]); the default
 * selection is today.
 */
@HiltViewModel
class WellbeingViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository,
    private val usageStats: UsageStatsSource,
    private val aggregateUsage: AggregateUsageUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WellbeingUiState())
    val uiState: StateFlow<WellbeingUiState> = _uiState.asStateFlow()

    /** The day whose details are shown. Null until the first load picks "today". */
    private var selectedEpochDay: Long? = null

    init {
        refresh()
    }

    /** Recomputes the displayed summary. Public so the screen can retry after enabling access. */
    fun refresh() = viewModelScope.launch(dispatchers.io) {
        val hasPermission = usageStats.hasPermission()
        val trackingEnabled = settingsRepository.observe().first().trackingEnabled
        val today = timeProvider.today()
        val todayEpoch = today.toEpochDay()

        if (!hasPermission) {
            _uiState.value = WellbeingUiState(availability = WellbeingAvailability.PERMISSION_MISSING)
            return@launch
        }

        // Make sure summaries reflect the latest raw sessions before reading them.
        if (trackingEnabled) runCatching { aggregateUsage() }

        // Last 7 local days (oldest first, ending today). The map holds each day's summary,
        // if any, so both the chart and the selected-day detail come from a single read.
        val weekDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val summariesByEpoch = weekDays.associate {
            it.toEpochDay() to usageRepository.dailySummary(it.toEpochDay())
        }

        // Default the selection to today; keep an existing selection when still within the window.
        val selected = selectedEpochDay?.takeIf { it in summariesByEpoch } ?: todayEpoch
        selectedEpochDay = selected

        val anyData = summariesByEpoch.values.any { it != null && it.totalScreenTimeMs > 0 }
        if (!anyData) {
            _uiState.value = WellbeingUiState(
                availability = WellbeingAvailability.NO_DATA,
                week = buildWeek(weekDays, summariesByEpoch, todayEpoch, selected),
            )
            return@launch
        }

        val selectedSummary = summariesByEpoch[selected]
        // The previous day can fall just outside the 7-day window (when the oldest day is
        // selected), so read it directly rather than only from the map.
        val previousSummary = summariesByEpoch[selected - 1] ?: usageRepository.dailySummary(selected - 1)

        val selectedTotal = selectedSummary?.totalScreenTimeMs ?: 0L
        val previousTotal = previousSummary?.totalScreenTimeMs ?: 0L
        val totals = summariesByEpoch.values.filterNotNull().map { it.totalScreenTimeMs }.filter { it > 0L }
        val daysWithData = totals.size
        val weekAverage = if (daysWithData > 0) totals.sum() / daysWithData else 0L

        _uiState.value = WellbeingUiState(
            availability = WellbeingAvailability.AVAILABLE,
            totalMs = selectedTotal,
            previousTotalMs = previousTotal,
            selectedDate = LocalDate.ofEpochDay(selected),
            previousDate = LocalDate.ofEpochDay(selected - 1),
            selectedIsToday = selected == todayEpoch,
            previousIsYesterday = selected - 1 == todayEpoch - 1,
            topApps = (selectedSummary?.topApps ?: emptyList()).take(MAX_TOP_APPS).map { it.toRow() },
            week = buildWeek(weekDays, summariesByEpoch, todayEpoch, selected),
            distractionRatio = selectedSummary?.distractionRatio,
            vsYesterday = if (previousTotal > 0) compare(selectedTotal, previousTotal) else null,
            vsWeekAverage = if (weekAverage > 0) compare(selectedTotal, weekAverage) else null,
            distractionDeltaPctPoints = distractionDelta(selectedSummary, previousSummary),
            daysWithData = daysWithData,
        )
    }

    /** Signed comparison of [value] against [baseline] (delta + integer %). */
    private fun compare(value: Long, baseline: Long): StatComparison {
        val delta = value - baseline
        val direction = when {
            delta > 0L -> TrendDirection.UP
            delta < 0L -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }
        // Percentage is only honest when the baseline is non-zero.
        val pct = if (baseline > 0L) ((delta.toDouble() / baseline) * 100.0).toInt() else null
        return StatComparison(direction = direction, deltaMs = delta, deltaPct = pct)
    }

    /** Change in distraction share between the selected day and the previous day, in pct points. */
    private fun distractionDelta(
        selected: DailyUsageSummary?,
        previous: DailyUsageSummary?,
    ): Float? {
        val a = selected?.distractionRatio ?: return null
        val b = previous?.distractionRatio ?: return null
        return (a - b) * 100f
    }

    /** Select a day in the chart to view its details. Ignores days outside the current window. */
    fun onSelectDay(epochDay: Long) {
        if (selectedEpochDay == epochDay) return
        selectedEpochDay = epochDay
        refresh()
    }

    private fun buildWeek(
        days: List<LocalDate>,
        summaries: Map<Long, DailyUsageSummary?>,
        todayEpoch: Long,
        selectedEpoch: Long,
    ): List<DayBar> = days.map { date ->
        val epoch = date.toEpochDay()
        DayBar(
            epochDay = epoch,
            label = date.dayOfWeek.short(),
            totalMs = summaries[epoch]?.totalScreenTimeMs ?: 0L,
            isToday = epoch == todayEpoch,
            isSelected = epoch == selectedEpoch,
        )
    }

    private fun AppUsage.toRow(): TopAppRow = TopAppRow(
        packageName = packageName,
        label = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
        totalMs = totalTimeMs,
    )

    private companion object {
        const val MAX_TOP_APPS = 3
    }
}

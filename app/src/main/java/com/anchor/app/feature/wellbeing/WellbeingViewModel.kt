package com.anchor.app.feature.wellbeing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
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
)

/** One row in the "top apps" list. */
data class TopAppRow(
    val packageName: String,
    val label: String,
    val totalMs: Long,
)

/** Availability of the Wellbeing data, driving the screen's state (§6 graceful degradation). */
enum class WellbeingAvailability { PERMISSION_MISSING, NO_DATA, AVAILABLE }

/** Wellbeing screen state (PRD §10.2, docs 07-screens.md §2.7). */
data class WellbeingUiState(
    val availability: WellbeingAvailability = WellbeingAvailability.PERMISSION_MISSING,
    val todayTotalMs: Long = 0,
    val yesterdayTotalMs: Long = 0,
    val topApps: List<TopAppRow> = emptyList(),
    val week: List<DayBar> = emptyList(),
    val distractionRatio: Float? = null,
)

/**
 * Wellbeing ViewModel (FR-W1, W6; PRD §10.2).
 *
 * Numbers come **only** from Android usage data (FR-W7) — never AI, never fabricated.
 * When the permission is missing or there is no data, the screen shows an honest empty
 * state instead of misleading zeros (AC §10.2).
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

    init {
        refresh()
    }

    /** Recomputes the displayed summary. Public so the screen can retry after enabling access. */
    fun refresh() = viewModelScope.launch(dispatchers.io) {
        val hasPermission = usageStats.hasPermission()
        val trackingEnabled = settingsRepository.observe().first().trackingEnabled
        val today = timeProvider.today()

        if (!hasPermission) {
            _uiState.value = WellbeingUiState(availability = WellbeingAvailability.PERMISSION_MISSING)
            return@launch
        }

        // Make sure summaries reflect the latest raw sessions before reading them.
        if (trackingEnabled) runCatching { aggregateUsage() }

        val todayEpoch = today.toEpochDay()
        val todays = usageRepository.dailySummary(todayEpoch)
        val yesterday = usageRepository.dailySummary(todayEpoch - 1)

        // Last 7 local days (Mon..today), oldest first.
        val weekDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val weekSummaries = weekDays.map { usageRepository.dailySummary(it.toEpochDay()) }

        val anyData = todays != null || weekSummaries.any { it != null && it.totalScreenTimeMs > 0 }
        if (!anyData) {
            _uiState.value = WellbeingUiState(
                availability = WellbeingAvailability.NO_DATA,
                week = buildWeek(weekDays, weekSummaries, todayEpoch),
            )
            return@launch
        }

        _uiState.value = WellbeingUiState(
            availability = WellbeingAvailability.AVAILABLE,
            todayTotalMs = todays?.totalScreenTimeMs ?: 0L,
            yesterdayTotalMs = yesterday?.totalScreenTimeMs ?: 0L,
            topApps = (todays?.topApps ?: emptyList()).take(MAX_TOP_APPS).map { it.toRow() },
            week = buildWeek(weekDays, weekSummaries, todayEpoch),
            distractionRatio = todays?.distractionRatio,
        )
    }

    private fun buildWeek(
        days: List<LocalDate>,
        summaries: List<DailyUsageSummary?>,
        todayEpoch: Long,
    ): List<DayBar> = days.mapIndexed { index, date ->
        DayBar(
            epochDay = date.toEpochDay(),
            label = date.dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() },
            totalMs = summaries[index]?.totalScreenTimeMs ?: 0L,
            isToday = date.toEpochDay() == todayEpoch,
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

package com.anchor.app.core.domain.usecase.usage

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.AppBaseline
import com.anchor.app.core.domain.model.AppUsage
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.model.WeeklyUsageSummary
import com.anchor.app.core.domain.repository.UsageRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Aggregates raw sessions into daily and weekly summaries (FR-W3, docs 02 §4.2).
 *
 * Deterministic: all day/week boundaries come from [TimeProvider] (device zone), so the same
 * input always yields the same summary (docs 11-testing-strategy.md §5).
 *
 * Applies overlap-aware clipping: a session that straddles midnight is split by day so the
 * totals per day are exact rather than attributing the whole session to its start day.
 */
class AggregateUsageUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val categorizeApps: CategorizeAppsUseCase,
    private val timeProvider: TimeProvider,
) {
    /**
     * Aggregates every full local day that has raw sessions. Returns the epoch-days whose
     * daily summary was (re)computed.
     */
    suspend operator fun invoke(nowMillis: Long = timeProvider.nowMillis()): List<Long> {
        val earliest = usageRepository.earliestSessionStart() ?: return emptyList()
        val firstDay = timeProvider.epochDayOf(earliest)
        val lastDay = timeProvider.epochDayOf(nowMillis)

        val computed = mutableListOf<Long>()
        for (epochDay in firstDay..lastDay) {
            val summary = aggregateDay(epochDay)
            usageRepository.upsertDailySummary(summary)
            computed += epochDay
        }

        // Recompute and PERSIST every ISO week that was touched, not just the latest one.
        // Drift detection reads the weekly baseline (PRD §10.3); persisting only the most recent
        // week left historical weeks without a baseline, so drift could never fire against them.
        val touchedWeeks = computed
            .map { timeProvider.isoWeekKey(LocalDate.ofEpochDay(it)) }
            .distinct()
        touchedWeeks.forEach { usageRepository.upsertWeeklySummary(aggregateWeek(it)) }

        return computed
    }

    /** Builds the [DailyUsageSummary] for one local epoch-day. */
    suspend fun aggregateDay(epochDay: Long): DailyUsageSummary {
        val zone = timeProvider.zoneId()
        val dayStart = LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = LocalDate.ofEpochDay(epochDay + 1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Widen the query slightly so sessions straddling the edges are included, then clip.
        val sessions = usageRepository.sessionsBetween(dayStart, dayEnd)

        // Ensure every app seen today has a category (user overrides are preserved).
        categorizeApps(sessions.map { it.packageName })

        val clipped = sessions.mapNotNull { it.clipTo(dayStart, dayEnd) }
        val totalMs = clipped.sumOf { it.durationMs }

        val perApp = clipped.groupBy { it.packageName }
            .map { (pkg, list) ->
                AppUsage(
                    packageName = pkg,
                    totalTimeMs = list.sumOf { it.durationMs },
                    sessionCount = list.size,
                )
            }
            .sortedByDescending { it.totalTimeMs }

        val categories = usageRepository.appCategories().associateBy { it.packageName }
        val breakdown = mutableMapOf<String, Long>()
        var distractionMs = 0L
        clipped.forEach { session ->
            val category = categories[session.packageName]
            val key = category?.category ?: DEFAULT_CATEGORY
            breakdown[key] = (breakdown[key] ?: 0L) + session.durationMs
            if (category?.isDistraction == true) distractionMs += session.durationMs
        }

        return DailyUsageSummary(
            dateEpochDay = epochDay,
            totalScreenTimeMs = totalMs,
            distractionRatio = if (totalMs > 0) (distractionMs.toFloat() / totalMs) else null,
            topApps = perApp,
            categoryBreakdown = breakdown,
            sessionCount = clipped.size,
            computedAt = timeProvider.nowMillis(),
        )
    }

    /** Builds the [WeeklyUsageSummary] for an ISO week key (e.g. `2026-W41`). */
    suspend fun aggregateWeek(isoWeek: String): WeeklyUsageSummary {
        val summaries = usageRepository.dailySummariesFrom(0L)
            .filter { timeProvider.isoWeekKey(LocalDate.ofEpochDay(it.dateEpochDay)) == isoWeek }

        val trend = summaries.sortedBy { it.dateEpochDay }.map { it.totalScreenTimeMs }
        val total = trend.sum()
        val dailyAvg = if (trend.isEmpty()) 0L else total / trend.size

        return WeeklyUsageSummary(
            isoWeek = isoWeek,
            totalScreenTimeMs = total,
            dailyAvgMs = dailyAvg,
            baseline = computeBaseline(summaries),
            trend = trend,
            computedAt = timeProvider.nowMillis(),
        )
    }

    /**
     * Personal per-app baseline = average & std-dev of that app's **daily** usage across the
     * week (PRD §10.3). `sampleDays` counts days the app actually appeared, so drift logic
     * can refuse to act when data is thin (FR-D6).
     */
    private fun computeBaseline(summaries: List<DailyUsageSummary>): List<AppBaseline> {
        val byApp = mutableMapOf<String, MutableList<Long>>()
        summaries.forEach { day ->
            day.topApps.forEach { app ->
                byApp.getOrPut(app.packageName) { mutableListOf() }.add(app.totalTimeMs)
            }
        }

        return byApp.map { (pkg, values) ->
            val n = values.size
            val mean = values.average().toLong()
            val variance = if (n == 0) 0.0 else values.sumOf { (it - mean) * (it - mean).toDouble() } / n
            AppBaseline(
                packageName = pkg,
                avgSessionMs = mean,
                stdDevMs = kotlin.math.sqrt(variance).toLong(),
                sampleDays = n,
            )
        }
    }

    companion object {
        const val DEFAULT_CATEGORY = "OTHER"
    }
}

/** Clips a session to [windowStart, windowEnd); null if fully outside or empty. */
private fun UsageSession.clipTo(windowStart: Long, windowEnd: Long): UsageSession? {
    val start = maxOf(startTime, windowStart)
    val end = minOf(endTime, windowEnd)
    if (end <= start) return null
    return copy(startTime = start, endTime = end, durationMs = end - start)
}

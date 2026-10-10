package com.anchor.app.testutil

import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.model.WeeklyUsageSummary
import com.anchor.app.core.domain.repository.UsageRepository
import com.anchor.app.platform.usagestats.AppTotal
import com.anchor.app.platform.usagestats.UsageEvent
import com.anchor.app.platform.usagestats.UsageStatsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [UsageRepository] fake (docs 11-testing-strategy.md §5). Mirrors the real query
 * semantics — including the [UsageSessionDao][com.anchor.app.core.data.dao.UsageSessionDao]
 * "startTime in [from, to)" range and IGNORE-on-duplicate-id insert.
 */
class FakeUsageRepository : UsageRepository {
    val sessions = MutableStateFlow<List<UsageSession>>(emptyList())
    val daily = MutableStateFlow<Map<Long, DailyUsageSummary>>(emptyMap())
    val weekly = MutableStateFlow<Map<String, WeeklyUsageSummary>>(emptyMap())
    val categories = MutableStateFlow<List<AppCategory>>(emptyList())

    override suspend fun insertSessions(sessions: List<UsageSession>) {
        val existing = this.sessions.value.map { it.id }.toSet()
        val incoming = sessions.filterNot { it.id in existing }
        this.sessions.value = this.sessions.value + incoming
    }

    override suspend fun sessionsBetween(from: Long, to: Long): List<UsageSession> =
        sessions.value.filter { it.endTime > from && it.startTime < to }.sortedBy { it.startTime }

    override fun observeSessions(): Flow<List<UsageSession>> = sessions

    override suspend fun earliestSessionStart(): Long? =
        sessions.value.minOfOrNull { it.startTime }

    override suspend fun latestSessionStart(): Long? =
        sessions.value.maxOfOrNull { it.startTime }

    override suspend fun latestSessionEnd(): Long? =
        sessions.value.maxOfOrNull { it.endTime }

    override suspend fun sessionCount(): Int = sessions.value.size

    override suspend fun pruneSessionsBefore(cutoff: Long): Int {
        val before = sessions.value.size
        sessions.value = sessions.value.filterNot { it.startTime < cutoff }
        return before - sessions.value.size
    }

    override suspend fun upsertDailySummary(summary: DailyUsageSummary) {
        daily.value = daily.value + (summary.dateEpochDay to summary)
    }

    override suspend fun dailySummary(epochDay: Long): DailyUsageSummary? = daily.value[epochDay]

    override suspend fun dailySummariesFrom(fromEpochDay: Long): List<DailyUsageSummary> =
        daily.value.values.filter { it.dateEpochDay >= fromEpochDay }.sortedBy { it.dateEpochDay }

    override fun observeDailySummaries(): Flow<List<DailyUsageSummary>> =
        daily.map { it.values.sortedBy { d -> d.dateEpochDay } }

    override suspend fun upsertWeeklySummary(summary: WeeklyUsageSummary) {
        weekly.value = weekly.value + (summary.isoWeek to summary)
    }

    override suspend fun weeklySummary(isoWeek: String): WeeklyUsageSummary? = weekly.value[isoWeek]

    override suspend fun latestWeeklySummary(): WeeklyUsageSummary? =
        weekly.value.values.maxByOrNull { it.isoWeek }

    override fun observeWeeklySummaries(): Flow<List<WeeklyUsageSummary>> =
        weekly.map { it.values.sortedBy { w -> w.isoWeek } }

    override suspend fun upsertAppCategory(category: AppCategory) {
        categories.value = categories.value.filterNot { it.packageName == category.packageName } + category
    }

    override suspend fun upsertAppCategories(categories: List<AppCategory>) {
        categories.forEach { upsertAppCategory(it) }
    }

    override suspend fun appCategories(): List<AppCategory> = categories.value

    override fun observeAppCategories(): Flow<List<AppCategory>> = categories

    override suspend fun clearAll() {
        sessions.value = emptyList()
        daily.value = emptyMap()
        weekly.value = emptyMap()
        categories.value = emptyList()
    }
}

/** Scriptable [UsageStatsSource] fake; returns queued events/totals and a permission flag. */
class FakeUsageStatsSource(
    var permissionGranted: Boolean = true,
    var events: List<UsageEvent> = emptyList(),
    var totals: List<AppTotal> = emptyList(),
) : UsageStatsSource {
    override fun hasPermission(): Boolean = permissionGranted

    override fun queryEvents(startMillis: Long, endMillis: Long): List<UsageEvent> =
        events.filter { it.startTime >= startMillis && it.startTime < endMillis }

    override fun queryTotals(startMillis: Long, endMillis: Long): List<AppTotal> =
        totals.filter { it.totalTimeMs > 0 }
}

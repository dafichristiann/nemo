package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.model.WeeklyUsageSummary
import kotlinx.coroutines.flow.Flow

/**
 * Usage/Wellbeing repository (docs 02-architecture.md §3.1). `feature` and workers depend
 * only on this; the Room-backed implementation lives in `core/data`.
 *
 * Raw sessions are pruned; daily/weekly summaries are permanent (docs 03 §10).
 */
interface UsageRepository {

    // --- Raw sessions (usage_session) ---

    suspend fun insertSessions(sessions: List<UsageSession>)

    suspend fun sessionsBetween(from: Long, to: Long): List<UsageSession>

    fun observeSessions(): Flow<List<UsageSession>>

    suspend fun earliestSessionStart(): Long?

    suspend fun latestSessionStart(): Long?

    /**
     * End time of the most recently **ended** raw session — the collection watermark
     * (CollectUsageUseCase). Uses `MAX(endTime)`, not `MAX(startTime)`: a long session that
     * started earlier but ended later must not mask other apps used in between.
     */
    suspend fun latestSessionEnd(): Long?

    suspend fun sessionCount(): Int

    /** Pruning (FR-W4): deletes raw sessions starting before [cutoff]. Returns rows removed. */
    suspend fun pruneSessionsBefore(cutoff: Long): Int

    // --- Daily summaries ---

    suspend fun upsertDailySummary(summary: DailyUsageSummary)

    suspend fun dailySummary(epochDay: Long): DailyUsageSummary?

    suspend fun dailySummariesFrom(fromEpochDay: Long): List<DailyUsageSummary>

    fun observeDailySummaries(): Flow<List<DailyUsageSummary>>

    // --- Weekly summaries ---

    suspend fun upsertWeeklySummary(summary: WeeklyUsageSummary)

    suspend fun weeklySummary(isoWeek: String): WeeklyUsageSummary?

    suspend fun latestWeeklySummary(): WeeklyUsageSummary?

    fun observeWeeklySummaries(): Flow<List<WeeklyUsageSummary>>

    // --- App categories ---

    suspend fun upsertAppCategory(category: AppCategory)

    suspend fun upsertAppCategories(categories: List<AppCategory>)

    suspend fun appCategories(): List<AppCategory>

    fun observeAppCategories(): Flow<List<AppCategory>>

    /** "Delete all data" (PRD §10.10) — clears every usage table. */
    suspend fun clearAll()
}

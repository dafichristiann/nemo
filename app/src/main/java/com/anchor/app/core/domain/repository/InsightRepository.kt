package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.Insight
import com.anchor.app.core.domain.model.InsightType
import kotlinx.coroutines.flow.Flow

/**
 * Insight repository (docs/superpowers/design/02-architecture.md §3.1, docs 03 §6.1).
 * Backs the Insights screen and the daily-reflection generator/cache.
 */
interface InsightRepository {

    suspend fun insert(insight: Insight)

    suspend fun getById(id: String): Insight?

    /** The daily reflection for [dateEpochDay], or null when not generated yet (cache, FR-AI3). */
    suspend fun dailyReflection(dateEpochDay: Long): Insight?

    fun observeAll(): Flow<List<Insight>>

    suspend fun forDay(dateEpochDay: Long): List<Insight>

    /** Removes a day's daily reflection so it can be regenerated (on-demand refresh). */
    suspend fun deleteDailyReflection(dateEpochDay: Long)

    /** "Delete all data" (PRD §10.10). */
    suspend fun clear()
}

/** Convenience: is [type] the daily-reflection type (used by cache lookups). */
fun InsightType.isDailyReflection(): Boolean = this == InsightType.DAILY_REFLECTION

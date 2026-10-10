package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.InterventionLog
import com.anchor.app.core.domain.model.InterventionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Intervention repository (docs/superpowers/design/02-architecture.md §3.1). Owns the
 * `intervention_log` table and the queries that back the daily quota and per-context cooldown
 * (docs 03 §5.1, docs 04 §3.1).
 */
interface InterventionRepository {

    suspend fun insert(log: InterventionLog)

    suspend fun getById(id: String): InterventionLog?

    fun observeAll(): Flow<List<InterventionLog>>

    /** Meaningful interventions (INTERACTED/DISMISSED) shown in `[startOfDay, endOfDay)`. */
    suspend fun quotaUsedBetween(startOfDay: Long, endOfDay: Long): Int

    /** Timestamp of the newest "Later" resolution for [contextPackage], or null. */
    suspend fun latestCooldownAt(contextPackage: String): Long?

    /** Count of meaningful interventions already at/above [level] (for escalation pacing). */
    suspend fun countMeaningfulAtOrAbove(level: Int): Int

    /**
     * Meaningful interventions at/above [level] within `[startOfDay, endOfDay)` — used so
     * escalation resets each local day rather than being permanently unlocked by all-time history.
     */
    suspend fun countMeaningfulAtOrAboveBetween(level: Int, startOfDay: Long, endOfDay: Long): Int

    /**
     * Timestamp of the newest *unresolved* (EXPIRED) intervention for [contextPackage], or null.
     * Backs the per-context re-show back-off that prevents re-notifying an ignored signal.
     */
    suspend fun latestUnresolvedShownAt(contextPackage: String): Long?

    /** Most recent meaningful intervention timestamp, or null (FR-I2 pacing). */
    suspend fun latestMeaningfulShownAt(): Long?

    /** Sets the terminal [status] + `resolvedAt`, and flags a cooldown when the user said Later. */
    suspend fun resolve(
        id: String,
        status: InterventionStatus,
        resolvedAt: Long,
        cooldownTriggered: Boolean,
    )

    /** "Delete all data" (PRD §10.10) — clears the intervention log. */
    suspend fun clear()
}

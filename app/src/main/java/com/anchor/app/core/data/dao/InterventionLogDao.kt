package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.app.core.data.entity.InterventionLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [InterventionLogEntity] (docs/superpowers/design/03-database-schema.md §5.1).
 *
 * Quota is computed in the local day window `[startOfDay, endOfDay)`; note `EXPIRED` rows are
 * excluded (PRD §10.4 rule 2/3). Cooldown uses the most recent "Later" row per context package.
 */
@Dao
interface InterventionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: InterventionLogEntity)

    @Query("SELECT * FROM intervention_log WHERE id = :id")
    suspend fun getById(id: String): InterventionLogEntity?

    @Query("SELECT * FROM intervention_log ORDER BY shownAt DESC")
    fun observeAll(): Flow<List<InterventionLogEntity>>

    /**
     * Quota-used-today: meaningful interventions (INTERACTED or DISMISSED) shown within the
     * given local-day window. `EXPIRED` is intentionally excluded.
     */
    @Query(
        """
        SELECT COUNT(*) FROM intervention_log
        WHERE status IN ('INTERACTED', 'DISMISSED')
          AND shownAt >= :startOfDay AND shownAt < :endOfDay
        """,
    )
    suspend fun quotaUsedBetween(startOfDay: Long, endOfDay: Long): Int

    /**
     * The latest "Later" resolution for [contextPackage] (drives the per-context cooldown).
     * Returns the resolvedAt of the newest such row, or null when none exists.
     */
    @Query(
        """
        SELECT MAX(resolvedAt) FROM intervention_log
        WHERE contextPackage = :contextPackage AND cooldownTriggered = 1
        """,
    )
    suspend fun latestCooldownAt(contextPackage: String): Long?

    /** Escalation history: does a meaningful intervention already exist at or above [level]? */
    @Query(
        """
        SELECT COUNT(*) FROM intervention_log
        WHERE status IN ('INTERACTED', 'DISMISSED') AND level >= :level
        """,
    )
    suspend fun countMeaningfulAtOrAbove(level: Int): Int

    /**
     * Meaningful interventions (INTERACTED/DISMISSED) at/above [level] within the local-day
     * window `[startOfDay, endOfDay)`. Escalation pacing is per-day so the user always starts a
     * new day at a gentle level instead of the all-time history permanently unlocking Level 4.
     */
    @Query(
        """
        SELECT COUNT(*) FROM intervention_log
        WHERE status IN ('INTERACTED', 'DISMISSED')
          AND level >= :level
          AND shownAt >= :startOfDay AND shownAt < :endOfDay
        """,
    )
    suspend fun countMeaningfulAtOrAboveBetween(level: Int, startOfDay: Long, endOfDay: Long): Int

    /**
     * Timestamp of the most recent intervention shown for [contextPackage] that the user has not
     * acted on (status still `EXPIRED`), or null. Used to stop re-notifying an *ignored* signal
     * every worker cycle. Once the user acts, the row leaves EXPIRED and this returns null, so a
     * fresh intervention for the context is allowed again.
     */
    @Query(
        """
        SELECT MAX(shownAt) FROM intervention_log
        WHERE contextPackage = :contextPackage AND status = 'EXPIRED'
        """,
    )
    suspend fun latestUnresolvedShownAt(contextPackage: String): Long?

    /** Most recent meaningful intervention timestamp (escalation pacing), or null. */
    @Query(
        """
        SELECT MAX(shownAt) FROM intervention_log
        WHERE status IN ('INTERACTED', 'DISMISSED')
        """,
    )
    suspend fun latestMeaningfulShownAt(): Long?

    /** Idempotent outcome update; leaves other columns untouched. */
    @Query(
        """
        UPDATE intervention_log
        SET status = :status, resolvedAt = :resolvedAt, cooldownTriggered = :cooldownTriggered
        WHERE id = :id
        """,
    )
    suspend fun resolve(
        id: String,
        status: String,
        resolvedAt: Long,
        cooldownTriggered: Boolean,
    )

    @Query("DELETE FROM intervention_log")
    suspend fun clear()
}

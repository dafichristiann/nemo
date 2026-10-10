package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.anchor.app.core.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [TaskEntity] (docs/superpowers/design/03-database-schema.md §2.2).
 *
 * Reads are exposed as [Flow] for reactive UI (PRD FR-T10: All/Today/Upcoming/Completed).
 * "Done" covers both DONE and ARCHIVED; open tasks are TODO/IN_PROGRESS.
 */
@Dao
interface TaskDao {

    /** All non-archived tasks, most urgent first (All view). */
    @Query(
        """
        SELECT * FROM task_instance
        WHERE status != 'ARCHIVED'
        ORDER BY
            CASE status WHEN 'DONE' THEN 2 ELSE 1 END,
            CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2
                          WHEN 'LOW' THEN 3 ELSE 4 END,
            deadline IS NULL,
            deadline ASC,
            createdAt DESC
        """,
    )
    fun observeAll(): Flow<List<TaskEntity>>

    /**
     * One-shot snapshot of the same rows as [observeAll]. Use this (not `observeAll().first()`)
     * for background logic like drift detection: a Room Flow may transiently emit an emptied list
     * during invalidation, and `.first()` could capture that spurious empty result.
     */
    @Query(
        """
        SELECT * FROM task_instance
        WHERE status != 'ARCHIVED'
        ORDER BY
            CASE status WHEN 'DONE' THEN 2 ELSE 1 END,
            CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2
                          WHEN 'LOW' THEN 3 ELSE 4 END,
            deadline IS NULL,
            deadline ASC,
            createdAt DESC
        """,
    )
    suspend fun getAllOnce(): List<TaskEntity>

    /**
     * Tasks shown in the Today view: open tasks whose deadline falls within
     * [startOfDay, endOfDay), PLUS a recurring instance issued for today
     * (`definitionOccurrenceDate == :todayEpochDay`) even when it carries no deadline
     * (e.g. "Morning run", DAILY) — PRD FR-T7/FR-T10, hardening F2.
     */
    @Query(
        """
        SELECT * FROM task_instance
        WHERE status IN ('TODO','IN_PROGRESS')
          AND (
                (deadline IS NOT NULL AND deadline >= :startOfDay AND deadline < :endOfDay)
                OR definitionOccurrenceDate = :todayEpochDay
              )
        ORDER BY
            CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2
                          WHEN 'LOW' THEN 3 ELSE 4 END,
            deadline IS NULL, deadline ASC, createdAt DESC
        """,
    )
    fun observeToday(startOfDay: Long, endOfDay: Long, todayEpochDay: Long): Flow<List<TaskEntity>>

    /** Open tasks with a deadline after [endOfDay] (Upcoming view). */
    @Query(
        """
        SELECT * FROM task_instance
        WHERE status IN ('TODO','IN_PROGRESS')
          AND deadline IS NOT NULL
          AND deadline >= :endOfDay
        ORDER BY deadline ASC,
            CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2
                          WHEN 'LOW' THEN 3 ELSE 4 END,
            createdAt DESC
        """,
    )
    fun observeUpcoming(endOfDay: Long): Flow<List<TaskEntity>>

    /** Completed tasks, newest completion first (Completed view). */
    @Query(
        """
        SELECT * FROM task_instance
        WHERE status = 'DONE'
        ORDER BY completedAt DESC, createdAt DESC
        """,
    )
    fun observeCompleted(): Flow<List<TaskEntity>>

    /**
     * Overdue open tasks: deadline strictly before [now] and not done (PRD FR-T11).
     * Deadline in the past is a soft signal only.
     */
    @Query(
        """
        SELECT * FROM task_instance
        WHERE status IN ('TODO','IN_PROGRESS')
          AND deadline IS NOT NULL
          AND deadline < :now
        ORDER BY deadline ASC
        """,
    )
    fun observeOverdue(now: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task_instance WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM task_instance WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM task_instance WHERE status = 'DONE'")
    fun observeCompletedCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM task_instance
        WHERE status IN ('TODO','IN_PROGRESS')
        """,
    )
    fun observeOpenCount(): Flow<Int>

    /** Idempotency guard for recurring generation (PRD §10.1, AC "no double instance"). */
    @Query(
        """
        SELECT COUNT(*) FROM task_instance
        WHERE definitionId = :definitionId
          AND definitionOccurrenceDate = :occurrenceDate
        """,
    )
    suspend fun countForDefinitionOnDate(definitionId: String, occurrenceDate: Long): Int

    /**
     * Most recent instance for a recurring definition (by occurrence date) at/before [upToEpochDay],
     * or null. Used to preserve the previous instance's deadline time-of-day when issuing the next
     * one, so a recurring task does not silently jump to 23:59 every cycle.
     */
    @Query(
        """
        SELECT * FROM task_instance
        WHERE definitionId = :definitionId
          AND definitionOccurrenceDate IS NOT NULL
          AND definitionOccurrenceDate <= :upToEpochDay
        ORDER BY definitionOccurrenceDate DESC
        LIMIT 1
        """,
    )
    suspend fun latestInstanceForDefinition(definitionId: String, upToEpochDay: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity)

    /**
     * Insert a recurring instance, ignoring the write if an instance for the same
     * `(definitionId, definitionOccurrenceDate)` already exists. Backed by a partial unique
     * index, this makes the "no double instance" guarantee atomic — closing the check-then-act
     * race between the Tasks screen and the recurring Worker (PRD FR-T7). Returns the rowId,
     * or -1 when the insert was ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicate(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("DELETE FROM task_instance WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM task_instance")
    suspend fun clear()
}

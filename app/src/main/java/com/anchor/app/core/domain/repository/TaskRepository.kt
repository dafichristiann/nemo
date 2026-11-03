package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.CompletionSource
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Task repository interface (docs 02-architecture.md §3.1). `feature` depends only on
 * this; the Room-backed implementation lives in `core/data`.
 *
 * `startOfDay`/`endOfDay`/`now` are passed in by callers (computed via
 * [com.anchor.app.core.common.TimeProvider]) so "today" is always deterministic.
 */
interface TaskRepository {

    fun observeAll(): Flow<List<Task>>

    /**
     * One-shot snapshot of all non-archived tasks. Prefer this over `observeAll().first()` in
     * background logic: a Room Flow can transiently emit an empty list during invalidation, so
     * `.first()` may return a spurious empty snapshot (e.g. drift detection silently missing).
     */
    suspend fun getAllOnce(): List<Task>

    fun observeToday(startOfDay: Long, endOfDay: Long, todayEpochDay: Long): Flow<List<Task>>

    fun observeUpcoming(endOfDay: Long): Flow<List<Task>>

    fun observeCompleted(): Flow<List<Task>>

    fun observeOverdue(now: Long): Flow<List<Task>>

    fun observeById(id: String): Flow<Task?>

    fun observeCompletedCount(): Flow<Int>

    fun observeTodayCompletedCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    fun observeOpenCount(): Flow<Int>

    suspend fun getById(id: String): Task?

    suspend fun insert(task: Task)

    /**
     * Insert a recurring instance, atomically ignoring it when an instance for the same
     * `(definitionId, definitionOccurrenceDate)` already exists. Returns true when a new row
     * was written, false when the write was a no-op (duplicate). Backed by a unique index so
     * the check-then-act race between the Tasks screen and the recurring Worker cannot create
     * a double instance (FR-T7).
     */
    suspend fun insertIgnoringDuplicate(task: Task): Boolean

    suspend fun update(task: Task)

    suspend fun delete(id: String)

    /** Marks a task DONE and records `completedAt` + how it was completed (FR-T8, PRD v0.5 §10.1). */
    suspend fun markDone(
        id: String,
        completedAt: Long,
        source: CompletionSource = CompletionSource.MANUAL,
    )

    suspend fun setStatus(id: String, status: com.anchor.app.core.domain.model.TaskStatus)

    suspend fun setProgress(id: String, progressPercent: Int)

    /** Recurring generation idempotency check (FR-T7). */
    suspend fun countForDefinitionOnDate(definitionId: String, occurrenceDate: Long): Int

    /**
     * Most recent instance for a recurring definition (by occurrence date) at/before
     * [upToEpochDay], or null — used to carry forward the deadline time-of-day.
     */
    suspend fun latestInstanceForDefinition(definitionId: String, upToEpochDay: Long): Task?

    suspend fun clear()

    // --- Subtasks (FR-T6, FR-T9) ---
    fun observeSubtasks(taskId: String): Flow<List<Subtask>>

    suspend fun getSubtasks(taskId: String): List<Subtask>

    suspend fun addSubtask(subtask: Subtask)

    suspend fun addSubtasks(subtasks: List<Subtask>)

    suspend fun updateSubtask(subtask: Subtask)

    suspend fun deleteSubtask(id: String)
}

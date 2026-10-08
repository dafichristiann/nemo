package com.anchor.app.core.domain.repository

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

    fun observeToday(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    fun observeUpcoming(endOfDay: Long): Flow<List<Task>>

    fun observeCompleted(): Flow<List<Task>>

    fun observeOverdue(now: Long): Flow<List<Task>>

    fun observeById(id: String): Flow<Task?>

    fun observeCompletedCount(): Flow<Int>

    fun observeOpenCount(): Flow<Int>

    suspend fun getById(id: String): Task?

    suspend fun insert(task: Task)

    suspend fun update(task: Task)

    suspend fun delete(id: String)

    /** Marks a task DONE and records `completedAt` (FR-T8). */
    suspend fun markDone(id: String, completedAt: Long)

    suspend fun setStatus(id: String, status: com.anchor.app.core.domain.model.TaskStatus)

    suspend fun setProgress(id: String, progressPercent: Int)

    /** Recurring generation idempotency check (FR-T7). */
    suspend fun countForDefinitionOnDate(definitionId: String, occurrenceDate: Long): Int

    suspend fun clear()

    // --- Subtasks (FR-T6, FR-T9) ---
    fun observeSubtasks(taskId: String): Flow<List<Subtask>>

    suspend fun getSubtasks(taskId: String): List<Subtask>

    suspend fun addSubtask(subtask: Subtask)

    suspend fun addSubtasks(subtasks: List<Subtask>)

    suspend fun updateSubtask(subtask: Subtask)

    suspend fun deleteSubtask(id: String)
}

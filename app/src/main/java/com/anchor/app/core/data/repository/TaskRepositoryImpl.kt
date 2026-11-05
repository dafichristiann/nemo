package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.SubtaskDao
import com.anchor.app.core.data.dao.TaskDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.CompletionSource
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.platform.notification.TaskReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [TaskRepository] (docs 02-architecture.md §3.1). */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val taskReminderScheduler: TaskReminderScheduler? = null,
) : TaskRepository {

    override fun observeAll(): Flow<List<Task>> =
        taskDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllOnce(): List<Task> =
        taskDao.getAllOnce().map { it.toDomain() }

    override fun observeToday(startOfDay: Long, endOfDay: Long, todayEpochDay: Long): Flow<List<Task>> =
        taskDao.observeToday(startOfDay, endOfDay, todayEpochDay).map { list -> list.map { it.toDomain() } }

    override fun observeUpcoming(endOfDay: Long): Flow<List<Task>> =
        taskDao.observeUpcoming(endOfDay).map { list -> list.map { it.toDomain() } }

    override fun observeCompleted(): Flow<List<Task>> =
        taskDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    override fun observeOverdue(now: Long): Flow<List<Task>> =
        taskDao.observeOverdue(now).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Task?> =
        taskDao.observeById(id).map { it?.toDomain() }

    override fun observeCompletedCount(): Flow<Int> = taskDao.observeCompletedCount()

    override fun observeTodayCompletedCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        taskDao.observeTodayCompletedCount(startOfDay, endOfDay)

    override fun observeOpenCount(): Flow<Int> = taskDao.observeOpenCount()

    override suspend fun getById(id: String): Task? = taskDao.getById(id)?.toDomain()

    override suspend fun insert(task: Task) {
        taskDao.insert(task.toEntity())
        taskReminderScheduler?.scheduleReminder(task)
    }

    override suspend fun insertIgnoringDuplicate(task: Task): Boolean {
        val inserted = taskDao.insertIgnoringDuplicate(task.toEntity()) != -1L
        if (inserted) {
            taskReminderScheduler?.scheduleReminder(task)
        }
        return inserted
    }

    override suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
        if (task.isDone) {
            taskReminderScheduler?.cancelReminder(task.id)
        } else {
            taskReminderScheduler?.scheduleReminder(task)
        }
    }

    override suspend fun delete(id: String) {
        taskDao.deleteById(id)
        taskReminderScheduler?.cancelReminder(id)
    }

    override suspend fun markDone(id: String, completedAt: Long, source: CompletionSource) {
        val current = taskDao.getById(id) ?: return
        taskDao.update(
            current.copy(
                status = TaskStatus.DONE.name,
                completedAt = completedAt,
                progressPercent = 100,
                completionSource = source.name,
            ),
        )
        taskReminderScheduler?.cancelReminder(id)
    }

    override suspend fun setStatus(id: String, status: TaskStatus) {
        val current = taskDao.getById(id) ?: return
        if (status == TaskStatus.DONE) {
            taskDao.update(
                current.copy(
                    status = status.name,
                    completedAt = current.completedAt ?: System.currentTimeMillis(),
                    progressPercent = 100,
                ),
            )
            taskReminderScheduler?.cancelReminder(id)
        } else {
            val totalSubtasks = subtaskDao.countForTask(id)
            val newProgress = if (totalSubtasks > 0) {
                val doneSubtasks = subtaskDao.countDoneForTask(id)
                ((doneSubtasks.toDouble() / totalSubtasks) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            val updated = current.copy(
                status = status.name,
                completedAt = null,
                completionSource = null,
                progressPercent = newProgress,
            )
            taskDao.update(updated)
            taskReminderScheduler?.scheduleReminder(updated.toDomain())
        }
    }

    override suspend fun setProgress(id: String, progressPercent: Int) {
        val current = taskDao.getById(id) ?: return
        taskDao.update(current.copy(progressPercent = progressPercent.coerceIn(0, 100)))
    }

    override suspend fun countForDefinitionOnDate(definitionId: String, occurrenceDate: Long): Int =
        taskDao.countForDefinitionOnDate(definitionId, occurrenceDate)

    override suspend fun latestInstanceForDefinition(definitionId: String, upToEpochDay: Long): Task? =
        taskDao.latestInstanceForDefinition(definitionId, upToEpochDay)?.toDomain()

    override suspend fun clear() {
        subtaskDao.clear()
        taskDao.clear()
    }

    override fun observeSubtasks(taskId: String): Flow<List<Subtask>> =
        subtaskDao.observeForTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSubtasks(taskId: String): List<Subtask> =
        subtaskDao.getForTask(taskId).map { it.toDomain() }

    override suspend fun addSubtask(subtask: Subtask) = subtaskDao.insert(subtask.toEntity())

    override suspend fun addSubtasks(subtasks: List<Subtask>) =
        subtaskDao.insertAll(subtasks.map { it.toEntity() })

    override suspend fun updateSubtask(subtask: Subtask) = subtaskDao.update(subtask.toEntity())

    override suspend fun deleteSubtask(id: String) = subtaskDao.deleteById(id)
}

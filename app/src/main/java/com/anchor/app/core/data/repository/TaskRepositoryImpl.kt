package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.SubtaskDao
import com.anchor.app.core.data.dao.TaskDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [TaskRepository] (docs 02-architecture.md §3.1). */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
) : TaskRepository {

    override fun observeAll(): Flow<List<Task>> =
        taskDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeToday(startOfDay: Long, endOfDay: Long): Flow<List<Task>> =
        taskDao.observeToday(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }

    override fun observeUpcoming(endOfDay: Long): Flow<List<Task>> =
        taskDao.observeUpcoming(endOfDay).map { list -> list.map { it.toDomain() } }

    override fun observeCompleted(): Flow<List<Task>> =
        taskDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    override fun observeOverdue(now: Long): Flow<List<Task>> =
        taskDao.observeOverdue(now).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Task?> =
        taskDao.observeById(id).map { it?.toDomain() }

    override fun observeCompletedCount(): Flow<Int> = taskDao.observeCompletedCount()

    override fun observeOpenCount(): Flow<Int> = taskDao.observeOpenCount()

    override suspend fun getById(id: String): Task? = taskDao.getById(id)?.toDomain()

    override suspend fun insert(task: Task) = taskDao.insert(task.toEntity())

    override suspend fun update(task: Task) = taskDao.update(task.toEntity())

    override suspend fun delete(id: String) = taskDao.deleteById(id)

    override suspend fun markDone(id: String, completedAt: Long) {
        val current = taskDao.getById(id) ?: return
        taskDao.update(
            current.copy(
                status = TaskStatus.DONE.name,
                completedAt = completedAt,
                progressPercent = 100,
            ),
        )
    }

    override suspend fun setStatus(id: String, status: TaskStatus) {
        val current = taskDao.getById(id) ?: return
        taskDao.update(current.copy(status = status.name))
    }

    override suspend fun setProgress(id: String, progressPercent: Int) {
        val current = taskDao.getById(id) ?: return
        taskDao.update(current.copy(progressPercent = progressPercent.coerceIn(0, 100)))
    }

    override suspend fun countForDefinitionOnDate(definitionId: String, occurrenceDate: Long): Int =
        taskDao.countForDefinitionOnDate(definitionId, occurrenceDate)

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

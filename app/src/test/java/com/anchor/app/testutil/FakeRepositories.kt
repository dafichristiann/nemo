package com.anchor.app.testutil

import com.anchor.app.core.domain.model.CompletionSource
import com.anchor.app.core.domain.model.RescheduleEvent
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskDefinition
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [TaskRepository] fake for unit tests (docs 11-testing-strategy.md §5:
 * "Fake repositories for unit test (bukan Room)"). Mirrors the real query semantics
 * closely enough to exercise the use cases deterministically.
 */
class FakeTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val subtasks = MutableStateFlow<List<Subtask>>(emptyList())

    override fun observeAll(): Flow<List<Task>> = tasks

    override suspend fun getAllOnce(): List<Task> = tasks.value

    override fun observeToday(startOfDay: Long, endOfDay: Long, todayEpochDay: Long): Flow<List<Task>> = tasks.map { list ->
        list.filter {
            it.status in OPEN &&
                (
                    (it.deadline != null && it.deadline!! >= startOfDay && it.deadline!! < endOfDay) ||
                        it.definitionOccurrenceDate == todayEpochDay
                    )
        }
    }

    override fun observeUpcoming(endOfDay: Long): Flow<List<Task>> = tasks.map { list ->
        list.filter { it.status in OPEN && it.deadline != null && it.deadline!! >= endOfDay }
    }

    override fun observeCompleted(): Flow<List<Task>> =
        tasks.map { list -> list.filter { it.status == TaskStatus.DONE } }

    override fun observeOverdue(now: Long): Flow<List<Task>> = tasks.map { list ->
        list.filter { it.status in OPEN && it.deadline != null && it.deadline!! < now }
    }

    override fun observeById(id: String): Flow<Task?> = tasks.map { list -> list.find { it.id == id } }

    override fun observeCompletedCount(): Flow<Int> =
        tasks.map { list -> list.count { it.status == TaskStatus.DONE } }

    override fun observeOpenCount(): Flow<Int> =
        tasks.map { list -> list.count { it.status in OPEN } }

    override suspend fun getById(id: String): Task? = tasks.value.find { it.id == id }

    override suspend fun insert(task: Task) {
        tasks.value = tasks.value + task
    }

    override suspend fun insertIgnoringDuplicate(task: Task): Boolean {
        if (task.definitionId != null && task.definitionOccurrenceDate != null) {
            val duplicate = tasks.value.any {
                it.definitionId == task.definitionId &&
                    it.definitionOccurrenceDate == task.definitionOccurrenceDate
            }
            if (duplicate) return false
        }
        tasks.value = tasks.value + task
        return true
    }

    override suspend fun update(task: Task) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun delete(id: String) {
        tasks.value = tasks.value.filterNot { it.id == id }
    }

    override suspend fun markDone(id: String, completedAt: Long, source: CompletionSource) {
        // Mirror the real repo: a missing task is a safe no-op (not an exception).
        val current = tasks.value.firstOrNull { it.id == id } ?: return
        update(
            current.copy(
                status = TaskStatus.DONE,
                completedAt = completedAt,
                progressPercent = 100,
                completionSource = source,
            ),
        )
    }

    override suspend fun setStatus(id: String, status: TaskStatus) {
        update(tasks.value.first { it.id == id }.copy(status = status))
    }

    override suspend fun setProgress(id: String, progressPercent: Int) {
        update(tasks.value.first { it.id == id }.copy(progressPercent = progressPercent))
    }

    override suspend fun countForDefinitionOnDate(definitionId: String, occurrenceDate: Long): Int =
        tasks.value.count { it.definitionId == definitionId && it.definitionOccurrenceDate == occurrenceDate }

    override suspend fun latestInstanceForDefinition(definitionId: String, upToEpochDay: Long): Task? =
        tasks.value
            .filter { it.definitionId == definitionId && it.definitionOccurrenceDate != null && it.definitionOccurrenceDate <= upToEpochDay }
            .maxByOrNull { it.definitionOccurrenceDate!! }

    override suspend fun clear() {
        tasks.value = emptyList()
        subtasks.value = emptyList()
    }

    override fun observeSubtasks(taskId: String): Flow<List<Subtask>> =
        subtasks.map { list -> list.filter { it.taskId == taskId }.sortedBy { it.orderIndex } }

    override suspend fun getSubtasks(taskId: String): List<Subtask> =
        subtasks.value.filter { it.taskId == taskId }.sortedBy { it.orderIndex }

    override suspend fun addSubtask(subtask: Subtask) {
        subtasks.value = subtasks.value + subtask
    }

    override suspend fun addSubtasks(subtasks: List<Subtask>) {
        this.subtasks.value = this.subtasks.value + subtasks
    }

    override suspend fun updateSubtask(subtask: Subtask) {
        subtasks.value = subtasks.value.map { if (it.id == subtask.id) subtask else it }
    }

    override suspend fun deleteSubtask(id: String) {
        subtasks.value = subtasks.value.filterNot { it.id == id }
    }

    private companion object {
        val OPEN = setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS)
    }
}

/** In-memory [TaskDefinitionRepository] fake for unit tests. */
class FakeTaskDefinitionRepository : TaskDefinitionRepository {
    val definitions = MutableStateFlow<List<TaskDefinition>>(emptyList())
    val reschedules = MutableStateFlow<List<RescheduleEvent>>(emptyList())

    override fun observeActiveDefinitions(): Flow<List<TaskDefinition>> =
        definitions.map { list -> list.filter { it.archivedAt == null } }

    override suspend fun getRecurringDefinitions(): List<TaskDefinition> =
        definitions.value.filter { it.archivedAt == null && it.recurrenceRule != com.anchor.app.core.domain.model.RecurrenceRule.NONE }

    override suspend fun getById(id: String): TaskDefinition? = definitions.value.find { it.id == id }

    override suspend fun insert(definition: TaskDefinition) {
        definitions.value = definitions.value + definition
    }

    override suspend fun upsert(definition: TaskDefinition) {
        definitions.value = definitions.value.filterNot { it.id == definition.id } + definition
    }

    override fun observeRescheduleEvents(taskId: String): Flow<List<RescheduleEvent>> =
        reschedules.map { list -> list.filter { it.taskId == taskId } }

    override fun observeRescheduleCount(): Flow<Int> = reschedules.map { it.size }

    override suspend fun recordReschedule(event: RescheduleEvent) {
        reschedules.value = reschedules.value + event
    }

    override suspend fun clear() {
        definitions.value = emptyList()
        reschedules.value = emptyList()
    }
}

package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.RecurrenceRule
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskDefinition
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import javax.inject.Inject

/** Input for creating a task (PRD FR-T1..T5). Title is the only required field. */
data class CreateTaskInput(
    val title: String,
    val description: String? = null,
    val deadline: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val estimatedDurationMin: Int? = null,
    val category: String? = null,
    val recurrenceRule: RecurrenceRule = RecurrenceRule.NONE,
    val subtaskTitles: List<String> = emptyList(),
)

/** Why a create attempt failed, if it did. */
sealed interface CreateTaskResult {
    data class Success(val taskId: String) : CreateTaskResult
    data object EmptyTitle : CreateTaskResult
}

/**
 * Creates a task, optionally as a **recurring** template (FR-T7).
 *
 * When [CreateTaskInput.recurrenceRule] is not NONE, a [TaskDefinition] is stored and a
 * first [Task] instance is issued immediately, linked to the definition and stamped with
 * its occurrence date (start-of-day of the deadline, or today). This keeps the template
 * intact while instance history accumulates (PRD §10.1).
 *
 * Empty titles are rejected (PRD §10.1 Edge Cases: "Judul kosong → tidak bisa disimpan").
 */
class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val definitionRepository: TaskDefinitionRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(input: CreateTaskInput): CreateTaskResult {
        val title = input.title.trim()
        if (title.isEmpty()) return CreateTaskResult.EmptyTitle

        val now = timeProvider.nowMillis()
        val taskId = idProvider.newId()

        val definitionId = if (input.recurrenceRule != RecurrenceRule.NONE) {
            val definition = TaskDefinition(
                id = idProvider.newId(),
                title = title,
                description = input.description,
                defaultPriority = input.priority,
                defaultDurationMin = input.estimatedDurationMin,
                category = input.category,
                recurrenceRule = input.recurrenceRule,
                createdAt = now,
            )
            definitionRepository.insert(definition)
            definition.id
        } else {
            null
        }

        // Occurrence key is a LOCAL EPOCH DAY (not ms), matching GenerateRecurringTasksUseCase's
        // idempotency guard (`today.toEpochDay()`). It is TODAY — the generation day — not the
        // deadline's day: a recurring task created with a future deadline must not be stamped with
        // the deadline's day, or the generator would also issue a second instance for today
        // (PRD FR-T7, "no double instance").
        val occurrenceDate = if (definitionId != null) {
            timeProvider.todayEpochDay()
        } else {
            null
        }

        val task = Task(
            id = taskId,
            definitionId = definitionId,
            title = title,
            description = input.description,
            deadline = input.deadline,
            priority = input.priority,
            estimatedDurationMin = input.estimatedDurationMin,
            category = input.category,
            createdAt = now,
            definitionOccurrenceDate = occurrenceDate,
        )
        taskRepository.insert(task)

        if (input.subtaskTitles.isNotEmpty()) {
            val subtasks = input.subtaskTitles
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapIndexed { index, subTitle ->
                    Subtask(
                        id = idProvider.newId(),
                        taskId = taskId,
                        title = subTitle,
                        orderIndex = index,
                    )
                }
            taskRepository.addSubtasks(subtasks)
            // Progress reflects subtasks from the start (FR-T9).
            taskRepository.setProgress(taskId, 0)
        }

        return CreateTaskResult.Success(taskId)
    }
}

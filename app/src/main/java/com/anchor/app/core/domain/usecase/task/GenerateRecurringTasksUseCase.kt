package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.RecurrenceRule
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Issues new [Task] instances from recurring [com.anchor.app.core.domain.model.TaskDefinition]s
 * (PRD FR-T7, §10.1 Recurring Task Architecture).
 *
 * Rules: **DAILY** (every day) and **WEEKLY** (once per ISO week, Monday–Sunday).
 * Idempotent — a definition never produces two instances for the same occurrence date
 * (checked via `countForDefinitionOnDate`), which satisfies the "no double instance" AC.
 *
 * The deadline time-of-day is preserved from a previous instance when one exists;
 * otherwise it defaults to 23:59 on the occurrence date (domain heuristic, PRD §10.6).
 */
class GenerateRecurringTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val definitionRepository: TaskDefinitionRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
) {
    /**
     * Ensure an instance exists for [today] (default: device today) for every active
     * recurring definition. Returns the number of instances created.
     */
    suspend operator fun invoke(today: LocalDate = timeProvider.today()): Int {
        val definitions = definitionRepository.getRecurringDefinitions()
        if (definitions.isEmpty()) return 0

        var created = 0
        for (definition in definitions) {
            if (!isDue(definition.recurrenceRule, definition.id, today)) continue

            val occurrenceDate = today.toEpochDay()
            val alreadyIssued = taskRepository.countForDefinitionOnDate(definition.id, occurrenceDate)
            if (alreadyIssued > 0) continue

            val newTaskId = idProvider.newId()
            val inserted = taskRepository.insertIgnoringDuplicate(
                Task(
                    id = newTaskId,
                    definitionId = definition.id,
                    title = definition.title,
                    description = definition.description,
                    deadline = deadlineFor(definition.id, today),
                    priority = definition.defaultPriority,
                    estimatedDurationMin = definition.defaultDurationMin,
                    category = definition.category,
                    createdAt = timeProvider.nowMillis(),
                    definitionOccurrenceDate = occurrenceDate,
                ),
            )
            if (inserted) {
                created++
                val prevInstance = taskRepository.latestInstanceForDefinition(definition.id, occurrenceDate)
                    ?.takeIf { it.id != newTaskId }
                    ?: taskRepository.latestInstanceForDefinition(definition.id, occurrenceDate - 1)
                if (prevInstance != null) {
                    val prevSubtasks = taskRepository.getSubtasks(prevInstance.id)
                    if (prevSubtasks.isNotEmpty()) {
                        val clonedSubtasks = prevSubtasks.map { subtask ->
                            subtask.copy(
                                id = idProvider.newId(),
                                taskId = newTaskId,
                                isDone = false,
                            )
                        }
                        taskRepository.addSubtasks(clonedSubtasks)
                    }
                }
            }
        }
        return created
    }

    private suspend fun isDue(rule: RecurrenceRule, definitionId: String, today: LocalDate): Boolean =
        when (rule) {
            RecurrenceRule.DAILY -> true
            // Weekly = once per ISO week (Mon–Sun), not "same weekday as an instance found in the
            // last 7 days". The old `?: true` fallback re-issued an instance every single day when
            // no prior instance existed (e.g. after the user deleted it), producing duplicates.
            RecurrenceRule.WEEKLY -> !hasInstanceInIsoWeek(definitionId, today)
            RecurrenceRule.NONE -> false
        }

    /** True when an instance for [definitionId] already exists in [today]'s ISO week. */
    private suspend fun hasInstanceInIsoWeek(definitionId: String, today: LocalDate): Boolean {
        val monday = today.with(DayOfWeek.MONDAY)
        return (0..6).any { offset ->
            val epoch = monday.plusDays(offset.toLong()).toEpochDay()
            taskRepository.countForDefinitionOnDate(definitionId, epoch) > 0
        }
    }

    /**
     * Deadline for a new instance on [today]. The time-of-day is carried forward from the
     * definition's most recent prior instance when one exists (so a task scheduled for, say,
     * 08:00 keeps 08:00); otherwise it defaults to 23:59 on the occurrence date (PRD §10.6).
     */
    private suspend fun deadlineFor(definitionId: String, today: LocalDate): Long {
        val timeOfDay = previousDeadlineTimeOfDay(definitionId, today) ?: DEFAULT_DEADLINE_TIME
        return timeProvider.toMillis(today.atTime(timeOfDay))
    }

    /** Local time-of-day of the previous instance's deadline, or null when there is none / none set. */
    private suspend fun previousDeadlineTimeOfDay(
        definitionId: String,
        today: LocalDate,
    ): LocalTime? {
        val previous = taskRepository.latestInstanceForDefinition(definitionId, today.toEpochDay())
        val deadline = previous?.deadline ?: return null
        return timeProvider.toLocalTime(deadline)
    }

    private companion object {
        val DEFAULT_DEADLINE_TIME: LocalTime = LocalTime.of(23, 59)
    }
}

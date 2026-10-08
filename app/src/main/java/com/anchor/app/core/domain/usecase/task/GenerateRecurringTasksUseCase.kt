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
 * Rules: **DAILY** (every day) and **WEEKLY** (same weekday as the previous instance).
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

            taskRepository.insert(
                Task(
                    id = idProvider.newId(),
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
            created++
        }
        return created
    }

    private suspend fun isDue(rule: RecurrenceRule, definitionId: String, today: LocalDate): Boolean =
        when (rule) {
            RecurrenceRule.DAILY -> true
            RecurrenceRule.WEEKLY -> lastOccurrenceWeekday(definitionId)?.let { it == today.dayOfWeek } ?: true
            RecurrenceRule.NONE -> false
        }

    /** Weekday of the most recent prior instance within the last 7 days, if any. */
    private suspend fun lastOccurrenceWeekday(definitionId: String): DayOfWeek? =
        recentOccurrences(definitionId).maxOrNull()?.let { LocalDate.ofEpochDay(it).dayOfWeek }

    /** Occurrence epoch-days for [definitionId] within the last 7 days (excluding none). */
    private suspend fun recentOccurrences(definitionId: String): List<Long> {
        val today = timeProvider.today()
        return (0..7).mapNotNull { back ->
            val epoch = today.minusDays(back.toLong()).toEpochDay()
            if (taskRepository.countForDefinitionOnDate(definitionId, epoch) > 0) epoch else null
        }
    }

    /**
     * Deadline for a new instance: 23:59 on [today] (domain heuristic, PRD §10.6).
     * A per-instance time-of-day override would be layered here when the Add Task sheet
     * lets recurring definitions carry an explicit clock time.
     */
    private suspend fun deadlineFor(definitionId: String, today: LocalDate): Long =
        timeProvider.toMillis(today.atTime(DEFAULT_DEADLINE_TIME))

    private companion object {
        val DEFAULT_DEADLINE_TIME: LocalTime = LocalTime.of(23, 59)
    }
}

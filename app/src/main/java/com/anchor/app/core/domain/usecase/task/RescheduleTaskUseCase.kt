package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.RescheduleEvent
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Moves a task's deadline and records a [RescheduleEvent] for delay-pattern insight
 * (PRD FR-T12). Choosing "Keep for Today" keeps the task open with a new deadline.
 */
class RescheduleTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val definitionRepository: TaskDefinitionRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(taskId: String, newDeadline: Long?, reason: String? = null) {
        val task = taskRepository.getById(taskId) ?: return
        val from = task.deadline

        taskRepository.update(
            task.copy(
                deadline = newDeadline,
                // Rescheduling an overdue open task returns it to TODO (PRD §10.1 autonomy).
                status = if (task.status == TaskStatus.ARCHIVED) task.status else TaskStatus.TODO,
            ),
        )

        definitionRepository.recordReschedule(
            RescheduleEvent(
                id = idProvider.newId(),
                taskId = taskId,
                fromDeadline = from,
                toDeadline = newDeadline,
                reason = reason,
                createdAt = timeProvider.nowMillis(),
            ),
        )
    }
}

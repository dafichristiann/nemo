package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Marks a task DONE and records `completedAt` (PRD FR-T8).
 * Completion sets progress to 100 (docs 03 §2.2).
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(taskId: String) {
        taskRepository.markDone(taskId, timeProvider.nowMillis())
    }
}

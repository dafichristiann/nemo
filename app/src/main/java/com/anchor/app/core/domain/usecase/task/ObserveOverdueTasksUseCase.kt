package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes overdue open tasks for the non-blocking Home banner (PRD FR-T11).
 *
 * Overdue is only shown as a calm signal — the UI never auto-moves tasks. The user keeps
 * full autonomy (Reschedule / Complete / Keep / Dismiss).
 */
class ObserveOverdueTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(): Flow<List<Task>> = taskRepository.observeOverdue(timeProvider.nowMillis())
}

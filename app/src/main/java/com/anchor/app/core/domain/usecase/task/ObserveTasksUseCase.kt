package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Task list filters from PRD FR-T10. */
enum class TaskFilter { ALL, TODAY, UPCOMING, COMPLETED }

/**
 * Observes the task list for a given [TaskFilter] (PRD FR-T10).
 *
 * Day boundaries are computed from [TimeProvider] so "Today" always matches the device's
 * local day and is deterministic under test.
 */
class ObserveTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(filter: TaskFilter): Flow<List<Task>> {
        val startOfToday = timeProvider.startOfDayMillis(timeProvider.nowMillis())
        val endOfToday = startOfToday + ONE_DAY_MS

        return when (filter) {
            TaskFilter.ALL -> taskRepository.observeAll()
            TaskFilter.TODAY -> taskRepository.observeToday(startOfToday, endOfToday)
            TaskFilter.UPCOMING -> taskRepository.observeUpcoming(endOfToday)
            TaskFilter.COMPLETED -> taskRepository.observeCompleted()
        }
    }

    private companion object {
        const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    }
}

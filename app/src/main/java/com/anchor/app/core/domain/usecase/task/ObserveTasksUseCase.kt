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
        val now = timeProvider.nowMillis()
        val startOfToday = timeProvider.startOfDayMillis(now)
        // Derive the end of day from the next local midnight (not start + 24h) so the window stays
        // correct on DST-transition days, when a local day is 23 or 25 hours long.
        val endOfToday = timeProvider.endOfDayMillis(now)
        val todayEpochDay = timeProvider.epochDayOf(now)

        return when (filter) {
            TaskFilter.ALL -> taskRepository.observeAll()
            TaskFilter.TODAY -> taskRepository.observeToday(startOfToday, endOfToday, todayEpochDay)
            TaskFilter.UPCOMING -> taskRepository.observeUpcoming(endOfToday)
            TaskFilter.COMPLETED -> taskRepository.observeCompleted()
        }
    }
}

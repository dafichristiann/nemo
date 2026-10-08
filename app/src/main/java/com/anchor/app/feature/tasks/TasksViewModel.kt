package com.anchor.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DateTimeFormatter
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.RecurrenceRule
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.usecase.task.CompleteTaskUseCase
import com.anchor.app.core.domain.usecase.task.CreateTaskInput
import com.anchor.app.core.domain.usecase.task.CreateTaskResult
import com.anchor.app.core.domain.usecase.task.CreateTaskUseCase
import com.anchor.app.core.domain.usecase.task.GenerateRecurringTasksUseCase
import com.anchor.app.core.domain.usecase.task.ObserveOverdueTasksUseCase
import com.anchor.app.core.domain.usecase.task.ObserveTasksUseCase
import com.anchor.app.core.domain.usecase.task.RescheduleTaskUseCase
import com.anchor.app.core.domain.usecase.task.TaskFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row of the Home list, pre-formatted for display. */
data class TaskRowUi(
    val task: Task,
    val deadlineLabel: String?,
    val metaLabel: String?,
    val overdue: Boolean,
)

/** Aggregated Home screen state (PRD FR-T10). */
data class TasksUiState(
    val filter: TaskFilter = TaskFilter.TODAY,
    val rows: List<TaskRowUi> = emptyList(),
    val overdueCount: Int = 0,
    val today: java.time.LocalDate = java.time.LocalDate.now(),
    val zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
)

/**
 * Home (Smart Todo) ViewModel — PRD FR-T1..T12.
 *
 * Reads are reactive Flows from the domain layer; writes go through use cases. All day
 * math uses [TimeProvider] so behavior is deterministic (docs 11-testing-strategy.md §5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val observeTasks: ObserveTasksUseCase,
    private val observeOverdue: ObserveOverdueTasksUseCase,
    private val createTask: CreateTaskUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val rescheduleTask: RescheduleTaskUseCase,
    private val generateRecurring: GenerateRecurringTasksUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val filter = MutableStateFlow(TaskFilter.TODAY)

    val uiState: StateFlow<TasksUiState> = combine(
        filter.flatMapLatest { observeTasks(it) },
        observeOverdue(),
        filter,
    ) { tasks, overdue, currentFilter ->
        val now = timeProvider.nowMillis()
        val zone = timeProvider.zoneId()
        TasksUiState(
            filter = currentFilter,
            rows = tasks.map { it.toRowUi(now, zone) },
            overdueCount = overdue.size,
            today = timeProvider.today(),
            zone = zone,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TasksUiState(),
    )

    init {
        // Ensure today's recurring instances exist (idempotent) on first open (FR-T7).
        viewModelScope.launch(dispatchers.io) {
            runCatching { generateRecurring() }
        }
    }

    fun setFilter(newFilter: TaskFilter) {
        filter.value = newFilter
    }

    fun onCreateTask(
        title: String,
        description: String?,
        deadline: Long?,
        priority: TaskPriority,
        estimatedDurationMin: Int?,
        category: String?,
        subtaskTitles: List<String>,
        recurrenceRule: RecurrenceRule,
        onResult: (CreateTaskResult) -> Unit,
    ) {
        viewModelScope.launch(dispatchers.io) {
            val result = createTask(
                CreateTaskInput(
                    title = title,
                    description = description,
                    deadline = deadline,
                    priority = priority,
                    estimatedDurationMin = estimatedDurationMin,
                    category = category,
                    recurrenceRule = recurrenceRule,
                    subtaskTitles = subtaskTitles,
                ),
            )
            launch(dispatchers.main) { onResult(result) }
        }
    }

    fun onComplete(taskId: String) {
        viewModelScope.launch(dispatchers.io) { completeTask(taskId) }
    }

    fun onReschedule(taskId: String, newDeadline: Long?) {
        viewModelScope.launch(dispatchers.io) { rescheduleTask(taskId, newDeadline) }
    }

    /** "Keep for Today": push the deadline to today 23:59 and clear overdue (FR-T11). */
    fun onKeepForToday(taskId: String) {
        val endOfToday = timeProvider.startOfDayMillis(timeProvider.nowMillis()) +
            ONE_DAY_MS - 1
        onReschedule(taskId, endOfToday)
    }

    private fun Task.toRowUi(now: Long, zone: java.time.ZoneId): TaskRowUi {
        val deadlineText = deadline?.let { DateTimeFormatter.deadlineLabel(it, now, zone) }
        // Meta shows duration and (if there were subtasks) progress; the deadline is
        // rendered as the row subtitle, so it is not repeated here.
        val meta = buildList {
            DateTimeFormatter.durationLabel(estimatedDurationMin)?.let { add(it) }
        }.joinToString(" · ").ifEmpty { null }
        return TaskRowUi(
            task = this,
            deadlineLabel = deadlineText,
            metaLabel = meta,
            overdue = isOverdue(now),
        )
    }

    private companion object {
        const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    }
}

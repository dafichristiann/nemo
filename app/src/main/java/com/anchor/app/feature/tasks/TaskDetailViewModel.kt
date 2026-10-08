package com.anchor.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DateTimeFormatter
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.usecase.task.CompleteTaskUseCase
import com.anchor.app.core.domain.usecase.task.ManageSubtasksUseCase
import com.anchor.app.core.domain.usecase.task.RescheduleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Task Detail screen (07-screens.md §2.14). */
data class TaskDetailUiState(
    val task: Task? = null,
    val subtasks: List<Subtask> = emptyList(),
    val deadlineLabel: String? = null,
    val estimatedLabel: String? = null,
) {
    val doneSubtaskCount: Int get() = subtasks.count { it.isDone }
    val progressPercent: Int
        get() = if (subtasks.isEmpty()) task?.progressPercent ?: 0
        else (doneSubtaskCount * 100f / subtasks.size).toInt()
}

/** Task Detail ViewModel — PRD FR-T8, FR-T9, FR-T12. */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val completeTask: CompleteTaskUseCase,
    private val rescheduleTask: RescheduleTaskUseCase,
    private val manageSubtasks: ManageSubtasksUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val taskId: String = savedStateHandle.get<String>(ARG_TASK_ID).orEmpty()

    val uiState: StateFlow<TaskDetailUiState> = combine(
        taskRepository.observeById(taskId),
        taskRepository.observeSubtasks(taskId),
    ) { task, subtasks ->
        val now = timeProvider.nowMillis()
        val zone = timeProvider.zoneId()
        TaskDetailUiState(
            task = task,
            subtasks = subtasks,
            deadlineLabel = task?.deadline?.let { DateTimeFormatter.deadlineLabel(it, now, zone) },
            estimatedLabel = DateTimeFormatter.durationLabel(task?.estimatedDurationMin),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskDetailUiState(),
    )

    fun onComplete() = viewModelScope.launch(dispatchers.io) { completeTask(taskId) }

    fun onAddSubtask(title: String) =
        viewModelScope.launch(dispatchers.io) { manageSubtasks.add(taskId, title) }

    fun onToggleSubtask(subtask: Subtask) =
        viewModelScope.launch(dispatchers.io) { manageSubtasks.toggle(subtask) }

    fun onDeleteSubtask(subtask: Subtask) =
        viewModelScope.launch(dispatchers.io) { manageSubtasks.delete(subtask) }

    fun onReschedule(newDeadline: Long?) =
        viewModelScope.launch(dispatchers.io) { rescheduleTask(taskId, newDeadline) }

    fun onDelete() = viewModelScope.launch(dispatchers.io) { taskRepository.delete(taskId) }

    companion object {
        const val ARG_TASK_ID = "taskId"
    }
}

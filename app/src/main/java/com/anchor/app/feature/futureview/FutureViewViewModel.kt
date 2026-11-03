package com.anchor.app.feature.futureview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.DominoEffect
import com.anchor.app.core.domain.model.FutureView
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.repository.TaskDependencyRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.usecase.futureview.ComputeDominoEffectUseCase
import com.anchor.app.core.domain.usecase.futureview.FutureViewCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** UI state for screen 07 Future View + Domino (docs 07-screens.md §2.11, PRD §10.6-10.7). */
@androidx.compose.runtime.Immutable
data class FutureViewUiState(
    val task: Task? = null,
    val futureView: FutureView? = null,
    val domino: DominoEffect? = null,
    val zone: ZoneId = ZoneId.systemDefault(),
    val loading: Boolean = true,
    /** Other open tasks the user can pick to become affected (downstream) tasks. */
    val linkCandidates: List<Task> = emptyList(),
)

/**
 * ViewModel for Future View + Domino Effect (FR-FV1..FV5, FR-DE1..DE5). Assembles the two
 * scenarios (rule-based) and the dependency chain, then exposes them for the screen. All
 * projections are labelled with their assumptions by the calculator (no fabricated certainty).
 */
@HiltViewModel
class FutureViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val dependencyRepository: TaskDependencyRepository,
    private val futureViewCalculator: FutureViewCalculator,
    private val computeDomino: ComputeDominoEffectUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val taskId: String = savedStateHandle.get<String>(ARG_TASK_ID).orEmpty()

    private val _uiState = MutableStateFlow(FutureViewUiState())
    val uiState: StateFlow<FutureViewUiState> = _uiState.asStateFlow()

    private val _cycleMessage = MutableStateFlow(false)
    val cycleMessage: StateFlow<Boolean> = _cycleMessage.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            combine(
                taskRepository.observeById(taskId).distinctUntilChanged(),
                dependencyRepository.observeAll().distinctUntilChanged(),
                taskRepository.observeAll().distinctUntilChanged(),
            ) { task, _, allTasks ->
                val domino = computeDomino(taskId)
                val onChain = domino?.let { listOf(it.root.taskId) + it.affected.map { n -> n.taskId } }.orEmpty().toSet()
                val linkCandidates = allTasks.filter { it.id != taskId && it.id !in onChain }
                FutureViewUiState(
                    task = task,
                    futureView = task?.let { futureViewCalculator(it) },
                    domino = domino,
                    zone = timeProvider.zoneId(),
                    loading = false,
                    linkCandidates = linkCandidates,
                )
            }.collect { _uiState.value = it }
        }
    }

    /** Adds `taskId → otherId`, surfacing a cycle rejection message (PRD §10.7 AC). */
    fun onAddDependency(otherTaskId: String) = viewModelScope.launch(dispatchers.io) {
        val result = dependencyRepository.add(fromTaskId = taskId, toTaskId = otherTaskId)
        if (result is TaskDependencyRepository.AddDependencyResult.Cycle) {
            _cycleMessage.value = true
        }
    }

    fun onCycleMessageShown() { _cycleMessage.value = false }

    companion object {
        const val ARG_TASK_ID = "taskId"
    }
}

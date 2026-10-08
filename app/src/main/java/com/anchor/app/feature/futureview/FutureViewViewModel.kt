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
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** UI state for screen 07 Future View + Domino (docs 07-screens.md §2.11, PRD §10.6-10.7). */
data class FutureViewUiState(
    val task: Task? = null,
    val futureView: FutureView? = null,
    val domino: DominoEffect? = null,
    val zone: ZoneId = ZoneId.systemDefault(),
    val loading: Boolean = true,
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

    init {
        viewModelScope.launch(dispatchers.io) { load() }
    }

    private suspend fun load() {
        val task = taskRepository.getById(taskId)
        _uiState.value = FutureViewUiState(
            task = task,
            futureView = task?.let { futureViewCalculator(it) },
            domino = computeDomino(taskId),
            zone = timeProvider.zoneId(),
            loading = false,
        )
    }

    /** Adds `taskId → otherId`, surfacing a cycle rejection message (PRD §10.7 AC). */
    fun onAddDependency(otherTaskId: String) = viewModelScope.launch(dispatchers.io) {
        val result = dependencyRepository.add(fromTaskId = taskId, toTaskId = otherTaskId)
        if (result is TaskDependencyRepository.AddDependencyResult.Cycle) {
            _cycleMessage.value = true
        } else {
            load()
        }
    }

    private val _cycleMessage = MutableStateFlow(false)
    val cycleMessage: StateFlow<Boolean> = _cycleMessage.asStateFlow()

    fun onCycleMessageShown() { _cycleMessage.value = false }

    companion object {
        const val ARG_TASK_ID = "taskId"
    }
}

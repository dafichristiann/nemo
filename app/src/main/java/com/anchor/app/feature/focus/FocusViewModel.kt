package com.anchor.app.feature.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.usecase.focus.FinishFocusUseCase
import com.anchor.app.core.domain.usecase.focus.PauseFocusUseCase
import com.anchor.app.core.domain.usecase.focus.StartFocusUseCase
import com.anchor.app.platform.service.FocusService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Duration choices offered on Focus Setup (07-screens.md §2.9: 25/45/60 + custom). */
val FOCUS_DURATIONS_MIN = listOf(25, 45, 60)

/** One selectable task row on Focus Setup. */
data class FocusTaskOption(val id: String, val title: String)

/** UI state for screens 05 (Setup) and 06 (Active). */
data class FocusUiState(
    val taskOptions: List<FocusTaskOption> = emptyList(),
    val selectedTaskId: String? = null,
    val selectedMode: FocusMode = FocusMode.GENTLE,
    val selectedDurationMin: Int = FOCUS_DURATIONS_MIN.first(),
    val active: ActiveFocus? = null,
    val lastFallbackReason: String? = null,
    val lastFinishedTaskId: String? = null,
) {
    val isRunning: Boolean get() = active != null
}

/** Everything screen 06 needs to render (PRD §10.5 required fields). */
data class ActiveFocus(
    val sessionId: String,
    val taskId: String?,
    val taskTitle: String?,
    val mode: FocusMode,
    val plannedDurationMin: Int?,
    val elapsedMs: Long,
    val paused: Boolean,
    val currentStep: Subtask?,
    val totalSteps: Int,
    val doneSteps: Int,
) {
    val remainingMs: Long?
        get() = plannedDurationMin?.let { (it * 60_000L - elapsedMs).coerceAtLeast(0L) }
    val progressFraction: Float
        get() = plannedDurationMin?.takeIf { it > 0 }
            ?.let { (elapsedMs.toFloat() / (it * 60_000f)).coerceIn(0f, 1f) } ?: 0f
}

/**
 * ViewModel for Focus Setup + Active (FR-F1..F7, docs 07-screens.md §2.9-2.10).
 *
 * Owns the whole session flow: pick task/mode/duration → start (recording a `FocusSession`,
 * starting [FocusService]) → live timer with pause/resume → finish/abort (recording duration,
 * stopping the service, offering a progress update). The elapsed clock is driven here with a
 * 1s tick so the UI stays declarative; the durable truth is the Room row.
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val startFocus: StartFocusUseCase,
    private val pauseFocus: PauseFocusUseCase,
    private val finishFocus: FinishFocusUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val selectedTaskId = MutableStateFlow<String?>(null)
    private val selectedMode = MutableStateFlow(FocusMode.GENTLE)
    private val selectedDurationMin = MutableStateFlow(FOCUS_DURATIONS_MIN.first())
    private val elapsedMs = MutableStateFlow(0L)
    private val fallbackReason = MutableStateFlow<String?>(null)
    private val lastFinishedTaskId = MutableStateFlow<String?>(null)
    private var startedAt: Long = 0L

    val uiState: StateFlow<FocusUiState> = combine(
        taskRepository.observeAll(),
        focusRepository.observeCurrent(),
        combine(selectedTaskId, selectedMode, selectedDurationMin) { id, mode, dur -> Triple(id, mode, dur) },
        elapsedMs,
    ) { tasks, current, selection, elapsed ->
        buildState(tasks, current, selection.first, selection.second, selection.third, elapsed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState(),
    )

    init {
        // Keep the *actual* selection in sync with the effective default shown in the UI: when
        // no task is chosen yet and open tasks exist, adopt the first one. Without this the setup
        // screen would display a selection that `onBegin()` never sees (taskId stays null).
        viewModelScope.launch {
            taskRepository.observeAll().collect { tasks ->
                if (selectedTaskId.value == null) {
                    val firstOpen = tasks.firstOrNull {
                        it.status == TaskStatus.TODO || it.status == TaskStatus.IN_PROGRESS
                    }
                    if (firstOpen != null) selectedTaskId.value = firstOpen.id
                }
            }
        }
    }

    private fun buildState(
        tasks: List<Task>,
        current: FocusSession?,
        taskId: String?,
        mode: FocusMode,
        duration: Int,
        elapsed: Long,
    ): FocusUiState {
        val options = tasks
            .filter { it.status == TaskStatus.TODO || it.status == TaskStatus.IN_PROGRESS }
            .map { FocusTaskOption(it.id, it.title) }

        val active = current?.let { session ->
            ActiveFocus(
                sessionId = session.id,
                taskId = session.taskId,
                taskTitle = session.taskId?.let { id -> tasks.find { it.id == id }?.title },
                mode = session.mode,
                plannedDurationMin = session.plannedDurationMin,
                elapsedMs = if (session.status == FocusStatus.ACTIVE) elapsed else session.actualDurationMs,
                paused = session.status == FocusStatus.PAUSED,
                currentStep = null,
                totalSteps = 0,
                doneSteps = 0,
            )
        }
        return FocusUiState(
            taskOptions = options,
            selectedTaskId = taskId ?: options.firstOrNull()?.id,
            selectedMode = mode,
            selectedDurationMin = duration,
            active = active,
            lastFallbackReason = fallbackReason.value,
            lastFinishedTaskId = lastFinishedTaskId.value,
        )
    }

    fun onSelectTask(id: String) { selectedTaskId.value = id }

    fun onSelectMode(mode: FocusMode) { selectedMode.value = mode }

    fun onSelectDuration(minutes: Int) { selectedDurationMin.value = minutes }

    /** Begins a session (FR-F1, FR-F2). Records the session and starts the foreground service. */
    fun onBegin() = viewModelScope.launch(dispatchers.io) {
        val result = startFocus(
            taskId = selectedTaskId.value,
            mode = selectedMode.value,
            plannedDurationMin = selectedDurationMin.value,
        )
        fallbackReason.value = result.fallbackReason
        lastFinishedTaskId.value = null
        FocusService.start(context)
        startTicking()
    }

    /** Pause / resume (FR-F5). */
    fun onTogglePause() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        if (session.status == FocusStatus.ACTIVE) {
            pauseFocus(session.id, currentElapsed())
            stopTicking()
        } else {
            // Resume: keep the accumulated duration, restart the clock from "now".
            startedAt = timeProvider.nowMillis() - session.actualDurationMs
            focusRepository.updateState(
                id = session.id,
                status = FocusStatus.ACTIVE,
                actualDurationMs = session.actualDurationMs,
                endedAt = null,
                fallbackReason = session.fallbackReason,
            )
            startTicking()
        }
    }

    /** Normal finish (FR-F6): record duration, stop the service, offer a progress update. */
    fun onFinish() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        val result = finishFocus(session.id, currentElapsed(), aborted = false)
        stopTicking()
        FocusService.stop(context)
        lastFinishedTaskId.value = result.taskId
        elapsedMs.value = 0L
    }

    /** Emergency exit (FR-F6, PRD §10.5): always available, aborts the session. */
    fun onEmergencyExit() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        finishFocus(session.id, currentElapsed(), aborted = true)
        stopTicking()
        FocusService.stop(context)
        elapsedMs.value = 0L
    }

    /** Applies the offered progress update to the finished task (FR-F6). */
    fun onUpdateProgress(percent: Int) = viewModelScope.launch(dispatchers.io) {
        val taskId = lastFinishedTaskId.value ?: return@launch
        taskRepository.setProgress(taskId, percent.coerceIn(0, 100))
        lastFinishedTaskId.value = null
    }

    fun onDismissProgressOffer() { lastFinishedTaskId.value = null }

    private fun currentElapsed(): Long =
        if (startedAt == 0L) elapsedMs.value
        else (timeProvider.nowMillis() - startedAt).coerceAtLeast(0L)

    private var tickJob: kotlinx.coroutines.Job? = null

    private fun startTicking() {
        if (tickJob?.isActive == true) return
        startedAt = timeProvider.nowMillis()
        tickJob = viewModelScope.launch {
            while (true) {
                elapsedMs.value = (timeProvider.nowMillis() - startedAt).coerceAtLeast(0L)
                delay(1_000)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTicking()
    }
}

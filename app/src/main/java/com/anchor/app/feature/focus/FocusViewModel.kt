package com.anchor.app.feature.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.usecase.focus.FinishFocusUseCase
import com.anchor.app.core.domain.usecase.focus.FinishFocusUseCase.FinishFocusResult
import com.anchor.app.core.domain.usecase.focus.PauseFocusUseCase
import com.anchor.app.core.domain.usecase.focus.StartFocusUseCase
import com.anchor.app.core.domain.usecase.focus.isFocusVerified
import com.anchor.app.core.domain.usecase.task.ManageSubtasksUseCase
import com.anchor.app.BuildConfig
import com.anchor.app.R
import com.anchor.app.platform.accessibility.AccessibilityStatus
import com.anchor.app.platform.audio.LofiGenre
import com.anchor.app.platform.service.FocusService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Duration choices offered on Focus Setup (07-screens.md §2.9): a scroll wheel in seconds. */
val FOCUS_DURATION_STEPS_SEC: List<Int> = (1..120).map { it * 5 } // 5s .. 600s in 5s steps

/** Default selected duration (seconds) — the middle-ish 25 min option when present. */
val FOCUS_DEFAULT_DURATION_SEC: Int = 25 * 60

/**
 * Which apps a session should monitor or pause, given the chosen [mode] and the user's blocked set (FR-S4).
 *
 * Strict/Custom and Gentle (Santai) all monitor the selected blocked apps. In Gentle mode, an overlay
 * is shown with an option to stay in the app or return to focus.
 */
internal fun effectiveBlockedApps(mode: FocusMode, blockedApps: Set<String>): Set<String> =
    blockedApps

/** One selectable task row on Focus Setup. */
@androidx.compose.runtime.Immutable
data class FocusTaskOption(
    val id: String,
    val title: String,
    val deadline: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val estimatedDurationMin: Int? = null,
)

/** Internal bundle of the Focus Setup selections, combined into [FocusUiState]. */
private data class SetupSelection(
    val taskId: String?,
    val mode: FocusMode,
    val durationSec: Int,
    val serviceEnabled: Boolean,
    val soundscapeEnabled: Boolean,
    val lofiGenre: LofiGenre,
)

/** Result of the last finished session, for post-session feedback (PRD v0.5 §10.1). */
@androidx.compose.runtime.Immutable
data class CompletionResult(
    val sessionId: String? = null,
    val taskTitle: String,
    val verified: Boolean,
    val focusMinutes: Int,
    val rating: Int? = null,
)

/** UI state for screens 05 (Setup) and 06 (Active). */
@androidx.compose.runtime.Immutable
data class FocusUiState(
    val taskOptions: List<FocusTaskOption> = emptyList(),
    val selectedTaskId: String? = null,
    val selectedMode: FocusMode = FocusMode.GENTLE,
    val selectedDurationSec: Int = FOCUS_DEFAULT_DURATION_SEC,
    val soundscapeEnabled: Boolean = true,
    val selectedLofiGenre: LofiGenre = LofiGenre.DEFAULT,
    val active: ActiveFocus? = null,
    val lastFallbackReason: String? = null,
    /** Outcome shown after a session ends (PRD v0.5 §10.1). Null once acknowledged/cleared. */
    val lastCompletion: CompletionResult? = null,
    /** Apps the user chose to pause (FR-S4). Pre-filled into Strict/Custom sessions. */
    val blockedApps: Set<String> = emptySet(),
    /**
     * True when Strict/Custom blocking can actually engage: build flag, persisted opt-in, and the
     * OS Accessibility service all active (FR-F4/F8, PRD v0.4 §10.5). Drives the setup-screen card.
     */
    val blockingReady: Boolean = false,
) {
    val isRunning: Boolean get() = active != null
    val selectedTask: FocusTaskOption? get() = taskOptions.find { it.id == selectedTaskId }
}

/** Everything screen 06 needs to render (PRD §10.5 required fields). */
@androidx.compose.runtime.Immutable
data class ActiveFocus(
    val sessionId: String,
    val taskId: String?,
    val taskTitle: String?,
    val mode: FocusMode,
    val plannedDurationSec: Int?,
    val elapsedMs: Long,
    val paused: Boolean,
    val currentStep: Subtask?,
    val totalSteps: Int,
    val doneSteps: Int,
    val subtasks: List<Subtask> = emptyList(),
) {
    val remainingMs: Long?
        get() = plannedDurationSec?.let { (it * 1_000L - elapsedMs).coerceAtLeast(0L) }
    val progressFraction: Float
        get() = plannedDurationSec?.takeIf { it > 0 }
            ?.let { (elapsedMs.toFloat() / (it * 1_000f)).coerceIn(0f, 1f) } ?: 0f
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
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val settingsRepository: SettingsRepository,
    private val startFocus: StartFocusUseCase,
    private val pauseFocus: PauseFocusUseCase,
    private val finishFocus: FinishFocusUseCase,
    private val accessibilityStatus: AccessibilityStatus,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
    private val manageSubtasksUseCase: ManageSubtasksUseCase,
    private val rateFocusSession: com.anchor.app.core.domain.usecase.focus.RateFocusSessionUseCase,
) : ViewModel() {

    /** Optional task preselected by navigation (Detail/FutureView "start focus for this task"). */
    private val initialTaskId: String? = savedStateHandle.get<String>(ARG_TASK_ID)

    private val selectedTaskId = MutableStateFlow<String?>(initialTaskId)
    private val selectedMode = MutableStateFlow(FocusMode.GENTLE)
    private val selectedDurationSec = MutableStateFlow(FOCUS_DEFAULT_DURATION_SEC)
    val soundscapeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val selectedLofiGenre: MutableStateFlow<LofiGenre> = MutableStateFlow(LofiGenre.DEFAULT)
    private val elapsedMs = MutableStateFlow(0L)
    private val fallbackReason = MutableStateFlow<String?>(null)
    private val lastCompletion = MutableStateFlow<CompletionResult?>(null)
    private val serviceEnabled = MutableStateFlow(false)
    private var startedAt: Long = 0L

    /** True once the user picks a duration on the wheel; until then we mirror the profile default. */
    private var durationChosenByUser = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentSessionWithSubtasks = focusRepository.observeCurrent()
        .flatMapLatest { session ->
            val taskId = session?.taskId
            if (taskId != null) {
                taskRepository.observeSubtasks(taskId).map { subtasks -> session to subtasks }
            } else {
                flowOf(session to emptyList<Subtask>())
            }
        }

    val uiState: StateFlow<FocusUiState> = combine(
        taskRepository.observeAll(),
        currentSessionWithSubtasks,
        combine(
            selectedTaskId,
            selectedMode,
            selectedDurationSec,
            combine(serviceEnabled, soundscapeEnabled, selectedLofiGenre) { service, soundscape, genre ->
                Triple(service, soundscape, genre)
            },
        ) { taskId, mode, duration, (service, soundscape, genre) ->
            SetupSelection(taskId, mode, duration, service, soundscape, genre)
        },
        elapsedMs,
        combine(settingsRepository.observe(), lastCompletion, fallbackReason) { settings, completion, fallback ->
            Triple(settings, completion, fallback)
        },
    ) { tasks, currentPair, selection, elapsed, (settings, completion, fallback) ->
        val (current, subtasks) = currentPair
        buildState(tasks, current, subtasks, selection, elapsed, settings, completion, fallback)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState(),
    )

    init {
        // Keep the *actual* selection in sync with the effective default shown in the UI: when
        // no task is chosen yet and open tasks exist, adopt the first one.
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

        // Duration is driven by the chosen task's initial estimatedDurationMin,
        // falling back to user settings if unspecified.
        viewModelScope.launch {
            combine(
                taskRepository.observeAll(),
                selectedTaskId,
                settingsRepository.observe(),
            ) { tasks, chosenTaskId, settings ->
                val effectiveTaskId = chosenTaskId ?: tasks.firstOrNull {
                    it.status == TaskStatus.TODO || it.status == TaskStatus.IN_PROGRESS
                }?.id
                val activeTask = tasks.find { it.id == effectiveTaskId }
                val durationMin = activeTask?.estimatedDurationMin?.takeIf { it > 0 }
                    ?: settings.defaultFocusDurationMin.coerceAtLeast(1)
                durationMin * 60
            }.collect { calculatedDurationSec ->
                if (!durationChosenByUser) {
                    selectedDurationSec.value = calculatedDurationSec
                }
            }
        }
        // Adopt a session that is already running (e.g. after the screen was recreated by the
        // focus-block overlay's "Back to focus"): keep the clock counting from its real start
        // instead of resetting to zero. An ACTIVE row's wall-clock start *is* the anchor, so the
        // elapsed shown stays `now - startedAt` (accumulated duration is only folded in on pause).
        var previousSessionId: String? = null
        viewModelScope.launch {
            focusRepository.observeCurrent().collect { session ->
                if (session != null) {
                    previousSessionId = session.id
                    when (session.status) {
                        FocusStatus.ACTIVE -> {
                            val anchor = if (session.actualDurationMs > 0L) {
                                timeProvider.nowMillis() - session.actualDurationMs
                            } else {
                                session.startedAt
                            }
                            if (startedAt != anchor) startTicking(anchor)
                        }
                        FocusStatus.PAUSED -> {
                            stopTicking()
                            elapsedMs.value = session.actualDurationMs
                            startedAt = 0L
                        }
                        else -> {
                            stopTicking()
                            elapsedMs.value = 0L
                            startedAt = 0L
                        }
                    }
                } else {
                    stopTicking()
                    startedAt = 0L
                    elapsedMs.value = 0L
                    val lastId = previousSessionId
                    previousSessionId = null
                    if (lastId != null && lastCompletion.value == null) {
                        val finished = focusRepository.getById(lastId)
                        if (finished != null && finished.status == FocusStatus.FINISHED) {
                            val title = finished.taskId?.let { taskRepository.getById(it)?.title }
                                ?: context.getString(R.string.focus_task_default)
                            lastCompletion.value = CompletionResult(
                                sessionId = finished.id,
                                taskTitle = title,
                                verified = isFocusVerified(finished.actualDurationMs, finished.plannedDurationSec),
                                focusMinutes = if (finished.actualDurationMs > 0L) (finished.actualDurationMs / 60_000L).toInt().coerceAtLeast(1) else 0,
                                rating = finished.rating,
                            )
                        }
                    }
                }
            }
        }
        refreshBlockingStatus()
    }

    /** Refresh the OS Accessibility status; call on resume after returning from Settings (FR-F8). */
    fun refreshBlockingStatus() = viewModelScope.launch(dispatchers.io) {
        serviceEnabled.value = accessibilityStatus.isEnabled(context)
    }

    /** Opens the OS Accessibility settings so the user can enable the blocking service. */
    fun onOpenAccessibilitySettings() = accessibilityStatus.open(context)

    private fun buildState(
        tasks: List<Task>,
        current: FocusSession?,
        subtasks: List<Subtask>,
        selection: SetupSelection,
        elapsed: Long,
        settings: AppSettings,
        completion: CompletionResult?,
        fallback: String?,
    ): FocusUiState {
        val options = tasks
            .filter { it.status == TaskStatus.TODO || it.status == TaskStatus.IN_PROGRESS }
            .map { FocusTaskOption(it.id, it.title, it.deadline, it.priority, it.estimatedDurationMin) }

        val active = current?.let { session ->
            val doneCount = subtasks.count { it.isDone }
            ActiveFocus(
                sessionId = session.id,
                taskId = session.taskId,
                taskTitle = session.taskId?.let { id -> tasks.find { it.id == id }?.title },
                mode = session.mode,
                plannedDurationSec = session.plannedDurationSec,
                elapsedMs = if (session.status == FocusStatus.ACTIVE) elapsed else session.actualDurationMs,
                paused = session.status == FocusStatus.PAUSED,
                currentStep = subtasks.firstOrNull { !it.isDone },
                totalSteps = subtasks.size,
                doneSteps = doneCount,
                subtasks = subtasks,
            )
        }
        return FocusUiState(
            taskOptions = options,
            selectedTaskId = selection.taskId ?: options.firstOrNull()?.id,
            selectedMode = selection.mode,
            selectedDurationSec = selection.durationSec,
            soundscapeEnabled = selection.soundscapeEnabled,
            selectedLofiGenre = selection.lofiGenre,
            active = active,
            lastFallbackReason = fallback,
            lastCompletion = completion,
            blockedApps = settings.blockedApps,
            blockingReady = BuildConfig.FOCUS_BLOCKING_ENABLED &&
                settings.focusBlockingEnabled &&
                selection.serviceEnabled,
        )
    }

    fun toggleSubtask(subtask: Subtask) = viewModelScope.launch(dispatchers.io) {
        manageSubtasksUseCase.toggle(subtask)
    }

    fun onSelectTask(id: String) { selectedTaskId.value = id }

    fun onSelectMode(mode: FocusMode) { selectedMode.value = mode }

    fun onSelectDuration(seconds: Int) {
        durationChosenByUser = true
        selectedDurationSec.value = seconds
    }

    fun onToggleSoundscape(enabled: Boolean) {
        soundscapeEnabled.value = enabled
    }

    /** Selects a lofi genre and switches playback dynamically if active. */
    fun onSelectLofiGenre(genre: LofiGenre) {
        selectedLofiGenre.value = genre
        val currentActive = uiState.value.active
        if (currentActive != null && !currentActive.paused && soundscapeEnabled.value) {
            FocusService.changeSound(context, genre.soundResId, soundscapeEnabled = true)
        }
    }

    /** Quick toggle for soundscape (mute/unmute) during an active focus session. */
    fun onToggleActiveSoundscape() {
        val newValue = !soundscapeEnabled.value
        soundscapeEnabled.value = newValue
        val currentActive = uiState.value.active
        if (currentActive != null && !currentActive.paused) {
            if (newValue) {
                FocusService.changeSound(context, selectedLofiGenre.value.soundResId, soundscapeEnabled = true)
            } else {
                FocusService.pause(context)
            }
        }
    }

    /** Begins a session (FR-F1, FR-F2). Records the session and starts the foreground service. */
    fun onBegin() = viewModelScope.launch(dispatchers.io) {
        val mode = selectedMode.value
        val result = startFocus(
            taskId = selectedTaskId.value,
            mode = mode,
            plannedDurationSec = selectedDurationSec.value,
            blockedApps = effectiveBlockedApps(mode, uiState.value.blockedApps),
        )
        fallbackReason.value = result.fallbackReason
        lastCompletion.value = null
        FocusService.start(
            context,
            soundscapeEnabled = soundscapeEnabled.value,
            soundResId = selectedLofiGenre.value.soundResId,
        )
        startTicking()
    }

    /** Pause / resume (FR-F5). */
    fun onTogglePause() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        if (session.status == FocusStatus.ACTIVE) {
            val elapsed = currentElapsed()
            pauseFocus(session.id, elapsed)
            stopTicking()
            FocusService.pause(context)
            // Freeze the clock: with startedAt cleared, currentElapsed() now returns the
            // accumulated elapsedMs, so a finish/abort while paused never counts the paused
            // interval as focus time (PRD v0.5 §10.1 effort-based completion).
            elapsedMs.value = elapsed
            startedAt = 0L
        } else {
            // Resume: keep the accumulated duration, restart the clock anchored so elapsed continues.
            val anchor = timeProvider.nowMillis() - session.actualDurationMs
            focusRepository.updateState(
                id = session.id,
                status = FocusStatus.ACTIVE,
                actualDurationMs = session.actualDurationMs,
                endedAt = null,
                fallbackReason = session.fallbackReason,
                startedAt = anchor,
            )
            if (soundscapeEnabled.value) {
                FocusService.resume(context)
            }
            startTicking(anchor)
        }
    }

    /** Auto-pause focus when user leaves the screen via back navigation (allows resuming later). */
    fun autoPauseFocus() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        if (session.status == FocusStatus.ACTIVE) {
            val elapsed = currentElapsed()
            pauseFocus(session.id, elapsed)
            stopTicking()
            FocusService.pause(context)
            elapsedMs.value = elapsed
            startedAt = 0L
        }
    }

    private var isFinishing = false

    /** Normal finish (FR-F6): records the duration and completes the linked task. */
    fun onFinish() = viewModelScope.launch(dispatchers.io) {
        if (isFinishing) return@launch
        isFinishing = true
        try {
            val session = focusRepository.current() ?: return@launch
            val elapsed = currentElapsed()
            val plannedMs = (session.plannedDurationSec ?: 0) * 1000L
            val finalElapsed = if (plannedMs > 0 && elapsed >= plannedMs) maxOf(elapsed, plannedMs) else elapsed
            val result = finishFocus(session.id, finalElapsed, aborted = false, forceCompleteTask = true)
            stopTicking()
            FocusService.stop(context)
            lastCompletion.value = result.toCompletionResult()
            elapsedMs.value = 0L
            startedAt = 0L
        } finally {
            isFinishing = false
        }
    }

    /** Emergency exit (FR-F6, PRD §10.5): always available, aborts the session (no completion). */
    fun onEmergencyExit() = viewModelScope.launch(dispatchers.io) {
        if (isFinishing) return@launch
        isFinishing = true
        try {
            val session = focusRepository.current() ?: return@launch
            val result = finishFocus(session.id, currentElapsed(), aborted = true)
            stopTicking()
            FocusService.stop(context)
            lastCompletion.value = result.toCompletionResult()
            elapsedMs.value = 0L
            startedAt = 0L
        } finally {
            isFinishing = false
        }
    }

    /** Clears the post-session feedback banner. */
    fun onDismissCompletion() { lastCompletion.value = null }

    /** Rates the last completed session (1..5) for adaptive circadian learning. */
    fun onRateLastSession(rating: Int) = viewModelScope.launch(dispatchers.io) {
        val completion = lastCompletion.value ?: return@launch
        val sid = completion.sessionId ?: return@launch
        rateFocusSession(sid, rating)
        lastCompletion.value = completion.copy(rating = rating)
    }

    private suspend fun FinishFocusResult.toCompletionResult(): CompletionResult? {
        if (aborted) return null
        val title = if (taskId != null) {
            taskRepository.getById(taskId)?.title ?: context.getString(R.string.focus_task_default)
        } else {
            context.getString(R.string.focus_task_default)
        }
        return CompletionResult(
            sessionId = sessionId,
            taskTitle = title,
            // `verified` reflects whether the session ran its full duration (effort signal),
            // shown as a softer/stronger feedback line — the task is completed either way.
            verified = verified,
            focusMinutes = if (totalMs > 0L) (totalMs / 60_000L).toInt().coerceAtLeast(1) else 0,
            rating = null,
        )
    }

    private fun currentElapsed(): Long =
        if (startedAt == 0L) elapsedMs.value
        else (timeProvider.nowMillis() - startedAt).coerceAtLeast(0L)

    private var tickJob: kotlinx.coroutines.Job? = null

    private fun startTicking(anchorStartMillis: Long = timeProvider.nowMillis()) {
        // Already ticking for this exact anchor: nothing to do. Otherwise (re)start so adoption
        // of a persisted session re-anchors the clock instead of showing a stale value.
        if (tickJob?.isActive == true && startedAt == anchorStartMillis) return
        tickJob?.cancel()
        startedAt = anchorStartMillis
        tickJob = viewModelScope.launch {
            while (true) {
                val current = (timeProvider.nowMillis() - startedAt).coerceAtLeast(0L)
                elapsedMs.value = current
                val plannedSec = uiState.value.active?.plannedDurationSec
                if (plannedSec != null && plannedSec > 0 && current >= plannedSec * 1_000L) {
                    onFinish()
                    break
                }
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

    companion object {
        /** Optional nav argument: pre-selects the task a session is started for. */
        const val ARG_TASK_ID = "taskId"
    }
}

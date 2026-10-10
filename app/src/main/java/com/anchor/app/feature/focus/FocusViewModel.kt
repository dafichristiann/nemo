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
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.usecase.focus.FinishFocusUseCase
import com.anchor.app.core.domain.usecase.focus.FinishFocusUseCase.FinishFocusResult
import com.anchor.app.core.domain.usecase.focus.PauseFocusUseCase
import com.anchor.app.core.domain.usecase.focus.StartFocusUseCase
import com.anchor.app.BuildConfig
import com.anchor.app.platform.accessibility.AccessibilityStatus
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

/** Duration choices offered on Focus Setup (07-screens.md §2.9): a scroll wheel in seconds. */
val FOCUS_DURATION_STEPS_SEC: List<Int> = (1..120).map { it * 5 } // 5s .. 600s in 5s steps

/** Default selected duration (seconds) — the middle-ish 25 min option when present. */
val FOCUS_DEFAULT_DURATION_SEC: Int = 25 * 60

/**
 * Which apps a session should pause, given the chosen [mode] and the user's blocked set (FR-S4).
 *
 * Strict/Custom pause the user's blocked apps; Gentle never blocks anything (PRD §10.5), so it
 * always yields an empty set regardless of what Settings holds.
 */
internal fun effectiveBlockedApps(mode: FocusMode, blockedApps: Set<String>): Set<String> =
    if (mode == FocusMode.GENTLE) emptySet() else blockedApps

/** One selectable task row on Focus Setup. */
data class FocusTaskOption(val id: String, val title: String)

/** Internal bundle of the Focus Setup selections, combined into [FocusUiState]. */
private data class SetupSelection(
    val taskId: String?,
    val mode: FocusMode,
    val durationSec: Int,
    val serviceEnabled: Boolean,
)

/** Result of the last finished session, for post-session feedback (PRD v0.5 §10.1). */
data class CompletionResult(
    val taskTitle: String,
    val verified: Boolean,
    val focusMinutes: Int,
)

/** UI state for screens 05 (Setup) and 06 (Active). */
data class FocusUiState(
    val taskOptions: List<FocusTaskOption> = emptyList(),
    val selectedTaskId: String? = null,
    val selectedMode: FocusMode = FocusMode.GENTLE,
    val selectedDurationSec: Int = FOCUS_DEFAULT_DURATION_SEC,
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
}

/** Everything screen 06 needs to render (PRD §10.5 required fields). */
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
) : ViewModel() {

    /** Optional task preselected by navigation (Detail/FutureView "start focus for this task"). */
    private val initialTaskId: String? = savedStateHandle.get<String>(ARG_TASK_ID)

    private val selectedTaskId = MutableStateFlow<String?>(initialTaskId)
    private val selectedMode = MutableStateFlow(FocusMode.GENTLE)
    private val selectedDurationSec = MutableStateFlow(FOCUS_DEFAULT_DURATION_SEC)
    private val elapsedMs = MutableStateFlow(0L)
    private val fallbackReason = MutableStateFlow<String?>(null)
    private val lastCompletion = MutableStateFlow<CompletionResult?>(null)
    private val serviceEnabled = MutableStateFlow(false)
    private var startedAt: Long = 0L

    /** True once the user picks a duration on the wheel; until then we mirror the profile default. */
    private var durationChosenByUser = false

    val uiState: StateFlow<FocusUiState> = combine(
        taskRepository.observeAll(),
        focusRepository.observeCurrent(),
        combine(
            selectedTaskId,
            selectedMode,
            selectedDurationSec,
            serviceEnabled,
            ::SetupSelection,
        ),
        elapsedMs,
        settingsRepository.observe(),
    ) { tasks, current, selection, elapsed, settings ->
        buildState(tasks, current, selection, elapsed, settings)
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
        // Adopt a session that is already running (e.g. after the screen was recreated by the
        // focus-block overlay's "Back to focus"): keep the clock counting from its real start
        // instead of resetting to zero. An ACTIVE row's wall-clock start *is* the anchor, so the
        // elapsed shown stays `now - startedAt` (accumulated duration is only folded in on pause).
        viewModelScope.launch {
            focusRepository.observeCurrent().collect { session ->
                if (session != null && session.status == FocusStatus.ACTIVE) {
                    val anchor = session.startedAt
                    if (startedAt != anchor) startTicking(anchor)
                }
            }
        }
        // Prefill the duration wheel from the user's profile default (onboarding/Settings) until
        // they pick a value themselves — the wheel stays fully adjustable.
        viewModelScope.launch {
            settingsRepository.observe().collect { settings ->
                if (!durationChosenByUser) {
                    selectedDurationSec.value = settings.defaultFocusDurationMin * 60
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
        selection: SetupSelection,
        elapsed: Long,
        settings: AppSettings,
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
                plannedDurationSec = session.plannedDurationSec,
                elapsedMs = if (session.status == FocusStatus.ACTIVE) elapsed else session.actualDurationMs,
                paused = session.status == FocusStatus.PAUSED,
                currentStep = null,
                totalSteps = 0,
                doneSteps = 0,
            )
        }
        return FocusUiState(
            taskOptions = options,
            selectedTaskId = selection.taskId ?: options.firstOrNull()?.id,
            selectedMode = selection.mode,
            selectedDurationSec = selection.durationSec,
            active = active,
            lastFallbackReason = fallbackReason.value,
            lastCompletion = lastCompletion.value,
            blockedApps = settings.blockedApps,
            blockingReady = BuildConfig.FOCUS_BLOCKING_ENABLED &&
                settings.focusBlockingEnabled &&
                selection.serviceEnabled,
        )
    }

    fun onSelectTask(id: String) { selectedTaskId.value = id }

    fun onSelectMode(mode: FocusMode) { selectedMode.value = mode }

    fun onSelectDuration(seconds: Int) {
        durationChosenByUser = true
        selectedDurationSec.value = seconds
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
        FocusService.start(context)
        startTicking()
    }

    /** Pause / resume (FR-F5). */
    fun onTogglePause() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        if (session.status == FocusStatus.ACTIVE) {
            val elapsed = currentElapsed()
            pauseFocus(session.id, elapsed)
            stopTicking()
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
            )
            startTicking(anchor)
        }
    }

    /** Normal finish (FR-F6): records the duration and completes the linked task. */
    fun onFinish() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        val result = finishFocus(session.id, currentElapsed(), aborted = false)
        stopTicking()
        FocusService.stop(context)
        lastCompletion.value = result.toCompletionResult()
        elapsedMs.value = 0L
    }

    /** Emergency exit (FR-F6, PRD §10.5): always available, aborts the session (no completion). */
    fun onEmergencyExit() = viewModelScope.launch(dispatchers.io) {
        val session = focusRepository.current() ?: return@launch
        val result = finishFocus(session.id, currentElapsed(), aborted = true)
        stopTicking()
        FocusService.stop(context)
        lastCompletion.value = result.toCompletionResult()
        elapsedMs.value = 0L
    }

    /** Clears the post-session feedback banner. */
    fun onDismissCompletion() { lastCompletion.value = null }

    private suspend fun FinishFocusResult.toCompletionResult(): CompletionResult? {
        if (!completedTask) return null
        val taskId = taskId ?: return null
        val title = taskRepository.getById(taskId)?.title ?: return null
        return CompletionResult(
            taskTitle = title,
            // `verified` reflects whether the session ran its full duration (effort signal),
            // shown as a softer/stronger feedback line — the task is completed either way.
            verified = verified,
            focusMinutes = (totalMs / 60_000L).toInt(),
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

    companion object {
        /** Optional nav argument: pre-selects the task a session is started for. */
        const val ARG_TASK_ID = "taskId"
    }
}

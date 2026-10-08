package com.anchor.app.core.domain.usecase.focus

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.core.domain.model.CompletionSource
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Finishes or aborts a focus session (FR-F6; docs 07-screens.md §2.10). Records the final
 * duration + status, releases any blocking, and — per PRD v0.5 §10.1 "Effort-based
 * Completion" — **completes the linked task when the session was run in full**.
 *
 * A task is marked DONE (with `completionSource = VERIFIED`) only when the session was not
 * aborted AND [isFocusVerified] passes for the recorded duration vs. the planned duration.
 * Finishing early or an emergency exit still records the session but never completes the task.
 *
 * [aborted] distinguishes an emergency exit (ABORTED) from a normal Finish (FINISHED). Both
 * always release blocking — the emergency exit must never be blocked by anything (PRD §10.5).
 */
class FinishFocusUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val blockingStrategy: BlockingStrategy,
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        sessionId: String,
        elapsedMs: Long,
        aborted: Boolean = false,
    ): FinishFocusResult {
        val session = focusRepository.getById(sessionId)
        val status = if (aborted) FocusStatus.ABORTED else FocusStatus.FINISHED
        val now = timeProvider.nowMillis()
        val duration = elapsedMs.coerceAtLeast(0L)

        focusRepository.updateState(
            id = sessionId,
            status = status,
            actualDurationMs = duration,
            endedAt = now,
            fallbackReason = session?.fallbackReason,
        )
        runCatching { blockingStrategy.release() }

        // Effort-based completion (PRD v0.5 §10.1): only a full, non-aborted session earns DONE.
        val taskId = session?.taskId
        val verified = !aborted && isFocusVerified(duration, session?.plannedDurationMin)
        if (taskId != null && verified) {
            taskRepository.markDone(taskId, now, CompletionSource.VERIFIED)
        }

        return FinishFocusResult(
            sessionId = sessionId,
            taskId = taskId,
            totalMs = duration,
            aborted = aborted,
            completedTask = taskId != null && verified,
        )
    }

    /** Summary returned to the UI after a session ends. */
    data class FinishFocusResult(
        val sessionId: String,
        val taskId: String?,
        val totalMs: Long,
        val aborted: Boolean,
        /** True when this session completed its task (PRD v0.5 §10.1). */
        val completedTask: Boolean,
    )
}

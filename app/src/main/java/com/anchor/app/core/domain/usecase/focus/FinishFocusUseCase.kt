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
 * duration + status, releases any blocking, and completes the linked task.
 *
 * Completion rule: a normal **Finish** completes the linked task — pressing "Finish" is an
 * explicit "I'm done with this task". Only the [aborted] *emergency exit* skips completion (the
 * user is bailing out, not finishing).
 *
 * The stricter effort signal is kept **separately**: [FinishFocusResult.verified] reports whether
 * the session actually ran for its full planned duration ([isFocusVerified], PRD v0.5 §10.1).
 * The UI uses it only to tailor the post-session feedback ("full session" vs "ended early"), not
 * to withhold completion.
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

        // Finish completes the linked task; the emergency exit (aborted) does not.
        val taskId = session?.taskId
        val completed = taskId != null && !aborted
        if (completed) {
            taskRepository.markDone(taskId!!, now, CompletionSource.VERIFIED)
        }

        // "Verified" = the session reached its planned duration. Independent of [completed]:
        // an early Finish still completes the task, but is reported as not (fully) verified.
        val verified = completed && isFocusVerified(duration, session?.plannedDurationSec)

        return FinishFocusResult(
            sessionId = sessionId,
            taskId = taskId,
            totalMs = duration,
            aborted = aborted,
            completedTask = completed,
            verified = verified,
        )
    }

    /** Summary returned to the UI after a session ends. */
    data class FinishFocusResult(
        val sessionId: String,
        val taskId: String?,
        val totalMs: Long,
        val aborted: Boolean,
        /** True when this session completed its task (any non-aborted Finish). */
        val completedTask: Boolean,
        /** True when the session ran for its full planned duration (effort-based signal). */
        val verified: Boolean,
    )
}

package com.anchor.app.core.domain.usecase.focus

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import javax.inject.Inject

/**
 * Finishes or aborts a focus session (FR-F6; docs 07-screens.md §2.10). Records the final
 * duration + status, releases any blocking, and returns the summary so the UI can offer to
 * update the task's progress (FR-F6, screen offers "update progress").
 *
 * [aborted] distinguishes an emergency exit (ABORTED) from a normal Finish (FINISHED). Both
 * always release blocking — the emergency exit must never be blocked by anything (PRD §10.5).
 */
class FinishFocusUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val blockingStrategy: BlockingStrategy,
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

        focusRepository.updateState(
            id = sessionId,
            status = status,
            actualDurationMs = elapsedMs.coerceAtLeast(0L),
            endedAt = now,
            fallbackReason = session?.fallbackReason,
        )
        runCatching { blockingStrategy.release() }

        return FinishFocusResult(
            sessionId = sessionId,
            taskId = session?.taskId,
            totalMs = elapsedMs.coerceAtLeast(0L),
            aborted = aborted,
        )
    }

    /** Summary returned to the UI after a session ends. */
    data class FinishFocusResult(
        val sessionId: String,
        val taskId: String?,
        val totalMs: Long,
        val aborted: Boolean,
    )
}

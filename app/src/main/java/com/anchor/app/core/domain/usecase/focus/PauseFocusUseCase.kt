package com.anchor.app.core.domain.usecase.focus

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import javax.inject.Inject

/**
 * Pauses a running focus session (FR-F5). Accumulates elapsed time into `actualDurationMs`
 * so a later resume does not lose it, then marks the row PAUSED.
 */
class PauseFocusUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(sessionId: String, elapsedMs: Long) {
        focusRepository.updateState(
            id = sessionId,
            status = FocusStatus.PAUSED,
            actualDurationMs = elapsedMs.coerceAtLeast(0L),
            endedAt = timeProvider.nowMillis(),
            fallbackReason = focusRepository.getById(sessionId)?.fallbackReason,
        )
    }
}

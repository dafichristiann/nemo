package com.anchor.app.core.domain.usecase.focus

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import javax.inject.Inject

/**
 * Starts a focus session (FR-F1, FR-F2; docs 07-screens.md §2.9). Records a `FocusSession`
 * in ACTIVE state and, for Strict/Custom, asks the [BlockingStrategy] to engage.
 *
 * Graceful degradation (Spike B `13-spike-b-accessibility.md`, PRD §10.5): if blocking cannot
 * engage for a Strict/Custom request, the session **falls back to Gentle** — it is still recorded,
 * with `fallbackReason` set, rather than failing.
 *
 * @return the id of the created session plus the mode actually in effect.
 */
class StartFocusUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val blockingStrategy: BlockingStrategy,
    private val timeProvider: TimeProvider,
    private val idProvider: IdProvider,
) {
    suspend operator fun invoke(
        taskId: String?,
        mode: FocusMode,
        plannedDurationSec: Int?,
        blockedApps: Set<String> = emptySet(),
    ): StartFocusResult {
        val now = timeProvider.nowMillis()
        val id = idProvider.newId()

        var effectiveMode = mode
        var fallbackReason: String? = null

        if (mode == FocusMode.STRICT || mode == FocusMode.CUSTOM) {
            val engaged = blockingStrategy.isAvailable() && blockingStrategy.engage(blockedApps)
            if (!engaged) {
                effectiveMode = FocusMode.GENTLE
                fallbackReason = REASON_BLOCKING_UNAVAILABLE
            }
        }

        focusRepository.insert(
            FocusSession(
                id = id,
                taskId = taskId,
                mode = effectiveMode,
                plannedDurationSec = plannedDurationSec,
                actualDurationMs = 0L,
                startedAt = now,
                endedAt = null,
                status = FocusStatus.ACTIVE,
                blockedApps = if (effectiveMode == FocusMode.GENTLE) emptyList() else blockedApps.toList(),
                fallbackReason = fallbackReason,
            ),
        )
        return StartFocusResult(sessionId = id, effectiveMode = effectiveMode, fallbackReason = fallbackReason)
    }

    /** Result of starting a session, so the UI can honestly report a Gentle fallback. */
    data class StartFocusResult(
        val sessionId: String,
        val effectiveMode: FocusMode,
        val fallbackReason: String?,
    )

    companion object {
        const val REASON_BLOCKING_UNAVAILABLE = "blocking-unavailable"
    }
}

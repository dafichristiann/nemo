package com.anchor.app.core.domain.usecase.intervention

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.DriftSignal
import com.anchor.app.core.domain.model.InterventionAction
import com.anchor.app.core.domain.model.InterventionChannel
import com.anchor.app.core.domain.model.InterventionDecision
import com.anchor.app.core.domain.model.InterventionLevel
import com.anchor.app.core.domain.model.InterventionLog
import com.anchor.app.core.domain.model.InterventionStatus
import com.anchor.app.core.domain.model.SuppressReason
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.repository.InterventionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Decides whether a [DriftSignal] becomes a shown intervention or is held back, and records
 * every outcome (docs/superpowers/design/04-intervention-engine.md §3, PRD §10.4).
 *
 * Hard rules (LOCKED, PRD §10.4):
 *  - max [MAX_DAILY_INTERVENTIONS] meaningful interventions per local day (reset at midnight);
 *  - "Later" triggers a [COOLDOWN_AFTER_LATER_MS] cooldown for the same context/app;
 *  - `Detection Event != Intervention Attempt`: a suppressed signal **never** consumes quota;
 *  - escalation is gradual — never jumps straight to [InterventionLevel.FOCUS].
 *
 * All decisions are pure and deterministic given repositories + a [TimeProvider], so the whole
 * engine is unit-testable with fake repositories (docs 11-testing-strategy.md §5).
 */
class InterventionEngine @Inject constructor(
    private val interventionRepository: InterventionRepository,
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
    private val idProvider: IdProvider,
) {

    /**
     * Evaluates [signal] against quota, cooldown and context, returning either a [Show] carrying
     * the chosen level+channel, or a [Suppress] with the reason (no quota consumed).
     *
     * [notificationPermissionGranted] selects the channel (FR-I3): Notification when allowed,
     * else In-app. [userInsideAnchor] suppresses notifications while the user is already in-app
     * for the *same* context (PRD edge case "User sedang di dalam Anchor").
     */
    suspend fun evaluate(
        signal: DriftSignal?,
        now: Long = timeProvider.nowMillis(),
        notificationPermissionGranted: Boolean = true,
        userInsideAnchor: Boolean = false,
    ): InterventionDecision {
        if (signal == null) return InterventionDecision.Suppress(SuppressReason.NO_DRIFT)

        // Rule 2 (LOCKED): a detection that would be held back must not consume quota.
        if (quotaRemaining(now) <= 0) {
            return InterventionDecision.Suppress(SuppressReason.QUOTA_EXHAUSTED)
        }

        if (cooldownActive(signal.packageName, now)) {
            return InterventionDecision.Suppress(SuppressReason.COOLDOWN)
        }

        // Don't interrupt the user with a notification for the same context while they're in Anchor.
        if (userInsideAnchor) {
            return InterventionDecision.Suppress(SuppressReason.USER_IN_APP)
        }

        val level = chooseLevel(signal, now)
        val channel = chooseChannel(level, notificationPermissionGranted)
        return InterventionDecision.Show(level = level, channel = channel, signal = signal)
    }

    /** Meaningful interventions already spent in today's local-day window (docs 04 §3.1). */
    suspend fun quotaUsedToday(now: Long = timeProvider.nowMillis()): Int {
        val startOfDay = timeProvider.startOfDayMillis(now)
        val endOfDay = startOfDay + DAY_MS
        return interventionRepository.quotaUsedBetween(startOfDay, endOfDay)
    }

    /** Remaining quota for today; never negative (FR-I6). */
    suspend fun quotaRemaining(now: Long = timeProvider.nowMillis()): Int =
        (MAX_DAILY_INTERVENTIONS - quotaUsedToday(now)).coerceAtLeast(0)

    /** True while a "Later" cooldown for [contextPackage] is still active (FR-I4, 2 hours). */
    suspend fun cooldownActive(
        contextPackage: String,
        now: Long = timeProvider.nowMillis(),
    ): Boolean {
        val lastLater = interventionRepository.latestCooldownAt(contextPackage) ?: return false
        return now - lastLater < COOLDOWN_AFTER_LATER_MS
    }

    /**
     * Persists a shown intervention as a fresh log row (status defaults to [InterventionStatus.EXPIRED];
     * it is overwritten when the user acts). Returns the generated id so the UI can resolve it.
     */
    suspend fun recordShown(
        decision: InterventionDecision.Show,
        now: Long = timeProvider.nowMillis(),
    ): String {
        val id = idProvider.newId()
        interventionRepository.insert(
            InterventionLog(
                id = id,
                level = decision.level,
                channel = decision.channel,
                contextPackage = decision.signal.packageName,
                relatedTaskId = decision.signal.relatedTaskId,
                // Default to EXPIRED: if the user never reacts, the row costs no quota (docs 04 §3.5).
                status = InterventionStatus.EXPIRED,
                shownAt = now,
                resolvedAt = null,
                cooldownTriggered = false,
            ),
        )
        return id
    }

    /**
     * Applies the user's [action] to log [logId] (FR-I5, FR-I7). Quota impact:
     *  - START_FOCUS -> INTERACTED (-1 quota)
     *  - LATER       -> DISMISSED (-1 quota) + 2h cooldown for this context
     *  - DISMISS     -> DISMISSED (-1 quota)
     *  - EXPIRE      -> EXPIRED   (0 quota)
     */
    suspend fun recordAction(
        logId: String,
        action: InterventionAction,
        now: Long = timeProvider.nowMillis(),
    ) {
        val (status, cooldown) = when (action) {
            InterventionAction.START_FOCUS -> InterventionStatus.INTERACTED to false
            InterventionAction.LATER -> InterventionStatus.DISMISSED to true
            InterventionAction.DISMISS -> InterventionStatus.DISMISSED to false
            InterventionAction.EXPIRE -> InterventionStatus.EXPIRED to false
        }
        interventionRepository.resolve(
            id = logId,
            status = status,
            resolvedAt = now,
            cooldownTriggered = cooldown,
        )
    }

    /**
     * Gradual escalation (FR-I2, docs 04 §3.3): start at Level 1, step up **one** level per
     * cycle only when the signal supports it, and never jump straight to Level 4.
     *
     * Level 2 needs a related task; Level 3 needs a task with an estimate/deadline; Level 4 is
     * only ever reached later, after the user has already meaningfully engaged at Level ≥ 3.
     */
    private suspend fun chooseLevel(signal: DriftSignal, now: Long): Int {
        val relatedTask = signal.relatedTaskId?.let { taskRepository.getById(it) }
            ?: return InterventionLevel.GENTLE

        // The signal only carries HIGH/URGENT pending tasks, so Level 2 is always warranted.
        var level = InterventionLevel.CONTEXTUAL

        // Level 3 only if the task has a concrete estimate or deadline to reason about.
        if (relatedTask.estimatedDurationMin != null || relatedTask.deadline != null) {
            level = InterventionLevel.FUTURE_VIEW
        }

        // Level 4 is gated: the user must have engaged at/above Level 3 before (never a cold jump).
        if (level >= InterventionLevel.FUTURE_VIEW &&
            interventionRepository.countMeaningfulAtOrAbove(InterventionLevel.FUTURE_VIEW) > 0
        ) {
            level = InterventionLevel.FOCUS
        }
        return level
    }

    /** Channel priority (FR-I3): Notification when allowed, else In-app. Fullscreen stays rare. */
    private fun chooseChannel(level: Int, notificationPermissionGranted: Boolean): InterventionChannel =
        if (notificationPermissionGranted) InterventionChannel.NOTIFICATION else InterventionChannel.IN_APP

    companion object {
        /** LOCKED maximum meaningful interventions per local day (PRD §10.4 rule 1). */
        const val MAX_DAILY_INTERVENTIONS = 3

        /** LOCKED "Later" cooldown, per context/app (PRD §10.4 rule 4). */
        const val COOLDOWN_AFTER_LATER_MS = 2 * 60 * 60 * 1000L

        private const val DAY_MS = 24 * 60 * 60 * 1000L

        /** Priorities that make a pending task "meaningful" for contextual interventions. */
        val MEANINGFUL_PRIORITIES = setOf(TaskPriority.HIGH, TaskPriority.URGENT)
    }
}

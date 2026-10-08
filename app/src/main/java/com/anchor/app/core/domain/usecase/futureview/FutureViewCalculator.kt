package com.anchor.app.core.domain.usecase.futureview

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.FutureScenario
import com.anchor.app.core.domain.model.FutureView
import com.anchor.app.core.domain.model.Task
import javax.inject.Inject

/**
 * Computes the two Future View scenarios — "start now" vs "keep delaying"
 * (docs/superpowers/design/07-screens.md §2.11, PRD §10.6, FR-FV1..FV5).
 *
 * Pure and rule-based: given a task + the current time it returns projections with every
 * assumption flagged. It never fabricates data — when a task lacks a duration or deadline it
 * applies the locked defaults ([DEFAULT_TASK_DURATION_MIN], [DEFAULT_SAME_DAY_DEADLINE_MINUTE])
 * and reports `usingAssumed*` so the UI marks them (FR-FV5).
 *
 * The "delay" scenario models a realistic slip: the task tends to start late, so we project a
 * start [DELAY_OFFSET_MIN] from now (still before the same-day deadline). This is a heuristic,
 * communicated as "based on your current schedule" — never a certainty (PRD §10.6 language).
 */
class FutureViewCalculator @Inject constructor(
    private val timeProvider: TimeProvider,
) {

    /** Builds a [FutureView] for [task] at [now] (defaults to the injected clock). */
    operator fun invoke(task: Task, now: Long = timeProvider.nowMillis()): FutureView {
        val assumptions = mutableListOf<String>()

        val usingAssumedDuration = task.estimatedDurationMin == null
        val durationMin = task.estimatedDurationMin ?: DEFAULT_TASK_DURATION_MIN
        if (usingAssumedDuration) assumptions += ASSUMPTION_DURATION

        val usingAssumedDeadline = task.deadline == null
        val deadlineAt: Long = task.deadline ?: sameDayDeadline(now)
        if (usingAssumedDeadline) assumptions += ASSUMPTION_DEADLINE

        val startNow = FutureScenario(
            startAt = now,
            endAt = now + durationMin * MINUTE_MS,
            deadlineAt = deadlineAt,
            assumptions = emptyList(),
        )

        // Delay scenario: a realistic slip, but never starting after the deadline.
        val plannedDelayStart = now + DELAY_OFFSET_MIN * MINUTE_MS
        val delayStart = minOf(plannedDelayStart, (deadlineAt - durationMin * MINUTE_MS).coerceAtLeast(now))
        val delay = FutureScenario(
            startAt = delayStart,
            endAt = delayStart + durationMin * MINUTE_MS,
            deadlineAt = deadlineAt,
            assumptions = listOf(ASSUMPTION_DELAY),
        )

        return FutureView(
            startNow = startNow,
            delay = delay,
            estimatedDurationMin = durationMin,
            usingAssumedDuration = usingAssumedDuration,
            usingAssumedDeadline = usingAssumedDeadline,
            assumptions = assumptions,
        )
    }

    private fun sameDayDeadline(now: Long): Long =
        timeProvider.startOfDayMillis(now) + DEFAULT_SAME_DAY_DEADLINE_MINUTE * MINUTE_MS

    companion object {
        /** Heuristic default task duration (PRD §10.6, docs 05 §3). Locked default. */
        const val DEFAULT_TASK_DURATION_MIN = 60

        /**
         * Heuristic same-day deadline: **23:59** (PRD §10.6, `DEFAULT_SAME_DAY_DEADLINE = 23:59`).
         * Expressed as minutes-from-midnight so it is exact (23 * 60 + 59 = 1439).
         */
        const val DEFAULT_SAME_DAY_DEADLINE_MINUTE = 23 * 60 + 59

        /** How late the "delay" scenario assumes the user starts (heuristic, tunable). */
        const val DELAY_OFFSET_MIN = 240

        const val MINUTE_MS = 60_000L

        const val ASSUMPTION_DURATION = "duration-assumed"

        const val ASSUMPTION_DEADLINE = "deadline-assumed"

        const val ASSUMPTION_DELAY = "delay-offset-assumed"
    }
}

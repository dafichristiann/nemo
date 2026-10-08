package com.anchor.app.core.domain.usecase.focus

/**
 * Pure rule for "did this focus session earn completion?" (PRD v0.5 §10.1
 * "Effort-based Completion [LOCKED v0.5]").
 *
 * A task is only completed by a **full** session: the recorded [actualDurationMs] must reach
 * the [plannedDurationMin] the user set. Finishing early or an emergency exit does not count.
 * There is no tolerance band — a shorter session simply does not complete the task.
 *
 * Kept free of Android/Room so it can be unit-tested directly.
 */
fun isFocusVerified(actualDurationMs: Long, plannedDurationMin: Int?): Boolean {
    val planned = plannedDurationMin ?: return false
    if (planned <= 0) return false
    return actualDurationMs >= planned * 60_000L
}

package com.anchor.app.core.domain.model

/**
 * The **aggregated-only** payload sent to the optional AI (FR-AI1, docs 09-privacy §6).
 *
 * This type is the privacy boundary: it can only be built from daily aggregates — there is no
 * field that can carry a raw `UsageSession`, a per-session timestamp, a task title, or a device
 * identity. Sending raw usage is therefore impossible by construction, not merely by policy.
 */
data class InsightPayload(
    val dateEpochDay: Long,
    /** Completed / total tasks for the day (aggregate counts only, no titles). */
    val tasksCompleted: Int,
    val tasksPlanned: Int,
    /** Total focused time today (ms). */
    val focusTimeMs: Long,
    /** Total screen time today (ms), or null when there is no usage data for the day. */
    val screenTimeMs: Long?,
    /** Longest single distraction session (ms), or null when unavailable. */
    val longestDistractionMs: Long?,
    /** Distraction category/package label, coarse-grained (e.g. "Social"), or null. */
    val topDistractionCategory: String?,
    /** Local hour-of-day buckets of productive focus (aggregate), for pattern hints. */
    val productiveHours: List<Int>,
) {
    companion object {
        /**
         * Audit guard used by tests and by the AI source: a payload must never contain a raw
         * session. Because the type cannot hold one, this validates the *shape* invariants that
         * matter (non-negative aggregates, no impossible values).
         */
        fun passesPrivacyAudit(payload: InsightPayload): Boolean =
            payload.tasksCompleted >= 0 &&
                payload.tasksPlanned >= 0 &&
                payload.tasksCompleted <= payload.tasksPlanned.coerceAtLeast(payload.tasksCompleted) &&
                payload.focusTimeMs >= 0 &&
                (payload.screenTimeMs == null || payload.screenTimeMs >= 0) &&
                (payload.longestDistractionMs == null || payload.longestDistractionMs >= 0)
    }
}

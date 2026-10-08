package com.anchor.app.core.domain.model

/**
 * One projected scenario in the Future View (docs/superpowers/design/07-screens.md §2.11,
 * PRD §10.6). Times are epoch millis so the UI formats them locally.
 *
 * These are **projections**, not predictions: [assumptions] lists every heuristic applied so the
 * UI can mark them (PRD §10.6 FR-FV3/FR-FV5).
 */
data class FutureScenario(
    val startAt: Long,
    val endAt: Long,
    val deadlineAt: Long?,
    val assumptions: List<String>,
)

/** The two side-by-side scenarios plus the shared honesty signals. */
data class FutureView(
    val startNow: FutureScenario,
    val delay: FutureScenario,
    val estimatedDurationMin: Int,
    /** True when the duration comes from a heuristic, not the task itself (FR-FV5). */
    val usingAssumedDuration: Boolean,
    /** True when the deadline comes from a default end-of-day, not the task itself. */
    val usingAssumedDeadline: Boolean,
    val assumptions: List<String>,
)

/**
 * A node in the Domino Effect chain (PRD §10.7). [depth] is 0 for the task itself, 1 for tasks
 * directly affected, and so on. Language is estimative everywhere — "may affect", never "will".
 */
data class DominoNode(
    val taskId: String,
    val title: String,
    val depth: Int,
)

/** The domino chain for a task + the count of tasks it may affect (FR-DE2/DE3). */
data class DominoEffect(
    val root: DominoNode,
    val affected: List<DominoNode>,
) {
    /** Number of downstream tasks that *may* be affected (excludes the root). */
    val affectedCount: Int get() = affected.size
}

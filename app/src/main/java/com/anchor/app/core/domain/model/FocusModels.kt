package com.anchor.app.core.domain.model

/**
 * Focus session mode (docs/superpowers/design/03-database-schema.md §4.1, PRD §10.5).
 *
 *  - [GENTLE] only reduces interruptions — never blocks anything;
 *  - [STRICT] may temporarily limit selected distraction apps;
 *  - [CUSTOM] lets the user pick which apps are limited.
 *
 * Used by [com.anchor.app.core.domain.blocking.BlockingStrategy]. Stored as `String` in Room.
 */
enum class FocusMode { GENTLE, STRICT, CUSTOM }

/** Lifecycle of a focus session (docs 03 §4.1). */
enum class FocusStatus { ACTIVE, PAUSED, FINISHED, ABORTED }

/** A focus session (docs 03 §4.1, PRD §10.5). */
data class FocusSession(
    val id: String,
    val taskId: String?,
    val mode: FocusMode,
    val plannedDurationSec: Int?,
    val actualDurationMs: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val status: FocusStatus,
    val blockedApps: List<String>,
    val fallbackReason: String?,
    val rating: Int? = null,
)

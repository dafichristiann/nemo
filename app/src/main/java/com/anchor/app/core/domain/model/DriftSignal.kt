package com.anchor.app.core.domain.model

/**
 * A drift signal: current usage of [packageName] deviates from the user's personal baseline
 * (docs/superpowers/design/04-intervention-engine.md §2.4, PRD §10.3).
 *
 * Language is neutral by contract — this object carries facts, never judgement (FR-D5).
 */
data class DriftSignal(
    val packageName: String,
    val currentSessionMs: Long,
    val baselineAvgMs: Long,
    val deviationSigma: Double,
    /** A high/urgent priority task still open, if any (drives contextual level, FR-D3). */
    val relatedTaskId: String?,
    val detectedAt: Long,
)

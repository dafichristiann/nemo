package com.anchor.app.core.domain.blocking

import com.anchor.app.core.domain.model.FocusMode

/**
 * Seam for Focus blocking (docs/superpowers/design/04-intervention-engine.md §2.5,
 * 08-permissions.md §5, Spike B `13-spike-b-accessibility.md`).
 *
 * Phase 3 Focus has three modes (PRD §10.5):
 *  - [FocusMode.GENTLE] never blocks — it only reduces interruptions;
 *  - [FocusMode.STRICT]/[FocusMode.CUSTOM] *may* limit distraction apps via an
 *    `AccessibilityService`, which is **experimental, feature-flagged and optional**.
 *
 * Contract (Spike B decision): any inability to engage blocking — permission denied, service
 * disabled by the OS, aggressive OEM — must **degrade to [FocusMode.GENTLE]**, never fail hard.
 * Implementations are introduced in Sprint 5; this interface only fixes the boundary now.
 */
interface BlockingStrategy {

    /** The mode this strategy actually applies (may differ from the requested one after fallback). */
    val effectiveMode: FocusMode

    /**
     * True when this strategy can block right now (permission + service available). Suspending
     * so implementations may consult an authoritative (non-cached) source of truth.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Requests that [packages] be blocked for the session. Returns true when blocking is active;
     * false when the caller must fall back to [FocusMode.GENTLE] (record the reason).
     */
    suspend fun engage(packages: Set<String>): Boolean = engage(packages, effectiveMode)

    /**
     * Overload accepting an explicit [mode] to enforce on the session.
     */
    suspend fun engage(packages: Set<String>, mode: FocusMode): Boolean = engage(packages)

    /** Releases any blocking engaged for the current session. Must be safe to call twice. */
    suspend fun release()
}

package com.anchor.app.platform.blocking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Live snapshot of what the current focus session is blocking (FR-F4/F8). */
data class BlockingSnapshot(
    val sessionActive: Boolean = false,
    val blockedApps: Set<String> = emptySet(),
)

/**
 * Single source of truth for the *live* blocking state of a focus session (PRD v0.4 §10.5).
 *
 * Why in-memory: the [com.anchor.app.platform.accessibility.AnchorAccessibilityService] and the
 * app run in the same process, so a `@Singleton` can bridge them without Room. This is ephemeral
 * session state, not persistable data — if the OS kills the process, the session is over anyway.
 *
 * Written by [AccessibilityBlockingStrategy] on engage/release; read by the service on each
 * window-state change. [release] is idempotent (matches the `BlockingStrategy` contract).
 */
@Singleton
class BlockingState @Inject constructor() {

    private val _state = MutableStateFlow(BlockingSnapshot())
    val state: StateFlow<BlockingSnapshot> = _state.asStateFlow()

    /** Marks a session active and records [packages] as the blocked set. */
    fun engage(packages: Set<String>) {
        _state.value = BlockingSnapshot(sessionActive = true, blockedApps = packages)
    }

    /** Clears the state. Safe to call repeatedly (no-op when already clear). */
    fun release() {
        _state.value = BlockingSnapshot()
    }

    fun snapshot(): BlockingSnapshot = _state.value
}

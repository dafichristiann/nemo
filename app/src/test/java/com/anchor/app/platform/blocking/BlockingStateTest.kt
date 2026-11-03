package com.anchor.app.platform.blocking

import com.anchor.app.core.domain.model.FocusMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BlockingState] (FR-F4/F8 seam). Pure JVM — no Android deps.
 * Verifies the live-session state the AccessibilityService reads.
 */
class BlockingStateTest {

    @Test
    fun engage_setsSessionActiveAndBlockedApps() {
        val state = BlockingState()
        state.engage(setOf("com.tiktok", "com.instagram"))

        val snap = state.snapshot()
        assertTrue(snap.sessionActive)
        assertEquals(setOf("com.tiktok", "com.instagram"), snap.blockedApps)
        assertEquals(FocusMode.STRICT, snap.mode)
    }

    @Test
    fun engage_withGentleMode_storesGentleMode() {
        val state = BlockingState()
        state.engage(setOf("com.tiktok"), FocusMode.GENTLE)

        val snap = state.snapshot()
        assertTrue(snap.sessionActive)
        assertEquals(setOf("com.tiktok"), snap.blockedApps)
        assertEquals(FocusMode.GENTLE, snap.mode)
    }

    @Test
    fun release_clearsState() {
        val state = BlockingState()
        state.engage(setOf("com.tiktok"))
        state.release()

        val snap = state.snapshot()
        assertFalse(snap.sessionActive)
        assertTrue(snap.blockedApps.isEmpty())
    }

    @Test
    fun release_isIdempotent() {
        val state = BlockingState()
        state.engage(setOf("com.tiktok"))
        state.release()
        state.release() // Must not throw; contract says safe to call twice.

        assertFalse(state.snapshot().sessionActive)
    }

    @Test
    fun stateFlow_reflectsLatestSnapshot() {
        val state = BlockingState()
        state.engage(setOf("a"))
        assertEquals(setOf("a"), state.state.value.blockedApps)
        state.release()
        assertFalse(state.state.value.sessionActive)
    }
}

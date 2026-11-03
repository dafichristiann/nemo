package com.anchor.app.platform.blocking

import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.core.domain.model.FocusMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AccessibilityBlockingStrategy] — the three-gate readiness rule
 * (flag + opt-in toggle + OS service) and its engage/release behaviour (FR-F4/F8,
 * PRD v0.4 §10.5, Spike B). Uses a fake [BlockingGate] so it stays pure JVM.
 */
class AccessibilityBlockingStrategyTest {

    /** Scriptable gate: flip any of the three conditions independently. */
    private class FakeGate(
        var flagOn: Boolean = true,
        var toggleOn: Boolean = true,
        var serviceOn: Boolean = true,
    ) : BlockingGate {
        override fun isBuildFlagOn(): Boolean = flagOn
        override fun isToggleOn(): Boolean = toggleOn
        override suspend fun isToggleOnNow(): Boolean = toggleOn
        override fun isServiceEnabled(): Boolean = serviceOn
    }

    private fun strategy(gate: BlockingGate, state: BlockingState = BlockingState()) =
        AccessibilityBlockingStrategy(gate, state)

    @Test
    fun effectiveMode_isStrictWhenReady() {
        assertEquals(FocusMode.STRICT, strategy(FakeGate()).effectiveMode)
    }

    @Test
    fun isAvailable_trueOnlyWhenAllThreeGatesPass() = runTest {
        assertTrue(strategy(FakeGate(flagOn = true, toggleOn = true, serviceOn = true)).isAvailable())
    }

    @Test
    fun isAvailable_falseWhenFlagOff() = runTest {
        assertFalse(strategy(FakeGate(flagOn = false)).isAvailable())
    }

    @Test
    fun isAvailable_falseWhenToggleOff() = runTest {
        assertFalse(strategy(FakeGate(toggleOn = false)).isAvailable())
    }

    @Test
    fun isAvailable_falseWhenServiceDisabled() = runTest {
        assertFalse(strategy(FakeGate(serviceOn = false)).isAvailable())
    }

    @Test
    fun engage_whenAvailable_fillsStateAndReturnsTrue() = runTest {
        val state = BlockingState()
        val s = strategy(FakeGate(), state)

        val engaged = s.engage(setOf("com.tiktok"))

        assertTrue(engaged)
        assertTrue(state.snapshot().sessionActive)
        assertEquals(setOf("com.tiktok"), state.snapshot().blockedApps)
        assertEquals(FocusMode.STRICT, state.snapshot().mode)
    }

    @Test
    fun engage_withMode_storesSpecifiedMode() = runTest {
        val state = BlockingState()
        val s = strategy(FakeGate(), state)

        val engaged = s.engage(setOf("com.tiktok"), FocusMode.GENTLE)

        assertTrue(engaged)
        assertTrue(state.snapshot().sessionActive)
        assertEquals(setOf("com.tiktok"), state.snapshot().blockedApps)
        assertEquals(FocusMode.GENTLE, state.snapshot().mode)
    }

    @Test
    fun engage_whenUnavailable_returnsFalseAndLeavesStateEmpty() = runTest {
        val state = BlockingState()
        val s = strategy(FakeGate(serviceOn = false), state)

        val engaged = s.engage(setOf("com.tiktok"))

        assertFalse(engaged)
        assertFalse(state.snapshot().sessionActive)
    }

    @Test
    fun release_clearsState() = runTest {
        val state = BlockingState()
        val s = strategy(FakeGate(), state)
        s.engage(setOf("com.tiktok"))

        s.release()

        assertFalse(state.snapshot().sessionActive)
    }
}

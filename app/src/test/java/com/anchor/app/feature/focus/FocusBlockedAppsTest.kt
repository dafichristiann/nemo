package com.anchor.app.feature.focus

import com.anchor.app.core.domain.model.FocusMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Opsi 2 (FR-S4 → FR-F4): the Focus Setup must pre-fill the user's blocked apps into
 * Strict/Custom sessions, and never into Gentle (PRD §10.5). This covers the pure
 * mode → blocked-apps decision [effectiveBlockedApps] used by `FocusViewModel.onBegin`.
 */
class FocusBlockedAppsTest {

    private val blocked = setOf("com.tiktok", "com.instagram")

    @Test
    fun strict_pausesTheUsersBlockedApps() {
        assertEquals(blocked, effectiveBlockedApps(FocusMode.STRICT, blocked))
    }

    @Test
    fun custom_pausesTheUsersBlockedApps() {
        assertEquals(blocked, effectiveBlockedApps(FocusMode.CUSTOM, blocked))
    }

    @Test
    fun gentle_neverPausesAnything() {
        assertTrue(effectiveBlockedApps(FocusMode.GENTLE, blocked).isEmpty())
    }

    @Test
    fun strict_withNoBlockedApps_yieldsEmpty() {
        assertTrue(effectiveBlockedApps(FocusMode.STRICT, emptySet()).isEmpty())
    }
}

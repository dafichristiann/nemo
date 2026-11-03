package com.anchor.app.core

import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.testutil.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun fakeSettingsRepository_defaultsAndDoomscrollUpdates() = runTest {
        val repo = FakeSettingsRepository()

        val initial = repo.get()
        assertFalse(initial.doomscrollAlertEnabled)
        assertEquals(AppSettings.DEFAULT_DOOMSCROLL_THRESHOLD_MIN, initial.doomscrollThresholdMinutes)

        repo.setDoomscrollAlertEnabled(true)
        assertTrue(repo.get().doomscrollAlertEnabled)

        repo.setDoomscrollThresholdMinutes(30)
        assertEquals(30, repo.get().doomscrollThresholdMinutes)
    }
}

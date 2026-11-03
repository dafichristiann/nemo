package com.anchor.app.feature.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSoundscapeTest {

    @Test
    fun focusUiState_soundscapeEnabled_defaultsToTrue() {
        val state = FocusUiState()
        assertTrue(state.soundscapeEnabled)
    }

    @Test
    fun focusUiState_soundscapeEnabled_canBeToggled() {
        val state = FocusUiState(soundscapeEnabled = false)
        assertFalse(state.soundscapeEnabled)
    }
}

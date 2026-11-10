package com.anchor.app.feature.focus

import com.anchor.app.platform.audio.LofiGenre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    @Test
    fun focusUiState_lofiGenre_defaultsToChill() {
        val state = FocusUiState()
        assertEquals(LofiGenre.CHILL, state.selectedLofiGenre)
    }

    @Test
    fun focusUiState_lofiGenre_canBeSelected() {
        val state = FocusUiState(selectedLofiGenre = LofiGenre.CHRISTIAN)
        assertEquals(LofiGenre.CHRISTIAN, state.selectedLofiGenre)
    }

    @Test
    fun lofiGenre_containsSevenDistinctGenres() {
        assertEquals(7, LofiGenre.ALL.size)
        val uniqueIds = LofiGenre.ALL.map { it.id }.toSet()
        assertEquals(7, uniqueIds.size)
    }

    @Test
    fun lofiGenre_fromId_resolvesOrDefaults() {
        assertEquals(LofiGenre.JAZZ, LofiGenre.fromId("jazz"))
        assertEquals(LofiGenre.CHRISTIAN, LofiGenre.fromId("christian"))
        assertEquals(LofiGenre.DEFAULT, LofiGenre.fromId("unknown_genre"))
        assertEquals(LofiGenre.DEFAULT, LofiGenre.fromId(null))
    }

    @Test
    fun lofiGenre_allGenresHaveValidResources() {
        LofiGenre.ALL.forEach { genre ->
            assertNotEquals(0, genre.soundResId)
            assertNotEquals(0, genre.titleRes)
            assertNotEquals(0, genre.descRes)
            assertTrue(genre.tag.isNotBlank())
        }
    }
}

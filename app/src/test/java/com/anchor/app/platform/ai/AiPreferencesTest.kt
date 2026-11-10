package com.anchor.app.platform.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiPreferencesTest {

    @Test
    fun maskKey_masksStandardApiKey() {
        val masked = AiPreferences.maskKey("AIzaSyD-1234567890abcdef")
        assertEquals("AIza••••cdef", masked)
    }

    @Test
    fun maskKey_handlesShortKey() {
        val masked = AiPreferences.maskKey("12345678")
        assertEquals("••••••••", masked)
    }

    @Test
    fun maskKey_handlesEmptyOrBlankKey() {
        assertEquals("", AiPreferences.maskKey(""))
        assertEquals("", AiPreferences.maskKey("   "))
    }
}

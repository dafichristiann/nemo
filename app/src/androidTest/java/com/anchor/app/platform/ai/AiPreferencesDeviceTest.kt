package com.anchor.app.platform.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiPreferencesDeviceTest {

    private lateinit var context: Context
    private val prefsName = "nemo_ai_prefs"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun setCustomApiKey_encryptsWithKeyStoreAndPersistsSecurely() {
        val aiPrefs = AiPreferences(context)
        val plainKey = "AIzaSyD-HardwareKeyStoreTest123456"

        aiPrefs.setCustomApiKey(plainKey)

        // Memory State
        assertEquals(plainKey, aiPrefs.getCustomApiKey())
        assertTrue(aiPrefs.hasCustomApiKey())
        assertEquals(plainKey, aiPrefs.getEffectiveApiKey())

        // Disk State (SharedPreferences)
        val rawPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val encrypted = rawPrefs.getString("custom_gemini_api_key_enc", null)
        val legacy = rawPrefs.getString("custom_gemini_api_key", null)

        assertNull("Plaintext key must not be written to legacy key slot", legacy)
        assertTrue("Encrypted key must be non-null and not empty", !encrypted.isNullOrEmpty())
        assertNotEquals("Stored value must NOT be plaintext", plainKey, encrypted)

        // Decryption round-trip with a new instance
        val reloadedPrefs = AiPreferences(context)
        assertEquals(plainKey, reloadedPrefs.getCustomApiKey())

        // Clear test
        reloadedPrefs.clear()
        assertEquals("", reloadedPrefs.getCustomApiKey())
        assertFalse(reloadedPrefs.hasCustomApiKey())
        assertNull(rawPrefs.getString("custom_gemini_api_key_enc", null))
    }

    @Test
    fun legacyKeyMigration_automaticallyEncryptsAndRemovesPlaintext() {
        val rawPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val legacyKey = "AIzaSyD-LegacyUnencryptedKey-999"
        rawPrefs.edit().putString("custom_gemini_api_key", legacyKey).commit()

        val aiPrefs = AiPreferences(context)

        assertEquals(legacyKey, aiPrefs.getCustomApiKey())
        assertNull("Legacy plaintext key must be removed from prefs after migration", rawPrefs.getString("custom_gemini_api_key", null))
        val encrypted = rawPrefs.getString("custom_gemini_api_key_enc", null)
        assertTrue("Encrypted slot must be populated", !encrypted.isNullOrEmpty())
        assertNotEquals(legacyKey, encrypted)
    }
}

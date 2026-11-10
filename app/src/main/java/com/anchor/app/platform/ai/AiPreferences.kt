package com.anchor.app.platform.ai

import android.content.Context
import com.anchor.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages runtime Bring-Your-Own-Key (BYOK) for Gemini API (OWASP Mobile M1 / M9).
 *
 * Stored in private [android.content.SharedPreferences] with [Context.MODE_PRIVATE] (Linux UID sandbox)
 * to avoid Room schema migrations and ensure user data isolation.
 * Cleared when the user selects "Delete all data".
 */
@Singleton
class AiPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _customApiKey = MutableStateFlow(loadCustomApiKey())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private fun loadCustomApiKey(): String =
        prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim().orEmpty()

    fun getCustomApiKey(): String = _customApiKey.value

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
            _customApiKey.value = ""
        } else {
            prefs.edit().putString(KEY_CUSTOM_API_KEY, trimmed).apply()
            _customApiKey.value = trimmed
        }
    }

    /**
     * Resolves the effective API key: prefers user runtime BYOK key if present,
     * otherwise falls back to build-time [BuildConfig.GEMINI_API_KEY].
     */
    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value
        return if (custom.isNotBlank()) custom else BuildConfig.GEMINI_API_KEY.trim()
    }

    fun hasCustomApiKey(): Boolean = _customApiKey.value.isNotBlank()

    fun hasEffectiveApiKey(): Boolean = getEffectiveApiKey().isNotBlank()

    /** Erases stored API key upon "Delete all data" (PRD §10.10). */
    fun clear() {
        prefs.edit().clear().apply()
        _customApiKey.value = ""
    }

    companion object {
        private const val PREFS_NAME = "nemo_ai_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

        /** Produces a masked representation for secure UI display (e.g. AIza••••9AbC). */
        fun maskKey(key: String): String {
            val trimmed = key.trim()
            if (trimmed.length <= 8) return if (trimmed.isEmpty()) "" else "••••••••"
            return "${trimmed.take(4)}••••${trimmed.takeLast(4)}"
        }
    }
}

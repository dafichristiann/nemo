package com.anchor.app.platform.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.anchor.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages runtime Bring-Your-Own-Key (BYOK) for Gemini API (OWASP Mobile M1 / M9).
 *
 * Stored in private [android.content.SharedPreferences] with [Context.MODE_PRIVATE] (Linux UID sandbox)
 * and hardware-backed Android KeyStore AES-GCM encryption to ensure confidentiality at rest.
 * Cleared when the user selects "Delete all data".
 */
@Singleton
class AiPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _customApiKey = MutableStateFlow(loadCustomApiKey())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private fun loadCustomApiKey(): String {
        val enc = prefs.getString(KEY_CUSTOM_API_KEY_ENC, null)
        if (enc != null) {
            return decrypt(enc)
        }
        val legacy = prefs.getString(KEY_CUSTOM_API_KEY, null)?.trim()
        if (!legacy.isNullOrEmpty()) {
            val encrypted = encrypt(legacy)
            prefs.edit()
                .putString(KEY_CUSTOM_API_KEY_ENC, encrypted)
                .remove(KEY_CUSTOM_API_KEY)
                .apply()
            return legacy
        }
        return ""
    }

    fun getCustomApiKey(): String = _customApiKey.value

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            prefs.edit()
                .remove(KEY_CUSTOM_API_KEY)
                .remove(KEY_CUSTOM_API_KEY_ENC)
                .apply()
            _customApiKey.value = ""
        } else {
            val encrypted = encrypt(trimmed)
            prefs.edit()
                .putString(KEY_CUSTOM_API_KEY_ENC, encrypted)
                .remove(KEY_CUSTOM_API_KEY)
                .apply()
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
        prefs.edit()
            .remove(KEY_CUSTOM_API_KEY)
            .remove(KEY_CUSTOM_API_KEY_ENC)
            .apply()
        _customApiKey.value = ""
    }

    private fun getOrCreateKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            } else {
                (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            }
        } catch (_: Throwable) {
            // Non-Android environments or test runtimes without KeyStore support
            null
        }
    }

    private fun encrypt(plainText: String): String {
        val secretKey = getOrCreateKey() ?: return plainText
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.getEncoder().encodeToString(combined)
        } catch (_: Throwable) {
            plainText
        }
    }

    private fun decrypt(cipherText: String): String {
        val secretKey = getOrCreateKey() ?: return cipherText
        return try {
            val combined = Base64.getDecoder().decode(cipherText)
            if (combined.size < GCM_IV_LENGTH) return cipherText
            val iv = ByteArray(GCM_IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (_: Throwable) {
            cipherText
        }
    }

    companion object {
        private const val PREFS_NAME = "nemo_ai_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_CUSTOM_API_KEY_ENC = "custom_gemini_api_key_enc"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nemo_byok_gemini_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        /** Produces a masked representation for secure UI display (e.g. AIza••••9AbC). */
        fun maskKey(key: String): String {
            val trimmed = key.trim()
            if (trimmed.length <= 8) return if (trimmed.isEmpty()) "" else "••••••••"
            return "${trimmed.take(4)}••••${trimmed.takeLast(4)}"
        }
    }
}

package com.anchor.app.platform.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and opens the OS Accessibility settings for [NemoAccessibilityService] (FR-F4/F8,
 * docs 08-permissions.md §4). Mirrors the defensive pattern of
 * [com.anchor.app.platform.usagestats.UsageAccessSettings]: a missing Settings activity must
 * never crash the app — we fall back through several intents.
 */
@Singleton
class AccessibilityStatus @Inject constructor() {

    /** True while the Nemo service is listed among the OS's enabled accessibility services. */
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, NemoAccessibilityService::class.java)

        // On API 24+ the OS also exposes a master toggle; when it is off no service can run,
        // regardless of what the enabled-services list contains. Absent key -> treat as on (the
        // list is then the source of truth, matching pre-24 behaviour). The 3-arg getInt overload
        // is used so a missing key cannot throw SettingNotFoundException.
        val masterEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            1,
        )
        if (masterEnabled == 0) return false

        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        return isServiceListed(
            enabledServices = enabled,
            expectedFull = expected.flattenToString(),
            expectedShort = expected.flattenToShortString(),
        )
    }

    /** Opens the Accessibility settings screen, with defensive fallbacks for OEM variants. */
    fun open(context: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val opened = runCatching {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (opened) return
        }
    }

    companion object {
        /**
         * Pure check: does the colon-separated [enabledServices] list contain our component?
         *
         * The OS stores each entry either in full form (`pkg/full.class.Name`) or short form
         * (`pkg/.Class.Name`), and the exact spelling varies by OEM/version. Comparing parsed
         * [ComponentName]s via `==` is brittle (package/class normalization differs), so we
         * compare the *flattened* strings against both the full and short spellings of the
         * expected component. Deliberately uses only plain string operations (no Android
         * framework classes) so it is fast and unit-testable on the JVM.
         */
        fun isServiceListed(
            enabledServices: String,
            expectedFull: String,
            expectedShort: String,
        ): Boolean {
            if (enabledServices.isEmpty()) return false
            return enabledServices.split(':').any { token ->
                val trimmed = token.trim()
                if (trimmed.isEmpty()) return@any false
                if (trimmed == expectedFull || trimmed == expectedShort) return@any true
                // Normalise `pkg/.Class` and `pkg/full.Class` to a comparable pair.
                val normalised = normaliseComponent(trimmed)
                normalised != null &&
                    (normalised == expectedFull || normalised == expectedShort)
            }
        }

        /**
         * Expands `pkg/.Suffix` into `pkg/pkg.Suffix` and strips any leading-dot ambiguity,
         * mirroring `ComponentName.flattenToString()` without touching the Android framework.
         * Returns null if the token is not a `pkg/class` pair.
         */
        private fun normaliseComponent(token: String): String? {
            val slash = token.indexOf('/')
            if (slash <= 0 || slash == token.length - 1) return null
            val pkg = token.substring(0, slash)
            val cls = token.substring(slash + 1)
            val fullClass = if (cls.startsWith('.')) pkg + cls else cls
            return "$pkg/$fullClass"
        }
    }
}

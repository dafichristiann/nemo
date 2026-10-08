package com.anchor.app.platform.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and opens the OS Accessibility settings for [AnchorAccessibilityService] (FR-F4/F8,
 * docs 08-permissions.md §4). Mirrors the defensive pattern of
 * [com.anchor.app.platform.usagestats.UsageAccessSettings]: a missing Settings activity must
 * never crash the app — we fall back through several intents.
 */
@Singleton
class AccessibilityStatus @Inject constructor() {

    /** True while the Anchor service is listed among the OS's enabled accessibility services. */
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AnchorAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next()) ?: continue
            if (component == expected) return true
        }
        return false
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
}

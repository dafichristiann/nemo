package com.anchor.app.platform.usagestats

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Opens the OS screen where the user grants `PACKAGE_USAGE_STATS` (special access).
 *
 * `PACKAGE_USAGE_STATS` can only be granted in system Settings, never in-app (PRD §14). We try
 * the direct "Usage access" list first; on some OEMs that intent is absent or filtered, so we
 * fall back to the app's own details page and, last, to the top-level Settings. All paths are
 * wrapped defensively — a missing activity must never crash Wellbeing (docs 02 §6).
 */
object UsageAccessSettings {

    fun open(context: Context) {
        val candidates = listOf(
            // Direct: Settings → Special access → Usage access (list of apps).
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            // Some OEMs expose it per-package via a component; try the app details page next.
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", context.packageName, null)),
            // Build-specific usage-access screen (AOSP/GMS variants).
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$UsageAccessSettingsActivity",
                ),
            ),
            // Last resort: top-level Settings.
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

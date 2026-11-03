package com.anchor.app.platform.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A launchable app the user can choose to block (label shown, package stored). */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
)

/**
 * Lists the launchable apps installed on the device so the user can pick which ones to pause
 * during Strict/Custom focus (FR-S4, PRD §10.10).
 *
 * Deliberately does **not** use `QUERY_ALL_PACKAGES` (PRD §08 §7 forbids it). Instead the
 * manifest declares a targeted `<queries>` intent (MAIN/LAUNCHER), which is exactly the
 * recommended alternative, so we only ever see apps with a launcher entry.
 */
@Singleton
class InstalledAppsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Launchable apps, sorted by label, excluding Nemo itself. */
    fun launchableApps(): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved: List<ResolveInfo> = runCatching {
            pm.queryIntentActivities(intent, 0)
        }.getOrDefault(emptyList())

        return resolved
            .asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                InstalledApp(
                    packageName = pkg,
                    label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg),
                    icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Icon drawable for a package, or null if unavailable. */
    fun iconFor(packageName: String): Drawable? = runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    /** Friendly label for a package, falling back to the package name if it can't be resolved. */
    fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}

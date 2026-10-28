package com.anchor.app.platform.usagestats

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [UsageStatsSource] backed by [UsageStatsManager]
 * (Spike A, Sprint 2 · docs 02-architecture.md §4.1).
 *
 * Behaviour notes:
 *  - `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` (API 29+) map to session start/end. On API 26–28
 *    the constant names differ (`MOVE_TO_FOREGROUND`/`MOVE_TO_BACKGROUND`), so both are
 *    handled explicitly.
 *  - Results are sanitised: zero-length and inverted intervals are dropped so downstream
 *    aggregation never sees negative durations.
 *  - Any platform exception yields an empty list — the app must degrade gracefully rather
 *    than crash when an OEM blocks the API (PRD §14, docs 02 §6).
 */
@Singleton
class AndroidUsageStatsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : UsageStatsSource {

    private val manager: UsageStatsManager?
        get() = context.getSystemService()

    override fun hasPermission(): Boolean {
        // PACKAGE_USAGE_STATS is a *special* permission granted via Settings → Usage access,
        // not a runtime permission. `queryUsageStats` returns a non-null (often empty) list
        // even when access is missing, so it is NOT a reliable probe. The correct check is
        // the AppOps "get_usage_stats" op for this app's uid.
        return runCatching {
            val appOps = context.getSystemService<AppOpsManager>() ?: return false
            @Suppress("DEPRECATION")
            val mode = appOps.checkOpNoThrow(
                OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
            if (mode == AppOpsManager.MODE_DEFAULT) {
                // MODE_DEFAULT: fall back to the declared-permission grant state.
                context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                mode == AppOpsManager.MODE_ALLOWED
            }
        }.getOrDefault(false)
    }

    override fun queryEvents(startMillis: Long, endMillis: Long): List<UsageEvent> {
        val m = manager ?: return emptyList()
        val events = runCatching { m.queryEvents(startMillis, endMillis) }.getOrNull()
            ?: return emptyList()

        val result = mutableListOf<UsageEvent>()
        var currentPackage: String? = null
        var currentStart: Long = 0L

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, FOREGROUND_EVENT -> {
                    // Close any dangling session from a missed PAUSE, then open a new one.
                    currentPackage?.let { pkg ->
                        addIfValid(result, pkg, currentStart, event.timeStamp)
                    }
                    currentPackage = event.packageName
                    currentStart = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, BACKGROUND_EVENT -> {
                    if (event.packageName == currentPackage) {
                        addIfValid(result, event.packageName, currentStart, event.timeStamp)
                        currentPackage = null
                    } else if (currentPackage == null) {
                        // App opened before startMillis and closed inside this window:
                        // clamp start to startMillis so active usage is not lost.
                        addIfValid(result, event.packageName, startMillis, event.timeStamp)
                    }
                }
            }
        }
        // A session still open at the window edge is closed at endMillis.
        currentPackage?.let { addIfValid(result, it, currentStart, endMillis) }

        return result.sortedBy { it.startTime }
    }

    override fun queryTotals(startMillis: Long, endMillis: Long): List<AppTotal> {
        // Derive totals from the event stream (the same source of truth as [queryEvents]) so the
        // result is genuinely clamped to [startMillis, endMillis). `queryUsageStats(INTERVAL_DAILY,
        // ...)` would instead return whole daily buckets that can extend beyond the window, which
        // over-counts "today so far" style queries.
        return queryEvents(startMillis, endMillis)
            .groupBy { it.packageName }
            .map { (pkg, events) -> AppTotal(pkg, events.sumOf { it.durationMs }) }
            .sortedByDescending { it.totalTimeMs }
    }

    private fun addIfValid(
        target: MutableList<UsageEvent>,
        packageName: String,
        start: Long,
        end: Long,
    ) {
        if (end > start && start > 0L) {
            target += UsageEvent(packageName = packageName, startTime = start, endTime = end)
        }
    }

    private companion object {
        // AppOps op name for the PACKAGE_USAGE_STATS special permission.
        const val OPSTR_GET_USAGE_STATS = "android:get_usage_stats"

        // ACTIVITY_* were renamed in API 29; the FOREGROUND/BACKGROUND aliases cover 26–28.
        const val FOREGROUND_EVENT = UsageEvents.Event.MOVE_TO_FOREGROUND
        const val BACKGROUND_EVENT = UsageEvents.Event.MOVE_TO_BACKGROUND
    }
}

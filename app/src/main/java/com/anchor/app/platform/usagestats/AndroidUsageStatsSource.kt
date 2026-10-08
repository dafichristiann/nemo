package com.anchor.app.platform.usagestats

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
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
        val m = manager ?: return false
        // queryEvents with a tiny window returns an empty cursor when access is missing;
        // a non-null cursor from queryUsageStats is the cheapest reliable probe.
        return runCatching {
            val now = System.currentTimeMillis()
            m.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - ONE_MINUTE_MS, now) != null
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
                    }
                }
            }
        }
        // A session still open at the window edge is closed at endMillis.
        currentPackage?.let { addIfValid(result, it, currentStart, endMillis) }

        return result.sortedBy { it.startTime }
    }

    override fun queryTotals(startMillis: Long, endMillis: Long): List<AppTotal> {
        val m = manager ?: return emptyList()
        val stats = runCatching { m.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMillis, endMillis) }
            .getOrNull()
            ?: return emptyList()

        return stats
            .filter { it.totalTimeInForeground > 0 && it.lastTimeUsed >= startMillis }
            .groupBy { it.packageName }
            .map { (pkg, list) -> AppTotal(pkg, list.sumOf { it.totalTimeInForeground }) }
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
        const val ONE_MINUTE_MS = 60_000L

        // ACTIVITY_* were renamed in API 29; the FOREGROUND/BACKGROUND aliases cover 26–28.
        const val FOREGROUND_EVENT = UsageEvents.Event.MOVE_TO_FOREGROUND
        const val BACKGROUND_EVENT = UsageEvents.Event.MOVE_TO_BACKGROUND
    }
}

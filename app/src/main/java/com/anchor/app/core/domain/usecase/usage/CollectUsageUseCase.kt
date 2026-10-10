package com.anchor.app.core.domain.usecase.usage

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.repository.UsageRepository
import com.anchor.app.platform.usagestats.UsageStatsSource
import javax.inject.Inject

/** Outcome of a collection pass, for logging/tests. */
data class CollectUsageResult(
    val collected: Int,
    val permissionMissing: Boolean,
)

/**
 * Collects raw app-usage sessions into the store (FR-W2, docs 02-architecture.md §4.1).
 *
 * Steps:
 *  1. Bail out (without crashing) if `PACKAGE_USAGE_STATS` is not granted — the app stays a
 *     Smart Todo (PRD §14).
 *  2. Query events since the last collection (or since [lookbackFloorMillis] on first run).
 *  3. Map to [UsageSession] and insert (idempotent — duplicate ids are ignored).
 *
 * Idempotent and safe to re-run (docs 02 §5). Overlapping windows never duplicate rows
 * because session ids are derived deterministically from package + start time.
 */
class CollectUsageUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageStats: UsageStatsSource,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        lookbackFloorMillis: Long = DEFAULT_LOOKBACK_MS,
        nowMillis: Long = timeProvider.nowMillis(),
    ): CollectUsageResult {
        if (!usageStats.hasPermission()) {
            return CollectUsageResult(collected = 0, permissionMissing = true)
        }

        // Start from the furthest point we have collected up to (max *end* time, not max start
        // time — a long session that started earlier must not hide apps used in between), or a
        // bounded lookback on first run.
        val from = usageRepository.latestSessionEnd()?.let { it + 1 } ?: (nowMillis - lookbackFloorMillis)
        val events = usageStats.queryEvents(from, nowMillis)

        val sessions = events.map { event ->
            UsageSession(
                id = sessionId(event.packageName, event.startTime),
                packageName = event.packageName,
                startTime = event.startTime,
                endTime = event.endTime,
                durationMs = event.durationMs,
                collectedAt = nowMillis,
            )
        }.filter { it.durationMs > 0 }

        if (sessions.isNotEmpty()) usageRepository.insertSessions(sessions)

        return CollectUsageResult(collected = sessions.size, permissionMissing = false)
    }

    /** Deterministic id → re-collecting an overlapping window is idempotent. */
    private fun sessionId(packageName: String, startTime: Long): String =
        "us-$packageName-$startTime"

    companion object {
        /** 30 days: bounded first-run lookback (also the retention window, docs 03 §10). */
        const val DEFAULT_LOOKBACK_MS = 30L * 24 * 60 * 60 * 1000
    }
}

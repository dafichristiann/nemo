package com.anchor.app.core.domain.usecase.usage

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.DriftSignal
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject/**
 * Detects drift against the **personal baseline** (FR-D1..D6,
 * docs/superpowers/design/04-intervention-engine.md §2, PRD §10.3).
 *
 * Drift fires only when all three hold (§2.3):
 *  1. the app has enough sample days (`>= MIN_SAMPLE_DAYS` — FR-D6);
 *  2. today's usage deviates `>= DRIFT_DEVIATION_THRESHOLD` sigma from the baseline;
 *  3. a HIGH/URGENT task is still open (the deviation actually matters).
 *
 * Never uses universal rules ("more than 30 min is bad") and never diagnoses (FR-D5).
 */
class DetectDriftUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    /** Returns the strongest [DriftSignal] for today, or null when there is no drift. */
    suspend operator fun invoke(now: Long = timeProvider.nowMillis()): DriftSignal? {
        val today = timeProvider.epochDayOf(now)
        val todaySummary = usageRepository.dailySummary(today) ?: return null
        if (todaySummary.topApps.isEmpty()) return null

        // Only bother if a meaningful task is still open (condition 3).
        val pendingTaskId = mostImportantOpenTaskId() ?: return null

        val baselineByApp = usageRepository.latestWeeklySummary()
            ?.baseline
            ?.associateBy { it.packageName }
            ?: return null

        var strongest: DriftSignal? = null
        todaySummary.topApps.forEach { app ->
            val baseline = baselineByApp[app.packageName] ?: return@forEach
            // Condition 1: thin data → never claim drift (FR-D6).
            if (baseline.sampleDays < MIN_SAMPLE_DAYS) return@forEach

            val current = app.totalTimeMs
            val avg = baseline.avgSessionMs
            if (avg <= 0L) return@forEach

            // Condition 2: deviation in sigma units; fall back to ratio when stddev is ~0.
            val sigma = if (baseline.stdDevMs >= EPSILON_MS) {
                (current - avg).toDouble() / baseline.stdDevMs
            } else {
                val ratio = current.toDouble() / avg
                if (ratio >= RATIO_FALLBACK_THRESHOLD) Double.MAX_VALUE else 0.0
            }
            if (sigma < DRIFT_DEVIATION_THRESHOLD) return@forEach

            val signal = DriftSignal(
                packageName = app.packageName,
                currentSessionMs = current,
                baselineAvgMs = avg,
                deviationSigma = sigma,
                relatedTaskId = pendingTaskId,
                detectedAt = now,
            )
            if (strongest == null || signal.currentSessionMs > strongest!!.currentSessionMs) {
                strongest = signal
            }
        }
        return strongest
    }

    /** Highest-priority still-open task (HIGH/URGENT first), or null. */
    private suspend fun mostImportantOpenTaskId(): String? {
        val open = taskRepository.observeAll().first()
            .filter { it.status == TaskStatus.TODO || it.status == TaskStatus.IN_PROGRESS }
            .filter { it.priority == TaskPriority.HIGH || it.priority == TaskPriority.URGENT }
        return open.maxByOrNull { it.priority.ordinal }?.id
    }

    companion object {
        /** Baseline is meaningless below this many days (FR-D6). */
        const val MIN_SAMPLE_DAYS = 3

        /** ≥ 2σ from habit (04-intervention-engine.md §2.3). Tunable, not locked. */
        const val DRIFT_DEVIATION_THRESHOLD = 2.0

        /** Guard against divide-by-zero when a baseline has ~0 std-dev. */
        const val EPSILON_MS = 60_000.0

        /** When std-dev is ~0, drift requires at least 1.5× the average. */
        const val RATIO_FALLBACK_THRESHOLD = 1.5
    }
}

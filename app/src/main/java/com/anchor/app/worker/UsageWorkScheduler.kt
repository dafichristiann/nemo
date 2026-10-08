package com.anchor.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central place that enqueues Anchor's background work (docs 02-architecture.md §5).
 *
 * Rules applied here:
 *  - `KEEP` for periodic work → never duplicate an already-scheduled chain.
 *  - No `setExpedited` (respect Doze; docs 02 §5).
 *  - Aggregation is chained **after** collection so a single pass stays consistent.
 */
@Singleton
class UsageWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    /**
     * Schedules periodic collection + a daily prune, then a one-off aggregate.
     * Safe to call repeatedly (idempotent via unique names + KEEP).
     */
    fun schedulePeriodic() {
        val collecting = PeriodicWorkRequestBuilder<UsageCollectWorker>(
            COLLECT_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            UsageCollectWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            collecting,
        )

        val pruning = PeriodicWorkRequestBuilder<PruningWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            PruningWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            pruning,
        )

        // Daily reflection: once per day (FR-DR1). Cached per day, so an extra run is harmless.
        val reflection = PeriodicWorkRequestBuilder<DailyReflectionWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            DailyReflectionWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reflection,
        )

        // Phase-2/3 drift check → intervention cycle (UsageStatsManager has no real-time events).
        val driftCheck = PeriodicWorkRequestBuilder<InterventionCheckWorker>(
            COLLECT_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            InterventionCheckWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            driftCheck,
        )

        // Kick off one aggregation now so the first summaries appear without waiting a day.
        runAggregationNow()
    }

    /** Runs a single aggregation pass, replacing any pending one. */
    fun runAggregationNow() {
        val aggregation = OneTimeWorkRequestBuilder<AggregationWorker>().build()
        workManager.enqueueUniqueWork(
            AggregationWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            aggregation,
        )
    }

    /**
     * Schedules the daily recurring-task generator (FR-T7). Independent of usage tracking:
     * recurring tasks are a Phase-1 core feature, so this runs even when tracking is off.
     * Idempotent via a unique name + KEEP.
     */
    fun scheduleRecurringTasks() {
        val recurring = PeriodicWorkRequestBuilder<RecurringTaskWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            RecurringTaskWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            recurring,
        )

        // Also issue today's instances immediately, so a first install doesn't wait a day.
        val now = OneTimeWorkRequestBuilder<RecurringTaskWorker>().build()
        workManager.enqueueUniqueWork(
            RecurringTaskWorker.UNIQUE_NAME + "_now",
            ExistingWorkPolicy.KEEP,
            now,
        )
    }

    companion object {
        /** ~15 minutes: the shortest battery-friendly periodic interval (docs 02 §4.1). */
        const val COLLECT_INTERVAL_MINUTES = 15L
    }
}

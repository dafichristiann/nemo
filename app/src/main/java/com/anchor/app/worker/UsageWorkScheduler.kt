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
 * Central place that enqueues Nemo's background work (docs 02-architecture.md §5).
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

        // Daily reflection: once per day (FR-DR1). Scheduled near evening (21:00) local time.
        val reflectionInitialDelay = calculateInitialDelayForReflection(targetHour = 21, targetMinute = 0)
        val reflection = PeriodicWorkRequestBuilder<DailyReflectionWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(reflectionInitialDelay, TimeUnit.MILLISECONDS)
            .build()
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

    /**
     * Cancels the usage-tracking pipeline: collection, pruning, aggregation, drift check and the
     * daily reflection. Called when the user opts out of tracking, so no background collection or
     * processing keeps running after consent is withdrawn (privacy default is OFF, FR-S6).
     *
     * Recurring-task generation is intentionally left untouched — it is a Phase-1 core feature
     * independent of usage tracking.
     */
    fun cancelPeriodic() {
        workManager.cancelUniqueWork(UsageCollectWorker.UNIQUE_NAME)
        workManager.cancelUniqueWork(PruningWorker.UNIQUE_NAME)
        workManager.cancelUniqueWork(InterventionCheckWorker.UNIQUE_NAME)
        workManager.cancelUniqueWork(DailyReflectionWorker.UNIQUE_NAME)
        workManager.cancelUniqueWork(AggregationWorker.UNIQUE_NAME)
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

    private fun calculateInitialDelayForReflection(targetHour: Int = 21, targetMinute: Int = 0): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            set(java.util.Calendar.MINUTE, targetMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (now.after(target)) {
            target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        /** ~15 minutes: the shortest battery-friendly periodic interval (docs 02 §4.1). */
        const val COLLECT_INTERVAL_MINUTES = 15L
    }
}

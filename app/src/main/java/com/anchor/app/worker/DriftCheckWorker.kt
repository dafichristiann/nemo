package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.usage.DetectDriftUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic drift check (FR-D1..D6, docs 02-architecture.md §4.3, §5).
 *
 * Phase 2 detection is **periodic** (WorkManager) because `UsageStatsManager` has no
 * real-time events (PRD §10.3). Sprint 3 only computes the signal; the Intervention Engine
 * that consumes it arrives in Sprint 4. Result is a successful no-op when there is no drift
 * or not enough data — never a crash.
 */
@HiltWorker
class DriftCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val detectDrift: DetectDriftUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        detectDrift()
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "drift_check"
    }
}

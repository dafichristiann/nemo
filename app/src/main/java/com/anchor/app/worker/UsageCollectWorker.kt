package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.usage.CollectUsageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic collection of raw usage sessions (FR-W2, docs 02-architecture.md §5).
 *
 * Idempotent and permission-aware: missing `PACKAGE_USAGE_STATS` is a successful no-op
 * (the app keeps working as a Smart Todo). Results feed [AggregationWorker].
 */
@HiltWorker
class UsageCollectWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val collectUsage: CollectUsageUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        collectUsage()
        Result.success()
    }.getOrElse {
        // Transient failures (OEM throttling) → retry with backoff; never crash (docs 02 §6).
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "usage_collect"
    }
}

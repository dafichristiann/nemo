package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.usage.AggregateUsageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Aggregates raw sessions into daily/weekly summaries (FR-W3, docs 02-architecture.md §5).
 *
 * Idempotent: aggregation fully recomputes each day from raw data, so re-running (or running
 * after a collection) never double-counts.
 */
@HiltWorker
class AggregationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val aggregateUsage: AggregateUsageUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        aggregateUsage()
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "usage_aggregate"
    }
}

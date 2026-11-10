package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.usage.AggregateUsageUseCase
import com.anchor.app.core.domain.usecase.usage.CollectUsageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Periodic collection of raw usage sessions (FR-W2, docs 02-architecture.md §5).
 *
 * Idempotent and permission-aware: missing `PACKAGE_USAGE_STATS` is a successful no-op
 * (the app keeps working as a Smart Todo). Automatically aggregates raw sessions into
 * daily summaries so drift detection and insights stay fresh in the background.
 */
@HiltWorker
class UsageCollectWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val collectUsage: CollectUsageUseCase,
    private val aggregateUsage: AggregateUsageUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        collectUsage()
        aggregateUsage()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        if (runAttemptCount >= 3) Result.failure() else Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "usage_collect"
    }
}

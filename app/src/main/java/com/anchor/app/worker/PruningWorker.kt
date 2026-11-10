package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.usage.PruneUsageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Prunes raw sessions past the retention window (FR-W4, PRD §10.2; docs 03 §10).
 *
 * Idempotent: deleting already-deleted rows is a no-op. The retention window is clamped to
 * 14–30 days inside [PruneUsageUseCase].
 */
@HiltWorker
class PruningWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pruneUsage: PruneUsageUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        pruneUsage()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        if (runAttemptCount >= 3) Result.failure() else Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "usage_prune"
    }
}

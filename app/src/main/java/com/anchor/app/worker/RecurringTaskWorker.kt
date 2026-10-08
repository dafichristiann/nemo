package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.task.GenerateRecurringTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Issues the day's recurring task instances in the background (PRD FR-T7, §10.1), so a new
 * daily/weekly instance exists even on days the user never opens the Tasks tab.
 *
 * Idempotent: [GenerateRecurringTasksUseCase] never creates a second instance for the same
 * occurrence date, so running alongside the Tasks-screen generation is harmless.
 */
@HiltWorker
class RecurringTaskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val generateRecurring: GenerateRecurringTasksUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        generateRecurring()
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "recurring_tasks"
    }
}

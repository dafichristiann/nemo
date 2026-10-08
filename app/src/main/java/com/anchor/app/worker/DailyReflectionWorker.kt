package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.insight.GenerateDailyReflectionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Generates the Daily Reflection once per day (FR-DR1). Safe to run repeatedly: the use case
 * caches per day, so a second run in the same day is a no-op unless forced.
 *
 * Never fails hard — invalid/again-requested AI responses already fall back to rule-based, so a
 * success result is returned after the (always-available) local generation.
 */
@HiltWorker
class DailyReflectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val generateDailyReflection: GenerateDailyReflectionUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        generateDailyReflection()
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "daily_reflection"
    }
}

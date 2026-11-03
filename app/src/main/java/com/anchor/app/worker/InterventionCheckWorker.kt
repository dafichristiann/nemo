package com.anchor.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.app.core.domain.usecase.intervention.InterventionEngine
import com.anchor.app.core.domain.usecase.usage.DetectDriftUseCase
import com.anchor.app.platform.notification.InterventionDispatcher
import com.anchor.app.platform.notification.NotificationPermissions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Full intervention cycle (Sprint 4): detect drift → evaluate against quota/cooldown →
 * deliver via the chosen channel (docs/superpowers/design/04-intervention-engine.md §1,
 * 02-architecture.md §4.3).
 *
 * Replaces the Sprint-3 `DriftCheckWorker` no-op that only computed the signal. Suppressed
 * detections cost no quota (the engine never records them). Any failure retries; a missing
 * permission or no-drift day is a successful no-op, never a crash.
 */
@HiltWorker
class InterventionCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val detectDrift: DetectDriftUseCase,
    private val engine: InterventionEngine,
    private val dispatcher: InterventionDispatcher,
    private val notificationPermissions: NotificationPermissions,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val signal = detectDrift()
        // The worker runs in the background, so the user is not inside Nemo; the channel is
        // picked from notification availability (falls back to in-app when denied).
        val decision = engine.evaluate(
            signal,
            notificationPermissionGranted = notificationPermissions.canPostNotifications(),
            userInsideNemo = false,
        )
        dispatcher.dispatch(decision)
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "intervention_check"
    }
}

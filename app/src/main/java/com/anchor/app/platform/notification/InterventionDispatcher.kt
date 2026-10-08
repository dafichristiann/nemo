package com.anchor.app.platform.notification

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.InterventionChannel
import com.anchor.app.core.domain.model.InterventionDecision
import com.anchor.app.core.domain.usecase.intervention.InterventionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delivers an [InterventionDecision.Show] through the chosen channel (FR-I3, docs 04 §3.4, §8).
 *
 * Priority: **Notification** (primary) → **In-app** (when notifications are unavailable). The
 * in-app surface is exposed as [activeIntervention] so the UI layer can render it as an overlay
 * (Sprint 4 screen 04). Every shown intervention is persisted via the engine first, so quota and
 * cooldown accounting stays correct regardless of channel.
 *
 * Fullscreen is intentionally not used yet — docs 04 §3.4 keeps it for rare, truly relevant cases.
 */
@Singleton
class InterventionDispatcher @Inject constructor(
    private val engine: InterventionEngine,
    private val notifier: InterventionNotifier,
    private val notificationPermissions: NotificationPermissions,
    private val timeProvider: TimeProvider,
) {

    /** The currently shown in-app intervention, if the in-app channel was used. */
    private val _activeIntervention = MutableStateFlow<ActiveIntervention?>(null)
    val activeIntervention: StateFlow<ActiveIntervention?> = _activeIntervention.asStateFlow()

    /** Persists and delivers [decision]; a no-op when it was suppressed. Returns the log id. */
    suspend fun dispatch(decision: InterventionDecision): String? {
        if (decision !is InterventionDecision.Show) return null

        val logId = engine.recordShown(decision, now = timeProvider.nowMillis())
        val useNotification = decision.channel == InterventionChannel.NOTIFICATION &&
            notificationPermissions.canPostNotifications() &&
            notifier.canNotify()

        if (useNotification) {
            notifier.show(logId, decision.level, decision.signal.relatedTaskId)
        } else {
            // Fallback: surface in-app (PRD edge case "notification ditolak -> in-app saja").
            _activeIntervention.value = ActiveIntervention(
                logId = logId,
                level = decision.level,
                relatedTaskId = decision.signal.relatedTaskId,
            )
        }
        return logId
    }

    /** Clears the in-app surface once the user has acted or dismissed it. */
    fun clearActive() {
        _activeIntervention.value = null
    }

    /** Data the in-app intervention overlay needs (docs 07-screens.md screen 04). */
    data class ActiveIntervention(
        val logId: String,
        val level: Int,
        val relatedTaskId: String?,
    )
}

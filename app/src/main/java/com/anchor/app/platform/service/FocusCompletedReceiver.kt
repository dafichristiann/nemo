package com.anchor.app.platform.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anchor.app.MainActivity
import com.anchor.app.R
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.usecase.focus.FinishFocusUseCase
import com.anchor.app.platform.notification.NotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the alarm when a focus session's countdown completes in the background.
 */
@AndroidEntryPoint
class FocusCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var focusRepository: FocusRepository

    @Inject
    lateinit var finishFocus: FinishFocusUseCase

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val session = if (sessionId != null) {
                    focusRepository.getById(sessionId)
                } else {
                    focusRepository.current()
                }

                if (session != null && (session.status == FocusStatus.ACTIVE || session.status == FocusStatus.PAUSED)) {
                    val plannedMs = (session.plannedDurationSec ?: 0) * 1000L
                    val rawElapsed = if (session.status == FocusStatus.ACTIVE) {
                        (timeProvider.nowMillis() - session.startedAt).coerceAtLeast(session.actualDurationMs)
                    } else {
                        session.actualDurationMs
                    }
                    val elapsed = if (plannedMs > 0) maxOf(rawElapsed, plannedMs) else rawElapsed
                    finishFocus(session.id, elapsed, aborted = false, forceCompleteTask = true)
                    FocusService.stop(context)

                    if (settingsRepository.get().notificationsEnabled) {
                        showCompletedNotification(context, session.id)
                    }
                }
            }
            pendingResult.finish()
        }
    }

    private fun showCompletedNotification(context: Context, sessionId: String) {
        val openAppIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, "focus")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.FOCUS_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_nemo)
            .setContentTitle("Sesi Fokus Selesai!")
            .setContentText("Selamat! Sesi fokus Anda telah selesai.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION = "com.anchor.app.action.FOCUS_COMPLETED"
        const val EXTRA_SESSION_ID = "extra_session_id"
        private const val NOTIFICATION_ID = 4202
    }
}

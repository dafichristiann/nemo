package com.anchor.app.platform.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anchor.app.MainActivity
import com.anchor.app.R
import com.anchor.app.core.domain.model.InterventionAction
import com.anchor.app.core.domain.model.InterventionLevel
import com.anchor.app.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the contextual intervention notification — the primary channel (FR-I3,
 * docs/superpowers/design/04-intervention-engine.md §3.4, §8).
 *
 * Tone is calm and non-judgemental (docs 04 §6): the copy only says "you've been here longer
 * than usual" and offers choices ([Start Focus] / [Later]) — never a command or a diagnosis.
 * The notification carries the intervention level so the receiver can record the right outcome.
 */
@Singleton
class InterventionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    private val manager = NotificationManagerCompat.from(context)

    /** Whether the OS and user settings currently allow posting. */
    suspend fun canNotify(): Boolean =
        manager.areNotificationsEnabled() && settingsRepository.get().notificationsEnabled

    /**
     * Shows one intervention notification. [logId] and [level] travel back through the intent
     * actions so the user's choice updates the exact `intervention_log` row.
     */
    suspend fun show(logId: String, level: Int, relatedTaskId: String?) {
        val settings = settingsRepository.get()
        if (!settings.notificationsEnabled) return

        ensureChannel()

        val actionName = if (!relatedTaskId.isNullOrBlank()) ACTION_OPEN_TASK else ACTION_START_FOCUS
        val route = if (!relatedTaskId.isNullOrBlank()) "tasks" else "focus"

        val contentIntent = PendingIntent.getActivity(
            context,
            logId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_LOG_ID, logId)
                putExtra(MainActivity.EXTRA_ACTION, actionName)
                putExtra(MainActivity.EXTRA_TASK_ID, relatedTaskId)
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, route)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nemo)
            .setContentTitle(context.getString(titleFor(level)))
            .setContentText(context.getString(bodyFor(level)))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(bodyFor(level))))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent(logId))
            .addAction(
                0,
                context.getString(R.string.intervention_action_start_focus),
                actionIntent(logId, InterventionAction.START_FOCUS, relatedTaskId),
            )
            .addAction(
                0,
                context.getString(R.string.intervention_action_later),
                actionIntent(logId, InterventionAction.LATER, relatedTaskId),
            )

        // Guard the POST_NOTIFICATIONS requirement at the call site (Android 13+) so the lint
        // dataflow and the runtime both see an explicit check before notifying (FR-I3).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(logId.hashCode(), builder.build())
    }

    /** Dismisses a previously shown intervention notification. */
    fun cancel(logId: String) {
        manager.cancel(logId.hashCode())
    }

    private fun deleteIntent(logId: String): PendingIntent {
        val intent = Intent(context, InterventionActionReceiver::class.java).apply {
            action = InterventionActionReceiver.ACTION
            putExtra(InterventionActionReceiver.EXTRA_LOG_ID, logId)
            putExtra(InterventionActionReceiver.EXTRA_ACTION, "DISMISSED")
        }
        return PendingIntent.getBroadcast(
            context,
            (logId + "_delete").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(
        logId: String,
        action: InterventionAction,
        relatedTaskId: String?,
    ): PendingIntent {
        val intent = Intent(context, InterventionActionReceiver::class.java).apply {
            this.action = InterventionActionReceiver.ACTION
            putExtra(InterventionActionReceiver.EXTRA_LOG_ID, logId)
            putExtra(InterventionActionReceiver.EXTRA_ACTION, action.name)
            putExtra(InterventionActionReceiver.EXTRA_TASK_ID, relatedTaskId)
        }
        return PendingIntent.getBroadcast(
            context,
            (logId + action.name).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.intervention_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.intervention_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = NotificationChannels.INTERVENTION
        const val ACTION_OPEN_TASK = "ACTION_OPEN_TASK"
        const val ACTION_START_FOCUS = "ACTION_START_FOCUS"

        fun titleFor(level: Int): Int = when (level) {
            InterventionLevel.FOCUS -> R.string.intervention_notif_title_focus
            InterventionLevel.FUTURE_VIEW -> R.string.intervention_notif_title_future
            InterventionLevel.CONTEXTUAL -> R.string.intervention_notif_title_contextual
            else -> R.string.intervention_notif_title_gentle
        }

        fun bodyFor(level: Int): Int = when (level) {
            InterventionLevel.FOCUS -> R.string.intervention_notif_body_focus
            InterventionLevel.FUTURE_VIEW -> R.string.intervention_notif_body_future
            InterventionLevel.CONTEXTUAL -> R.string.intervention_notif_body_contextual
            else -> R.string.intervention_notif_body_gentle
        }
    }
}

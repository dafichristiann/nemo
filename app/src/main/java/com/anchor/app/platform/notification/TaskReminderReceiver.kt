package com.anchor.app.platform.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anchor.app.MainActivity
import com.anchor.app.R
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver triggered by AlarmManager when a task reminder is due.
 */
@AndroidEntryPoint
class TaskReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val type = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TYPE_AT_DEADLINE
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (!settingsRepository.get().notificationsEnabled) return@runCatching

                val task = taskRepository.getById(taskId)
                if (task == null || task.isDone) return@runCatching

                val title = if (type == TYPE_BEFORE_DEADLINE) {
                    context.getString(R.string.task_reminder_title_approaching)
                } else {
                    context.getString(R.string.task_reminder_title_due)
                }

                val contentText = if (type == TYPE_BEFORE_DEADLINE) {
                    context.getString(R.string.task_reminder_body_approaching, task.title)
                } else {
                    context.getString(R.string.task_reminder_body_due, task.title)
                }

                val openTaskIntent = PendingIntent.getActivity(
                    context,
                    taskId.hashCode(),
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_OPEN_ROUTE, "tasks")
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
                    .setSmallIcon(R.drawable.ic_stat_nemo)
                    .setContentTitle(title)
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(openTaskIntent)
                    .build()

                NotificationManagerCompat.from(context).notify((taskId + "_" + type).hashCode(), notification)
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION = "com.anchor.app.action.TASK_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val TYPE_BEFORE_DEADLINE = "BEFORE"
        const val TYPE_AT_DEADLINE = "DEADLINE"
    }
}

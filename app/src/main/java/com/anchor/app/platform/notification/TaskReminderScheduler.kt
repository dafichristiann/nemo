package com.anchor.app.platform.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules alarms for task deadline reminders:
 *  - H-1 hour before the deadline
 *  - At the exact deadline
 */
@Singleton
class TaskReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider,
) {

    /**
     * Schedules reminders for [task] if it has an upcoming deadline and is not yet completed.
     * Cancels existing reminders if completed or no deadline.
     */
    fun scheduleReminder(task: Task) {
        if (task.isDone || task.deadline == null) {
            cancelReminder(task.id)
            return
        }

        val deadline = task.deadline
        val now = timeProvider.nowMillis()

        // 1. Reminder 1 hour before deadline (H-1)
        val oneHourBefore = deadline - ONE_HOUR_MS
        if (oneHourBefore > now) {
            scheduleAlarm(
                triggerAtMillis = oneHourBefore,
                requestCode = requestCode(task.id, TaskReminderReceiver.TYPE_BEFORE_DEADLINE),
                taskId = task.id,
                type = TaskReminderReceiver.TYPE_BEFORE_DEADLINE,
            )
        } else {
            cancelAlarm(requestCode(task.id, TaskReminderReceiver.TYPE_BEFORE_DEADLINE))
        }

        // 2. Reminder at deadline
        if (deadline > now) {
            scheduleAlarm(
                triggerAtMillis = deadline,
                requestCode = requestCode(task.id, TaskReminderReceiver.TYPE_AT_DEADLINE),
                taskId = task.id,
                type = TaskReminderReceiver.TYPE_AT_DEADLINE,
            )
        } else {
            cancelAlarm(requestCode(task.id, TaskReminderReceiver.TYPE_AT_DEADLINE))
        }
    }

    /**
     * Cancels any scheduled reminders for [taskId].
     */
    fun cancelReminder(taskId: String) {
        cancelAlarm(requestCode(taskId, TaskReminderReceiver.TYPE_BEFORE_DEADLINE))
        cancelAlarm(requestCode(taskId, TaskReminderReceiver.TYPE_AT_DEADLINE))
    }

    private fun scheduleAlarm(
        triggerAtMillis: Long,
        requestCode: Int,
        taskId: String,
        type: String,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_REMINDER_TYPE, type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun requestCode(taskId: String, type: String): Int =
        (taskId + "_" + type).hashCode()

    companion object {
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
    }
}

package com.anchor.app.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anchor.app.core.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reschedules task reminders when the device finishes booting.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var taskReminderScheduler: TaskReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskRepository.getAllOnce()
                tasks.filter { !it.isDone && it.deadline != null }.forEach { task ->
                    taskReminderScheduler.scheduleReminder(task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

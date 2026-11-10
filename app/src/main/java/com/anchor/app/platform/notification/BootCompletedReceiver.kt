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
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskRepository.getAllOnce()
                tasks.filter { !it.isDone && it.deadline != null }.forEach { task ->
                    taskReminderScheduler.scheduleReminder(task)
                }
            } catch (_: Throwable) {
                // Ignore reschedule errors during broadcast handling
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

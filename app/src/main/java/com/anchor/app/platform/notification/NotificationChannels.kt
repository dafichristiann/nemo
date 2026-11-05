package com.anchor.app.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Manages notification channel definitions for the app.
 */
object NotificationChannels {
    const val INTERVENTION = "intervention"
    const val FOCUS = "focus"
    const val FOCUS_ALERTS = "focus_alerts"
    const val REMINDERS = "reminders"
    const val DAILY_REFLECTION = "daily_reflection"

    fun createAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intervention = NotificationChannel(
            INTERVENTION,
            "Gentle check-ins",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Occasional, non-judgemental nudges when you drift from what matters."
        }

        val focus = NotificationChannel(
            FOCUS,
            "Focus sessions",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows an ongoing focus session with a way to exit at any time."
        }

        val focusAlerts = NotificationChannel(
            FOCUS_ALERTS,
            "Focus Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when a focus session is completed."
            enableVibration(true)
        }

        val reminders = NotificationChannel(
            REMINDERS,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for task deadlines."
            enableVibration(true)
        }

        val dailyReflection = NotificationChannel(
            DAILY_REFLECTION,
            "Daily Reflection",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reflection and productivity insights."
        }

        manager.createNotificationChannels(
            listOf(intervention, focus, focusAlerts, reminders, dailyReflection),
        )
    }
}

package com.anchor.app.platform.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anchor.app.MainActivity
import com.anchor.app.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that keeps the Focus session alive while it runs (FR-F7,
 * docs 08-permissions.md §5, docs 07-screens.md §2.10).
 *
 * Rules honoured here:
 *  - started **only** while a session is active, stopped on finish/abort;
 *  - always shows a persistent, transparent notification;
 *  - exposes a clear **emergency exit** action (stops the service, opens the app);
 *  - battery-aware: the caller decides whether to start it; the service never forces itself.
 *
 * The actual session state lives in Room (`focus_session`); this service only owns the
 * persistent notification + process lifetime, so it stays thin and testable-by-inspection.
 */
@AndroidEntryPoint
class FocusService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startForegroundCompat()
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        ensureChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(getString(R.string.focus_service_title))
        .setContentText(getString(R.string.focus_service_text))
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setContentIntent(openAppIntent())
        .addAction(0, getString(R.string.focus_emergency_exit), stopIntent())
        .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        this,
        1,
        Intent(this, FocusService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.focus_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.focus_channel_desc) }
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.anchor.app.action.FOCUS_START"
        const val ACTION_STOP = "com.anchor.app.action.FOCUS_STOP"
        private const val CHANNEL_ID = "focus"
        private const val NOTIFICATION_ID = 4201

        /** Starts the service; a no-op on devices that forbid the start (never throws). */
        fun start(context: Context) {
            runCatching {
                val intent = Intent(context, FocusService::class.java).setAction(ACTION_START)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        /** Stops the service (finish, abort, emergency exit). Safe to call repeatedly. */
        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, FocusService::class.java).setAction(ACTION_STOP),
                )
            }
        }
    }
}

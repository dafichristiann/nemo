package com.anchor.app.platform.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anchor.app.MainActivity
import com.anchor.app.R
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.usecase.focus.FinishFocusUseCase
import com.anchor.app.platform.audio.SoundscapePlayer
import com.anchor.app.platform.notification.NotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    @Inject
    lateinit var focusRepository: FocusRepository

    @Inject
    lateinit var finishFocus: FinishFocusUseCase

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var soundscapePlayer: SoundscapePlayer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_FOCUS, ACTION_STOP -> {
                cancelCompletionAlarm()
                soundscapePlayer.stop()
                serviceScope.launch {
                    try {
                        val session = focusRepository.current()
                        if (session != null && (session.status == FocusStatus.ACTIVE || session.status == FocusStatus.PAUSED)) {
                            val elapsed = if (session.status == FocusStatus.ACTIVE) {
                                (timeProvider.nowMillis() - session.startedAt).coerceAtLeast(session.actualDurationMs)
                            } else {
                                session.actualDurationMs
                            }
                            finishFocus(session.id, elapsed, aborted = true)
                        }
                    } finally {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
                return START_NOT_STICKY
            }
            ACTION_PAUSE_FOCUS -> {
                cancelCompletionAlarm()
                soundscapePlayer.pause()
                serviceScope.launch {
                    val session = focusRepository.current()
                    updateNotification(session = session, isPaused = true)
                }
                return START_NOT_STICKY
            }
            ACTION_RESUME_FOCUS -> {
                soundscapePlayer.resume()
                serviceScope.launch {
                    val session = focusRepository.current()
                    session?.let { scheduleCompletionAlarm(it) }
                    updateNotification(session = session, isPaused = false)
                }
                return START_NOT_STICKY
            }
            ACTION_START_FOCUS, ACTION_START, null -> {
                startForegroundCompat()
                serviceScope.launch {
                    val session = focusRepository.current()
                    session?.let { scheduleCompletionAlarm(it) }
                    updateNotification(session = session, isPaused = false)
                }
                val soundscapeEnabled = intent?.getBooleanExtra(EXTRA_SOUNDSCAPE_ENABLED, true) ?: true
                if (soundscapeEnabled) {
                    val soundResId = intent?.getIntExtra(EXTRA_SOUND_RES_ID, R.raw.nemo_lofi_focus)
                        ?: R.raw.nemo_lofi_focus
                    soundscapePlayer.play(soundResId)
                }
            }
        }
        // NOT sticky: the session lives in Room and the service is started only while a session is
        // active, so the OS must not recreate a sessionless "phantom" foreground notification.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCompletionAlarm()
        soundscapePlayer.release()
        serviceScope.cancel()
    }

    private fun startForegroundCompat() {
        ensureChannel()
        val notification = buildNotification(session = null, isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(session: FocusSession?, isPaused: Boolean) {
        val notification = buildNotification(session = session, isPaused = isPaused)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(session: FocusSession?, isPaused: Boolean): Notification {
        val contentText = if (isPaused) {
            getString(R.string.focus_service_paused)
        } else {
            getString(R.string.focus_service_text)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nemo)
            .setContentTitle(getString(R.string.focus_service_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent())
            .addAction(0, getString(R.string.focus_emergency_exit), stopIntent())

        if (!isPaused && session != null && session.status == FocusStatus.ACTIVE && session.plannedDurationSec != null && session.plannedDurationSec > 0) {
            val now = timeProvider.nowMillis()
            val elapsedMs = session.actualDurationMs + (now - session.startedAt).coerceAtLeast(0L)
            val remainingMs = (session.plannedDurationSec * 1000L - elapsedMs).coerceAtLeast(0L)
            if (remainingMs > 0) {
                val targetEndTime = now + remainingMs
                builder.setWhen(targetEndTime)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
            }
        }
        return builder.build()
    }

    private fun scheduleCompletionAlarm(session: FocusSession) {
        if (session.plannedDurationSec == null || session.plannedDurationSec <= 0) return
        val now = timeProvider.nowMillis()
        val elapsedMs = session.actualDurationMs + if (session.status == FocusStatus.ACTIVE) {
            (now - session.startedAt).coerceAtLeast(0L)
        } else {
            0L
        }
        val remainingMs = (session.plannedDurationSec * 1000L - elapsedMs).coerceAtLeast(0L)
        if (remainingMs <= 0) return
        val triggerAtMillis = now + remainingMs

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(this, FocusCompletedReceiver::class.java).apply {
            action = FocusCompletedReceiver.ACTION
            putExtra(FocusCompletedReceiver.EXTRA_SESSION_ID, session.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            COMPLETION_ALARM_REQ_CODE,
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

    private fun cancelCompletionAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(this, FocusCompletedReceiver::class.java).apply {
            action = FocusCompletedReceiver.ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            COMPLETION_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, "focus")
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        this,
        1,
        Intent(this, FocusService::class.java).setAction(ACTION_STOP_FOCUS),
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
        const val ACTION_START_FOCUS = "com.anchor.app.action.FOCUS_START"
        const val ACTION_START = ACTION_START_FOCUS
        const val ACTION_STOP_FOCUS = "com.anchor.app.action.FOCUS_STOP"
        const val ACTION_STOP = ACTION_STOP_FOCUS
        const val ACTION_PAUSE_FOCUS = "com.anchor.app.action.FOCUS_PAUSE"
        const val ACTION_RESUME_FOCUS = "com.anchor.app.action.FOCUS_RESUME"
        const val EXTRA_SOUNDSCAPE_ENABLED = "extra_soundscape_enabled"
        const val EXTRA_SOUND_RES_ID = "extra_sound_res_id"
        private const val CHANNEL_ID = NotificationChannels.FOCUS
        private const val NOTIFICATION_ID = 4201
        private const val COMPLETION_ALARM_REQ_CODE = 4203

        /** Starts the service; a no-op on devices that forbid the start (never throws). */
        fun start(
            context: Context,
            soundscapeEnabled: Boolean = true,
            @RawRes soundResId: Int = R.raw.nemo_lofi_focus,
        ) {
            runCatching {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_START_FOCUS
                    putExtra(EXTRA_SOUNDSCAPE_ENABLED, soundscapeEnabled)
                    putExtra(EXTRA_SOUND_RES_ID, soundResId)
                }
                // minSdk is 26, so startForegroundService is always available.
                context.startForegroundService(intent)
            }
        }

        /** Pauses background soundscape while focus is paused. */
        fun pause(context: Context) {
            runCatching {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_PAUSE_FOCUS
                }
                context.startService(intent)
            }
        }

        /** Resumes background soundscape when focus is resumed. */
        fun resume(context: Context) {
            runCatching {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_RESUME_FOCUS
                }
                context.startService(intent)
            }
        }

        /** Stops the service (finish, abort, emergency exit). Safe to call repeatedly. */
        fun stop(context: Context) {
            // `stopService` is the correct, always-permitted API for tearing down a started
            // foreground service. (`startService(...ACTION_STOP)` throws from the background on
            // API 26+, so the emergency exit could silently fail exactly when it is needed.)
            runCatching { context.stopService(Intent(context, FocusService::class.java)) }
        }
    }
}

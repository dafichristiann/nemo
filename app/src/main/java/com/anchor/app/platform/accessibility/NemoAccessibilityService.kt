package com.anchor.app.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anchor.app.R
import com.anchor.app.core.di.BlockingEntryPoint
import com.anchor.app.core.di.DoomscrollEntryPoint
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.platform.blocking.BlockingState
import com.anchor.app.platform.notification.NotificationChannels
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Enforces Strict/Custom blocking (FR-F4/F8, PRD v0.4 §10.5) and continuous doomscroll alerts (specs 2026-11-01).
 *
 * Subscribes to `TYPE_WINDOW_STATE_CHANGED` only. When the foreground app changes to one the
 * current session is blocking, it draws the [BlockOverlayWindow].
 * When continuous scrolling on entertainment/social media exceeds the configured threshold,
 * it presents [DoomscrollOverlayWindow] as a mindful intervention and posts a companion alert notification.
 */
class NemoAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main)

    private val blockingState: BlockingState by lazy {
        EntryPoints.get(
            applicationContext,
            BlockingEntryPoint::class.java,
        ).blockingState()
    }

    private val settingsRepository by lazy {
        EntryPoints.get(
            applicationContext,
            DoomscrollEntryPoint::class.java,
        ).settingsRepository()
    }

    private val overlay: BlockOverlayWindow by lazy { BlockOverlayWindow(this) }
    private val doomscrollOverlay: DoomscrollOverlayWindow by lazy { DoomscrollOverlayWindow(this) }

    private val doomscrollTracker: DoomscrollTracker by lazy {
        DoomscrollTracker(scope = serviceScope).apply {
            onThresholdReached = { _, thresholdMinutes ->
                android.util.Log.i(
                    "NemoAccessibilityService",
                    "Doomscroll threshold reached ($thresholdMinutes min)! Showing overlay & alert.",
                )
                doomscrollOverlay.show(
                    thresholdMinutes = thresholdMinutes,
                    onStop = {
                        dismissDoomscrollNotification()
                        reset()
                    },
                    onSnooze = {
                        dismissDoomscrollNotification()
                        snooze()
                    },
                )
                showDoomscrollNotification(thresholdMinutes)
            }
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> doomscrollTracker.onScreenOff()
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> doomscrollTracker.onScreenOn()
                ACTION_DOOMSCROLL_STOP -> {
                    dismissDoomscrollNotification()
                    doomscrollOverlay.hide()
                    doomscrollTracker.reset()
                }
                ACTION_DOOMSCROLL_SNOOZE -> {
                    dismissDoomscrollNotification()
                    doomscrollOverlay.hide()
                    doomscrollTracker.snooze()
                }
            }
        }
    }

    /** Debounce: avoid re-showing the overlay for consecutive events of the same package. */
    private var lastBlockedPackage: String? = null

    companion object {
        const val DOOMSCROLL_NOTIFICATION_ID = 8842
        const val ACTION_DOOMSCROLL_STOP = "com.anchor.app.ACTION_DOOMSCROLL_STOP"
        const val ACTION_DOOMSCROLL_SNOOZE = "com.anchor.app.ACTION_DOOMSCROLL_SNOOZE"

        private val SYSTEM_IMMUNE_PACKAGES = setOf(
            "android",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.google.android.gms",
            "com.google.android.googlequicksearchbox",
            "com.samsung.android.app.cocktailbarservice",
            "com.transsion.smartpanel",
            "com.transsion.floating",
            // Keyboards / IMEs: input events must not reset or interrupt doomscroll tracking
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.sec.android.inputmethod",
            "com.touchtype.swiftkey",
            "com.sohu.inputmethod.sogou",
            "com.baidu.input",
        )
    }

    private fun isImmunePackage(pkg: String): Boolean {
        if (pkg == packageName || pkg in SYSTEM_IMMUNE_PACKAGES) return true
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        val imeList = runCatching { imm?.enabledInputMethodList }.getOrNull()
        return imeList?.any { it.packageName == pkg } == true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.i("NemoAccessibilityService", "onServiceConnected successfully called and active!")
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(ACTION_DOOMSCROLL_STOP)
            addAction(ACTION_DOOMSCROLL_SNOOZE)
        }
        runCatching {
            ContextCompat.registerReceiver(
                this,
                systemReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }

        serviceScope.launch {
            settingsRepository.observe().collect { settings ->
                android.util.Log.i(
                    "NemoAccessibilityService",
                    "Config update: doomscrollEnabled=${settings.doomscrollAlertEnabled}, threshold=${settings.doomscrollThresholdMinutes}m, blockedApps=${settings.blockedApps.size}",
                )
                doomscrollTracker.updateConfiguration(
                    enabled = settings.doomscrollAlertEnabled,
                    thresholdMinutes = settings.doomscrollThresholdMinutes,
                    blockedApps = settings.blockedApps,
                )
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return

        if (isImmunePackage(pkg)) return

        // Non-fullscreen sub-windows or overlays from other packages (e.g. launcher exit animations,
        // floating helpers, volume bars) should not disrupt an active doomscroll tracking session.
        val isTarget = DoomscrollTargets.isTarget(pkg, doomscrollTracker.blockedApps)
        if (!isTarget && event?.isFullScreen == false && doomscrollTracker.isTracking) {
            return
        }

        val snapshot = blockingState.snapshot()
        val isBlocked = snapshot.sessionActive && pkg in snapshot.blockedApps

        if (isBlocked) {
            // Priority 1: Focus session active and app is blocked
            dismissDoomscrollNotification()
            doomscrollOverlay.hide()
            doomscrollTracker.reset()

            if (pkg != lastBlockedPackage) {
                val isGentle = snapshot.mode == FocusMode.GENTLE
                val shown = overlay.show(isGentle = isGentle) { /* dismissed; re-arms on next entry */ }
                if (shown) lastBlockedPackage = pkg
            }
        } else {
            // Priority 2: Not focus-blocked
            lastBlockedPackage = null
            overlay.hide()

            if (doomscrollTracker.isEnabled && !snapshot.sessionActive) {
                doomscrollTracker.onPackageEvent(pkg)
                // Only dismiss overlay if tracking has fully stopped (e.g. user stayed away past debounce)
                if (!doomscrollTracker.isTracking) {
                    dismissDoomscrollNotification()
                    doomscrollOverlay.hide()
                }
            } else {
                dismissDoomscrollNotification()
                doomscrollTracker.reset()
                doomscrollOverlay.hide()
            }
        }
    }

    private fun showDoomscrollNotification(thresholdMinutes: Int) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return

        val homePendingIntent = PendingIntent.getActivity(
            this,
            9001,
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(ACTION_DOOMSCROLL_SNOOZE).apply {
            `package` = packageName
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            9002,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.INTERVENTION)
            .setSmallIcon(R.drawable.ic_stat_nemo)
            .setContentTitle(getString(R.string.doomscroll_overlay_title, thresholdMinutes))
            .setContentText(getString(R.string.doomscroll_overlay_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(homePendingIntent)
            .addAction(
                0,
                getString(R.string.doomscroll_overlay_stop),
                homePendingIntent,
            )
            .addAction(
                0,
                getString(R.string.doomscroll_overlay_snooze),
                snoozePendingIntent,
            )
            .build()

        runCatching {
            manager.notify(DOOMSCROLL_NOTIFICATION_ID, notification)
        }
    }

    private fun dismissDoomscrollNotification() {
        runCatching {
            NotificationManagerCompat.from(this).cancel(DOOMSCROLL_NOTIFICATION_ID)
        }
    }

    /** OS disabled or interrupted the service: release blocking so no app stays paused. */
    override fun onInterrupt() {
        dismissDoomscrollNotification()
        overlay.hide()
        doomscrollOverlay.hide()
        doomscrollTracker.reset()
        runCatching { blockingState.release() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        dismissDoomscrollNotification()
        overlay.hide()
        doomscrollOverlay.hide()
        doomscrollTracker.reset()
        runCatching { blockingState.release() }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(systemReceiver) }
        dismissDoomscrollNotification()
        overlay.hide()
        doomscrollOverlay.hide()
        doomscrollTracker.reset()
        serviceJob.cancel()
        super.onDestroy()
    }
}

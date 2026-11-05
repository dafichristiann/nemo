package com.anchor.app.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.anchor.app.core.di.BlockingEntryPoint
import com.anchor.app.core.di.DoomscrollEntryPoint
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.platform.blocking.BlockingState
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
 * it presents [DoomscrollOverlayWindow] as a mindful intervention.
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
                doomscrollOverlay.show(
                    thresholdMinutes = thresholdMinutes,
                    onStop = { reset() },
                    onSnooze = { snooze() },
                )
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> doomscrollTracker.onScreenOff()
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> doomscrollTracker.onScreenOn()
            }
        }
    }

    /** Debounce: avoid re-showing the overlay for consecutive events of the same package. */
    private var lastBlockedPackage: String? = null

    companion object {
        private val SYSTEM_IMMUNE_PACKAGES = setOf(
            "android",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
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
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching { registerReceiver(screenReceiver, filter) }

        serviceScope.launch {
            settingsRepository.observe().collect { settings ->
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

        val snapshot = blockingState.snapshot()
        val isBlocked = snapshot.sessionActive && pkg in snapshot.blockedApps

        if (isBlocked) {
            // Priority 1: Focus session active and app is blocked
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
                if (pkg != doomscrollTracker.currentPackage) {
                    doomscrollOverlay.hide()
                }
            } else {
                doomscrollTracker.reset()
                doomscrollOverlay.hide()
            }
        }
    }

    /** OS disabled or interrupted the service: release blocking so no app stays paused. */
    override fun onInterrupt() {
        overlay.hide()
        doomscrollOverlay.hide()
        doomscrollTracker.reset()
        runCatching { blockingState.release() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.hide()
        doomscrollOverlay.hide()
        doomscrollTracker.reset()
        runCatching { blockingState.release() }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        overlay.hide()
        doomscrollOverlay.hide()
        doomscrollTracker.reset()
        serviceJob.cancel()
        super.onDestroy()
    }
}

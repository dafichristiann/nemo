package com.anchor.app.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.anchor.app.core.di.BlockingEntryPoint
import com.anchor.app.platform.blocking.BlockingState
import dagger.hilt.EntryPoints

/**
 * Enforces Strict/Custom blocking (FR-F4/F8, PRD v0.4 §10.5).
 *
 * Subscribes to `TYPE_WINDOW_STATE_CHANGED` only. When the foreground app changes to one the
 * current session is blocking, it draws the [BlockOverlayWindow] — a calm "back to focus" screen,
 * never a forced kill (Awareness > Control, PRD §4). It reads [BlockingState] (kept in sync by
 * [com.anchor.app.platform.blocking.AccessibilityBlockingStrategy]) so it always sees the live
 * blocked set for the active session.
 *
 * No screen content is read (config: `canRetrieveWindowContent=false`), and it never throws:
 * showing the overlay is wrapped defensively; if the OS disables us mid-session we clear the
 * blocking state so nothing stays stuck (FR-F8).
 */
class AnchorAccessibilityService : AccessibilityService() {

    private val blockingState: BlockingState by lazy {
        EntryPoints.get(
            applicationContext,
            BlockingEntryPoint::class.java,
        ).blockingState()
    }

    private val overlay: BlockOverlayWindow by lazy { BlockOverlayWindow(this) }

    /** Debounce: avoid re-showing the overlay for consecutive events of the same package. */
    private var lastBlockedPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return

        // Never block ourselves — the overlay and the app must always be reachable.
        if (pkg == packageName) return

        val snapshot = blockingState.snapshot()
        val isBlocked = snapshot.sessionActive && pkg in snapshot.blockedApps

        if (isBlocked) {
            // Show on every *entry* to a blocked app. Re-arming happens when the user leaves to a
            // non-blocked app (see else-branch), so each time they come back the overlay re-appears.
            if (pkg != lastBlockedPackage) {
                // Only debounce the entry once the overlay is confirmed attached; a failed
                // addView must not leave the app entered-but-unblocked with no retry.
                val shown = overlay.show { /* dismissed; re-arms on next entry */ }
                if (shown) lastBlockedPackage = pkg
            }
        } else {
            // Moved to a non-blocked app (or Home): drop the interlude and re-arm for next entry.
            lastBlockedPackage = null
            overlay.hide()
        }
    }

    /** OS disabled or interrupted the service: release blocking so no app stays paused. */
    override fun onInterrupt() {
        overlay.hide()
        runCatching { blockingState.release() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.hide()
        runCatching { blockingState.release() }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        overlay.hide()
        super.onDestroy()
    }
}

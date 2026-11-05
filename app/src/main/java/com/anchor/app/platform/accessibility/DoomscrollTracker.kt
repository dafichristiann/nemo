package com.anchor.app.platform.accessibility

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * In-memory continuous foreground tracker for doomscroll-prone packages (specs 2026-11-01).
 *
 * Tracks active foreground duration on apps configured in [blockedApps] (or defaults).
 * Provides a 10-second debounce grace period when switching temporarily to other apps,
 * supports +5 minutes snooze, and triggers [onThresholdReached] once the continuous threshold is hit.
 */
class DoomscrollTracker(
    private val scope: CoroutineScope,
    private val clock: () -> Long = {
        try {
            android.os.SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    },
    private val debounceDelayMs: Long = 10_000L,
    private val snoozeDurationMs: Long = 5 * 60 * 1000L,
    var onThresholdReached: (packageName: String, thresholdMinutes: Int) -> Unit = { _, _ -> },
) {
    var isEnabled: Boolean = false
        private set

    var thresholdMinutes: Int = 20
        private set

    var blockedApps: Set<String> = emptySet()
        private set

    var currentPackage: String? = null
        private set

    var isPaused: Boolean = false
        private set

    var isOverlayShowing: Boolean = false
        private set

    var targetRemainingMs: Long = 0L
        private set

    private var segmentStartRealtimeMs: Long = 0L
    private var thresholdJob: Job? = null
    private var debounceJob: Job? = null

    val isTracking: Boolean
        get() = currentPackage != null

    fun updateConfiguration(
        enabled: Boolean,
        thresholdMinutes: Int,
        blockedApps: Set<String>,
    ) {
        this.isEnabled = enabled
        this.thresholdMinutes = thresholdMinutes
        this.blockedApps = blockedApps
        if (!enabled) {
            reset()
        }
    }

    fun onPackageEvent(pkg: String) {
        if (!isEnabled) {
            reset()
            return
        }

        val effectiveTargets = DoomscrollTargets.DEFAULT_TARGETS + blockedApps
        val isTarget = pkg in effectiveTargets

        if (isTarget) {
            debounceJob?.cancel()
            debounceJob = null

            if (currentPackage == null) {
                // Sesi baru
                startNewSession(pkg, thresholdMinutes * 60 * 1000L)
            } else if (currentPackage == pkg) {
                // Kembali ke aplikasi target yang sama
                if (isPaused) {
                    val pausedDuration = maxOf(0L, clock() - segmentStartRealtimeMs)
                    if (pausedDuration >= debounceDelayMs) {
                        reset()
                        startNewSession(pkg, thresholdMinutes * 60 * 1000L)
                    } else {
                        resumeSession()
                    }
                }
            } else {
                // Berpindah langsung ke aplikasi target yang berbeda
                reset()
                startNewSession(pkg, thresholdMinutes * 60 * 1000L)
            }
        } else {
            // Berpindah ke luar aplikasi target (home, launcher, app lain)
            if (currentPackage != null) {
                if (!isPaused) {
                    pauseSession()
                } else if (clock() - segmentStartRealtimeMs >= debounceDelayMs) {
                    reset()
                }
            }
        }
    }

    private fun startNewSession(pkg: String, durationMs: Long) {
        currentPackage = pkg
        isPaused = false
        isOverlayShowing = false
        targetRemainingMs = durationMs
        segmentStartRealtimeMs = clock()
        scheduleThresholdJob(targetRemainingMs)
    }

    private fun pauseSession() {
        val now = clock()
        val elapsedInSegment = maxOf(0L, now - segmentStartRealtimeMs)
        targetRemainingMs = maxOf(0L, targetRemainingMs - elapsedInSegment)
        segmentStartRealtimeMs = now
        isPaused = true

        thresholdJob?.cancel()
        thresholdJob = null

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceDelayMs)
            reset()
        }
    }

    private fun resumeSession() {
        debounceJob?.cancel()
        debounceJob = null
        isPaused = false
        segmentStartRealtimeMs = clock()
        scheduleThresholdJob(targetRemainingMs)
    }

    private fun scheduleThresholdJob(delayMs: Long) {
        thresholdJob?.cancel()
        thresholdJob = scope.launch {
            if (delayMs > 0L) {
                delay(delayMs)
            }
            targetRemainingMs = 0L
            isOverlayShowing = true
            currentPackage?.let { pkg ->
                onThresholdReached(pkg, thresholdMinutes)
            }
        }
    }

    fun onScreenOff() {
        if (!isEnabled || currentPackage == null) return
        if (!isPaused) {
            pauseSession()
        }
    }

    fun onScreenOn() {
        if (!isEnabled || currentPackage == null) return
        if (isPaused) {
            val pausedDuration = maxOf(0L, clock() - segmentStartRealtimeMs)
            if (pausedDuration < debounceDelayMs) {
                resumeSession()
            } else {
                reset()
            }
        }
    }

    fun snooze() {
        if (currentPackage == null) return
        isOverlayShowing = false
        debounceJob?.cancel()
        debounceJob = null
        isPaused = false
        targetRemainingMs = snoozeDurationMs
        segmentStartRealtimeMs = clock()
        scheduleThresholdJob(targetRemainingMs)
    }

    fun reset() {
        thresholdJob?.cancel()
        thresholdJob = null
        debounceJob?.cancel()
        debounceJob = null
        currentPackage = null
        isPaused = false
        isOverlayShowing = false
        targetRemainingMs = 0L
        segmentStartRealtimeMs = 0L
    }
}

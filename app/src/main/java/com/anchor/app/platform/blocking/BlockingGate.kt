package com.anchor.app.platform.blocking

/**
 * The three runtime conditions that must all hold before Strict/Custom blocking may engage
 * (PRD v0.4 §10.5, Spike B). Kept as a tiny seam so [AccessibilityBlockingStrategy] is unit-
 * testable without the Android framework: the production implementation reads BuildConfig, the
 * persisted opt-in toggle, and the OS Accessibility status.
 */
interface BlockingGate {
    /** Build-level master switch ([com.anchor.app.BuildConfig.FOCUS_BLOCKING_ENABLED]). */
    fun isBuildFlagOn(): Boolean

    /** User's persisted opt-in ([com.anchor.app.core.domain.model.AppSettings.focusBlockingEnabled]). */
    fun isToggleOn(): Boolean

    /** True while the Anchor AccessibilityService is enabled in the OS. */
    fun isServiceEnabled(): Boolean
}

package com.anchor.app.core.domain.model

/**
 * App-wide settings (docs/superpowers/design/03-database-schema.md §7.1).
 * Defaults are privacy-preserving: tracking & AI OFF until the user opts in (FR-S6).
 */
data class AppSettings(
    val trackingEnabled: Boolean = false,
    val aiEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val usageRetentionDays: Int = 30,
    val onboardingCompleted: Boolean = false,
    val locale: String = "en",
    /** Apps to pause during Strict/Custom focus (FR-S4). Stored as a set. */
    val blockedApps: Set<String> = emptySet(),
    /**
     * Opt-in master switch for Strict/Custom app blocking (FR-F4/F8, PRD v0.4 §10.5).
     * Default OFF: privacy-preserving, per Spike B "feature-flagged & opt-in".
     */
    val focusBlockingEnabled: Boolean = false,
) {
    companion object {
        const val MIN_RETENTION_DAYS = 14
        const val MAX_RETENTION_DAYS = 30
    }
}

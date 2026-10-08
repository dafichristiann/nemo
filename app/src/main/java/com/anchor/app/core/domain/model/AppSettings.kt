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
) {
    companion object {
        const val MIN_RETENTION_DAYS = 14
        const val MAX_RETENTION_DAYS = 30
    }
}

package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row settings table (docs/superpowers/design/03-database-schema.md §7.1).
 *
 * Defaults are privacy-preserving: tracking & AI are OFF until the user opts in
 * (PRD FR-S6, docs 09-privacy §0).
 *
 * Enum-valued fields are stored as `String` (via the enums' `.name`) so adding new
 * values never requires a migration (docs 03 §0 point 6).
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val trackingEnabled: Boolean = false,
    val aiEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val usageRetentionDays: Int = DEFAULT_RETENTION_DAYS,
    val defaultFocusMode: String = "GENTLE",
    val dailyInterventionQuota: Int = 3,
    val cooldownHours: Int = 2,
    val onboardingCompleted: Boolean = false,
    val locale: String = "en",
    /** JSON array of blocked package names (FR-S4). Null/empty = none. */
    val blockedAppsJson: String? = null,
    /** Opt-in: master switch for Strict/Custom app blocking (FR-F4/F8, PRD v0.4 §10.5). Default OFF. */
    val focusBlockingEnabled: Boolean = false,
    /** Preferred name the mascot greets the user with. Null/blank = none set. */
    val nickname: String? = null,
    /** The user's main focus area, stored as the [com.anchor.app.core.domain.model.FocusArea] name. */
    val focusArea: String = "COLLEGE",
    /** Default focus-session length in minutes (25/45/60). Prefills Focus Setup. */
    val defaultFocusDurationMin: Int = DEFAULT_FOCUS_DURATION_MIN,
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_RETENTION_DAYS = 30
        const val DEFAULT_FOCUS_DURATION_MIN = 25
    }
}
